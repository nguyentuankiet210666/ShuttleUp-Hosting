# Firebase setup cho Hosting

Các bước này chỉ cần làm một lần cho Firebase project.

## 1. Tạo Firebase project

1. Mở Firebase Console.
2. Tạo project mới, ví dụ `ShuttleUp-Hosting`.
3. Thêm Android app với package name:

```text
com.hosting.badminton
```

Package name phải khớp với `applicationId` trong `app/build.gradle.kts`.

## 2. Thêm google-services.json

Tải file cấu hình Android từ Firebase và đặt đúng vị trí:

```text
ShuttleUp-Hosting/
└── app/
    └── google-services.json
```

File này đã được thêm vào `.gitignore` để tránh commit nhầm cấu hình project cá nhân.

Sau khi file tồn tại, Gradle sẽ tự áp dụng plugin Google Services.

## 3. Bật Authentication bằng Email/Mật khẩu

Trong Firebase Console:

- Authentication → Sign-in method
- Bật **Email/Password**
- Không cần bật Email link (passwordless) ở phiên bản hiện tại
- Ứng dụng sẽ gửi email xác minh sau khi tạo tài khoản
- Google và Phone có thể bật ở bước sau

Không cần nhét mật khẩu Google hay secret vào source code.

Sau khi bật, chạy ứng dụng và mở **Cài đặt → Tài khoản** để:

1. Tạo tài khoản bằng họ tên, email và mật khẩu từ 6 ký tự.
2. Mở email xác minh do Firebase gửi.
3. Đăng nhập lại và kiểm tra tên/email hiển thị trong ứng dụng.

Nếu chưa có `google-services.json`, ứng dụng vẫn chạy các chức năng quản lý cục bộ và sẽ hiển thị hướng dẫn cấu hình khi người dùng thử đăng nhập.

## 4. Tạo Firestore

Tạo Cloud Firestore. Chọn khu vực gần phần lớn người dùng (ví dụ Singapore cho cộng đồng ở Đông Nam Á) vì vị trí database không thể đổi sau khi tạo.

Cấu trúc dữ liệu hiện tại và phần mở rộng cho cộng đồng:

```text
users/{uid}
  displayName, email, defaultGroupId

groups/{groupId}
  name, area, areaKey, isPublic, ownerUid, memberCount
  members/{uid}
    displayName, role, joinedAt
  sessions/{yyyy-MM-dd}
    date, payload, updatedBy, updatedAt
  joinRequests/{uid}
    userId, status, createdAt
  messages/{messageId}
    senderUid, text, createdAt
```

Mỗi tài khoản mới bắt đầu với một nhóm do chính tài khoản đó sở hữu. Nhóm chỉ xuất hiện trong kết quả tìm kiếm khi `isPublic = true` và đã có khu vực.

## 5. Security Rules

Repo có file `firestore.rules`. Sao chép nội dung file này vào **Firestore Database → Rules** rồi bấm **Publish**.

Rule hiện tại cho phép:

- Người dùng chỉ đọc/sửa hồ sơ của chính mình.
- Người đã đăng nhập xem danh sách nhóm công khai.
- Chỉ chủ nhóm sửa hồ sơ nhóm và duyệt yêu cầu tham gia.
- Chủ nhóm tạo, sửa và xóa dữ liệu buổi chơi; thành viên chỉ đọc dữ liệu buổi chơi và có thể đọc/gửi tin nhắn.
- Người gửi chỉ sửa hoặc xóa tin nhắn của chính họ.

Không dùng rule kiểu:

```text
allow read, write: if true;
```

cho app thật, vì như vậy bất kỳ client nào cũng có thể đọc hoặc sửa dữ liệu.

## 6. Luồng đồng bộ hiện tại

1. Đăng nhập bằng Email/Password và lấy Firebase UID.
2. Tạo/cập nhật hồ sơ `users/{uid}` và nhóm mặc định `groups/{uid}`.
3. Mỗi ngày được lưu thành một tài liệu trong `sessions/{yyyy-MM-dd}`.
4. Listener Firestore nhận thay đổi để cập nhật thiết bị đang mở cùng buổi chơi.
5. `localStorage` tiếp tục làm cache khi ngoại tuyến nhưng được tách theo Firebase UID. Dữ liệu từ phiên bản cũ chỉ được nhập vào tài khoản sau khi người dùng xác nhận.
6. Ảnh biên lai không tải lên Firestore; ảnh chỉ nằm trên thiết bị.

## 7. Bước cộng đồng tiếp theo

1. Màn hình khám phá nhóm công khai, lọc theo `areaKey`.
2. Gửi và duyệt yêu cầu tham gia.
3. Danh sách thành viên, vai trò chủ nhóm/thành viên.
4. Bảng tin hoặc trò chuyện theo nhóm.
5. Tạo lịch chơi, giới hạn số người và đăng ký tham gia.

Trong giai đoạn chuyển đổi, dữ liệu `localStorage` cũ được giữ trong vùng khách. Ứng dụng phải hỏi người dùng trước khi nhập dữ liệu đó vào một tài khoản Firebase, tránh lẫn dữ liệu giữa hai tài khoản.

