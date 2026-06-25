package com.verte;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Builds the horror atmosphere: ambient sounds that scale with corruption,
 * screen distortion / lighting effects, and "fourth wall" chat messages.
 * Everything runs server-side so it works in singleplayer and on servers.
 */
public class AtmosphereManager {

    private static final SoundEvent[] WHISPERS = {
            SoundEvents.AMBIENT_CAVE,
            SoundEvents.ENDERMAN_AMBIENT,
            SoundEvents.SCULK_SHRIEKER_SHRIEK,
            SoundEvents.WARDEN_HEARTBEAT
    };

    private AtmosphereManager() {
    }

    /** Random creepy sound played just behind the player; louder at higher phases. */
    public static void ambient(ServerLevel level, ServerPlayer player, int phase, RandomSource random) {
        SoundEvent sound = WHISPERS[random.nextInt(WHISPERS.length)];
        Vec3 dir = player.getViewVector(1.0F).normalize();
        double bx = player.getX() - dir.x * 3.0D;
        double bz = player.getZ() - dir.z * 3.0D;
        BlockPos at = BlockPos.containing(bx, player.getY(), bz);
        float volume = 0.4F + phase * 0.2F;
        level.playSound(null, at, sound, SoundSource.AMBIENT, volume, 0.6F + random.nextFloat() * 0.3F);
    }

    /** Screen distortion (nausea), darkening and occasional flicker (brief blindness). */
    public static void distort(ServerPlayer player, int phase, RandomSource random) {
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60 + phase * 20, 0, false, false));
        if (random.nextInt(3) == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false));
        }
        if (random.nextInt(5) == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 12, 0, false, false));
        }
    }

    /** Messages that break the fourth wall — fake system / other-player lines. */
    public static void fourthWall(ServerPlayer player, RandomSource random) {
        String name = player.getGameProfile().getName();
        switch (random.nextInt(5)) {
            case 0 -> player.sendSystemMessage(Component.literal("<" + name + "> \u043f\u043e\u043c\u043e\u0433\u0438\u0442\u0435")
                    .withStyle(ChatFormatting.GRAY));
            case 1 -> player.sendSystemMessage(Component.literal(name + " \u0432\u044b\u0448\u0435\u043b \u0438\u0437 \u0438\u0433\u0440\u044b")
                    .withStyle(ChatFormatting.YELLOW));
            case 2 -> player.sendSystemMessage(Component.literal("\u041e\u0431\u0435\u0440\u043d\u0438\u0441\u044c.")
                    .withStyle(ChatFormatting.WHITE));
            case 3 -> player.sendSystemMessage(Component.literal("\u042d\u0442\u043e\u0442 \u043c\u0438\u0440 \u0431\u043e\u043b\u044c\u0448\u0435 \u043d\u0435 \u0442\u0432\u043e\u0439.")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            default -> knowsWhereYouSleep(player);
        }
    }

    /** "I know where you sleep" — with the player's actual respawn coordinates. */
    public static void knowsWhereYouSleep(ServerPlayer player) {
        BlockPos bed = player.getRespawnPosition();
        String where = bed != null ? " (" + bed.getX() + ", " + bed.getY() + ", " + bed.getZ() + ")" : "";
        player.sendSystemMessage(Component.literal("\u042f \u0437\u043d\u0430\u044e, \u0433\u0434\u0435 \u0442\u044b \u0441\u043f\u0438\u0448\u044c." + where)
                .withStyle(ChatFormatting.DARK_RED));
    }
}
