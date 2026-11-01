package com.zythera.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import java.net.URI;
import java.util.Locale;

/** Small, touch-friendly menu; only exposes controls backed by real behavior. */
public final class ZytheraScreen extends Screen {
    private final Screen parent;
    private String page = "Mods", query = "";
    private boolean draggingArmor, draggingText;
    private int modsPage, fpsPage;
    private int dragX, dragY;
    private TextFieldWidget search;
    private int left, top, panelWidth, panelHeight;
    public ZytheraScreen(Screen parent) { super(Text.literal("Zythera Client")); this.parent = parent; }
    @Override protected void init() { rebuild(); }
    private void rebuild() {
        clearChildren();
        panelWidth = Math.min(width - 16, 640); panelHeight = Math.min(height - 16, 360);
        left = (width - panelWidth) / 2; top = (height - panelHeight) / 2;
        int sidebar = Math.min(105, panelWidth / 3), content = left + sidebar + 12;
        String[] pages = {"Mods", "HUD Editor", "FPS Boost", "Settings", "About"};
        for (int i = 0; i < pages.length; i++) {
            String name = pages[i];
            addDrawableChild(ButtonWidget.builder(Text.literal(name), b -> { page = name; query = ""; rebuild(); })
                .dimensions(left + 7, top + 43 + i * 25, sidebar - 12, 20).build());
        }
        int w = Math.max(60, panelWidth - sidebar - 24);
        if (page.equals("Mods")) {
            search = new TextFieldWidget(textRenderer, content, top + 42, w, 20, Text.literal("Search modules"));
            search.setMaxLength(40); search.setText(query);
            search.setChangedListener(value -> { query = value; rebuild(); if (search != null) { setFocused(search); search.setCursorToEnd(); } });
            addDrawableChild(search);
            Settings s = ZytheraClient.settings;
            button(modsPage == 0 ? "More modules >" : "< Previous modules", content, top + 66, w, () -> { modsPage = 1 - modsPage; rebuild(); });
            int y = top + 90;
            if (modsPage == 0) {
            y = toggle("Armor HUD", s.armor, () -> s.armor = !s.armor, content, y, w);
            y = toggle("FPS Counter", s.fps, () -> s.fps = !s.fps, content, y, w);
            y = toggle("Coordinates", s.coordinates, () -> s.coordinates = !s.coordinates, content, y, w);
            y = toggle("Direction", s.direction, () -> s.direction = !s.direction, content, y, w);
            y = toggle("Memory", s.memory, () -> s.memory = !s.memory, content, y, w);
            toggle("Clock", s.clock, () -> s.clock = !s.clock, content, y, w);
            } else {
                y = toggle("CPS Counter", s.cps, () -> s.cps = !s.cps, content, y, w);
                y = toggle("Ping", s.ping, () -> s.ping = !s.ping, content, y, w);
                y = toggle("Keystrokes", s.keystrokes, () -> s.keystrokes = !s.keystrokes, content, y, w);
                toggle("Zoom (hold C)", s.zoom, () -> s.zoom = !s.zoom, content, y, w);
            }
        } else if (page.equals("HUD Editor")) {
            button("Drag HUD elements on screen", content, top + 50, w, () -> { page = "Drag"; rebuild(); });
            button("Reset positions", content, top + 75, w, () -> { Settings s = ZytheraClient.settings; s.armorX = 8; s.armorY = 8; s.textX = 8; s.textY = 100; s.save(); });
            Settings s = ZytheraClient.settings;
            button("Armor layout: " + (s.armorVertical ? "Vertical" : "Horizontal"), content, top + 100, w, () -> { s.armorVertical = !s.armorVertical; s.save(); rebuild(); });
            button("Armor durability: " + (s.armorDurability ? "ON" : "OFF"), content, top + 125, w, () -> { s.armorDurability = !s.armorDurability; s.save(); rebuild(); });
            button("Armor icons: " + (s.armorIcons ? "ON" : "OFF"), content, top + 150, w, () -> { s.armorIcons = !s.armorIcons; s.save(); rebuild(); });
        } else if (page.equals("FPS Boost")) {
            var o = client.options;
            button("Settings page " + (fpsPage + 1) + "/2  >", content, top + 42, w, () -> { fpsPage = 1 - fpsPage; rebuild(); });
            if (fpsPage == 0) {
                button("Profile: " + ZytheraClient.settings.performanceProfile + "  (change)", content, top + 66, w, () -> {
                    String p = ZytheraClient.settings.performanceProfile;
                    PerformanceSettings.apply(client, "Potato".equals(p) ? "Balanced" : "Balanced".equals(p) ? "Smooth FPS" : "Potato"); rebuild();
                });
                button("Render distance: " + o.getViewDistance().getValue() + " chunks", content, top + 90, w, () -> {
                    int value = o.getViewDistance().getValue(); o.getViewDistance().setValue(value >= 12 ? 2 : value + 1); saveOptions();
                });
                button("Simulation distance: " + o.getSimulationDistance().getValue() + " chunks", content, top + 114, w, () -> {
                    int value = o.getSimulationDistance().getValue(); o.getSimulationDistance().setValue(value >= 12 ? 2 : value + 1); saveOptions();
                });
                button("Graphics: " + o.getGraphicsMode().getValue(), content, top + 138, w, () -> {
                    o.getGraphicsMode().setValue(o.getGraphicsMode().getValue() == GraphicsMode.FAST ? GraphicsMode.FANCY : GraphicsMode.FAST); saveOptions();
                });
                button("Particles: " + o.getParticles().getValue(), content, top + 162, w, () -> {
                    ParticlesMode p = o.getParticles().getValue(); o.getParticles().setValue(p == ParticlesMode.MINIMAL ? ParticlesMode.DECREASED : p == ParticlesMode.DECREASED ? ParticlesMode.ALL : ParticlesMode.MINIMAL); saveOptions();
                });
                button("Entity distance: " + Math.round(o.getEntityDistanceScaling().getValue() * 100) + "%", content, top + 186, w, () -> {
                    double value = o.getEntityDistanceScaling().getValue(); o.getEntityDistanceScaling().setValue(value >= 1.0 ? 0.5 : value + 0.25); saveOptions();
                });
            } else {
                button("VSync: " + (o.getEnableVsync().getValue() ? "ON" : "OFF"), content, top + 66, w, () -> { o.getEnableVsync().setValue(!o.getEnableVsync().getValue()); saveOptions(); });
                button("Clouds: " + o.getCloudRenderMode().getValue(), content, top + 90, w, () -> { o.getCloudRenderMode().setValue(o.getCloudRenderMode().getValue() == CloudRenderMode.OFF ? CloudRenderMode.FAST : CloudRenderMode.OFF); saveOptions(); });
                button("Smooth lighting: " + (o.getAo().getValue() ? "ON" : "OFF"), content, top + 114, w, () -> { o.getAo().setValue(!o.getAo().getValue()); saveOptions(); });
                button("FPS counter: " + (ZytheraClient.settings.fps ? "ON" : "OFF"), content, top + 138, w, () -> { ZytheraClient.settings.fps = !ZytheraClient.settings.fps; ZytheraClient.settings.save(); rebuild(); });
                button("FPS limit: " + o.getMaxFps().getValue(), content, top + 162, w, () -> { int v = o.getMaxFps().getValue(); o.getMaxFps().setValue(v == 30 ? 60 : v == 60 ? 120 : 30); saveOptions(); });
                button("Reset optimization settings (Potato)", content, top + 186, w, () -> { PerformanceSettings.apply(client, "Potato"); rebuild(); });
            }
        } else if (page.equals("Settings")) {
            TextFieldWidget url = new TextFieldWidget(textRenderer, content, top + 65, w, 20, Text.literal("YouTube URL"));
            url.setMaxLength(512); url.setText(ZytheraClient.settings.youtubeUrl == null ? "" : ZytheraClient.settings.youtubeUrl);
            url.setChangedListener(value -> { ZytheraClient.settings.youtubeUrl = value; ZytheraClient.settings.save(); });
            addDrawableChild(url);
            button("Subscribe on YouTube", content, top + 95, w, ZytheraScreen::openYoutube);
        } else if (page.equals("About")) {
            button("Subscribe on YouTube", content, top + 95, w, ZytheraScreen::openYoutube);
        }
    }
    private void saveOptions() {
        client.options.write();
        ZytheraClient.settings.performanceProfile = "Custom";
        ZytheraClient.settings.save();
        rebuild();
    }
    private static void openYoutube() {
        try {
            URI uri = URI.create(ZytheraClient.settings.youtubeUrl);
            String host = uri.getHost();
            if ("https".equalsIgnoreCase(uri.getScheme()) && host != null && (host.equalsIgnoreCase("youtube.com") || host.toLowerCase(Locale.ROOT).endsWith(".youtube.com") || host.equalsIgnoreCase("youtu.be"))) Util.getOperatingSystem().open(uri);
        } catch (IllegalArgumentException ignored) { }
    }
    private int toggle(String name, boolean value, Runnable action, int x, int y, int w) {
        if (!name.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))) return y;
        button(name + "  " + (value ? "ON" : "OFF"), x, y, w, () -> { action.run(); ZytheraClient.settings.save(); rebuild(); });
        return y + 24;
    }
    private void button(String label, int x, int y, int w, Runnable action) {
        addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> action.run()).dimensions(x, y, w, 20).build());
    }
    @Override public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);
        if (page.equals("Drag")) {
            Settings s = ZytheraClient.settings;
            ctx.fill(s.armorX, s.armorY, s.armorX + 90, s.armorY + 18, 0xAA554400);
            ctx.fill(s.textX, s.textY, s.textX + 90, s.textY + 18, 0xAA554400);
            ctx.drawTextWithShadow(textRenderer, "ARMOR (drag)", s.armorX + 2, s.armorY + 4, 0xFFFFD166);
            ctx.drawTextWithShadow(textRenderer, "TEXT (drag)", s.textX + 2, s.textY + 4, 0xFFFFD166);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Drag to reposition · ESC to save"), width / 2, height - 18, 0xFFFFD166);
            return;
        }
        ctx.fill(left, top, left + panelWidth, top + panelHeight, 0xEF11100D);
        ctx.fill(left, top, left + panelWidth, top + 2, 0xFFFFD166);
        ctx.drawTextWithShadow(textRenderer, "ZYTHERA CLIENT", left + 9, top + 12, 0xFFFFD166);
        ctx.drawTextWithShadow(textRenderer, "Made by JIGNESH_GAMING-k7j", left + 9, top + panelHeight - 15, 0xFFAAAAAA);
        int content = left + Math.min(105, panelWidth / 3) + 12;
        ctx.drawTextWithShadow(textRenderer, page.toUpperCase(Locale.ROOT), content, top + 16, 0xFFFFFFFF);
        if (page.equals("FPS Boost")) {
            Runtime runtime = Runtime.getRuntime();
            String memory = "Memory: " + (runtime.totalMemory() - runtime.freeMemory()) / 1048576 + " / " + runtime.maxMemory() / 1048576 + " MB";
            ctx.drawTextWithShadow(textRenderer, memory, content, top + 213, 0xFFFFD166);
        }
        if (page.equals("Settings")) ctx.drawTextWithShadow(textRenderer, "YouTube channel URL (https):", content, top + 50, 0xFFFFFFFF);
        if (page.equals("About")) {
            ctx.drawTextWithShadow(textRenderer, "WELCOME TO ZYTHERA CLIENT", content, top + 45, 0xFFFFD166);
            ctx.drawTextWithShadow(textRenderer, "Play Smooth • Play Better • With Zythera", content, top + 65, 0xFFFFFFFF);
        }
        super.render(ctx, mouseX, mouseY, delta);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (page.equals("Drag") && button == 0) {
            Settings s = ZytheraClient.settings;
            draggingArmor = x >= s.armorX && x < s.armorX + 90 && y >= s.armorY && y < s.armorY + 18;
            draggingText = !draggingArmor && x >= s.textX && x < s.textX + 90 && y >= s.textY && y < s.textY + 18;
            if (draggingArmor || draggingText) { dragX = (int)x - (draggingArmor ? s.armorX : s.textX); dragY = (int)y - (draggingArmor ? s.armorY : s.textY); return true; }
        }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        Settings s = ZytheraClient.settings;
        if (draggingArmor) { s.armorX = Math.max(0, Math.min(width - 20, (int)x - dragX)); s.armorY = Math.max(0, Math.min(height - 10, (int)y - dragY)); return true; }
        if (draggingText) { s.textX = Math.max(0, Math.min(width - 20, (int)x - dragX)); s.textY = Math.max(0, Math.min(height - 10, (int)y - dragY)); return true; }
        return super.mouseDragged(x, y, button, dx, dy);
    }
    @Override public boolean mouseReleased(double x, double y, int button) { if (draggingArmor || draggingText) { draggingArmor = draggingText = false; ZytheraClient.settings.save(); return true; } return super.mouseReleased(x, y, button); }
    @Override public void close() { client.options.write(); ZytheraClient.settings.save(); if (page.equals("Drag")) { page = "HUD Editor"; rebuild(); } else client.setScreen(parent); }
    @Override public boolean shouldPause() { return false; }
}
