package com.verte;

import com.verte.entity.VerteEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Routes a chat line addressed to Verte. First Verte tries to understand it as a
 * direct instruction (chop a tree, come, follow, stop, befriend). Anything else
 * becomes free-form conversation handled by the AI brain, answered in chat as if
 * Verte were another player.
 */
public final class VerteInteraction {

    private VerteInteraction() {
    }

    public static void handle(ServerPlayer player, String message) {
        ServerLevel level = player.serverLevel();
        int corruption = CorruptionManager.get(player);
        int phase = CorruptionManager.phaseOf(corruption);

        VerteEntity verte = nearest(level, player);
        if (verte == null) {
            player.sendSystemMessage(Component.literal("(Verte нет рядом. Поставь его из коробки.)")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return;
        }

        String msg = message.isBlank() ? "привет" : message;
        if (verte.handleChatCommand(player, msg, phase)) {
            return;
        }
        VerteBrain.handle(player, msg, phase, true);
    }

    private static VerteEntity nearest(ServerLevel level, ServerPlayer player) {
        List<VerteEntity> list = level.getEntitiesOfClass(VerteEntity.class,
                player.getBoundingBox().inflate(96.0D));
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
}
