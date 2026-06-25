package com.verte;

import com.verte.entity.ModEntities;
import com.verte.entity.VerteEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Right-click a block with the box and Verte climbs out of it, behaving like
 * another player. Sneak-interacting Verte puts him back into a box (handled in
 * {@link VerteEntity#mobInteract}).
 */
public class VerteBoxItem extends Item {

    public VerteBoxItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        VerteEntity verte = ModEntities.VERTE.get().create(serverLevel);
        if (verte == null) {
            return InteractionResult.PASS;
        }

        verte.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, context.getRotation(), 0.0F);
        Player player = context.getPlayer();
        if (player != null) {
            verte.setOwnerUUID(player.getUUID());
        }
        verte.startEmerge();
        serverLevel.addFreshEntity(verte);
        serverLevel.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.8F, 1.0F);

        context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
