# Kiểm thử

## Kiểm tra tự động khi build

```sh
export JAVA_HOME=/path/to/jdk-21
bash build.sh
```

Build chạy RegistrationPolicy, native startup Zalo, biến đổi versionCode, runtime chặn cập nhật, audit giấy phép/extension và liệt kê bản vá trong MPP. Policy kiểm tra thiếu phiên đăng nhập, chống chạy trùng, backoff, token handoff và đổi tài khoản; không mô phỏng Android lifecycle hoặc máy chủ push.

## Kiểm tra APK đã vá

Sau khi build, biên dịch các checker:

```sh
mkdir -p build/test-classes
"$JAVA_HOME/bin/javac" -cp .tools/morphe-desktop-1.18.0-all.jar \
  -d build/test-classes tests/Verify*.java
export PATCH_TEST_CP=build/test-classes:.tools/morphe-desktop-1.18.0-all.jar
```

Facebook và Messenger:

```sh
"$JAVA_HOME/bin/java" -Xmx1280m -cp "$PATCH_TEST_CP" VerifyFacebookRpc /path/facebook-patched.apk
"$JAVA_HOME/bin/java" -Xmx1280m -cp "$PATCH_TEST_CP" VerifyMetaDex /path/facebook-patched.apk facebook
"$JAVA_HOME/bin/java" -Xmx1280m -cp "$PATCH_TEST_CP" VerifyMetaDex /path/messenger-patched.apk messenger
python3 tests/VerifyMetaManifest.py /path/aapt \
  /path/facebook-input.apk /path/messenger-input.apk \
  /path/facebook-patched.apk /path/messenger-patched.apk
```

Các checker kiểm tra route IPC, hook/cache, lớp trùng, invoke/move-result, tách quyền và bảo toàn package/component/authority/native. Kiểm tra manifest xét cả hai thứ tự cài khi một app còn là bản gốc và khi cả hai đã vá; không thay thế trình cài Android.

Zalo và Messenger:

```sh
"$JAVA_HOME/bin/java" -Xmx1280m -cp "$PATCH_TEST_CP" VerifyZalo /path/zalo-patched.apk
"$JAVA_HOME/bin/java" -Xmx1280m -cp "$PATCH_TEST_CP" VerifyNoUi /path/messenger-patched.apk
```

Gmail có `VerifyGmail`, `VerifyGmailRouting`, `VerifyGmailManifest`; xem tham số đầu vào tại hàm `main` của từng checker trước khi chạy. Không đưa APK, tài khoản, token hoặc khóa ký fixture vào repo.

## Kiểm tra trên thiết bị

- Cài/cập nhật bằng cùng khóa ký; thử cài chung Facebook và Messenger.
- Đăng nhập và kiểm tra ứng dụng xuất hiện trong Cloud Messaging của microG.
- Thử nhận thông báo ở foreground, background, sau khi tắt màn hình và sau khởi động lại.
- Gmail: kiểm tra gửi/nhận, đồng bộ nền và đổi tài khoản.
- Đối chiếu log theo thời điểm lỗi; phân biệt lấy token cache, đăng ký với microG và gửi token lên máy chủ ứng dụng.

`tests/build-fixture.sh` chỉ tạo fixture kiểm tra từ APK đã chuyển hướng; không khôi phục APK gốc. Ký fixture bằng khóa tạm riêng. Báo cáo trong `validation/` ghi rõ phiên bản, đầu vào và phạm vi đã xác minh.
