# Repository Inventory and Architectural Gap Audit

**Phase**: Phase 0 — Repository Reconnaissance  
**Task**: Task 0.1 Repository Inventory & Architectural Gap Survey  
**Date**: 2026-11-19  
**Target Repository**: `/home/chocolat/Documents/Project/m.ebanking`  
**Evaluation Status**: Baseline Established — Read-Only Audit  

---

## 1. Executive Summary

This document establishes the repository inventory and architectural gap audit for the Android Mobile Banking application (`m.ebanking`) as mandated by Phase 0, Task 0.1 of the course project execution specification.

The target repository is a single-module Android project structured around Jetpack Compose and Retrofit. It currently represents an early-stage starter skeleton comprising exactly 9 Kotlin source files, configured to fetch mock account data over HTTP from `https://api.example.com/`. Crucially, none of the 8 Room database tables, DAO layers, authentication systems, eKYC face recognition engines, 2FA transaction pipelines, or offline simulators mandated by `.agent/work.md` and the course specification (`FinalProject_2026_2027.pdf`) have been implemented. Furthermore, seven critical architectural, financial, and security defects (cataloged as findings **C01** through **C07**) have been identified in the existing source files.

---

## 2. Technology Stack & Environment Configuration

The toolchain and runtime configuration were audited directly from repository configuration files:

| Dimension | Specification | Configuration Source |
|---|---|---|
| **Build Tool** | Gradle 9.6.0 | `gradle/wrapper/gradle-wrapper.properties:3` |
| **Android Gradle Plugin** | AGP 9.5.1 | `gradle/libs.versions.toml:2`, `build.gradle.kts:2` |
| **Kotlin Language** | Kotlin 2.4.21 | `gradle/libs.versions.toml:3`, `build.gradle.kts:3` |
| **Compose Compiler Plugin** | `org.jetbrains.kotlin.plugin.compose:2.4.21` | `gradle/libs.versions.toml:35` |
| **Compile SDK** | API 37 (Android 16 preview) | `app/build.gradle.kts:8` |
| **Target SDK** | API 37 | `app/build.gradle.kts:13` |
| **Minimum SDK** | API 24 (Android 7.0 Nougat) | `app/build.gradle.kts:12` |
| **Java Compatibility** | Java 17 (`JavaVersion.VERSION_17`) | `app/build.gradle.kts:35-36` |
| **UI Framework** | Jetpack Compose (BOM `2026.09.00`) | `gradle/libs.versions.toml:7,19-24`, `app/build.gradle.kts:40,58-63` |
| **Design System** | Material 3 (`androidx.compose.material3:1.4.0`) | `gradle/libs.versions.toml:23`, `app/build.gradle.kts:62` |
| **Networking Client** | OkHttp 5.5.1 & Retrofit 3.0.0 | `gradle/libs.versions.toml:8-10`, `app/build.gradle.kts:65-70` |
| **JSON Serialization** | Gson 2.13.2 | `gradle/libs.versions.toml:10,28-29`, `app/build.gradle.kts:67,70` |
| **Local Database** | **ABSENT** (Zero Room/SQLite dependencies) | `gradle/libs.versions.toml`, `app/build.gradle.kts` |
| **Test Framework** | **ABSENT** (Zero test dependencies, no test dirs) | `gradle/libs.versions.toml`, `app/build.gradle.kts` |

---

## 3. Repository Directory and File Inventory

### 3.1 Root Configuration Files
- `build.gradle.kts` (4 lines): Declares root plugins `android.application` and `kotlin.compose` with `apply false`.
- `settings.gradle.kts` (18 lines): Configures plugin management, dependency resolution repositories (`google()`, `mavenCentral()`), and includes `:app`.
- `gradle.properties` (9 lines): JVM heap size (`-Xmx2048m`), configuration caching (`org.gradle.configuration-cache=true`), parallel builds, and AndroidX enablement.
- `gradle/wrapper/gradle-wrapper.properties` (9 lines): Gradle 9.6.0 binary distribution.
- `gradle/libs.versions.toml` (35 lines): Version catalog for AGP, Kotlin, AndroidX, Compose BOM, Retrofit, and OkHttp.

### 3.2 Application Module (`app/`) Files
The `:app` module contains exactly 9 Kotlin source files organized in clean packages:

```
app/src/main/java/com/mebanking/app/
├── MainActivity.kt                                (20 lines) — Activity entry point
├── ui/
│   ├── MEbankingApp.kt                            (22 lines) — Root composable host
│   ├── home/
│   │   ├── HomeScreen.kt                          (249 lines) — Accounts UI, Balance card, Error/Loading states
│   │   ├── HomeUiState.kt                         (15 lines) — UI state model with computed total balance
│   │   └── HomeViewModel.kt                       (42 lines) — ViewModel orchestrating account loading
│   └── theme/
│       ├── Color.kt                               (22 lines) — Material 3 palette tokens
│       ├── Theme.kt                               (78 lines) — Application Theme setup
│       └── Type.kt                                (36 lines) — Typography system
└── data/
    ├── model/
    │   └── Account.kt                             (19 lines) — Account data entity (Retrofit DTO)
    ├── remote/
    │   ├── BankingApi.kt                          (17 lines) — Retrofit API endpoint definitions
    │   └── NetworkModule.kt                       (51 lines) — OkHttp and Retrofit singleton provider
    └── repository/
        └── AccountRepository.kt                   (17 lines) — Repository delegating to remote API
```

### 3.3 Resource Files (`app/src/main/res/`)
- `drawable/`: Vector drawables (`ic_launcher_background.xml`, `ic_launcher_foreground.xml`).
- `mipmap-*/`: App launcher icons (standard and round).
- `values/`:
  - `strings.xml`: Application name (`m.ebanking`), UI labels for accounts, balance, retry, error.
  - `colors.xml`: Legacy XML color palette placeholders.
  - `themes.xml`: XML theme (`Theme.MEbanking`) inheriting from `android:Theme.Material.Light.NoActionBar`.
- `xml/`:
  - `backup_rules.xml`: Legacy Android backup rules.
  - `data_extraction_rules.xml`: Android 12+ data extraction rules (excludes `sharedpref/auth.xml`).

---

## 4. Architectural Gap Audit: Deep-Dive on Findings C01 to C07

### 4.1 C01 — Monetary Data Type Precision Defect
- **Target Files & Lines**:
  - `app/src/main/java/com/mebanking/app/data/model/Account.kt:15-18`
  - `app/src/main/java/com/mebanking/app/ui/home/HomeUiState.kt:10-11`
  - `app/src/main/java/com/mebanking/app/ui/home/HomeScreen.kt:219-220`
- **Code Snippet**:
  ```kotlin
  // Account.kt:15-18
  @SerializedName("balance")
  val balance: Double,
  @SerializedName("currency")
  val currency: String,

  // HomeUiState.kt:10-11
  val totalBalance: Double
      get() = accounts.sumOf { it.balance }

  // HomeScreen.kt:219-220
  private fun formatAmount(amount: Double, currency: String): String =
      String.format(Locale.US, "%,.2f %s", amount, currency)
  ```
- **Architectural & Financial Defect**:
  1. `balance` and `totalBalance` use IEEE 754 64-bit binary floating-point numbers (`Double`). Binary floating-point representation cannot represent base-10 fractional quantities precisely (e.g., `0.1 + 0.2 != 0.3`). In financial ledgers, recurring calculations accumulate rounding drift, corrupting balance integrity and balance transfers.
  2. Rule A2.10 in `.agent/work.md` explicitly mandates: *"Không sử dụng `Double` hoặc `Float` để lưu trữ tiền. Dùng `Long` theo đơn vị VND."*
  3. UI formatting in `HomeScreen.kt` uses `Locale.US` with `%,.2f` (cents). The Vietnamese Dong (`VND`) has no minor currency units (cents); amounts must be whole integers formatted per Vietnamese locale (`#,##0 ₫` or `1.000.000 VND`).
  4. Currency formatting logic is duplicated and embedded privately inside UI composable files rather than encapsulated in a domain-level monetary formatting utility.
- **Required Remedy**:
  - Refactor all account balances, transaction amounts, and totals to `Long` representing integer units in VND.
  - Replace floating-point summation with integer `sumOf { it.balance }`.
  - Introduce a centralized `CurrencyFormatter` utility using `Locale("vi", "VN")` and integer formatting.

---

### 4.2 C02 — Offline-First Architecture Failure
- **Target Files & Lines**:
  - `app/src/main/java/com/mebanking/app/data/repository/AccountRepository.kt:8-17`
  - `app/src/main/java/com/mebanking/app/ui/home/HomeViewModel.kt:24-30`
  - `app/src/main/AndroidManifest.xml:4`
- **Code Snippet**:
  ```kotlin
  // AccountRepository.kt:8-17
  /**
   * Single source of truth for account data. Add caching (Room) here later
   * without touching the UI layer.
   */
  class AccountRepository(
      private val api: BankingApi = NetworkModule.api,
  ) {
      suspend fun getAccounts(): List<Account> = api.getAccounts()
      suspend fun getAccount(id: String): Account = api.getAccount(id)
  }
  ```
- **Architectural Defect**:
  1. The application architecture establishes a direct network pipeline: `HomeScreen -> HomeViewModel -> AccountRepository -> BankingApi -> Retrofit -> OkHttp -> Remote Server`.
  2. Room / SQLite local storage is completely absent from the project.
  3. The repository comments explicitly acknowledge the deficiency (`"Add caching (Room) here later"`).
  4. When network connectivity is absent or `https://api.example.com/` is unreachable, `getAccounts()` throws an immediate network exception (`UnknownHostException` / `SocketTimeoutException`), leaving the UI completely blank with an error banner.
  5. This violates the core system requirement in `.agent/work.md` Section A1: *"Local-first, hoạt động không cần Internet. Không kết nối API ngân hàng, cổng thanh toán..."*
- **Required Remedy**:
  - Add Room database dependencies (`androidx.room:room-runtime`, `androidx.room:room-ktx`, and KSP compiler).
  - Implement Room entities, DAOs, and the database singleton.
  - Re-architect `AccountRepository` to serve data locally from Room as the single source of truth (SSOT), with pre-populated seed data for accounts and transactions.

---

### 4.3 C03 — Hardcoded Dependency Wiring & Inversion of Control Violation
- **Target Files & Lines**:
  - `app/src/main/java/com/mebanking/app/ui/home/HomeScreen.kt:43-46`
  - `app/src/main/java/com/mebanking/app/ui/home/HomeViewModel.kt:12-14`
  - `app/src/main/java/com/mebanking/app/data/repository/AccountRepository.kt:11-13`
  - `app/src/main/java/com/mebanking/app/data/remote/NetworkModule.kt:16, 50`
- **Code Snippet**:
  ```kotlin
  // HomeScreen.kt:43-46
  @Composable
  fun HomeScreen(
      modifier: Modifier = Modifier,
      viewModel: HomeViewModel = viewModel(),
  )

  // HomeViewModel.kt:12-14
  class HomeViewModel(
      private val repository: AccountRepository = AccountRepository(),
  ) : ViewModel()

  // AccountRepository.kt:11-13
  class AccountRepository(
      private val api: BankingApi = NetworkModule.api,
  )
  ```
- **Architectural Defect**:
  1. Default constructor arguments instantiate concrete dependencies directly up the dependency tree: `HomeScreen` instantiates `HomeViewModel`, which instantiates `AccountRepository`, which references the singleton `NetworkModule.api`.
  2. There is no Dependency Injection framework (Hilt/Koin) or structured Service Locator (`AppContainer`).
  3. Automated unit testing is obstructed: `HomeViewModel` cannot be tested with fake/mock repositories without constructor overriding, and Compose preview composables cannot instantiate fake state cleanly.
  4. Repository interfaces are missing; components depend on concrete implementations instead of abstractions (violating Dependency Inversion Principle).
- **Required Remedy**:
  - Define interfaces for repositories (`IAccountRepository`).
  - Introduce an `AppContainer` / manual DI factory pattern in `MEbankingApplication` to supply dependencies.
  - Provide a custom `ViewModelProvider.Factory` for `HomeViewModel` and all subsequent ViewModels.

---

### 4.4 C04 — Coroutine Cancellation Exception Swallowing
- **Target Files & Lines**:
  - `app/src/main/java/com/mebanking/app/ui/home/HomeViewModel.kt:24-41`
- **Code Snippet**:
  ```kotlin
  viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      runCatching { repository.getAccounts() }
          .onSuccess { accounts ->
              _uiState.update {
                  it.copy(isLoading = false, accounts = accounts, errorMessage = null)
              }
          }
          .onFailure { throwable ->
              _uiState.update {
                  it.copy(
                      isLoading = false,
                      errorMessage = throwable.message ?: "Unable to load accounts",
                  )
              }
          }
  }
  ```
- **Architectural Defect**:
  1. `runCatching` catches all instances of `java.lang.Throwable`. In Kotlin Coroutines, cooperative cancellation relies on throwing `kotlinx.coroutines.CancellationException`.
  2. When `viewModelScope` is cancelled (e.g. host screen dismissed, ViewModel cleared), the ongoing coroutine emits `CancellationException`.
  3. `runCatching` catches this `CancellationException` as a failure and dispatches it into `.onFailure`.
  4. This updates `_uiState` on a cancelled coroutine job, setting the error message to `"StandaloneCoroutine was cancelled"`.
  5. The coroutine fails to cancel cooperatively, violating Kotlin Structured Concurrency invariants.
- **Required Remedy**:
  - Replace `runCatching` with idiomatic `try-catch` blocks that explicitly re-throw `CancellationException`:
    ```kotlin
    try {
        val accounts = repository.getAccounts()
        _uiState.update { it.copy(isLoading = false, accounts = accounts) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        _uiState.update { it.copy(isLoading = false, errorMessage = ...) }
    }
    ```

---

### 4.5 C05 — Unstructured Error Presentation to End Users
- **Target Files & Lines**:
  - `app/src/main/java/com/mebanking/app/ui/home/HomeViewModel.kt:34-39`
  - `app/src/main/java/com/mebanking/app/ui/home/HomeUiState.kt:8`
  - `app/src/main/java/com/mebanking/app/ui/home/HomeScreen.kt:112-116`
- **Code Snippet**:
  ```kotlin
  // HomeViewModel.kt:34-39
  .onFailure { throwable ->
      _uiState.update {
          it.copy(
              isLoading = false,
              errorMessage = throwable.message ?: "Unable to load accounts",
          )
      }
  }

  // HomeScreen.kt:112-116
  Text(
      text = message,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.error,
  )
  ```
- **Architectural Defect**:
  1. The ViewModel extracts `throwable.message` and exposes it verbatim to the UI.
  2. In failure scenarios (e.g. DNS failure, HTTP 502, JSON serialization error), users see raw technical stack descriptions such as `Unable to resolve host "api.example.com": No address associated with hostname`.
  3. There is no domain-level error model (`sealed interface UiError` or string resource IDs).
  4. Error presentation is not localized, nor is it actionable (e.g. distinguishing network offline from authorization timeout or invalid account state).
- **Required Remedy**:
  - Implement a typed error hierarchy (e.g. `sealed interface UiMessage` or string resource wrappers).
  - Map repository and data errors into user-friendly localized strings in `HomeViewModel`.

---

### 4.6 C06 — Invalid Multi-Currency Total Balance Aggregation
- **Target Files & Lines**:
  - `app/src/main/java/com/mebanking/app/ui/home/HomeUiState.kt:10-15`
  - `app/src/main/java/com/mebanking/app/ui/home/HomeScreen.kt:182`
- **Code Snippet**:
  ```kotlin
  // HomeUiState.kt:10-15
  val totalBalance: Double
      get() = accounts.sumOf { it.balance }

  val currency: String
      get() = accounts.firstOrNull()?.currency ?: "EUR"

  // HomeScreen.kt:182
  text = formatAmount(uiState.totalBalance, uiState.currency),
  ```
- **Architectural Defect**:
  1. The getter `totalBalance` computes the arithmetic sum of `it.balance` regardless of the currency of individual accounts. If Account 1 is in `VND` (e.g., 50,000,000 VND) and Account 2 is in `USD` (e.g., 500 USD), the result is `50000500`, which is economically meaningless.
  2. The getter `currency` arbitrarily selects the first account's currency (`accounts.firstOrNull()?.currency`) and labels the total sum with it.
  3. The fallback currency is hardcoded to `"EUR"`, contradicting the Vietnamese banking context (`VND`).
  4. Significant business aggregation logic is embedded inside a UI State data class getter rather than being computed cleanly in a domain use case or repository.
- **Required Remedy**:
  - Enforce standard account base currency as `VND` across all accounts.
  - If multi-currency accounts are supported in future phases, compute totals through explicit exchange rate conversion use cases.
  - Set default fallback currency to `VND`.

---

### 4.7 C07 — Sensitive Financial Data Logging & Unrestricted Device Backup
- **Target Files & Lines**:
  - `app/src/main/java/com/mebanking/app/data/remote/NetworkModule.kt:18-24`
  - `app/src/main/AndroidManifest.xml:6-9`
  - `app/src/main/res/xml/backup_rules.xml:1-4`
  - `app/src/main/res/xml/data_extraction_rules.xml:1-9`
- **Code Snippet**:
  ```kotlin
  // NetworkModule.kt:18-24
  private val loggingInterceptor = HttpLoggingInterceptor().apply {
      level = if (BuildConfig.DEBUG) {
          HttpLoggingInterceptor.Level.BODY
      } else {
          HttpLoggingInterceptor.Level.NONE
      }
  }

  // AndroidManifest.xml:6-9
  <application
      android:allowBackup="true"
      android:dataExtractionRules="@xml/data_extraction_rules"
      android:fullBackupContent="@xml/backup_rules"
  ```
- **Architectural & Security Defect**:
  1. `HttpLoggingInterceptor.Level.BODY` logs complete HTTP request headers and response bodies into the Android system Logcat buffer during debug runs. In a banking application, this dumps plaintext account numbers, balances, credentials, and PII into shared log buffers, creating a high-severity data leakage vulnerability.
  2. `android:allowBackup="true"` allows extraction of application private storage via `adb backup` and Google Cloud automated device backups.
  3. The exclusion rules in `backup_rules.xml` and `data_extraction_rules.xml` only exclude `sharedpref/auth.xml`. Local SQLite/Room database files (`*.db`), cached transaction histories, and user biometric templates are left completely exposed to physical/ADB backup extraction.
- **Required Remedy**:
  - Restrict HTTP logging to `Level.BASIC` or `Level.HEADERS` with header redaction, or eliminate remote HTTP logging in favor of local sanitized logging.
  - Set `android:allowBackup="false"` in `AndroidManifest.xml` or configure comprehensive backup exclusions covering databases, files, and preferences for a secure financial app.

---

## 5. Missing Infrastructure & Tooling Summary

| Missing Component | Description | Impact | Target Phase |
|---|---|---|---|
| **Room Database & KSP** | Room runtime, KTX, compiler, KSP plugin | Application cannot persist data or function offline | Phase 1 & 2 |
| **Navigation Compose** | `androidx.navigation:navigation-compose` | App is locked to single screen (`HomeScreen`) | Phase 3 |
| **Unit Testing Libraries** | JUnit 4, MockK, Coroutines-Test | No automated verification of ViewModels or Repositories | Phase 2, 3, 4 |
| **UI Testing Libraries** | Compose Test JUnit4, Espresso | No automated verification of UI interaction flows | Phase 3, 4 |
| **Biometric & Face ML** | `androidx.biometric`, MediaPipe, MobileFaceNet | Cannot fulfill eKYC or 2FA high-value transaction mandates | Phase 4 |
| **Security Encryption** | `androidx.security:security-crypto` | Local tokens or sensitive keys stored in plaintext | Phase 3 |

---

## 6. Gate 0 Evaluation

- **Codebase Integrity**: Preserved (Zero modifications made outside documentation directory).
- **Toolchain Operational Status**: Healthy (Gradle 9.6.0, AGP 9.5.1, Kotlin 2.4.21).
- **Architectural Baseline**: Fully cataloged. Gaps C01–C07 are documented with exact lines and remediation paths.
- **Readiness**: Ready to proceed with Project Structure documentation and Implementation Status audit.
