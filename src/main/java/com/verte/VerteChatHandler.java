package com.verte;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Lets the player talk to Verte by typing in normal chat, e.g.
 * "verte давай дружить" or "verte сруби дерево" — no slash command and no GUI.
 * Also catches a plain "да"/"нет" answer when Verte has just asked whether the
 * player is home, so the player never needs a command to reply.
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

        // Answering Verte's "ты сейчас дома?" question (plain yes/no, no prefix needed).
        if (HomeManager.isPending(player)) {
            if (isNo(lower)) {
                server.execute(() -> {
                    HomeManager.setPending(player, false);
                    server.getPlayerList().broadcastSystemMessage(
                            Component.literal("<verte> жаль. я всё равно найду."), false);
                });
                return;
            }
            if (isYes(lower)) {
                server.execute(() -> {
                    HomeManager.setHome(player, player.blockPosition());
                    HomeManager.setPending(player, false);
                    server.getPlayerList().broadcastSystemMessage(
                            Component.literal("<verte> хорошо. теперь я знаю, где ты живёшь."), false);
                });
                return;
            }
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

    private static boolean isYes(String s) {
        return s.startsWith("да") || s.contains(" да") || s.contains("ага")
                || s.contains("конечно") || s.contains("yes") || s.contains("yeah");
    }

    private static boolean isNo(String s) {
        return s.startsWith("нет") || s.contains("нет") || s.contains("не ")
                || s.contains("неа") || s.equals("no") || s.startsWith("no ");
    }
}
