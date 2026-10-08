# Nghiên cứu Gmail qua MicroG-RE

Ngày kiểm tra: 2026-10-08. Trạng thái: phân tích tĩnh, chưa có bản vá Gmail được phát hành và chưa chạy thử trên Android.

## Đầu vào đã kiểm tra

- Gói: `com.google.android.gm`.
- Phiên bản: `2026.09.21.992487546.Release`, versionCode `66054195`.
- minSdk 32, targetSdk 37 theo manifest của file gửi; targetSdk không có nghĩa là yêu cầu thiết bị chạy API 37.
- 10 DEX trong base, 4 APK trong APKS: base, arm64-v8a, vi, xxxhdpi.
- Cả bốn APK có cùng package/versionCode và cùng SHA-1 chứng thư signer v2: `38918a453d07199354f8b19af05ec6562ced5788`. Đã trích chứng thư; chưa thực hiện kiểm tra mật mã toàn bộ chữ ký APK.
- Base SHA-256: `8090009520cc92c98810f64991af38c04582af4294c5d7f19a603025f24b8e44`.
- Base khai báo requiredSplitTypes `base__abi,base__density` và splits.required=true, không có thư viện native; split arm64 chứa 9 thư viện .so. Phải bảo toàn cả bộ hoặc hợp nhất đúng resources/native. Không chỉ xóa cờ split rồi cài base.
- Báo cáo không chứa APK, thư/tài khoản hay token người dùng.

## Những điểm đã xác định trong DEX và resources

| Phần | Điểm cụ thể | Ý nghĩa |
| --- | --- | --- |
| Chọn/kiểm tra tài khoản | `Laboa;<clinit>`, `m(Account)`, `j(Context,String)`, `k(Context,String[])` | Chấp nhận com.google/com.google.work/cn.google; có thể ném Account type not supported nếu đưa account app.revanced vào mà không sửa kiểm tra |
| Lấy token xác thực | `Laboa;b/e/f`, ComponentName ở static initializer | Bind rõ package com.google.android.gms và class com.google.android.gms.auth.GetToken |
| Danh sách tài khoản | `Laboa;j`, `Laupz;c` | Dùng provider com.google.android.gms.auth.accounts và phương thức get_accounts |
| Sync thư | `MailSyncAdapterService`, resource 0x7f190073 = res/w9i.xml | Sync adapter khai báo accountType=com.google; cần khớp loại tài khoản thực tế |
| Giao thức đăng ký push | `Labvk;a(Bundle)`, `Lbtfd;f`, `Lcfrk;b`, `Lcftc;b` | Có cả c2dm REGISTER và IID TOKEN_REQUEST; kiểm tra package/action/permission của backend |
| Cache token | `Lbrjc;`, `Lcfsd;`, `Lcftl;`, `Lcftz;` | Tổng 6 const-string cho appid và appid-no-backup; phải xem vai trò từng cache trước khi tách |
| Nhận FCM | FirebaseInstanceIdReceiver | Manifest yêu cầu com.google.android.c2dm.permission.SEND, action RECEIVE |
| Xử lý push | FirebaseMessagingServiceImpl, `Laufw;a(Intent)` | Có kiểm tra action RECEIVE trong mã, không chỉ manifest |
| Đăng ký Chime theo tài khoản | `Lzlp;a`, owner `Lzlr;` | Lưu last_registration_id và last_representative_target_id, so sánh dữ liệu cũ/mới rồi requestSync(Account,authority,Bundle) |

Gmail chứa cả FCM/IID và Google Notifications/Chime. Việc hiện trong danh sách Cloud Messaging chỉ chứng minh một bước đăng ký push; không chứng minh Chime đã đăng ký tài khoản hoặc thư đã đồng bộ.

Trong 10 DEX đã đếm 237 const-string đúng bằng com.google và 73 đúng bằng com.google.android.gms. Đây là số vị trí khảo sát, không phải số thay thế được duyệt. Nhiều vị trí thuộc xác thực, Meet/Chat, kiểm tra chữ ký, Dynamite và các API khác. Không thay tất cả bằng thao tác tìm/đổi chuỗi chung; không đổi tên lớp Java/Binder descriptor, OAuth scope, URL máy chủ hay Firebase project ID.

## Đối chiếu MicroG-RE

Nguồn đối chiếu cố định:
[MicroG-RE commit 66a2bbac7d88c3e038f1b047774fe2ae65534b6a](https://github.com/MorpheApp/MicroG-RE/tree/66a2bbac7d88c3e038f1b047774fe2ae65534b6a).

- build.gradle: BASE_PACKAGE_NAME=app.revanced, applicationId=app.revanced.android.gms.
- AuthConstants.DEFAULT_ACCOUNT_TYPE=BASE_PACKAGE_NAME, WORK_ACCOUNT_TYPE=BASE_PACKAGE_NAME + ".work".
- AccountContentProvider và AuthManagerServiceImpl hỗ trợ lấy danh sách/visibility tài khoản; authority cũng dùng BASE_PACKAGE_NAME.
- PackageSpoofUtils đọc metadata package/chứng thư gốc. PackageUtils dùng digest sau spoof khi kiểm tra quyền Google/extended access. Phải giữ danh tính gốc đúng với file đầu vào, không giả thành YouTube.
- GcmConstants tạo action c2dm theo BASE_PACKAGE_NAME; cần chuyển các điểm tương ứng của Gmail.
- AuthManagerServiceImpl.getHubToken hiện trả null và ghi Not implemented. Đây là giới hạn của backend; chưa chứng minh Gmail gọi API này trên đường đọc thư chính. Phải kiểm tra các tính năng dùng API mới trước khi nhận hỗ trợ đầy đủ Gmail/Chat/Meet.
- Bản microG cài trên điện thoại có thể khác commit này. Cần đối chiếu phiên bản thực tế khi thử.

Nguồn:
[AuthConstants](https://github.com/MorpheApp/MicroG-RE/blob/66a2bbac7d88c3e038f1b047774fe2ae65534b6a/play-services-basement/src/main/java/org/microg/gms/auth/AuthConstants.java),
[AuthManagerServiceImpl](https://github.com/MorpheApp/MicroG-RE/blob/66a2bbac7d88c3e038f1b047774fe2ae65534b6a/play-services-core/src/main/java/org/microg/gms/auth/AuthManagerServiceImpl.java),
[PackageUtils](https://github.com/MorpheApp/MicroG-RE/blob/66a2bbac7d88c3e038f1b047774fe2ae65534b6a/play-services-base/core/src/main/java/org/microg/gms/common/PackageUtils.java).

## Phạm vi bản thử đề xuất

1. Tạo mục Gmail riêng, chỉ nhận đúng package/version đã khảo sát. Không sử dụng dependency hiện tại dành riêng Messenger/Zalo.
2. Chuyển package/service/provider xác thực sang MicroG-RE, sửa account type tại điểm tạo/lọc/kiểm tra Account và sync adapter. Giữ tài khoản IMAP/Exchange/POP3 và scope máy chủ của Gmail.
3. Chuyển FCM/IID và kiểm tra action của Notifications SDK. Tách cache đăng ký push có kiểm tra vai trò; giữ luồng Chime per-account và callback nguyên bản.
4. Nếu Gmail gốc đang cài và không thay được chữ ký, cần bản clone với package riêng, authority/permission tự sở hữu riêng và metadata danh tính Gmail gốc. Spoof metadata chỉ giúp microG hiểu danh tính gốc, không giúp Android cho cài đè APK đã ký khác.
5. Bảo toàn đủ 4 split và ký chúng bằng cùng khóa, hoặc dùng quá trình merge đã kiểm tra. MPP cần quy định đầu vào rõ ràng, không tự coi APKS là một APK độc lập.
6. Ưu tiên đăng nhập + đọc/gửi/đồng bộ thư + push. Đánh giá riêng Meet/Chat, tài khoản công việc và các API xác thực mới. Không thêm UI chẩn đoán, service duy trì, socket hay vòng lặp heartbeat vào Gmail.

Một biến thể chỉ chuyển FCM nhưng giữ tài khoản Google gốc cần chứng minh việc lấy auth token của APK đã ký lại vẫn được GMS gốc chấp nhận. Chưa có bằng chứng này; không được coi đó là giải pháp đã hoạt động.

## Điều kiện kiểm chứng trước phát hành

- Đầu ra rebuild/cài được với đầy đủ native/resources; không đụng authority/permission của Gmail gốc nếu clone.
- Đăng nhập vào microG, liệt kê account đúng và quyền visibility được cấp.
- Đọc và gửi thư thật, refresh thủ công, sync nền, đổi tài khoản.
- Gmail đăng ký FCM; Chime hoàn thành luồng per-account; nhận thư mới khi đóng màn hình.
- Kiểm tra token đổi sau reset backend, khởi động lại máy, thay mạng và hai tài khoản.
- Nếu thêm retry, dùng tín hiệu của luồng đăng ký Gmail/Chime; không dùng callback Messenger/Zalo làm bằng chứng thành công.
- Kiểm tra idempotency, invoke/move-result, scope/chứng thư/receiver permission, bảo toàn các file không liên quan.
- Kết quả phân tích tĩnh hiện tại chưa chứng minh đăng nhập/nhận thư/push trên thiết bị, chưa có Gmail trong release 0.6.0.
