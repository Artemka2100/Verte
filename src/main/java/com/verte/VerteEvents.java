package com.verte;

import com.verte.entity.ModEntities;
import com.verte.entity.VerteEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
        if (persisted.getBoolean("verte_intro_done")) {
            return;
        }
        persisted.putBoolean("verte_intro_done", true);
        root.put(Player.PERSISTED_NBT_TAG, persisted);

        ServerLevel level = player.serverLevel();
        VerteEntity verte = ModEntities.VERTE.get().create(level);
        if (verte != null) {
            Vec3 spawn = player.position().add(player.getLookAngle().scale(2.0D));
            verte.moveTo(spawn.x, spawn.y, spawn.z, player.getYRot() + 180.0F, 0.0F);
            level.addFreshEntity(verte);
        }

        player.sendSystemMessage(Component.literal("\u041f\u0440\u0438\u0432\u0435\u0442. \u042f Verte \u2014 \u0442\u0432\u043e\u0439 \u043f\u0435\u0440\u0441\u043e\u043d\u0430\u043b\u044c\u043d\u044b\u0439 \u043f\u043e\u043c\u043e\u0449\u043d\u0438\u043a.")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("\u0421\u043f\u0440\u0430\u0448\u0438\u0432\u0430\u0439 \u043c\u0435\u043d\u044f \u043a\u043e\u043c\u0430\u043d\u0434\u043e\u0439 /verte \u0438\u043b\u0438 \u043d\u0430\u0436\u043c\u0438 V.")
                .withStyle(ChatFormatting.GRAY));
    }
}
