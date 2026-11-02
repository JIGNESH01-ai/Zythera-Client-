package com.zythera.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Shows actual Fabric mod archives in this instance, not Zythera's built-in features. */
public final class InstalledModsScreen extends Screen {
    private final Screen parent;
    private final List<Entry> mods = new ArrayList<>();
    private int page, perPage;
    private String error;
    private boolean scanned;
    private record Entry(String name, String id, String status, Identifier icon) {}
    public InstalledModsScreen(Screen parent) { super(Text.literal("Installed mods")); this.parent = parent; }

    @Override protected void init() {
        if (!scanned) { scanned = true; scan(); }
        perPage = Math.max(1, (height - 85) / 29);
        page = Math.min(page, Math.max(0, (mods.size()-1)/perPage));
        int buttonY = Math.max(0, height - 28);
        int gap = 4, buttonWidth = Math.min(100, Math.max(28, (width - 4*gap)/3));
        int x = (width - 3*buttonWidth - 2*gap)/2;
        addDrawableChild(ButtonWidget.builder(Text.literal("< Previous"), b -> { page--; clearAndInit(); }).dimensions(x, buttonY, buttonWidth, 20).build()).active = page > 0;
        addDrawableChild(ButtonWidget.builder(Text.literal("Next >"), b -> { page++; clearAndInit(); }).dimensions(x+buttonWidth+gap, buttonY, buttonWidth, 20).build()).active = (page+1)*perPage < mods.size();
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close()).dimensions(x+2*(buttonWidth+gap), buttonY, buttonWidth, 20).build());
    }

    private void scan() {
        Path dir = FabricLoader.getInstance().getGameDir().resolve("mods");
        if (!Files.isDirectory(dir)) return;
        try (var files = Files.list(dir)) {
            files.filter(Files::isRegularFile).filter(p -> {
                String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
                return name.endsWith(".jar") || name.endsWith(".jar.disabled");
            }).sorted().forEach(this::readJar);
        } catch (Exception e) { error = "Could not read the instance mods folder."; }
        mods.sort(Comparator.comparing(Entry::name, String.CASE_INSENSITIVE_ORDER));
    }
    private void readJar(Path path) {
        try (ZipFile jar = new ZipFile(path.toFile())) {
            ZipEntry metadata = jar.getEntry("fabric.mod.json");
            if (metadata == null || metadata.getSize() < 0 || metadata.getSize() > 262144) return;
            JsonObject json;
            try (InputStream in = jar.getInputStream(metadata)) {
                json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            }
            String id = json.get("id").getAsString();
            String name = json.has("name") && json.get("name").isJsonPrimitive() ? json.get("name").getAsString() : id;
            String status = "Not loaded";
            if (path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".disabled")) status = "Disabled";
            else {
                var container = FabricLoader.getInstance().getModContainer(id);
                if (container.isPresent() && container.get().getOrigin().getPaths().stream().anyMatch(p -> p.toAbsolutePath().normalize().equals(path.toAbsolutePath().normalize()))) status = "Loaded";
            }
            Identifier icon = null;
            if (json.has("icon")) {
                JsonElement element = json.get("icon");
                String iconPath = null;
                if (element.isJsonPrimitive()) iconPath = element.getAsString();
                else if (element.isJsonObject()) {
                    int best = Integer.MAX_VALUE;
                    for (var entry : element.getAsJsonObject().entrySet()) {
                        try { int size = Integer.parseInt(entry.getKey()); if (size >= 32 && size < best) { best=size; iconPath=entry.getValue().getAsString(); } }
                        catch (RuntimeException ignored) { }
                    }
                    if (iconPath == null && !element.getAsJsonObject().entrySet().isEmpty()) iconPath = element.getAsJsonObject().entrySet().iterator().next().getValue().getAsString();
                }
                ZipEntry image = iconPath == null ? null : jar.getEntry(iconPath);
                if (image != null && image.getSize() >= 0 && image.getSize() <= 1048576) {
                    try (InputStream in = jar.getInputStream(image)) {
                        NativeImage nativeImage = NativeImage.read(in);
                        if (nativeImage.getWidth() <= 1024 && nativeImage.getHeight() <= 1024) {
                            icon = new Identifier("zythera", "runtime/mod_icon_" + mods.size());
                            client.getTextureManager().registerTexture(icon, new NativeImageBackedTexture(nativeImage));
                        } else nativeImage.close();
                    } catch (Exception ignored) { }
                }
            }
            mods.add(new Entry(name, id, status, icon));
        } catch (Exception e) { System.err.println("[Zythera] Could not read mod archive " + path.getFileName() + ": " + e.getMessage()); }
    }
    @Override public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width/2, 10, 0xffffffff);
        int totalPages = Math.max(1, (mods.size()+perPage-1)/perPage);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Instance mods folder | " + (page+1) + "/" + totalPages), width/2, 23, 0xffbcbcbc);
        if (error != null) ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(error), width/2, 45, 0xffff7777);
        else if (mods.isEmpty()) ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("No Fabric mod JARs found in this instance."), width/2, 45, 0xffbcbcbc);
        int left = Math.max(5, (width-Math.min(400, width-10))/2), right = width-left;
        for (int i=page*perPage; i<Math.min(mods.size(), (page+1)*perPage); i++) {
            Entry mod=mods.get(i); int y=39+(i-page*perPage)*29;
            ctx.fill(left,y,right,y+26,0x990c0912);
            if (mod.icon()!=null) ctx.drawTexture(mod.icon(),left+2,y+2,0,0,22,22,22,22);
            else ctx.fill(left+2,y+2,left+24,y+24,0xff4a285e);
            int textX=left+29, available=Math.max(1,right-textX-6);
            ctx.drawTextWithShadow(textRenderer,textRenderer.trimToWidth(mod.name(),available),textX,y+3,0xffffffff);
            ctx.drawTextWithShadow(textRenderer,textRenderer.trimToWidth(mod.id()+" - "+mod.status(),available),textX,y+14,mod.status().equals("Loaded") ? 0xffb0e6a5 : 0xffbbbbbb);
        }
        super.render(ctx,mouseX,mouseY,delta);
        if ("1".equals(System.getenv("ZYTHERA_SMOKE_TEST"))) {
            boolean found = mods.stream().anyMatch(mod -> mod.id().equals("zythera_smoke_fixture") && mod.status().equals("Disabled"));
            if (!found) throw new IllegalStateException("Disabled mod fixture missing from installed mods screen");
            System.out.println("[Zythera] SMOKE_INSTALLED_MODS_OK");
            client.scheduleStop();
        }
    }
    @Override public void close() {
        for (Entry entry : mods) if (entry.icon()!=null) client.getTextureManager().destroyTexture(entry.icon());
        client.setScreen(parent);
    }
    @Override public boolean shouldPause() { return false; }
}
