# Phase 3 — Implementation Map

**Last Updated**: 2026-11-21

Maps every Phase 3 source file to its task, type, and test coverage.

---

## Domain Services (`app/src/main/java/com/mebanking/app/domain/service/`)

| File | Task | Type | Tests |
|------|------|------|-------|
| `AuthService.kt` | 3.2 | Interface | `SessionManagerTest`, `RbacEnforcementTest` |
| `DefaultAuthService.kt` | 3.2 | Implementation | (via integration) |
| `SessionManager.kt` | 3.2 | Interface + Impl | `SessionManagerTest` |
| `OfficerCustomerService.kt` | 3.2 | Interface + Impl | `OfficerCustomerServiceTest`, `RbacEnforcementTest` |
| `FaceRecognitionService.kt` | 3.3 | Interface + Impl | `FaceRecognitionServiceTest`, `Tier2FaceBiometricBoundaryE2ETest` |
| `AccountService.kt` | 3.4 | Interface + Impl | `AccountServiceTest`, `Tier1AccountDomainE2ETest` |
| `TransactionService.kt` | 3.5 | Interface | `TransactionServiceTest`, `Tier1TransactionCoreE2ETest` |
| `DefaultTransactionService.kt` | 3.5 | Implementation | `TransactionServiceTest` |
| `TransactionAuthenticationPolicy.kt` | 3.6 | Interface + Impl | `TransactionAuthenticationPolicyTest`, `Tier1TransactionAuthPolicyE2ETest` |
| `SimulatorService.kt` | 3.7 | Interface | `Tier1SimulatorsE2ETest` |
| `DefaultSimulatorService.kt` | 3.7 | Implementation | `Tier1SimulatorsE2ETest` |
| `ChatbotService.kt` | 3.9 | Interface | `Tier1ChatbotE2ETest` |
| `DefaultChatbotService.kt` | 3.9 | Implementation | `Tier1ChatbotE2ETest` |

---

## Domain Models (`app/src/main/java/com/mebanking/app/domain/model/`)

| File | Task | Contents |
|------|------|----------|
| `AuthenticationDecision.kt` | 3.6 | Sealed: Approved, Rejected, Cancelled, TimedOut, Unavailable, PendingPolicy |
| `FaceRecognitionModels.kt` | 3.3 | Pipeline stages, failure modes, result types |
| `SimulatorModels.kt` | 3.7 | Order, Payment, Transaction, status enums |
| `ChatbotModels.kt` | 3.9 | ChatQuery, ChatResponse, IntentMatch |
| `TypedAccount.kt` | 3.4 | Sealed: CheckingAccount, SavingAccount, MortgageAccount |
| `TransactionModels.kt` | 3.5 | TransferRequest, TransferResult, LedgerEntry |

---

## Repositories (`app/src/main/java/com/mebanking/app/data/repository/`)

| File | Task | Type | Tests |
|------|------|------|-------|
| `AuthRepository.kt` | 3.2 | Interface | `SessionManagerTest` |
| `DefaultAuthRepository.kt` | 3.2 | Room-backed | (via integration) |
| `TransactionRepository.kt` | 3.5 | Interface | `TransactionRepositoryTest` |
| `DefaultTransactionRepository.kt` | 3.5 | Room-backed + `@Transaction` | `TransactionRepositoryTest` |
| `SimulatorRepository.kt` | 3.7 | Interface | `Tier1SimulatorsE2ETest` |
| `DefaultSimulatorRepository.kt` | 3.7 | Room-backed | `Tier1SimulatorsE2ETest` |
| `ChatbotRepository.kt` | 3.9 | Interface | `Tier1ChatbotE2ETest` |
| `DefaultChatbotRepository.kt` | 3.9 | Room-backed | `Tier1ChatbotE2ETest` |

---

## DI Container (`app/src/main/java/com/mebanking/app/di/`)

| File | Task | Note |
|------|------|------|
| `AppContainer.kt` | 3.1 | Interface declaring all service/repo dependencies + `databaseReady` |
| `DefaultAppContainer.kt` | 3.1 | Full wiring: all Repositories + Services; idempotent first-launch seeding |
| `ViewModelFactory.kt` | 3.1 | ViewModel creation from container for all screens |

**Wired services in `DefaultAppContainer`:**
- `authRepository`, `authService`, `sessionManager`
- `officerCustomerService`
- `faceRecognitionService`
- `accountRepository`, `accountService`
- `transactionRepository`, `transactionService`
- `transactionAuthenticationPolicy`
- `simulatorRepository`, `simulatorService`
- `chatbotRepository`, `chatbotService`
- `branchNavigationRepository` (Phase 2, retained)

---

## Utilities (`app/src/main/java/com/mebanking/app/domain/util/`)

| File | Task | Purpose |
|------|------|---------|
| `CoroutineUtils.kt` | 3.1 | Safe coroutine helpers; `CancellationException` guard |
| `FinancialFormulas.kt` | 3.4 | BLOCKED formulas with OPEN placeholders |
| `SafeMoneyMath.kt` | 3.1/3.5 | `Math.addExact`/`subtractExact` wrappers → business error |
| `CurrencyFormatter.kt` | 3.1 | VND display formatting |
| `BalanceAggregator.kt` | 3.1/3.4 | Same-currency-only aggregation |

---

## UI Screens (`app/src/main/java/com/mebanking/app/ui/`)

| File | Task | Status |
|------|------|--------|
| `home/HomeScreen.kt` | 3.10 | ✅ Quick actions, account deep links, logout |
| `home/HomeViewModel.kt` | 3.10 | ✅ Session-scoped via `AccountRepository` + `SessionManager` |
| `navigation/BranchLocatorScreen.kt` | 3.8 | ✅ Phase 2, verified |
| `navigation/BranchLocatorViewModel.kt` | 3.8 | ✅ Phase 2, verified |
| `navigation/OfflineMapView.kt` | 3.8 | ✅ Phase 2, verified |
| `auth/LoginScreen.kt` | 3.10 | ✅ Offline login gate for the root NavHost |
| `auth/LoginViewModel.kt` | 3.10 | ✅ Awaits `databaseReady`, real `AuthService` errors |
| `chatbot/ChatbotScreen.kt` | 3.10 | ✅ Transcript, suggestions, navigation chips |
| `chatbot/ChatbotViewModel.kt` | 3.10 | ✅ `ChatbotService` via DI |
| `transfer/TransferScreen.kt` | 3.10 | ✅ Internal transfer form + result state |
| `transfer/TransferViewModel.kt` | 3.10 | ✅ Long VND validation, idempotency key reuse |
| `bills/BillPaymentScreen.kt` | 3.10 | ✅ Unpaid bills, account picker, history |
| `bills/BillPaymentViewModel.kt` | 3.10 | ✅ Room-backed Flows + idempotent `payBill` |
| `account/AccountDetailScreen.kt` | 3.10 | ✅ Checking / Saving / Mortgage detail |
| `account/AccountDetailViewModel.kt` | 3.10 | ✅ Blocked saving formula surfaced, mortgage details |
| `common/AccountLabels.kt` | 3.10 | ✅ Shared account-type labels |

---

## Test Files — Phase 3

### Unit Tests

| File | Task | Passes |
|------|------|--------|
| `HomeViewModelCancellationTest` | 3.1 | ✅ |
| `DomainErrorMappingTest` | 3.1 | ✅ |
| `SessionManagerTest` | 3.2 | ✅ |
| `RbacEnforcementTest` | 3.2 | ✅ |
| `OfficerCustomerServiceTest` | 3.2 | ✅ |
| `FaceRecognitionServiceTest` | 3.3 | ✅ |
| `AccountServiceTest` | 3.4 | ✅ |
| `FinancialFormulasTest` | 3.4 | ✅ |
| `TransactionRepositoryTest` | 3.5 | ✅ |
| `TransactionServiceTest` | 3.5 | ✅ |
| `TransactionAuthenticationPolicyTest` | 3.6 | ✅ |
| `ChatbotViewModelTest` | 3.10 | ✅ |
| `AccountDetailViewModelTest` | 3.10 | ✅ |

### E2E Tests — Tier 1 (Happy Path)

| File | Task | Passes |
|------|------|--------|
| `Tier1AuthRbacE2ETest` | 3.2 | ✅ |
| `Tier1AccountDomainE2ETest` | 3.4 | ✅ |
| `Tier1TransactionCoreE2ETest` | 3.5 | ✅ |
| `Tier1TransactionAuthPolicyE2ETest` | 3.6 | ✅ |
| `Tier1SimulatorsE2ETest` | 3.7 | ✅ |
| `Tier1NavigationE2ETest` | 3.8 | ✅ |
| `Tier1ChatbotE2ETest` | 3.9 | ✅ |

### E2E Tests — Tier 2 (Boundary)

| File | Key Scenario | Passes |
|------|-------------|--------|
| `Tier2SessionBoundaryE2ETest` | Session expiry & re-auth | ✅ |
| `Tier2RbacBoundaryE2ETest` | Cross-role rejection | ✅ |
| `Tier2FaceBiometricBoundaryE2ETest` | Pipeline failures | ✅ |
| `Tier2FinancialFormulaBoundaryE2ETest` | BLOCKED formula guards | ✅ |
| `Tier2IdempotencyBoundaryE2ETest` | Duplicate transaction rejection | ✅ |
| `Tier2ConcurrencyBoundaryE2ETest` | Concurrent withdrawal race | ✅ |
| `Tier2MonetaryOverflowBoundaryE2ETest` | Long overflow rejection | ✅ |
| `Tier2NegativeAmountBoundaryE2ETest` | Negative/zero amount | ✅ |
| `Tier2NavigationBoundaryE2ETest` | Asset error, no route | ✅ |

### E2E Tests — Tier 3 & 4

| File | Scenario | Passes |
|------|---------|--------|
| `Tier3CrossFeatureE2ETest` | Auth → Account → Transaction → Chatbot | ✅ |
| `Tier4RealWorldScenarioE2ETest` | Full user journey | ✅ |

---

## Build Status

| Command | Last Result | Date |
|---------|------------|------|
| `./gradlew testDebugUnitTest` | ✅ BUILD SUCCESSFUL | 2026-11-21 |
| `./gradlew compileDebugKotlin` | ✅ 0 errors | 2026-11-21 |
| `./gradlew assembleDebug` | ✅ APK built | 2026-11-21 |
| `./gradlew lintDebug` | ✅ BUILD SUCCESSFUL | 2026-11-21 |
