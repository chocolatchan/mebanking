# M.Ebanking — Mobile Banking Application

A production-grade, offline-first Android Mobile Banking application built with modern **Kotlin**, **Jetpack Compose (Material 3)**, **Room / SQLite**, and hardware-backed **Android Keystore** security.

---

## 🌟 Key Features

### 1. Core Banking & Cash Operations
- **Account Management:** Checking, Saving, and Mortgage accounts with balance aggregation and transaction history.
- **Deposit & Withdrawal Workflows:** Dedicated Compose screens with amount validation, confirmation modals, and atomic ledger consistency.
- **Internal & Interbank Transfers:** Instant transfers between customer accounts and simulated interbank settlements.
- **Durable SQLite Idempotency:** Eliminates duplicate charges across network retries and app restarts via unique database constraints (`MIGRATION_1_2`).
- **Regulatory Formula Boundaries (Requirement R4):** Strict adherence to bank compliance; unapproved compounding interest and mortgage repayment formulas fail-closed (`DomainError.UnapprovedFormula`). All currency calculations use exact 64-bit `Long` whole-VND units.

### 2. Transaction Security & 2FA (QĐ 2345/QĐ-NHNN)
- **Mandatory System Authentication:** AndroidX `BiometricPrompt` supporting modern biometric credentials with device credential (PIN/pattern/password) fallback.
- **Ordinary Transactions (≤ 10.000.000 VND):** Active In-App OTP challenge (CSPRNG 6-digit code, 30-second rotation, 60-second expiration, constant-time `MessageDigest.isEqual` comparison).
- **High-Value Transactions (> 10.000.000 VND):** Biometric Face Recognition verification against enrolled templates. Valid matches issue a single-use 60-second `AuthorizationGrant` consumed immediately upon commit.

### 3. Bank Officer Platform & Customer eKYC
- **Role-Based Access Control (RBAC):** Strict isolation between Customer and Officer roles.
- **Officer Dashboard:** Search customers by name or 12-digit National ID (CCCD), review profiles, and create new customer accounts.
- **eKYC Face Enrollment:** In-person facial biometric enrollment with templates encrypted via AES-256 GCM in the Android Keystore (`FaceTemplateCipher`).
- **National ID Protection:** Encrypted at rest via AES-256 GCM (`national_id_cipher`) with keyed HMAC-SHA256 index (`national_id_hash`) for fast lookups without plaintext leakage (`MIGRATION_2_3`).

### 4. Extended Lifestyle Simulators
- **Flight Booking:** Flight schedules, seat selection, and booking reference generation.
- **Hotel Reservation:** City filters, check-in/out date selection, and room reservation.
- **Cinema Booking:** Interactive 2D cinema seat matrix (Regular / VIP / Reserved) and ticket issuance.
- **E-Commerce Shopping:** Category browsing, quantity management, and cart checkout.
- **Mobile Telco Top-up:** Multi-carrier denomination selection (10k - 500k VND) and instant account debit.

### 5. Pure Offline-First Architecture
- **Offline Map & Branch Locator:** Nearest bank branch discovery and shortest-path driving route calculation using on-device Dijkstra / A* algorithms over local GeoJSON road graphs.
- **Offline AI Assistant:** Rule-based natural language banking FAQ and intent classifier operating with zero internet connectivity.

---

## 🏗️ Technical Architecture

```
com.mebanking.app
 ├── data/
 │    ├── local/
 │    │    ├── dao/                # Room SQLite DAOs (User, Account, Transaction, Lifestyle, etc.)
 │    │    ├── entity/             # Room SQLite Entities (Core banking & Lifestyle)
 │    │    ├── security/           # PasswordHasher (PBKDF2), NationalIdProtector (AES-256 GCM)
 │    │    └── seed/               # Initial database seeders (Accounts, Branches, Products)
 │    └── repository/              # Repository implementations adhering to Repository Pattern
 ├── di/                           # AppContainer, DefaultAppContainer, ViewModelFactory
 ├── domain/
 │    ├── model/                   # Domain entities, authentication decisions, domain errors
 │    ├── navigation/              # Offline Dijkstra and A* pathfinding algorithms
 │    ├── service/                 # TransactionService, OtpService, OfficerService, FaceRecognitionService
 │    └── util/                    # FinancialFormulas (R4 fail-closed math), CurrencyFormatter
 └── ui/                           # Jetpack Compose UI (Material 3)
      ├── account/                 # Account details & blocked formula notices
      ├── auth/                    # LoginScreen, DeviceAuthenticator (BiometricPrompt)
      ├── cash/                    # DepositScreen, WithdrawScreen & ViewModels
      ├── history/                 # TransactionHistoryScreen, TransactionDetailScreen
      ├── home/                    # HomeScreen & dashboard navigation
      ├── lifestyle/               # Flight, Hotel, Cinema, Shop, Top-up screens
      ├── navigation/              # BranchLocatorScreen (Offline Map Routing)
      ├── officer/                 # OfficerDashboardScreen & eKYC enrollment
      └── transfer/                # TransferScreen & FaceVerificationDialog
```

---

## 🛠️ Technology Stack

| Layer | Technologies |
|---|---|
| **Language & Platform** | Kotlin 2.4.21, Android SDK (Min 24, Target 37, Compile 37) |
| **UI Framework** | Jetpack Compose, Material 3, Navigation Compose, Lifecycle Compose |
| **Persistence** | AndroidX Room 2.8.5 with SQLite triggers and migrations |
| **Security & Cryptography** | AndroidX Biometric 1.1.0, Android Keystore, AES-256 GCM, HMAC-SHA256, PBKDF2 |
| **Testing** | JUnit 4, Robolectric 4.14.1, Kotlinx Coroutines Test, Turbine, Room Testing |
| **Build & Tooling** | Gradle 9.6.0, Android Gradle Plugin 9.3.0, KSP 2.3.14 |

---

## 🚀 Getting Started

### 1. Default Demo Credentials
- **Customer User:** Username `customer1` / Password `Password@123`
- **Bank Officer:** Username `officer1` / Password `Officer@123`

### 2. Build & Packaging Commands
```bash
# Build Debug APK
./gradlew :app:assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Build Release APK
./gradlew :app:assembleRelease
# Output: app/build/outputs/apk/release/app-release-unsigned.apk
```

### 3. Running Automated Tests
```bash
# Execute entire unit & integration test suite (495 tests)
./gradlew :app:testDebugUnitTest

# Compile Android runtime instrumented tests
./gradlew :app:compileDebugAndroidTestSources
```

---

## 📑 Quality Assurance & Documentation

Detailed Phase 5 audit reports are available in `docs/agent/phase_05/`:
- [Feature Traceability Matrix](file:///home/chocolat/Documents/Project/m.ebanking/docs/agent/phase_05/feature_traceability_matrix.md)
- [Test Execution Report (495/495 Tests Passing)](file:///home/chocolat/Documents/Project/m.ebanking/docs/agent/phase_05/test_execution_report.md)
- [Device & UI Testing Report](file:///home/chocolat/Documents/Project/m.ebanking/docs/agent/phase_05/device_test_report.md)
- [Security Audit Report](file:///home/chocolat/Documents/Project/m.ebanking/docs/agent/phase_05/security_audit.md)
- [Financial Integrity & Recovery Audit](file:///home/chocolat/Documents/Project/m.ebanking/docs/agent/phase_05/financial_integrity_report.md)
- [Offline Runtime Audit](file:///home/chocolat/Documents/Project/m.ebanking/docs/agent/phase_05/offline_runtime_audit.md)
- [Build & Release Verification Report](file:///home/chocolat/Documents/Project/m.ebanking/docs/agent/phase_05/build_release_report.md)
- [Official Demo Script & Walkthrough](file:///home/chocolat/Documents/Project/m.ebanking/docs/agent/phase_05/demo_script.md)
- [Final Handover Document](file:///home/chocolat/Documents/Project/m.ebanking/docs/agent/phase_05/handover_document.md)
- [Final Audit & Gate 5 Verdict](file:///home/chocolat/Documents/Project/m.ebanking/docs/agent/phase_05/final_audit_report.md)
