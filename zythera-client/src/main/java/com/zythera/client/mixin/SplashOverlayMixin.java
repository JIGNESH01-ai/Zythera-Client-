package com.zythera.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.io.InputStream;

/** Draws over vanilla after it has advanced its reload and fade lifecycle. */
@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {
    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private ResourceReload reload;
    @Shadow private long reloadCompleteTime;
    @Unique private static final Identifier ZYTHERA_ART = new Identifier("zythera", "loading-art-runtime");
    @Unique private static final String[] TIPS = {
        "Tip: Configure your HUD in Zythera Mods.",
        "Tip: Lower render distance to improve performance.",
        "Tip: Enjoy your gameplay with Zythera Client!"
    };
    @Unique private boolean zythera$textureAttempted;
    @Unique private boolean zythera$textureReady;

    @Inject(method = "render", at = @At("TAIL"))
    private void zythera$render(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // The overlay may outlive its reload for the vanilla two-second fade-out.
        float opacity = reloadCompleteTime < 0 ? 1.0f :
            1.0f - MathHelper.clamp((Util.getMeasuringTimeMs() - reloadCompleteTime - 1000L) / 1000.0f, 0.0f, 1.0f);
        if (opacity <= 0 || client.textRenderer == null) return;
        int width = ctx.getScaledWindowWidth(), height = ctx.getScaledWindowHeight();
        if (width < 1 || height < 1) return;
        int alpha = MathHelper.clamp((int)(opacity * 255), 0, 255);
        ctx.fill(0, 0, width, height, zythera$color(alpha, 0x090610));
        // Stylized End pillars remain visible when the image cannot be loaded.
        for (int i = 0; i < 9; i++) {
            int x = i * width / 8 - width / 20;
            int pillarHeight = height / 5 + (i * 73 % Math.max(1, height / 4));
            ctx.fill(x, height - pillarHeight, x + Math.max(3, width / 24), height, zythera$color(alpha, 0x1b1027));
        }
        if (!zythera$textureAttempted) {
            zythera$textureAttempted = true;
            // The normal resource manager is still reloading at startup. Load this one
            // small image directly from the mod JAR, then register a runtime texture.
            try (InputStream stream = SplashOverlayMixin.class.getResourceAsStream("/assets/zythera/textures/gui/loading-art.png")) {
                if (stream != null) {
                    client.getTextureManager().registerTexture(ZYTHERA_ART, new NativeImageBackedTexture(NativeImage.read(stream)));
                    zythera$textureReady = true;
                }
            } catch (IOException | RuntimeException e) {
                System.err.println("[Zythera] Could not load splash artwork: " + e.getMessage());
            }
        }
        int artWidth = Math.min(width - 12, Math.min(640, (int)(height * 1.35f)));
        int artHeight = artWidth * 417 / 1024;
        int artY = Math.max(20, (height * 10 / 100));
        if (zythera$textureReady && artWidth > 0) {
            ctx.getMatrices().push();
            ctx.getMatrices().translate((width - artWidth) / 2.0, artY, 0);
            ctx.getMatrices().scale(artWidth / 1024.0f, artHeight / 417.0f, 1);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1, 1, 1, opacity);
            ctx.drawTexture(ZYTHERA_ART, 0, 0, 0, 0, 1024, 417, 1024, 417);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            ctx.getMatrices().pop();
        } else {
            ctx.drawCenteredTextWithShadow(client.textRenderer, "ZYTHERA CLIENT", width / 2, artY + 15, zythera$color(alpha, 0xffd34e));
        }
        float progress = MathHelper.clamp(reload.getProgress(), 0.0f, 1.0f);
        int barWidth = Math.max(1, Math.min(width - 30, 410));
        int barX = (width - barWidth) / 2;
        int barY = Math.min(height - 55, artY + artHeight + Math.max(12, height / 20));
        int fill = Math.round((barWidth - 4) * progress);
        ctx.drawCenteredTextWithShadow(client.textRenderer, "Loading resources...", width / 2, barY - 15, zythera$color(alpha, 0xffffff));
        ctx.fill(barX, barY, barX + barWidth, barY + 9, zythera$color(alpha, 0xffd148));
        ctx.fill(barX + 1, barY + 1, barX + barWidth - 1, barY + 8, zythera$color(alpha, 0x170f0b));
        if (fill > 0) {
            ctx.fill(barX + 2, barY + 2, barX + 2 + fill, barY + 7, zythera$color(alpha, 0xffc42a));
            int shine = (int)((Util.getMeasuringTimeMs() / 14) % Math.max(1, fill));
            ctx.fill(barX + 2 + shine, barY + 2, barX + 2 + Math.min(fill, shine + 8), barY + 4, zythera$color(alpha, 0xffed8b));
        }
        ctx.drawCenteredTextWithShadow(client.textRenderer, Math.round(progress * 100) + "%", width / 2, barY + 13, zythera$color(alpha, 0xffffff));
        String tip = TIPS[(int)((Util.getMeasuringTimeMs() / 7000) % TIPS.length)];
        if (height > 170) ctx.drawCenteredTextWithShadow(client.textRenderer, tip, width / 2, barY + 27, zythera$color(alpha, 0xffd34e));
        ctx.drawTextWithShadow(client.textRenderer, "FPS: " + client.getCurrentFps(), 6, 6, zythera$color(alpha, 0xffd34e));
        if (height > 120) {
            ctx.drawCenteredTextWithShadow(client.textRenderer, "Made by JIGNESH_GAMING-k7j  |  Subscribe on YouTube", width / 2, height - 21, zythera$color(alpha, 0xffffff));
            ctx.drawCenteredTextWithShadow(client.textRenderer, "Optimized for Low End Devices", width / 2, height - 10, zythera$color(alpha, 0xffd34e));
        }
    }
    @Unique private static int zythera$color(int alpha, int rgb) { return (alpha << 24) | rgb; }
}
