# Roadmap A: Core Banking, Security & Officer Platform — Execution Plan

**Project:** `m.ebanking`  
**Branch:** `feature/roadmap-a`  
**Scope:** Core Banking Hardening, Durable Idempotency, Transaction Security (Biometrics + In-app OTP), Officer Platform, Face Recognition & eKYC Flow, Financial Formulas Alignment.  
**Baseline Test Status:** 448 unit/integration tests passing (100% success rate).

---

## 1. Executive Summary & Architecture Separation

Roadmap A focuses on enterprise-grade core banking reliability, hardware security, regulatory compliance, and administrative capabilities:
1. **Durable Idempotency (Room Schema v2):** Guarantees at the database level that no transaction can be double-debited, even across process termination or app restart.
2. **Deposit & Withdraw Workflows:** Native Jetpack Compose screens supporting account selection, balance checks, whole-VND amounts, and atomic ledger consistency.
3. **Transaction History & Detail:** Multi-criteria filtering (All, Deposit, Withdrawal, Transfer, Bill Payment) with immutable detail view.
4. **Two-Factor Transaction Authentication Policy (QĐ 2345/QĐ-NHNN):**
   - **Step 1:** Mandatory System Authentication (`AndroidX BiometricPrompt` with biometric strong or device screen lock fallback).
   - **Step 2 (Ordinary Transactions <= 10.000.000 VND):** Active In-App OTP challenge (CSPRNG 6 digits, 30s rotation, 60s timeout, constant-time verification).
   - **Step 2 (High-Value Transactions > 10.000.000 VND):** Biometric Face Recognition verification against enrolled template.
5. **Officer Platform & Customer eKYC (Room Schema v3):**
   - Officer Dashboard: Customer search by CCCD/name, customer profile creation and modification.
   - National ID Protection: AES-256 GCM encrypted storage (`national_id_cipher`) + SHA-256 HMAC/hash lookup index (`national_id_hash`).
   - eKYC enrollment and face verification flow.
6. **Financial Invariants:**
   - Strict VND whole-unit `Long` currency arithmetic.
   - Fail-closed handling for unapproved/blocked financial formulas.

---

## 2. Progress & Task Breakdown

### Completed Tasks (Verified & Committed)
- [x] **Task A.1 — Durable Idempotency in Room Database (DEF-4.2-01 Fix)**
  - Schema migration v1 -> v2 adding `idempotency_key TEXT` with unique index `index_transactions_idempotency_key` on `transactions`.
  - `DefaultTransactionRepository` deduplicates transactions at SQLite level within `withTransaction`.
  - `DefaultTransactionService` removed in-memory store and delegates deduplication to database.
  - Tests: `AppDatabaseMigrationTest.testMigration1To2()`, `TransactionRepositoryTest.testIdempotencyAcrossInstances()`.
- [x] **Task A.2 — Core Banking Cash Transactions & History UI**
  - `CashTransactionScreen.kt` & `CashTransactionViewModel.kt` (`DepositViewModel`, `WithdrawViewModel`).
  - `TransactionHistoryScreen.kt` & `TransactionDetailScreen.kt` with filter chips.
  - Navigation integrated into `MEbankingApp.kt` and `HomeScreen.kt`.
  - Tests: `CashTransactionViewModelTest`, `TransactionHistoryViewModelTest`.
- [x] **Task A.3 — System Authentication, In-App OTP & Officer Platform (Room v3)**
  - `DeviceAuthenticator.kt`: AndroidX BiometricPrompt with modern Android 11+ strong biometrics and device credential fallback.
  - `OtpService.kt`: CSPRNG 6-digit OTP, 30s rotation, 60s timeout, `MessageDigest.isEqual` comparison.
  - `OfficerDashboardScreen.kt`, `OfficerDashboardViewModel.kt`, `OfficerCustomerService.kt`.
  - `NationalIdProtector.kt`: AES-256 GCM encryption + SHA-256 hash lookup.
  - Room migration v2 -> v3 adding `national_id_cipher` and `national_id_hash` with unique index.
  - Tests: `OtpServiceTest`, `OfficerCustomerServiceTest`, `AppDatabaseMigrationTest.testMigration2To3()`.

---

### Remaining Tasks for Roadmap A Completion

- [x] **Task A.4 — High-Value Transaction Face Authentication Integration**
  - Updated `TransactionAuthenticationPolicy` with single-use cryptographic authorization grant mechanism (`AuthorizationGrant`) valid for 60 seconds.
  - Implemented `evaluateWithBiometrics(request, session, imageBytes)` evaluating probe frame similarity against enrolled face template (`FaceRecognitionService.verifyFace`) and granting transfer authorization for `(idempotencyKey, userId, amount)`.
  - Updated `TransferViewModel` and `TransferScreen` to trigger `FaceVerificationDialog` for high-value transactions (> 10M VND) when eKYC enrolled, or inform customer to complete eKYC enrollment when not enrolled.
  - Verified with `TransactionAuthenticationPolicyTest` and `Phase4ViewModelIntegrationTest`.

- [x] **Task A.5 — Customer eKYC Camera & Face Biometric Flow**
  - Added biometric face enrollment capability in `OfficerDashboardViewModel.enrollFaceBiometrics(customerId, imageBytes)` and `OfficerDashboardScreen`.
  - Officer can collect face biometrics and complete eKYC verification directly from officer customer management portal.
  - Preprocessing pipeline: Encrypted template generation via `FaceRecognitionService`, stored with AES-256 GCM in local app storage.
  - Verified with `OfficerDashboardViewModelTest`.

- [x] **Task A.6 — Financial Formulas & Policy Boundary Audit**
  - Audited `FinancialFormulas.kt` and `AccountService.kt`: unapproved saving compounding and mortgage amortization formulas fail-closed (`DomainError.UnapprovedFormula`).
  - Handled cleanly in `AccountDetailViewModel.kt` without inventing local arithmetic.
  - Invariant strictly preserved: 64-bit whole unit `Long` VND currency without floating-point fraction leakage.
  - Verified with `FinancialFormulasTest` and `Tier2FinancialFormulaBoundaryE2ETest`.

- [x] **Task A.7 — Setup `app/src/androidTest` for Android Runtime Verification**
  - Configured `app/build.gradle.kts` with `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"` and `androidTestImplementation` dependencies (JUnit4, AndroidX Test Core/Ext, Room testing, Coroutines test).
  - Implemented `AppDatabaseAndroidTest` in `app/src/androidTest/java/com/mebanking/app/data/local/` validating Room schema persistence, user/account entities, and transaction unique idempotency keys on Android runtime.
  - Verified clean compilation with `./gradlew :app:compileDebugAndroidTestSources`.

- [x] **Task A.8 — Final Roadmap A Test Suite & Verification**
  - Executed full unit and integration test suite via `./gradlew :app:testDebugUnitTest`.
  - All 450+ tests passing (100% pass rate).
  - All Track A tasks completed and self-contained on `feature/roadmap-a`.
