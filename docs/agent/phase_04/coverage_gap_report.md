# Task 4.1 — Coverage Gap Report

**Date:** 2026-11-22  
**Role:** Independent QA Engineer  
**Scope:** Test coverage analysis across priorities, edge cases, and known specification gaps.

---

## 1. Test Suite Distribution

- **Total Unit & Integration Test Files:** 75 Kotlin files under `app/src/test/`
- **Total Test Executions:** 437 tests passing (0 failures, 0 errors, 0 skipped)
- **Robolectric & Coroutines Test Support:** Fully integrated with `TestAppContainer` and `MainDispatcherRule`.
- **Instrumented Test Execution:** 0 tests (No `app/src/androidTest` suite exists).

---

## 2. Priority Area Coverage & Gap Analysis

| Area | Current Test Coverage | Status | Identified Gaps & Mitigation |
|---|---|:---:|---|
| **1. Authentication** | 7 test files, 35+ tests (`PasswordHasherTest`, `SessionManagerTest`, `Tier1AuthRbacE2ETest`, `Tier2SessionBoundaryE2ETest`). | **COMPREHENSIVE (LOCAL)** | Real Android BiometricPrompt hardware integration cannot be verified on headless JVM; covered via `TransactionAuthenticationPolicy` contracts. |
| **2. Authorization (RBAC)** | 4 test files (`OfficerCustomerServiceTest`, `RbacEnforcementTest`, `Tier1AuthRbacE2ETest`, `Tier2RbacBoundaryE2ETest`). | **COMPREHENSIVE (LOCAL)** | Full boundary testing verifying customer cannot access officer capabilities and officer cannot modify unauthorized accounts. |
| **3. Account Calculations** | 10 test files (`AccountDaoTest`, `AccountServiceTest`, `SafeMoneyMathTest`, `FinancialFormulasTest`, `BalanceAggregationTest`). | **VERIFIED (WITH BLOCKED FORMULAS)** | Saving interest and mortgage amortization formulas are intentionally `BLOCKED` in `FinancialFormulas` pending approval (OQ-3.4-*). Tests explicitly assert fail-closed handling. |
| **4. Deposit & Withdraw** | 4 test files (`TransactionServiceTest`, `Tier1TransactionCoreE2ETest`, `Tier2MonetaryOverflowBoundaryE2ETest`, `Tier2NegativeAmountBoundaryE2ETest`). | **COMPREHENSIVE (LOCAL)** | Negative amount, overflow, zero amount, and balance mutation ledger consistency are strictly verified. |
| **5. Internal Transfer** | 4 test files (`TransactionRepositoryTest`, `TransactionServiceTest`, `Tier2ConcurrencyBoundaryE2ETest`, `Tier2IdempotencyBoundaryE2ETest`). | **COMPREHENSIVE (LOCAL PROCESS)** | Full atomic `@Transaction` rollback, concurrent race prevention, and in-memory idempotency key deduplication covered. **Gap:** Database schema does not store `idempotency_key` with unique constraint; process death resets idempotency protection. |
| **6. Interbank Transfer** | 2 test files (`ExternalBankDaoTest`, `Tier1SimulatorsE2ETest`). | **SATISFACTORY (SIMULATED)** | External bank routing simulation verified. No external network integration (offline-first requirement). |
| **7. Transaction Authentication** | 2 test files (`Tier1TransactionAuthPolicyE2ETest`, `Tier2FaceBiometricBoundaryE2ETest`). | **SATISFACTORY (ALGORITHMIC)** | Face recognition pipeline errors and biometric decision states (`Approved`, `Rejected`, `PendingPolicy`) verified. **Gap:** On-device camera capture lifecycle and live MobileFaceNet TFLite inference are not executed; embedding generation is a deterministic seed stub. High-value VND threshold remains open (OQ-3.6-*). |
| **8. Utility Payments** | 2 test files (`BillDaoTest`, `Tier1SimulatorsE2ETest`). | **SATISFACTORY (SIMULATED)** | Bill payment lifecycle (`CREATED` -> `PAID`) verified. Real utility provider pricing/fees remain simulated. |
| **9. Booking Workflows** | *None* | **KNOWN GAP** | Ticket/flight/hotel booking workflows mentioned in preliminary course notes are not implemented as dedicated database entities. Mitigated via utility bill simulator patterns. |
| **10. Chatbot** | 3 test files (`ChatIntentDaoTest`, `ChatbotViewModelTest`, `Tier1ChatbotE2ETest`). | **COMPREHENSIVE (LOCAL)** | Offline pattern matching, intent resolution, and response generation tested. |
| **11. Navigation Routing** | 11 test files (`RoadGraphLoaderTest`, `AStarPathFinderTest`, `DijkstraRouterTest`, `GeoDistanceCalculatorTest`, `BranchLocatorViewModelTest`, etc.). | **COMPREHENSIVE (LOCAL)** | Graph parsing, A* / Dijkstra shortest paths, Haversine distance, and Canvas coordinate projections verified. |

---

## 3. UI and Instrumentation Test Coverage

- **ViewModel Tests:** `HomeViewModelCancellationTest`, `BranchLocatorViewModelTest`, `ChatbotViewModelTest`, `AccountDetailViewModelTest`, `Phase4ViewModelIntegrationTest` provide full state flow verification on Robolectric.
- **Instrumented UI Tests:** Currently, there is no `app/src/androidTest` directory (all 437 tests execute under Robolectric/JVM). Screen layouts are verified via Compose Preview and ViewModel integration tests; hardware-level interactions remain untested.

---

## 4. Recommendations for Subsequent Phase 4 Tasks

1. **Task 4.2 (Transaction Integrity Audit):** Deep dive into the missing database-level persistent idempotency column and evaluate the consequences of app termination mid-transfer.
2. **Task 4.3 (Security Review):** Ensure face templates, keystore interactions, and session tokens are audited with zero over-claim regarding liveness and anti-spoofing.
