#!/usr/bin/env python3
from pathlib import Path
import sys
import zipfile
ROOT = Path(__file__).resolve().parents[1]
version = sys.argv[1]
build = ROOT / "build"
target = ROOT / "dist" / f"DTinh-MicroG-FCM-{version}.mpp"
manifest = ("Manifest-Version: 1.0\r\nName: DTinh - Bản vá thông báo\r\n"
            f"Version: {version}\r\nAuthor: DTinh\r\nLicense: GPL-3.0-only\r\n"
            "Patcher-Version: 1.14.0\r\n\r\n")
with zipfile.ZipFile(build / "patch.jar") as source, zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED) as out:
    for name in source.namelist():
        if not name.endswith("/") and name != "META-INF/MANIFEST.MF":
            out.writestr(name, source.read(name))
    out.writestr("META-INF/MANIFEST.MF", manifest)
    # Ship corresponding source inside this local test MPP; no GitHub upload needed.
    files = [ROOT / "build.sh", ROOT / "VERSION", ROOT / "tools.lock.json", ROOT / "README.md"]
    files.extend(ROOT.glob("LICENSE*"))
    files.extend(ROOT.glob("NOTICE*"))
    files.extend([ROOT / "LICENSING.md", ROOT / "THIRD_PARTY_NOTICES.md"])
    for folder in ("src", "extension", "scripts", "tests"):
        files.extend(p for p in (ROOT / folder).rglob("*") if p.is_file()
                     and "__pycache__" not in p.parts)
    for p in files:
        out.write(p, "source/" + str(p.relative_to(ROOT)))
    for name in ("LICENSE", "LICENSE-patches", "LICENSE-zeldris", "NOTICE-zeldris", "NOTICE-morphe-patches", "NOTICE-update-blocking", "NOTICE-morphe-patcher", "THIRD_PARTY_NOTICES.md", "LICENSING.md"):
        out.write(ROOT / name, name)
    for dex in (build / "patch-dex").glob("classes*.dex"):
        out.write(dex, dex.name)
    for app in ("messenger", "zalo", "updates"):
        out.write(build / f"extension-{app}-dex/classes.dex", f"extensions/{app}-fcm.dex")
with zipfile.ZipFile(target) as archive:
    names = archive.namelist()
    assert set(n for n in names if n.startswith("extensions/")) == {
        "extensions/messenger-fcm.dex", "extensions/zalo-fcm.dex", "extensions/updates-fcm.dex"}
    # Gmail source may be archived for review; its compiled patch stays on dev.
    assert not any("gmail" in n.lower() for n in names if not n.startswith("source/")), "Gmail must stay experimental"
    for name in ("LICENSE", "LICENSE-patches", "LICENSE-zeldris", "NOTICE-zeldris",
                 "NOTICE-morphe-patches", "NOTICE-update-blocking", "NOTICE-morphe-patcher", "THIRD_PARTY_NOTICES.md", "LICENSING.md"):
        assert archive.read(name) == (ROOT / name).read_bytes(), name
    public_sources = [ROOT / "src/messenger/MessengerRepairPatch.kt",
                      ROOT / "src/zalo/ZaloMicroGPatch.kt",
                      ROOT / "src/zalo/BypassNativeStartupTamperPatch.kt"]
    assert all(p.exists() for p in public_sources)
print("Bundle audit: PASS (Messenger/Zalo only; licenses and notices byte-exact)")
print(target)
