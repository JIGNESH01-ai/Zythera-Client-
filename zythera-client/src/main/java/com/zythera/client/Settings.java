package com.zythera.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Per-installation settings. The config folder can be isolated using launcher game directories. */
public final class Settings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("zythera-1.20.1.json");
    public boolean armor = true, fps = true, coordinates = true, direction = false, memory = false, clock = false;
    public boolean armorVertical = true, armorDurability = true, armorIcons = true;
    public int armorX = 8, armorY = 8, textX = 8, textY = 100;
    public int color = 0xFFFFD166;
    public String youtubeUrl = "";

    public static Settings load() {
        try (Reader reader = Files.newBufferedReader(FILE)) {
            Settings settings = GSON.fromJson(reader, Settings.class);
            return settings == null ? new Settings() : settings;
        } catch (Exception e) {
            if (Files.exists(FILE)) System.err.println("[Zythera] Could not read settings: " + e.getMessage());
            return new Settings();
        }
    }
    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) { GSON.toJson(this, writer); }
        } catch (Exception e) { System.err.println("[Zythera] Could not save settings: " + e.getMessage()); }
    }
}
