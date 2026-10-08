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
4. Giữ nguyên package com.google.android.gm theo yêu cầu thay hoàn toàn bản gốc; giữ authority/permission riêng của Gmail. Metadata danh tính Gmail gốc phục vụ microG, không thay đổi quy tắc chữ ký cài đặt của Android.
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

## Đối chiếu sâu theo yêu cầu thay hoàn toàn Gmail gốc

Phạm vi đã chốt: giữ package `com.google.android.gm` và authority/permission riêng của Gmail hiện tại. Không clone. Các đề xuất clone ở phần khảo sát trước không thuộc bản thử này. Việc cài thay bản gốc được người dùng tự xử lý; giữ package không thay đổi quy tắc chữ ký của Android.

Đã kiểm tra cả source tag `7.2.1` và APK phát hành `microg-7.2.1-icon-arm64-v8a.apk`.
Tag trỏ đúng commit `66a2bbac7d88c3e038f1b047774fe2ae65534b6a`.
SHA-256 APK microG: `3425e46e95cd45012892cfaa23061b70db0a25affffcfcb84e3df4511d63eb82`.

### Đích xác thực đã kiểm chứng bằng APK phát hành

| Thành phần | Giá trị chính xác |
| --- | --- |
| Package microG | app.revanced.android.gms |
| Tên lớp GetToken trong manifest APK microG | com.google.android.gms.auth.GetToken |
| Authority accounts | app.revanced.android.gms.auth.accounts |
| Loại tài khoản cá nhân, resolve từ XML/resource APK | app.revanced |
| Binder descriptor | com.google.android.auth.IAuthManagerService |
| Transaction lấy token với Account | 5 |
| Transaction lấy danh sách tài khoản | 6 |
| Dữ liệu token trả về | Bundle tokenDetails chứa Parcelable TokenData |

Điểm quan trọng: tại `Laboa;<clinit>` chỉ đổi package truyền vào ComponentName; **giữ nguyên chuỗi tên lớp com.google.android.gms.auth.GetToken**. Đổi cả tên lớp sang app.revanced sẽ bind vào thành phần không tồn tại.

`Laboa;b` đi thẳng qua ComponentName này; `Labnv;a(IBinder)` giữ descriptor gốc, ghi Account/scope/Bundle, dùng transaction 5 và đọc tokenDetails/TokenData.
AIDL microG khai báo getTokenWithAccount = 4, tương ứng transaction 5 (FIRST_CALL_TRANSACTION cộng 4).
`AuthManagerServiceImpl.getTokenWithAccount` tạo đúng cấu trúc Bundle này.
`Labnu;a` lấy danh sách qua transaction 6, khớp getAccounts = 5 của AIDL.
Do đó đường OAuth cổ điển được khảo sát không phụ thuộc getHubToken chưa triển khai. Điều này xác nhận tương thích giao thức, chưa chứng minh máy chủ cấp được token.

### Danh sách thay thế tối thiểu theo ngữ cảnh

1. Package đích dịch vụ/receiver/visibility: com.google.android.gms → app.revanced.android.gms. Chỉ áp dụng chỗ định tuyến và kiểm tra backend.
2. Authority tài khoản: com.google.android.gms.auth.accounts → app.revanced.android.gms.auth.accounts.
3. Account type cá nhân: com.google → app.revanced ở chỗ tạo, chọn, lọc, kiểm tra và serialize Account; gồm Laboa, các luồng AccountManager.addAccount, AccountChanged handling và sync adapter res/w9i.xml. Phải kiểm kê tác dụng của từng vị trí trong 237 literal trước khi sửa.
4. C2DM REGISTER/UNREGISTER/REGISTRATION/RECEIVE → các action app.revanced.android.c2dm tương ứng.
5. IID TOKEN_REQUEST → app.revanced.iid.TOKEN_REQUEST.
6. C2DM permission.SEND và RECEIVE → app.revanced.android.c2dm permission tương ứng.
7. Laufw.a(Intent) và FirebaseMessagingService xử lý action phải đồng bộ với receiver manifest.
8. Chỉ thay action auth.api.signin.service.START/GOOGLE_SIGN_IN khi luồng đang dùng được xác định và endpoint được microG hỗ trợ; không biến mọi action auth thành app.revanced theo tiền tố chung.
9. Giữ scope mail/notifications, OAuth URL, Gmail server/project/sender IDs, tên lớp Java và Binder descriptor.
10. Metadata microG: package gốc com.google.android.gm và signer 38918a453d07199354f8b19af05ec6562ced5788 của đầu vào. Không dùng signer của APK đã ký lại cho metadata OAuth.

Account công việc không thuộc phạm vi đã xác nhận: APK microG có WorkAccountAuthenticatorService **disabled và không exported**; XML của nó còn là com.google.work, trong khi hằng WORK_ACCOUNT_TYPE là app.revanced.work. Không đổi account công việc rồi tuyên bố đã hỗ trợ Workspace/work profile.

### Kiểm tra GMS và FCM transport

`Labwk;b(Context,int)` kiểm tra package/version/metadata, chữ ký GMS và quan hệ chữ ký Play Store-GMS. Chỉ đổi package vẫn có thể trả lỗi chữ ký.
Bản thử phải xử lý kiểm tra tương thích của backend tại điểm gọi đã xác định; kiểm tra microG có cài/bật trước khi cho đi tiếp, không báo mọi dịch vụ đều sẵn sàng khi microG vắng mặt. Không vô hiệu hóa kiểm tra chữ ký tùy ý trong Gmail.

`Labvd;e` bind REGISTER theo package gốc, `Lcfvf;` là MessengerIpcClient.
MicroG PushRegisterService hỗ trợ Messenger, kiểm tra UID của package gửi, xử lý registration và phản hồi.
Cần chuyển đúng REGISTER/package, giữ request/reply schema và identity PendingIntent của chính Gmail.
Không thay creator bằng package microG và không sửa UID của caller.

### Điểm cần kiểm chứng về chứng thư FCM

OAuth AuthManager lấy signature qua PackageUtils.firstSignatureDigest và dùng spoof metadata khi gửi AuthRequest.
Trong đường FCM khảo sát, PushRegisterManager lấy firstCertificateSha1Hex từ ExtendedPackageInfo khi request chưa có signature; helper PackageManager.getCertificates đọc GET_SIGNATURES của APK đã cài.
RegisterRequest đưa giá trị đó vào trường cert của register3.
Không thấy lời gọi spoof signature trong các file đường FCM đã đọc.

Vì thế không được suy ra metadata signer gốc đã tự áp dụng cho cả FCM. Không kết luận máy chủ chắc chắn từ chối signer mới; cần thử request/response đăng ký FCM và Chime thực tế. Nếu đây là nguyên nhân bị từ chối, bản vá chỉ trong Gmail không tự thay được trường cert do microG dựng; khi đó mới đánh giá sửa backend MicroG-RE theo bằng chứng lỗi.

### Tiêu chí dừng nghiên cứu để chuyển sang bản thử

Đã xác định đường OAuth cổ điển và route FCM có implementation microG tương ứng; có đủ căn cứ để triển khai bản thử riêng cho phiên bản này.
Chưa có xác nhận động về toàn bộ các vị trí account type, cache Chime/IID migration, API account state/Meet/Chat, rebuild splits hay nhận thư trên thiết bị.
Bản thử cần giữ luồng đăng ký/refresh của Gmail, kiểm tra và tách cache đăng ký push; không thêm retry cưỡng bức trước khi biết scheduler/callback gốc hoạt động thế nào.

Nguồn bổ sung:
[GetToken](https://github.com/MorpheApp/MicroG-RE/blob/7.2.1/play-services-core/src/main/java/com/google/android/gms/auth/GetToken.java),
[IAuthManagerService](https://github.com/MorpheApp/MicroG-RE/blob/7.2.1/play-services-auth-base/src/main/aidl/com/google/android/auth/IAuthManagerService.aidl),
[PushRegisterService](https://github.com/MorpheApp/MicroG-RE/blob/7.2.1/play-services-core/src/main/kotlin/org/microg/gms/gcm/PushRegisterService.kt),
[PushRegisterManager](https://github.com/MorpheApp/MicroG-RE/blob/7.2.1/play-services-core/src/main/java/org/microg/gms/gcm/PushRegisterManager.java),
[ExtendedPackageInfo](https://github.com/MorpheApp/MicroG-RE/blob/7.2.1/play-services-base/core/src/main/kotlin/org/microg/gms/utils/ExtendedPackageInfo.kt).
