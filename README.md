# DTinh MicroG FCM Patches

Bộ bản vá Morphe chuyển FCM sang **MicroG-RE** (`app.revanced.android.gms`). Một file `.mpp` chứa bản tổng quát và phần dành riêng cho Messenger.

| Bản vá | Phạm vi |
| --- | --- |
| FCM via MicroG-RE (experimental) | Ứng dụng có tuyến FCM được nhận biết; cần kiểm tra tương thích từng ứng dụng |
| Messenger microG FCM support | `com.facebook.orca`, phiên bản `573.0.0.44.88` |

Bản Messenger tự phụ thuộc vào bước tổng quát, nên chọn một mục là thực hiện đủ trong một lượt. Không thêm UI, launcher, nút hay nhật ký vào Messenger.

## Tải bản vá

Repository: https://github.com/tinhtinh1908/Morphe-RE

File 0.4 đã build: [DTinh-MicroG-FCM-0.4.mpp](releases/DTinh-MicroG-FCM-0.4.mpp). Đây là bộ bản vá để nhập vào Morphe, không phải APK ứng dụng.

Bản build mới có trong artifact `microg-fcm-mpp` tại [GitHub Actions](https://github.com/tinhtinh1908/Morphe-RE/actions). Các bản phát hành theo tag nằm tại [Releases](https://github.com/tinhtinh1908/Morphe-RE/releases).

## Dùng trong Morphe

- Thay nguồn DTinh cũ bằng file `.mpp` mới.
- Với Messenger đúng phiên bản, chọn **Messenger microG FCM support**.
- Với ứng dụng khác, chọn bản tổng quát để thử nghiệm.
- Dùng APK gốc hoặc APK trước khi áp dụng bản Messenger có UI ở 0.2/0.3. Bản vá sẽ chặn đầu vào còn UI/hook cũ.
- Mở Messenger, đăng nhập rồi kiểm tra Cloud Messaging trong microG.

Cần cài đúng MicroG-RE và bật đăng ký thiết bị/Cloud Messaging. Build hoặc vá thành công chưa chứng minh máy chủ Meta chấp nhận token hay nhận push trên máy. Bản vá giữ các đường MQTT/FBNS sẵn có.

## Build tại máy

Dùng Linux hoặc WSL, **JDK 21**, Python **3.10 trở lên** và mạng Internet. Đặt `JAVA_HOME` tới JDK 21, sau đó chạy từ gốc repo:

```bash
bash build.sh
```

Lần đầu script tự tải các công cụ được ghim trong `tools.lock.json` từ nguồn chính thức và kiểm tra SHA-256. Công cụ tải về nằm trong `.tools`, đầu ra ở `dist/DTinh-MicroG-FCM-0.4.0.mpp`. Script không phụ thuộc workspace của cuộc trò chuyện.

## Update và phát hành

1. Sửa mã nguồn trong `src` hoặc phần runtime trong `extension`.
2. Tăng phiên bản trong **`VERSION`**; cập nhật `CHANGELOG.md`.
3. Push vào `main`: Actions build bản mới, tải `.mpp` tại artifact của lượt chạy.
4. Để tự phát hành GitHub Release, tạo và push tag trùng phiên bản, ví dụ `v0.4.0`:

```bash
git tag v0.4.0
git push origin v0.4.0
```

Workflow kiểm tra tag khớp `VERSION`, build, rồi tạo Release và đính kèm `.mpp`. Chỉ job phát hành theo tag có quyền `contents: write`; không cần tự thêm token. Workflow cũng có nút chạy thủ công trong Actions.

Bạn cũng có thể tạo Release bằng giao diện GitHub và đính kèm file `.mpp` đã build. Khi có Release, dùng URL repository của bạn làm nguồn GitHub trong Morphe nếu Manager hỗ trợ nguồn repository.

## Cấu trúc

- `src/general`: bản chuyển tuyến FCM tổng quát.
- `src/messenger`: phần hỗ trợ Messenger, giới hạn package/version.
- `extension/vn/dtinh/messenger`: runtime FCM, không có màn hình chẩn đoán.
- `scripts`: chuẩn bị công cụ và đóng gói MPP.
- `tests`: mã kiểm tra cấu trúc và tạo fixture; không chứa APK mẫu.
- `.github/workflows/build.yml`: build trên push/PR/manual và release theo tag.
- `releases`: bản `.mpp` 0.4 đã cung cấp.
- `VALIDATION.md`: phạm vi đã kiểm tra và giới hạn.

Không kèm APK Messenger, keystore hoặc công cụ SDK trong repo. Mã nguồn bộ bản vá được cung cấp theo GPL-3.0-only, xem `LICENSE-patches`. File `LICENSE` Apache 2.0 có sẵn của repository được giữ nguyên; nó không thay thế giấy phép của mã nguồn bộ bản vá. Đây là dự án thử nghiệm độc lập.
