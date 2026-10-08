# Validation — 0.5.0

## Uploaded APK

Messenger `com.facebook.orca` 573.0.0.44.88, SHA-256 `69d491d416dbcc6eb281b8a0c416a26f320d2b75d297be4680428bf615a35858`.

The uploaded APK already contains the 0.4 Messenger support: exactly three isolated IID storage literals, one boot hook and one PendingIntent hook, routed receiver/action/permission, microG package visibility and valid original signer metadata. It has no injected diagnostic UI. Static inspection cannot establish whether the device has registered in microG or received a push.

## Findings and changes

- The 0.4 runtime sets a process-wide attempted flag before registration. Package lookup failures, exceptions and failures returned by Messenger can suppress retries until process restart.
- Messenger's `X.1Gw.C2F` returns false on its successful path as well as some failure paths. The new runtime does not interpret this boolean as success.
- The 0.5 callback follows the existing `X.1cT.A07(session, token, ...)` call in the nonempty-token branch. This establishes local token handoff to Messenger's existing flow; it is not a server ACK or a delivery receipt.
- Foreground retries use 20/60/120/300-second backoff. One extension worker runs at a time. Pending retries are removed when all Activities pause. An already running host token request can finish; no new background service, alarm or socket is added.
- Account IDs scope completion; account changes can register again. No token is stored, exported or logged by the extension.
- Already patched inputs reuse storage and replace old hook destinations. The versioned runtime avoids Morphe's extension merger retaining old method bodies. Dormant 0.4 helper classes can remain in an upgraded APK, but no application hook calls them.
- Patch-time checks verify the reflected constructor/session/method signatures. Boot insertion preserves invoke/move-result adjacency. Inputs with old diagnostic Activities remain rejected.

## Executed checks

Official Morphe Desktop 1.18.0 / patcher 1.14.0 and JDK 21:

1. Build and MPP loading passed; exactly two public patches.
2. RegistrationPolicy tests passed: missing login, concurrency, cooldown, capped backoff, completion and account switches. These are JVM policy tests, not Android lifecycle/device tests.
3. Selecting only Messenger support upgraded the uploaded 0.4 APK in FULL mode. Dependency routing ran first; original spoofed signer was preserved.
4. Reapplying 0.5 to that output passed in STRIP_SAFE mode. No duplicate hooks or classes.
5. A synthetic fixture reverses routing and removes Messenger support hooks/storage substitutions from the uploaded APK; applying general routing plus Messenger support passed in STRIP_SAFE mode, including fresh insertion of all three hooks. Explicit original signer metadata was supplied for this unsigned fixture. This is not a separately obtained stock APK; dormant helper classes remain in the fixture.
6. All three output APKs passed full dexlib2 checks: 128761 unique classes; exactly three support hooks and three storage literals; no application calls to the old runtime; no diagnostic class/reference, added extension Activity or extension UI/log/token-storage call; every move-result follows an instruction that sets a result.
7. For the upgraded output, decoded manifest text matches the uploaded APK after removing source line numbers. All 78039 lines of decoded resources match exactly, although resource-table serialization bytes changed during rebuild. All 18618 audited non-DEX archive entries, including 13 native library entries, are byte-identical.

Machine-readable patching results and archive audit are under `validation/`. Earlier checks are in `validation/0.4.md`.

Not verified: Android runtime/lifecycle behavior, microG token registration on the phone, Meta server acceptance, push delivery/latency, device patcher 1.14.1, other Messenger versions, or arbitrary combinations of third-party patches. Build success and local token handoff do not prove end-to-end notifications. Check GitHub Actions for remote build results.
