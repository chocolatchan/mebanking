# Repository Inventory

- Task: 0.1 - Repository Inventory
- Repository: `chocolatchan/mebanking`
- Inspected source ref: `master`
- Inspected source commit: `efc15e138cb0bc4f3420e8b2caedb29fdfb36299` (2026-11-19)
- Documentation branch: `agent/phase-00-reconnaissance`
- Scope: read-only inspection of tracked repository source via GitHub.
- Task status: **CONDITIONAL PASS**
- Gate contribution: source inventory completed; local working-tree integrity and one external reference remain unverified.

## 1. Repository and technology overview

| Item | Observed |
|---|---|
| Android modules | Single application module, `:app` |
| Language | Kotlin |
| Build scripts | Gradle Kotlin DSL |
| Wrapper | Gradle 9.6.0 |
| Android Gradle Plugin | 9.5.1 |
| Kotlin / Compose compiler plugin | 2.4.21 |
| minSdk / compileSdk / targetSdk | 24 / 37 / 37 |
| Java compatibility | 17 |
| Namespace / applicationId | `com.mebanking.app` |
| UI | Jetpack Compose, Material 3 |
| Architecture present | Preliminary UI → ViewModel → Repository → remote API |
| Persistence | No Room/SQLite implementation or dependency found |
| DI | Manual constructor defaults and singleton object; no DI framework |
| Network | Retrofit 3.0.0, OkHttp 5.5.1, Gson 2.13.2 |
| Tests | No tracked `app/src/test` or `app/src/androidTest` sources in inspected commit |

The project is an early single-screen account-display skeleton, not an implemented mobile-banking system.

## 2. Module and component inventory

- `:app`: only Android application module.
- `MainActivity.kt`: application activity and Compose entry point.
- `ui/MEbankingApp.kt`: root Compose content; directly renders Home.
- `ui/home/HomeScreen.kt`: account list, balance card, loading/error/empty UI and amount formatting.
- `ui/home/HomeViewModel.kt`: refresh coroutine and StateFlow UI state.
- `ui/home/HomeUiState.kt`: accounts, loading/error state, aggregate balance and selected currency.
- `data/model/Account.kt`: remote account DTO.
- `data/repository/AccountRepository.kt`: delegates directly to BankingApi.
- `data/remote/BankingApi.kt`: GET accounts and GET account by ID.
- `data/remote/NetworkModule.kt`: manually constructs Retrofit/OkHttp API client.
- `res/xml/backup_rules.xml`, `data_extraction_rules.xml`: backup exclusions.
- No domain/use-case, local data, DAO, navigation, or DI package was found.

## 3. Manifest and platform declarations

`app/src/main/AndroidManifest.xml` declares `android.permission.INTERNET` and one exported launcher activity. It enables `android:allowBackup="true"` and references backup/data-extraction rules.

No camera, location, biometric, notification, or other runtime permission/integration was found in the manifest snapshot. This is absence in the inspected source, not a claim about future capabilities.

## 4. Current data flow

`HomeScreen → HomeViewModel → AccountRepository → BankingApi → Retrofit → OkHttp`

- HomeViewModel calls refresh during initialization.
- AccountRepository.getAccounts() delegates directly to the API.
- No local cache, Room DAO, SQLite database, or offline fallback is present.
- API base URL is `https://api.example.com/`, a placeholder.
- The current home account-loading path therefore depends on a successful remote endpoint and network access.

## 5. Explicit architectural findings C01-C07

### C01 - Monetary data type and currency formatting
**Severity: HIGH**

Evidence:
- `data/model/Account.kt`: `Account.balance: Double`.
- `ui/home/HomeUiState.kt`: `totalBalance: Double`.
- `.agent/work.md` A2.10 explicitly prohibits Double/Float for money and requires `Long` in VND units.
- `ui/home/HomeScreen.kt` defines `formatAmount(amount: Double, currency: String)` locally and uses it for both aggregate and per-account amounts.

Assessment:
- Current monetary representation violates A2.10 for VND.
- Formatting is currently concentrated in a private UI helper rather than a shared currency/amount presentation policy. It uses `Locale.US` and two decimal places, which is not yet a VND-specific presentation rule.
- The design reference's ERD/class diagram also models balance/amount using `real` / `double`; this is inconsistent with A2.10 and should be reconciled during approved design work.
- No migration or code change is made in Phase 0.

### C02 - Offline-first architecture gap
**Severity: HIGH**

Evidence:
- The current call chain is remote-only: `HomeViewModel → AccountRepository → BankingApi → Retrofit → OkHttp`.
- No Room/SQLite dependency, entity, DAO, database, migration, or local source exists.
- The project design reference describes `UI → ViewModel → Repository → DAO → SQLite` and local simulators.

Assessment:
- Repository is not offline-first and has no local source of truth.
- A local-first repository backed by Room/SQLite is absent. The placeholder remote API is still invoked by HomeViewModel.
- This is a direct mismatch with the project's stated offline/local-data architecture.

### C03 - Dependency wiring and testability
**Severity: MEDIUM**

Evidence:
- `HomeViewModel` constructor defaults to `AccountRepository()`.
- `AccountRepository` constructor defaults to `NetworkModule.api`.
- `NetworkModule` is a manually wired singleton object that constructs Retrofit and OkHttp.

Assessment:
- Constructor parameters provide limited substitution points, but production defaults instantiate the concrete repository and bind it to the global network module.
- There is no explicit application-level composition root, DI container, or repository factory.
- This makes controlled test fakes and future local/remote data-source selection less explicit. DI/factory abstraction is a design consideration, not an automatic requirement to add a framework.

### C04 - Coroutine cancellation
**Severity: MEDIUM**

Evidence:
- `HomeViewModel.refresh()` launches in `viewModelScope` and wraps `repository.getAccounts()` in Kotlin `runCatching`.

Assessment:
- `runCatching` catches all `Throwable`, including `CancellationException`.
- A cancelled ViewModel coroutine may be converted into ordinary error UI state rather than allowing structured cancellation to propagate.
- Cancellation should be rethrown or handled explicitly in a later approved change.

### C05 - Error presentation
**Severity: MEDIUM**

Evidence:
- HomeViewModel assigns `throwable.message` to `HomeUiState.errorMessage`.
- HomeScreen renders that string directly.

Assessment:
- There is no structured domain/network error mapping demonstrated.
- Raw exception messages can be unstable, technical, or disclose implementation/network details.
- UI-safe error categories and localized messages are not established.

### C06 - Total balance aggregation and currency correctness
**Severity: HIGH**

Evidence:
- `HomeUiState.totalBalance` calculates `accounts.sumOf { it.balance }`.
- `HomeUiState.currency` chooses the first account's currency, defaulting to EUR.
- HomeScreen formats the sum using that single currency value.

Assessment:
- Balances are summed without checking currency equality or applying an exchange-rate conversion.
- If accounts use different currencies, the displayed total is mathematically invalid and labels the mixed sum with the first account's currency.
- No currency-conversion policy or same-currency invariant is present in the inspected code.
- The UI currently displays the aggregate as a single-currency total.

### C07 - Security and privacy exposure
**Severity: MEDIUM (pre-production); HIGH once real sensitive data is used**

Evidence:
- `NetworkModule.kt` sets `HttpLoggingInterceptor.Level.BODY` for debug builds.
- Manifest sets `android:allowBackup="true"`.
- Backup rules exclude only `sharedpref/auth.xml` for cloud backup and device transfer; they do not establish a comprehensive policy for future banking database/files.
- The current app has no local banking database or real authentication implementation.

Assessment:
- BODY logging can expose request/response payloads, including account/customer data when real endpoints and data are introduced.
- Backup is enabled. Existing narrow auth preference exclusions do not prove future Room databases, files, or other sensitive records are excluded.
- No current evidence of actual customer data leakage is claimed; the risk is prospective and must be resolved before sensitive data is introduced.

## 6. Functional implementation snapshot

Implemented narrowly: Android entry point, Compose theme, Home account-display UI, state flow, account DTO, remote GET API contract and HTTP client setup.

Missing in inspected source: authentication/authorization, Customer/Officer roles, eKYC, account-product rules, transactions, local database, transaction authentication, utility simulators, interbank simulator, offline map/routing, chatbot and tests. See `implementation_status.md`.

## 7. Reference documents and limitations

- `.agent/work.md` was read from the repository. A2.10 specifies Long in VND.
- `Sơ đồ thiết kế app ngân hàng.md` was available in the ChatGPT file library and inspected. It describes two actors (Customer/Officer), MVVM with Room/SQLite, local simulators, eight SQLite tables, OTP-oriented sequence diagrams, Camera + ML Kit and Google Maps in its solution diagram. These are design descriptions, not evidence of implementation.
- The exact file `FinalProject_2026_2027.pdf` from the user's local Downloads directory was not available in this environment. A separately generated BRD/Use Case document is not treated as a substitute for the original assignment PDF.
- The GitHub source snapshot does not expose local uncommitted files or the user's local SDK environment.

## 8. Integrity and gate result

No application source, Gradle build file, dependency declaration, manifest, database schema, or configuration was changed by this reconnaissance update. Documentation is written only to `docs/agent/` on the dedicated documentation branch.

**Task 0.1: CONDITIONAL PASS.** Source inventory and C01-C07 review completed. Local working-tree cleanliness and the exact assignment PDF remain unverified.
