# Task 4.1 — Coverage Gap Report

**Date:** 2026-11-21  
**Scope:** Static mapping of the existing test source to the Task 4.1 priority areas. Execution status is in `test_execution_report.md`.

## Inventory

- 73 Kotlin test files under `app/src/test`.
- 412 `@Test` annotations found by source search. This is not a test-run total.
- No `app/src/androidTest` directory and no Compose UI testing dependency in `app/build.gradle.kts` / `gradle/libs.versions.toml`.
- Tests are organized across DAO/migration/security/seeding, repository, domain service, utility, navigation, E2E tiers, and a small number of ViewModel tests.

## Coverage Mapping (source assertions inspected; execution unverified)

| Area | Existing evidence | Static assessment | Gaps / follow-up |
|---|---|---|---|
| Authentication and session | `domain/service/SessionManagerTest.kt`, `RbacEnforcementTest.kt`, `e2e/phase3/tier1/Tier1AuthRbacE2ETest.kt`, `tier2/Tier2SessionBoundaryE2ETest.kt`, `Tier2RbacBoundaryE2ETest.kt` | Positive, negative, expired-session and role-boundary scenarios are represented. | Must execute; no real Android credential/device authentication integration test is present. |
| Account calculations | `AccountServiceTest.kt`, `FinancialFormulasTest.kt`, `SafeMoneyMathTest.kt`, `BalanceAggregationTest.kt`, `Tier2FinancialFormulaBoundaryE2ETest.kt` | Account type dispatch, safe arithmetic, currency aggregation and blocked unapproved formulas have tests. | Formula cases intentionally remain blocked pending business decisions; test assertions still need execution. |
| Deposit / withdrawal | `TransactionServiceTest.kt`, `Tier1TransactionCoreE2ETest.kt`, `Tier2NegativeAmountBoundaryE2ETest.kt`, `Tier2MonetaryOverflowBoundaryE2ETest.kt` | Success, invalid amount, insufficient funds, overflow and ledger assertions are represented. | Runtime test evidence unavailable. Injected database write failures should be verified for balance and ledger rollback. |
| Internal transfer | `TransactionRepositoryTest.kt`, `TransactionServiceTest.kt`, `Tier1TransactionCoreE2ETest.kt`, `Tier2IdempotencyBoundaryE2ETest.kt`, `Tier2ConcurrencyBoundaryE2ETest.kt` | Success, duplicate key, conflicting payload, insufficient balance, overflow, concurrency and reconciliation scenarios are represented. | Runtime test evidence unavailable. Current service idempotency cache is process-local; process-death/restart replay behavior is not established by the inspected test inventory. |
| Interbank transfer | `DefaultSimulatorService` paths are covered by `Tier1SimulatorsE2ETest.kt` and simulator service/repository tests. | Happy path/auth and simulator behavior have named coverage. | Verify invalid bank/account, concurrent duplicate request, persistence across process restart, and database exceptions by running/augmenting tests. |
| Transaction authentication | `TransactionAuthenticationPolicyTest.kt`, `Tier1TransactionAuthPolicyE2ETest.kt`, `Tier2FaceBiometricBoundaryE2ETest.kt`, transaction service rejection cases | Decision outcomes and fail-closed policy boundaries are represented. | Real BiometricPrompt/face model/device flow is not proven by local service tests; no 2FA equivalence claim can be made while OQ-3.6 remains open. |
| Utility payment | `Tier1SimulatorsE2ETest.kt`, transaction and idempotency boundary E2E files | Bill payment and simulator state transitions have coverage by source names and assertions. | Pricing/fees/refunds remain blocked by OQ-3.7; inspect/execute DB failure, duplicate payment, and refund/cancel scenarios after policy decisions. |
| Booking workflows | No dedicated booking test files or booking domain entities are present in the inspected Kotlin inventory. | Missing from current implementation/test suite. | Requirement/implementation acceptance audit must classify flights, hotels, cinema, top-up and other booking categories against the original specification. |
| Chatbot | `ChatbotViewModelTest.kt`, `Tier1ChatbotE2ETest.kt` | Intent response and transcript behavior are covered at source level. | Actual Compose navigation/deep-link interaction is not tested. |
| Navigation routing | `AStarPathFinderTest.kt`, `DijkstraRouterTest.kt`, `GeoDistanceCalculatorTest.kt`, `RoadGraphLoaderTest.kt`, `BranchNavigationRepositoryTest.kt`, locator/routing tier tests, `BranchLocatorViewModelTest.kt`, `OfflineMapProjectionTest.kt` | Route finding, boundary conditions, asset loading and projection have broad unit/E2E source coverage. | No device/runtime location permission or rendered map interaction test is present. |
| Room / migration | DAO test files for all current entities, `AppDatabaseMigrationTest.kt`, `DatabaseSeederTest.kt` | CRUD, schema presence, and seed behavior have dedicated tests. | Runtime could not verify any; migration test inventory does not establish upgrade-path behavior unless old-schema migration fixtures are exercised. |
| UI integration | `ChatbotViewModelTest.kt`, `AccountDetailViewModelTest.kt`, `HomeViewModelCancellationTest.kt`, `BranchLocatorViewModelTest.kt` | Some state transitions and cancellation are covered. | No Compose screen tests for input validation, loading/empty/error, rotation/lifecycle, accessibility, or end-to-end customer/officer navigation. |

## Priority Gaps

1. **Execution evidence is absent in this environment.** Run `testDebugUnitTest` where AGP 9.5.1 and all declared dependencies can resolve; record actual totals and failures before any dependent QA task.
2. **No database exception/rollback fault-injection coverage was identified** for transaction writes. Existing Room `withTransaction` implementation and normal failure cases do not alone prove rollback after a mid-transaction write exception.
3. **No process-restart replay test was identified.** Transaction idempotency in `DefaultTransactionService` uses an in-memory cache; verify repository/database behavior across service recreation and process restart.
4. **No Compose instrumentation tests exist.** UI state ViewModel assertions are not screen acceptance evidence.
5. **Booking/product workflows are not represented** in the current inventory and must be reconciled with source requirements.
6. **Real device authentication and face capture/inference are unverified** by JVM tests; current service tests cannot prove platform integration, camera behavior, liveness, or 2FA compliance.

## Gate Recommendation

Task 4.1 remains **BLOCKED**, not PASS. Static coverage looks broad in money, role/session, policy, Room DAOs, navigation, and simulator domains, but execution is required by `.agent/work.md`; the repository currently has no UI instrumentation suite, and the identified gaps need to feed the next QA steps.
