package com.zythera.client.mixin;

import net.minecraft.client.gui.DrawContext;
import com.zythera.client.PerformanceSettings;
import com.zythera.client.PurpleButton;
import com.zythera.client.AccountsInfoScreen;
import com.zythera.client.ZytheraScreen;
import com.zythera.client.TextureLayout;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.fabricmc.loader.api.FabricLoader;
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
    private static final Identifier ART = new Identifier("zythera", "textures/gui/purple-logo.png");
    private static final Identifier BG = new Identifier("zythera", "textures/gui/purple-background.png");
    protected TitleScreenMixin(Text title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void zythera$buttons(CallbackInfo ci) {
        clearChildren();
        int w = Math.max  (80, Math.min(270, width - 20));
        int h = Math.max(14, Math.min(22, (height - 80) / 6));
        int gap = Math.max(2, Math.min(5, height / 100));
        int y = Math.max(40, height - 4 * (h + gap) - h - 26);
        int x = (width - w) / 2;
        addDrawableChild(new PurpleButton(x, y, w, h, Text.literal("Singleplayer"), b -> client.setScreen(new SelectWorldScreen(this))));
        PurpleButton multiplayer = new PurpleButton(x, y+h+gap, w, h, Text.literal("Multiplayer"), b -> client.setScreen(new MultiplayerScreen(this)));
        multiplayer.active = client.isMultiplayerEnabled();
        addDrawableChild(multiplayer);
        addDrawableChild(new PurpleButton(x, y+2*(h+gap), w, h, Text.literal("Mods"), b -> client.setScreen(new ZytheraScreen(this))));
        addDrawableChild(new PurpleButton(x, y+3*(h+gap), w, h, Text.literal("Settings"), b -> client.setScreen(new OptionsScreen(this, client.options))));
        addDrawableChild(new PurpleButton(x, y+4*(h+gap), (w-gap)/2, h, Text.literal("Accounts"), b -> client.setScreen(new AccountsInfoScreen(this))));
        addDrawableChild(new PurpleButton(x+(w+gap)/2, y+4*(h+gap), (w-gap)/2, h, Text.literal("Quit Game"), b -> client.scheduleStop()));
    }
    private void zythera$image(DrawContext ctx, Identifier id, TextureLayout.Placement pos, int texW, int texH) {
        client.getTextureManager().getTexture(id).setFilter(true, false);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(pos.x(), pos.y(), 0);
        ctx.getMatrices().scale((float)pos.scale(), (float)pos.scale(), 1);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        ctx.drawTexture(id, 0, 0, 0, 0, texW, texH, texW, texH);
        ctx.getMatrices().pop();
    }
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void zythera$mainMenu(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        zythera$image(ctx, BG, TextureLayout.cover(1376, 768, width, height), 1376, 768);
        ctx.fill(0, 0, width, height, 0x44090016);
        int h = Math.max(14, Math.min(22, (height - 80) / 6));
        int gap = Math.max(2, Math.min(5, height / 100));
        int buttonsY = Math.max(40, height - 4 * (h + gap) - h - 26);
        TextureLayout.Placement logo = TextureLayout.contain(970, 425, Math.max(1, width - 24), Math.max(1, buttonsY - 20));
        zythera$image(ctx, ART, new TextureLayout.Placement((width - 970 * logo.scale()) / 2, 15, logo.scale()), 970, 425);
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawTextWithShadow(textRenderer, "FPS: " + client.getCurrentFps(), 6, 6, 0xffffffff);
        String version = "Zythera Client " + FabricLoader.getInstance().getModContainer("zythera").map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("1.20.1");
        ctx.drawTextWithShadow(textRenderer, version, Math.max(6, width - textRenderer.getWidth(version) - 6), 6, 0xffe3a1ff);
        if (height > 170 && width > 320) {
            ctx.drawTextWithShadow(textRenderer, "Minecraft 1.20.1", 6, height - 22, 0xffffffff);
            ctx.drawTextWithShadow(textRenderer, "Fabric " + FabricLoader.getInstance().getModContainer("fabricloader").map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse(""), 6, height - 11, 0xffffffff);
            String credit = "Made by JIGNESH_GAMING-k7j";
            ctx.drawTextWithShadow(textRenderer, credit, width - textRenderer.getWidth(credit) - 6, height - 11, 0xffe3a1ff);
        }
        // Opt-in CI smoke test: reaching here proves the title screen rendered.
        String smoke = client.getOverlay() == null ? System.getenv("ZYTHERA_SMOKE_TEST") : null;
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
