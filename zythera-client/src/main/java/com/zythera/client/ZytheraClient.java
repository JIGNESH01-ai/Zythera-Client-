package com.zythera.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class ZytheraClient implements ClientModInitializer {
    public static Settings settings;
    private static KeyBinding menu, zoomKey;
    private static final java.util.ArrayDeque<Long> leftClicks = new java.util.ArrayDeque<>();
    public static void recordLeftClick() { if (settings != null && settings.cps) { clicksPerSecond(); leftClicks.addLast(System.nanoTime()); } }
    private static int clicksPerSecond() {
        long cutoff = System.nanoTime() - 1_000_000_000L;
        while (!leftClicks.isEmpty() && leftClicks.peekFirst() < cutoff) leftClicks.removeFirst();
        return leftClicks.size();
    }
    public static boolean isZooming() {
        MinecraftClient client = MinecraftClient.getInstance();
        return settings != null && settings.zoom && zoomKey != null && zoomKey.isPressed() && client.currentScreen == null && client.player != null;
    }
    @Override public void onInitializeClient() {
        settings = Settings.load();
        menu = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.zythera.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.zythera"));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.zythera.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.zythera"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menu.wasPressed()) client.setScreen(new ZytheraScreen(client.currentScreen));
        });
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof TitleScreen) && !(screen instanceof GameMenuScreen)) return;
            int x = 5, y = 5, w = 110, h = 20;
            Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("Zythera Mods"),
                button -> client.setScreen(new ZytheraScreen(screen))).dimensions(x, y, w, h).build());
            ScreenMouseEvents.allowMouseClick(screen).register((current, mouseX, mouseY, button) -> {
                if (button == 1 && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h) {
                    client.setScreen(new ZytheraScreen(current));
                    return false;
                }
                return true;
            });
            if (screen instanceof TitleScreen) ScreenEvents.afterRender(screen).register((current, context, mouseX, mouseY, tickDelta) -> {
                context.drawTextWithShadow(client.textRenderer, "ZYTHERA CLIENT", 8, 30, 0xFFFFD166);
                context.drawTextWithShadow(client.textRenderer, "Made by JIGNESH_GAMING-k7j", 8, 41, 0xFFFFD166);
            });
        });
        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && !client.options.hudHidden) renderHud(context, client);
        });
    }
    static void renderHud(DrawContext ctx, MinecraftClient client) {
        Settings s = settings;
        if (s.armor) {
            List<ItemStack> stacks = new ArrayList<>();
            for (int i = 3; i >= 0; i--) stacks.add(client.player.getInventory().getArmorStack(i));
            stacks.add(client.player.getOffHandStack());
            for (int i = 0; i < stacks.size(); i++) {
                ItemStack stack = stacks.get(i);
                int x = s.armorX + (s.armorVertical ? 0 : i * 85), y = s.armorY + (s.armorVertical ? i * 20 : 0);
                if (stack.isEmpty()) continue;
                if (s.armorIcons) { ctx.drawItem(stack, x, y); ctx.drawItemInSlot(client.textRenderer, stack, x, y); }
                if (s.armorDurability && stack.isDamageable()) {
                    int remaining = stack.getMaxDamage() - stack.getDamage();
                    String label = remaining + " (" + (remaining * 100 / stack.getMaxDamage()) + "%)";
                    ctx.drawTextWithShadow(client.textRenderer, label, x + (s.armorIcons ? 19 : 0), y + 4, s.color);
                }
            }
        }
        int y = s.textY;
        if (s.fps) { line(ctx, client, "FPS: " + client.getCurrentFps(), s.textX, y); y += 11; }
        if (s.coordinates) { line(ctx, client, String.format(java.util.Locale.ROOT, "XYZ: %.1f / %.1f / %.1f", client.player.getX(), client.player.getY(), client.player.getZ()), s.textX, y); y += 11; }
        if (s.direction) { line(ctx, client, "Direction: " + client.player.getHorizontalFacing().getName(), s.textX, y); y += 11; }
        if (s.memory) { Runtime r = Runtime.getRuntime(); line(ctx, client, "Memory: " + ((r.totalMemory() - r.freeMemory()) / 1048576) + " MB", s.textX, y); y += 11; }
        if (s.clock) { line(ctx, client, LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), s.textX, y); y += 11; }
        if (s.cps) { line(ctx, client, "CPS: " + clicksPerSecond(), s.textX, y); y += 11; }
        if (s.ping) {
            var handler = client.getNetworkHandler();
            var entry = handler == null ? null : handler.getPlayerListEntry(client.player.getUuid());
            line(ctx, client, "Ping: " + (client.isInSingleplayer() || entry == null ? "N/A" : entry.getLatency() + " ms"), s.textX, y); y += 11;
        }
        if (s.keystrokes) {
            line(ctx, client, "Keys: " + (client.options.forwardKey.isPressed() ? "W " : "- ")
                + (client.options.leftKey.isPressed() ? "A " : "- ")
                + (client.options.backKey.isPressed() ? "S " : "- ")
                + (client.options.rightKey.isPressed() ? "D" : "-"), s.textX, y);
        }
    }
    private static void line(DrawContext ctx, MinecraftClient client, String text, int x, int y) { ctx.drawTextWithShadow(client.textRenderer, text, x, y, settings.color); }
}
