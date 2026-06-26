package com.verte;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Drives the ambient horror that is NOT tied to a scripted story beat. Every few
 * seconds, scaled by the player's hidden corruption phase, Verte may breathe a
 * creepy sound behind the player, twist their screen, or whisper a line that
 * breaks the fourth wall. The higher the phase, the more often it happens.
 *
 * <p>All of this runs server-side, so it works the same in singleplayer and when
 * playing with a friend over LAN.
 */
public class DreadManager {

    /** How often (in ticks) we even consider doing something. 60 ticks = 3s. */
    private static final int CHECK_INTERVAL = 60;

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % CHECK_INTERVAL != 0) {
            return;
        }

        int phase = CorruptionManager.phaseOf(CorruptionManager.get(player));
        if (phase <= CorruptionManager.PHASE_FRIENDLY) {
            return; // a calm, friendly Verte does not haunt you
        }

        ServerLevel level = player.serverLevel();
        RandomSource random = player.getRandom();

        // Creepy ambient sound behind the player. More likely at higher phases.
        if (random.nextInt(4) < phase) {
            AtmosphereManager.ambient(level, player, phase, random);
        }

        // Screen distortion / darkness once things have turned hostile.
        if (phase >= CorruptionManager.PHASE_HOSTILE && random.nextInt(3) == 0) {
            AtmosphereManager.distort(player, phase, random);
        }

        // Rare fourth-wall whisper from the strange phase onward.
        if (phase >= CorruptionManager.PHASE_STRANGE && random.nextInt(8) == 0) {
            AtmosphereManager.fourthWall(player, random);
        }
    }
}
