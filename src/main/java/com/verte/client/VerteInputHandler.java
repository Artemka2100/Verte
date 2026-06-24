package com.verte.client;

import com.verte.Verte;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Verte.MOD_ID, value = Dist.CLIENT)
public class VerteInputHandler {

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null) {
            return;
        }
        if (VerteKeybinds.TALK.consumeClick()) {
            mc.setScreen(new VerteChatScreen());
        }
    }
}
