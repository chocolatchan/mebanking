# ME Banking – Completion Roadmap

**Project:** `m.ebanking`  
**Architecture:** Kotlin, Jetpack Compose, MVVM, Room, Offline-first  
**Roadmap:** Phase 1–5  
**Objective:** Hoàn thiện các nghiệp vụ ngân hàng, bảo mật giao dịch, dịch vụ mở rộng, chức năng Officer/eKYC và kiểm thử toàn diện.

---

# Nguyên tắc triển khai chung

1. Đọc `.agent/work.md` và tài liệu kiến trúc trước khi thực hiện mỗi Phase.
2. Kiểm tra source code hiện tại trước khi tạo hoặc sửa thành phần.
3. Không viết lại code đang hoạt động nếu không có lý do kỹ thuật rõ ràng.
4. Không tự quyết định các nghiệp vụ đang BLOCKED hoặc TBD.
5. Không dùng mock implementation để tuyên bố một tính năng thực tế đã hoàn thành.
6. Mỗi Task phải có implementation, tests và báo cáo kết quả.
7. Tất cả dữ liệu nghiệp vụ phải được lưu trữ local bằng Room.
8. Không thêm backend, cloud service hoặc payment gateway thực tế.
9. Không sử dụng `Float` hoặc `Double` để biểu diễn tiền. Tiếp tục dùng `Long` với đơn vị VND nguyên.
10. Không chuyển sang Phase tiếp theo nếu Gate của Phase hiện tại chưa đạt yêu cầu.

Mỗi Task cần có:
- Objective.
- Scope.
- Dependencies.
- Implementation requirements.
- Test requirements.
- Deliverables.
- Acceptance criteria.

---

# PHASE 1 – CORE BANKING

**Objective:** Hoàn thiện toàn bộ nghiệp vụ ngân hàng cốt lõi, đảm bảo người dùng có thể thực hiện và theo dõi giao dịch từ UI đến database.

## Task 1.1 – Repository & Implementation Audit

**Mục tiêu:** Xác minh hiện trạng trước khi tiếp tục phát triển.

Công việc:
- Đọc `.agent/work.md`.
- Đọc `project_status.md`.
- Kiểm tra Account, Transaction, Repository, Service, DAO và UI.
- Xác định chức năng đã hoàn thành, thiếu UI, thiếu business logic hoặc chưa được kiểm thử.
- Kiểm tra các business decisions đang BLOCKED.
- Xác định code có thể tái sử dụng.

Deliverables:
- `docs/agent/phase_01/implementation_audit.md`
- Danh sách task cần bổ sung.
- Danh sách vấn đề cần xử lý trước khi triển khai.

**Acceptance criteria:**
- Không tạo implementation trùng với code đã tồn tại.
- Mọi phát hiện đều có source evidence.
- Không sửa code trong Task audit.

## Task 1.2 – Account Management Completion

**Mục tiêu:** Hoàn thiện quản lý tài khoản ngân hàng.

Công việc:
- Kiểm tra Checking, Saving và Mortgage.
- Hoàn thiện Account List.
- Hoàn thiện Account Detail.
- Hiển thị số dư và thông tin tài khoản.
- Hiển thị lịch sử giao dịch theo tài khoản.
- Hoàn thiện trạng thái loading, error và empty.
- Kiểm tra quyền truy cập tài khoản theo Customer ID.

Lưu ý:
- Saving interest calculation tiếp tục BLOCKED nếu chưa có công thức được phê duyệt.
- Mortgage repayment calculation tiếp tục BLOCKED nếu chưa có quy tắc được phê duyệt.
- Không tự đặt lãi suất, kỳ hạn hoặc công thức tài chính.

Deliverables:
- Account UI hoàn thiện.
- ViewModel và Repository integration.
- Unit tests và database tests.

**Acceptance criteria:**
- Customer chỉ truy cập được tài khoản thuộc quyền sở hữu.
- Account data được lấy từ Room.
- Không hiển thị dữ liệu tài chính sai lệch.

## Task 1.3 – Deposit Workflow

**Mục tiêu:** Hoàn thiện luồng nạp tiền.

Công việc:
- Tạo Deposit Screen.
- Chọn tài khoản nhận tiền.
- Nhập số tiền.
- Kiểm tra số tiền hợp lệ.
- Hiển thị màn hình xác nhận.
- Thực hiện nghiệp vụ nạp tiền.
- Lưu transaction record.
- Hiển thị kết quả và Transaction ID.

Tests:
- Deposit thành công.
- Amount bằng 0.
- Amount âm.
- Long overflow.
- Account không tồn tại.
- Database exception.
- Duplicate request.

**Acceptance criteria:**
- Balance và transaction record được cập nhật atomic.
- Không thể tạo giao dịch trùng khi retry.
- Kết quả được lưu bền vững.

## Task 1.4 – Withdraw Workflow

**Mục tiêu:** Hoàn thiện luồng rút tiền.

Công việc:
- Tạo Withdraw Screen.
- Chọn tài khoản.
- Nhập số tiền.
- Kiểm tra số dư.
- Xác nhận giao dịch.
- Thực hiện debit.
- Lưu transaction record.
- Hiển thị kết quả.

Tests:
- Withdraw thành công.
- Insufficient balance.
- Amount bằng 0 hoặc âm.
- Overflow và underflow.
- Account không tồn tại.
- Concurrent withdrawals.
- Database rollback.

**Acceptance criteria:**
- Không cho phép số dư âm.
- Không thể rút nhiều hơn số dư khả dụng.
- Balance và ledger luôn nhất quán.

## Task 1.5 – Internal Transfer Completion

**Mục tiêu:** Hoàn thiện chuyển khoản nội bộ.

Công việc:
- Hoàn thiện Transfer Screen.
- Chọn tài khoản nguồn.
- Nhập tài khoản nhận.
- Nhập số tiền và nội dung.
- Kiểm tra ownership.
- Kiểm tra tài khoản nguồn và đích.
- Hiển thị thông tin xác nhận.
- Thực hiện chuyển khoản.
- Lưu debit/credit transaction records.

Tests:
- Chuyển khoản thành công.
- Tự chuyển khoản.
- Destination không tồn tại.
- Insufficient balance.
- Duplicate request.
- Concurrent requests.
- Rollback khi destination credit thất bại.
- Process interruption.

**Acceptance criteria:**
- Debit và credit atomic.
- Không tạo giao dịch một chiều.
- Idempotency được bảo vệ bằng database constraint và dữ liệu lưu bền vững.

## Task 1.6 – Interbank Transfer Simulator

**Mục tiêu:** Hoàn thiện giả lập chuyển khoản liên ngân hàng.

Công việc:
- Danh sách ngân hàng.
- Tìm kiếm ngân hàng.
- Nhập tài khoản nhận.
- Kiểm tra thông tin người nhận bằng dữ liệu demo.
- Tạo transaction request.
- Giả lập xử lý liên ngân hàng.
- Lưu trạng thái và transaction reference.
- Hiển thị lịch sử giao dịch.

Tests:
- Ngân hàng hợp lệ.
- Ngân hàng không tồn tại.
- Tài khoản nhận không hợp lệ.
- Giao dịch thất bại.
- Duplicate request.
- Retry và rollback.

**Acceptance criteria:**
- Không gọi ngân hàng hoặc payment gateway thực tế.
- Dữ liệu giả lập được lưu trong Room.
- Trạng thái giao dịch được thể hiện chính xác.

## Task 1.7 – Transaction History & Detail

**Mục tiêu:** Hoàn thiện khả năng tra cứu giao dịch.

Công việc:
- Transaction History Screen.
- Lọc theo tài khoản.
- Lọc theo loại giao dịch.
- Lọc theo trạng thái.
- Lọc theo khoảng thời gian.
- Transaction Detail Screen.
- Hiển thị reference, timestamp, amount, sender, receiver và status.
- Phân trang hoặc tải dữ liệu theo batch nếu cần.

Tests:
- Empty history.
- Nhiều loại giao dịch.
- Filter combinations.
- Transaction detail không tồn tại.
- Quyền truy cập transaction.
- Flow cập nhật dữ liệu.

**Acceptance criteria:**
- Lịch sử phản ánh đúng database.
- Không hiển thị giao dịch của người dùng khác.
- Không có dữ liệu giả lập cứng trong UI production.

## Task 1.8 – Transaction Integrity Hardening

**Mục tiêu:** Kiểm tra tính toàn vẹn tài chính xuyên suốt.

Công việc:
- Audit tất cả transaction write paths.
- Audit atomic database transactions.
- Kiểm tra conditional debit.
- Kiểm tra balance constraints.
- Thiết kế durable idempotency.
- Đảm bảo idempotency key có unique constraint.
- Lưu transaction result cùng transaction key trong cùng atomic transaction.
- Kiểm tra concurrent requests.
- Kiểm tra process restart và recovery.

**Acceptance criteria:**
- Không thể thực hiện cùng một giao dịch nhiều lần do retry.
- Idempotency tồn tại sau process death và restart.
- Ledger và balance không bị mất đồng bộ.
- Có test rollback và concurrency.

## PHASE 1 GATE

Chỉ PASS khi:
- Tất cả nghiệp vụ cốt lõi có UI, ViewModel, Service, Repository và DAO.
- Các transaction được lưu bền vững.
- Atomicity và idempotency được xác minh.
- Transaction History hoạt động.
- Unit tests và integration tests đạt yêu cầu.
- Không còn lỗi Critical/High chưa xử lý.

Saving/Mortgage formulas không thuộc điều kiện bắt buộc để tự động mở khóa. Nếu chưa được phê duyệt, phải tiếp tục hiển thị BLOCKED.

---

# PHASE 2 – TRANSACTION SECURITY

**Objective:** Xây dựng quy trình xác thực giao dịch thống nhất, áp dụng System Authentication trước phương thức xác thực theo giá trị giao dịch.

## Transaction Authentication Policy

### Quy tắc đã chốt

1. Mọi giao dịch cần System Authentication trước.
2. Giao dịch có giá trị không vượt quá 10.000.000 VND sử dụng In-app OTP.
3. Giao dịch có giá trị lớn hơn 10.000.000 VND sử dụng Face Recognition.
4. OTP được làm mới mỗi 30 giây.
5. Giao dịch hết hạn sau 1 phút nếu chưa hoàn thành xác thực.
6. Xác thực thất bại, bị hủy hoặc timeout không được thực hiện giao dịch.
7. Không tự động thêm OTP vào nhánh Face Recognition nếu chưa có quyết định mới.

### Authentication Flow

`Transaction Request → System Authentication → Amount Policy → OTP / Face Recognition → Authorization → Transaction Execution`

## Task 2.1 – Authentication Architecture Audit

**Mục tiêu:** Đánh giá authentication hiện tại và chuẩn hóa policy.

Công việc:
- Kiểm tra TransactionAuthenticationService.
- Kiểm tra Authentication Policy.
- Kiểm tra SessionState.
- Kiểm tra FaceRecognitionService.
- Xác định các thành phần có thể tái sử dụng.
- Thiết kế transaction authentication state machine.
- Định nghĩa authentication result riêng với transaction result.

Deliverables:
- `docs/agent/phase_02/authentication_architecture.md`
- Authentication flow diagram.
- Transaction authentication state diagram.

**Acceptance criteria:**
- Không có hai nơi tự quyết định policy khác nhau.
- Mọi kết quả xác thực gắn với đúng transaction challenge.
- Không thực thi giao dịch trước khi authorization thành công.

## Task 2.2 – System Authentication

**Mục tiêu:** Tích hợp xác thực của hệ thống Android.

Công việc:
- Tích hợp AndroidX Biometric.
- Tích hợp Android BiometricPrompt.
- Hỗ trợ biometric authentication.
- Hỗ trợ device credential theo cấu hình Android.
- Kiểm tra khả năng xác thực của thiết bị.
- Xử lý biometric unavailable.
- Xử lý user cancellation.
- Xử lý lockout và authentication failure.
- Quản lý lifecycle khi Activity hoặc ứng dụng bị dừng.

Tests:
- Biometric available.
- Device credential available.
- Biometric unavailable.
- User cancellation.
- Authentication failure.
- Lockout.
- Lifecycle interruption.

**Acceptance criteria:**
- System Authentication là bước bắt buộc.
- Không lưu PIN hoặc mật khẩu khóa màn hình.
- Có xác minh trên Emulator hoặc thiết bị Android thực tế.
- Không giả lập thành công khi Android API chưa được gọi.

## Task 2.3 – In-app OTP Implementation

**Mục tiêu:** Xây dựng OTP hiển thị trong ứng dụng và yêu cầu người dùng tự nhập.

Công việc:
- Tạo transaction-specific OTP challenge.
- Sinh OTP bằng CSPRNG.
- Hiển thị OTP trên màn hình.
- Tạo ô nhập OTP.
- Người dùng phải tự nhập.
- OTP được thay đổi mỗi 30 giây.
- OTP cũ mất hiệu lực khi mã mới được phát hành.
- Challenge hết hạn sau 60 giây.
- Kiểm tra OTP và challenge.
- Giới hạn số lần nhập sai.
- Xử lý hủy và timeout.
- Không ghi OTP vào log.

Lưu ý:
- OTP phải gắn với transaction ID hoặc challenge ID.
- Không được chấp nhận OTP của giao dịch khác.
- Không được dựa duy nhất vào countdown UI để kiểm tra hết hạn.
- Phải kiểm tra thời gian bằng timestamp.
- OTP không được sử dụng lại.

Tests:
- OTP đúng.
- OTP sai.
- OTP hết hạn.
- OTP thay đổi sau 30 giây.
- OTP cũ bị từ chối.
- OTP của transaction khác.
- Nhập sai quá số lần cho phép.
- Timeout sau 60 giây.
- App background/foreground.
- Process restart.
- Clock boundary.

**Acceptance criteria:**
- Người dùng luôn phải tự nhập OTP.
- OTP cũ không còn hợp lệ sau khi rotation.
- Transaction challenge hết hạn sau 60 giây.
- Không có transaction execution khi OTP chưa được xác minh.

**Security note:** In-app OTP trên cùng thiết bị là bước xác nhận chủ động, không được tuyên bố là một yếu tố 2FA độc lập.

## Task 2.4 – Face Recognition Transaction Authentication Integration

**Mục tiêu:** Tích hợp Face Recognition vào policy xác thực giao dịch giá trị cao.

Công việc:
- Kiểm tra FaceRecognitionService hiện tại.
- Chuẩn hóa FaceRecognitionResult.
- Tích hợp transaction challenge.
- Kiểm tra giá trị giao dịch.
- Chuyển sang Face Recognition khi amount > 10.000.000 VND.
- Xử lý Approved, Rejected, Failed và PendingPolicy.
- Không chấp nhận embedding giả lập làm bằng chứng Face Recognition thực tế.
- Chuẩn bị interface cho camera/model inference thật.

**Dependency:** Task 4.4 phải hoàn thiện Camera và MobileFaceNet inference thực tế trước khi có thể nghiệm thu đầy đủ khả năng nhận diện.

Tests:
- Amount đúng 10 triệu.
- Amount lớn hơn 10 triệu.
- Face match.
- Face mismatch.
- Camera unavailable.
- Multiple faces.
- Poor image quality.
- Model error.
- Timeout.
- User cancellation.

**Acceptance criteria:**
- Policy chọn đúng phương thức.
- Không có fallback tự động bỏ qua Face Recognition.
- Chưa có model inference thực tế thì phải báo PARTIAL, không PASS.

## Task 2.5 – Transaction Authorization Orchestration

**Mục tiêu:** Kết nối System Authentication, OTP/Face Recognition và Transaction Execution.

Công việc:
- Tích hợp authentication policy vào Deposit, Withdraw, Transfer và các payment flows.
- Tạo transaction challenge.
- Gắn authentication attempt với transaction.
- Kiểm tra challenge trước khi thực thi.
- Đảm bảo challenge không thể dùng lại.
- Đảm bảo transaction không thực thi hai lần.
- Hủy giao dịch khi authentication timeout.
- Xử lý process interruption.

**Acceptance criteria:**
- Tất cả giao dịch có authentication gate.
- Không thể gọi trực tiếp repository để bỏ qua policy từ UI/business flow.
- Authentication success không đồng nghĩa transaction success.
- Transaction success chỉ được xác nhận sau database commit.

## PHASE 2 GATE

Chỉ PASS khi:
- System Authentication hoạt động.
- OTP rotation và expiry hoạt động.
- Face Recognition được tích hợp với transaction policy.
- Transaction authorization không thể bị bypass.
- Có tests cho cả ba bước.
- Có kiểm thử thiết bị thực tế cho Android BiometricPrompt.

Nếu Camera/MobileFaceNet chưa được hoàn thiện ở Phase 4, phần Face Recognition chỉ được đánh dấu PARTIAL.

---

# PHASE 3 – EXTENDED SERVICE SIMULATORS

**Objective:** Mở rộng ứng dụng với các dịch vụ thanh toán và đặt dịch vụ giả lập, toàn bộ dữ liệu và nghiệp vụ được xử lý local.

## Task 3.1 – Simulator Architecture & Domain Design

**Mục tiêu:** Thiết kế kiến trúc chung cho các dịch vụ mở rộng.

Công việc:
- Audit DefaultSimulatorService.
- Xác định các thành phần dùng chung.
- Thiết kế domain model riêng cho từng dịch vụ.
- Thiết kế Payment Orchestration.
- Thiết kế Order Lifecycle.
- Thiết kế catalog và demo data.
- Xác định transaction authorization integration.
- Xác định database entities và relations.

Deliverables:
- `docs/agent/phase_03/simulator_architecture.md`
- Domain diagram.
- Database ERD.
- State diagrams.

**Acceptance criteria:**
- Không nhồi tất cả dịch vụ vào một service chung.
- Mỗi loại dịch vụ có domain model riêng.
- Không tạo nghiệp vụ thanh toán thật.

## Task 3.2 – Utility Payment Completion

**Mục tiêu:** Hoàn thiện các dịch vụ điện, nước và nạp tiền điện thoại.

Công việc:
- Utility provider catalog.
- Customer code lookup bằng demo data.
- Bill inquiry.
- Bill detail.
- Bill payment.
- Transaction authorization.
- Payment history.
- Payment reference.

Tests:
- Bill hợp lệ.
- Bill không tồn tại.
- Bill đã thanh toán.
- Insufficient balance.
- Duplicate payment.
- Payment rollback.
- Authorization failure.

**Acceptance criteria:**
- Payment được xử lý atomic.
- Không thể thanh toán cùng một bill nhiều lần.
- Dữ liệu được lưu trong Room.

## Task 3.3 – Flight Booking Simulator

**Mục tiêu:** Giả lập đặt vé máy bay.

Công việc:
- Flight catalog.
- Airport catalog.
- Search flights.
- Flight detail.
- Passenger information.
- Seat selection nếu thuộc phạm vi dữ liệu demo.
- Booking creation.
- Payment.
- E-ticket reference.
- Booking history.

Tests:
- Flight không tồn tại.
- Passenger validation.
- Booking thành công.
- Payment thất bại.
- Duplicate booking.
- Payment retry.

**Acceptance criteria:**
- Booking và Payment có lifecycle riêng.
- Không tạo booking đã thanh toán khi payment thất bại.
- Booking được lưu bền vững.

## Task 3.4 – Hotel Booking Simulator

**Mục tiêu:** Giả lập đặt phòng khách sạn.

Công việc:
- Hotel catalog.
- Room catalog.
- Search hotel.
- Search availability bằng dữ liệu demo.
- Room detail.
- Guest information.
- Booking creation.
- Payment.
- Booking reference.
- Booking history.

Tests:
- Hotel không tồn tại.
- Room không khả dụng.
- Invalid date range.
- Booking thành công.
- Payment thất bại.
- Duplicate request.

**Acceptance criteria:**
- Không đặt phòng vượt quá dữ liệu availability giả lập.
- Booking và payment nhất quán.
- Dữ liệu được lưu trong Room.

## Task 3.5 – Cinema Booking Simulator

**Mục tiêu:** Giả lập mua vé xem phim.

Công việc:
- Movie catalog.
- Cinema catalog.
- Showtime.
- Seat map.
- Seat selection.
- Booking creation.
- Payment.
- E-ticket.
- Booking history.

Tests:
- Movie không tồn tại.
- Showtime không hợp lệ.
- Seat unavailable.
- Duplicate seat booking.
- Payment failure.
- Booking retry.

**Acceptance criteria:**
- Không bán trùng một ghế trong cùng suất chiếu.
- Seat reservation và payment được quản lý rõ ràng.
- Có transaction reference.

## Task 3.6 – E-commerce Simulator

**Mục tiêu:** Giả lập mua sắm trực tuyến.

Công việc:
- Product catalog.
- Product detail.
- Search và filter.
- Shopping cart.
- Order creation.
- Quantity validation.
- Payment.
- Order history.
- Order detail.

Tests:
- Product không tồn tại.
- Quantity không hợp lệ.
- Cart empty.
- Insufficient balance.
- Duplicate order.
- Payment failure.

**Acceptance criteria:**
- Không cho phép quantity âm hoặc bằng 0.
- Giá trị đơn hàng được tính bằng Long.
- Order và payment được lưu nhất quán.

## Task 3.7 – Shared Payment & Order Lifecycle

**Mục tiêu:** Chuẩn hóa cơ chế thanh toán cho các simulator.

Công việc:
- Xây dựng Payment Orchestration.
- Tích hợp Transaction Authorization.
- Chuẩn hóa payment reference.
- Chuẩn hóa trạng thái payment.
- Chuẩn hóa order status.
- Kiểm tra idempotency.
- Kiểm tra atomic debit và payment record.
- Kiểm tra duplicate payment.
- Xử lý process interruption.

State lifecycle tham khảo:

`CREATED → PENDING_PAYMENT → PAID → COMPLETED`

Các trạng thái FAILED, CANCELLED, REFUNDED chỉ được triển khai khi quy tắc nghiệp vụ tương ứng được xác định.

**Acceptance criteria:**
- Mọi dịch vụ sử dụng cùng nguyên tắc transaction integrity.
- Không debit hai lần.
- Không đánh dấu payment thành công trước database commit.
- Không tự đặt chính sách phí, hoàn tiền hoặc hủy.

## PHASE 3 GATE

Chỉ PASS khi:
- Tất cả simulator được triển khai riêng.
- Có UI, ViewModel, Service, Repository, DAO.
- Catalog và demo data đầy đủ.
- Payment integration hoạt động.
- Có unit, integration và database tests.
- Không có external payment calls.

---

# PHASE 4 – OFFICER & eKYC

**Objective:** Hoàn thiện chức năng Officer và triển khai nhận diện khuôn mặt thực tế.

## Task 4.1 – Officer Feature Audit

**Mục tiêu:** Audit chức năng Officer hiện tại.

Công việc:
- Kiểm tra OfficerCustomerService.
- Kiểm tra RBAC.
- Kiểm tra Customer creation.
- Kiểm tra Customer update.
- Kiểm tra profile access.
- Kiểm tra các API/local repository liên quan.
- Xác định UI còn thiếu.

Deliverables:
- `docs/agent/phase_04/officer_feature_audit.md`

**Acceptance criteria:**
- Phân biệt rõ nghiệp vụ đã có và UI còn thiếu.
- Không tạo chức năng vượt quyền.

## Task 4.2 – Officer Dashboard

**Mục tiêu:** Xây dựng giao diện làm việc cho Officer.

Công việc:
- Officer Home/Dashboard.
- Customer List.
- Customer Search.
- Customer Detail.
- Customer Creation.
- Customer Update.
- Customer profile review.
- Navigation riêng cho Officer.
- Loading, error và empty states.

**Acceptance criteria:**
- Customer không truy cập Officer Dashboard.
- Officer chỉ thực hiện thao tác được cấp quyền.
- UI sử dụng dữ liệu thật từ Room.

## Task 4.3 – Customer eKYC Workflow

**Mục tiêu:** Hoàn thiện quy trình đăng ký và xác minh danh tính.

Công việc:
- Officer hỗ trợ Customer đăng ký.
- Camera preview.
- Camera capture.
- Face detection.
- Kiểm tra chất lượng ảnh.
- Xử lý không tìm thấy khuôn mặt.
- Xử lý nhiều khuôn mặt.
- Face alignment.
- Template enrollment.
- Template storage.
- Template deletion.

**Acceptance criteria:**
- Camera hoạt động trên Android.
- Không lưu ảnh hoặc embedding plaintext.
- Có xử lý lỗi camera và lifecycle.
- Có chính sách bảo vệ dữ liệu sinh trắc học.

## Task 4.4 – MediaPipe & MobileFaceNet Integration

**Mục tiêu:** Thay thế embedding giả lập bằng model inference thực tế.

Công việc:
- Tích hợp MediaPipe Face Detection/Face Landmarker.
- Tích hợp MobileFaceNet model.
- Chuẩn hóa preprocessing.
- Face alignment.
- Model inference.
- Embedding extraction.
- Cosine similarity hoặc matching algorithm đã được kiểm chứng.
- Threshold configuration.
- Model error handling.
- Android Keystore integration.
- Template encryption/decryption.
- Template lifecycle.

**Acceptance criteria:**
- Model được chạy thực tế trên ảnh camera.
- Không sử dụng byte hash để tạo embedding.
- Kết quả matching được kiểm thử với dữ liệu thử nghiệm.
- Không tuyên bố liveness nếu chưa triển khai.

## Task 4.5 – High-value Transaction Face Authentication Integration

**Mục tiêu:** Kết nối Face Recognition thực tế với Phase 2.

Công việc:
- Tích hợp Face Recognition vào Transaction Authentication Policy.
- Kiểm tra transaction challenge.
- Kiểm tra face template của đúng Customer.
- Xử lý mismatch.
- Xử lý poor quality.
- Xử lý camera unavailable.
- Xử lý timeout và cancellation.
- Đảm bảo transaction không được thực thi khi nhận diện thất bại.

**Dependencies:** Task 2.4 và Task 4.4.

**Acceptance criteria:**
- Giao dịch trên 10 triệu VND sử dụng Face Recognition thực tế.
- Không có fallback bỏ qua xác thực.
- Face result gắn với đúng transaction.

## PHASE 4 GATE

Chỉ PASS khi:
- Officer Dashboard hoàn chỉnh.
- RBAC hoạt động.
- Camera capture hoạt động.
- MediaPipe và MobileFaceNet inference thực tế.
- Face template được bảo vệ.
- High-value transaction sử dụng Face Recognition thực tế.
- Có kiểm thử trên Emulator hoặc thiết bị Android.

---

# PHASE 5 – QUALITY ASSURANCE & DELIVERY

**Objective:** Kiểm thử tổng thể, xác minh bảo mật, tính ổn định và chuẩn bị bàn giao.

## Task 5.1 – Requirements & Feature Traceability Audit

**Mục tiêu:** Đối chiếu toàn bộ tính năng với yêu cầu.

Công việc:
- Đọc `.agent/work.md`.
- Đối chiếu tất cả FR/NFR.
- Đối chiếu UI, ViewModel, Service, Repository, DAO.
- Đối chiếu test cases.
- Đối chiếu runtime evidence.
- Phân loại PASS, PARTIAL, MISSING, BLOCKED, NOT VERIFIED.

Deliverable:
- `docs/agent/phase_05/feature_traceability_matrix.md`

## Task 5.2 – Unit & Integration Test Audit

**Mục tiêu:** Xác minh toàn bộ automated tests.

Công việc:
- Chạy toàn bộ unit tests.
- Chạy integration tests.
- Kiểm tra test isolation.
- Kiểm tra flaky tests.
- Kiểm tra migration tests.
- Kiểm tra concurrency tests.
- Kiểm tra authentication tests.
- Kiểm tra financial calculation tests.
- Đối chiếu test count và actual execution results.

Deliverable:
- `docs/agent/phase_05/test_execution_report.md`

## Task 5.3 – Instrumented UI & Device Testing

**Mục tiêu:** Kiểm thử trên Emulator và thiết bị thực tế.

Công việc:
- Login/Logout.
- Customer workflows.
- Officer workflows.
- Deposit/Withdraw.
- Internal/Interbank Transfer.
- Transaction History.
- OTP.
- BiometricPrompt.
- Camera capture.
- Face Recognition.
- Utility Payment.
- Flight/Hotel/Cinema Booking.
- Offline Navigation.
- Chatbot.

Kiểm tra:
- Rotation.
- App background/foreground.
- Process death.
- Device restart.
- Permission denial.
- Camera unavailable.
- Authentication cancellation.

Deliverable:
- `docs/agent/phase_05/device_test_report.md`

## Task 5.4 – Security Audit

**Mục tiêu:** Kiểm tra các rủi ro bảo mật còn tồn tại.

Công việc:
- Password hashing.
- Session management.
- RBAC.
- Biometric authentication.
- Face template encryption.
- Sensitive data storage.
- Android backup configuration.
- Database access.
- OTP security.
- Transaction authorization bypass.
- Idempotency bypass.
- Logging.
- Data deletion.

Deliverable:
- `docs/agent/phase_05/security_audit.md`

## Task 5.5 – Financial Integrity & Recovery Audit

**Mục tiêu:** Kiểm tra tính toàn vẹn tài chính khi xảy ra lỗi.

Công việc:
- Concurrent transfers.
- Concurrent withdrawals.
- Duplicate requests.
- Database exceptions.
- Process kill during transaction.
- Process restart.
- Ledger-to-balance consistency.
- Payment retry.
- Booking payment recovery.
- Transaction authentication timeout.

**Acceptance criteria:**
- Không mất tiền do partial update.
- Không debit trùng.
- Không ghi nhận giao dịch thành công giả.
- Không thể thực hiện lại giao dịch đã hoàn tất.

## Task 5.6 – Offline Runtime Audit

**Mục tiêu:** Xác minh ứng dụng thực sự hoạt động offline.

Công việc:
- Airplane mode.
- Fresh installation.
- App restart.
- Database persistence.
- Asset loading.
- Login.
- Banking workflows.
- Payment simulators.
- Chatbot.
- Offline Navigation.
- Face Recognition.
- Device Authentication.

**Acceptance criteria:**
- Không cần Internet để sử dụng các chức năng được thiết kế offline.
- Không phụ thuộc vào remote API hoặc cloud service.
- Các lỗi môi trường được xử lý rõ ràng.

## Task 5.7 – Build, Lint & Release Verification

**Mục tiêu:** Xác minh khả năng build và phát hành.

Công việc:
- Gradle Sync.
- Clean build.
- Unit tests.
- Instrumented tests.
- Lint.
- Static analysis.
- Assemble Debug.
- Assemble Release.
- Kiểm tra manifest.
- Kiểm tra dependencies.
- Kiểm tra APK.
- Kiểm tra cấu hình release.

Deliverable:
- `docs/agent/phase_05/build_release_report.md`

## Task 5.8 – Final Documentation & Handover

**Mục tiêu:** Chuẩn bị tài liệu bàn giao.

Công việc:
- Cập nhật README.
- Cập nhật project architecture.
- Cập nhật database schema.
- Cập nhật navigation map.
- Cập nhật feature list.
- Ghi lại các business decisions.
- Ghi lại known limitations.
- Hướng dẫn build và chạy.
- Hướng dẫn test.
- Tổng hợp test evidence.
- Tổng hợp security findings.

Deliverables:
- `docs/agent/phase_05/final_audit_report.md`
- `docs/agent/phase_05/handover_document.md`
- Updated README.

## PHASE 5 GATE

Chỉ PASS khi:
- Không còn Critical/High security findings chưa xử lý.
- Các nghiệp vụ chính có test evidence.
- Có kiểm thử UI trên Android.
- Có kiểm thử Camera, BiometricPrompt và Keystore.
- Offline runtime được xác minh.
- Financial integrity được xác minh.
- Debug và Release build thành công.
- Tài liệu bàn giao đầy đủ.
- Các giới hạn chưa xử lý được công khai rõ ràng.

---

# DEPENDENCY MAP

Các dependency quan trọng giữa các Phase:

| Task | Dependency |
|---|---|
| 1.2–1.8 | 1.1 |
| 2.1 | Phase 1 architecture audit |
| 2.2 | 2.1 |
| 2.3 | 2.1 |
| 2.4 | 2.1, 4.4 |
| 2.5 | 1.3–1.6, 2.2–2.4 |
| 3.1 | Phase 1, 2.5 |
| 3.2–3.6 | 3.1 |
| 3.7 | 3.2–3.6 |
| 4.2 | 4.1 |
| 4.3 | 4.2 |
| 4.4 | 4.3 |
| 4.5 | 2.4, 2.5, 4.4 |
| 5.1–5.8 | Các Phase trước |

**Lưu ý:** Phase 2 có thể xây dựng Authentication Policy và Face Recognition interface trước. Việc nghiệm thu Face Recognition thực tế phải đợi Phase 4 hoàn thành model inference. Không dùng implementation giả lập để vượt Gate.

# FINAL PROJECT COMPLETION CRITERIA

Dự án chỉ được tuyên bố hoàn thiện khi:

1. Core Banking workflows hoạt động đầy đủ.
2. Transaction Security hoạt động theo đúng policy.
3. Extended Service Simulators có nghiệp vụ và persistence riêng.
4. Officer Dashboard và eKYC hoạt động.
5. Face Recognition sử dụng model inference thực tế.
6. Unit, integration và instrumented tests có bằng chứng chạy.
7. Database và financial integrity được xác minh.
8. Ứng dụng hoạt động theo thiết kế offline-first.
9. Debug và Release builds thành công.
10. Tất cả giới hạn và business decisions còn mở được ghi rõ.

**Nguyên tắc cuối cùng:** PASS của một Phase chỉ chứng minh phạm vi Phase đó đã được xác minh. Nó không tự động chứng minh toàn bộ Mobile Banking đã sẵn sàng production.
