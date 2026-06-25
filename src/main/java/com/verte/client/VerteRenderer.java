package com.verte.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.verte.entity.VerteEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class VerteRenderer extends MobRenderer<VerteEntity, VerteModel> {

    // Vanilla block textures used as "faces" per corruption phase so the entity
    // always renders even without custom assets. To use real custom faces, drop
    // PNGs in src/main/resources/assets/verte/textures/entity/ and point these
    // ResourceLocations at e.g. new ResourceLocation("verte", "textures/entity/face_0.png").
    private static final ResourceLocation[] FACES = {
            new ResourceLocation("minecraft", "textures/block/lime_wool.png"),
            new ResourceLocation("minecraft", "textures/block/green_wool.png"),
            new ResourceLocation("minecraft", "textures/block/green_concrete.png"),
            new ResourceLocation("minecraft", "textures/block/redstone_block.png")
    };

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
        int phase = entity.getPhase();
        if (phase < 0) phase = 0;
        if (phase >= FACES.length) phase = FACES.length - 1;
        return FACES[phase];
    }
}
