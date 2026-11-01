#!/usr/bin/env python3
"""Assemble the distributable; dependency download and Gradle build are separate steps."""
import hashlib
import json
import shutil
import sys
import zipfile
from pathlib import Path

root = Path(__file__).resolve().parents[2]
source = root / 'modpack'
output = root / 'modpack-build'
mods = output / 'mods'
inputs = {
    'fabric-api-0.92.2+1.20.1.jar': ('fabric-api', '0.92.2+1.20.1'),
    'sodium-fabric-0.5.11+mc1.20.1.jar': ('sodium', '0.5.11'),
    'lithium-fabric-mc1.20.1-0.11.4.jar': ('lithium', '0.11.4'),
    'zythera-client-0.3.0.jar': ('zythera', '0.3.0'),
}
if output.exists():
    shutil.rmtree(output)
mods.mkdir(parents=True)
for name in ('config', 'resourcepacks', 'licenses'):
    shutil.copytree(source / name, output / name)
for name in ('INSTALL.txt', 'THIRD-PARTY-NOTICES.txt', 'ZYTHERA-LICENSE.txt', 'options.txt'):
    shutil.copy2(source / name, output / name)
for name, (mod_id, version) in inputs.items():
    origin = Path(sys.argv[1]) / name
    if not origin.is_file():
        raise SystemExit(f'Missing required dependency: {origin}')
    with zipfile.ZipFile(origin) as jar:
        metadata = json.loads(jar.read('fabric.mod.json'))
        if metadata.get('id') != mod_id or not str(metadata.get('version', '')).startswith(version):
            raise SystemExit(f'Unexpected mod metadata in {origin}: {metadata.get("id")} {metadata.get("version")}')
        if jar.testzip() is not None:
            raise SystemExit(f'Corrupt JAR: {origin}')
    shutil.copy2(origin, mods / name)
with (output / 'SHA256SUMS.txt').open('w', encoding='utf-8') as checksums:
    for jar in sorted(mods.glob('*.jar')):
        checksums.write(hashlib.sha256(jar.read_bytes()).hexdigest() + '  mods/' + jar.name + '\n')
archive = root / 'zythera-modpack-1.20.1-v0.3.0-alpha.zip'
with zipfile.ZipFile(archive, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as z:
    for path in sorted(output.rglob('*')):
        if path.is_file():
            z.write(path, path.relative_to(output).as_posix())
with zipfile.ZipFile(archive) as z:
    if z.testzip() is not None:
        raise SystemExit('Corrupt modpack ZIP')
print(f'Created {archive} with {len(inputs)} checked mod JARs')
