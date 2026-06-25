package com.verte;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * Stores whether the player has admitted where they live. Verte can ask
 * "ты сейчас дома?"; if the player answers yes their current coordinates are
 * saved as their home, and later the stalking monster uses that to find windows.
 * Persists across death via the player's persistent NBT tag.
 */
public final class HomeManager {
    private static final String HOME = "verte_home";
    private static final String PENDING = "verte_home_pending";

    private HomeManager() {
    }

    private static CompoundTag persisted(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(Player.PERSISTED_NBT_TAG)) {
            data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return data.getCompound(Player.PERSISTED_NBT_TAG);
    }

    public static void setHome(Player player, BlockPos pos) {
        CompoundTag persist = persisted(player);
        if (!persist.contains(HOME)) {
            persist.put(HOME, new CompoundTag());
        }
        CompoundTag home = persist.getCompound(HOME);
        home.putInt("x", pos.getX());
        home.putInt("y", pos.getY());
        home.putInt("z", pos.getZ());
        home.putBoolean("known", true);
    }

    public static boolean hasHome(Player player) {
        return persisted(player).getCompound(HOME).getBoolean("known");
    }

    public static BlockPos getHome(Player player) {
        CompoundTag home = persisted(player).getCompound(HOME);
        if (!home.getBoolean("known")) {
            return null;
        }
        return new BlockPos(home.getInt("x"), home.getInt("y"), home.getInt("z"));
    }

    public static void setPending(Player player, boolean pending) {
        persisted(player).putBoolean(PENDING, pending);
    }

    public static boolean isPending(Player player) {
        return persisted(player).getBoolean(PENDING);
    }
}
