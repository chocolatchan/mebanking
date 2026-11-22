# MOBILE BANKING PROJECT
# Agent Task & Prompt Execution Pack

**Workflow:** Sequential Agent Execution with Quality Gates  
**Architecture:** Android + MVVM + Room/SQLite + Offline-first  
**Development strategy:** Requirements-driven, incremental implementation, independent verification.

---

# A. GLOBAL INSTRUCTIONS

## A1. Project context

Nguồn yêu cầu chính:

1. `FinalProject_2026_2027(1).pdf`: đề bài chính thức.
2. `Sơ đồ thiết kế app ngân hàng.md`: thiết kế ban đầu.

Nếu tài liệu nằm ở vị trí khác, hãy xác định đúng tài liệu trước khi làm việc. Không tự tạo nội dung thay thế khi không đọc được nguồn.

Định hướng dự án:

- Android Mobile Banking.
- MVVM architecture.
- Repository Pattern.
- Room / SQLite.
- Local-first, hoạt động không cần Internet.
- Không kết nối API ngân hàng, cổng thanh toán, nhà cung cấp tiện ích, chatbot hoặc bản đồ bên ngoài.
- Sử dụng module mô phỏng nội bộ cho các nghiệp vụ cần dịch vụ bên ngoài.

Các quyết định nghiệp vụ đã xác định:

| Hạng mục | Quyết định |
|---|---|
| Giao dịch thông thường | Android Device Authentication |
| Giao dịch giá trị cao | Face Recognition trong ứng dụng |
| Face recognition | MediaPipe Face Detection / Face Landmarker + MobileFaceNet |
| Ngưỡng giao dịch giá trị cao | TBD |
| Xác thực thiết bị bổ sung cho giao dịch giá trị cao | TBD |
| Database | SQLite / Room |
| Payment | Internal Simulator |
| Interbank transfer | ExternalBankSimulator |
| Navigation | Offline map + local road graph |
| Chatbot | Rule-based / Intent-based |

Không được tự ý thay đổi các quyết định trên.

## A2. Quy tắc dành cho tất cả agent

1. Đọc code và tài liệu hiện tại trước khi thay đổi.
2. Không viết lại module đang hoạt động nếu chưa chứng minh cần thiết.
3. Không tự tạo yêu cầu nghiệp vụ ngoài tài liệu.
4. Không đánh dấu chức năng hoàn thành chỉ vì đã có UI.
5. Mọi chức năng phải có implementation và bằng chứng kiểm thử.
6. Không tự ý thay đổi kiến trúc đã được phê duyệt.
7. Không tự ý thay đổi database schema mà không cập nhật migration.
8. Không lưu mật khẩu dạng plaintext.
9. Không lưu PIN, mật khẩu thiết bị hoặc dữ liệu xác thực sinh trắc học của Android.
10. Không sử dụng `Double` hoặc `Float` để lưu trữ tiền. Dùng `Long` theo đơn vị VND.
11. Không đưa API key, password, token hoặc secret vào repository.
12. Không tự ý thêm dependency nếu chưa đánh giá sự cần thiết, tính tương thích và khả năng offline.
13. Phân biệt thư viện được đóng gói trong ứng dụng với dịch vụ bên ngoài. Thư viện local được phép nếu đáp ứng ràng buộc dự án.
14. Không tự đánh giá công việc của mình là PASS mà thiếu bằng chứng.
15. Không xóa hoặc ghi đè thay đổi chưa commit của người dùng.

## A3. Quy tắc báo cáo

Mỗi task phải tạo báo cáo theo cấu trúc:

```text
Task ID:
Task Name:

1. Objective
2. Input Sources
3. Work Performed
4. Files Created
5. Files Modified
6. Technical Decisions
7. Tests Executed
8. Test Results
9. Known Limitations
10. Unresolved Questions
11. Acceptance Criteria
12. Gate Status
```

Gate Status phải là một trong:

- PASS
- CONDITIONAL PASS
- FAIL
- BLOCKED

Không được chuyển sang task phụ thuộc khi task trước chưa PASS hoặc chưa được chấp thuận rõ ràng.

---

# B. PHASE 0: REPOSITORY RECONNAISSANCE

**Mục tiêu:** Hiểu dự án hiện tại trước khi sửa đổi.

**Quyền chỉnh sửa source code:** Không được phép.

## TASK 0.1 - Repository Inventory

**Agent:** Repository Analyst

**Prompt:**

```text
Bạn là Repository Analyst.

Nhiệm vụ: Khảo sát toàn bộ Android repository để xác định trạng thái hiện tại.

KHÔNG sửa source code, dependencies, database schema hoặc configuration.

Kiểm tra:

1. Cấu trúc thư mục.
2. Android application modules.
3. Ngôn ngữ lập trình.
4. Android Gradle Plugin và Gradle.
5. minSdk, targetSdk, compileSdk.
6. Manifest và permissions.
7. Kiến trúc đang sử dụng.
8. Navigation structure.
9. UI framework.
10. Dependency Injection.
11. Room / SQLite implementation.
12. Các repository, service, ViewModel và DAO hiện có.
13. Các chức năng đã được triển khai.
14. Các chức năng đang triển khai dở.
15. Các chức năng chưa có.

Đọc các tài liệu liên quan trong repository.

Không kết luận chức năng đã hoàn thành chỉ dựa trên tên class hoặc tên file.

Tạo:

docs/agent/phase_00/repository_inventory.md
docs/agent/phase_00/project_structure.md
docs/agent/phase_00/implementation_status.md

Mỗi chức năng cần có:
- Vị trí source code.
- Mô tả hiện trạng.
- Mức độ hoàn thành sơ bộ.
- Những phần chưa xác minh được.

Kết thúc bằng báo cáo Gate Status.

DỪNG SAU KHI HOÀN THÀNH.
```

**Acceptance Criteria:**

- Có sơ đồ cấu trúc repository.
- Xác định được technology stack.
- Phân biệt được implementation thật và placeholder.
- Không có source code bị thay đổi.

## TASK 0.2 - Baseline Build & Test Audit

**Agent:** Build & Test Analyst

**Dependency:** TASK 0.1 PASS.

**Prompt:**

```text
Bạn là Build & Test Analyst.

Mục tiêu: Xác định trạng thái build và test ban đầu của repository.

KHÔNG sửa source code hoặc configuration để làm cho build thành công.

Thực hiện:

1. Kiểm tra các Gradle tasks hiện có.
2. Chạy Gradle build phù hợp.
3. Chạy unit tests.
4. Chạy instrumentation tests nếu môi trường cho phép.
5. Thu thập compile errors.
6. Thu thập test failures.
7. Kiểm tra warnings.
8. Kiểm tra dependency resolution.
9. Kiểm tra lint nếu dự án đã cấu hình.

Ghi lại chính xác:
- Command được thực thi.
- Exit code.
- Tổng số test.
- Số test passed.
- Số test failed.
- Các lỗi quan trọng.
- Những bước không thể thực thi và lý do.

Không được giả định test thành công khi chưa chạy.

Tạo:

docs/agent/phase_00/baseline_build_report.md
docs/agent/phase_00/baseline_test_report.md

DỪNG SAU KHI HOÀN THÀNH.
```

**Acceptance Criteria:**

- Có bằng chứng build/test thực tế.
- Phân loại được lỗi tồn tại trước khi triển khai.
- Không che giấu lỗi baseline.

## TASK 0.3 - Dependency & Offline Audit

**Agent:** Platform Analyst

**Dependency:** TASK 0.1 và 0.2 PASS.

**Prompt:**

```text
Bạn là Platform Analyst.

Mục tiêu: Xác định các dependency và thành phần có thể khiến ứng dụng phụ thuộc vào Internet hoặc dịch vụ bên ngoài.

Không sửa project.

Kiểm tra:

1. Gradle dependencies.
2. Android permissions.
3. Manifest services, receivers và providers.
4. Network-related libraries.
5. Firebase hoặc cloud SDK nếu có.
6. Payment SDK.
7. Map SDK.
8. Chatbot API.
9. Face recognition libraries.
10. Các API endpoint được hardcode.
11. Các HTTP clients.
12. Các thành phần cần Internet khi runtime.

Phân loại:

A. Android OS APIs:
Camera, Location, BiometricPrompt...

B. Local libraries:
Thư viện chạy trực tiếp trên thiết bị.

C. External services:
API, cloud, remote database, payment gateway...

Đối với mỗi dependency, ghi:
- Tên.
- Phiên bản.
- Vai trò.
- Có cần Internet khi runtime không?
- Có thể thay thế bằng local implementation không?

Tạo:

docs/agent/phase_00/offline_dependency_audit.md

Không được kết luận ứng dụng offline chỉ dựa trên việc không thấy API URL trong source.

DỪNG SAU KHI HOÀN THÀNH.
```

**GATE 0:** Chỉ PASS khi đã hiểu repository, baseline và các phụ thuộc hiện tại.

---

# C. PHASE 1: REQUIREMENTS ANALYSIS

**Mục tiêu:** Biến đề bài thành danh sách yêu cầu có thể truy vết và kiểm thử.

## TASK 1.1 - Requirement Extraction

**Agent:** Requirements Analyst

**Dependency:** GATE 0 PASS.

**Prompt:**

```text
Bạn là Requirements Analyst.

Nguồn chính:
FinalProject_2026_2027(1).pdf

Nguồn tham khảo:
Sơ đồ thiết kế app ngân hàng.md

Nhiệm vụ: Trích xuất toàn bộ yêu cầu từ đề bài.

Phân nhóm:

1. User Profiles.
2. Security Requirements.
3. Account Management.
4. Transaction Management.
5. Utilities.
6. Navigation.
7. AI Chatbot.
8. Technical Requirements.
9. Project Report Requirements.
10. Demo Requirements.

Mỗi yêu cầu phải có:

- Requirement ID.
- Original description.
- Vietnamese interpretation.
- Requirement type.
- Expected system behavior.
- Acceptance criteria.
- Source page and section.

Giữ nguyên ý nghĩa của đề bài.

Không tự ý bỏ yêu cầu vì dự án sử dụng offline architecture.

Không tự thêm nghiệp vụ mà nguồn không yêu cầu. Có thể ghi nhận đề xuất riêng nhưng phải đánh dấu là đề xuất.

Tạo:

docs/agent/phase_01/requirement_catalog.md

DỪNG SAU KHI HOÀN THÀNH.
```

**Acceptance Criteria:**

- Bao phủ đầy đủ Business Requirements và Technical Requirements.
- Không nhầm yêu cầu bắt buộc với đề xuất thiết kế.
- Có nguồn tham chiếu cho từng requirement.

## TASK 1.2 - Requirement Traceability Matrix

**Agent:** Requirements Analyst

**Dependency:** TASK 1.1 PASS.

**Prompt:**

```text
Bạn là Requirements Traceability Analyst.

Sử dụng:
- requirement_catalog.md
- repository_inventory.md
- implementation_status.md
- baseline_test_report.md

Tạo ma trận truy vết giữa:

Requirement
-> Use Case
-> Screen
-> Component
-> Service
-> Database Entity
-> Test Case

Phân loại trạng thái implementation:

IMPLEMENTED:
Có implementation thực tế và bằng chứng phù hợp.

PARTIALLY_IMPLEMENTED:
Đã có một phần nhưng chưa đáp ứng đầy đủ.

MISSING:
Chưa có implementation.

UNVERIFIED:
Chưa đủ bằng chứng để kết luận.

Không đánh dấu IMPLEMENTED chỉ vì:
- Có tên màn hình.
- Có tên class.
- Có diagram.
- Có mockup.
- Có TODO hoặc placeholder.

Tạo:

docs/agent/phase_01/traceability_matrix.md

DỪNG SAU KHI HOÀN THÀNH.
```

## TASK 1.3 - Gap Analysis & Open Questions

**Agent:** Requirements Auditor

**Dependency:** TASK 1.1 và 1.2 PASS.

**Prompt:**

```text
Bạn là Requirements Auditor.

Mục tiêu: Xác định tất cả khoảng trống giữa đề bài, thiết kế và implementation.

Phân tích:

1. Requirements chưa được đáp ứng.
2. Requirements chỉ có UI.
3. Requirements chưa có business logic.
4. Requirements chưa có database support.
5. Requirements chưa có test.
6. Các mâu thuẫn giữa tài liệu.
7. Các nghiệp vụ chưa có business rules.
8. Các quyết định kiến trúc còn TBD.

Đặc biệt kiểm tra:

- Đề bài yêu cầu tất cả giao dịch phải qua 2FA.
- Giao dịch thông thường sử dụng Android Device Authentication.
- Giao dịch giá trị cao sử dụng app Face Recognition.
- Ngưỡng giao dịch giá trị cao chưa xác định.
- Yêu cầu xác thực thiết bị bổ sung cho giao dịch giá trị cao chưa xác định.

Không tự kết luận các cơ chế xác thực hiện tại đã đáp ứng 2FA.

Ngoài ra, ghi nhận các nghiệp vụ chưa được định nghĩa rõ:
- Công thức tính lợi nhuận Saving.
- Công thức thanh toán Mortgage.
- Quy tắc làm tròn tiền.
- Giới hạn giao dịch.
- Quy tắc giao dịch thất bại.
- Quy tắc hủy đơn và hoàn tiền.
- Quy tắc lưu trữ dữ liệu khuôn mặt.

Phân loại mức độ:
CRITICAL / HIGH / MEDIUM / LOW.

Tạo:

docs/agent/phase_01/gap_analysis.md
docs/agent/phase_01/open_questions.md
docs/agent/phase_01/requirement_risk_register.md

Đưa ra phương án đề xuất nhưng không tự ý quyết định những nội dung cần chủ dự án phê duyệt.

DỪNG SAU KHI HOÀN THÀNH.
```

**GATE 1:** Không còn requirement bị bỏ sót. Mọi điểm chưa rõ phải được đưa vào Open Questions.

---

# D. PHASE 2: ARCHITECTURE & DATABASE DESIGN

**Mục tiêu:** Chốt thiết kế trước khi bắt đầu implementation.

## TASK 2.1 - Architecture Decision Records

**Agent:** Software Architect

**Dependency:** GATE 1 PASS.

**Prompt:**

```text
Bạn là Software Architect.

Sử dụng:
- Requirement Catalog.
- Gap Analysis.
- Open Questions.
- Repository Inventory.
- Offline Dependency Audit.

Nhiệm vụ: Xây dựng Architecture Decision Records.

Các chủ đề bắt buộc:

1. MVVM architecture.
2. Repository Pattern.
3. Dependency Injection.
4. Room / SQLite.
5. Local-first data handling.
6. Authentication.
7. Transaction processing.
8. Face recognition.
9. Device Authentication.
10. Utility simulation.
11. Interbank simulation.
12. Offline navigation.
13. Rule-based chatbot.
14. Data privacy and protection.

Mỗi ADR gồm:
- Context.
- Problem.
- Options.
- Selected decision hoặc Pending Decision.
- Consequences.
- Risks.
- Related requirements.

Không tự ý thay đổi các quyết định nghiệp vụ đã được chốt.

Các quyết định chưa được chốt phải giữ trạng thái PENDING.

Tạo:

docs/agent/phase_02/architecture_decisions.md

DỪNG SAU KHI HOÀN THÀNH.
```

## TASK 2.2 - Use Case & Screen Flow

**Agent:** System Analyst

**Dependency:** TASK 2.1 PASS.

**Prompt:**

```text
Bạn là System Analyst.

Thiết kế Use Case Diagram và Screen Flow Diagram dựa trên Requirement Catalog.

Actors:
- Customer.
- Bank Officer.

Đảm bảo bao phủ:

Customer:
- Login.
- View account information.
- View transaction history.
- Deposit.
- Withdraw.
- Transfer.
- Pay utilities.
- Purchase services/products.
- Navigate to bank branches.
- Use chatbot.
- Update personal information.

Bank Officer:
- Login.
- Create customer accounts.
- Support customer eKYC.
- Modify customer information upon request.
- Modify Saving interest rates.

Yêu cầu:

1. Phân biệt quyền truy cập Customer và Officer.
2. Thể hiện các luồng thành công và thất bại quan trọng.
3. Không tạo actor là dịch vụ thanh toán bên ngoài.
4. Thể hiện các module giả lập như thành phần nội bộ.
5. Thể hiện luồng xác thực giao dịch theo quyết định đã được phê duyệt.
6. Không dùng OTP làm mặc định nếu kiến trúc đã xác định cơ chế khác.

Tạo:

docs/agent/phase_02/use_case_diagram.md
docs/agent/phase_02/screen_flow.md

Sử dụng Mermaid.

DỪNG SAU KHI HOÀN THÀNH.
```

## TASK 2.3 - Solution Architecture

**Agent:** Software Architect

**Dependency:** TASK 2.2 PASS.

**Prompt:**

```text
Bạn là Software Architect.

Thiết kế Solution Diagram cho ứng dụng Android.

Biểu diễn rõ:

1. UI Layer.
2. ViewModel Layer.
3. Domain / Service Layer.
4. Repository Layer.
5. Data Access Layer.
6. Room / SQLite.
7. Local ML Components.
8. Device OS Integration.
9. Utility Simulators.
10. Navigation Engine.
11. Chatbot Engine.

Định nghĩa trách nhiệm của từng thành phần.

Các quy tắc:

- UI không truy cập DAO trực tiếp.
- ViewModel không chứa business logic phức tạp.
- Repository quản lý truy cập dữ liệu.
- TransactionService xử lý nghiệp vụ giao dịch.
- Authentication policy phải được tách khỏi UI.
- Face recognition không được phụ thuộc cloud API.
- Navigation không được sử dụng online map API.
- Payment simulators không được gọi payment gateway thật.

Phân biệt Android OS APIs với external business services.

Kiến trúc phải phù hợp với technology stack thực tế đã xác định ở Phase 0.

Tạo:

docs/agent/phase_02/solution_architecture.md

DỪNG SAU KHI HOÀN THÀNH.
```

## TASK 2.4 - Database Design & ERD

**Agent:** Database Architect

**Dependency:** TASK 2.3 PASS.

**Prompt:**

```text
Bạn là Database Architect.

Thiết kế ERD cho hệ thống Mobile Banking local-first.

Bắt đầu bằng cách kiểm tra ERD hiện tại.

Đánh giá các bảng:

- USERS
- ACCOUNTS
- TRANSACTIONS
- OTP_CODES
- BILLS
- EXTERNAL_BANKS
- BRANCHES
- CHAT_INTENTS

Đề xuất bổ sung hoặc điều chỉnh bảng theo yêu cầu nghiệp vụ đã được xác nhận.

Xem xét:

- Utility orders.
- Product catalog.
- Bookings.
- Saved beneficiaries.
- Device authentication metadata.
- Face templates.
- Audit logs.
- Transaction limits.
- Transaction authentication records.

Không tạo bảng chỉ vì chúng xuất hiện trong danh sách đề xuất. Phải chứng minh mối quan hệ với requirement.

Yêu cầu:

1. PK/FK rõ ràng.
2. Cardinality chính xác.
3. Unique constraints.
4. NOT NULL constraints.
5. Check constraints khi phù hợp.
6. Indexes.
7. Transaction consistency.
8. Data lifecycle.
9. Migration strategy.
10. Monetary precision.

Sử dụng integer Long để lưu số tiền VND.

Không lưu dữ liệu bí mật của Android Device Authentication.

Không lưu raw biometric credentials.

Với face template, phải phân tích bảo vệ dữ liệu và vòng đời lưu trữ.

Chưa tự ý thay đổi database code trong task này.

Tạo:

docs/agent/phase_02/erd.md
docs/agent/phase_02/data_dictionary.md
docs/agent/phase_02/database_constraints.md

DỪNG SAU KHI HOÀN THÀNH.
```

## TASK 2.5 - Class Diagram & Sequence Diagrams

**Agent:** Software Designer

**Dependency:** TASK 2.4 PASS.

**Prompt:**

```text
Bạn là Software Designer.

Xây dựng Class Diagram và Sequence Diagrams dựa trên Solution Architecture và ERD.

Class Diagram phải thể hiện:

- User / Customer / Officer.
- Account / CheckingAccount / SavingAccount / MortgageAccount.
- Transaction.
- Authentication components.
- Account management components.
- Repository.
- DAO.
- Utility simulator.
- Navigation components.
- Chatbot components.

Sequence Diagrams bắt buộc:

1. Login.
2. Customer account creation.
3. eKYC.
4. View account and transaction history.
5. Deposit.
6. Withdraw.
7. Internal transfer.
8. Interbank transfer simulation.
9. Ordinary transaction authentication.
10. High-value transaction authentication.
11. Utility payment.
12. Chatbot navigation.

Thể hiện:
- Normal flow.
- Validation failure.
- Authentication failure.
- Database failure.
- Rollback khi cần thiết.

Không tự đặt ra các quy tắc nghiệp vụ chưa được duyệt.

Các luồng authentication phải phản ánh các ADR đã được phê duyệt. Nếu chính sách 2FA còn pending, thể hiện rõ trạng thái chưa được quyết định.

Tạo:

docs/agent/phase_02/class_diagram.md
docs/agent/phase_02/sequence_diagrams.md

DỪNG SAU KHI HOÀN THÀNH.
```

## TASK 2.6 - Architecture Consistency Audit

**Agent:** Independent Architecture Reviewer

**Dependency:** TASK 2.1 đến 2.5 PASS.

**Prompt:**

```text
Bạn là Independent Architecture Reviewer.

Không chỉnh sửa thiết kế trong lượt kiểm tra đầu tiên.

Kiểm tra chéo:

1. Requirement Catalog vs Use Case.
2. Use Case vs Screen Flow.
3. Screen Flow vs Solution Architecture.
4. Solution Architecture vs Class Diagram.
5. Class Diagram vs ERD.
6. Sequence Diagram vs Business Rules.
7. Architecture Decision Records vs toàn bộ diagrams.

Tìm:
- Missing requirements.
- Missing entities.
- Unused entities.
- Missing relationships.
- Inconsistent names.
- Contradictory states.
- Security design gaps.
- External service dependencies.
- Transaction consistency problems.

Tạo:

docs/agent/phase_02/design_consistency_report.md

Nếu có lỗi:
- Ghi vị trí.
- Mức độ.
- Ảnh hưởng.
- Đề xuất sửa.

Chỉ PASS khi không còn lỗi CRITICAL hoặc HIGH chưa được xử lý.

DỪNG SAU KHI HOÀN THÀNH.
```

**GATE 2:** Kiến trúc, database và các diagram phải nhất quán. Chưa bắt đầu implementation khi Gate 2 chưa PASS.

---

# E. PHASE 3: IMPLEMENTATION

**Mục tiêu:** Triển khai từng nhóm nghiệp vụ đã được phê duyệt.

**Quyền chỉnh sửa source code:** Được phép trong phạm vi task.

## TASK 3.1 - Project Foundation

**Agent:** Android Foundation Developer

**Dependency:** GATE 2 PASS.

**Prompt:**

```text
Bạn là Android Foundation Developer.

Triển khai nền tảng kiến trúc đã được phê duyệt.

Kiểm tra source hiện tại trước khi sửa.

Triển khai hoặc hoàn thiện:

1. MVVM.
2. Package structure.
3. Dependency Injection.
4. Repository base structure.
5. Room database initialization.
6. DAO registration.
7. Navigation framework.
8. Coroutine / Flow handling.
9. Error handling convention.
10. Logging convention.

Không triển khai nghiệp vụ Banking trong task này.

Không tự ý nâng cấp Gradle hoặc Android dependencies nếu chưa có lý do kỹ thuật rõ ràng.

Phải bảo đảm project build được.

Tạo báo cáo:

docs/agent/phase_03/task_03_1_report.md

Chạy build và các test liên quan.

DỪNG SAU KHI HOÀN THÀNH.
```

## TASK 3.2 - User, Authentication & Profile

**Agent:** Identity Developer

**Dependency:** TASK 3.1 PASS.

**Prompt:**

```text
Bạn là Identity Developer.

Triển khai User Management và Authentication theo thiết kế đã duyệt.

Chức năng:

1. Login.
2. Logout.
3. Role management.
4. Customer profile.
5. Officer profile.
6. Customer profile update.
7. Officer creates customer.
8. Officer updates customer information upon request.
9. Session handling phù hợp với local application.

Yêu cầu:

- Phân biệt CUSTOMER và OFFICER.
- Kiểm soát quyền ở business layer, không chỉ ẩn UI.
- Password hashing bằng phương pháp phù hợp.
- Validation dữ liệu đầu vào.
- Không lưu plaintext password.
- Không cho phép truy cập dữ liệu trái quyền.
- Có error handling rõ ràng.

Không triển khai face recognition trong task này.

Kiểm thử các trường hợp:
- Login thành công.
- Password sai.
- Tài khoản không tồn tại.
- Sai role.
- Update profile không hợp lệ.
- Officer thực hiện thao tác không đủ quyền.

Chạy tests và build.

DỪNG SAU KHI HOÀN THÀNH.
```

## TASK 3.3 - eKYC & Local Face Recognition

**Agent:** Computer Vision Developer

**Dependency:** TASK 3.2 PASS.

**Prompt:**

```text
Bạn là Computer Vision Developer.

Triển khai eKYC và Face Recognition offline.

Technology direction:
- Android Camera.
- MediaPipe Face Detection / Face Landmarker.
- MobileFaceNet.
- Local model inference.

Nghiệp vụ:

1. Officer hỗ trợ customer đăng ký dữ liệu nhận diện.
2. Capture khuôn mặt.
3. Detect face.
4. Landmark / alignment nếu cần.
5. Extract face embedding.
6. Store protected face template.
7. Compare submitted face with registered template.
8. Return verification result.

Yêu cầu:

- Không upload ảnh hoặc face template lên cloud.
- Không gọi face recognition API bên ngoài.
- Không lưu Android biometric credentials.
- Bảo vệ ảnh và face embedding.
- Có cơ chế xóa dữ liệu khuôn mặt.
- Có xử lý khi không detect được face.
- Có xử lý nhiều khuôn mặt.
- Kiểm tra chất lượng ảnh đầu vào.

Đánh giá riêng liveness và anti-spoofing.

Không được tuyên bố chống giả mạo nếu chưa có implementation và kiểm thử tương ứng.

Không tự quyết định threshold giao dịch giá trị cao.

Tạo tests cho:
- No face.
- Multiple faces.
- Low-quality input.
- Face mismatch.
- Face match.
- Model unavailable.

Chạy tests và build.

DỪNG SAU KHI HOÀN THÀNH.
```

## TASK 3.4 - Account Management

**Agent:** Account Developer

**Dependency:** TASK 3.2 PASS và database được phê duyệt.

**Prompt:**

```text
Bạn là Account Management Developer.

Triển khai ba loại tài khoản:

1. Checking.
2. Saving.
3. Mortgage.

Chức năng:

- View account list.
- View checking balance.
- View saving balance.
- View saving interest rate.
- View monthly saving profit.
- View mortgage loan amount.
- View mortgage payment per cycle.
- View transaction history.
- Officer modifies saving interest rate.

Yêu cầu:

- Tách biệt cách hiển thị và tính toán cho từng loại tài khoản.
- Business logic nằm trong domain/service layer.
- Không tính lãi bằng công thức tự suy đoán.
- Sử dụng công thức đã được phê duyệt.
- Nếu công thức chưa có, tạo unresolved business rule và không tự đánh dấu chức năng hoàn thành.
- Có unit tests cho các phép tính.
- Có validation cho interest rate.

Không triển khai transfer trong task này.

Chạy build và tests.

DỪNG SAU KHI HOÀN THÀNH.
```

## TASK 3.5 - Transaction Core

**Agent:** Transaction Developer

**Dependency:** TASK 3.4 PASS.

**Prompt:**

```text
Bạn là Transaction Developer.

Triển khai Transaction Core.

Chức năng:

1. Deposit.
2. Withdraw.
3. Internal transfer.
4. Interbank transfer simulation.
5. Transaction history.
6. Transaction detail.
7. Transaction status management.

Yêu cầu:

- Validate account.
- Validate account ownership.
- Validate amount.
- Validate available balance.
- Validate destination.
- Prevent self-transfer khi không hợp lệ theo business rules.
- Ghi nhận transaction reference.
- Ghi nhận transaction timestamp.
- Ghi nhận trạng thái transaction.
- Sử dụng Long cho số tiền VND.
- Bảo đảm atomicity khi cập nhật dữ liệu.

Đối với internal transfer:

Cập nhật tài khoản nguồn, tài khoản đích và transaction records trong cùng một database transaction.

Đối với external bank simulation:

Sử dụng dữ liệu ngân hàng và người nhận giả lập được lưu cục bộ.

Không gọi API ngân hàng thật.

Kiểm thử:
- Successful transfer.
- Insufficient balance.
- Invalid destination.
- Duplicate confirmation.
- Database exception.
- Rollback.
- Repeated request.

Chạy tests và build.

DỪNG SAU KHI HOÀN THÀNH.
```

## TASK 3.6 - Transaction Authentication Policy

**Agent:** Security Developer

**Dependency:** TASK 3.3 và 3.5 PASS. Chính sách xác thực cần được phê duyệt trước khi đánh dấu hoàn thành.

**Prompt:**

```text
Bạn là Security Developer.

Triển khai Transaction Authentication theo ADR đã được phê duyệt.

Các cơ chế:

ORDINARY TRANSACTION:
Android Device Authentication thông qua Android authentication APIs.

HIGH-VALUE TRANSACTION:
In-app face recognition với MediaPipe và MobileFaceNet.

Yêu cầu:

1. Tách Authentication Policy khỏi UI.
2. Tách Authentication Result khỏi Transaction Result.
3. Không cho phép ghi nhận transaction thành công trước khi xác thực được chấp nhận.
4. Authentication thất bại phải giữ nguyên số dư.
5. Authentication timeout phải được xử lý.
6. Có trạng thái cancelled.
7. Có trạng thái failed.
8. Không cho xác nhận lại giao dịch đã hoàn tất.
9. Có liên kết giữa transaction và authentication attempt.

Đặc biệt:

Đề bài yêu cầu mọi giao dịch phải có 2FA.

Không tự tuyên bố Android Device Authentication đơn lẻ hoặc Face Recognition đơn lẻ đáp ứng 2FA.

Nếu ADR về mapping 2FA hoặc xác thực giao dịch giá trị cao còn PENDING:
- Không tự đưa ra quyết định thay chủ dự án.
- Hoàn thiện interface và policy abstraction nếu phù hợp.
- Báo cáo chính xác phần implementation bị BLOCKED.
- Không giả lập authentication thành công để vượt qua test.

Không lưu PIN hoặc raw biometric data.

Tạo tests cho mọi nhánh xác thực.

DỪNG SAU KHI HOÀN THÀNH.
```

## TASK 3.7 - Utility Simulators

**Agent:** Utility Developer

**Dependency:** TASK 3.5 PASS và authentication interface đã được tích hợp.

**Prompt:**

```text
Bạn là Utility Developer.

Triển khai các utility simulators offline.

Bao gồm:

1. Electricity bill.
2. Water bill.
3. Mobile top-up.
4. Flight ticket.
5. Movie ticket.
6. Hotel booking.
7. E-commerce product payment.

Mỗi module phải có:

- Local catalog hoặc test data.
- Business rules.
- Input validation.
- Order creation.
- Payment processing.
- Transaction reference.
- Order status.
- History.
- Error handling.

Không chỉ tạo giao diện demo.

Không gọi VNPay, Stripe hoặc nhà cung cấp dịch vụ thật.

Tất cả dữ liệu phải lưu trong Room/SQLite.

Đối với thanh toán:
- Kiểm tra số dư.
- Xác thực theo transaction policy.
- Cập nhật giao dịch và trạng thái đơn hàng nhất quán.
- Không trừ tiền hai lần.
- Có chính sách rollback.

Phân biệt booking và payment nếu hai nghiệp vụ có vòng đời khác nhau.

Không tự đặt phí, giá, quy tắc hủy hoặc hoàn tiền nếu chưa được phê duyệt.

Tạo tests cho từng module.

DỪNG SAU KHI HOÀN THÀNH.
```

## TASK 3.8 - Offline Navigation

**Agent:** Navigation Developer

**Dependency:** TASK 2.1 và 2.3 PASS.

**Prompt:**

```text
Bạn là Navigation Developer.

Triển khai Navigation System offline.

Yêu cầu:

1. Obtain user location bằng Android Location APIs.
2. Local branch database.
3. Display bank branches.
4. Calculate distance to nearby branches.
5. Find nearest branch.
6. Find shortest walking path.

Kiến trúc:

- Không sử dụng Google Maps API.
- Không sử dụng online routing API.
- Không phụ thuộc remote map tile.
- Bản đồ và road graph phải có dữ liệu local.
- Sử dụng Dijkstra hoặc A* phù hợp với data model.

Kiểm tra:

- Location permission denied.
- Location unavailable.
- No nearby branches.
- No valid route.
- Multiple candidate routes.
- Map dataset unavailable.

Phân biệt khoảng cách đường chim bay và khoảng cách đi bộ trên road graph.

Không tuyên bố shortest walking path nếu chỉ tính khoảng cách tọa độ.

Nếu chưa có dữ liệu bản đồ/road graph hợp lệ:
- Xác định dataset cần thiết.
- Đưa ra phương án cung cấp dữ liệu offline.
- Đánh dấu navigation là chưa hoàn thành.

Tạo tests cho thuật toán routing.

DỪNG SAU KHI HOÀN THÀNH.
```

## TASK 3.9 - Local Chatbot

**Agent:** Chatbot Developer

**Dependency:** TASK 3.2, 3.4 và 3.5 PASS.

**Prompt:**

```text
Bạn là Chatbot Developer.

Triển khai Rule-based / Intent-based Chatbot hoàn toàn offline.

Không gọi LLM API hoặc chatbot API bên ngoài.

Các intent tối thiểu:

- CHECK_BALANCE.
- VIEW_TRANSACTIONS.
- CHECK_SAVINGS.
- CHECK_MORTGAGE.
- TRANSFER_MONEY.
- PAY_ELECTRICITY.
- PAY_WATER.
- PHONE_TOPUP.
- VIEW_PROFILE.
- UNKNOWN_INTENT.

Mỗi intent phải có:

- Intent identifier.
- Utterance patterns.
- Required parameters.
- Response.
- Target screen.
- Navigation action.

Yêu cầu:

- Chatbot chỉ truy vấn dữ liệu thông qua các interface hợp lệ.
- Không tự thực hiện chuyển tiền chỉ dựa trên câu chat.
- Giao dịch luôn phải đi qua luồng xác nhận và xác thực.
- Không để chatbot bypass role permissions.
- Có fallback khi intent không xác định.

Kiểm thử:
- Recognized intent.
- Missing parameters.
- Ambiguous request.
- Unknown intent.
- Unauthorized request.
- Navigation target invalid.

DỪNG SAU KHI HOÀN THÀNH.
```

## TASK 3.10 - UI & Module Integration

**Agent:** Android Integration Developer

**Dependency:** Các module cần tích hợp đã PASS.

**Prompt:**

```text
Bạn là Android Integration Developer.

Tích hợp toàn bộ module đã hoàn thành vào ứng dụng.

Kiểm tra:

1. Authentication navigation.
2. Customer dashboard.
3. Officer dashboard.
4. Account screens.
5. Transaction screens.
6. Authentication screens.
7. Utility screens.
8. Navigation screens.
9. Chatbot screens.
10. Profile screens.

Yêu cầu:

- UI phản ánh trạng thái thật từ ViewModel.
- Không sử dụng hardcoded success response.
- Có loading state.
- Có empty state.
- Có error state.
- Có validation message.
- Navigation không mở sai role.
- Các thao tác không bị lặp ngoài ý muốn.
- Kiểm tra lifecycle và coroutine cancellation.

Không tự ý thay đổi business logic hoặc database schema nếu không có task tương ứng.

Chạy build, unit tests và kiểm tra tích hợp.

Tạo:

docs/agent/phase_03/integration_report.md

DỪNG SAU KHI HOÀN THÀNH.
```

**GATE 3:** Các module được tích hợp, build thành công và có test evidence.

---

# F. PHASE 4: TESTING & VERIFICATION

**Mục tiêu:** Kiểm chứng hệ thống thay vì tin vào báo cáo implementation.

## TASK 4.1 - Unit & Integration Test Audit

**Agent:** QA Engineer

**Dependency:** GATE 3 PASS.

**Prompt:**

```text
Bạn là QA Engineer độc lập.

Kiểm tra toàn bộ unit tests và integration tests.

Ưu tiên:

1. Authentication.
2. Authorization.
3. Account calculations.
4. Deposit.
5. Withdraw.
6. Internal transfer.
7. Interbank transfer.
8. Transaction authentication.
9. Utility payments.
10. Booking workflows.
11. Chatbot.
12. Navigation routing.

Kiểm tra các tình huống:
- Normal cases.
- Boundary cases.
- Invalid input.
- Failure cases.
- Duplicate requests.
- Database exceptions.
- Transaction rollback.

Không chỉ kiểm tra happy path.

Tự chạy tests.
Không dựa vào kết quả được ghi trong báo cáo của developer.

Tạo:

docs/agent/phase_04/test_execution_report.md
docs/agent/phase_04/coverage_gap_report.md

DỪNG SAU KHI HOÀN THÀNH.
```

## TASK 4.2 - Transaction Integrity Audit

**Agent:** Database & Transaction QA

**Dependency:** TASK 4.1 PASS.

**Prompt:**

```text
Bạn là Database & Transaction QA.

Kiểm tra tính toàn vẹn dữ liệu của các giao dịch.

Các tình huống bắt buộc:

1. Successful transfer.
2. Insufficient balance.
3. Source account invalid.
4. Destination account invalid.
5. Payment failure.
6. Authentication failure.
7. App interrupted during transaction.
8. Repeated transaction confirmation.
9. Database write failure.
10. Rollback after partial failure.

Đảm bảo:

- Không tạo tiền từ hư không.
- Không trừ tiền hai lần.
- Không cộng tiền hai lần.
- Tổng dữ liệu thay đổi phải nhất quán với transaction ledger.
- Trạng thái transaction không mâu thuẫn.
- Không ghi transaction thành công khi cập nhật tài khoản thất bại.

Kiểm tra migrations, foreign keys, unique indexes và constraints.

Tạo:

docs/agent/phase_04/transaction_integrity_report.md

Nếu phát hiện lỗi, tạo defect report và dừng việc phê duyệt.

DỪNG SAU KHI HOÀN THÀNH.
```

## TASK 4.3 - Security Review

**Agent:** Independent Security Reviewer

**Dependency:** TASK 4.1 và 4.2 PASS.

**Prompt:**

```text
Bạn là Independent Security Reviewer.

Audit ứng dụng theo các threat scenarios phù hợp với Mobile Banking.

Kiểm tra:

1. Authentication.
2. Role-based authorization.
3. Password storage.
4. Local database protection.
5. Sensitive data storage.
6. Face template protection.
7. Android Device Authentication integration.
8. Transaction authentication.
9. Transaction replay.
10. Unauthorized data modification.
11. Officer privileges.
12. Logging sensitive information.
13. App backup and data exposure.
14. Screenshot / UI exposure nếu cần thiết.

Đối với biometric:

- Không đọc hoặc trích xuất raw Android biometric credentials.
- Không tuyên bố liveness nếu không có implementation.
- Đánh giá giới hạn face matching.
- Kiểm tra bảo vệ face template.
- Kiểm tra quyền xóa và vòng đời dữ liệu.

Kiểm tra 2FA requirement:
- Xác định chính xác các yếu tố được sử dụng.
- Không đồng nhất một phương thức biometric với hai yếu tố.
- Liệt kê các quyết định còn pending.

Không sửa code trong lượt audit đầu tiên.

Tạo:

docs/agent/phase_04/security_review.md
docs/agent/phase_04/security_findings.md

Phân loại CRITICAL / HIGH / MEDIUM / LOW.

DỪNG SAU KHI HOÀN THÀNH.
```

## TASK 4.4 - Offline Runtime Verification

**Agent:** Offline QA Engineer

**Dependency:** TASK 3.10 PASS.

**Prompt:**

```text
Bạn là Offline QA Engineer.

Mục tiêu: Xác minh ứng dụng hoạt động mà không cần Internet hoặc dịch vụ nghiệp vụ bên ngoài.

Thực hiện kiểm tra ở runtime.

Kiểm tra:

1. Application startup.
2. Login.
3. Account query.
4. Transaction history.
5. Deposit.
6. Withdraw.
7. Transfer.
8. Utility simulation.
9. Face recognition local inference.
10. Device authentication.
11. Chatbot.
12. Offline navigation.

Kiểm tra dependency và network behavior.

Phân biệt:
- Android OS functionality.
- Local libraries.
- External business services.

Cần chú ý:

- Location APIs có thể cần tín hiệu GNSS hoặc quyền truy cập vị trí.
- Bản đồ phải có dữ liệu local.
- ML models phải được đóng gói hoặc có sẵn trong thiết bị.
- Không được tải dữ liệu bắt buộc từ cloud khi chạy.

Kiểm thử khi:
- Wi-Fi tắt.
- Mobile data tắt.
- Không có kết nối Internet.
- Ứng dụng khởi động lại.
- Database đã có dữ liệu.

Tạo:

docs/agent/phase_04/offline_verification.md

Chỉ PASS khi các chức năng được cam kết offline thực sự hoạt động và có bằng chứng.

DỪNG SAU KHI HOÀN THÀNH.
```

## TASK 4.5 - UI & Functional Acceptance

**Agent:** UI QA Engineer

**Dependency:** TASK 4.1 đến 4.4 PASS hoặc các hạn chế đã được chấp thuận.

**Prompt:**

```text
Bạn là UI QA Engineer.

Kiểm thử UI và luồng chức năng của ứng dụng.

Đối chiếu với:
- Requirement Catalog.
- Use Case Diagram.
- Screen Flow.
- Acceptance Criteria.

Kiểm tra:

1. Customer screens.
2. Officer screens.
3. Input validation.
4. Navigation.
5. Loading states.
6. Empty states.
7. Error messages.
8. Transaction confirmation.
9. Authentication prompts.
10. Screen rotation / lifecycle nếu được hỗ trợ.
11. Accessibility cơ bản.
12. UI consistency.

Không đánh dấu chức năng PASS chỉ dựa trên việc màn hình hiển thị.

Phải xác nhận hành vi thực tế tương ứng.

Tạo:

docs/agent/phase_04/ui_acceptance_report.md

DỪNG SAU KHI HOÀN THÀNH.
```

**GATE 4:** Không còn lỗi CRITICAL chưa xử lý. Các lỗi HIGH phải được khắc phục hoặc có chấp thuận rủi ro rõ ràng.

---

# G. PHASE 5: FINAL AUDIT & DELIVERY

**Mục tiêu:** Xác nhận sản phẩm đáp ứng đề bài và chuẩn bị bàn giao.

## TASK 5.1 - Independent Requirement Acceptance Audit

**Agent:** Final Requirements Auditor

**Dependency:** GATE 4 PASS.

**Prompt:**

```text
Bạn là Final Requirements Auditor.

Đối chiếu toàn bộ implementation với FinalProject_2026_2027(1).pdf.

Đọc lại requirement gốc.

Với từng requirement, xác minh:

1. Requirement.
2. Corresponding implementation.
3. Source file / component.
4. Related database entity.
5. Related test.
6. Test execution evidence.
7. Actual status.

Trạng thái chỉ được là:

PASS:
Có implementation và bằng chứng xác minh.

PARTIAL:
Đáp ứng một phần.

FAIL:
Không đáp ứng.

NOT VERIFIED:
Thiếu bằng chứng.

Không tin báo cáo của Implementation Agent nếu chưa kiểm chứng được.

Đặc biệt audit:
- All transaction 2FA requirement.
- eKYC.
- Customer and Officer permissions.
- Three account types.
- All utility categories.
- Nearby branch navigation.
- Shortest walking path.
- Chatbot links to activities.
- SQLite persistence.
- Android project.
- Project report.
- Demo video requirement.

Tạo:

docs/agent/phase_05/requirement_acceptance.md
docs/agent/phase_05/final_audit.md

DỪNG SAU KHI HOÀN THÀNH.
```

## TASK 5.2 - Release Build Verification

**Agent:** Release Engineer

**Dependency:** TASK 5.1 PASS hoặc CONDITIONAL PASS.

**Prompt:**

```text
Bạn là Android Release Engineer.

Chuẩn bị release artifact cho Android application.

Kiểm tra:

1. Release build configuration.
2. Application ID.
3. Version code.
4. Version name.
5. App permissions.
6. Manifest.
7. Release dependencies.
8. Signing configuration.
9. Debug logging.
10. Database initialization.
11. Offline startup.
12. App installation.
13. App launch.
14. Critical workflow smoke tests.

Không ghi signing keys hoặc secrets vào source code.

Không tự phát hành ứng dụng lên Google Play Store.

Chuẩn bị các thông tin và artifact cần thiết cho việc phát hành.

Tạo:

docs/agent/phase_05/release_checklist.md
docs/agent/phase_05/release_build_report.md

Ghi rõ:
- Build command.
- Artifact path.
- Artifact type.
- Version.
- Test results.
- Known limitations.

DỪNG SAU KHI HOÀN THÀNH.
```

## TASK 5.3 - Final Report & Demo Preparation

**Agent:** Documentation Engineer

**Dependency:** TASK 5.1 và 5.2 PASS.

**Prompt:**

```text
Bạn là Documentation Engineer.

Chuẩn bị nội dung bàn giao theo yêu cầu Final Project.

Phần 1: Analysis and Design

- Software development principles.
- Design patterns.
- Use Case Diagrams.
- ERD.
- Class Diagrams.
- Solution Diagram.
- Infrastructure / architecture description.

Phần 2: Implementation

- Project structure.
- Technologies.
- Architecture.
- Database.
- Main components.
- Business logic.
- Offline simulators.
- Security implementation.

Phần 3: User Interface

- Customer functionality.
- Officer functionality.
- Các màn hình.
- Các luồng sử dụng.

Phần 4: Demo Video

Tạo kịch bản demo bao gồm:

1. Login as Customer.
2. View checking account.
3. View saving account.
4. View mortgage account.
5. Transfer.
6. Deposit / Withdraw.
7. Transaction authentication.
8. Utility payment.
9. Interbank transfer simulation.
10. Navigation.
11. Chatbot.
12. Login as Officer.
13. Create customer.
14. eKYC.
15. Modify customer data.
16. Modify saving interest rate.

Đối chiếu nội dung báo cáo với implementation thực tế.

Không mô tả chức năng chưa hoàn thành như thể đã hoàn thành.

Không tự tạo ảnh chụp màn hình hoặc kết quả demo giả.

Tạo:

docs/agent/phase_05/final_project_report.md
docs/agent/phase_05/demo_script.md
docs/agent/phase_05/submission_checklist.md

DỪNG SAU KHI HOÀN THÀNH.
```

**GATE 5:** Final Reviewer xác nhận trạng thái dự án, các hạn chế còn tồn tại và danh sách artifact bàn giao.

---

# H. EXECUTION ORDER

Thứ tự thực thi chính thức:

| Phase | Task | Nội dung |
|---|---|---|
| 0 | 0.1 → 0.3 | Khảo sát repository |
| 1 | 1.1 → 1.3 | Requirements |
| 2 | 2.1 → 2.6 | Architecture & Design |
| 3 | 3.1 → 3.10 | Implementation |
| 4 | 4.1 → 4.5 | QA & Security |
| 5 | 5.1 → 5.3 | Audit & Delivery |

Tổng cộng: **30 task**.

## Quy tắc điều phối

- Có thể dùng cùng một coding agent cho nhiều task.
- Các task implementation thuộc module khác nhau có thể chạy song song khi dependency đã PASS và ownership của file không xung đột.
- Các task database và architecture không được chỉnh sửa đồng thời khi chưa thống nhất schema.
- Security Reviewer và Final Reviewer phải độc lập với Implementation Agent.
- Không tự động chuyển task nếu gate thất bại.

## Definition of Done

Một task chỉ được xem là hoàn thành khi:

- [ ] Đúng phạm vi.
- [ ] Đúng requirement.
- [ ] Có đầy đủ file đầu ra.
- [ ] Có test phù hợp.
- [ ] Test đã chạy thực tế.
- [ ] Không có lỗi nghiêm trọng chưa xử lý.
- [ ] Không tạo thêm dependency trái kiến trúc.
- [ ] Báo cáo rõ limitations.
- [ ] Gate được đánh giá.

---

# I. MASTER PROJECT STATUS

Duy trì một file:

`docs/agent/project_status.md`

Nội dung mẫu:

| Task | Status | Evidence | Blocker |
|---|---|---|---|
| 0.1 | NOT STARTED | | |
| 0.2 | NOT STARTED | | |
| 0.3 | NOT STARTED | | |
| 1.1 | BLOCKED | | Gate 0 |
| 1.2 | BLOCKED | | Task 1.1 |
| ... | ... | ... | ... |

Trạng thái cho phép:

- NOT STARTED
- IN PROGRESS
- BLOCKED
- PASS
- CONDITIONAL PASS
- FAIL

Orchestrator là agent duy nhất được phép cập nhật trạng thái tổng thể.

Không sử dụng phần trăm tiến độ dựa trên số file đã tạo. Tiến độ phải dựa trên task đã được nghiệm thu.

## Quy tắc cuối cùng

**Không xây dựng một ứng dụng có vẻ hoàn chỉnh. Hãy xây dựng một ứng dụng mà từng requirement đều có implementation, dữ liệu và bằng chứng kiểm thử tương ứng.**
