# Gmail 0.7.0-dev.1 validation

Experimental release; no Android device or mail-server test has been performed.

- Pinned JDK 21/Kotlin/Morphe build succeeded. Policy tests passed.
- Bundle exposes exactly three public app-scoped patches: Gmail, Messenger, Zalo. Gmail is disabled by default and compatible only with com.google.android.gm 2026.09.21.992487546.Release.
- Uploaded APKS was truncated in workspace. Recovered the complete first ZIP local entry (base.apk), validating decompression size and CRC; SHA-256 matches the base surveyed earlier: 8090009520cc92c98810f64991af38c04582af4294c5d7f19a603025f24b8e44. ARM64 entry is incomplete; language/density entries are unavailable.
- Derived fixture from the intact original base by removing requiredSplitTypes and invalidating its signature block. This fixture retains the original DEX/resources; it is NOT a complete merged Gmail and NOT installable output for users.
- Original standalone base is rejected by the patch's split requirement check.
- Official Morphe Desktop 1.18.0 applied only the Gmail patch to the derived fixture, STRIP_SAFE mode, and rebuilt APK/resources successfully.
- VerifyGmail checks class uniqueness, invoke/move-result adjacency, exactly 237 app.revanced account literals, six isolated-cache literals, unchanged GetToken class literal, one availability hook and one helper class, and absence of Messenger/Zalo extensions or new helper UI/log/socket calls.
- Reapplication to the fixture output also patched/rebuilt successfully; VerifyGmail passed both outputs with 71061 unique classes and exactly one helper/availability hook.

Unsigned merged input is accepted because Morphe Manager's APKS merge writes an unsigned monolithic APK. For such inputs the known original Gmail signer is pinned from the surveyed original and structural guards are enforced; this is not cryptographic verification of an unsigned merged input. Other signed inputs need the measured original signer or valid existing routing metadata.

Input: choose the FULL original APKS in Morphe Manager, which merges its splits, then select only Gmail microG support (experimental). Do not select base.apk alone and do not clear split requirements manually for installation. Keep com.google.android.gm, original Gmail-owned authorities/permissions, server scopes and project IDs. Personal-account support is experimental; work accounts are not validated.

FCM/Chime registration, token refresh, account visibility, real mail read/send, background notifications, complete split merge/install and OEM behavior remain unverified. Successful patching and local token routing do not establish delivery or server acceptance.
