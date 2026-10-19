# Kiến trúc bản vá

## Cấu trúc nguồn

| Thư mục | Vai trò |
| --- | --- |
| `src/shared/` | Chuyển hướng FCM, tách quyền Meta, chặn cập nhật CH Play |
| `src/facebook/`, `src/messenger/`, `src/zalo/`, `src/gmail/` | Điểm vá và điều kiện tương thích từng ứng dụng |
| `extension/vn/dtinh/` | Helper runtime Java, đóng DEX riêng theo ứng dụng |
| `scripts/` | Tải dependencies có kiểm tra SHA-256, đóng gói MPP |
| `tests/` | Kiểm thử policy, biến đổi DEX/native và checker APK |
| `validation/` | Kết quả theo từng phiên bản; không tự áp dụng cho phiên bản khác |

## Chuyển hướng FCM

`MicroGRoute.kt` là dependency nội bộ cho Facebook, Messenger và Zalo. Nó đồng bộ action C2DM/IID, package dịch vụ, receiver permission và metadata danh tính gốc. Phạm vi lớp được giới hạn theo APK hỗ trợ. Không đổi Binder descriptor hay sender/project ID.

Cache đăng ký được tách khỏi cache Google cũ. Helper dùng luồng lấy/gửi token của ứng dụng; retry chỉ chạy khi ứng dụng ở foreground, với backoff 20/60/120/300 giây. Callback token cục bộ không được coi là xác nhận của máy chủ.

## Facebook và Messenger

`MetaCoexistence.kt` đổi hai quyền chữ ký dùng chung sang namespace riêng của từng package:

| Quyền gốc | Quyền sau vá |
| --- | --- |
| `com.facebook.permission.prod.FB_APP_COMMUNICATION` | `<package>.dtinh.permission.prod.FB_APP_COMMUNICATION` |
| `com.facebook.receiver.permission.ACCESS` | `<package>.dtinh.receiver.permission.ACCESS` |

Thay thế bao gồm khai báo/quyền component trong manifest, literal/static field và định dạng `%s` trong DEX. Giữ protectionLevel, tên package/component và provider authority. Đăng nhập chéo phụ thuộc chữ ký Meta không nằm trong phạm vi hỗ trợ.

Facebook 573 dùng cả IPC mới và IID cũ. `X.cd1.A02` và `X.fAd.run` được kiểm tra để bảo đảm đích backend là microG; giữ nhánh chọn transport và fallback. `FacebookMicroGFcmSupportV2` hỗ trợ chuyển hook/cache từ dev.1 và khôi phục cờ FCM cũ một lần khi đổi backend.

## Gmail

Gmail có dependency manifest/routing riêng. Bản vá giữ package gốc, chuyển account type cá nhân sang `app.revanced`, authority tài khoản và package dịch vụ sang MicroG-RE. Giữ tên lớp `com.google.android.gms.auth.GetToken`, descriptor `com.google.android.auth.IAuthManagerService`, OAuth scope và các luồng Gmail/Chime gốc.

Luồng xác thực cổ điển được đối chiếu với [MicroG-RE 7.2.1, commit 66a2bbac](https://github.com/MorpheApp/MicroG-RE/tree/66a2bbac7d88c3e038f1b047774fe2ae65534b6a). Không suy diễn hỗ trợ Workspace, work profile, Chat hoặc Meet từ việc đăng nhập tài khoản cá nhân hoạt động. Đầu vào phải có đầy đủ resources và thư viện từ các split.

## Build và đóng gói

`build.sh` biên dịch runtime từng app, Kotlin patch, chạy kiểm thử, tạo DEX rồi gọi `scripts/package_mpp.py`. Mỗi MPP chứa lớp JVM, DEX bản vá, DEX runtime theo ứng dụng, mã nguồn và giấy phép. Chỉ extension của bản vá được chọn được đưa vào APK đích.
