# Final Project Handover Document (Phase 5 — Task 5.8)

**Project:** `m.ebanking`  
**Delivery Date:** 2026-11-23  
**Audience:** Instructors, Evaluators, and Core Engineering Handover  
**Repository State:** Branch `master` / Commit `c11e9fd`

---

## 1. Project Architecture & Technical Stack

```
com.mebanking.app
 ├── data/
 │    ├── local/
 │    │    ├── dao/                # Room SQLite DAOs (User, Account, Transaction, Lifestyle, etc.)
 │    │    ├── entity/             # Room Entities (Core banking & Lifestyle entities)
 │    │    ├── security/           # PasswordHasher (PBKDF2), NationalIdProtector (AES-256 GCM)
 │    │    └── seed/               # DatabaseSeeder, LifestyleDatabaseSeeder
 │    └── repository/              # Repository implementations adhering to Repository Pattern
 ├── di/                           # AppContainer, DefaultAppContainer, ViewModelFactory
 ├── domain/
 │    ├── model/                   # Pure domain models (User, Account, Transaction, Enums)
 │    ├── navigation/              # Offline Dijkstra & A* pathfinding
 │    ├── service/                 # AccountService, TransactionService, FaceRecognitionService,
 │    │                            # OtpService, OfficerCustomerService, FaceTemplateCipher
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

## 2. Database Schema Versions & Migrations

- **Schema v1:** Core entities (`users`, `accounts`, `transactions`, `otp_codes`, `bills`, `external_banks`, `branches`, `chat_intents`).
- **Schema v2 (`MIGRATION_1_2`):** Added nullable `idempotency_key TEXT` and unique index `index_transactions_idempotency_key` on `transactions` table.
- **Schema v3 (`MIGRATION_2_3`):**
  - Added `national_id_cipher TEXT` and `national_id_hash TEXT` with unique index `index_users_national_id_hash` on `users`.
  - Added 9 lifestyle simulator tables (`flights`, `flight_bookings`, `hotels`, `hotel_bookings`, `movies`, `cinema_showtimes`, `cinema_bookings`, `products`, `orders`).
- All migrations verified using `MigrationTestHelper` in [AppDatabaseMigrationTest.kt](file:///home/chocolat/Documents/Project/m.ebanking/app/src/test/java/com/mebanking/app/data/local/migration/AppDatabaseMigrationTest.kt).

---

## 3. Security Implementation Highlights

1. **Transaction Security (QĐ 2345/QĐ-NHNN):**
   - Transactions `<= 10.000.000 VND`: System Authentication (Biometrics / Screen Lock) + In-App OTP 6 digits (30s rotation).
   - Transactions `> 10.000.000 VND`: System Authentication + Face Biometric Matching against enrolled template -> 60s single-use `AuthorizationGrant`.
2. **Biometric Face Template Protection:**
   - Saved with **AES-256 GCM** in Android Keystore ([FaceTemplateCipher.kt](file:///home/chocolat/Documents/Project/m.ebanking/app/src/main/java/com/mebanking/app/domain/service/FaceTemplateCipher.kt)).
   - Memory zeroized immediately after reading or writing.
3. **National ID (CCCD) Protection:**
   - AES-256 GCM encrypted storage + HMAC-SHA256 index for lookup ([NationalIdProtector.kt](file:///home/chocolat/Documents/Project/m.ebanking/app/src/main/java/com/mebanking/app/data/local/security/NationalIdProtector.kt)).
4. **Financial Formula Boundaries (Requirement R4):**
   - Unapproved compounding interest and mortgage repayment formulas strictly return `DomainError.UnapprovedFormula` (fail-closed).
   - No floating-point rounding errors; all money math is whole 64-bit `Long` VND integers.

---

## 4. Build, Test & Deployment Instructions

### A. Environment Requirements
- **JDK:** Java 17+ (e.g. OpenJDK 17 or 21)
- **Android SDK:** Compile SDK 37, Min SDK 24
- **Gradle:** Wrapper included (Gradle 9.6.0)

### B. Common Commands
- **Run All Unit & Integration Tests (495 tests):**
  ```bash
  ./gradlew :app:testDebugUnitTest
  ```
- **Compile Android Runtime Instrumented Test Suite:**
  ```bash
  ./gradlew :app:compileDebugAndroidTestSources
  ```
- **Build Debug APK:**
  ```bash
  ./gradlew :app:assembleDebug
  # Output: app/build/outputs/apk/debug/app-debug.apk
  ```
- **Build Release APK:**
  ```bash
  ./gradlew :app:assembleRelease
  # Output: app/build/outputs/apk/release/app-release-unsigned.apk
  ```

---

## 5. Known Limitations & Technical Disclosures

1. **Face Embedding Extractor:**
   - As documented in `FaceRecognitionService.kt`, feature extraction uses a deterministic byte-hash vector for automated test repeatability without embedding multi-megabyte native binaries. Upgrading to production requires loading on-device MobileFaceNet `.tflite`.
2. **Camera Frame Feed:**
   - eKYC uses local byte array probe frames; full production integration requires binding `CameraX ImageAnalysis.Analyzer` directly to the camera hardware.
3. **Liveness Detection:**
   - Anti-spoofing (blink detection, texture analysis) is not currently implemented and must be added prior to actual production deployment.
