package com.verte;

import com.verte.net.VerteNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Every once in a while (random 5, 10 or 15 minutes) the player gets a short
 * ~5 second hallucination once Verte's corruption is at least "strange".
 * The kind is random: screen distortion (plus real nausea), a blood overlay,
 * or a looming huge Verte. Purely a scare — nothing is actually harmed.
 */
@Mod.EventBusSubscriber(modid = Verte.MOD_ID)
public final class HallucinationManager {

    // 5, 10 and 15 minutes expressed in ticks (20 ticks per second).
    private static final int[] INTERVALS = {6000, 12000, 18000};
    private static final int DURATION = 100; // ~5 seconds

    private static final Map<UUID, Long> NEXT_FIRE = new HashMap<>();

    private HallucinationManager() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        int phase = CorruptionManager.phaseOf(CorruptionManager.get(player));
        if (phase < CorruptionManager.PHASE_STRANGE) {
            NEXT_FIRE.remove(player.getUUID());
            return;
        }
        long now = player.level().getGameTime();
        Long next = NEXT_FIRE.get(player.getUUID());
        if (next == null) {
            NEXT_FIRE.put(player.getUUID(), now + nextInterval(player));
            return;
        }
        if (now >= next) {
            trigger(player, phase);
            NEXT_FIRE.put(player.getUUID(), now + nextInterval(player));
        }
    }

    private static long nextInterval(ServerPlayer player) {
        return INTERVALS[player.getRandom().nextInt(INTERVALS.length)];
    }

    private static void trigger(ServerPlayer player, int phase) {
        int type = player.getRandom().nextInt(3);
        if (type == 0) {
            // Distortion: real nausea warp on top of the client overlay.
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, DURATION + 40, 0, false, false));
        }
        VerteNetwork.sendHallucination(player, type, DURATION);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        NEXT_FIRE.remove(event.getEntity().getUUID());
    }
}
