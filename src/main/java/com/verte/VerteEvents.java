package com.verte;

import com.verte.entity.ModEntities;
import com.verte.entity.VerteEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class VerteEvents {

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        boolean introDone = persisted.getBoolean("verte_intro_done");
        if (!introDone) {
            persisted.putBoolean("verte_intro_done", true);
            root.put(Player.PERSISTED_NBT_TAG, persisted);
        }

        // There is only ever ONE Verte in the world. Spawn him for the very first
        // player to ever join, and never again \u2014 a friend joining later does not
        // create a second one.
        if (!verteExists(player.getServer())) {
            ServerLevel level = player.serverLevel();
            VerteEntity verte = ModEntities.VERTE.get().create(level);
            if (verte != null) {
                Vec3 spawn = player.position().add(player.getLookAngle().scale(2.0D));
                verte.moveTo(spawn.x, spawn.y, spawn.z, player.getYRot() + 180.0F, 0.0F);
                verte.setOwnerUUID(player.getUUID());
                level.addFreshEntity(verte);
            }
        }

        if (!introDone) {
            player.sendSystemMessage(Component.literal("\u041f\u0440\u0438\u0432\u0435\u0442. \u042f Verte \u2014 \u0442\u0432\u043e\u0439 \u043f\u0435\u0440\u0441\u043e\u043d\u0430\u043b\u044c\u043d\u044b\u0439 \u043f\u043e\u043c\u043e\u0449\u043d\u0438\u043a.")
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
            player.sendSystemMessage(Component.literal("\u041f\u0438\u0448\u0438 \u043c\u043d\u0435 \u043f\u0440\u044f\u043c\u043e \u0432 \u0447\u0430\u0442: \u043d\u0430\u0447\u043d\u0438 \u0441\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0435 \u0441\u043e \u0441\u043b\u043e\u0432\u0430 verte \u2014 \u043d\u0430\u043f\u0440\u0438\u043c\u0435\u0440: verte \u043f\u0440\u0438\u0432\u0435\u0442. \u041c\u0435\u043d\u044f \u0432\u0438\u0434\u044f\u0442 \u0432\u0441\u0435 \u0438\u0433\u0440\u043e\u043a\u0438.")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    private static boolean verteExists(MinecraftServer server) {
        if (server == null) {
            return true; // be safe: don't spawn if we can't check
        }
        for (ServerLevel sl : server.getAllLevels()) {
            for (Entity e : sl.getAllEntities()) {
                if (e instanceof VerteEntity verte && verte.isAlive()) {
                    return true;
                }
            }
        }
        return false;
    }
}
