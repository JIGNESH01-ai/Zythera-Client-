package com.zythera.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Lightweight custom button; vanilla input, narration and keyboard behavior remain intact. */
public class PurpleButton extends ButtonWidget {
    public PurpleButton(int x, int y, int width, int height, Text label, PressAction action) {
        super(x, y, width, height, label, action, DEFAULT_NARRATION_SUPPLIER);
    }
    @Override public void renderButton(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        int edge = isHovered() || isFocused() ? 0xffffa0ff : 0xffb83ef0;
        ctx.fill(x-1, y-1, x+w+1, y+h+1, 0x883d1067);
        ctx.fill(x, y, x+w, y+h, edge);
        ctx.fill(x+1, y+1, x+w-1, y+h-1, 0xe8090610);
        ctx.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(), x+w/2, y+(h-8)/2, active ? 0xffffffff : 0xff888888);
    }
}
