package com.zythera.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.zythera.client.LoadingTimeline;
import com.zythera.client.TextureLayout;
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
    @Unique private static final Identifier BACKGROUND = new Identifier("zythera", "loading-background-runtime");
    @Unique private static final Identifier FONT = new Identifier("zythera", "loading-font-runtime");
    @Unique private static final String[] TIPS = {
        "Tip: Use F3 to check your FPS and debug info!",
        "Tip: Lower render distance for better performance.",
        "Tip: Enjoy your gameplay with Zythera Client!"
    };
    @Unique private LoadingTimeline zythera$timeline;
    @Unique private boolean zythera$textureAttempted, zythera$artReady, zythera$fontReady, zythera$backgroundReady, zythera$smokeLogged, zythera$completionLogged;
    @Unique private double zythera$shown = 1;
    @Unique private float zythera$pixelUnit = 1;

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
        int guiWidth = ctx.getScaledWindowWidth(), guiHeight = ctx.getScaledWindowHeight();
        int width = client.getWindow().getFramebufferWidth(), height = client.getWindow().getFramebufferHeight();
        if (guiWidth < 24 || guiHeight < 45 || width <= 0 || height <= 0) return;
        if (!zythera$textureAttempted) {
            zythera$textureAttempted = true;
            zythera$artReady = zythera$loadTexture(ART, "/assets/zythera/textures/gui/splash-reference-logo.png", true);
            zythera$backgroundReady = zythera$loadTexture(BACKGROUND, "/assets/zythera/textures/gui/splash-village.png", true);
            zythera$fontReady = zythera$loadTexture(FONT, "/assets/zythera/textures/gui/loading-font-hd.png", true);
        }
        // Draw directly in framebuffer pixels, avoiding any scaled intermediate render target.
        // Convert framebuffer coordinates to the GUI projection, accounting for rounded GUI dimensions.
        zythera$pixelUnit = Math.min(Math.min(width / (float) guiWidth, height / (float) guiHeight), Math.min(width / 320f, height / 200f));
        float u = zythera$pixelUnit;
        ctx.getMatrices().push();
        ctx.getMatrices().scale(guiWidth / (float)width, guiHeight / (float)height, 1);
        ctx.fill(0, 0, width, height, 0xff080512);
        if (zythera$backgroundReady) {
            TextureLayout.Placement bg = TextureLayout.cover(1376, 768, width, height);
            zythera$drawImage(ctx, BACKGROUND, bg.x(), bg.y(), bg.scale(), 1376, 768);
            ctx.fill(0, 0, width, height, 0x22000000);
        } else {
            for (int i = 0; i < 8; i++) {
                int x = i * width / 7;
                int h = height / 5 + (i * 47 % Math.max(1, height / 4));
                ctx.fill(x, height - h, x + Math.max(2, width / 28), height, 0xff1b1024);
            }
        }
        int margin = Math.max(2, Math.round(7*u));
        int headerH = Math.round(16*u);
        int headW = Math.min(width/3, Math.round(135*u));
        zythera$panel(ctx, margin, margin, headW, headerH, u);
        zythera$center(ctx, "Zythera Client 0.5.4", margin+headW/2, margin+Math.round(2*u), headW-Math.round(4*u), 0xffffff);
        int fpsW = Math.min(width/4, Math.round(85*u));
        zythera$panel(ctx, width-margin-fpsW, margin, fpsW, headerH, u);
        zythera$center(ctx, "FPS: " + client.getCurrentFps(), width-margin-fpsW/2, margin+Math.round(2*u), fpsW-Math.round(4*u), 0xffffff);

        int statusY = Math.round(height * 0.59f);
        int statusH = Math.round(19*u);
        int logoY = margin + headerH;
        int maxLogoHeight = Math.max(1, statusY - logoY - Math.round(5*u));
        int maxLogoWidth = Math.max(1, Math.min(width - 2*margin, Math.round(650*u)));
        if (zythera$artReady) {
            var logo = TextureLayout.contain(1010, 445, maxLogoWidth, maxLogoHeight);
            double x = (width - 1010 * logo.scale()) / 2.0;
            zythera$drawImage(ctx, ART, x, logoY + (maxLogoHeight - 445 * logo.scale())/2, logo.scale(), 1010, 445);
        } else {
            zythera$center(ctx, "ZYTHERA CLIENT", width / 2, logoY + maxLogoHeight / 2, width - 2*margin, 0xe1a2ff);
        }
        if (!zythera$smokeLogged && "1".equals(System.getenv("ZYTHERA_SMOKE_TEST"))) {
            zythera$smokeLogged = true;
            System.out.println("[Zythera] SMOKE_SPLASH_RENDERED artwork=" + zythera$artReady + " font=" + zythera$fontReady + " background=" + zythera$backgroundReady);
        }
        int visualPercent = Math.min(100, Math.max(1, (int)Math.floor(zythera$shown)));
        if (visualPercent == 100 && !zythera$completionLogged && "1".equals(System.getenv("ZYTHERA_SMOKE_TEST"))) {
            zythera$completionLogged = true;
            System.out.println("[Zythera] SMOKE_ANIMATION_COMPLETE");
        }
        int statusW = Math.min(width - 2*margin, Math.round(320*u));
        zythera$panel(ctx, (width-statusW)/2, statusY, statusW, statusH, u);
        String status = visualPercent == 100 ? "Loading Complete!" : "Loading... Please Wait";
        zythera$center(ctx, status, width/2, statusY+Math.round(3*u), statusW-Math.round(6*u), 0xffffff);
        int barY = statusY + statusH + Math.round(5*u);
        int barWidth = Math.min(width - 2*margin, Math.round(410*u));
        int barHeight = Math.round(18*u);
        int x = (width - barWidth) / 2;
        zythera$panel(ctx, x, barY, barWidth, barHeight, u);
        int inset = Math.max(2, Math.round(2*u));
        int fill = (int)Math.round((barWidth - 2*inset) * visualPercent / 100.0);
        if (fill > 0) {
            ctx.fill(x+inset, barY+inset, x+inset+fill, barY+barHeight-inset, 0xffaa1ad6);
            ctx.fill(x+inset, barY+inset, x+inset+fill, barY+inset+Math.max(1, Math.round(3*u)), 0xffea78ff);
        }
        zythera$center(ctx, visualPercent + "%", width/2, barY+Math.round(3*u), barWidth-Math.round(4*u), 0xffffff);
        int real = MathHelper.clamp(Math.round(reload.getProgress() * 100), 0, 100);
        int infoY = barY + barHeight + Math.round(3*u);
        zythera$center(ctx, "Visual animation | Resources: " + real + "%", width/2, infoY, width-2*margin, 0xffffff);
        int footerY = height - margin - Math.round(17*u);
        int tipY = infoY + Math.round(16*u);
        if (tipY + Math.round(18*u) < footerY) {
            int tipW = Math.min(width-2*margin, Math.round(360*u));
            zythera$panel(ctx, (width-tipW)/2, tipY, tipW, Math.round(18*u), u);
            String tip = TIPS[(int)((Util.getMeasuringTimeMs() / 7000) % TIPS.length)];
            zythera$center(ctx, tip, width/2, tipY+Math.round(3*u), tipW-Math.round(6*u), 0xffffff);
        }
        if (footerY > infoY + Math.round(12*u)) {
            int footW = Math.min(width/2 - margin*2, Math.round(160*u));
            zythera$panel(ctx, margin, footerY, footW, Math.round(17*u), u);
            zythera$center(ctx, "Made by Zythera Team", margin+footW/2, footerY+Math.round(2*u), footW-Math.round(4*u), 0xffffff);
            zythera$panel(ctx, width-margin-footW, footerY, footW, Math.round(17*u), u);
            zythera$center(ctx, "discord.gg/Zythera", width-margin-footW/2, footerY+Math.round(2*u), footW-Math.round(4*u), 0xe1a2ff);
        }
        ctx.getMatrices().pop();
    }
    @Unique private void zythera$panel(DrawContext ctx, int x, int y, int w, int h, float u) {
        int border = Math.max(1, Math.round(u));
        ctx.fill(x-border, y-border, x+w+border, y+h+border, 0x7748006d);
        ctx.fill(x, y, x+w, y+h, 0xffbc59ec);
        ctx.fill(x+border, y+border, x+w-border, y+h-border, 0xe9080710);
    }
    @Unique private void zythera$drawImage(DrawContext ctx, Identifier id, double x, double y, double scale, int textureWidth, int textureHeight) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0);
        ctx.getMatrices().scale((float)scale, (float)scale, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        ctx.drawTexture(id, 0, 0, 0, 0, textureWidth, textureHeight, textureWidth, textureHeight);
        ctx.getMatrices().pop();
    }
    @Unique private boolean zythera$loadTexture(Identifier id, String resource, boolean bilinear) {
        try (InputStream stream = SplashOverlayMixin.class.getResourceAsStream(resource)) {
            if (stream == null) return false;
            NativeImageBackedTexture texture = new NativeImageBackedTexture(NativeImage.read(stream));
            client.getTextureManager().registerTexture(id, texture);
            texture.setFilter(bilinear, false);
            return true;
        } catch (IOException | RuntimeException e) {
            System.err.println("[Zythera] Could not load " + resource + ": " + e.getMessage());
            return false;
        }
    }
    /** ASCII-only bitmap glyphs are available even before Minecraft's font resources finish loading. */
    @Unique private void zythera$center(DrawContext ctx, String text, int centerX, int y, int maxWidth, int rgb) {
        if (!zythera$fontReady) {
            if (client.textRenderer != null) {
                float scale = Math.min(zythera$pixelUnit, maxWidth / (float)Math.max(1, client.textRenderer.getWidth(text)));
                ctx.getMatrices().push();
                ctx.getMatrices().translate(centerX, y, 0);
                ctx.getMatrices().scale(scale, scale, 1);
                ctx.drawCenteredTextWithShadow(client.textRenderer, text, 0, 0, 0xff000000 | rgb);
                ctx.getMatrices().pop();
            }
            return;
        }
        float scale = Math.min(12f * zythera$pixelUnit / 48f, maxWidth / (float)Math.max(1, text.length() * 28));
        ctx.getMatrices().push();
        ctx.getMatrices().translate(Math.round(centerX - text.length() * 28 * scale / 2.0), y, 0);
        ctx.getMatrices().scale(scale, scale, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, 1);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int index = (c >= 32 && c <= 126 ? c : '?') - 32;
            ctx.drawTexture(FONT, i * 28, 0, (index % 16) * 28, (index / 16) * 48, 28, 48, 448, 288);
        }
        RenderSystem.setShaderColor(1, 1, 1, 1);
        ctx.getMatrices().pop();
    }
}
