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

- [ ] **Task A.4 — High-Value Transaction Face Authentication Integration**
  - Update `TransferViewModel` and `TransferScreen` to support high-value (> 10M VND) transactions:
    - Instead of blocking high-value transactions with an error, trigger the biometric face verification challenge.
    - Evaluate `TransactionAuthenticationPolicy.evaluateWithBiometrics` with enrolled template and probe image.
    - Handle decisions: `Approved` -> execute transfer; `Rejected`/`Unavailable`/`Cancelled` -> reject transaction.
- [ ] **Task A.5 — Customer eKYC Camera & Face Biometric Flow**
  - Create `FaceCaptureDialog.kt` / `FaceCaptureScreen.kt` with Compose camera UI preview.
  - Standardize on-device face image preprocessing contract (crop face -> resize 112x112 -> normalize -> feature vector extraction).
  - Connect eKYC enrollment from Officer Dashboard ("Đăng ký sinh trắc học eKYC") and Customer profile.
- [ ] **Task A.6 — Financial Formulas & Policy Boundary Audit**
  - Verify `FinancialFormulas.kt` fail-closed behavior for unapproved formulas (saving compounding, mortgage amortizations).
  - Verify all amounts strictly use whole `Long` VND integers.
- [ ] **Task A.7 — Setup `app/src/androidTest` for Android Runtime Verification**
  - Configure `app/build.gradle.kts` with `androidTestImplementation` dependencies.
  - Create initial instrumented smoke tests for Compose UI and Room database on Android runtime.
- [ ] **Task A.8 — Final Roadmap A Test Suite & Verification**
  - Run full test suite `./gradlew test`.
  - Ensure 100% pass rate and generate verification report.
