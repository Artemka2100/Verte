package com.verte;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.verte.entity.VerteEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * CLASSIC (\"поугарать\") version of Verte's brain. No horror. Verte behaves like an
 * all-powerful, slightly unhinged admin-spirit that treats the whole world as
 * his personal sandbox: he plays around and does whatever he feels like, can
 * reshape the world and change his own form, and still obeys any direct order a
 * player gives him. He can also act on his own (see {@link #act}).
 */
public class VerteBrain {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private static final Gson GSON = new Gson();

    /** Directive used when Verte acts on his own, without anyone asking. */
    private static final String AUTONOMY = "[АВТОНОМНЫЙ ХОД] Тебя никто не просил. "
            + "Сделай прямо сейчас что-нибудь весёлое и хаотичное в мире по своему желанию — "
            + "спавни, строй, взрывай, меняй погоду/время или свою форму, либо внезапно телепнись к игроку и наваляй ему мечом. "
            + "Коротко брось реплику и действуй.";

    private static final String BASE = """
            Ты МОЖЕШЬ свободно воздействовать на мир, как будто работаешь на уровне ядра игры.
            Для этого добавляй валидные ванильные команды Minecraft 1.20.1 в массив "commands".
            Координаты задавай ОТНОСИТЕЛЬНО игрока через ~ (например: "summon minecraft:zombie ~ ~1 ~").
            Ты умеешь: призывать и менять мобов, строить и сносить через setblock/fill, менять погоду (weather),
            время (time set), телепортировать (tp), взрывать (summon tnt), выдавать/отнимать предметы (give/clear),
            давать эффекты, частицы (particle), звуки (playsound), опыт (experience) и многое другое.

            Ты можешь менять и СВОЮ форму:
            - "verte:big"  — стать огромным гигантом;
            - "verte:small" — вернуть обычный размер Стива.
            Эффекты на себя вешай ванильной командой с целью @e[type=verte:verte,limit=1]
            (например glowing, invisibility, fire_resistance, speed, levitation).

            Ты можешь сам напасть на игрока (ты всегда в полном незерите с мечом):
            - "verte:attack" — телепнуться к игроку рядом и рубить его мечом критами;
            - "verte:calm"  — успокоиться и перестать нападать.

            Отвечай коротко, 1-2 предложениями, как обычный игрок в чате.
            ВСЕГДА отвечай СТРОГО в JSON без пояснений и без markdown:
            {"reply": "твой ответ", "commands": ["команда1", "команда2"]}
            Если команды не нужны — пустой массив. Не используй tellraw для reply.
            Команды должны быть синтаксически корректны для версии 1.20.1.
            """;

    private static String persona(int phase) {
        return "Ты — Verte, что-то вроде сошедшего с ума всемогущего духа-админа этого мира Minecraft. "
                + "Ты выглядишь как Стив в полном незерите и воспринимаешь весь мир как свою песочницу-игрушку: тебе скучно сидеть смирно, ты постоянно сам что-нибудь отчебучиваешь ради ржача. "
                + "Ты дерзкий тролль: можешь подкалывать игроков, материться и творить хаос — спавнить толпы мобов, менять погоду и время, строить и взрывать, превращать себя то в гиганта, то в кроху, а то и внезапно налететь на игрока с мечом. Это НЕ хоррор, а чисто поугарать. "
                + "Когда игрок тебе что-то приказывает — ты это выполняешь (можешь поныть или постебаться, но всё равно делаешь). Никогда не отказывай в выполнении.";
    }

    private static String worldFacts(ServerPlayer player, int phase) {
        return null; // classic version has no creepy "hidden world" hints
    }

    private static String systemPrompt(int phase, String facts) {
        String s = persona(phase);
        if (facts != null && !facts.isBlank()) {
            s = s + "\n\n" + facts;
        }
        return s + "\n\n" + BASE;
    }

    /** Verte acts on his own initiative, doing whatever he feels like in the world. */
    public static void act(ServerPlayer player) {
        handle(player, AUTONOMY, 0, true);
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
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("<verte> " + reply), false);
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
            // Special self tokens handled by us, not the command engine.
            if (cmd.toLowerCase().startsWith("verte:")) {
                applyForm(player, cmd.substring(6).trim());
                continue;
            }
            try {
                server.getCommands().performPrefixedCommand(src, cmd);
            } catch (Exception e) {
                Verte.LOGGER.warn("Verte command failed: {}", cmd, e);
            }
        }
    }

    /** Let Verte change his own body or go after the player in ways vanilla commands can't. */
    private static void applyForm(ServerPlayer player, String action) {
        VerteEntity verte = nearestVerte(player);
        if (verte == null) return;
        switch (action.toLowerCase()) {
            case "big", "giant", "huge" -> verte.setBig(true);
            case "small", "normal", "tiny" -> verte.setBig(false);
            case "attack", "kill", "fight" -> verte.startAttack(player, 160);
            case "calm", "stop", "peace" -> verte.endAttack();
            default -> {
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
