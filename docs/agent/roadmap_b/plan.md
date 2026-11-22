# Roadmap B — Lifestyle Simulators & Local Ecosystem Plan

**Nhánh Git:** `feature/roadmap-b-lifestyle-simulators`  
**Kiến trúc:** Kotlin, Jetpack Compose, MVVM, Room SQLite, Offline-first, VND `Long`  
**Mục tiêu:** Mở rộng toàn bộ các dịch vụ tiện ích, đặt vé và mua sắm giả lập với cơ sở dữ liệu Room và UI Jetpack Compose riêng biệt, không đụng chạm đến core banking account ledger của Roadmap A.

---

## 1. Danh sách các Task chi tiết (Thực hiện tuần tự)

| Task | Tên Task | Mục tiêu | Thành phần bàn giao (Deliverables) | Trạng thái |
|---|---|---|---|:---:|
| **Task B.1** | **Lifestyle Database Schema & DAOs** | Thiết kế các bảng Room cho Chuyến bay, Khách sạn, Rạp chiếu phim, Sản phẩm & Đơn hàng | `FlightEntity`, `HotelEntity`, `CinemaEntity`, `MovieEntity`, `ProductEntity`, `OrderEntity`, các DAOs | ✅ **DONE** |
| **Task B.2** | **Offline Catalogs & Seed Data** | Bổ sung dữ liệu mẫu phong phú (chuyến bay, khách sạn, phim + sơ đồ ghế, sản phẩm) | `LifestyleDatabaseSeeder`, Unit tests kiểm thử seed | ✅ **DONE** |
| **Task B.3** | **Flight Booking Simulator** | Giao diện và nghiệp vụ tìm kiếm chuyến bay, chọn ghế, đặt chỗ và thanh toán | `FlightBookingScreen`, `FlightViewModel`, `FlightRepository` | 🟡 **IN PROGRESS** |
| **Task B.4** | **Hotel Booking Simulator** | Giao diện và nghiệp vụ tìm phòng khách sạn, chọn ngày check-in/out, đặt phòng | `HotelBookingScreen`, `HotelViewModel`, `HotelRepository` | ⏳ PENDING |
| **Task B.5** | **Cinema Booking Simulator** | Giao diện chọn phim, suất chiếu, **sơ đồ ghế trực quan chống trùng**, xuất vé | `CinemaBookingScreen`, `CinemaViewModel`, `CinemaRepository` | ⏳ PENDING |
| **Task B.6** | **E-commerce Shopping Simulator** | Danh mục sản phẩm, chi tiết sản phẩm, **Giỏ hàng (Cart)**, tính tổng tiền VND | `ShopScreen`, `CartScreen`, `ShopViewModel`, `ShopRepository` | ⏳ PENDING |
| **Task B.7** | **Utility Payment Enhancements** | Bổ sung tra cứu mã khách hàng mẫu (EVN, Sawaco), nạp tiền điện thoại (Top-up) | Cập nhật `BillPaymentScreen`, `TopupScreen`, tests | ⏳ PENDING |
| **Task B.8** | **Integration, NavHost & Full Suite Test** | Đăng ký toàn bộ màn hình vào `MEbankingApp` NavHost, chạy full test suite | Navigation routes, E2E Simulator Tests, Gate B Passed | ⏳ PENDING |

---

## 2. Tiêu chuẩn hoàn thành (Definition of Done) cho mỗi Task
1. Đúng phạm vi, độc lập hoàn toàn với code core banking của Roadmap A.
2. Tiền tệ duy trì tuyệt đối kiểu `Long` với đơn vị VND nguyên (không dùng Float/Double).
3. Hoạt động 100% offline, lưu trữ bền vững trong Room SQLite.
4. Có unit/integration test chứng minh hành vi.
5. Biên dịch sạch không lỗi (`./gradlew testDebugUnitTest`).
