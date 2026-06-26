package com.verte.client;

import com.verte.Verte;
import com.verte.entity.VerteEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Verte uses the standard humanoid (player) shape so the vanilla Steve skin maps
 * correctly and so equipped armor + a held sword render through the normal
 * humanoid layers.
 */
public class VerteModel extends HumanoidModel<VerteEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(Verte.MOD_ID, "verte"), "main");

    public VerteModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        return LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 64);
    }

    @Override
    public void setupAnim(VerteEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        // Friendly wave from the emerge animation.
        if (entity.isWaving()) {
            this.rightArm.xRot = -2.1F;
            this.rightArm.yRot = 0.0F;
            this.rightArm.zRot = -0.5F + Mth.cos(ageInTicks * 0.6F) * 0.45F;
        }
    }
}
