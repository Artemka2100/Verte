package com.verte.client;

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
    public ResourceLocation getTextureLocation(VerteEntity entity) {
        return TEXTURE;
    }
}
