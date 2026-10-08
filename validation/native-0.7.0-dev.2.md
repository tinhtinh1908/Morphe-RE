# Kiểm chứng 0.7.0-dev.2

- Build JDK 21 + Kotlin 2.3.10 + Morphe Desktop 1.18.0 / Patcher 1.14.0: PASS.
- MPP: `DTinh-MicroG-FCM-0.7.0-dev.2.mpp`; SHA-256: `1d09b1dcf76c617cfb26f41bd07744ec99ad12c0ff47e2c518d19f123a66fbdd`.
- `list-patches`: 4 bản vá công khai, tắt chọn mặc định. Zalo có 2 bản độc lập; native không có phụ thuộc microG.
- Toàn bộ 4 mô tả: tiếng Việt. 3 tên microG giữ nguyên để không thay danh tính lựa chọn hiện có.
- Native: kiểm thử mới, lặp lại, 2 trạng thái đã vá một phần, thay đổi từng vị trí (3 ca), file thiếu byte. So sánh toàn bộ byte đầu ra: PASS; chỉ 2 lệnh được đổi thành NOP, lệnh cấu hình giữ nguyên. Ca không khớp không sửa byte nào.
- Kiểm thử RegistrationPolicy hiện có: PASS.
- MPP chứa LICENSE-patches, LICENSE-zeldris và THIRD_PARTY_NOTICES.md, khớp từng byte với nguồn.
- Nguồn native: zeldrisho/morphe-patches @ 9418386935c67b382a58cbac7084eb67bc20ae86, GPL v3.
- Giới hạn: dữ liệu native giả lập; chưa chạy vá APK Zalo thực tế ở phiên này và chưa kiểm chứng khởi động, giải mã hoặc nhận push trên thiết bị. Logic FCM không thay đổi so với dev.1. Gmail tiếp tục thử nghiệm.
