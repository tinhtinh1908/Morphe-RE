#!/usr/bin/env python3
"""Exercise the injected Java helper with a host PackageInfo stub and patched constant."""
import os
from pathlib import Path
import subprocess
root = Path(__file__).resolve().parents[1]
target = root / "build/update-runtime-test"
stub = target / "android/content/pm/PackageInfo.java"
stub.parent.mkdir(parents=True, exist_ok=True)
stub.write_text("""package android.content.pm;
public class PackageInfo {
    public String packageName;
    public int versionCode;
    public int versionCodeMajor;
    public long getLongVersionCode() {
        return ((long) versionCodeMajor << 32) | (versionCode & 0xffffffffL);
    }
}
""")
helper = target / "vn/dtinh/updates/PlayStoreUpdates.java"
helper.parent.mkdir(parents=True, exist_ok=True)
source = (root / "extension/vn/dtinh/updates/PlayStoreUpdates.java").read_text()
assert source.count("return 0;") == 1
helper.write_text(source.replace("return 0;", "return 12345;").replace('return "";', 'return "com.facebook.orca";'))
test = target / "UpdateRuntimeTest.java"
test.write_text("""import android.content.pm.PackageInfo;
import vn.dtinh.updates.PlayStoreUpdates;
public class UpdateRuntimeTest {
    static void check(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) {
        PackageInfo info = new PackageInfo();
        for (String name : new String[] {"com.facebook.orca"}) {
            info.packageName = name;
            info.versionCode = Integer.MAX_VALUE;
            info.versionCodeMajor = 7;
            check(PlayStoreUpdates.getVersionCode(info) == 12345);
            check(PlayStoreUpdates.getVersionCodeLong(info) == (7L << 32 | 12345L));
            check(info.versionCode == Integer.MAX_VALUE); // Installed version stays high.
            info.versionCode = 42;
            check(PlayStoreUpdates.getVersionCode(info) == 42);
            check(PlayStoreUpdates.getVersionCodeLong(info) == (7L << 32 | 42L));
        }
        info.packageName = "com.zing.zalo";
        info.versionCode = Integer.MAX_VALUE;
        check(PlayStoreUpdates.getVersionCode(info) == Integer.MAX_VALUE);
        info.packageName = "other.app";
        info.versionCode = Integer.MAX_VALUE;
        check(PlayStoreUpdates.getVersionCode(info) == Integer.MAX_VALUE);
        check(PlayStoreUpdates.getVersionCodeLong(info) == (7L << 32 | 2147483647L));
        info.packageName = null;
        check(PlayStoreUpdates.getVersionCode(info) == Integer.MAX_VALUE);
        System.out.println("Update blocking runtime: PASS (own app, other supported app, original int/long, major bits, no mutation, other packages)");
    }
}
""")
java = Path(os.environ["JAVA_HOME"]) / "bin"
subprocess.run([str(java / "javac"), "--release", "8", "-d", str(target), str(stub), str(helper), str(test)], check=True)
subprocess.run([str(java / "java"), "-cp", str(target), "UpdateRuntimeTest"], check=True)

# Repeat the same checks with Zalo as the patched package.
helper.write_text(source.replace("return 0;", "return 12345;").replace('return "";', 'return "com.zing.zalo";'))
test.write_text(test.read_text().replace("com.facebook.orca", "__OWN__").replace("com.zing.zalo", "com.facebook.orca").replace("__OWN__", "com.zing.zalo"))
subprocess.run([str(java / "javac"), "--release", "8", "-d", str(target), str(stub), str(helper), str(test)], check=True)
subprocess.run([str(java / "java"), "-cp", str(target), "UpdateRuntimeTest"], check=True)
