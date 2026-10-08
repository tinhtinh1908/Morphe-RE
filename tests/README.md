# Developer checks

Build with `bash build.sh` first. No app APK or fixture signing key is included.

`bash tests/test-policy.sh` tests missing login, concurrency, cooldown, capped retry backoff, local token handoff and account switches. It runs automatically during build; it does not exercise an Android lifecycle or a real push provider.

Compile both structural checkers:

```bash
mkdir -p build/test-classes
"$JAVA_HOME/bin/javac" -cp .tools/morphe-desktop-1.18.0-all.jar -d build/test-classes tests/VerifyNoUi.java tests/VerifyZalo.java
```

For Messenger:

```bash
"$JAVA_HOME/bin/java" -Xmx1800m -cp build/test-classes:.tools/morphe-desktop-1.18.0-all.jar VerifyNoUi /absolute/path/to/messenger-patched.apk
```

For Zalo:

```bash
"$JAVA_HOME/bin/java" -Xmx1800m -cp build/test-classes:.tools/morphe-desktop-1.18.0-all.jar VerifyZalo /absolute/path/to/zalo-patched.apk
```

Both check every DEX for duplicate classes, diagnostic/UI/log calls, isolated storage, exact hook counts and invoke/move-result adjacency. Zalo's checker rejects Messenger runtime injection. Manifest component/resource comparison, reflection signature access, Android runtime behavior and device push verification require separate checks.

`build-fixture.sh /absolute/path/to/routed.apk` reverses route markers/metadata and known support hooks/storage under `build/fixture`. The fixture MPP is test-only; this does not recreate a pristine stock APK and dormant helpers can remain. Sign the resulting fixture with your own temporary test key before testing automatic signer extraction, then select only the appropriate app patch from the release MPP. The universal patch and its old certificate option do not exist in 0.6.
