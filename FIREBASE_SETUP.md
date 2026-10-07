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

## 3. Bật Authentication

Trong Firebase Console:

- Authentication → Sign-in method
- Bật Google
- Phone có thể bật ở bước sau

Không cần nhét mật khẩu Google hay secret vào source code.

## 4. Tạo Firestore

Tạo Cloud Firestore và ưu tiên cấu trúc dữ liệu:

```text
users/{uid}
  profile
  sessions/{sessionId}
    date
    courts
    shuttleCount
    shuttlePrice
    players/{playerId}
      name
      timeIn
      timeOut
      sets
      money
      water
      payment
```

Cách này giúp rule bảo mật đơn giản hơn và tránh host này đọc dữ liệu của host khác.

## 5. Security Rules

Repo có file `firestore.rules` mẫu. Rule mặc định yêu cầu đăng nhập và chỉ cho tài khoản thao tác trong `users/{uid}` của chính mình.

Không dùng rule kiểu:

```text
allow read, write: if true;
```

cho app thật, vì như vậy bất kỳ client nào cũng có thể đọc hoặc sửa dữ liệu.

## 6. Thứ tự tích hợp tiếp theo

1. Google Login
2. Lấy Firebase UID
3. Tạo `users/{uid}`
4. Chuyển dữ liệu session/player từ localStorage lên Firestore
5. Giữ localStorage làm cache/offline tạm thời
6. Thêm listener Firestore để đồng bộ nhiều máy

Trong giai đoạn chuyển đổi, không xoá localStorage ngay để tránh mất dữ liệu đang có.
