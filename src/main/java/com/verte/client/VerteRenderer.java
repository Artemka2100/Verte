package com.verte.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.verte.entity.VerteEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class VerteRenderer extends MobRenderer<VerteEntity, VerteModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("minecraft", "textures/block/white_wool.png");

    public VerteRenderer(EntityRendererProvider.Context context) {
        super(context, new VerteModel(context.bakeLayer(VerteModel.LAYER)), 0.4F);
    }

    @Override
    protected void scale(VerteEntity entity, PoseStack poseStack, float partialTickTime) {
        float s = entity.isBig() ? 6.0F : 1.0F;
        poseStack.scale(s, s, s);
        super.scale(entity, poseStack, partialTickTime);
    }

    @Override
    public ResourceLocation getTextureLocation(VerteEntity entity) {
        return TEXTURE;
    }
}
