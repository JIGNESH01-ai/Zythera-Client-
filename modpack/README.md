# Zythera Client 1.20.1 modpack (experimental)

This directory holds the pack template. GitHub Actions builds Zythera and downloads pinned 1.20.1 releases of Fabric API, Lithium and Sodium, checks their JAR metadata, assembles the ZIP and publishes an alpha on a successful branch build. JARs and the generated ZIP are not committed to Git. The downloadable ZIP contains the `mods`, `config` and `resourcepacks` folders, installation guide, license notices and checksums. Hashes describe the downloaded files, but are not independent upstream authenticity verification.

Read [INSTALL.txt](INSTALL.txt) before installation. You must supply Minecraft Java, Java 17, Fabric Loader and a launcher. The modpack is not a standalone launcher and it is not an Android APK.

The title and loading overlays, Zythera GUI, HUD and zoom are implemented in the Zythera JAR. Sprint toggle is Minecraft's own accessibility setting; no separate sprint mod is bundled. The resource pack is an empty placeholder. The module manager controls Zythera modules, not external JARs. Android 9, Pojav, low-RAM hardware, gameplay, touch inputs, optimization interactions and rendering have not been tested; do not assume compatibility or performance. Remove Sodium if its renderer is incompatible. Some servers restrict zoom or toggle sprint.
