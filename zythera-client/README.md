# ZYTHERA CLIENT

Made by JIGNESH_GAMING-k7j · Subscribe on YouTube

Play Smooth • Play Better • With Zythera

This directory is a **client-side Fabric mod**, not an Android APK or a replacement Minecraft distribution. It is an initial implementation, not the complete requested client.

## Install and run

1. In PojavLauncher Legacy, install Minecraft Java 1.20.1 with Fabric Loader 0.15.11 or newer and a working Java 17 runtime. Pojav's Android renderer/runtime compatibility varies by device; this has not been tested on Android 9.
2. Download the matching Fabric API for 1.20.1 and a Zythera release JAR from the GitHub Actions build. Put both in the selected game directory's `mods` folder. Do not install the `-sources.jar`.
3. Launch the profile. Open the menu using Right Shift (rebind under Minecraft Controls; on touch devices map a launcher button to the key), or left/right click the Zythera Mods button on the title or pause screen. Set the YouTube URL in Settings before using Subscribe.
4. Use HUD Editor → Drag to move the armor and text groups; Escape saves and returns to the editor. Config is stored in `config/zythera-1.20.1.json` under the game directory.

## Features implemented

Armor/offhand icons and durability, configurable vertical/horizontal layout, FPS, coordinates, facing, JVM memory and clock; searchable enable switches; draggable group positions; potato/balanced presets that change Minecraft's render distance, particle level and FPS cap. Presets overwrite the corresponding vanilla options. The menu is accessible in-game or from the title screen via the keybind. No external optimization mods are bundled; see [optional optimization mods](optimizations/README.md).

## Compatibility and limitations

Only **1.20.1** is targeted. 1.21.1, 1.21.11, 26.1, 26.2 and 26.3 are not implemented or verified; they need separate mapped builds and dependency checks. There are no compatibility guarantees for third-party optimization mods or Android renderers. The other requested HUD/PvP/visual modules, per-element scaling/color editor, profiles, a fully replaced main menu, animated logo and full Android-specific optimizations are not implemented. The FPS presets are vanilla settings, not a modpack. Never add optimization JARs without checking each mod's Minecraft, Fabric and renderer compatibility and license.

Build with JDK 17: `./zythera-client/gradlew -p zythera-client build` from the repository root. The build needs access to Gradle, Mojang and Fabric Maven repositories.

## Builds and releases

The workflow builds and uploads JAR artifacts on relevant pushes and pull requests. To publish a **prerelease** after reviewing the build, run the “Zythera Fabric client” workflow manually from the Actions tab with a new tag such as `zythera-1.20.1-v0.1.0`. GitHub requires the workflow to exist on the default branch for manual dispatch. The release job runs only after the build succeeds and requires Actions to have permission to create releases. A successful compile alone does not establish in-game or Android compatibility.

## Resource loading screen

The Fabric client overlays the Minecraft resource-reload screen with Zythera artwork and the reload monitor’s real progress. Minecraft still controls reload completion and error handling. It is not a world/terrain loading progress indicator; the game does not expose a single equivalent progress value there. The artwork is derived from the creator-provided reference. This feature has not yet been tested in-game or with PojavLauncher.

## Installable instance

See [the modpack project](../modpack/README.md) for the Modrinth-format import archive, manual fallback, bundled dependencies, and Android limitations.
