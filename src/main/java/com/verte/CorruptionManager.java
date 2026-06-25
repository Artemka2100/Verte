package com.verte;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * Hidden "corruption" stat (0-100) persisted per-player in the player's NBT.
 * Maps corruption to one of four behaviour phases:
 *   0  (0-24)  Friendly helper
 *   1  (25-49) Strange / intrusive
 *   2  (50-74) Hostile
 *   3  (75-100) Monster
 */
public class CorruptionManager {

    public static final String KEY = "verte_corruption";
    public static final int MAX = 100;

    public static final int PHASE_FRIENDLY = 0;
    public static final int PHASE_STRANGE = 1;
    public static final int PHASE_HOSTILE = 2;
    public static final int PHASE_MONSTER = 3;

    private CorruptionManager() {
    }

    public static int get(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return clamp(persisted.getInt(KEY));
    }

    public static void set(Player player, int value) {
        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putInt(KEY, clamp(value));
        root.put(Player.PERSISTED_NBT_TAG, persisted);
    }

    public static int add(Player player, int delta) {
        int value = clamp(get(player) + delta);
        set(player, value);
        return value;
    }

    public static int clamp(int value) {
        if (value < 0) return 0;
        if (value > MAX) return MAX;
        return value;
    }

    public static int phaseOf(int corruption) {
        if (corruption >= 75) return PHASE_MONSTER;
        if (corruption >= 50) return PHASE_HOSTILE;
        if (corruption >= 25) return PHASE_STRANGE;
        return PHASE_FRIENDLY;
    }
}
