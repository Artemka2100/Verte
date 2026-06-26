package com.verte;

import net.minecraft.world.entity.player.Player;

/**
 * CLASSIC (\"old\") version of the mod: there is no horror arc. Verte never
 * becomes strange, hostile or a monster \u2014 he stays a normal Steve-looking
 * companion who simply does whatever you ask. To guarantee that, corruption is
 * hard-wired to 0, which keeps every phase-gated system (DreadManager, story
 * escalation, the monster transformation) permanently in the friendly phase.
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

    /** Always 0 in the classic version \u2014 Verte never escalates. */
    public static int get(Player player) {
        return 0;
    }

    /** No-op in the classic version. */
    public static void set(Player player, int value) {
        // intentionally does nothing: the classic Verte cannot be corrupted
    }

    /** No-op in the classic version; always reports 0. */
    public static int add(Player player, int delta) {
        return 0;
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
