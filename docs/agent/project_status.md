# Project Status & Phase Gate Tracking

**Project**: Android Mobile Banking Application (`m.ebanking`)  
**Specification Reference**: `.agent/work.md`, `FinalProject_2026_2027.pdf`, `Sơ đồ thiết kế app ngân hàng.md`  
**Last Updated**: 2026-11-19  

---

## 1. Project Phase Overview

| Phase | Description | Deliverables / Focus | Status |
|---|---|---|---|
| **Phase 0** | **Repository Reconnaissance** | Audit repository, tech stack, baseline build/tests, dependency classification, catalog C01–C07 | **COMPLETED (Gate 0 Passed)** |
| **Phase 1** | **Domain Modeling & DB Schema** | Room 8-table relational schema, Entity design, Long VND currency models, simulator contracts | **PLANNED** |
| **Phase 2** | **Persistence & Room Implementation** | Room Database, DAOs, TypeConverters, seed data population, DI container (`AppContainer`) | **PLANNED** |
| **Phase 3** | **Auth, Navigation & Accounts** | Navigation Compose, Login screen, Customer/Officer RBAC, Home & Account screen Room SSOT | **PLANNED** |
| **Phase 4** | **Transactions, Simulators & 2FA** | Internal/Interbank transfers, ExternalBankSimulator, 2FA OTP, Face Recognition, VNPay/Stripe | **PLANNED** |
| **Phase 5** | **Utilities, Offline Map & Chatbot** | Utility bill payments, ticket bookings, offline branch map with walking routing, AI Chatbot | **PLANNED** |
| **Phase 6** | **Hardening, Testing & Final Gate** | Automated unit/UI test coverage, lint fixes, release Proguard build, course demo verification | **PLANNED** |

---

## 2. Gate 0 Assessment

**Gate 0 Criteria**:
1. All application source code, build configurations, and manifests outside `docs/` must remain untouched (`git status --porcelain app/` clean).
2. Canonical Gradle build commands (`assembleDebug`, `assembleRelease`) compile successfully without code changes.
3. Baseline test execution results recorded verbatim with accurate root-cause documentation (0 tests run / NO-SOURCE).
4. All architectural, monetary, and security gaps (C01 through C07) cataloged with line-level evidence and remediation plans.
5. All 6 Phase 0 documentation deliverables produced in `docs/agent/phase_00/`.

### 2.1 Gate 0 Verification Checklist

| Criterion | Target | Verification Evidence | Status |
|---|---|---|---|
| **Source Integrity** | 0 files modified outside `docs/` | `git status --porcelain app/` returns empty (clean) | **PASS** |
| **Debug Build** | `./gradlew assembleDebug` | Exit code 0, APK generated (13,049,216 bytes) | **PASS** |
| **Release Build** | `./gradlew assembleRelease` | Exit code 0, APK generated (8,918,824 bytes) | **PASS** |
| **Unit Test Baseline** | `./gradlew testDebugUnitTest` | Exit code 0, 0 tests discovered (NO-SOURCE), documented | **PASS** |
| **Static Analysis** | `./gradlew lintDebug` | Exit code 0, 0 errors, 4 warnings cataloged | **PASS** |
| **Repository Inventory** | `docs/agent/phase_00/repository_inventory.md` | Exists, details tech stack, catalog of gaps C01–C07 | **PASS** |
| **Project Structure** | `docs/agent/phase_00/project_structure.md` | Exists, maps directory tree, packages, layers | **PASS** |
| **Implementation Status**| `docs/agent/phase_00/implementation_status.md` | Exists, feature audit matrix F01–F10, placeholders | **PASS** |
| **Build Report** | `docs/agent/phase_00/baseline_build_report.md` | Exists, verbatim command outputs, exit codes, artifacts | **PASS** |
| **Test Report** | `docs/agent/phase_00/baseline_test_report.md` | Exists, test counts, root cause of 0-test baseline | **PASS** |
| **Offline & Security** | `docs/agent/phase_00/offline_dependency_audit.md` | Exists, 3-tier taxonomy, C02 & C07 in-depth analysis | **PASS** |

### 2.2 Gate 0 Decision
**Decision: PASS**  
The reconnaissance phase has established the objective baseline for the repository. All known defects, architectural discrepancies, and missing components are documented with exact line citations and clear transition plans. The repository is ready to enter **Phase 1: Domain Modeling & Database Schema**.

---

## 3. Catalog of Known Architectural Gaps (C01–C07)

The following gaps were cataloged during Phase 0 and will be resolved across subsequent phases:

| ID | Title | Summary | Target Phase |
|---|---|---|---|
| **C01** | Monetary Data Type | IEEE 754 `Double` used in `Account.balance` and UI formatting; must use integer `Long` in VND (Rule A2.10). | Phase 1 & 2 |
| **C02** | Offline-First Gap | Direct network-only flow to `api.example.com` without local Room/SQLite persistence. | Phase 1 & 2 |
| **C03** | Dependency Wiring | Hardcoded constructor instantiation in ViewModels; missing DI container (`AppContainer`). | Phase 2 & 3 |
| **C04** | Coroutine Cancellation | `runCatching` in `HomeViewModel.refresh()` swallows `CancellationException`. | Phase 3 |
| **C05** | Error Presentation | Low-level technical exceptions exposed raw to UI without domain error mapping. | Phase 3 |
| **C06** | Total Balance Aggregation | Direct nominal summation across mixed currencies and arbitrary fallback to `"EUR"`. | Phase 1 & 3 |
| **C07** | Security & Privacy Exposure | Full HTTP body logging in debug Logcat; `android:allowBackup="true"` with incomplete exclusions. | Phase 3 & 4 |
