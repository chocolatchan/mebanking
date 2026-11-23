# ME Banking — Official Demo Script & Walkthrough

**Project:** `m.ebanking`  
**Target:** Final Project Demonstration & Examination  
**Architecture:** Pure Offline Android, Jetpack Compose, MVVM, Room SQLite, Android Keystore  
**Default Credentials:**
- Customer: Username `customer1` / Password `Password@123`
- Bank Officer: Username `officer1` / Password `Officer@123`

---

## 1. Demo Scenario Overview

This script guides the evaluator step-by-step through all major workflows of the application, validating core banking, high-value transaction security, officer customer management, eKYC, lifestyle simulators, offline navigation, and offline AI chatbot.

---

## 2. Step-by-Step Walkthrough

### Act I: Customer Authentication & Account Inspection
1. **Launch App:**
   - Launch `app-debug.apk` or run via Android Studio.
   - Screen displays `LoginScreen` with modern Material 3 styling.
2. **Login as Customer:**
   - Enter `customer1` / `Password@123`. Click "Đăng nhập".
   - App transitions smoothly into `HomeScreen`.
3. **Inspect Accounts:**
   - Review checking account balance (default 50,000,000 VND).
   - Click on the account card to open `AccountDetailScreen`.
   - Review account type, account number, balance, status.
   - Verify unapproved formula notice: *"Công thức tính lãi suất định kỳ chưa được phê duyệt (OPEN/BLOCKED)"* (R4 compliance).

---

### Act II: Core Banking Cash Operations & History
4. **Deposit Workflow:**
   - On `HomeScreen`, tap shortcut `"Nạp tiền"`.
   - Select checking account, enter `5.000.000` VND.
   - Review confirmation dialog -> Click "Xác nhận nạp tiền".
   - Success dialog shows new balance (55,000,000 VND) and transaction ID.
5. **Withdrawal Workflow:**
   - Tap shortcut `"Rút tiền"`.
   - Select account, enter `2.000.000` VND.
   - Confirm -> Account updates immediately to 53,000,000 VND.
6. **Transaction History & Filter:**
   - Tap `"Lịch sử GD"`.
   - Filter chips: Select "Nạp tiền" -> shows deposit transaction; Select "Rút tiền" -> shows withdrawal.
   - Click any transaction item to view immutable details (timestamp, IDs, status).

---

### Act III: Two-Factor Transaction Security & eKYC (QĐ 2345/QĐ-NHNN)
7. **Ordinary Transfer (<= 10.000.000 VND):**
   - Tap `"Chuyển khoản"`.
   - Enter destination account `200123456789`, amount `5.000.000` VND.
   - Tap "Chuyển tiền" -> System Authentication triggered (Device Biometrics / Screen Lock).
   - Enter In-App OTP (displayed on active challenge card, 30s rotation).
   - Transfer succeeds; balance debited by 5,000,000 VND.
8. **High-Value Transfer (> 10.000.000 VND):**
   - Enter destination account `200123456789`, amount `25.000.000` VND.
   - Tap "Chuyển tiền" -> System Auth -> Displays `FaceVerificationDialog`.
   - Camera frame evaluated: If face matches enrolled biometric template, single-use `AuthorizationGrant` issued and transaction completes immediately.
   - If not enrolled, informs user to visit a branch for eKYC.

---

### Act IV: Bank Officer Platform & Customer eKYC
9. **Logout Customer & Login as Officer:**
   - Return to Home, tap "Đăng xuất".
   - Enter `officer1` / `Officer@123`.
   - App detects `Role.OFFICER` and automatically presents `OfficerDashboardScreen`.
10. **Customer Search & Profile Review:**
    - Enter query (e.g. `customer1` or 12-digit CCCD) in search bar.
    - Select customer from results to review full name, phone, email, and encrypted CCCD.
11. **Collect eKYC Biometrics:**
    - With customer selected, tap `"👤 Thu thập sinh trắc học khuôn mặt eKYC"`.
    - Face template extracted and encrypted with AES-256 GCM in Android Keystore.
    - System updates `kycVerified = true` in SQLite Room.
12. **Create New Customer Account:**
    - Tap `"Tạo hồ sơ mới"`.
    - Enter username, temporary password, full name, email, phone, 12-digit CCCD.
    - Click "Lưu hồ sơ" -> Creates customer in `users` and default checking account in `accounts`.

---

### Act V: Lifestyle Extended Simulators
13. **Flight Booking:**
    - On Home, tap `"Vé máy bay"`. Search flights (HAN -> SGN) -> Select seat `12A` -> Confirm booking with booking reference code.
14. **Hotel Reservation:**
    - Tap `"Khách sạn"`. Filter city "Hà Nội" -> Choose Deluxe Room -> Pick dates -> Reserve.
15. **Cinema Booking:**
    - Tap `"Vé xem phim"`. Pick movie -> Choose showtime -> Select seats on interactive seat matrix -> Receive ticket code.
16. **Shopping Cart & Checkout:**
    - Tap `"Mua sắm"`. Add tech products to cart -> Adjust quantities -> Enter delivery address -> Checkout.
17. **Mobile Top-up:**
    - Tap `"Nạp ĐT"`. Enter phone number `0901234567` -> Select denomination `100.000đ` -> Instant top-up.

---

### Act VI: Offline Navigation & AI Banking Chatbot
18. **Branch Locator & Offline Routing:**
    - Tap `"Tìm chi nhánh"`.
    - App renders local branch list and interactive canvas map using `hanoi_roads.json`.
    - Tap a branch -> Computes shortest driving path via on-device Dijkstra / A* algorithm.
19. **Offline AI Banking Chatbot:**
    - Tap `"Trợ lý AI"`.
    - Type query: *"Cách chuyển khoản"* or *"Lãi suất tiết kiệm"*.
    - Chatbot matches intent locally via keyword/regex catalog and provides instant advice without internet.
