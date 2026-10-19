# DTinh MicroG FCM Patches

Bộ bản vá Morphe chuyển các luồng xác thực và thông báo được hỗ trợ sang **MicroG-RE** (`app.revanced.android.gms`). Phiên bản: **0.7.4**.

## Tương thích

| Ứng dụng | Package | Phiên bản hỗ trợ |
| --- | --- | --- |
| Facebook | `com.facebook.katana` | `573.0.0.37.74` |
| Messenger | `com.facebook.orca` | `573.0.0.44.88` |
| Zalo | `com.zing.zalo` | `26.08.01` |
| Gmail | `com.google.android.gm` | `2026.09.21.992487546.Release` |

Mỗi ứng dụng có một bản vá thông báo riêng, không chọn mặc định. Hai mục bổ sung:

- **Chặn cập nhật CH Play:** tăng versionCode theo cơ chế Morphe; hỗ trợ bốn phiên bản trên. Không dùng khi cài bằng root mount.
- **Sửa lỗi Zalo tự thoát:** bỏ kiểm tra native khi khởi động Zalo 26.08.01 ARM64.

## Sử dụng

1. Cài MicroG-RE và Morphe Manager.
2. Thêm nguồn `https://github.com/tinhtinh1908/Morphe-RE` hoặc nhập MPP từ [Releases](https://github.com/tinhtinh1908/Morphe-RE/releases/latest).
3. Chọn đúng APK và bản vá thông báo tương ứng. Gmail cần APK đã hợp nhất đủ các split.
4. Cài APK đầu ra, mở ứng dụng và đăng nhập. Kiểm tra đăng ký trong Cloud Messaging của microG và thử nhận thông báo.

Facebook/Messenger tách hai quyền chữ ký dùng chung để tránh xung đột cài đặt. Nếu bản cũ còn gây xung đột, vá lại ứng dụng báo lỗi bằng 0.7.4. Khi cập nhật APK đã ký lại, dùng cùng khóa ký với bản đã cài.

## Build

Yêu cầu Linux/WSL, **JDK 21**, Python **3.10+**; Morphe Patcher API **1.14.0** trở lên.

```sh
export JAVA_HOME=/path/to/jdk-21
bash build.sh
```

Đầu ra: `dist/DTinh-MicroG-FCM-0.7.4.mpp`. Dependencies được ghim phiên bản và SHA-256 trong `tools.lock.json`; build tự chạy các kiểm thử nội bộ và audit gói.

## Tài liệu

- [Kiến trúc và phạm vi chỉnh sửa](docs/architecture.md)
- [Kiểm thử và xác minh APK](docs/testing.md)
- [Lịch sử phát hành](CHANGELOG.md)
- [Báo cáo kiểm chứng](validation/)

Build thành công không thay thế kiểm tra đăng nhập, đồng bộ và nhận push trên thiết bị. Báo lỗi cần kèm phiên bản app, Manager, nguồn bản vá và log của lần lỗi.

## Giấy phép

Xem [LICENSING.md](LICENSING.md), [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md), `LICENSE*` và `NOTICE*`. MPP chứa nguồn tương ứng trong `source/`; release đính kèm source ZIP và SHA-256. Không phân phối APK của ứng dụng đích trong gói.
