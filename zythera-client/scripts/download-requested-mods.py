#!/usr/bin/env python3
"""Download pinned upstream mods into an EMPTY local folder, without rehosting them."""
import hashlib
import json
import os
import shutil
import sys
import tempfile
import urllib.request
import zipfile
from pathlib import Path

# Exact Modrinth version IDs; their primary files publish checksums in the API.
VERSIONS = {
    'Clumps': 'hefSwtn6', 'Entity Culling': 'xuiHEnFJ', 'Exordium': 'Wpxm5HM3',
    'Fabric API': 'P7uGFii0', 'Fabric Chunk Pregenerator': 'dDZVCrf6',
    'FerriteCore': 'unerR5MN', 'FPS Reducer': 'i4klNdvx', 'JourneyMap': 'mQisuQcc',
    'Lithium': 'iEcXOkz4', 'YACL': 'dvS5DjUA', 'Zoomify': 'hWl13Hjr',
    'Fabric Language Kotlin (Zoomify dependency)': 'E4WyjCxJ',
}
AETHERIUM = ('https://github.com/entitybrian69-bit/aetherium/releases/download/v1.15.0%2B1.20.1/aetherium-fabric-1.15.0%2B1.20.1.jar',
             'aetherium-fabric-1.15.0+1.20.1.jar', '5392f2ffe2e7c1cc13c68d9ef6102af8e50e547d1e31bc84579f7efbbefac4ad')
HEADERS = {'User-Agent': 'ZytheraClient/0.5.7 (mod installer)'}
def get(url):
    with urllib.request.urlopen(urllib.request.Request(url, headers=HEADERS), timeout=60) as response:
        return response.read()

def main():
    target = Path(sys.argv[1]) if len(sys.argv)>1 else Path(__file__).resolve().parents[1] / 'mods'
    target.mkdir(parents=True, exist_ok=True)
    if any(target.glob('*.jar')):
        raise SystemExit(f'{target} already contains JARs. Use an empty folder; resolve duplicates yourself.')
    with tempfile.TemporaryDirectory(dir=target) as temp:
        stage = Path(temp)
        for name, vid in VERSIONS.items():
            meta = json.loads(get('https://api.modrinth.com/v2/version/' + vid))
            if meta['id'] != vid or 'fabric' not in meta['loaders'] or '1.20.1' not in meta['game_versions']:
                raise ValueError(f'Unexpected metadata for {name}')
            primary = next(f for f in meta['files'] if f['primary'])
            filename = primary['filename']
            if Path(filename).name != filename or not filename.endswith('.jar'):
                raise ValueError(f'Unsafe filename for {name}')
            data = get(primary['url'])
            if hashlib.sha512(data).hexdigest() != primary['hashes']['sha512']:
                raise ValueError(f'Checksum mismatch for {name}')
            path = stage / filename
            path.write_bytes(data)
            with zipfile.ZipFile(path) as jar:
                json.loads(jar.read('fabric.mod.json'))
                if jar.testzip(): raise ValueError(f'Corrupt JAR: {name}')
            print(f'{name}: {filename}', flush=True)
        url, filename, sha = AETHERIUM
        data = get(url)
        if hashlib.sha256(data).hexdigest() != sha: raise ValueError('Aetherium checksum mismatch')
        (stage / filename).write_bytes(data)
        with zipfile.ZipFile(stage / filename) as jar:
            json.loads(jar.read('fabric.mod.json'))
            if jar.testzip(): raise ValueError('Corrupt Aetherium JAR')
        print('Aetherium: ' + filename, flush=True)
        for path in stage.iterdir():
            if (target / path.name).exists(): raise ValueError('Destination exists: ' + path.name)
        for path in stage.iterdir(): os.replace(path, target / path.name)
    print('Downloaded to', target)
if __name__ == '__main__':
    try: main()
    except Exception as e:
        print('Download failed:', e, file=sys.stderr)
        sys.exit(1)
