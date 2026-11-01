package com.zythera.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.zythera.client.LoadingTimeline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Overlay;
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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.io.IOException;
import java.io.InputStream;

/** Keeps vanilla's reload and error handling, replacing only its loading visuals. */
@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {
    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private ResourceReload reload;
    @Unique private static final Identifier ART = new Identifier("zythera", "loading-art-runtime");
    @Unique private static final Identifier FONT = new Identifier("zythera", "loading-font-runtime");
    @Unique private static final String[] TIPS = {
        "Tip: Open Zythera Mods to customize the HUD.",
        "Tip: Lower render distance for better performance.",
        "Tip: Enjoy your gameplay with Zythera Client!"
    };
    @Unique private LoadingTimeline zythera$timeline;
    @Unique private boolean zythera$textureAttempted, zythera$artReady, zythera$fontReady, zythera$smokeLogged, zythera$completionLogged;
    @Unique private double zythera$shown = 1;

    @Unique private boolean zythera$reloadSucceeded() {
        if (!reload.isComplete()) return false;
        try { reload.throwException(); return true; }
        catch (RuntimeException e) { return false; }
    }
    @Unique private boolean zythera$reloadFailed() {
        if (!reload.isComplete()) return false;
        try { reload.throwException(); return false; }
        catch (RuntimeException e) { return true; }
    }
    @Inject(method = "render", at = @At("HEAD"))
    private void zythera$advance(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        long now = Util.getMeasuringTimeMs();
        if (zythera$timeline == null) zythera$timeline = new LoadingTimeline(now);
        zythera$shown = zythera$timeline.update(now, zythera$reloadSucceeded());
    }
    /** Vanilla may finish first. Delay only overlay removal, not the reload, callback, or error handling. */
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;setOverlay(Lnet/minecraft/client/gui/screen/Overlay;)V"))
    private void zythera$finishWhenReady(MinecraftClient client, Overlay overlay) {
        if (overlay != null || zythera$reloadFailed() || (zythera$timeline != null && zythera$timeline.canClose(Util.getMeasuringTimeMs()))) {
            client.setOverlay(overlay);
        }
    }
    @Inject(method = "render", at = @At("TAIL"))
    private void zythera$render(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (zythera$reloadFailed() || zythera$timeline == null || zythera$timeline.canClose(Util.getMeasuringTimeMs())) return;
        int width = ctx.getScaledWindowWidth(), height = ctx.getScaledWindowHeight();
        if (width < 24 || height < 45) return;
        if (!zythera$textureAttempted) {
            zythera$textureAttempted = true;
            zythera$artReady = zythera$loadTexture(ART, "/assets/zythera/textures/gui/loading-art.png");
            zythera$fontReady = zythera$loadTexture(FONT, "/assets/zythera/textures/gui/loading-font.png");
        }
        ctx.fill(0, 0, width, height, 0xff09070b);
        for (int i = 0; i < 8; i++) {
            int x = i * width / 7;
            int h = height / 5 + (i * 47 % Math.max(1, height / 4));
            ctx.fill(x, height - h, x + Math.max(2, width / 28), height, 0xff1b1024);
        }
        if (width >= 200) zythera$center(ctx, "FPS: " + client.getCurrentFps(), 38, 3, 70, 0xffd166);
        int barLimit = height - (height >= 170 ? 68 : height >= 145 ? 57 : height >= 125 ? 39 : 28);
        int artY = 16;
        int maxArtHeight = Math.max(0, barLimit - artY - 17);
        int artWidth = Math.max(1, Math.min(width - 16, Math.min(640, maxArtHeight * 1024 / 417)));
        int artHeight = Math.max(1, artWidth * 417 / 1024);
        if (zythera$artReady && maxArtHeight >= 12) {
            ctx.getMatrices().push();
            ctx.getMatrices().translate((width - artWidth) / 2.0, artY, 0);
            ctx.getMatrices().scale(artWidth / 1024.0f, artHeight / 417.0f, 1);
            ctx.drawTexture(ART, 0, 0, 0, 0, 1024, 417, 1024, 417);
            ctx.getMatrices().pop();
        } else {
            artHeight = 12;
            zythera$center(ctx, "ZYTHERA CLIENT", width / 2, artY, width - 12, 0xffd166);
        }
        if (!zythera$smokeLogged && "1".equals(System.getenv("ZYTHERA_SMOKE_TEST"))) {
            zythera$smokeLogged = true;
            System.out.println("[Zythera] SMOKE_SPLASH_RENDERED artwork=" + zythera$artReady + " font=" + zythera$fontReady);
        }
        int barY = Math.min(barLimit, artY + artHeight + 17);
        int barWidth = Math.max(8, Math.min(width - 24, 410));
        int x = (width - barWidth) / 2;
        int visualPercent = Math.min(100, Math.max(1, (int)Math.floor(zythera$shown)));
        if (visualPercent == 100 && !zythera$completionLogged && "1".equals(System.getenv("ZYTHERA_SMOKE_TEST"))) {
            zythera$completionLogged = true;
            System.out.println("[Zythera] SMOKE_ANIMATION_COMPLETE");
        }
        String status = visualPercent == 100 ? "Loading Complete!" : "Visual startup animation";
        zythera$center(ctx, status, width / 2, barY - 13, width - 12, 0xffffff);
        ctx.fill(x, barY, x + barWidth, barY + 8, 0xffffd148);
        ctx.fill(x + 1, barY + 1, x + barWidth - 1, barY + 7, 0xff170f0b);
        int fill = (int)Math.round((barWidth - 4) * visualPercent / 100.0);
        if (fill > 0) ctx.fill(x + 2, barY + 2, x + 2 + fill, barY + 6, 0xffffc42a);
        zythera$center(ctx, visualPercent + "%", width / 2, barY + 11, width - 12, 0xffffff);
        // This is actual resource progress, separate from the explicitly labelled animation.
        int real = MathHelper.clamp(Math.round(reload.getProgress() * 100), 0, 100);
        if (height >= 125) zythera$center(ctx, "Resources: " + real + "%", width / 2, barY + 22, width - 12, 0xffd166);
        if (height >= 190 && barY + 44 < height - 27) {
            String tip = TIPS[(int)((Util.getMeasuringTimeMs() / 7000) % TIPS.length)];
            zythera$center(ctx, tip, width / 2, barY + 35, width - 12, 0xffd166);
        }
        if (height >= 145) {
            if (height >= 170) zythera$center(ctx, "Optimized for Low End Devices", width / 2, height - 33, width - 12, 0xffd166);
            zythera$center(ctx, "Made by JIGNESH_GAMING-k7j", width / 2, height - 22, width - 12, 0xffffff);
            zythera$center(ctx, "Subscribe on YouTube", width / 2, height - 11, width - 12, 0xffd166);
        }
    }
    @Unique private boolean zythera$loadTexture(Identifier id, String resource) {
        try (InputStream stream = SplashOverlayMixin.class.getResourceAsStream(resource)) {
            if (stream == null) return false;
            client.getTextureManager().registerTexture(id, new NativeImageBackedTexture(NativeImage.read(stream)));
            return true;
        } catch (IOException | RuntimeException e) {
            System.err.println("[Zythera] Could not load " + resource + ": " + e.getMessage());
            return false;
        }
    }
    /** ASCII-only bitmap glyphs are available even before Minecraft's font resources finish loading. */
    @Unique private void zythera$center(DrawContext ctx, String text, int centerX, int y, int maxWidth, int rgb) {
        if (!zythera$fontReady) {
            if (client.textRenderer != null) ctx.drawCenteredTextWithShadow(client.textRenderer, text, centerX, y, 0xff000000 | rgb);
            return;
        }
        float scale = Math.min(1.0f, Math.max(0.1f, maxWidth / (float)Math.max(1, text.length() * 8)));
        ctx.getMatrices().push();
        ctx.getMatrices().translate(centerX - text.length() * 8 * scale / 2.0, y, 0);
        ctx.getMatrices().scale(scale, scale, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, 1);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int index = (c >= 32 && c <= 126 ? c : '?') - 32;
            ctx.drawTexture(FONT, i * 8, 0, (index % 16) * 8, (index / 16) * 12, 8, 12, 128, 72);
        }
        RenderSystem.setShaderColor(1, 1, 1, 1);
        ctx.getMatrices().pop();
    }
}
