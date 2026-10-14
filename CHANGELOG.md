# Changelog

## 0.7.1

- Thay thế release 0.7.0 đã thu hồi theo yêu cầu chủ repo.
- Tự chặn cập nhật từ CH Play trong hai mục microG Messenger/Zalo, theo cơ chế của Morphe; giữ mã phiên bản gốc khi app đọc và hỗ trợ vá lại.
- Viết lại mô tả cả ba mục bằng tiếng Việt dễ hiểu.
- Giữ nguyên nguồn Morphe/ReVanced, copyright, giấy phép và khối NOTICE từng tệp; bổ sung kiểm thử mã phiên bản và runtime.
- Chỉ Messenger/Zalo; Gmail tiếp tục thử nghiệm. Chưa kiểm thử thông báo, cài đặt và CH Play trên thiết bị ở phiên này.

## 0.7.0

- Phát hành chính thức cho Messenger 573.0.0.44.88 và Zalo 26.08.01; không chứa Gmail.
- Zalo có hai mục: microG FCM và native startup độc lập (ARM64), nguồn zeldrisho/morphe-patches.
- Hoàn thiện nguồn, ghi công, giấy phép và NOTICE; đóng kèm trong MPP.
- Đính kèm mã nguồn tương ứng, công cụ build được ghim và SHA-256.
- Logic FCM giữ nguyên 0.6.0; native giữ nguyên dev.2. Kiểm thử policy/native và kiểm tra gói khi build; chưa kiểm thử khởi động/nhận push trên thiết bị ở phiên phát hành này.

## 0.6.0

- Bỏ mục vá phổ quát; chỉ còn hai bản vá riêng theo package/version: Zalo và Messenger.
- Thêm Zalo 26.08.01: chuyển tuyến FCM có phạm vi, tách kho token và retry luồng đăng ký sẵn có sau đăng nhập.
- Giữ cơ chế chống đăng ký trùng của Zalo; không thêm UI/socket/service và không ép đổi kênh push.
- Runtime DEX của hai app được đóng gói riêng, chỉ merge phần của app đang vá.
- Kiểm tra vá mới, vá lại, fixture Zalo và hồi quy Messenger.

## 0.5.0

- Sửa lỗi thử đăng ký một lần rồi dừng dù thất bại ở 0.4.
- Retry khi ở tiền cảnh với backoff tối đa 5 phút, một worker và hỗ trợ đổi tài khoản.
- Dùng callback bàn giao token của Messenger để dừng retry; không coi boolean C2E là thành công.
- Hỗ trợ nâng cấp 0.4 và vá lại 0.5, không nhân đôi hook hoặc đổi kho token.
- Kiểm tra chữ ký các method/session trước khi vá; giữ thứ tự invoke/move-result.
- Thêm kiểm tra policy tự chạy khi build và kiểm tra DEX/fixture cho ba hook.

## 0.4.0

- Bỏ UI chẩn đoán, launcher và nhật ký khỏi phần Messenger.
- Giữ hỗ trợ đăng ký FCM qua MicroG-RE, kho token riêng và cờ PendingIntent tương thích.
- Mục Messenger tự chạy bản tổng quát trước thông qua dependency.

## 0.3.0

- Sửa thứ tự dependency giữa bản tổng quát và phần Messenger.
- Nhận biết đầu vào đã chuyển tuyến và giữ metadata chữ ký gốc.

## 0.2.0

- Gộp bản tổng quát và phần Messenger vào một file MPP, lọc theo package/version.

## 0.1.0

- Bản tổng quát chuyển các tuyến FCM được nhận biết sang MicroG-RE.
