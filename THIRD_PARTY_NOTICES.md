# Nguồn và giấy phép bản vá bên thứ ba

## Bỏ qua kiểm tra chỉnh sửa khi khởi động Zalo

- Tác giả/nguồn: **zeldrisho**, dự án **Zeldris Patches**.
- Kho nguồn: https://github.com/zeldrisho/morphe-patches
- Commit tham chiếu: `9418386935c67b382a58cbac7084eb67bc20ae86`.
- Tệp gốc: https://github.com/zeldrisho/morphe-patches/blob/9418386935c67b382a58cbac7084eb67bc20ae86/patches/src/main/kotlin/com/zeldrisho/patches/zalo/native/BypassNativeStartupTamperPatch.kt
- Giấy phép tại commit này: **GNU GPL, phiên bản 3**, bản đầy đủ tại [LICENSE-zeldris](LICENSE-zeldris). Tệp gốc không có thông báo bản quyền cá nhân; không bổ sung thông báo giả thay tác giả.
- Bản chuyển vào Morphe-RE: `src/zalo/BypassNativeStartupTamperPatch.kt`, thuộc phần bản vá GPL-3.0-only của dự án; giấy phép Apache của hạ tầng không được dùng để cấp lại giấy phép cho mã này.
- Thay đổi của DTinh ngày 08/10/2026: chuyển package Kotlin, thay metadata tương thích để dùng với Patcher 1.14, Việt hóa tên/mô tả, tắt chọn mặc định, cho phép hai vị trí đã là NOP để vá lại an toàn. Giữ nguyên offsets và byte kiểm tra cấu hình.
- Phạm vi: Zalo `com.zing.zalo`, phiên bản `26.08.01`, thư viện `lib/arm64-v8a/libnative_utils.so`. Kiểm tra ba vị trí trước khi ghi; chỉ sửa nhánh tại `0x2812c` và lệnh JNI tại `0x2c4ec`; giữ nguyên lệnh cấu hình tại `0x28160`.

MPP đính kèm thông báo này, giấy phép GPL của nguồn và của dự án. Mã nguồn tương ứng có trong tag phát hành của https://github.com/tinhtinh1908/Morphe-RE. Bản vá được chọn độc lập với bản vá microG cho Zalo. Build và kiểm thử dữ liệu giả không chứng minh ứng dụng sẽ khởi động trên mọi thiết bị.


## Bản đồ nguồn của release 0.7.0

| Thành phần trong repo | Tác giả/nguồn và cách sử dụng | Giấy phép / nguồn |
| --- | --- | --- |
| `src/zalo/BypassNativeStartupTamperPatch.kt` | Chuyển và sửa từ zeldrisho; không phải DTinh viết từ đầu | GPL v3, commit và tệp gốc ở trên; giữ NOTICE-zeldris |
| `src/shared/MicroGRoute.kt` | DTinh triển khai chuyển manifest/literal và metadata chữ ký. Lịch sử bắt đầu ở 4af058de3acb4adef8d7025b6b4d3e4e5f5ed9f1, chuyển tên ở 951157a2ccd1b159d3c0bde6520b0ec177bdfecd | GPL-3.0-only; tương thích API MicroG-RE/ReVanced và Morphe Patcher |
| `src/messenger/MessengerRepairPatch.kt`, `extension/vn/dtinh/messenger/` | DTinh triển khai hook đăng ký, retry/backoff và policy; lịch sử 4af058d / b8406c5 | GPL-3.0-only |
| `src/zalo/ZaloMicroGPatch.kt`, `extension/vn/dtinh/zalo/` | DTinh triển khai FCM riêng Zalo, tái sử dụng policy Messenger; lịch sử 951157a | GPL-3.0-only |
| API patcher `app.morphe.patcher.*` | MorpheApp và các contributors; thư viện compile/runtime ngoài MPP | [morphe-patcher](https://github.com/MorpheApp/morphe-patcher), GPL v3, NOTICE-morphe-patcher |
| API smali/dexlib2 `com.android.tools.smali.*` | Google và smali contributors; dependency ngoài MPP | [google/smali](https://github.com/google/smali), xem LICENSE/NOTICE từng module upstream |
| Cơ chế GmsCore/microG, metadata `app.revanced.MICROG_PACKAGE_NAME`, namespace `app.revanced` | Tham chiếu tương thích giao thức; không kèm source/binary GmsCore trong MPP | [MicroG-RE 7.2.1](https://github.com/MorpheApp/MicroG-RE/tree/7.2.1), Apache-2.0; [microg/GmsCore](https://github.com/microg/GmsCore); [ReVanced patches](https://github.com/ReVanced/revanced-patches) GPL v3 |
| Morphe patches | Nguồn tham khảo hệ sinh thái GmsCore; không tuyên bố toàn bộ mã FCM được chép từ dự án này | [MorpheApp/morphe-patches](https://github.com/MorpheApp/morphe-patches), GPL v3; NOTICE-morphe-patches được bảo toàn |

Các thông tin DTinh ở bảng căn cứ lịch sử mã trong repo; không gán tác giả upstream cho những tệp không có chứng cứ sao chép. Với bản native có tệp gốc xác định, nguồn và sửa đổi được khai báo trực tiếp ở đầu tệp. Không có Gmail trong gói chính thức; nghiên cứu và bản vá Gmail chỉ ở kênh thử nghiệm `dev`.

## Công cụ build, không phân phối trong MPP

| Công cụ ghim | Chủ dự án/nguồn | Giấy phép |
| --- | --- | --- |
| Morphe Desktop 1.18.0 (fat JAR) | [MorpheApp/morphe-desktop](https://github.com/MorpheApp/morphe-desktop/tree/v1.18.0) | GPL v3 và NOTICE upstream; các thành phần trong fat JAR có giấy phép riêng |
| Kotlin compiler 2.3.10 | [JetBrains/kotlin](https://github.com/JetBrains/kotlin/tree/v2.3.10) | Apache-2.0 và thông báo dependency đi kèm |
| JetBrains annotations 26.0.2 | [JetBrains/java-annotations](https://github.com/JetBrains/java-annotations) | Apache-2.0 |
| Android API 35 / Build Tools 35 | Google, tải từ dl.google.com | Điều khoản Android SDK và giấy phép từng công cụ; không cấp lại toàn bộ SDK theo Apache |
| D8/R8 | [Google R8](https://r8.googlesource.com/r8/) | BSD-3-Clause và giấy phép dependency |
| OpenJDK 21 | OpenJDK / Eclipse Temurin khi build CI | GPL v2 với Classpath Exception; chỉ dùng chạy compiler |

URL tải, phiên bản và SHA-256 công cụ: `tools.lock.json`. Source archive không chứa các công cụ, APK ứng dụng, khóa ký, font hoặc icon. Messenger/Zalo là ứng dụng sở hữu riêng của Meta/VNG; giấy phép bộ bản vá không cấp quyền phân phối lại các APK đó.

## NOTICE và nguồn tương ứng

Giữ nguyên NOTICE-zeldris (commit gốc); bảo toàn thêm NOTICE-morphe-patches và NOTICE-morphe-patcher được kiểm tra ngày 14/10/2026. Đây là thông báo upstream, không phải tuyên bố DTinh sở hữu Morphe. Tên sản phẩm trong manifest/release là DTinh MicroG FCM Patches. Tên kho hiện có là địa chỉ lưu trữ lịch sử, không dùng làm nhãn hiệu sản phẩm.

MPP đóng kèm LICENSE, LICENSE-patches, LICENSE-zeldris, NOTICE-*, LICENSING.md và tài liệu này. Release cung cấp source ZIP đúng commit, kèm hướng dẫn build và SHA-256. Người phân phối phải duy trì quyền truy cập mã nguồn tương ứng cùng bản nhị phân.


## Chặn cập nhật CH Play (0.7.1)

- Nguồn: MorpheApp/morphe-patches, commit `e5464395441e916b643af4e2175c4e253e6df487`.
- Tệp Kotlin gốc: `patches/src/main/kotlin/app/morphe/patches/all/misc/updates/DisablePlayStoreUpdatesPatch.kt`.
- Tệp Java gốc: `extensions/all/change-version-code/src/main/java/app/morphe/extension/all/versioncode/DisablePlayStoreUpdatesPatch.java`.
- Copyright 2026 Morphe; GPL v3 cùng thông báo Section 7 ở đầu tệp. Đã giữ nguyên toàn bộ khối thông báo trong các bản chuyển và đóng bản source vào release.
- Nguồn trước đó do Morphe ghi nhận: ReVanced/revanced-patches @ `724e6d61b2ecd868c1a9a37d465a688e83a74799`, `patches/src/main/kotlin/app/revanced/patches/all/misc/versioncode/ChangeVersionCodePatch.kt`. Liên kết nguồn được giữ nguyên trong đầu tệp Kotlin.
- Bản chuyển: `src/shared/DisablePlayStoreUpdates.kt`, `extension/vn/dtinh/updates/PlayStoreUpdates.java`.
- DTinh sửa ngày 14/10/2026: chuyển package/API về Patcher 1.14; ghép thành dependency ẩn của Messenger/Zalo; giữ mã gốc trong manifest để vá lại; chỉ khôi phục khi PackageInfo thuộc hai app; hỗ trợ invoke/range; không dùng AndroidX. Giữ versionCodeMajor.
- Cơ chế: versionCode ngoài manifest là 2147483647; các lệnh đọc PackageInfo.versionCode/getLongVersionCode được chuyển qua helper trả mã gốc cho app. Không đổi versionName và không ngăn lời nhắc cập nhật nội bộ.
- Chặn cập nhật theo versionCode từ CH Play; không áp dụng cho app cài bằng root mount. Có thể cần gỡ app trước khi cài bản mã phiên bản thấp hơn; gỡ app có thể mất dữ liệu cục bộ.
