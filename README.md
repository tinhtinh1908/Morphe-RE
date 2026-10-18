# DTinh — Bản vá thông báo

Bản phát hành **0.7.3** gồm Messenger, Zalo và **Gmail thử nghiệm**.

| Mục chọn | Chức năng |
| --- | --- |
| Thông báo Messenger qua microG | Nhận thông báo qua MicroG-RE; Messenger 573.0.0.44.88 |
| Thông báo Zalo qua microG | Nhận thông báo qua MicroG-RE; Zalo 26.08.01 |
| Gmail qua microG (thử nghiệm) | Đăng nhập và nhận thông báo qua MicroG-RE; Gmail 2026.09.21.992487546.Release |
| Chặn cập nhật CH Play | Mục riêng, tùy chọn; giữ bản đã vá bằng cơ chế mã phiên bản của Morphe |
| Sửa lỗi Zalo tự thoát | Bỏ kiểm tra chống chỉnh sửa khi mở; Zalo 26.08.01 ARM64 |

[Tải bản phát hành](https://github.com/tinhtinh1908/Morphe-RE/releases/tag/v0.7.3). Nhập MPP hoặc cập nhật nguồn repo trong Manager. Cài MicroG-RE và đăng nhập ứng dụng để dùng bản vá thông báo. Không chọn trùng mục chặn cập nhật từ nguồn Morphe khác. Mục này không chặn lời nhắc cập nhật nội bộ và không dùng cho cài bằng root mount.

Nguồn và giấy phép: THIRD_PARTY_NOTICES.md, LICENSING.md, LICENSE-patches và NOTICE-*. Giữ nguyên ghi công Morphe/ReVanced/Zeldris. Release đính kèm source ZIP và SHA-256; MPP cũng chứa mã nguồn trong `source/`.

Build trên Linux/WSL với JDK 21 và Python 3.10+: đặt JAVA_HOME rồi chạy `bash build.sh`. Công cụ tải theo tools.lock.json và kiểm tra SHA-256. Kiểm thử policy đăng ký, native Zalo, chỉnh DEX đọc mã phiên bản, helper runtime và audit gói được chạy khi build.

Chưa xác nhận thông báo, cài đặt và cập nhật CH Play trên thiết bị thực trong phiên phát hành này. Các báo cáo trong validation/ chỉ áp dụng đúng bản ghi trong từng báo cáo.

Gmail là bản vá **thử nghiệm**, không chọn mặc định và chưa bảo đảm thông báo ổn định trên mọi máy. Giữ nguyên gói `com.google.android.gm`; dùng APK đã hợp nhất đầy đủ các split, không dùng riêng `base.apk`. Không hỗ trợ các phiên bản Gmail khác. Mục chặn cập nhật CH Play hỗ trợ cả Messenger, Zalo và phiên bản Gmail nêu trên; chọn thêm mục này khi vá để giữ bản đã vá.
