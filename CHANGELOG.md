# Changelog

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
