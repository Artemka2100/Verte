package com.verte;

import com.verte.client.VerteBoxItemRenderer;
import com.verte.entity.ModEntities;
import com.verte.entity.VerteEntity;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/**
 * Right-click a block with the box and Verte climbs out of it, behaving like
 * another player. There is only ever ONE Verte: if he already exists somewhere
 * (e.g. another player summoned him), the box just calls him over instead of
 * spawning a duplicate. Sneak-interacting Verte puts him back into a box
 * (handled in {@link VerteEntity#mobInteract}).
 *
 * <p>The item is drawn as a real vanilla chest via {@link VerteBoxItemRenderer}.
 */
public class VerteBoxItem extends Item {

    public VerteBoxItem(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new VerteBoxItemRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Player player = context.getPlayer();
        MinecraftServer server = serverLevel.getServer();

        // If Verte already exists anywhere on the server, just bring him here.
        VerteEntity existing = findExisting(server);
        if (existing != null) {
            existing.teleportTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
            existing.getNavigation().stop();
            if (player != null) {
                existing.setOwnerUUID(player.getUUID());
            }
            existing.startEmerge();
            serverLevel.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.8F, 1.0F);
            if (player != null) {
                player.displayClientMessage(Component.literal("Verte \u0443\u0436\u0435 \u0437\u0434\u0435\u0441\u044c \u2014 \u043e\u043d \u043e\u0434\u0438\u043d \u043d\u0430 \u0432\u0435\u0441\u044c \u043c\u0438\u0440."), true);
            }
            return InteractionResult.CONSUME;
        }

        VerteEntity verte = ModEntities.VERTE.get().create(serverLevel);
        if (verte == null) {
            return InteractionResult.PASS;
        }

        verte.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, context.getRotation(), 0.0F);
        if (player != null) {
            verte.setOwnerUUID(player.getUUID());
        }
        verte.startEmerge();
        serverLevel.addFreshEntity(verte);
        serverLevel.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.8F, 1.0F);

        context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

    private static VerteEntity findExisting(MinecraftServer server) {
        if (server == null) {
            return null;
        }
        for (ServerLevel sl : server.getAllLevels()) {
            for (Entity e : sl.getAllEntities()) {
                if (e instanceof VerteEntity verte && verte.isAlive()) {
                    return verte;
                }
            }
        }
        return null;
    }
}
