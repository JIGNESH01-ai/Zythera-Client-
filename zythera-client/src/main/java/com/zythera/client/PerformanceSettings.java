package com.zythera.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.ParticlesMode;

/** Presets are applied once when selected; vanilla options remain user-editable. */
public final class PerformanceSettings {
    private PerformanceSettings() {}
    public static void apply(MinecraftClient client, String profile) {
        var options = client.options;
        boolean balanced = "Balanced".equals(profile);
        boolean smooth = "Smooth FPS".equals(profile);
        options.getViewDistance().setValue(balanced ? 6 : 2);
        options.getSimulationDistance().setValue(balanced ? 4 : 2);
        options.getGraphicsMode().setValue(GraphicsMode.FAST);
        options.getParticles().setValue(balanced ? ParticlesMode.DECREASED : ParticlesMode.MINIMAL);
        options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
        options.getAo().setValue(false);
        options.getEntityDistanceScaling().setValue(balanced ? 0.75 : 0.5);
        options.getEnableVsync().setValue(false);
        options.getMipmapLevels().setValue(balanced ? 2 : 0);
        options.getMaxFps().setValue(smooth || balanced ? 60 : 30);
        options.write();
        ZytheraClient.settings.performanceProfile = profile;
        ZytheraClient.settings.optimizationInitialized = true;
        ZytheraClient.settings.save();
    }
}
