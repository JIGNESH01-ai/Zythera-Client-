package com.zythera.client.mixin;

import net.minecraft.client.gui.DrawContext;
import com.zythera.client.PerformanceSettings;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Retains vanilla's functional title buttons and navigation, replacing its visual render. */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    private static final Identifier ART = new Identifier("zythera", "textures/gui/loading-art.png");
    protected TitleScreenMixin(Text title) { super(title); }
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void zythera$mainMenu(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ctx.fill(0, 0, width, height, 0xff090611);
        for (int i = 0; i < 8; i++) {
            int x = i * width / 7;
            int h = height / 5 + (i * 47 % Math.max(1, height / 3));
            ctx.fill(x, height - h, x + Math.max(4, width / 22), height, 0xff21132e);
        }
        int artWidth = Math.min(width - 16, Math.min(460, height));
        int artHeight = artWidth * 417 / 1024;
        ctx.getMatrices().push();
        ctx.getMatrices().translate((width - artWidth) / 2.0, 2, 0);
        ctx.getMatrices().scale(artWidth / 1024f, artHeight / 417f, 1);
        ctx.drawTexture(ART, 0, 0, 0, 0, 1024, 417, 1024, 417);
        ctx.getMatrices().pop();
        ctx.fill(0, height - 2, width, height, 0xffdba923);
        // Screen renders the actual vanilla buttons, including the Zythera menu button.
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawTextWithShadow(textRenderer, "Made by JIGNESH_GAMING-k7j", 6, height - 23, 0xffffd166);
        ctx.drawTextWithShadow(textRenderer, "Subscribe on YouTube", 6, height - 12, 0xffffffff);
        // Opt-in CI smoke test: reaching here proves the title screen rendered.
        String smoke = System.getenv("ZYTHERA_SMOKE_TEST");
        if ("restart".equals(smoke) || "custom".equals(smoke)) {
            int expected = "restart".equals(smoke) ? 2 : 3;
            if (client.options.getViewDistance().getValue() != expected || client.options.getSimulationDistance().getValue() != expected)
                throw new IllegalStateException("Restart option check failed: " + client.options.getViewDistance().getValue() + "/" + client.options.getSimulationDistance().getValue());
            if ("restart".equals(smoke)) {
                client.options.getViewDistance().setValue(3);
                client.options.getSimulationDistance().setValue(3);
                client.options.write();
            }
            System.out.println("[Zythera] SMOKE_RESTART_OK " + smoke);
            client.scheduleStop();
        } else if ("1".equals(smoke)) {
            PerformanceSettings.apply(client, "Potato");
            if (client.options.getViewDistance().getValue() != 2 || client.options.getSimulationDistance().getValue() != 2)
                throw new IllegalStateException("Potato chunk settings rejected: " + client.options.getViewDistance().getValue() + "/" + client.options.getSimulationDistance().getValue());
            PerformanceSettings.apply(client, "Balanced");
            if (client.options.getViewDistance().getValue() != 6 || client.options.getSimulationDistance().getValue() != 4)
                throw new IllegalStateException("Balanced chunk settings rejected");
            PerformanceSettings.apply(client, "Smooth FPS");
            if (client.options.getMaxFps().getValue() != 60) throw new IllegalStateException("Smooth FPS setting rejected");
            PerformanceSettings.apply(client, "Potato");
            System.out.println("[Zythera] SMOKE_PROFILES_OK");
            System.out.println("[Zythera] SMOKE_TITLE_RENDERED");
            client.scheduleStop();
        }
        ci.cancel();
    }
}
