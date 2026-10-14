# Phạm vi giấy phép

Tên sản phẩm: **DTinh MicroG FCM Patches**, dự án độc lập, tương thích với Morphe Manager/Patcher. Không phải bản phát hành của Morphe, Zeldris, microG, ReVanced, Meta hoặc VNG; không nhận quyền nhãn hiệu của các dự án này.

| Phạm vi | Giấy phép | Văn bản |
| --- | --- | --- |
| `src/`, `extension/`, `tests/`, gói MPP | GPL-3.0-only | LICENSE-patches |
| Bản chuyển native Zalo từ Zeldris | GNU GPL v3 và thông báo nguồn được giữ nguyên | LICENSE-zeldris, NOTICE-zeldris |
| `build.sh`, `scripts/`, workflow do dự án triển khai, tài liệu do DTinh viết | Apache-2.0 | LICENSE |
| NOTICE/giấy phép của bên thứ ba | Điều khoản nguyên gốc, không cấp lại giấy phép | THIRD_PARTY_NOTICES.md và NOTICE-* |

`LICENSE` Apache có sẵn từ commit khởi tạo không áp dụng để cấp lại giấy phép mã GPL. Gói bản vá kết hợp được phân phối theo GPL v3, với các thông báo bổ sung của nguồn gốc được bảo toàn. Các phụ thuộc bên ngoài giữ giấy phép riêng.

Không có bảo hành; xem đầy đủ điều khoản trong các tệp giấy phép. Khi phân phối lại, giữ giấy phép, ghi công và NOTICE; đánh dấu các sửa đổi; cung cấp mã nguồn tương ứng và script build cho đúng bản nhị phân. Không chỉ đổi tên tác giả thành người đóng gói.

Mã nguồn tương ứng của 0.7.0 được đính kèm cùng release, gồm mã Kotlin/Java, tests, build.sh, scripts và tools.lock.json. Dùng JDK 21, Python 3.10+ và `bash build.sh`. Công cụ bên ngoài tải theo URL/version/SHA-256 trong tools.lock.json. Các dependency patcher/smali giữ nguồn upstream được liệt kê trong THIRD_PARTY_NOTICES.md. Không đóng gói lại APK ứng dụng, SDK hoặc microG trong MPP.
