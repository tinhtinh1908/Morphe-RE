# Changelog

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
