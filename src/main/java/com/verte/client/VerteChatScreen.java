package com.verte.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class VerteChatScreen extends Screen {
    private EditBox input;

    public VerteChatScreen() {
        super(Component.literal("Verte"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        this.input = new EditBox(this.font, cx - 150, cy - 10, 300, 20, Component.literal("Verte"));
        this.input.setMaxLength(256);
        this.input.setHint(Component.literal("\u0421\u043a\u0430\u0436\u0438 Verte..."));
        this.addRenderableWidget(this.input);
        this.setInitialFocus(this.input);

        this.addRenderableWidget(Button.builder(Component.literal("\u041e\u0442\u043f\u0440\u0430\u0432\u0438\u0442\u044c"), b -> send())
                .bounds(cx - 50, cy + 20, 100, 20).build());
    }

    private void send() {
        String text = this.input.getValue().trim();
        if (!text.isEmpty() && this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.connection.sendCommand("verte " + text);
        }
        this.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            send();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, "\u0427\u0442\u043e \u0441\u043a\u0430\u0436\u0435\u0448\u044c Verte?", this.width / 2, this.height / 2 - 35, 0x55FF55);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
