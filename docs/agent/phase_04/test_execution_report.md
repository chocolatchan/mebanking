# Task 4.1 — Unit & Integration Test Audit Report

**Date:** 2026-11-22  
**Role:** Independent QA Engineer  
**Status:** **CONDITIONAL PASS** (Execution verified on JVM/Robolectric; critical architecture & test scope limitations cataloged)  
**Execution Environment:** Linux, OpenJDK 21, Gradle 9.0  

---

## 1. Executive Summary & Verification Findings

This report documents the independent verification of the test suite for `m.ebanking` across unit, integration, boundary, and end-to-end tiers.
The test suite was executed locally using `./gradlew testDebugUnitTest --rerun-tasks` and `./gradlew testDebugUnitTest`.

- **Total Test Classes Executed:** 70 test classes (75 Kotlin test source files in total)
- **Total Tests Executed:** **437**
- **Passed:** **437 (100%)**
- **Failures:** **0**
- **Errors:** **0**
- **Skipped:** **0**
- **Duration:** 1m 42.98s
- **Gradle Task Result:** `BUILD SUCCESSFUL`

---

## 2. Critical Audit Points & Limitations (Pre-Phase 4 Review)

### Point A: Idempotency Key Persistence vs. In-Memory Store Limitation
* **Observed Implementation:** In `DefaultTransactionService.kt`, idempotency tracking uses `ConcurrentHashMap<String, TransactionResult>()`.
* **Identified Vulnerability / Limitation:** 
  - If the application is killed, crashes, or the OS reclaims memory, the in-memory cache is wiped.
  - A duplicate transaction request submitted after a cold app restart will bypass this in-memory check.
  - While `DefaultTransactionRepository` uses atomic Room transactions and safe subtraction checks, **the database schema currently lacks a unique constraint or persistent `idempotency_key` column on the `transactions` table**.
* **Audit Verdict:** The current JVM tests pass because tests run in a single process lifecycle. True process-death idempotency is **NOT** backed by persistent database unique constraints. This must be formally audited and cataloged in Task 4.2.

### Point B: Face Recognition Embedding Pipeline vs. Actual Model Inference
* **Observed Implementation:** `DefaultFaceRecognitionService.kt` simulates embedding generation deterministically from byte hash seeds (`extractEmbedding`) and compares 128-dimensional vectors via cosine similarity.
* **Identified Limitation:**
  - This test suite validates the *mathematical vector matching and boundary error flow* (poor quality, low light, multiple faces, permission denied, threshold matching).
  - It does **NOT** prove that a live on-device TensorFlow Lite / MobileFaceNet model or MediaPipe Face Landmarker is executing inference on camera frames at runtime.
  - As declared in the codebase comments, the embedding extractor is a deterministic stub for offline identity verification algorithm testing.

### Point C: Absence of Instrumented Android Runtime Tests (androidTest)
* **Observed Implementation:** All 437 tests execute under the JVM / Robolectric environment. There is no `app/src/androidTest` directory.
* **Identified Limitation:**
  There is currently no automated test evidence on physical hardware or an Android emulator for:
  - Real Android OS `BiometricPrompt` and hardware secure enclave integration.
  - Camera hardware capture lifecycle, preview surface, and dynamic runtime permission granting.
  - Real hardware-backed `AndroidKeyStore` operations (MasterKey / KeyGenParameterSpec).
  - Real Jetpack Compose UI rendering, touch dispatch, gestures, and user interaction.

### Point D: Known Gap in Booking Workflows
* **Observed Implementation:** There are no dedicated entities, DAOs, or repositories for Flight, Hotel, or Cinema Ticket bookings.
* **Identified Limitation:** This is a **Known Gap** against preliminary course concept notes. In the current implementation, third-party services are approximated solely through the utility bill payment simulator.

### Point E: Unresolved Business Decisions (Phase 3 Blocker Items)
The following decisions remain strictly **OPEN / BLOCKED / PENDING**:
1. **Saving Interest & Mortgage Formulas (OQ-3.4-*):** Intentionally `BLOCKED` in `FinancialFormulas.kt`. No interest or amortization calculation is guessed or fabricated.
2. **High-Value Transaction Threshold (OQ-3.6-1):** Marked as `PendingPolicy`. (Decision 2345/QĐ-NHNN and 10M VND are external reference context only; no unapproved product threshold has been hardcoded).
3. **Step-Up Authentication for High-Value (OQ-3.6-2):** Undecided.
4. **2FA Compliance Mapping (OQ-3.6-3):** Face verification + device screen lock are NOT conflated with true independent 2FA.
5. **Utility Simulator Pricing & Fees (OQ-3.7-1):** Unapproved; amounts are user-entered or simulated.
6. **Refund & Cancellation Policies (OQ-3.7-2):** Unapproved; lifecycle ends at `PAID`/`COMPLETED`.

---

## 3. Test Execution Details by Package

| Package / Domain Slice | Test Classes | Test Count | Result |
|---|:---:|:---:|:---:|
| `com.mebanking.app.data.local` (Converters) | 1 | 5 | ✅ PASS |
| `com.mebanking.app.data.local.dao` (Room DAOs) | 8 | 33 | ✅ PASS |
| `com.mebanking.app.data.local.migration` (Schema Migration) | 1 | 2 | ✅ PASS |
| `com.mebanking.app.data.local.security` (PBKDF2 Password Hashing) | 2 | 23 | ✅ PASS |
| `com.mebanking.app.data.local.seed` (Database Seeder) | 1 | 4 | ✅ PASS |
| `com.mebanking.app.data.navigation` (Road Graph Asset Loader) | 1 | 4 | ✅ PASS |
| `com.mebanking.app.data.repository` (Repositories) | 3 | 22 | ✅ PASS |
| `com.mebanking.app.di` (AppContainer & ViewModelFactory) | 2 | 7 | ✅ PASS |
| `com.mebanking.app.domain` (Math, Currency, Balance) | 3 | 21 | ✅ PASS |
| `com.mebanking.app.domain.model` (Error Mapping) | 1 | 3 | ✅ PASS |
| `com.mebanking.app.domain.navigation` (A*, Dijkstra, Haversine) | 3 | 20 | ✅ PASS |
| `com.mebanking.app.domain.service` (Domain & Security Services) | 7 | 82 | ✅ PASS |
| `com.mebanking.app.domain.util` (Financial Formula Guards) | 1 | 6 | ✅ PASS |
| `com.mebanking.app.e2e` (Phase 2 E2E Tiers) | 10 | 56 | ✅ PASS |
| `com.mebanking.app.e2e.phase3.tier1` (Phase 3 Happy Path E2E) | 7 | 38 | ✅ PASS |
| `com.mebanking.app.e2e.phase3.tier2` (Phase 3 Boundary E2E) | 9 | 45 | ✅ PASS |
| `com.mebanking.app.e2e.phase3.tier3` (Phase 3 Cross-Feature E2E) | 1 | 10 | ✅ PASS |
| `com.mebanking.app.e2e.phase3.tier4` (Phase 3 Real-World Scenarios) | 1 | 6 | ✅ PASS |
| `com.mebanking.app.phase4` (Production Integrity & Security) | 1 | 18 | ✅ PASS |
| `com.mebanking.app.ui` (Integration & ViewModel Tests) | 6 | 32 | ✅ PASS |
| **TOTAL** | **70** | **437** | **100% PASS** |

---

## 4. Priority Areas Verification Audit

| Priority Area | Test Coverage Files | Scenarios Verified | Result |
|---|---|---|:---:|
| **1. Authentication** | `PasswordHasherTest`<br>`PasswordHasherVersioningTest`<br>`SessionManagerTest`<br>`Tier1AuthRbacE2ETest`<br>`Tier2SessionBoundaryE2ETest` | PBKDF2WithHmacSHA256 100k iterations, salt isolation, versioned hash upgrade, login session states (authenticated, unauthenticated, expired, logged_out), invalid credentials, session timeout. | ✅ PASS |
| **2. Authorization** | `OfficerCustomerServiceTest`<br>`RbacEnforcementTest`<br>`Tier1AuthRbacE2ETest`<br>`Tier2RbacBoundaryE2ETest` | Customer role restrictions, Officer role privilege limits, unauthorized customer modification rejection, service-layer RBAC enforcement. | ✅ PASS |
| **3. Account Calculations** | `AccountDaoTest`<br>`AccountServiceTest`<br>`AccountDetailViewModelTest`<br>`BalanceAggregationTest`<br>`CurrencyFormatterTest`<br>`SafeMoneyMathTest`<br>`FinancialFormulasTest`<br>`Tier2FinancialFormulaBoundaryE2ETest` | Whole-unit `Long` VND math, single currency aggregation, safe addition/subtraction without silent overflow, BLOCKED saving/mortgage unapproved formulas returning explicit error states. | ✅ PASS |
| **4. Deposit & 5. Withdraw** | `TransactionServiceTest`<br>`Tier1TransactionCoreE2ETest`<br>`Tier2NegativeAmountBoundaryE2ETest`<br>`Tier2MonetaryOverflowBoundaryE2ETest` | Valid deposit/withdrawal balance updates, non-negative balance constraint, negative/zero amount rejection, `Long.MAX_VALUE` overflow rejection, ledger audit trail. | ✅ PASS |
| **6. Internal Transfer** | `TransactionRepositoryTest`<br>`TransactionServiceTest`<br>`Tier1TransactionCoreE2ETest`<br>`Tier2ConcurrencyBoundaryE2ETest`<br>`Tier2IdempotencyBoundaryE2ETest` | Atomic 6-step `@Transaction` (source debit, dest credit, ledger), concurrent withdrawal race protection, in-memory idempotency key deduplication. *(Persistent DB idempotency noted as gap).* | ⚠️ CONDITIONAL PASS |
| **7. Interbank Transfer** | `DefaultSimulatorService`<br>`Tier1SimulatorsE2ETest`<br>`ExternalBankDaoTest` | External bank code validation, interbank account number lookup, simulated network delay, lifecycle status transitions. | ✅ PASS |
| **8. Transaction Authentication** | `TransactionAuthenticationPolicyTest`<br>`Tier1TransactionAuthPolicyE2ETest`<br>`FaceRecognitionServiceTest`<br>`Tier2FaceBiometricBoundaryE2ETest` | Fail-closed policy evaluation, explicit `AuthenticationDecision` sealed types, no face detected, multiple faces, low light, embedding mismatch, no liveness claims. *(On-device camera/model inference noted as limitation).* | ⚠️ CONDITIONAL PASS |
| **9. Utility Payments** | `BillDaoTest`<br>`Tier1SimulatorsE2ETest`<br>`BillPaymentViewModel` | Bill lookup by code/provider, electricity/water/internet simulation, payment idempotency, state transition from `CREATED` to `PAID`. | ✅ PASS |
| **10. Booking Workflows** | *None in code base* | Requirements catalog gap: hotel/flight/cinema booking modules are absent from current core schema and test suite. Handled via utility simulator contract. | ⚠️ KNOWN GAP |
| **11. Chatbot** | `ChatIntentDaoTest`<br>`ChatbotViewModelTest`<br>`Tier1ChatbotE2ETest` | Offline keyword intent pattern matching, response template formatting, fallback to unknown intent help message. | ✅ PASS |
| **12. Navigation Routing** | `RoadGraphLoaderTest`<br>`AStarPathFinderTest`<br>`DijkstraRouterTest`<br>`GeoDistanceCalculatorTest`<br>`BranchNavigationRepositoryTest`<br>`BranchLocatorViewModelTest`<br>`OfflineMapProjectionTest`<br>`Tier1RoadGraphE2ETest`<br>`Tier1LocatorE2ETest`<br>`Tier2LocatorBoundaryE2ETest` | Local 18-vertex JSON road graph loading, Haversine distance, A* shortest path calculation, Dijkstra routing, offline map coordinates projection, missing branch/asset error handling. | ✅ PASS |

---

## 5. Gate 4.1 Decision

**Decision:** **CONDITIONAL PASS**  
The unit and integration test suite compiles and executes with 100% pass rate (437/437). All identified architectural limitations (in-memory idempotency vs. database persistence, embedding simulation vs. actual camera inference, absence of `androidTest` instrumented tests, booking gap, and open business decisions) are rigorously documented and must serve as explicit audit boundaries for Tasks 4.2 through 4.5.
