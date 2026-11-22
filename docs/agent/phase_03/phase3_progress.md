# Phase 3 — Progress Tracking

**Project**: Android Mobile Banking Application (`m.ebanking`)  
**Phase**: 3 — Domain Services, Auth, Transactions, Simulators & Chatbot  
**Agent Specification**: `.agent/work.md`  
**Last Updated**: 2026-11-21  

---

## Overall Status

| Milestone | Tasks | Status |
|-----------|-------|--------|
| **M1** — Project Foundation & Auth/Profile | 3.1, 3.2 | ✅ DONE |
| **M2** — eKYC & Account Management | 3.3, 3.4 | ✅ DONE |
| **M3** — Transaction Core & Auth Policy | 3.5, 3.6 | ✅ DONE |
| **M4** — Simulators, Chatbot & Navigation | 3.7, 3.8, 3.9 | ✅ DONE |
| **M5** — Full UI Integration | 3.10 | ✅ DONE |
| **Phase 3 Gate** | All tasks | ✅ PASSED (blocked business decisions remain documented and enforced) |

**Test count at last confirmed build (2026-11-21):** BUILD SUCCESSFUL — all tests passing, including new Task 3.10 ViewModel tests.

---

## Task-by-Task Status

### Task 3.1 — Project Foundation (DI & Architecture) ✅

**Status:** DONE  
**Deliverables:**

| Artifact | Path | Note |
|----------|------|------|
| `AppContainer` (interface) | `di/AppContainer.kt` | Single DI contract |
| `DefaultAppContainer` | `di/DefaultAppContainer.kt` | Unified wiring, no ad-hoc |
| `ErrorMapper` | `domain/model/ErrorMapper.kt` | No raw `Throwable.message` to UI |
| `CoroutineUtils` | `domain/util/CoroutineUtils.kt` | `CancellationException` NOT swallowed |
| `HomeViewModelCancellationTest` | `ui/home/HomeViewModelCancellationTest.kt` | Passes |
| `DomainErrorMappingTest` | `domain/model/DomainErrorMappingTest.kt` | Passes |

**Architecture enforced:**
- Single `AppContainer` → `DefaultAppContainer` path; zero ad-hoc constructor DI
- Repository is boundary between ViewModel and DAO
- `Flow` for observation; `suspend` for one-shot async
- `CancellationException` propagation guaranteed

---

### Task 3.2 — User, Authentication & Profile ✅

**Status:** DONE  
**Deliverables:**

| Artifact | Path | Note |
|----------|------|------|
| `AuthService` (interface) | `domain/service/AuthService.kt` | |
| `DefaultAuthService` | `domain/service/DefaultAuthService.kt` | PBKDF2WithHmacSHA256 verified |
| `SessionManager` (interface) | `domain/service/SessionManager.kt` | |
| `DefaultSessionManager` | `domain/service/SessionManager.kt` | 4-state lifecycle |
| `AuthRepository` (interface) | `data/repository/AuthRepository.kt` | |
| `DefaultAuthRepository` | `data/repository/DefaultAuthRepository.kt` | |
| `OfficerCustomerService` (interface) | `domain/service/OfficerCustomerService.kt` | RBAC |
| `DefaultOfficerCustomerService` | `domain/service/OfficerCustomerService.kt` | Business-layer enforcement |
| `RbacEnforcementTest` | `domain/service/RbacEnforcementTest.kt` | Passes |
| `SessionManagerTest` | `domain/service/SessionManagerTest.kt` | Passes |

**Session states:** `authenticated`, `unauthenticated`, `expired`, `logged_out`  
**Password hashing:** `PBKDF2WithHmacSHA256`, 100K iterations, 16-byte `SecureRandom` salt, versioned format `<iterations>:<saltHex>:<hashHex>`. **SHA-256 NOT restored.**  
**RBAC:** Privilege enforced at business/service layer, not only UI visibility.

---

### Task 3.3 — eKYC & Local Face Recognition ✅

**Status:** DONE  
**Deliverables:**

| Artifact | Path | Note |
|----------|------|------|
| `FaceRecognitionService` (interface) | `domain/service/FaceRecognitionService.kt` | |
| `DefaultFaceRecognitionService` | `domain/service/FaceRecognitionService.kt` | 6-stage pipeline |
| `FaceRecognitionModels` | `domain/model/FaceRecognitionModels.kt` | Sealed result types |
| `FaceRecognitionServiceTest` | `domain/service/FaceRecognitionServiceTest.kt` | Passes |

**6-Stage Pipeline:**  
`camera_capture` → `face_detection` → `face_alignment` → `mobilefacenet_extraction` → `embedding_comparison` → `verification_result`

**Explicit failure modes handled:**
- No face detected
- Multiple faces detected
- Low light / low quality image
- Face mismatch (embedding distance exceeds threshold)
- Model load failure
- Camera permission denied
- Embedding access control

> [!IMPORTANT]
> **Face matching ≠ Liveness Detection.** No anti-spoofing, video-replay or deepfake protection is implemented or claimed. Do NOT add such claims without implementing and testing liveness detection.

---

### Task 3.4 — Account Management ✅

**Status:** DONE (Saving/Mortgage formula decisions BLOCKED pending approval)  
**Deliverables:**

| Artifact | Path | Note |
|----------|------|------|
| `AccountService` (interface) | `domain/service/AccountService.kt` | |
| `DefaultAccountService` | `domain/service/AccountService.kt` | Checking, Saving, Mortgage |
| `TypedAccount` | `domain/model/TypedAccount.kt` | Sealed account types |
| `FinancialFormulas` | `domain/util/FinancialFormulas.kt` | OPEN placeholders for blocked formulas |
| `AccountServiceTest` | `domain/service/AccountServiceTest.kt` | Passes |
| `FinancialFormulasTest` | `domain/util/FinancialFormulasTest.kt` | Passes |

**Open Questions — BLOCKED:**

| # | Topic | Status |
|---|-------|--------|
| OQ-3.4-1 | Saving interest formula (simple vs compound) | ❌ NOT APPROVED |
| OQ-3.4-2 | Saving interest period / compounding frequency | ❌ NOT APPROVED |
| OQ-3.4-3 | Rounding rule for Saving interest | ❌ NOT APPROVED |
| OQ-3.4-4 | Mortgage amortization formula (fixed vs variable) | ❌ NOT APPROVED |
| OQ-3.4-5 | Mortgage principal/interest payment allocation | ❌ NOT APPROVED |

> [!CAUTION]
> Do not compute interest or amortization values using invented formulas. Corresponding features remain BLOCKED until OQ-3.4-1 through OQ-3.4-5 are explicitly approved.

**Monetary rule:** All VND balances stored and computed as whole `Long` units. No `Double`/`Float` for money.

---

### Task 3.5 — Transaction Core ✅

**Status:** DONE  
**Deliverables:**

| Artifact | Path | Note |
|----------|------|------|
| `TransactionRepository` (interface) | `data/repository/TransactionRepository.kt` | |
| `DefaultTransactionRepository` | `data/repository/DefaultTransactionRepository.kt` | Atomic `@Transaction` |
| `TransactionService` (interface) | `domain/service/TransactionService.kt` | |
| `DefaultTransactionService` | `domain/service/DefaultTransactionService.kt` | Full integrity |
| `TransactionModels` | `domain/model/TransactionModels.kt` | |
| `TransactionRepositoryTest` | `data/repository/TransactionRepositoryTest.kt` | Passes |
| `TransactionServiceTest` | `domain/service/TransactionServiceTest.kt` | Passes |

**Atomic transfer protocol (all 6 steps in single DB transaction):**
1. Validate source account, destination account, ownership
2. Validate amount and available balance
3. Debit source account
4. Credit destination account
5. Write transaction ledger entry
6. Commit all changes atomically

**Integrity guarantees tested:**

| Scenario | Expected Result | Test Coverage |
|----------|----------------|---------------|
| Insufficient balance | No data change | ✅ |
| Concurrent withdrawal race | Cannot overdraw | ✅ |
| Duplicate confirmation tap | Idempotency key deduplication | ✅ |
| DB error mid-transfer | Full rollback | ✅ |
| Amount exceeds `Long.MAX_VALUE` | Safe rejection | ✅ |
| Non-existent destination | Source NOT debited | ✅ |

**Idempotency:** Every transaction request carries an idempotency key; duplicates are safely rejected.  
**Overflow:** `Math.addExact`/`subtractExact` overflow mapped to business error — never silently ignored.

---

### Task 3.6 — Transaction Authentication Policy ✅

**Status:** DONE (threshold/2FA decisions TBD — policy abstraction in place)  
**Deliverables:**

| Artifact | Path | Note |
|----------|------|------|
| `TransactionAuthenticationPolicy` | `domain/service/TransactionAuthenticationPolicy.kt` | Interface |
| `AuthenticationDecision` | `domain/model/AuthenticationDecision.kt` | Sealed class |
| `DefaultTransactionAuthenticationPolicy` | `domain/service/TransactionAuthenticationPolicy.kt` | |
| `TransactionAuthenticationPolicyTest` | `domain/service/TransactionAuthenticationPolicyTest.kt` | Passes |

**`AuthenticationDecision` states:**
- `Approved` — authentication confirmed, transaction may proceed
- `Rejected` — authentication explicitly failed
- `Cancelled` — user cancelled authentication
- `TimedOut` — authentication window expired
- `Unavailable` — authentication mechanism unavailable on device
- `PendingPolicy` — policy not yet configured (TBD items below)

**Open Questions — TBD:**

| # | Topic | Status |
|---|-------|--------|
| OQ-3.6-1 | High-value transaction threshold (VND amount) | ❌ NOT DECIDED |
| OQ-3.6-2 | Additional Device Auth required for high-value? | ❌ NOT DECIDED |
| OQ-3.6-3 | Face Recognition + BiometricPrompt satisfies 2FA? | ❌ NOT CONFIRMED |

> [!CAUTION]
> Transactions **MUST NOT** be committed unless `TransactionAuthenticationPolicy` returns `Approved`. Authentication MUST NOT be mocked as successful to pass tests.

---

### Task 3.7 — Utility Simulators ✅

**Status:** DONE  
**Deliverables:**

| Artifact | Path | Note |
|----------|------|------|
| `SimulatorRepository` (interface) | `data/repository/SimulatorRepository.kt` | |
| `DefaultSimulatorRepository` | `data/repository/DefaultSimulatorRepository.kt` | Room-backed |
| `SimulatorService` (interface) | `domain/service/SimulatorService.kt` | |
| `DefaultSimulatorService` | `domain/service/DefaultSimulatorService.kt` | |
| `SimulatorModels` | `domain/model/SimulatorModels.kt` | Order, Payment, Transaction models |
| `Tier1SimulatorsE2ETest` | `e2e/phase3/tier1/Tier1SimulatorsE2ETest.kt` | Passes |

**Order lifecycle (all simulators):**
```
CREATED → PENDING_PAYMENT → PAID → COMPLETED
```
(Per-simulator variants as appropriate to business rules)

**Integrity rules enforced:**
- No double payment for same order
- No order created if payment fails
- Payment and Order updated atomically
- All data persisted locally (Room) — zero external API calls

**Open Questions — BLOCKED:**

| # | Topic | Status |
|---|-------|--------|
| OQ-3.7-1 | Utility/ticket pricing, fees, cancellation policy | ❌ NOT APPROVED |
| OQ-3.7-2 | Refund policy for failed/cancelled orders | ❌ NOT APPROVED |

---

### Task 3.8 — Offline Navigation Integration ✅

**Status:** DONE (integration & verification, NOT rebuilt — Phase 2 implementation reused)  
**Phase 2 artifacts verified:**

| Artifact | Path | Verified |
|----------|------|---------|
| `hcmc_road_network.json` | `assets/navigation/hcmc_road_network.json` | ✅ 18 vertices, 17 edges |
| `RoadGraphLoader` | `data/navigation/RoadGraphLoader.kt` | ✅ |
| `GeoDistanceCalculator` | `domain/navigation/GeoDistanceCalculator.kt` | ✅ Haversine |
| `AStarPathFinder` | `domain/navigation/AStarPathFinder.kt` | ✅ |
| `DijkstraRouter` | `domain/navigation/DijkstraRouter.kt` | ✅ |
| `BranchNavigationRepository` | `data/repository/BranchNavigationRepository.kt` | ✅ |
| `BranchLocatorViewModel` | `ui/navigation/BranchLocatorViewModel.kt` | ✅ |
| `OfflineMapView` | `ui/navigation/OfflineMapView.kt` | ✅ Canvas, pan/zoom |
| `BranchLocatorScreen` | `ui/navigation/BranchLocatorScreen.kt` | ✅ Wired to NavHost |

**Edge cases verified:**

| Scenario | Handling | Test |
|----------|----------|------|
| Location permission denied | Graceful UI state | ✅ |
| Location unavailable | Error state shown | ✅ |
| No branch nearby | Empty state shown | ✅ |
| No route found | No-route state shown | ✅ |
| Missing/corrupt asset | Load error state | ✅ |
| Internet OFF | Fully offline operation | ✅ |

> [!NOTE]
> Road network dataset contains **18 vertices / 17 edges** covering HCMC Districts 1 & 7 as a **demo dataset**. This is NOT production map data. Clearly distinguished in UI and documentation.

---

### Task 3.9 — Local Rule-Based Chatbot ✅

**Status:** DONE  
**Deliverables:**

| Artifact | Path | Note |
|----------|------|------|
| `ChatbotRepository` (interface) | `data/repository/ChatbotRepository.kt` | |
| `DefaultChatbotRepository` | `data/repository/DefaultChatbotRepository.kt` | Room-backed |
| `ChatbotService` (interface) | `domain/service/ChatbotService.kt` | |
| `DefaultChatbotService` | `domain/service/DefaultChatbotService.kt` | Keyword pattern matching |
| `ChatbotModels` | `domain/model/ChatbotModels.kt` | |
| `Tier1ChatbotE2ETest` | `e2e/phase3/tier1/Tier1ChatbotE2ETest.kt` | Passes |

**Matching strategy:** Pattern matching on `ChatIntent.patterns: List<String>` (lowercase contains check) → returns `responseTemplate`. Seeded intents include: `check_balance`, `transfer_money`, `pay_bills`, `nearest_atm`, and others from `DatabaseSeeder`.

---

### Task 3.10 — Full UI Integration ✅

**Status:** DONE  
**Gate impact:** Completes the last open Phase 3 deliverable.

| Item | Status | Artifact / Evidence |
|------|--------|---------------------|
| `ChatbotScreen` (Compose) | ✅ | `ui/chatbot/ChatbotScreen.kt` — query input, transcript, action chips |
| `ChatbotViewModel` | ✅ | `ui/chatbot/ChatbotViewModel.kt` — `ChatbotService.processQuery` via DI |
| `TransferScreen` (Compose) | ✅ | `ui/transfer/TransferScreen.kt` — source account, destination, amount, result state |
| `TransferViewModel` | ✅ | `ui/transfer/TransferViewModel.kt` — validated Long VND, idempotency key reuse |
| `BillPaymentScreen` (Compose) | ✅ | `ui/bills/BillPaymentScreen.kt` — unpaid list, account picker, payment history |
| `BillPaymentViewModel` | ✅ | `ui/bills/BillPaymentViewModel.kt` — Room-backed bill Flows + idempotent payment |
| `AccountDetailScreen` (Compose) | ✅ | `ui/account/AccountDetailScreen.kt` — Checking / Saving / Mortgage layouts |
| `AccountDetailViewModel` | ✅ | `ui/account/AccountDetailViewModel.kt` — blocked-formula notice instead of invented values |
| `HomeScreen` quick-actions | ✅ | Transfer, Bills (customer only), Assistant, Branch/ATM + account row deep links |
| `HomeViewModel` DI wiring | ✅ | `ViewModelFactory` injects `AccountRepository` + `SessionManager`; no ad-hoc repository |
| `MEbankingApp` NavHost | ✅ | Routes: `home`, `transfer`, `bills`, `chatbot`, `account_detail/{accountId}`, `branch_locator` |
| Offline login gate | ✅ | `ui/auth/LoginScreen.kt` + `LoginViewModel`; root NavHost only renders when session is authenticated |
| First-launch seeding | ✅ | `AppContainer.databaseReady` awaits idempotent `DatabaseSeeder.seed` before login authentication |
| Role-safe navigation | ✅ | Customer-only quick actions; all service calls still enforce RBAC/ownership server-side |

**State handling:**
- Loading, empty, error, validation and success states are rendered from ViewModel state — no hardcoded success paths.
- High-value auth decisions surface the service's `PolicyRejected` message; no biometric or 2FA success is simulated.
- Saving profit and mortgage schedule formulas remain visibly blocked while OQ-3.4-* are unapproved.
- Bill payment and transfer reuse one idempotency key until the form changes or the operation succeeds, preventing double debits on repeated taps.

**Tests added:**
- `ui/chatbot/ChatbotViewModelTest` — transcript append, localized chips, blank-input guard.
- `ui/account/AccountDetailViewModelTest` — blocked saving formula, mortgage schedule projection, expired session.

---

## Phase 3 E2E Test Coverage

### Tier 1 — Happy Path

| Test File | Coverage |
|-----------|----------|
| `Tier1AuthRbacE2ETest` | Login, session lifecycle, RBAC enforcement |
| `Tier1AccountDomainE2ETest` | Account creation, balance queries, type dispatch |
| `Tier1TransactionCoreE2ETest` | Full transfer flow, ledger reconciliation |
| `Tier1TransactionAuthPolicyE2ETest` | Policy evaluation, decision states |
| `Tier1SimulatorsE2ETest` | Order lifecycle, payment, status transitions |
| `Tier1ChatbotE2ETest` | Intent matching, response templating |
| `Tier1NavigationE2ETest` | Route finding, branch lookup, offline map |

### Tier 2 — Boundary & Error

| Test File | Coverage |
|-----------|----------|
| `Tier2SessionBoundaryE2ETest` | Session expiry, logout, re-auth |
| `Tier2RbacBoundaryE2ETest` | Cross-role privilege rejection |
| `Tier2FaceBiometricBoundaryE2ETest` | Pipeline failures, no-face, multi-face |
| `Tier2FinancialFormulaBoundaryE2ETest` | BLOCKED formula guards |
| `Tier2IdempotencyBoundaryE2ETest` | Duplicate transaction rejection |
| `Tier2ConcurrencyBoundaryE2ETest` | Concurrent withdrawal race |
| `Tier2MonetaryOverflowBoundaryE2ETest` | Long overflow rejection |
| `Tier2NegativeAmountBoundaryE2ETest` | Negative/zero amount rejection |
| `Tier2NavigationBoundaryE2ETest` | Missing asset, no route, permission denied |

### Tier 3 — Cross-Feature

| Test File | Coverage |
|-----------|----------|
| `Tier3CrossFeatureE2ETest` | Auth → Account → Transaction → Chatbot flow |

### Tier 4 — Real-World Scenario

| Test File | Coverage |
|-----------|----------|
| `Tier4RealWorldScenarioE2ETest` | Full user journey: register, login, transfer, pay bill |

---

## Open Questions Log

| ID | Phase | Topic | Status | Blocking |
|----|-------|-------|--------|---------|
| OQ-3.4-1 | 3.4 | Saving interest formula | ❌ Open | Saving interest calculation |
| OQ-3.4-2 | 3.4 | Saving compounding frequency | ❌ Open | Saving interest calculation |
| OQ-3.4-3 | 3.4 | Saving interest rounding rule | ❌ Open | Saving interest calculation |
| OQ-3.4-4 | 3.4 | Mortgage amortization formula | ❌ Open | Mortgage payment calculation |
| OQ-3.4-5 | 3.4 | Mortgage principal/interest allocation | ❌ Open | Mortgage payment calculation |
| OQ-3.6-1 | 3.6 | High-value threshold (VND) | ❌ Open | Auth policy routing |
| OQ-3.6-2 | 3.6 | Device Auth required for high-value | ❌ Open | Auth policy routing |
| OQ-3.6-3 | 3.6 | Face + BiometricPrompt = 2FA? | ❌ Open | 2FA compliance claim |
| OQ-3.7-1 | 3.7 | Utility/ticket pricing & fees | ❌ Open | Simulator billing |
| OQ-3.7-2 | 3.7 | Refund/cancellation policy | ❌ Open | Simulator refund flow |

---

## Architectural Gap Resolution (C01–C07 from Phase 0)

| Gap | Title | Phase 3 Resolution | Status |
|-----|-------|-------------------|--------|
| **C01** | Monetary Data Type | All `Long` VND units; `SafeMoneyMath`, overflow to business error | ✅ RESOLVED |
| **C02** | Offline-First Gap | All data via Room; zero runtime network calls | ✅ RESOLVED |
| **C03** | Dependency Wiring | `AppContainer`/`DefaultAppContainer` unified DI | ✅ RESOLVED |
| **C04** | Coroutine Cancellation | `CoroutineUtils` — `CancellationException` NOT swallowed | ✅ RESOLVED |
| **C05** | Error Presentation | `ErrorMapper` — no `Throwable.message` to UI | ✅ RESOLVED |
| **C06** | Balance Aggregation | `BalanceAggregator` with same-currency-only rule | ✅ RESOLVED |
| **C07** | Security/Privacy Exposure | No HTTP logging; password hashing; embedding access control | ✅ RESOLVED |

---

## Phase 3 Gate Checklist

| Criterion | Status |
|-----------|--------|
| All Tasks 3.1–3.10 DONE | ✅ |
| `./gradlew testDebugUnitTest` — BUILD SUCCESSFUL | ✅ Confirmed 2026-11-21 |
| `./gradlew assembleDebug` — APK builds | ✅ Confirmed 2026-11-21 |
| `./gradlew lintDebug` — no errors | ✅ Confirmed 2026-11-21 |
| All known open questions documented | ✅ |
| No SHA-256 regression in password hashing | ✅ |
| No liveness/anti-spoofing claimed | ✅ |
| No invented financial formulas | ✅ BLOCKED properly in UI and service |
| No authentication mocked to pass tests | ✅ |
| All data offline-first (Room, local assets) | ✅ |
| `docs/agent/phase_03/` deliverables complete | ✅ |

**Phase 3 Gate Decision: ✅ PASSED** — Task 3.10 closes the final deliverable. OQ-3.4-*, OQ-3.6-* and OQ-3.7-* remain unapproved business decisions and continue to be enforced as blocked states, not implemented with guessed formulas or fake authentication.
