#!/usr/bin/env python3
"""Download pinned official build dependencies into .tools; check every SHA-256."""
import hashlib
import json
from pathlib import Path
import shutil
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / ".tools"
LOCK = ROOT / "tools.lock.json"

def digest(path):
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()

def main():
    TOOLS.mkdir(exist_ok=True)
    for item in json.loads(LOCK.read_text())["dependencies"]:
        path = TOOLS / item["filename"]
        if not path.is_file() or digest(path) != item["sha256"]:
            print("Downloading", item["filename"], flush=True)
            temp = path.with_suffix(path.suffix + ".part")
            request = urllib.request.Request(item["url"], headers={"User-Agent": "microg-fcm-patches-build"})
            with urllib.request.urlopen(request, timeout=120) as source, temp.open("wb") as target:
                shutil.copyfileobj(source, target)
            if digest(temp) != item["sha256"]:
                temp.unlink()
                raise RuntimeError("Checksum mismatch: " + item["filename"])
            temp.replace(path)
        if "extract" in item:
            destination = TOOLS / item["extract"]
            marker = destination / ".archive-sha256"
            if not marker.is_file() or marker.read_text().strip() != item["sha256"]:
                if destination.exists():
                    shutil.rmtree(destination)
                destination.mkdir()
                with zipfile.ZipFile(path) as archive:
                    for name in archive.namelist():
                        candidate = (destination / name).resolve()
                        if not candidate.is_relative_to(destination.resolve()):
                            raise RuntimeError("Unsafe archive member")
                    archive.extractall(destination)
                marker.write_text(item["sha256"] + "\n")
    print("Pinned build tools ready.")

if __name__ == "__main__":
    main()
