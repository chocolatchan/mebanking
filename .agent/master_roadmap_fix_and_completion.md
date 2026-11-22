# ME Banking – Master Roadmap: Fixes & Implementation Plan

**Tài liệu tham chiếu chính:** [`.agent/fix.md`](file:///home/chocolat/Documents/Project/m.ebanking/.agent/fix.md) & [`.agent/work.md`](file:///home/chocolat/Documents/Project/m.ebanking/.agent/work.md)  
**Mục tiêu:** Kế hoạch thực hiện toàn diện để vừa khắc phục các điểm yếu kiến trúc đã phát hiện (idempotency, liveness, instrumented test, booking gaps, policy chốt), vừa hoàn thiện các nghiệp vụ chưa thực hiện theo chuẩn đồ án tốt nghiệp đại học.

---

## 1. Bảng đối chiếu hiện trạng và các vấn đề cần khắc phục (Gap Analysis)

| Hạng mục | Hiện trạng codebase | Yêu cầu trong `fix.md` / Cần khắc phục |
|---|---|---|
| **1. Idempotency** | Lưu trong `ConcurrentHashMap` (in-memory); mất khi app bị kill | **Bắt buộc**: Bổ sung cột `idempotency_key` và index `UNIQUE` vào bảng `transactions` trong SQLite/Room; lưu key cùng kết quả trong cùng `@Transaction` |
| **2. Deposit / Withdraw UI** | Đã có domain logic và unit tests, nhưng chưa có UI riêng trên Compose | Tạo Deposit Screen và Withdraw Screen riêng biệt; chọn tài khoản, xác nhận giao dịch |
| **3. Lịch sử giao dịch** | Đã hiển thị tóm tắt trên HomeScreen | Tạo màn hình Transaction History & Transaction Detail chuyên biệt (lọc theo ngày, loại, trạng thái) |
| **4. Xác thực giao dịch (Policy)** | Policy trả về `PendingPolicy` cho hạn mức cao | **Chốt quy tắc mới:**<br>- Bắt buộc **System Authentication** (BiometricPrompt / Device PIN) cho mọi giao dịch<br>- Hạn mức `<= 10.000.000 VND`: Xác thực qua **In-app OTP** (30s refresh, timeout 60s)<br>- Hạn mức `> 10.000.000 VND`: Xác thực qua **Face Recognition** |
| **5. Face Recognition** | Đang dùng deterministic seed hash để tạo 128-d vector | Tích hợp **MediaPipe Face Detection / Landmarker** và **MobileFaceNet** model inference thực tế trên camera preview |
| **6. Simulators mở rộng** | Mới có Utility Bills cơ bản (Điện/Nước) | Xây dựng domain, Room entities, repository và UI riêng cho:<br>- **Flight Booking Simulator**<br>- **Hotel Booking Simulator**<br>- **Cinema Booking Simulator**<br>- **E-commerce Simulator** |
| **7. Officer Dashboard & eKYC** | Đã có `OfficerCustomerService` ở domain layer, nhưng chưa có UI | Xây dựng giao diện **Officer Dashboard** (Quản lý khách hàng, tìm kiếm, sửa thông tin) và **Quy trình eKYC bằng Camera** cho Officer |
| **8. Testing & Runtime Evidence** | 437 bài test chạy trên JVM/Robolectric; chưa có `androidTest` | Thiết lập bộ **Instrumented UI Tests (`app/src/androidTest`)** chạy trên Android runtime thực tế để xác minh BiometricPrompt, Camera, Keystore và Compose UI |

---

## 2. Lộ trình triển khai chi tiết 5 giai đoạn (Phases 1 – 5)

```
[Phase 1: Core Banking & Durable Idempotency]
                     │
                     ▼
[Phase 2: Transaction Security (Biometric + In-app OTP)]
                     │
                     ▼
[Phase 3: Extended Simulators (Flight, Hotel, Cinema, Shop)]
                     │
                     ▼
[Phase 4: Officer Dashboard & Real Face Inference (MediaPipe)]
                     │
                     ▼
[Phase 5: Full QA, Instrumented Android Tests & Handover]
```

---

### Phase 1: Core Banking & Durable Idempotency

**Trọng tâm:** Hoàn thiện giao dịch tiền tệ, xóa bỏ rủi ro process-death mất idempotency.

* [ ] **Task 1.1 — Database Migration for Durable Idempotency (DEF-4.2-01 Fix):**
  - Cập nhật entity `TransactionEntity.kt`: Thêm trường `idempotencyKey: String?` với chỉ mục `Index(value = ["idempotency_key"], unique = true)`.
  - Cập nhật Room Database migration v1 -> v2 hoặc tạo schema migration sạch.
  - Cập nhật `DefaultTransactionRepository.kt`: Ghi nhận `idempotencyKey` trực tiếp vào database transaction. Khi phát hiện key đã tồn tại, trả về bản ghi cũ mà không trừ tiền lần hai.
* [ ] **Task 1.2 — Deposit & Withdraw Compose Screens:**
  - Viết `DepositScreen.kt` & `DepositViewModel.kt` (chọn tài khoản, nhập số tiền, dialog xác nhận, sinh Idempotency key).
  - Viết `WithdrawScreen.kt` & `WithdrawViewModel.kt` (kiểm tra số dư, cảnh báo không vượt hạn mức, xác nhận).
* [ ] **Task 1.3 — Transaction History & Transaction Detail Screen:**
  - Viết `TransactionHistoryScreen.kt` và `TransactionDetailScreen.kt` với bộ lọc (tất cả, nạp, rút, chuyển khoản, hóa đơn).
* [ ] **Task 1.4 — Phase 1 Test & Verification:**
  - Viết test mô phỏng Process Death / Cold Restart: gửi lại request cùng idempotency key sau khi khởi tạo lại service instance, khẳng định database chặn trừ tiền lần 2.

---

### Phase 2: Transaction Security (BiometricPrompt + In-app OTP)

**Trọng tâm:** Hiện thực hóa quy tắc xác thực 2 cấp độ đã chốt trong `fix.md`.

* [ ] **Task 2.1 — System Authentication Integration (AndroidX Biometric):**
  - Tích hợp `BiometricPrompt` của Android vào `MainActivity` / Compose wrapper.
  - Xử lý các tình huống: `BIOMETRIC_SUCCESS`, `BIOMETRIC_ERROR_NO_HARDWARE`, `DEVICE_CREDENTIAL_ALLOWED`, `USER_CANCELED`.
* [ ] **Task 2.2 — In-App OTP State Machine (Cho giao dịch <= 10.000.000 VND):**
  - Xây dựng `OtpService` & `OtpChallengeManager`: sinh mã 6 số bằng `SecureRandom`.
  - Timer tự động làm mới mã mỗi 30 giây, challenge hết hạn sau 60 giây.
  - Thiết kế UI `OtpVerificationDialog` yêu cầu người dùng tự nhập mã hiển thị trên màn hình.
* [ ] **Task 2.3 — Transaction Authorization Orchestrator:**
  - Hoàn thiện `TransactionAuthenticationPolicy`:
    - Bước 1: Bắt buộc System Authentication thành công.
    - Bước 2: Kiểm tra số tiền:
      - Nếu `<= 10.000.000 VND` -> Yêu cầu In-App OTP.
      - Nếu `> 10.000.000 VND` -> Yêu cầu Face Recognition (kết nối Phase 4).
* [ ] **Task 2.4 — Phase 2 Tests:**
  - Unit & Integration tests cho timeout OTP, nhập sai OTP quá số lần cho phép, hủy bỏ BiometricPrompt.

---

### Phase 3: Extended Service Simulators

**Trọng tâm:** Bổ sung các dịch vụ mô phỏng đặt vé và mua sắm có persistence riêng trong Room.

* [ ] **Task 3.1 — Database Expansion for Extended Services:**
  - Thêm các bảng Room:
    - `flights` & `flight_bookings`
    - `hotels` & `hotel_bookings`
    - `movies`, `cinemas`, `showtimes` & `cinema_bookings`
    - `products`, `orders` & `order_items`
  - Seed dữ liệu offline phong phú (chuyến bay SGN-HAN, khách sạn HCMC/Đà Lạt, rạp chiếu phim, sản phẩm demo).
* [ ] **Task 3.2 — Flight & Hotel Booking Simulators:**
  - Viết UI & Service tìm kiếm chuyến bay, chọn ghế, tạo mã đặt chỗ, thanh toán qua tài khoản ngân hàng.
  - Viết UI & Service tra cứu khách sạn, chọn phòng, chọn ngày check-in/out, thanh toán.
* [ ] **Task 3.3 — Cinema & E-commerce Simulators:**
  - Viết UI & Service chọn suất chiếu, sơ đồ ghế xem phim (chặn chọn trùng ghế đã bán).
  - Viết UI Giỏ hàng (Cart), tạo đơn hàng mua sắm, tính tổng tiền bằng `Long VND`.
* [ ] **Task 3.4 — Payment Lifecycle Integration:**
  - Kết nối toàn bộ 4 simulator vào `DefaultTransactionService` qua luồng chuẩn: `CREATED` -> `PENDING_PAYMENT` -> `System Auth / OTP` -> `PAID`.

---

### Phase 4: Officer Dashboard & Real Face Inference

**Trọng tâm:** Phân quyền Officer và tích hợp camera/model AI nhận diện khuôn mặt thực tế.

* [ ] **Task 4.1 — Officer Dashboard Screen & Navigation:**
  - Màn hình dành riêng cho vai trò `Role.OFFICER` sau khi login.
  - Quản lý danh sách khách hàng, tìm kiếm khách hàng theo CMND/CCCD/Họ tên.
  - Tạo mới tài khoản khách hàng, sửa thông tin liên hệ.
* [ ] **Task 4.2 — Customer eKYC Camera Flow:**
  - Sử dụng CameraX để hiển thị Camera Preview và chụp ảnh chân dung khách hàng trực tiếp trên thiết bị.
  - Kiểm tra điều kiện ánh sáng, góc nghiêng khuôn mặt.
* [ ] **Task 4.3 — MediaPipe / MobileFaceNet Model Inference:**
  - Đóng gói file model `mobilefacenet.tflite` trong thư mục `assets/models/`.
  - Viết pipeline tiền xử lý ảnh (crop khuôn mặt theo bounding box của MediaPipe -> resize 112x112 -> normalize -> inference qua TFLite Interpreter).
  - Trích xuất 128-d (hoặc 512-d) feature vector thực tế, thay thế hoàn toàn byte-hash stub cũ.
* [ ] **Task 4.4 — Kết nối Face Recognition cho giao dịch > 10.000.000 VND:**
  - Khi chuyển tiền > 10M, mở camera xác thực khuôn mặt người gửi so khớp với template đã mã hóa trong Keystore.

---

### Phase 5: Quality Assurance, Android Device Tests & Delivery

**Trọng tâm:** Kiểm thử toàn diện trên Android Runtime và hoàn tất tài liệu đồ án.

* [ ] **Task 5.1 — Setup Instrumented UI Tests (`app/src/androidTest`):**
  - Cấu hình Compose Test dependencies trong `build.gradle.kts`.
  - Viết test kiểm tra UI trên thiết bị/emulator: Luồng chuyển tiền, luồng thanh toán hóa đơn, luồng login.
* [ ] **Task 5.2 — Audit toàn diện 360 độ:**
  - Security Audit (bảo mật Keystore, SQL cipher, FLAG_SECURE).
  - Financial Integrity Audit (đảm bảo không rò rỉ tiền, đối soát ledger).
  - Offline Runtime Audit (kiểm tra hoạt động hoàn toàn ở chế độ Máy bay).
* [ ] **Task 5.3 — Release Build & Final Deliverables:**
  - Build Release APK (`./gradlew assembleRelease`).
  - Lập Ma trận truy xuất yêu cầu (`requirement_traceability_matrix.md`) đối chiếu file đề bài PDF.
  - Soạn tài liệu báo cáo tổng kết đồ án (`final_project_report.md`) và Kịch bản quay video demo 16 bước (`demo_script.md`).

---

## 3. Thứ tự hành động ngay tiếp theo

1. **Bước khởi đầu:** Bắt đầu ngay **Phase 1, Task 1.1** – Khắc phục tận gốc vấn đề **Durable Idempotency** trong SQLite Room schema và hoàn thiện UI cho **Deposit & Withdraw**.
2. **Kỷ luật:** Duy trì cập nhật tiến độ vào [`docs/agent/project_status.md`](file:///home/chocolat/Documents/Project/m.ebanking/docs/agent/project_status.md) sau mỗi task hoàn thành.
