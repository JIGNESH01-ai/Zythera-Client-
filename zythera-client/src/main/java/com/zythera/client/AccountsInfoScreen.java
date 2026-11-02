package com.zythera.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Account authentication is owned by the launcher, not the mod. */
public class AccountsInfoScreen extends Screen {
    private final Screen parent;
    public AccountsInfoScreen(Screen parent) { super(Text.literal("Accounts")); this.parent = parent; }
    @Override protected void init() {
        addDrawableChild(new PurpleButton(width/2-75, height/2+25, 150, 20, Text.literal("Back"), b -> close()));
    }
    @Override public void close() { client.setScreen(parent); }
    @Override public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width/2, height/2-24, 0xffffffff);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Manage accounts in your launcher."), width/2, height/2-5, 0xffffffff);
        super.render(ctx, mouseX, mouseY, delta);
    }
}
