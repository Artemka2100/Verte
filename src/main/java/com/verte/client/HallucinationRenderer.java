package com.verte.client;

import com.verte.Verte;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Draws short, fading full-screen hallucinations triggered by the server:
 *  - type 0: distortion (dark pulsing veil; paired with real nausea)
 *  - type 1: blood (red wash with a heavy vignette)
 *  - type 2: a looming, tall, thin huge-Verte silhouette with red eyes
 * Each lasts about 5 seconds and fades in/out. Client-only.
 */
@Mod.EventBusSubscriber(modid = Verte.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HallucinationRenderer {

    private static long startedAt;
    private static long endAt;
    private static long totalMs;
    private static int type;

    private HallucinationRenderer() {
    }

    public static void start(int hallucinationType, int durationTicks) {
        type = hallucinationType;
        totalMs = Math.max(1L, durationTicks * 50L);
        startedAt = System.currentTimeMillis();
        endAt = startedAt + totalMs;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        long now = System.currentTimeMillis();
        if (now >= endAt) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        float life = (now - startedAt) / (float) totalMs; // 0..1
        float fade;
        if (life < 0.2F) {
            fade = life / 0.2F;
        } else if (life > 0.8F) {
            fade = (1.0F - life) / 0.2F;
        } else {
            fade = 1.0F;
        }
        fade = Mth.clamp(fade, 0.0F, 1.0F);

        switch (type) {
            case 1 -> drawBlood(g, w, h, fade);
            case 2 -> drawHugeVerte(g, w, h, fade, now);
            default -> drawDistortion(g, w, h, fade, now);
        }
    }

    private static int argb(int a, int r, int gg, int b) {
        return (Mth.clamp(a, 0, 255) << 24) | (r << 16) | (gg << 8) | b;
    }

    private static void drawDistortion(GuiGraphics g, int w, int h, float fade, long now) {
        float pulse = 0.5F + 0.5F * Mth.sin(now / 120.0F);
        int alpha = (int) ((35.0F + 35.0F * pulse) * fade);
        g.fill(0, 0, w, h, argb(alpha, 40, 0, 60));
        int edge = (int) (120.0F * fade);
        int band = Math.max(8, h / 8);
        g.fillGradient(0, 0, w, band, argb(edge, 0, 0, 0), argb(0, 0, 0, 0));
        g.fillGradient(0, h - band, w, h, argb(0, 0, 0, 0), argb(edge, 0, 0, 0));
    }

    private static void drawBlood(GuiGraphics g, int w, int h, float fade) {
        int alpha = (int) (110.0F * fade);
        g.fill(0, 0, w, h, argb(alpha, 110, 0, 0));
        int edge = (int) (180.0F * fade);
        int band = Math.max(10, w / 6);
        g.fillGradient(0, 0, band, h, argb(edge, 50, 0, 0), argb(0, 0, 0, 0));
        g.fillGradient(w - band, 0, w, h, argb(0, 0, 0, 0), argb(edge, 50, 0, 0));
        int vband = Math.max(10, h / 6);
        g.fillGradient(0, 0, w, vband, argb(edge, 50, 0, 0), argb(0, 0, 0, 0));
        g.fillGradient(0, h - vband, w, h, argb(0, 0, 0, 0), argb(edge, 50, 0, 0));
    }

    private static void drawHugeVerte(GuiGraphics g, int w, int h, float fade, long now) {
        g.fill(0, 0, w, h, argb((int) (120.0F * fade), 0, 0, 0));

        int sway = (int) (Mth.sin(now / 350.0F) * (w * 0.02F));
        int cx = w / 2 + sway;
        int figW = Math.max(10, w / 8);
        int figH = (int) (h * 0.95F);
        int top = h - figH;
        int bodyTop = top + figH / 5;
        int alpha = (int) (235.0F * fade);
        int black = argb(alpha, 4, 4, 6);

        g.fill(cx - figW / 2, bodyTop, cx + figW / 2, h, black);
        int headW = Math.max(8, figW * 2 / 3);
        g.fill(cx - headW / 2, top, cx + headW / 2, bodyTop, black);
        int armW = Math.max(3, figW / 5);
        g.fill(cx - figW / 2 - armW, bodyTop, cx - figW / 2, h, black);
        g.fill(cx + figW / 2, bodyTop, cx + figW / 2 + armW, h, black);
        int eyeAlpha = (int) (255.0F * fade);
        int red = argb(eyeAlpha, 200, 0, 0);
        int eyeW = Math.max(2, headW / 6);
        int eyeY = top + (bodyTop - top) / 2 - eyeW / 2;
        g.fill(cx - headW / 4 - eyeW / 2, eyeY, cx - headW / 4 + eyeW / 2, eyeY + eyeW, red);
        g.fill(cx + headW / 4 - eyeW / 2, eyeY, cx + headW / 4 + eyeW / 2, eyeY + eyeW, red);
    }
}
