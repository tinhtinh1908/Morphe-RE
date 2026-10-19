# Validation — 0.6.0

This bundle exposes exactly two version-scoped patches: Messenger microG FCM support (`com.facebook.orca` 573.0.0.44.88) and Zalo microG FCM support (`com.zing.zalo` 26.08.01). The universal patch entry and its manual certificate/availability options have been removed. Shared routing is an unnamed internal dependency that rejects all other package/version combinations.

## Zalo input and analysis

Uploaded APK SHA-256: `88f8f9e8d840126c45659faf2154713a056379bf40f2d5ae8b028058a8dde27b`.

The supplied input already routes FCM actions, sender permissions and transport destinations to MicroG-RE. Its saved spoofed signer is `9487ba76b32e9e36785fb4c3540021f85af8d7b7`; the patch preserves it. The hash is format-validated, not independently authenticated against a pristine stock Zalo APK.

The verified token path uses `xi0.b.a(Context)` through the active push provider, then `yi0.f.e(FIREBASE, Context, token)` and `xi0.e.a(token)` for the existing submission flow. The latter applies Zalo's login, unchanged-token and timestamp checks. This patch preserves those checks and the native SDK token cache/in-flight deduplication.

Changes:

- Four storage/migration literals in `ei0.p`, `jf.w`, `ff.c` use `dtinh.microg.zalo.fcm.v1` instead of Google token storage. No account data or server registration preferences are deleted.
- The original Firebase IID receiver is explicitly enabled. No Activity, service, receiver, provider or launcher is added.
- A boot hook follows `StartupApplication.onCreate`; a local token-handoff hook follows `xi0.e.a` in the active Firebase provider branch of `yi0.f.e`.
- Runtime waits for Zalo's login guard, current UID, initialized listener and active Firebase channel. It calls the existing provider and registration flow on a background worker, with foreground-only 20/60/120/300-second retry backoff. It does not force a different push channel, add a socket, log/export tokens or interpret local handoff as a server ACK.
- Reflected methods/fields and exact hook/storage counts are checked before emitting an output. Reapplying 0.6 reuses existing hooks/storage.
- Zalo's existing Android 31+ PendingIntent mutability flag is retained. There is no hardcoded `o9/a` auth replacement count.
- On an unrouted input, Zalo routing changes are limited to the verified FCM transport/helper classes. Existing changes outside that scope in an already patched input are preserved rather than guessed/reversed.

## Executed checks

Official Morphe Desktop 1.18.0 / patcher 1.14.0, JDK 21:

1. Build and MPP loading pass; list-patches exposes exactly the two app entries, with no universal entry. RegistrationPolicy JVM checks pass.
2. Selecting only Zalo support patches/rebuilds the uploaded input in STRIP_SAFE mode; routing dependency executes automatically and saved signer metadata is retained.
3. Reapplying Zalo 0.6 passes in FULL mode, with no duplicate hooks/classes.
4. A synthetic fixture reverses routes/metadata from the uploaded input and is signed with a temporary test key. Selecting only Zalo support passes: automatic fixture signer extraction, routing and all fresh hooks. This is not a pristine stock APK or evidence of Google/Zalo backend acceptance. The fixture and key are not published.
5. All three Zalo outputs pass full dexlib2 checks: 50829 unique classes, five Zalo runtime classes, four isolated store literals and exactly two Zalo support hooks. No Messenger runtime class, duplicate class, diagnostic reference, added extension Activity, extension UI/log/preferences call or broken invoke/move-result adjacency.
6. For the upgraded uploaded input, decoded resource values match exactly. All 9532 audited non-DEX archive entries, including native libraries/assets, are byte-identical. Decoded manifest differs only by explicitly setting the existing Firebase IID receiver enabled=true (its prior default was enabled).
7. Messenger-only selection still patches/rebuilds the uploaded Messenger 0.4 input in STRIP_SAFE mode. Its existing 0.5 checker passes: 128761 unique classes, three isolated store literals and three support hooks. Zalo runtime is in a separate DEX and not merged by Messenger support.

Machine-readable patching reports and Zalo archive audit are under `validation/`. Earlier details are in `validation/0.4.md` and `validation/0.5.md`.

Not verified: device lifecycle/runtime behavior, microG registration on the phone, Zalo/Meta server token acceptance, push delivery/latency, device patcher 1.14.1, pristine stock inputs or arbitrary combinations of other patch sources. Local token handoff is not a server ACK. Check repository Actions for remote build results.
