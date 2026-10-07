# 🏸 ShuttleUp Hosting

**Hosting** là ứng dụng Android hỗ trợ host quản lý một buổi chơi cầu lông ngay trên điện thoại.

## ✅ Chức năng hiện có

- Thêm, sửa và xoá người chơi
- Ghi giờ vào / giờ ra
- Tăng giảm số set
- Nhập tiền chơi và nợ nước
- Check thanh toán: tiền mặt / chuyển khoản / chưa thanh toán
- Chụp hoặc chọn ảnh biên lai chuyển khoản
- Gợi ý 4 người cho set tiếp theo
- Theo dõi số lượng cầu và đơn giá cầu
- Tổng hợp tiền trong buổi chơi

## 🧱 Kiến trúc hiện tại

Ứng dụng hiện dùng:

- Kotlin cho Android host
- WebView để hiển thị giao diện
- HTML/CSS/JavaScript trong `app/src/main/assets/index.html`
- `localStorage` để lưu dữ liệu trên thiết bị
- Firebase Auth + Firestore đã được thêm dependency để chuẩn bị đồng bộ cloud

> Firebase chưa hoạt động cho đến khi bạn tạo Firebase project và thêm file `app/google-services.json`.

## 🔥 Firebase

Nhánh này đã chuẩn bị nền tảng cho:

- Firebase Authentication
- Cloud Firestore
- Dữ liệu tách riêng theo tài khoản host
- Đồng bộ nhiều điện thoại trong các bước tiếp theo

Xem hướng dẫn: [FIREBASE_SETUP.md](FIREBASE_SETUP.md)

## 🚀 Chạy dự án

```bash
git clone https://github.com/nguyentuankiet210666/ShuttleUp-Hosting.git
```

Sau đó:

1. Mở Android Studio.
2. Chọn **Open** và mở thư mục dự án.
3. Chờ Gradle Sync hoàn tất.
4. Chạy bằng Android Emulator hoặc điện thoại thật.
5. Nhấn **Run ▶**.

Nếu chưa cấu hình Firebase, app vẫn build và dùng dữ liệu local như trước.

## 🔐 Bảo mật

Không commit các file hoặc thông tin nhạy cảm:

- `app/google-services.json`
- `local.properties`
- file `.jks` / `.keystore`
- mật khẩu hoặc secret key

Firestore nên lưu dữ liệu theo cấu trúc:

```text
users/{uid}
  sessions/{sessionId}
    players/{playerId}
```

Nhờ đó security rules có thể giới hạn mỗi tài khoản chỉ đọc/ghi dữ liệu của chính mình.

## 🗺️ Lộ trình

- [x] App Android chạy trên điện thoại
- [x] Quản lý người chơi, set và thanh toán
- [x] Chuẩn bị Firebase dependencies
- [ ] Tạo Firebase project + thêm google-services.json
- [ ] Đăng nhập Google
- [ ] Đăng nhập số điện thoại
- [ ] Đồng bộ dữ liệu Firestore
- [ ] Lịch sử nhiều buổi chơi
- [ ] Đồng bộ nhiều thiết bị
- [ ] Google Sheets
- [ ] Thống kê doanh thu

## 📄 Ghi chú

Dự án đang trong quá trình phát triển và hiện ưu tiên chức năng quản lý nội bộ cho host cầu lông.
