package com.zythera.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.zythera.client.LoadingTimeline;
import com.zythera.client.TextureLayout;
import com.zythera.client.SplashLayout;
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
            zythera$artReady = zythera$loadTexture(ART, "/assets/zythera/textures/gui/purple-logo.png", true);
            zythera$backgroundReady = zythera$loadTexture(BACKGROUND, "/assets/zythera/textures/gui/purple-background.png", true);
            zythera$fontReady = zythera$loadTexture(FONT, "/assets/zythera/textures/gui/loading-font-proportional.png", true);
        }
        // Draw directly in framebuffer pixels, avoiding any scaled intermediate render target.
        // Convert framebuffer coordinates to the GUI projection, accounting for rounded GUI dimensions.
        SplashLayout layout = SplashLayout.forSize(width, height);
        zythera$pixelUnit = layout.unit();
        float u = layout.unit();
        ctx.getMatrices().push();
        ctx.getMatrices().scale(guiWidth / (float)width, guiHeight / (float)height, 1);
        ctx.fill(0, 0, width, height, 0xff080512);
        if (zythera$backgroundReady) {
            TextureLayout.Placement bg = TextureLayout.cover(1376, 768, width, height);
            zythera$drawImage(ctx, BACKGROUND, bg.x(), bg.y(), bg.scale(), 1376, 768);
        }
        // FPS is centred rather than in the top-right corner, where launcher mouse
        // controls commonly sit. External launcher overlays cannot be repositioned by a mod.
        zythera$center(ctx, "FPS: " + client.getCurrentFps(), width/2,
            Math.round(height*.025f), Math.round(width*.35f), 0xffffff);
        int maxLogoWidth = Math.max(1, Math.round(width*.84f));
        if (zythera$artReady) {
            var logo = TextureLayout.contain(970, 425, maxLogoWidth, layout.logoHeight());
            zythera$drawImage(ctx, ART, (width-970*logo.scale())/2,
                layout.logoTop()+(layout.logoHeight()-425*logo.scale())/2, logo.scale(), 970, 425);
        } else {
            zythera$center(ctx, "ZYTHERA CLIENT", width/2, layout.logoTop()+layout.logoHeight()/2, maxLogoWidth, 0xe1a2ff);
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
        String status = visualPercent == 100 ? "Loading Complete!" : "Starting client...";
        zythera$center(ctx, status, width/2, layout.statusY(), Math.round(width*.8f), 0xffffff);
        int x = (width-layout.barWidth())/2, y = layout.barY();
        int barW = layout.barWidth(), barH = layout.barHeight();
        int edge = Math.max(1, Math.round(u));
        // One subtle halo, then a thin track and fill. No shader or offscreen target.
        ctx.fill(x-edge, y-edge, x+barW+edge, y+barH+edge, 0x703d0065);
        ctx.fill(x, y, x+barW, y+barH, 0xdd130c1b);
        int fill = Math.round(barW * visualPercent / 100f);
        if (fill > 0) {
            ctx.fill(x, y, x+fill, y+barH, 0xffad32e5);
            ctx.fill(x, y, x+fill, y+Math.max(1, barH/3), 0xffdb82ff);
        }
        zythera$center(ctx, visualPercent+"% (animation)", width/2, layout.percentY(), Math.round(width*.8f), 0xffffff);
        int real = MathHelper.clamp(Math.round(reload.getProgress()*100), 0, 100);
        zythera$center(ctx, "Resources: "+real+"%", width/2, layout.resourceY(), Math.round(width*.8f), 0xe5c8ff);
        String tip = TIPS[(int)((Util.getMeasuringTimeMs()/7000)%TIPS.length)];
        zythera$center(ctx, tip, width/2, layout.tipY(), Math.round(width*.85f), 0xffffff);
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
    // An embedded proportional ASCII atlas is available during early resource reload.
    // All overlay labels are fixed ASCII; normal Minecraft screens use Minecraft's font.
    @Unique private static final int[] zythera$advances = {
        10,13,15,27,20,30,25,9,12,12,16,27,10,12,10,11,20,20,20,20,20,20,20,20,20,20,11,11,27,27,27,17,
        32,22,22,22,25,20,18,25,24,9,9,21,18,28,24,25,19,25,22,20,20,23,22,32,22,20,22,12,11,12,27,16,
        16,20,20,18,20,20,11,20,20,9,9,19,9,31,20,20,20,20,13,17,13,20,19,26,19,19,17,20,11,20,27
    };
    @Unique private int zythera$index(char c) { return (c >= 32 && c <= 126 ? c : '?')-32; }
    @Unique private void zythera$center(DrawContext ctx, String text, int centerX, int y, int maxWidth, int rgb) {
        if (!zythera$fontReady) {
            if (client.textRenderer != null) {
                float scale = Math.min(zythera$pixelUnit, maxWidth/(float)Math.max(1, client.textRenderer.getWidth(text)));
                ctx.getMatrices().push();
                ctx.getMatrices().translate(centerX, y, 0);
                ctx.getMatrices().scale(scale, scale, 1);
                ctx.drawCenteredTextWithShadow(client.textRenderer, text, 0, 0, 0xff000000 | rgb);
                ctx.getMatrices().pop();
            }
            return;
        }
        int width = 0;
        for (int i=0; i<text.length(); i++) width += zythera$advances[zythera$index(text.charAt(i))];
        float scale = Math.min(12f*zythera$pixelUnit/48f, maxWidth/(float)Math.max(1, width));
        ctx.getMatrices().push();
        ctx.getMatrices().translate(Math.round(centerX-width*scale/2), y, 0);
        ctx.getMatrices().scale(scale, scale, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        zythera$glyphs(ctx, text, 2, 2, 0x09040f);
        zythera$glyphs(ctx, text, 0, 0, rgb);
        RenderSystem.setShaderColor(1,1,1,1);
        ctx.getMatrices().pop();
    }
    @Unique private void zythera$glyphs(DrawContext ctx, String text, int ox, int oy, int rgb) {
        RenderSystem.setShaderColor(((rgb>>16)&255)/255f, ((rgb>>8)&255)/255f, (rgb&255)/255f, 1);
        int x=ox;
        for (int i=0; i<text.length(); i++) {
            int index = zythera$index(text.charAt(i));
            ctx.drawTexture(FONT, x, oy, (index%16)*48, (index/16)*48, 48, 48, 768, 288);
            x += zythera$advances[index];
        }
    }
}
