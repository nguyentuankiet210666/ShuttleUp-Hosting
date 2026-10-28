# 🏸 ShuttleUp Hosting

**Hosting** là ứng dụng Android hỗ trợ người tổ chức (host) quản lý một buổi chơi cầu lông trên điện thoại: người chơi, phân set, số cầu và thanh toán.

> **Trạng thái:** Đang phát triển. Các chức năng quản lý hiện lưu dữ liệu **cục bộ trên thiết bị**; đăng nhập và đồng bộ Firebase **chưa được tích hợp hoàn chỉnh**.

## ✨ Chức năng đang có

| Nhóm | Chức năng |
| --- | --- |
| 👥 Người chơi | Thêm, sửa, xóa; ghi giờ vào / giờ ra |
| 🏸 Xếp set | Tăng/giảm số set và gợi ý 4 người cho set tiếp theo |
| 💰 Thanh toán | Nhập tiền chơi, nợ nước; đánh dấu tiền mặt / chuyển khoản / chưa thanh toán |
| 🧾 Biên lai | Chụp hoặc chọn ảnh biên lai chuyển khoản |
| 🪶 Cầu | Theo dõi số lượng và đơn giá cầu |
| 📊 Tổng hợp | Xem tổng tiền trong buổi chơi |

## 📱 Giao diện và demo

Ứng dụng được thiết kế theo hướng **ưu tiên màn hình điện thoại**. Có thể bổ sung ảnh chụp các màn hình vào thư mục `docs/images/` để người xem trực tiếp trên GitHub:

- `docs/images/players.png` — Quản lý người chơi.
- `docs/images/sets.png` — Xếp người vào sân.
- `docs/images/payments.png` — Thanh toán.

> Chưa đính kèm ảnh trong README vì cần ảnh chụp thực tế từ ứng dụng, tránh hiển thị hình minh họa không đúng phiên bản.

## 🧱 Công nghệ và kiến trúc

- **Kotlin**: ứng dụng Android và WebView.
- **HTML/CSS/JavaScript**: giao diện trong `app/src/main/assets/index.html`.
- **localStorage**: lưu trạng thái dữ liệu hiện tại ngay trên thiết bị.
- **Firebase Authentication / Cloud Firestore**: đã có phần chuẩn bị dependency, chưa hoàn tất cấu hình và kết nối sử dụng thực tế.

**Lưu ý dữ liệu:** Chưa có đồng bộ đám mây. Không nên xóa dữ liệu ứng dụng hoặc gỡ cài đặt nếu chưa sao lưu thông tin cần giữ.

## ▶️ Cài đặt và chạy

**Yêu cầu:** Git, Android Studio, Android SDK phù hợp với dự án; điện thoại Android hoặc máy ảo.

```bash
git clone https://github.com/nguyentuankiet210666/ShuttleUp-Hosting.git
```

1. Mở **Android Studio** → **Open** → chọn thư mục `ShuttleUp-Hosting`.
2. Chờ Gradle Sync hoàn tất.
3. Kết nối điện thoại đã bật **USB debugging** hoặc chọn Android Emulator.
4. Chọn **Run ▶** để build và cài ứng dụng.
5. Khi chưa cấu hình Firebase, tiếp tục sử dụng chế độ dữ liệu cục bộ như hiện tại.

## ☁️ Kế hoạch đồng bộ Firebase

Xem hướng dẫn chi tiết tại [FIREBASE_SETUP.md](FIREBASE_SETUP.md). Hướng triển khai là xác thực tài khoản host, lưu dữ liệu theo `users/{uid}`, áp dụng Firestore Security Rules và đồng bộ giữa các thiết bị.

**Firebase hiện chưa phải tính năng đã hoàn thiện.**

## 🗺️ Lộ trình phát triển

- [x] Ứng dụng chạy trên thiết bị Android
- [x] Quản lý người chơi, set và các khoản thanh toán cục bộ
- [x] Chuẩn bị dependency Firebase
- [ ] Hoàn thiện giao diện xếp set tối ưu cho điện thoại
- [ ] Cấu hình Firebase project và file `google-services.json`
- [ ] Đăng nhập bằng Google / số điện thoại
- [ ] Đồng bộ Firestore với phân quyền theo tài khoản
- [ ] Lịch sử nhiều buổi chơi và đồng bộ nhiều điện thoại
- [ ] Kết nối Google Sheets (nếu cần)
- [ ] Báo cáo, thống kê doanh thu
- [ ] Bổ sung ảnh giao diện và video demo

## 🔐 Bảo mật

Không đưa API key bí mật, mật khẩu, file ký ứng dụng (`.jks`/`.keystore`) hoặc thông tin nhạy cảm lên repository. Tránh commit `local.properties` và file cấu hình Firebase dành riêng cho môi trường. Khi triển khai Firestore, cần kiểm tra security rules trước khi dùng thực tế.

---

**Dự án cá nhân đang phát triển**, ưu tiên thao tác nhanh, rõ ràng trên điện thoại dành cho host cầu lông.
