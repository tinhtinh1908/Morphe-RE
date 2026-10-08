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
