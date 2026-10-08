# DTinh MicroG FCM Patches

Bộ bản vá Morphe dùng **MicroG-RE** (`app.revanced.android.gms`) cho từng ứng dụng. Từ **0.6.0 không còn bản vá phổ quát**. Một file `.mpp` chứa hai mục riêng, Manager lọc theo package/version:

| Bản vá | Ứng dụng hỗ trợ |
| --- | --- |
| Zalo microG FCM support | `com.zing.zalo` — `26.08.01` |
| Messenger microG FCM support | `com.facebook.orca` — `573.0.0.44.88` |

Chỉ chọn bản vá của app đang vá. Mỗi mục tự chạy dependency nội bộ; không cần vá phổ quát trước. Không thêm UI, launcher, màn hình chẩn đoán hoặc nhật ký riêng vào app.

## Tải và sử dụng

[Tải DTinh-MicroG-FCM-0.6.0.mpp](releases/DTinh-MicroG-FCM-0.6.0.mpp), nhập file vào Morphe và thay nguồn DTinh cũ. Đây là bộ bản vá, không phải APK ứng dụng.

- **Zalo:** chọn `Zalo microG FCM support` cho đúng phiên bản. Có thể dùng APK đã chuyển tuyến như mẫu đã kiểm tra, hoặc vá lại đầu ra 0.6. Không chọn đồng thời các bản microG cũ của nguồn khác.
- **Messenger:** chọn `Messenger microG FCM support`. Hỗ trợ nâng cấp đầu vào 0.4/0.5; đầu vào còn UI từ 0.2/0.3 bị chặn, cần APK trước các bản đó.
- Dùng đúng MicroG-RE, bật đăng ký thiết bị và Cloud Messaging. Sau khi cài APK, mở app và đăng nhập, rồi kiểm tra danh sách Cloud Messaging của microG và thử nhận tin nhắn lúc app ở nền.

Bản vá giữ thông tin chữ ký spoof đã có nếu hợp lệ; với APK chưa chuyển tuyến, nó đọc chữ ký đầu vào. Khi dùng APK đã bị ký lại nhưng chưa có metadata, cần quay lại APK gốc để tránh spoof nhầm chữ ký. Không hỗ trợ package/version khác bằng cách ép tương thích.

Build/vá thành công hoặc token được bàn giao cho app chưa xác nhận máy chủ Zalo/Meta chấp nhận token hay nhận push trên máy. Xem phạm vi kiểm tra tại [VALIDATION.md](VALIDATION.md).

## Zalo

Tách kho token Firebase và phần đọc token cũ sang `dtinh.microg.zalo.fcm.v1`, giữ dữ liệu tài khoản và logic chống đăng ký trùng của Zalo. Runtime chờ đăng nhập, listener sẵn sàng và kênh Firebase đang hoạt động; dùng provider và luồng submit token gốc của Zalo.

Nếu chưa bàn giao được token, runtime retry khi app ở tiền cảnh với backoff 20/60/120/300 giây, tối đa một worker. Khi app chuyển nền, lịch retry bị hủy; yêu cầu SDK đang chạy có thể hoàn tất. Sau bàn giao token, runtime ngừng thử cho tài khoản đó; đổi tài khoản có thể đăng ký lại. Không ép chuyển kênh Huawei sang Firebase, không xóa token tài khoản, không tắt các kênh push khác và không ghi token ra nhật ký riêng.

Đường FCM chưa chuyển tuyến chỉ được sửa trong các lớp transport/helper đã xác định của Zalo 26.08.01. Không phụ thuộc vào số lần thay chuỗi trong lớp auth `o9/a` như lỗi của bản vá cũ.

## Messenger

Giữ runtime 0.5: kho IID riêng, wrapper PendingIntent, retry ở tiền cảnh và mốc bàn giao token vào luồng Messenger sẵn có. Không thay luồng MQTT/FBNS sẵn có. Runtime DEX của Messenger và Zalo được đóng gói riêng, chỉ phần cần thiết của app đang vá được merge.

Kết nối FCM/TLS do microG quản lý. Các runtime hỗ trợ đăng ký token, không tạo socket, heartbeat, alarm hay service nền riêng. Mốc bàn giao token là trạng thái cục bộ, không phải ACK máy chủ.

## Build và cập nhật

Linux/WSL, **JDK 21**, Python **3.10+** và Internet. Đặt `JAVA_HOME` tới JDK 21 rồi chạy:

```bash
bash build.sh
```

Script chạy kiểm tra retry policy, tải công cụ ghim SHA-256 từ nguồn chính thức, build hai runtime DEX và đóng gói `dist/DTinh-MicroG-FCM-0.6.0.mpp`. Chạy kiểm tra APK theo [tests/README.md](tests/README.md).

Sửa mã nguồn, tăng `VERSION`, cập nhật `CHANGELOG.md`, rồi push `main`. [GitHub Actions](https://github.com/tinhtinh1908/Morphe-RE/actions) build artifact `microg-fcm-mpp`. Để phát hành [GitHub Release](https://github.com/tinhtinh1908/Morphe-RE/releases), push tag khớp `VERSION`:

```bash
git tag v0.6.0
git push origin v0.6.0
```

Workflow kiểm tra tag, build và đính kèm `.mpp`; chỉ job phát hành có quyền ghi release. Morphe có thể dùng repo làm nguồn khi đã có Release tương thích.

## Mã nguồn

- `src/zalo`: bản riêng Zalo và kiểm tra layout.
- `src/messenger`: bản riêng Messenger và kiểm tra layout.
- `src/shared`: routing dependency nội bộ, chỉ chấp nhận hai package/version đã nêu.
- `extension/vn/dtinh/zalo`, `extension/vn/dtinh/messenger`: runtime và retry policy.
- `scripts`: chuẩn bị công cụ/đóng gói; `tests`: kiểm tra cấu trúc và fixture.
- `validation`: báo cáo kiểm tra; `releases`: các file `.mpp` đã build.

Khi thay đổi implementation runtime trong phiên bản sau, dùng descriptor runtime mới và cập nhật hook tương ứng: Morphe có thể giữ implementation cũ khi merge class trùng tên. Cần kiểm tra cả vá mới và nâng cấp.

Repo không kèm APK ứng dụng, SDK hoặc khóa ký. Mã nguồn bộ bản vá dùng GPL-3.0-only, xem `LICENSE-patches`; `LICENSE` Apache 2.0 có sẵn được giữ nguyên và không thay giấy phép của bộ bản vá. Dự án thử nghiệm độc lập.
