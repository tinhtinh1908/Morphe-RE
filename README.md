# DTinh MicroG FCM Patches

Bộ bản vá độc lập tương thích với Morphe Manager. Bản chính thức **0.7.0** chỉ hỗ trợ **Messenger và Zalo**. Gmail nằm ở nhánh `dev` và các Pre-release, chưa phát hành chính thức.

| Ứng dụng | Phiên bản | Mục chọn |
| --- | --- | --- |
| Messenger (`com.facebook.orca`) | 573.0.0.44.88 | Messenger microG FCM support |
| Zalo (`com.zing.zalo`) | 26.08.01 | Zalo microG support |
| Zalo ARM64 | 26.08.01 | Bỏ qua kiểm tra chỉnh sửa khi khởi động Zalo, chọn độc lập |

[Tải release chính thức](https://github.com/tinhtinh1908/Morphe-RE/releases/tag/v0.7.0). Một MPP chứa ba mục vá cho hai ứng dụng; không chứa Gmail. Nhập file MPP hoặc dùng nguồn GitHub của repo trong Manager, chọn mục đúng app/version. Cài MicroG-RE (`app.revanced.android.gms`), bật đăng ký thiết bị và Cloud Messaging, đăng nhập app rồi kiểm tra nhận thông báo. Mục native chỉ dùng đúng Zalo ARM64 đã ghim; không tự chọn mặc định.

Nguồn, tác giả, thay đổi và thư viện: [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Phạm vi GPL/Apache: [LICENSING.md](LICENSING.md). Bản native được chuyển từ Zeldris Patches, GPL v3; các NOTICE upstream được giữ nguyên cả trong source lẫn MPP.

Build: Linux/WSL, JDK 21, Python 3.10+, đặt JAVA_HOME rồi chạy `bash build.sh`. Script tải công cụ theo `tools.lock.json`, kiểm tra SHA-256, chạy kiểm thử policy/native và kiểm tra gói. Đầu ra `dist/DTinh-MicroG-FCM-0.7.0.mpp`. Release đính kèm source ZIP và SHA256SUMS.

Build/kiểm thử cấu trúc không chứng minh khởi động hoặc nhận push trên thiết bị; các báo cáo cũ trong `validation/` giữ phạm vi phiên bản được ghi ở từng báo cáo. Không kèm APK ứng dụng. Đây không phải sản phẩm do Morphe, Zeldris, microG, Meta hay VNG phát hành.
