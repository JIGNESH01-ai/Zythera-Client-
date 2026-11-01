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
        "Tip: Open Zythera Mods to customize the HUD.",
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
            zythera$artReady = zythera$loadTexture(ART, "/assets/zythera/textures/gui/loading-logo-hd.png", true);
            zythera$backgroundReady = zythera$loadTexture(BACKGROUND, "/assets/zythera/textures/gui/loading-background-hd.png", true);
            zythera$fontReady = zythera$loadTexture(FONT, "/assets/zythera/textures/gui/loading-font-hd.png", true);
        }
        // Draw directly in framebuffer pixels, avoiding any scaled intermediate render target.
        // Convert framebuffer coordinates to the GUI projection, accounting for rounded GUI dimensions.
        zythera$pixelUnit = Math.min(width / (float) guiWidth, height / (float) guiHeight);
        float u = zythera$pixelUnit;
        ctx.getMatrices().push();
        ctx.getMatrices().scale(guiWidth / (float)width, guiHeight / (float)height, 1);
        ctx.fill(0, 0, width, height, 0xff09070b);
        if (zythera$backgroundReady) {
            TextureLayout.Placement bg = TextureLayout.cover(1000, 775, width, height);
            zythera$drawImage(ctx, BACKGROUND, bg.x(), bg.y(), bg.scale(), 1000, 775);
            ctx.fill(0, 0, width, height, 0x99000000);
        } else {
            for (int i = 0; i < 8; i++) {
                int x = i * width / 7;
                int h = height / 5 + (i * 47 % Math.max(1, height / 4));
                ctx.fill(x, height - h, x + Math.max(2, width / 28), height, 0xff1b1024);
            }
        }
        if (guiWidth >= 200) zythera$center(ctx, "FPS: " + client.getCurrentFps(), (int)(38*u), (int)(3*u), (int)(70*u), 0xffd166);
        int margin = Math.round(8*u);
        int footerReserve = Math.round((guiHeight >= 170 ? 68 : guiHeight >= 145 ? 57 : guiHeight >= 125 ? 39 : 28) * u);
        int barLimit = height - footerReserve;
        int artY = Math.round(16*u);
        int maxArtHeight = Math.max(0, barLimit - artY - Math.round(17*u));
        int maxArtWidth = Math.max(1, Math.min(width - 2*margin, Math.round(640*u)));
        int artHeight;
        if (zythera$artReady && maxArtHeight >= 12*u) {
            var logo = TextureLayout.contain(1195, 485, maxArtWidth, maxArtHeight);
            artHeight = Math.max(1, (int)Math.ceil(485 * logo.scale()));
            double x = (width - 1195 * logo.scale()) / 2.0;
            zythera$drawImage(ctx, ART, x, artY, logo.scale(), 1195, 485);
        } else {
            artHeight = Math.round(12*u);
            zythera$center(ctx, "ZYTHERA CLIENT", width / 2, artY, width - 2*margin, 0xffd166);
        }
        if (!zythera$smokeLogged && "1".equals(System.getenv("ZYTHERA_SMOKE_TEST"))) {
            zythera$smokeLogged = true;
            System.out.println("[Zythera] SMOKE_SPLASH_RENDERED artwork=" + zythera$artReady + " font=" + zythera$fontReady + " background=" + zythera$backgroundReady);
        }
        int barY = Math.min(barLimit, artY + artHeight + Math.round(17*u));
        int barWidth = Math.max(Math.round(8*u), Math.min(width - Math.round(24*u), Math.round(410*u)));
        int x = (width - barWidth) / 2;
        int visualPercent = Math.min(100, Math.max(1, (int)Math.floor(zythera$shown)));
        if (visualPercent == 100 && !zythera$completionLogged && "1".equals(System.getenv("ZYTHERA_SMOKE_TEST"))) {
            zythera$completionLogged = true;
            System.out.println("[Zythera] SMOKE_ANIMATION_COMPLETE");
        }
        String status = visualPercent == 100 ? "Loading Complete!" : "Visual startup animation";
        zythera$center(ctx, status, width / 2, barY - Math.round(13*u), width - 2*margin, 0xffffff);
        int border = Math.max(1, Math.round(u)), inset = 2*border;
        ctx.fill(x, barY, x + barWidth, barY + Math.round(8*u), 0xffffd148);
        ctx.fill(x + border, barY + border, x + barWidth - border, barY + Math.round(7*u), 0xff170f0b);
        int fill = (int)Math.round((barWidth - 2*inset) * visualPercent / 100.0);
        if (fill > 0) ctx.fill(x + inset, barY + inset, x + inset + fill, barY + Math.round(6*u), 0xffffc42a);
        zythera$center(ctx, visualPercent + "%", width / 2, barY + Math.round(11*u), width - 2*margin, 0xffffff);
        int real = MathHelper.clamp(Math.round(reload.getProgress() * 100), 0, 100);
        if (guiHeight >= 125) zythera$center(ctx, "Resources: " + real + "%", width / 2, barY + Math.round(22*u), width - 2*margin, 0xffd166);
        if (guiHeight >= 190 && barY + Math.round(44*u) < height - Math.round(38*u)) {
            String tip = TIPS[(int)((Util.getMeasuringTimeMs() / 7000) % TIPS.length)];
            zythera$center(ctx, tip, width / 2, barY + Math.round(35*u), width - 2*margin, 0xffd166);
        }
        if (guiHeight >= 145) {
            if (guiHeight >= 170) zythera$center(ctx, "Optimized for Low End Devices", width / 2, height - Math.round(33*u), width - 2*margin, 0xffd166);
            zythera$center(ctx, "Made by JIGNESH_GAMING-k7j", width / 2, height - Math.round(22*u), width - 2*margin, 0xffffff);
            zythera$center(ctx, "Subscribe on YouTube", width / 2, height - Math.round(11*u), width - 2*margin, 0xffd166);
        }
        ctx.getMatrices().pop();
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
