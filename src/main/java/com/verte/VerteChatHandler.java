package com.verte;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Lets the player talk to Verte by typing in normal chat, e.g.
 * "verte \u0441\u0434\u0435\u043b\u0430\u0439 \u043d\u043e\u0447\u044c" or "verte \u0441\u0440\u0443\u0431\u0438 \u0434\u0435\u0440\u0435\u0432\u043e" \u2014 no slash command and no GUI.
 * Everything after the "verte" prefix is handed to the AI brain (or the entity's
 * task handler). Visible to all players.
 */
public class VerteChatHandler {

    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player == null) {
            return;
        }
        String raw = event.getRawText();
        if (raw == null) {
            return;
        }
        String trimmed = raw.trim();
        String lower = trimmed.toLowerCase();
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        if (!lower.startsWith("verte")) {
            return;
        }
        // Require "verte" to be its own word (avoid matching e.g. "vertex").
        if (lower.length() > 5 && Character.isLetter(lower.charAt(5))) {
            return;
        }

        String rest = trimmed.length() > 5 ? trimmed.substring(5).trim() : "";
        rest = rest.replaceFirst("^[,!:\\-\\s]+", "");

        final String message = rest;
        server.execute(() -> VerteInteraction.handle(player, message));
    }
}
