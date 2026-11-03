# 🏸 ShuttleUp Hosting

**ShuttleUp Hosting** là ứng dụng Android đang phát triển theo hướng kết nối cộng đồng cầu lông: tạo nhóm theo khu vực, tổ chức buổi chơi, trao đổi và tìm người chơi phù hợp.

> **Trạng thái:** Đã có tài khoản Firebase, hồ sơ nhóm theo khu vực, nhiều buổi chơi theo ngày và đồng bộ Firestore. Dữ liệu vẫn được giữ cục bộ để ứng dụng tiếp tục hoạt động khi mạng không ổn định.

## ✨ Chức năng đang có

| Nhóm | Chức năng |
| --- | --- |
| 👥 Người chơi | Thêm, sửa, xóa; ghi giờ vào / giờ ra |
| 🏸 Xếp set | Tăng/giảm số set và gợi ý 4 người cho set tiếp theo |
| 💰 Thanh toán | Nhập tiền chơi, nợ nước; đánh dấu tiền mặt / chuyển khoản / chưa thanh toán |
| 🧾 Biên lai | Chụp hoặc chọn ảnh biên lai chuyển khoản |
| 🪶 Cầu | Theo dõi số lượng và đơn giá cầu |
| 📊 Tổng hợp | Xem tổng tiền trong buổi chơi |
| 👤 Tài khoản | Tạo tài khoản, đăng nhập, xác minh email và đăng xuất |
| ☁️ Đồng bộ | Đồng bộ nhóm và từng buổi chơi giữa các thiết bị đăng nhập cùng tài khoản |
| 📍 Nhóm khu vực | Đặt tên, khu vực và trạng thái công khai cho nhóm |

## 📱 Giao diện và demo

Ứng dụng được thiết kế theo hướng **ưu tiên màn hình điện thoại**. Có thể bổ sung ảnh chụp các màn hình vào thư mục `docs/images/` để người xem trực tiếp trên GitHub:

- `docs/images/players.png` — Quản lý người chơi.
- `docs/images/sets.png` — Xếp người vào sân.
- `docs/images/payments.png` — Thanh toán.

> Chưa đính kèm ảnh trong README vì cần ảnh chụp thực tế từ ứng dụng, tránh hiển thị hình minh họa không đúng phiên bản.

## 🧱 Công nghệ và kiến trúc

- **Kotlin**: ứng dụng Android và WebView.
- **HTML/CSS/JavaScript**: giao diện trong `app/src/main/assets/index.html`.
- **localStorage**: bộ nhớ cục bộ và cache khi ngoại tuyến.
- **Firebase Authentication**: tạo tài khoản và đăng nhập bằng email/mật khẩu.
- **Cloud Firestore**: đồng bộ hồ sơ nhóm, thành viên và các buổi chơi theo ngày.

**Lưu ý dữ liệu:** Ảnh biên lai chỉ lưu trên điện thoại để tránh làm tài liệu Firestore quá lớn; các dữ liệu còn lại của buổi chơi được đồng bộ.

## ▶️ Cài đặt và chạy

**Yêu cầu:** Git, Android Studio, Android SDK phù hợp với dự án; điện thoại Android hoặc máy ảo.

```bash
git clone https://github.com/nguyentuankiet210666/ShuttleUp-Hosting.git
```

1. Mở **Android Studio** → **Open** → chọn thư mục `ShuttleUp-Hosting`.
2. Chờ Gradle Sync hoàn tất.
3. Kết nối điện thoại đã bật **USB debugging** hoặc chọn Android Emulator.
4. Chọn **Run ▶** để build và cài ứng dụng.
5. Nếu chưa cấu hình Firebase, các chức năng quản lý cục bộ vẫn hoạt động; tài khoản và đồng bộ cần `google-services.json`.

## ☁️ Kế hoạch đồng bộ Firebase

Xem hướng dẫn chi tiết tại [FIREBASE_SETUP.md](FIREBASE_SETUP.md). Dữ liệu cộng đồng được tổ chức theo `groups/{groupId}` với thành viên, buổi chơi, yêu cầu tham gia và tin nhắn. Quyền truy cập được kiểm soát bằng Firestore Security Rules.

## 🗺️ Lộ trình phát triển

- [x] Ứng dụng chạy trên thiết bị Android
- [x] Quản lý người chơi, set và các khoản thanh toán cục bộ
- [x] Tạo tài khoản, đăng nhập và xác minh email bằng Firebase
- [x] Cấu hình Firebase project và file `google-services.json`
- [x] Đồng bộ Firestore với phân quyền theo nhóm
- [x] Lịch sử nhiều buổi chơi theo ngày
- [x] Hồ sơ nhóm, khu vực và trạng thái công khai
- [ ] Hoàn thiện giao diện xếp set tối ưu cho điện thoại
- [ ] Khám phá và tìm nhóm công khai theo khu vực
- [ ] Gửi yêu cầu tham gia và quản lý thành viên
- [ ] Bảng tin / trò chuyện trong nhóm
- [ ] Tạo lịch chơi và nhận đăng ký tham gia
- [ ] Đăng nhập bằng Google / số điện thoại
- [ ] Kết nối Google Sheets (nếu cần)
- [ ] Báo cáo, thống kê doanh thu
- [ ] Bổ sung ảnh giao diện và video demo

## 🔐 Bảo mật

Không đưa API key bí mật, mật khẩu, file ký ứng dụng (`.jks`/`.keystore`) hoặc thông tin nhạy cảm lên repository. Tránh commit `local.properties` và file cấu hình Firebase dành riêng cho môi trường. Khi triển khai Firestore, cần kiểm tra security rules trước khi dùng thực tế.

---

**Dự án cá nhân đang phát triển**, ưu tiên thao tác nhanh, rõ ràng trên điện thoại dành cho host cầu lông.

