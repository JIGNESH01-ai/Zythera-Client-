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

The Fabric client overlays Minecraft's resource-reload screen with Zythera artwork and an ASCII bitmap font loaded directly from the mod, so text does not depend on font resources still being reloaded. The percentage labeled "Visual startup animation" follows the requested sequence with five-second holds at 50, 70 and 95; it is **not** actual loading progress. A separate "Resources" percentage comes from Minecraft's reload monitor. The animation waits below 100 until the reload succeeds, shows "Loading Complete!", then lets vanilla close the overlay. Errors are not suppressed. The intentional sequence adds at least 19.8 seconds to each resource reload, including reloads triggered in-game; it does not speed up or measure world generation. On multiplayer the server controls simulation behavior. The reference was a flattened 1672 x 941 screenshot, not separate master artwork. The logo is a lossless 1195 x 485 crop at source resolution; the background is reconstructed from unlabelled screenshot edges to avoid displaying the reference image’s fake 72% bar. The loading overlay uses framebuffer-pixel coordinates, aspect-preserving scaling and bilinear texture sampling. An original standalone high-resolution logo and background would be needed to improve beyond that screenshot’s native detail. PojavLauncher/Android compatibility has not been tested.

## Installable instance

See [the modpack project](../modpack/README.md) for the Modrinth-format import archive, manual fallback, bundled dependencies, and Android limitations.

### Purple reference design (0.5.3)
The purple logo is cut out from the supplied flattened loading-screen reference; its detail and edge quality are limited by that screenshot. The fantasy background is a generated interpretation of the references with their baked-in UI removed, not an original game artwork or an exact pixel match. Separate original logo and background files would allow higher fidelity. The Accounts button explains how to manage accounts in the launcher; it does not authenticate or switch accounts. Visual quality, full menu interaction, packaged mods, and Android/Pojav remain unverified unless separately tested.

### Loading screen reference (0.5.4)
Only the resource-loading overlay has been redesigned. Its logo is extracted from the supplied flattened screenshot; the scenery is generated reference-inspired artwork, not the original image. The Discord address is reproduced as reference text and has not been verified. The splash font uses an embedded ASCII atlas because the Minecraft font may not yet be loaded; the overlay's own labels use ASCII. The animation percentage is visual, while the Resources line reports the real reload progress. The main menu has not been redesigned in this update.

### Purple theme (0.5.5)
Both title and resource-loading screens use the earlier purple logo and bright castle scenery, as selected when the gold reference conflicted with the purple written requirements. The logo is extracted from a flattened reference image and the clean scenery is generated, so these are not original separate master assets or an exact reproduction. Title text uses Minecraft's language-aware font; the early loading overlay uses its bundled ASCII atlas for its own English labels. Account sign-in is managed in the launcher. Device compatibility and visual quality still require real-device validation.

### Loading screen cleanup (0.5.6)
The overlay uses proportional DejaVu Sans glyphs for its built-in English labels, framebuffer-relative layout and a single lightweight progress track. The percentage is explicitly labeled animation; the separate resource percentage remains Minecraft's actual reload progress. The FPS label is centred at the top to avoid the previous top-right placement, but Android launcher controls are outside the mod's control and their positions differ. The existing logo is extracted from a flattened reference screenshot; the bright background is generated reference-inspired artwork, not separate original masters. Real-device appearance and Pojav compatibility require testing.
