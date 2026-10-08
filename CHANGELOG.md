# Changelog

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
