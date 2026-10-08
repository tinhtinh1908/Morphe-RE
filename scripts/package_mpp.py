#!/usr/bin/env python3
from pathlib import Path
import sys
import zipfile
ROOT = Path(__file__).resolve().parents[1]
version = sys.argv[1]
build = ROOT / "build"
target = ROOT / "dist" / f"DTinh-MicroG-FCM-{version}.mpp"
manifest = ("Manifest-Version: 1.0\r\nName: DTinh MicroG FCM Patches\r\n"
            f"Version: {version}\r\nAuthor: DTinh\r\nLicense: GPL-3.0-only\r\n"
            "Patcher-Version: 1.14.0\r\n\r\n")
with zipfile.ZipFile(build / "patch.jar") as source, zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED) as out:
    for name in source.namelist():
        if not name.endswith("/") and name != "META-INF/MANIFEST.MF":
            out.writestr(name, source.read(name))
    out.writestr("META-INF/MANIFEST.MF", manifest)
    for name in ("LICENSE-patches", "LICENSE-zeldris", "THIRD_PARTY_NOTICES.md"):
        out.write(ROOT / name, name)
    for dex in (build / "patch-dex").glob("classes*.dex"):
        out.write(dex, dex.name)
    for app in ("messenger", "zalo", "gmail"):
        out.write(build / f"extension-{app}-dex/classes.dex", f"extensions/{app}-fcm.dex")
print(target)
