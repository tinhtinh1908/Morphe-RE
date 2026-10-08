# Developer checks

Build the project first with `bash build.sh`. No app APK is included in this repository.

For an APK patched with Messenger support, compile and run the structural checker:

```bash
mkdir -p build/test-classes
"$JAVA_HOME/bin/javac" -cp .tools/morphe-desktop-1.18.0-all.jar -d build/test-classes tests/VerifyNoUi.java
"$JAVA_HOME/bin/java" -Xmx1800m -cp build/test-classes:.tools/morphe-desktop-1.18.0-all.jar VerifyNoUi /absolute/path/to/patched.apk
```

The checker verifies no diagnostic classes/references or UI/log calls, no duplicate class descriptors, token storage namespace, three FCM support hooks, absence of active 0.4 hooks and invoke/move-result adjacency. Manifest component comparison and device push verification are separate checks.

`build-fixture.sh /absolute/path/to/routed-input.apk` creates a test-only unrouted fixture under build/fixture. Its MPP is not part of the release. It does not recreate a pristine stock APK; it reverses route markers and Messenger support hooks/storage substitutions for testing fresh insertion. Dormant helper classes can remain. Sign a fixture with your own test key before testing automatic certificate extraction; no signing key is included here.

`bash tests/test-policy.sh` runs JVM retry policy checks and is also run automatically by build.sh. It does not exercise Android lifecycle callbacks or a real push provider.
