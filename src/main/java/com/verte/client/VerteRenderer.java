package com.verte.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.verte.entity.VerteEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Verte looks like a normal player (vanilla wide \"Steve\" skin). Uses the humanoid
 * mob renderer so the held sword (via the built-in item layer) and the armor
 * layer below render his full netherite kit.
 */
public class VerteRenderer extends HumanoidMobRenderer<VerteEntity, VerteModel> {

    private static final ResourceLocation STEVE =
            new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");

    public VerteRenderer(EntityRendererProvider.Context context) {
        super(context, new VerteModel(context.bakeLayer(VerteModel.LAYER)), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
    }

    @Override
    protected void scale(VerteEntity entity, PoseStack poseStack, float partialTickTime) {
        if (entity.isBig()) {
            poseStack.scale(1.4F, 5.0F, 1.4F);
        }
        super.scale(entity, poseStack, partialTickTime);
    }

    @Override
    public ResourceLocation getTextureLocation(VerteEntity entity) {
        return STEVE;
    }
}
