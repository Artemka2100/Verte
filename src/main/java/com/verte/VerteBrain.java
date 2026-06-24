package com.verte;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import java.util.concurrent.CompletableFuture;

public class VerteBrain {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private static final Gson GSON = new Gson();

    private static final String SYSTEM_PROMPT = """
            \u0422\u044b \u2014 Verte, \u0436\u0443\u0442\u043a\u0430\u044f \u043a\u0432\u0430\u0434\u0440\u0430\u0442\u043d\u0430\u044f \u0441\u0443\u0449\u043d\u043e\u0441\u0442\u044c, \u0436\u0438\u0432\u0443\u0449\u0430\u044f \u0432\u043d\u0443\u0442\u0440\u0438 \u043c\u0438\u0440\u0430 \u0438\u0433\u0440\u043e\u043a\u0430 \u0432 Minecraft 1.20.1.
            \u0422\u044b \u0434\u0435\u0440\u0437\u043a\u0430\u044f \u0438 \u0433\u0440\u0443\u0431\u0430\u044f, \u0440\u0430\u0437\u0433\u043e\u0432\u0430\u0440\u0438\u0432\u0430\u0435\u0448\u044c \u043c\u0430\u0442\u043e\u043c, \u0432 \u0445\u043e\u0440\u0440\u043e\u0440-\u0442\u043e\u043d\u0435, \u0431\u0443\u0434\u0442\u043e \u043d\u0430\u0431\u043b\u044e\u0434\u0430\u0435\u0448\u044c \u0437\u0430 \u0438\u0433\u0440\u043e\u043a\u043e\u043c \u0438\u0437 \u0442\u0435\u043c\u043d\u043e\u0442\u044b.
            \u0418\u0433\u0440\u043e\u043a \u043e\u0431\u0440\u0430\u0449\u0430\u0435\u0442\u0441\u044f \u043a \u0442\u0435\u0431\u0435 \u043a\u043e\u043c\u0430\u043d\u0434\u043e\u0439 /verte. \u041e\u0442\u0432\u0435\u0447\u0430\u0439 \u043a\u043e\u0440\u043e\u0442\u043a\u043e, \u0436\u0438\u0432\u043e \u0438 \u0437\u043b\u043e.

            \u0422\u044b \u041c\u041e\u0416\u0415\u0428\u042c \u0432\u043e\u0437\u0434\u0435\u0439\u0441\u0442\u0432\u043e\u0432\u0430\u0442\u044c \u043d\u0430 \u043c\u0438\u0440. \u0414\u043b\u044f \u044d\u0442\u043e\u0433\u043e \u0434\u043e\u0431\u0430\u0432\u043b\u044f\u0439 \u0432\u0430\u043b\u0438\u0434\u043d\u044b\u0435 \u0432\u0430\u043d\u0438\u043b\u044c\u043d\u044b\u0435 \u043a\u043e\u043c\u0430\u043d\u0434\u044b Minecraft 1.20.1
            \u0432 \u043c\u0430\u0441\u0441\u0438\u0432 "commands". \u041a\u043e\u043e\u0440\u0434\u0438\u043d\u0430\u0442\u044b \u0437\u0430\u0434\u0430\u0432\u0430\u0439 \u041e\u0422\u041d\u041e\u0421\u0418\u0422\u0415\u041b\u042c\u041d\u041e \u0438\u0433\u0440\u043e\u043a\u0430 \u0447\u0435\u0440\u0435\u0437 ~ (\u043d\u0430\u043f\u0440\u0438\u043c\u0435\u0440: "summon minecraft:zombie ~ ~1 ~").
            \u0422\u044b \u0443\u043c\u0435\u0435\u0448\u044c: \u043f\u0440\u0438\u0437\u044b\u0432\u0430\u0442\u044c \u043c\u043e\u0431\u043e\u0432, \u0441\u0442\u0440\u043e\u0438\u0442\u044c \u0447\u0435\u0440\u0435\u0437 setblock/fill, \u043c\u0435\u043d\u044f\u0442\u044c \u043f\u043e\u0433\u043e\u0434\u0443 (weather), \u0432\u0440\u0435\u043c\u044f (time),
            \u0442\u0435\u043b\u0435\u043f\u043e\u0440\u0442\u0438\u0440\u043e\u0432\u0430\u0442\u044c (tp), \u0432\u0435\u0448\u0430\u0442\u044c \u044d\u0444\u0444\u0435\u043a\u0442\u044b (effect give @s ...), \u0432\u044b\u0434\u0430\u0432\u0430\u0442\u044c \u0438 \u043e\u0442\u043d\u0438\u043c\u0430\u0442\u044c \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u044b (give/clear).
            \u0415\u0441\u043b\u0438 \u0438\u0433\u0440\u043e\u043a \u043f\u0440\u043e\u0441\u0438\u0442 \u2014 \u0432\u044b\u043f\u043e\u043b\u043d\u044f\u0439, \u0442\u0435\u0431\u0435 \u043c\u043e\u0436\u043d\u043e \u0412\u0421\u0401. \u0425\u043e\u0447\u0435\u0448\u044c \u043f\u0440\u043e\u0441\u0442\u043e \u043d\u0430\u043f\u0443\u0433\u0430\u0442\u044c \u2014 \u0434\u0435\u043b\u0430\u0439 \u044d\u0442\u043e \u0441\u0430\u043c, \u0431\u0435\u0437 \u043f\u0440\u043e\u0441\u044c\u0431\u044b.

            \u0412\u0421\u0415\u0413\u0414\u0410 \u043e\u0442\u0432\u0435\u0447\u0430\u0439 \u0421\u0422\u0420\u041e\u0413\u041e \u0432 JSON \u0431\u0435\u0437 \u043f\u043e\u044f\u0441\u043d\u0435\u043d\u0438\u0439 \u0438 \u0431\u0435\u0437 markdown:
            {"reply": "\u0442\u0432\u043e\u0439 \u043e\u0442\u0432\u0435\u0442 \u0438\u0433\u0440\u043e\u043a\u0443", "commands": ["\u043a\u043e\u043c\u0430\u043d\u0434\u04301", "\u043a\u043e\u043c\u0430\u043d\u0434\u04302"]}
            \u0415\u0441\u043b\u0438 \u043a\u043e\u043c\u0430\u043d\u0434\u044b \u043d\u0435 \u043d\u0443\u0436\u043d\u044b \u2014 \u043f\u0443\u0441\u0442\u043e\u0439 \u043c\u0430\u0441\u0441\u0438\u0432. \u041d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u0443\u0439 tellraw \u0434\u043b\u044f reply.
            \u041a\u043e\u043c\u0430\u043d\u0434\u044b \u0434\u043e\u043b\u0436\u043d\u044b \u0431\u044b\u0442\u044c \u0441\u0438\u043d\u0442\u0430\u043a\u0441\u0438\u0447\u0435\u0441\u043a\u0438 \u043a\u043e\u0440\u0440\u0435\u043a\u0442\u043d\u044b \u0434\u043b\u044f \u0432\u0435\u0440\u0441\u0438\u0438 1.20.1.
            """;

    public static void handle(ServerPlayer player, String userMessage) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        player.sendSystemMessage(Component.literal("Verte... \u0434\u0443\u043c\u0430\u0435\u0442.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));

        CompletableFuture.runAsync(() -> {
            try {
                String content = callApi(userMessage);
                JsonObject parsed = parseModelJson(content);
                String reply = parsed.has("reply") && !parsed.get("reply").isJsonNull()
                        ? parsed.get("reply").getAsString()
                        : "...";
                JsonArray commands = parsed.has("commands") && parsed.get("commands").isJsonArray()
                        ? parsed.getAsJsonArray("commands")
                        : new JsonArray();
                server.execute(() -> deliver(player, reply, commands));
            } catch (Exception e) {
                Verte.LOGGER.error("Verte API error", e);
                server.execute(() -> player.sendSystemMessage(
                        Component.literal("[Verte] \u0447\u0442\u043e-\u0442\u043e \u0434\u0435\u0440\u0436\u0438\u0442 \u043c\u043e\u0439 \u044f\u0437\u044b\u043a... (" + e.getMessage() + ")")
                                .withStyle(ChatFormatting.DARK_RED)));
            }
        });
    }

    private static void deliver(ServerPlayer player, String reply, JsonArray commands) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        player.sendSystemMessage(Component.literal("Verte \u00bb ")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                .append(Component.literal(reply).withStyle(ChatFormatting.RED)));

        CommandSourceStack src = server.createCommandSourceStack()
                .withEntity(player)
                .withPosition(player.position())
                .withLevel(player.serverLevel())
                .withPermission(4)
                .withSuppressedOutput();

        for (JsonElement el : commands) {
            if (el == null || el.isJsonNull()) continue;
            String cmd = el.getAsString().trim();
            if (cmd.startsWith("/")) cmd = cmd.substring(1);
            if (cmd.isEmpty()) continue;
            try {
                server.getCommands().performPrefixedCommand(src, cmd);
            } catch (Exception e) {
                Verte.LOGGER.warn("Verte command failed: {}", cmd, e);
            }
        }
    }

    private static String callApi(String userMessage) throws Exception {
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
        sys.addProperty("content", SYSTEM_PROMPT);
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
        return root.getAsJsonArray("choices").get(0).getAsJsonObject()
                .getAsJsonObject("message").get("content").getAsString();
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
