package com.verte.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.verte.entity.VerteEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class VerteRenderer extends MobRenderer<VerteEntity, VerteModel> {

    // Verte looks like a normal player: the vanilla wide "Steve" skin. The model's
    // UV layout matches the classic player skin, so this maps correctly. To use a
    // custom skin instead, drop a 64x64 PNG in
    // src/main/resources/assets/verte/textures/entity/verte.png and point this at
    // new ResourceLocation("verte", "textures/entity/verte.png").
    private static final ResourceLocation STEVE =
            new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");

    public VerteRenderer(EntityRendererProvider.Context context) {
        super(context, new VerteModel(context.bakeLayer(VerteModel.LAYER)), 0.4F);
    }

    @Override
    protected void scale(VerteEntity entity, PoseStack poseStack, float partialTickTime) {
        if (entity.isBig()) {
            // Huge phase: a tall, thin, unnaturally stretched figure \u2014 just unsettling.
            poseStack.scale(1.4F, 5.0F, 1.4F);
        }
        super.scale(entity, poseStack, partialTickTime);
    }

    @Override
    public ResourceLocation getTextureLocation(VerteEntity entity) {
        return STEVE;
    }
}
