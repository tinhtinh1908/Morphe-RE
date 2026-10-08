#!/usr/bin/env bash
set -euo pipefail
project="$(cd "$(dirname "$0")" && pwd)"
cd "$project"
: "${JAVA_HOME:?Use JDK 21 and set JAVA_HOME}"
version="$(tr -d '\r\n' < VERSION)"
if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "VERSION must contain a semantic version, for example 0.4.0" >&2
  exit 1
fi
python3 scripts/setup_tools.py
fat="$project/.tools/morphe-desktop-1.18.0-all.jar"
compiler="$project/.tools/kotlin-compiler-2.3.10.jar"
annotations="$project/.tools/annotations-26.0.2.jar"
android="$project/.tools/platform/android-35/android.jar"
d8="$project/.tools/buildtools/android-15/lib/d8.jar"
build="$project/build"
rm -rf "$build/classes" "$build/patch-dex" "$build/extension-classes" "$build/extension-dex"
mkdir -p "$build/classes" "$build/patch-dex" "$build/extension-classes" "$build/extension-dex" "$project/dist"
"$JAVA_HOME/bin/javac" --release 8 -encoding UTF-8 -cp "$android" -d "$build/extension-classes" "$project"/extension/vn/dtinh/messenger/*.java
"$JAVA_HOME/bin/jar" cf "$build/extension.jar" -C "$build/extension-classes" .
"$JAVA_HOME/bin/java" -cp "$d8" com.android.tools.r8.D8 --min-api 28 --lib "$android" --output "$build/extension-dex" "$build/extension.jar"
"$JAVA_HOME/bin/java" -cp "$compiler:$fat:$annotations" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -no-reflect -language-version 2.0 -jvm-target 11 -classpath "$fat:$annotations" -d "$build/classes" "$project/src/general/FcmMicroGPatch.kt" "$project/src/messenger/MessengerRepairPatch.kt"
"$JAVA_HOME/bin/jar" cf "$build/patch.jar" -C "$build/classes" .
"$JAVA_HOME/bin/java" -cp "$d8" com.android.tools.r8.D8 --min-api 26 --lib "$android" --classpath "$fat" --output "$build/patch-dex" "$build/patch.jar"
python3 scripts/package_mpp.py "$version"
"$JAVA_HOME/bin/java" -jar "$fat" list-patches --patches "$project/dist/DTinh-MicroG-FCM-$version.mpp" -o
