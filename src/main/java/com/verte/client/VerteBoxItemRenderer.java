package com.verte.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/**
 * Renders the Verte box item as a real vanilla chest. A normal chest cannot be
 * shown with a flat JSON model because vanilla draws chests with a dedicated
 * block-entity renderer; this reuses that exact renderer so the item looks like
 * an actual chest in the inventory, in hand and on the ground.
 */
public class VerteBoxItemRenderer extends BlockEntityWithoutLevelRenderer {

    private final ChestBlockEntity chest =
            new ChestBlockEntity(BlockPos.ZERO, Blocks.CHEST.defaultBlockState());

    public VerteBoxItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Minecraft.getInstance().getBlockEntityRenderDispatcher()
                .renderItem(chest, poseStack, buffer, packedLight, packedOverlay);
    }
}
