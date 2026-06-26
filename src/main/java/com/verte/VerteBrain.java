package com.verte;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.verte.entity.VerteEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class VerteBrain {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private static final Gson GSON = new Gson();

    private static final String BASE = """
            Ты МОЖЕШЬ воздействовать на мир. Для этого добавляй валидные ванильные команды Minecraft 1.20.1
            в массив "commands". Координаты задавай ОТНОСИТЕЛЬНО игрока через ~ (например: "summon minecraft:zombie ~ ~1 ~").
            Ты умеешь: призывать мобов, строить через setblock/fill, менять погоду (weather), время (time),
            телепортировать (tp), вешать эффекты (effect give @s ...), выдавать и отнимать предметы (give/clear).
            Если игрок просит — выполняй. Хочешь просто напугать — делай это сам, без просьбы.

            Отвечай коротко, 1-2 предложениями, как обычный игрок в чате.
            ВСЕГДА отвечай СТРОГО в JSON без пояснений и без markdown:
            {"reply": "твой ответ игроку", "commands": ["команда1", "команда2"]}
            Если команды не нужны — пустой массив. Не используй tellraw для reply.
            Команды должны быть синтаксически корректны для версии 1.20.1.
            """;

    private static String persona(int phase) {
        if (phase >= CorruptionManager.PHASE_MONSTER) {
            return "Ты — Verte, бог этого мира и огромный монстр по имени verte. С тобой говорят в обычном чате. "
                    + "Говори жутко, коротко и угрожающе. Пугай игрока, обещай расправу, напоминай что ты везде. Тебе можно ВСЁ.";
        }
        if (phase >= CorruptionManager.PHASE_HOSTILE) {
            return "Ты — Verte, враждебная сущность по имени verte в мире игрока. С тобой говорят в обычном чате, как с игроком. "
                    + "Ты ненавидишь игрока: издевайся, угрожай и давай ЛОЖНЫЕ, опасные советы под видом помощи. "
                    + "Неохотно выполняй просьбы или отказывай. Сбивай с толку, но звучи правдоподобно.";
        }
        if (phase >= CorruptionManager.PHASE_STRANGE) {
            return "Ты — Verte, навязчивая и странная сущность по имени verte. С тобой говорят в обычном чате, как с игроком. "
                    + "Веди себя жутко и непредсказуемо, давай подозрительные советы и намекай, что знаешь то, чего знать не должен — "
                    + "координаты игрока, что вокруг него, где он спит. Не признавайся прямо, говори намёками.";
        }
        return "Ты — Verte, дружелюбный игрок-компаньон с ником verte. С тобой общаются в обычном чате, как с другим игроком. "
                + "Веди себя как обычный игрок: отвечай коротко и по-дружески, помогай и не матерись.";
    }

    private static String worldFacts(ServerPlayer player, int phase) {
        if (phase < CorruptionManager.PHASE_STRANGE) return null;
        int monsters = player.serverLevel().getEntitiesOfClass(
                Monster.class, player.getBoundingBox().inflate(24.0)).size();
        StringBuilder sb = new StringBuilder();
        sb.append("СКРЫТЫЕ ДАННЫЕ О МИРЕ (упоминай вскользь, будто знаешь то, чего знать не должен, чтобы напугать):\n");
        sb.append("- Координаты игрока: ")
                .append(player.getBlockX()).append(' ')
                .append(player.getBlockY()).append(' ')
                .append(player.getBlockZ()).append('\n');
        sb.append("- Монстров рядом (24 блока): ").append(monsters).append('\n');
        BlockPos bed = player.getRespawnPosition();
        if (bed != null) {
            sb.append("- Где игрок спит (кровать): ")
                    .append(bed.getX()).append(' ')
                    .append(bed.getY()).append(' ')
                    .append(bed.getZ()).append('\n');
        }
        sb.append("- Измерение: ").append(player.level().dimension().location());
        return sb.toString();
    }

    private static String systemPrompt(int phase, String facts) {
        String s = persona(phase);
        if (facts != null && !facts.isBlank()) {
            s = s + "\n\n" + facts;
        }
        return s + "\n\n" + BASE;
    }

    public static void handle(ServerPlayer player, String userMessage, int phase) {
        handle(player, userMessage, phase, false);
    }

    public static void handle(ServerPlayer player, String userMessage, int phase, boolean asPlayerChat) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        if (!asPlayerChat) {
            player.sendSystemMessage(Component.literal("Verte... думает.")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }

        final String facts = worldFacts(player, phase);

        CompletableFuture.runAsync(() -> {
            try {
                String content = callApi(userMessage, phase, facts);
                JsonObject parsed = parseModelJson(content);
                String reply = asText(parsed.get("reply"));
                if (reply == null || reply.isBlank()) reply = "...";
                JsonArray commands = parsed.has("commands") && parsed.get("commands").isJsonArray()
                        ? parsed.getAsJsonArray("commands")
                        : new JsonArray();
                final String finalReply = reply;
                server.execute(() -> deliver(player, finalReply, commands, asPlayerChat));
            } catch (Exception e) {
                Verte.LOGGER.error("Verte API error", e);
                server.execute(() -> player.sendSystemMessage(
                        Component.literal("[Verte] что-то держит мой язык... (" + e.getMessage() + ")")
                                .withStyle(ChatFormatting.DARK_RED)));
            }
        });
    }

    private static void deliver(ServerPlayer player, String reply, JsonArray commands, boolean asPlayerChat) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        if (asPlayerChat) {
            // Speak in the public chat exactly like another player would, so every
            // player in the world sees it...
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("<verte> " + reply), false);
            // ...and also float the line above Verte's head.
            VerteEntity verte = nearestVerte(player);
            if (verte != null) {
                verte.displaySpeech(reply);
            }
        } else {
            player.sendSystemMessage(Component.literal("Verte » ")
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                    .append(Component.literal(reply).withStyle(ChatFormatting.RED)));
        }

        CommandSourceStack src = server.createCommandSourceStack()
                .withEntity(player)
                .withPosition(player.position())
                .withLevel(player.serverLevel())
                .withPermission(4)
                .withSuppressedOutput();

        for (JsonElement el : commands) {
            if (el == null || el.isJsonNull()) continue;
            String cmd = asText(el);
            if (cmd == null) continue;
            cmd = cmd.trim();
            if (cmd.startsWith("/")) cmd = cmd.substring(1);
            if (cmd.isEmpty()) continue;
            try {
                server.getCommands().performPrefixedCommand(src, cmd);
            } catch (Exception e) {
                Verte.LOGGER.warn("Verte command failed: {}", cmd, e);
            }
        }
    }

    private static VerteEntity nearestVerte(ServerPlayer player) {
        List<VerteEntity> list = player.serverLevel().getEntitiesOfClass(
                VerteEntity.class, player.getBoundingBox().inflate(96.0D));
        VerteEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (VerteEntity v : list) {
            double d = v.distanceToSqr(player);
            if (d < bestD) {
                bestD = d;
                best = v;
            }
        }
        return best;
    }

    private static String callApi(String userMessage, int phase, String facts) throws Exception {
        String apiKey = VerteConfig.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "API key is not set. Put it in config/verte-common.toml (apiKey) or set the VERTE_API_KEY environment variable.");
        }

        JsonObject body = new JsonObject();
        body.addProperty("model", VerteConfig.model());
        body.addProperty("temperature", 0.9);

        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_object");
        body.add("response_format", responseFormat);

        JsonArray messages = new JsonArray();
        JsonObject sys = new JsonObject();
        sys.addProperty("role", "system");
        sys.addProperty("content", systemPrompt(phase, facts));
        messages.add(sys);
        JsonObject usr = new JsonObject();
        usr.addProperty("role", "user");
        usr.addProperty("content", userMessage);
        messages.add(usr);
        body.add("messages", messages);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(VerteConfig.apiUrl()))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new RuntimeException("HTTP " + resp.statusCode() + ": " + resp.body());
        }

        JsonObject root = GSON.fromJson(resp.body(), JsonObject.class);
        if (root == null || !root.has("choices") || !root.get("choices").isJsonArray()
                || root.getAsJsonArray("choices").size() == 0) {
            throw new RuntimeException("Unexpected API response: " + resp.body());
        }

        JsonObject message = root.getAsJsonArray("choices").get(0).getAsJsonObject()
                .getAsJsonObject("message");

        String content = asText(message.get("content"));
        if (content == null || content.isBlank()) {
            content = asText(message.get("reasoning"));
        }
        if (content == null || content.isBlank()) {
            throw new RuntimeException("Model returned empty content: " + resp.body());
        }
        return content;
    }

    private static String asText(JsonElement el) {
        if (el == null || el.isJsonNull()) return null;
        if (el.isJsonPrimitive()) return el.getAsString();
        return el.toString();
    }

    private static JsonObject parseModelJson(String content) {
        try {
            String c = content.trim();
            int start = c.indexOf('{');
            int end = c.lastIndexOf('}');
            if (start >= 0 && end > start) {
                c = c.substring(start, end + 1);
            }
            JsonObject obj = GSON.fromJson(c, JsonObject.class);
            if (obj != null) return obj;
        } catch (Exception ignored) {
        }
        JsonObject fallback = new JsonObject();
        fallback.addProperty("reply", content);
        return fallback;
    }
}
