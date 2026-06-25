package com.verte;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Lets the player talk to Verte by typing in normal chat, e.g.
 * "verte давай дружить" or "verte сруби дерево" — no slash command and no GUI.
 * The message itself is still shown in chat, so it reads like chatting with
 * another player, and Verte answers as <verte>.
 */
public class VerteChatHandler {

    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        String raw = event.getRawText();
        if (raw == null) {
            return;
        }
        String trimmed = raw.trim();
        String lower = trimmed.toLowerCase();
        if (!lower.startsWith("verte")) {
            return;
        }
        // Require "verte" to be its own word (avoid matching e.g. "vertex").
        if (lower.length() > 5 && Character.isLetter(lower.charAt(5))) {
            return;
        }

        ServerPlayer player = event.getPlayer();
        if (player == null) {
            return;
        }
        String rest = trimmed.length() > 5 ? trimmed.substring(5).trim() : "";
        rest = rest.replaceFirst("^[,!:\\-\\s]+", "");

        final String message = rest;
        MinecraftServer server = player.getServer();
        if (server != null) {
            server.execute(() -> VerteInteraction.handle(player, message));
        }
    }
}
