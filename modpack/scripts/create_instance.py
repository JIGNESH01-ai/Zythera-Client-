#!/usr/bin/env python3
"""Create a Modrinth-format import archive with bundled, offline mod overrides."""
import json
import shutil
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PACK = ROOT / 'modpack-build'
OUTPUT = ROOT / 'zythera-client-1.20.1-v0.3.0-alpha.mrpack'
MANIFEST = {
    'formatVersion': 1,
    'game': 'minecraft',
    'versionId': '0.3.0-alpha',
    'name': 'Zythera Client',
    'summary': 'Gold and black Fabric client by JIGNESH_GAMING-k7j',
    # Bundled mods are overrides, so imports need not download the mod JARs.
    'files': [],
    'dependencies': {'minecraft': '1.20.1', 'fabric-loader': '0.15.11'},
}
required = [
    'mods/zythera-client-0.3.0.jar',
    'mods/fabric-api-0.92.2+1.20.1.jar',
    'mods/lithium-fabric-mc1.20.1-0.11.4.jar',
    'mods/sodium-fabric-0.5.11+mc1.20.1.jar',
    'config/zythera-1.20.1.json', 'options.txt',
    'resourcepacks/Zythera-Extras/pack.mcmeta', 'INSTALL.txt',
]
for file in required:
    if not (PACK / file).is_file():
        raise SystemExit('Missing required instance file: ' + file)
with zipfile.ZipFile(OUTPUT, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as z:
    z.writestr('modrinth.index.json', json.dumps(MANIFEST, indent=2) + '\n')
    for path in sorted(PACK.rglob('*')):
        if path.is_file():
            z.write(path, 'overrides/' + path.relative_to(PACK).as_posix())
with zipfile.ZipFile(OUTPUT) as z:
    if z.testzip() is not None:
        raise SystemExit('Invalid ZIP CRC')
    parsed = json.loads(z.read('modrinth.index.json'))
    assert parsed == MANIFEST
    for file in required:
        if 'overrides/' + file not in z.namelist():
            raise SystemExit('Missing archive entry: ' + file)
        if file.endswith('.jar'):
            import io
            with zipfile.ZipFile(io.BytesIO(z.read('overrides/' + file))) as jar:
                if jar.testzip() is not None:
                    raise SystemExit('Invalid nested JAR: ' + file)
shutil.copyfile(OUTPUT, OUTPUT.with_suffix('.zip'))
print('Created and validated', OUTPUT, 'and identical .zip')
