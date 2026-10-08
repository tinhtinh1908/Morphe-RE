# Validation — 0.4.0

User-requested scope correction: remove diagnostic UI and runtime logs. The Messenger patch is now named Messenger microG FCM support. Its extension contains only registration support and the PendingIntent compatibility wrapper. General patch source is byte-identical to 0.3.

Verified using official Morphe Desktop 1.18.0 / patcher 1.14.0:

- Build and bundle loading succeeded.
- Selecting only Messenger support automatically executes the general FCM dependency.
- One-pass patching and rebuilding succeeded on the user's already routed Messenger APK and on the synthetic unrouted fixture used in 0.3. The fixture is derived from the uploaded APK, not a separately acquired stock build.
- Decoded both final binary manifests: component lists match the uploaded APK. No added Activity, launcher alias, service, provider or receiver. No diagnostic launcher label or Activity. MicroG routing metadata is present; the existing input's original spoofed signer remains unchanged.
- Parsed both complete output APKs through dexlib2. No duplicate classes, no FcmDiagnostics/DiagnosticsActivity classes or active references, no Activity subclass in the new extension. No Toast/Clipboard/Log/SharedPreferences calls in the new runtime extension. Exactly three token storage literals and two application hooks to MicroGFcmSupport (boot and pendingBroadcast).
- Existing Messenger FCM listeners/token/error methods no longer receive any diagnostic hooks. Registration support still runs on a background thread and uses Messenger's existing DI/token pipeline.

The patch deliberately rejects an input with the old diagnostic Activity, avoiding retained UI or hooks from earlier repairs. Rebuild from the original APK or the version before the old Messenger repair.

Not verified: runtime push on the phone, UI of existing app components, server token acceptance, all patches from other sources, or device patcher 1.14.1. Successful build/structural checks do not prove end-to-end notifications.

## Portable repository packaging

The repository export reorganizes only build/documentation infrastructure. Kotlin and runtime Java source files are unchanged from the saved 0.4 source artifact. Portable build completed under JDK 21 with the pinned tools, produced DTinh-MicroG-FCM-0.4.0.mpp, and official Morphe Desktop loaded both public patches. Bash/Python syntax and workflow YAML triggers were checked locally. Tool dependencies are SHA-256 pinned; Google platform/build-tools archives correspond to the official repository metadata. The original supplied 0.4 MPP remains available under releases/.

The local checks above do not cover remote GitHub Actions execution or tag-based release publication. Check the repository Actions page for current CI results.
