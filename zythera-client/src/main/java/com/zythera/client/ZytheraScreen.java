package com.zythera.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import java.net.URI;
import java.util.Locale;

/** Small, touch-friendly menu; only exposes controls backed by real behavior. */
public final class ZytheraScreen extends Screen {
    private final Screen parent;
    private String page = "Mods", query = "";
    private boolean draggingArmor, draggingText;
    private int modsPage;
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
            button("Potato preset (4 chunks, minimal particles, 30 FPS)", content, top + 50, w, () -> {
                client.options.getViewDistance().setValue(4);
                client.options.getParticles().setValue(ParticlesMode.MINIMAL);
                client.options.getMaxFps().setValue(30);
                client.options.write();
            });
            button("Balanced preset (8 chunks, decreased particles, 60 FPS)", content, top + 75, w, () -> {
                client.options.getViewDistance().setValue(8);
                client.options.getParticles().setValue(ParticlesMode.DECREASED);
                client.options.getMaxFps().setValue(60);
                client.options.write();
            });
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
    @Override public void close() { ZytheraClient.settings.save(); if (page.equals("Drag")) { page = "HUD Editor"; rebuild(); } else client.setScreen(parent); }
    @Override public boolean shouldPause() { return false; }
}
