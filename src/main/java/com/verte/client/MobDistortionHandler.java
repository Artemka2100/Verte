package com.verte.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.verte.Verte;
import com.verte.entity.VerteEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-only horror touch: in darkness, ordinary animals (cows, sheep, pigs...)
 * render stretched, swaying and twitching, so a harmless cow becomes something
 * wrong and unsettling. The darker it is, the stronger the distortion. Auto-
 * registers only on the physical client.
 */
@Mod.EventBusSubscriber(modid = Verte.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MobDistortionHandler {

    private MobDistortionHandler() {
    }

    @SubscribeEvent
    public static void onRenderPre(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Animal) || entity instanceof VerteEntity) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        int light = mc.level.getMaxLocalRawBrightness(entity.blockPosition());
        if (light > 4) {
            return;
        }

        float intensity = (5.0F - light) / 5.0F;
        float time = (float) mc.level.getGameTime() + event.getPartialTick();

        RandomSource rng = RandomSource.create(entity.getId() * 0x9E3779B97F4A7C15L);
        float warp = 0.6F + rng.nextFloat() * 0.9F;

        PoseStack pose = event.getPoseStack();
        // Stretch tall and thin so a familiar animal looks wrong.
        float stretchY = 1.0F + warp * intensity;
        float squeezeXZ = 1.0F - 0.25F * intensity;
        pose.scale(squeezeXZ, stretchY, squeezeXZ);
        // Slow unnatural sway plus a jittery head-tilt.
        float sway = Mth.sin(time * 0.05F + entity.getId()) * 0.18F * intensity;
        pose.mulPose(Axis.ZP.rotation(sway));
        float twitch = (rng.nextFloat() - 0.5F) * 0.12F * intensity;
        pose.mulPose(Axis.XP.rotation(twitch));
    }
}
