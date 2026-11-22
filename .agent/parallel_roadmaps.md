# ME Banking — Dual Parallel Roadmaps (Kế hoạch 2 nhánh song song)

**Mục tiêu:** Tách bạch toàn bộ các công việc cần sửa lỗi (từ `.agent/fix.md`) và các tính năng còn thiếu thành **2 nhánh độc lập (Track A & Track B)**.  
Hai nhánh này có sự phân tách hoàn toàn về **File Ownership**, **Database Entities**, và **Tầng nghiệp vụ**, cho phép thực hiện song song mà không xung đột mã nguồn (merge conflict) hay bị phụ thuộc chéo (blocking dependencies).

---

## Sơ đồ kiến trúc 2 nhánh song song

```
┌────────────────────────────────────────────────────────┐  ┌────────────────────────────────────────────────────────┐
│  ROADMAP A: CORE BANKING, SECURITY & OFFICER PLATFORM  │  │ ROADMAP B: LIFESTYLE SIMULATORS & LOCAL ECOSYSTEM      │
│  (Trọng tâm: Tài chính, Bảo mật phần cứng & Quyền hạn) │  │ (Trọng tâm: Dịch vụ mở rộng, Đặt chỗ & Thương mại)     │
├────────────────────────────────────────────────────────┤  ├────────────────────────────────────────────────────────┤
│ • Fix Database Idempotency (Room Schema v2)            │  │ • Bảng dữ liệu độc lập: Chuyến bay, Khách sạn, Rạp... │
│ • Deposit & Withdraw UI (Compose)                      │  │ • Flight Booking Simulator (Tìm kiếm, Chọn ghế, Vé)    │
│ • Transaction History & Filter Screen                  │  │ • Hotel Booking Simulator (Phòng, Ngày, Đặt phòng)     │
│ • System Auth (AndroidX BiometricPrompt)               │  │ • Cinema Simulator (Suất chiếu, Sơ đồ ghế, Chống trùng)│
│ • In-app OTP State Machine (GD <= 10 triệu)            │  │ • E-commerce Simulator (Giỏ hàng, Sản phẩm, Đơn hàng)  │
│ • Officer Dashboard & Customer Management UI           │  │ • Hoàn thiện Utility Payments (Điện, Nước, Topup)      │
│ • MediaPipe + MobileFaceNet TFLite Inference           │  │ • Cải tiến Chatbot nhận diện Intent đặt dịch vụ        │
│ • Customer eKYC Camera Flow                            │  │ • Bản đồ Offline Navigation dẫn đường chi nhánh        │
└───────────────────────────┬────────────────────────────┘  └───────────────────────────┬────────────────────────────┘
                            │                                                           │
                            └───────────────────────────┬───────────────────────────────┘
                                                        │
                                                        ▼
                            ┌────────────────────────────────────────────────────────┐
                            │    PHASE 5: CONVERGENCE & FINAL PROJECT DELIVERY       │
                            │    • Hợp nhất 2 nhánh và kiểm thử E2E tích hợp        │
                            │    • Chạy Instrumented Android Tests (androidTest)     │
                            │    • Build Release APK & Kịch bản Demo Video 16 bước   │
                            └────────────────────────────────────────────────────────┘
```

---

## 1. Phân định quyền sở hữu và ranh giới (Separation of Concerns)

| Tiêu chí | **Roadmap A (Core & Security Platform)** | **Roadmap B (Lifestyle Simulators)** |
|---|---|---|
| **Packages phụ trách** | `app/.../data/local/` (core entities)<br>`app/.../domain/service/` (auth, transaction, officer)<br>`app/.../ui/auth/`, `ui/account/`, `ui/transfer/`, `ui/deposit/`, `ui/withdraw/`, `ui/officer/` | `app/.../data/local/entity/lifestyle/`<br>`app/.../data/repository/lifestyle/`<br>`app/.../domain/model/lifestyle/`<br>`app/.../ui/bills/`, `ui/flights/`, `ui/hotels/`, `ui/cinema/`, `ui/shop/` |
| **Bảng Room SQLite** | `users`, `accounts`, `transactions` (cập nhật cột `idempotency_key`), `otp_codes` | `bills`, `flights`, `flight_bookings`, `hotels`, `hotel_bookings`, `cinemas`, `movies`, `cinema_bookings`, `products`, `orders` |
| **Điểm giao tiếp chung** | Cung cấp interface chuẩn: `TransactionService.executeTransfer()` và `AuthenticationPolicy` | Chỉ gọi checkout qua adapter: Trừ tiền tài khoản nguồn khi người dùng xác nhận thanh toán đơn hàng |
| **Rủi ro xung đột (Conflict)** | **GẦN NHƯ BẰNG 0** do không sửa chung file giao diện hay file repository |

---

## 2. ROADMAP A: Core Banking, Transaction Security & Officer Platform

**Mục tiêu:** Đảm bảo tính toán vẹn tài chính cấp doanh nghiệp, bảo mật phần cứng và quyền quản trị Officer.

### Giai đoạn A1: Core Banking Hardening & Bền vững Idempotency
* [ ] **Task A1.1 — Database Migration for Durable Idempotency (Fix DEF-4.2-01):**
  - Cập nhật `TransactionEntity.kt`: Thêm trường `idempotencyKey: String?` với chỉ mục `Index(value = ["idempotency_key"], unique = true)`.
  - Nâng cấp Room Database lên Version 2, viết migration bảo toàn dữ liệu cũ.
  - Cập nhật `DefaultTransactionRepository.kt`: Ghi nhận `idempotencyKey` trực tiếp vào database transaction. Khi phát hiện key đã tồn tại, trả về bản ghi cũ mà không trừ tiền lần hai ngay cả khi app bị restart.
* [ ] **Task A1.2 — Deposit & Withdraw Screens (Compose):**
  - Viết `DepositScreen.kt` & `DepositViewModel.kt` (chọn tài khoản, nhập số tiền, dialog xác nhận, sinh Idempotency key).
  - Viết `WithdrawScreen.kt` & `WithdrawViewModel.kt` (kiểm tra số dư, cảnh báo không vượt hạn mức, xác nhận).
* [ ] **Task A1.3 — Transaction History & Transaction Detail Screen:**
  - Viết `TransactionHistoryScreen.kt` và `TransactionDetailScreen.kt` với bộ lọc (tất cả, nạp, rút, chuyển khoản, hóa đơn).

### Giai đoạn A2: Transaction Security (Biometric + In-app OTP)
* [ ] **Task A2.1 — System Authentication (AndroidX BiometricPrompt):**
  - Tích hợp `BiometricPrompt` vào luồng xác thực hệ thống (Vân tay / Mở khóa màn hình thiết bị).
  - Xử lý các tình huống: `BIOMETRIC_SUCCESS`, `DEVICE_CREDENTIAL_ALLOWED`, hủy bỏ hoặc khóa tạm thời.
* [ ] **Task A2.2 — In-App OTP State Machine (GD <= 10.000.000 VND):**
  - Xây dựng `OtpService`: sinh mã OTP 6 chữ số ngẫu nhiên an toàn (`SecureRandom`).
  - Tự động thay đổi mã mỗi 30 giây, thời hạn challenge tối đa 60 giây.
  - Thiết kế `OtpVerificationDialog` yêu cầu người dùng tự đọc và nhập mã hiển thị trên màn hình.
* [ ] **Task A2.3 — Transaction Authorization Orchestrator:**
  - Hoàn thiện điều phối: Bước 1 bắt buộc System Auth -> Bước 2 phân luồng theo hạn mức (dưới hoặc bằng 10M dùng OTP, trên 10M chuyển sang Face Recognition).

### Giai đoạn A3: Officer Dashboard & Real Face Inference
* [ ] **Task A3.1 — Officer Dashboard Screen:**
  - Giao diện dành riêng cho `Role.OFFICER`: Tìm kiếm khách hàng theo CCCD/Họ tên, tạo khách hàng mới, cập nhật thông tin.
* [ ] **Task A3.2 — Customer eKYC Camera Flow:**
  - Tích hợp CameraX preview và chụp ảnh trực tiếp trên thiết bị Android, kiểm tra độ sáng và góc khuôn mặt.
* [ ] **Task A3.3 — Real Face Inference (MediaPipe + MobileFaceNet TFLite):**
  - Đóng gói model `mobilefacenet.tflite` trong `assets/models/`.
  - Thay thế thuật toán băm byte giả lập bằng pipeline trích xuất vector 128-d thực tế từ khuôn mặt.
  - Kết nối với luồng giao dịch trên 10.000.000 VND.

---

## 3. ROADMAP B: Lifestyle Simulators & Local Ecosystem

**Mục tiêu:** Xây dựng toàn bộ hệ sinh thái dịch vụ tiện ích, đặt vé và mua sắm giả lập hoạt động 100% offline với cơ sở dữ liệu Room riêng.

### Giai đoạn B1: Thiết kế thực thể và Dữ liệu Seed Offline
* [ ] **Task B1.1 — Lifestyle Database Schema:**
  - Tạo các Entity và DAO độc lập:
    - Chuyến bay: `FlightEntity`, `FlightBookingEntity` (`FlightDao`)
    - Khách sạn: `HotelEntity`, `HotelBookingEntity` (`HotelDao`)
    - Rạp chiếu phim: `MovieEntity`, `CinemaEntity`, `CinemaBookingEntity` (`CinemaDao`)
    - Mua sắm: `ProductEntity`, `OrderEntity`, `OrderItemEntity` (`ShopDao`)
* [ ] **Task B1.2 — Offline Catalog Seed Data:**
  - Bổ sung vào `DatabaseSeeder`: Chuyến bay SGN-HAN/DAD, khách sạn TP.HCM/Đà Lạt/Nha Trang, phim đang chiếu + sơ đồ ghế 50 vị trí, danh mục 20 sản phẩm điện tử.

### Giai đoạn B2: Triển khai các Simulator nghiệp vụ
* [ ] **Task B2.1 — Flight Booking Simulator:**
  - Màn hình tìm kiếm vé, danh sách chuyến bay, nhập thông tin hành khách, chọn ghế, xuất mã đặt chỗ và thanh toán.
* [ ] **Task B2.2 — Hotel Booking Simulator:**
  - Màn hình tìm kiếm khách sạn, chọn ngày check-in / check-out, xem chi tiết phòng, xác nhận đặt phòng và thanh toán.
* [ ] **Task B2.3 — Cinema Booking Simulator:**
  - Màn hình chọn phim, chọn suất chiếu, **sơ đồ chọn ghế tương tác trực quan**, khóa ghế chống đặt trùng, thanh toán vé xem phim.
* [ ] **Task B2.4 — E-commerce Shopping Simulator:**
  - Màn hình danh mục sản phẩm, xem chi tiết, giỏ hàng (Shopping Cart), tính tổng tiền VND, thanh toán đơn hàng.
* [ ] **Task B2.5 — Hoàn thiện Utility Payments (Điện, Nước, Viễn thông):**
  - Thêm tra cứu mã khách hàng mẫu (EVN, Sawaco), nạp tiền điện thoại (Top-up).

### Giai đoạn B3: Tích hợp hệ sinh thái nội bộ
* [ ] **Task B3.1 — Shared Payment & Order Lifecycle:**
  - Chuẩn hóa vòng đời mọi đơn hàng simulator: `CREATED → PENDING_PAYMENT → PAID → COMPLETED`.
* [ ] **Task B3.2 — Chatbot & Navigation Linking:**
  - Bổ sung intent trong chatbot để gợi ý đặt vé máy bay, rạp chiếu phim, khách sạn; dẫn đường bản đồ offline tới các chi nhánh/ATM gần nhất.

---

## 4. PHASE 5: Hợp nhất, Kiểm thử toàn diện & Bàn giao (Convergence Gate)

Sau khi cả 2 Roadmap A & B hoàn thành các giai đoạn riêng:

1. **Kiểm thử tích hợp (Integrated E2E):**
   - Chạy toàn bộ 437 unit/integration test cũ + các bộ test mới của Roadmap A và B.
   - Viết bộ **Instrumented Android UI Tests (`app/src/androidTest`)** xác minh tương tác màn hình trên thiết bị/emulator.
2. **Kiểm tra tiêu chuẩn đồ án:**
   - Đối chiếu từng tiêu chí với file đề bài gốc [`FinalProject_2026_2027.pdf`](file:///home/chocolat/Downloads/FinalProject_2026_2027.pdf).
   - Kiểm tra build bản phát hành `./gradlew assembleRelease` (tối ưu Proguard/R8, không rò rỉ log).
3. **Bộ tài liệu bàn giao tốt nghiệp:**
   - `final_project_report.md` (Báo cáo đầy đủ đồ án).
   - `demo_script.md` (Kịch bản quay video demo 16 bước).
   - `submission_checklist.md` (Danh mục kiểm tra nộp bài).
