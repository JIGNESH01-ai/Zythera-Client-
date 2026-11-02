# Zythera Client 1.20.1 instance (experimental)

This folder is the template for two distribution formats assembled by GitHub Actions:

* `zythera-client-1.20.1-v0.5.8-alpha.mrpack` and the identical `.zip`: Modrinth-format manifest plus bundled `overrides/` (Fabric API, Sodium, Lithium, FerriteCore, Zythera, config, low-end options and resourcepacks). The Pojav-based launcher in this repository parses this manifest, extracts overrides and creates a named, separate profile before downloading Fabric Loader. The `.mrpack` itself is a ZIP archive.
* `zythera-modpack-1.20.1-v0.5.8-alpha.zip`: manual copy format for launchers without compatible import.

[INSTALL.txt](INSTALL.txt) has mobile steps and recovery. The package does not contain Minecraft, Fabric Loader, Java or a launcher; the user must supply those. The import and Android gameplay have not been tested. Do not assume all Pojav forks support importing Modrinth packs. The bundled Sodium is a desktop renderer optimization and may be incompatible with Pojav renderers. Try removing it on failure. Minecraft 1.20.1 needs Java 17; low-memory performance varies.

The title/loading overlays, HUDs, controls and zoom are provided by the Zythera JAR. Toggle sprint uses vanilla accessibility; the optional resourcepack is a placeholder. The module manager controls Zythera modules, not third-party JARs. The assembler verifies embedded JAR metadata/CRC and archive structure and records SHA-256 hashes, but these hashes are not independently verified upstream signatures. CI additionally attempts a headless desktop launch of the development client; only the CI result can establish if that smoke test passed, and it does not test the final mod combination or Android.

The optimization selection and known exclusions are documented in [COMPATIBILITY.md](COMPATIBILITY.md).
