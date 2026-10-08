#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
build="$root/build/fixture"
python3 "$root/scripts/setup_tools.py"
mkdir -p "$build/classes" "$build/dex"
fat="$root/.tools/morphe-desktop-1.18.0-all.jar"
java_home="${JAVA_HOME:?Set JDK 21 JAVA_HOME}"
"$java_home/bin/java" -cp "$root/.tools/kotlin-compiler-2.3.10.jar:$fat:$root/.tools/annotations-26.0.2.jar" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -no-reflect -language-version 2.0 -jvm-target 11 -classpath "$fat:$root/.tools/annotations-26.0.2.jar" -d "$build/classes" "$root/tests/UnrouteFixture.kt"
"$java_home/bin/jar" cf "$build/fixture.jar" -C "$build/classes" .
"$java_home/bin/java" -cp "$root/.tools/buildtools/android-15/lib/d8.jar" com.android.tools.r8.D8 --min-api 26 --lib "$root/.tools/platform/android-35/android.jar" --classpath "$fat" --output "$build/dex" "$build/fixture.jar"
python3 - "$build" <<'PY'
import zipfile,pathlib,sys
b=pathlib.Path(sys.argv[1])
with zipfile.ZipFile(b/'fixture.jar') as src,zipfile.ZipFile(b/'fixture.mpp','w',zipfile.ZIP_DEFLATED) as out:
 for n in src.namelist():
  if not n.endswith('/') and n!='META-INF/MANIFEST.MF':out.writestr(n,src.read(n))
 out.writestr('META-INF/MANIFEST.MF','Manifest-Version: 1.0\r\nName: TEST ONLY\r\nVersion: 0.0.0\r\nPatcher-Version: 1.14.0\r\n\r\n')
 out.write(b/'dex/classes.dex','classes.dex')
PY
"$java_home/bin/java" -Xmx3g -jar "$fat" patch -p "$build/fixture.mpp" -e 'Test fixture remove microG route' --exclusive --unsigned -o "$build/unrouted.apk" -t "$build/work" "${1:?Pass the input APK as the first argument}" > "$build/create.log" 2>&1
