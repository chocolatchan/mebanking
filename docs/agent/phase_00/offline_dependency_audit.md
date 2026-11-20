# Dependency & Offline Architecture Audit

**Phase**: Phase 0 — Repository Reconnaissance  
**Task**: Task 0.3 Dependency and Offline Architecture Audit  
**Date**: 2026-11-19  
**Target Repository**: `/home/chocolat/Documents/Project/m.ebanking`  
**Evaluation Status**: Architectural Audit Completed  

---

## 1. Executive Summary

This audit evaluates all declared libraries, Android framework dependencies, manifest permissions, and network components against the project's **offline-first local-first mandate** as formulated in `.agent/work.md` Section A1 and Task 0.3.

The current application relies exclusively on a remote HTTP backend (`https://api.example.com/`) over Retrofit and OkHttp. It possesses zero local database persistence (Room/SQLite), zero local seed data, and zero offline fallback logic. Consequently, it fails finding **C02 (Offline-First Gap)**. In addition, logging complete HTTP bodies in debug builds and enabling device backups with inadequate exclusion rules violate secure banking standards, confirming finding **C07 (Security & Privacy Exposure)**.

---

## 2. Three-Tier Dependency Classification

All dependencies currently resolved in the project (via `app/build.gradle.kts` and `./gradlew :app:dependencies`) are classified into three structural tiers:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        TIER 1: ANDROID OS APIS                         │
│  androidx.core:core-ktx:1.18.0                                         │
│  androidx.lifecycle:lifecycle-runtime-ktx:2.11.0                       │
│  androidx.lifecycle:lifecycle-runtime-compose:2.11.0                   │
│  androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0                 │
│  androidx.activity:activity-compose:1.13.0                            │
│  androidx.compose.* (BOM 2026.09.00: ui, graphics, material3, tooling) │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│                    TIER 2: LOCAL ON-DEVICE LIBRARIES                   │
│  com.google.code.gson:gson:2.13.2                                      │
│  com.squareup.retrofit2:converter-gson:3.0.0 (Local JSON converter)   │
│  [MISSING: androidx.room:room-* (Local SQLite persistence)]            │
│  [MISSING: androidx.security:security-crypto (Encrypted local storage)]│
│  [MISSING: MediaPipe Face Detection + MobileFaceNet (On-device ML)]    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│                  TIER 3: EXTERNAL NETWORK SERVICES                     │
│  com.squareup.okhttp3:okhttp:5.5.1                                     │
│  com.squareup.okhttp3:logging-interceptor:5.5.1                        │
│  com.squareup.retrofit2:retrofit:3.0.0                                 │
│  Target Endpoint: BuildConfig.API_BASE_URL ("https://api.example.com/")│
└────────────────────────────────────────────────────────────────────────┘
```

### Detailed Classification Matrix

| Dependency Coordinates | Category | Runtime Internet Needed? | Replaceable Locally? | Mandate Evaluation |
|---|---|---|---|---|
| `androidx.core:core-ktx:1.18.0` | Tier 1: Android OS APIs | **No** | N/A | Essential Android framework support. |
| `androidx.lifecycle:lifecycle-runtime-ktx:2.11.0` | Tier 1: Android OS APIs | **No** | N/A | Jetpack Lifecycle management. |
| `androidx.lifecycle:lifecycle-runtime-compose:2.11.0` | Tier 1: Android OS APIs | **No** | N/A | Compose lifecycle collection helpers. |
| `androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0` | Tier 1: Android OS APIs | **No** | N/A | Compose ViewModel scoping. |
| `androidx.activity:activity-compose:1.13.0` | Tier 1: Android OS APIs | **No** | N/A | Compose activity host and edge-to-edge. |
| `androidx.compose.ui:ui:1.12.1` | Tier 1: Android OS APIs | **No** | N/A | Jetpack Compose core UI rendering. |
| `androidx.compose.ui:ui-graphics:1.12.1` | Tier 1: Android OS APIs | **No** | N/A | Compose vector & color primitives. |
| `androidx.compose.ui:ui-tooling-preview:1.12.1` | Tier 1: Android OS APIs | **No** | N/A | Design-time `@Preview` annotations. |
| `androidx.compose.material3:material3:1.4.0` | Tier 1: Android OS APIs | **No** | N/A | Material Design 3 design system. |
| `androidx.compose.ui:ui-tooling:1.12.1` | Tier 1: Android OS APIs | **No** | N/A | Debug layout inspection tools. |
| `com.google.code.gson:gson:2.13.2` | Tier 2: Local on-device | **No** | N/A | JSON parser running entirely in-memory. |
| `com.squareup.retrofit2:converter-gson:3.0.0` | Tier 2: Local on-device | **No** | N/A | In-memory stream converter. |
| `com.squareup.okhttp3:okhttp:5.5.1` | Tier 3: External Network | **Yes** | **Yes** (Replace with Room/Simulators) | Violates offline-first requirement when used as primary data source. |
| `com.squareup.okhttp3:logging-interceptor:5.5.1` | Tier 3: External Network | **Yes** | **Yes** (Remove or sanitize) | Violates privacy standards when logging HTTP bodies. |
| `com.squareup.retrofit2:retrofit:3.0.0` | Tier 3: External Network | **Yes** | **Yes** (Replace with Room DAOs) | Redundant if all banking APIs are local/simulated. |

---

## 3. Finding C02: In-Depth Offline-First Failure Analysis

### 3.1 Network-Only Invocation Chain
The application's runtime data path is strictly synchronous with external network availability:
1. `MainActivity` launches and inflates `MEbankingApp`.
2. `HomeScreen` instantiates `HomeViewModel`.
3. `HomeViewModel.init` immediately launches a coroutine calling `refresh()`:
   ```kotlin
   // HomeViewModel.kt:23-28
   fun refresh() {
       viewModelScope.launch {
           _uiState.update { it.copy(isLoading = true, errorMessage = null) }
           runCatching { repository.getAccounts() }
   ```
4. `AccountRepository.getAccounts()` delegates directly to `api.getAccounts()`:
   ```kotlin
   // AccountRepository.kt:14
   suspend fun getAccounts(): List<Account> = api.getAccounts()
   ```
5. `BankingApi.getAccounts()` issues an HTTP GET request to `https://api.example.com/accounts` via OkHttp:
   ```kotlin
   // BankingApi.kt:12
   @GET("accounts")
   suspend fun getAccounts(): List<Account>
   ```

### 3.2 Offline Failure Mechanics
- When an Android device running the current app is disconnected from WiFi and cellular data (or runs in airplane mode):
  1. The OkHttp client attempts DNS resolution for `api.example.com`.
  2. The DNS resolver fails immediately, throwing `java.net.UnknownHostException`.
  3. `runCatching` in `HomeViewModel.kt` catches the exception and sets `errorMessage = "Unable to resolve host \"api.example.com\": No address associated with hostname"`.
  4. `HomeScreen.kt` hides the balance summary card and the accounts list, rendering an `ErrorContent` composable with a retry button.
  5. The retry button triggers the identical network call, creating an infinite error loop.
- **Architectural Impact**: The application is 100% inoperable in an offline environment, directly violating the core project requirement:
  > *"Local-first, hoạt động không cần Internet. Không kết nối API ngân hàng, cổng thanh toán..."* (`.agent/work.md` Section A1).

---

## 4. Finding C07: Security & Privacy Exposure Analysis

### 4.1 Plaintext HTTP Body Logging in Logcat
In `app/src/main/java/com/mebanking/app/data/remote/NetworkModule.kt:18-24`:
```kotlin
private val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = if (BuildConfig.DEBUG) {
        HttpLoggingInterceptor.Level.BODY
    } else {
        HttpLoggingInterceptor.Level.NONE
    }
}
```
- In financial applications, `HttpLoggingInterceptor.Level.BODY` writes:
  - Account numbers, balances, currency, and names.
  - User authentication credentials, tokens, and PINs.
  - Transaction amounts, beneficiary names, and OTPs.
- This data is printed directly to the shared Android OS `logcat` circular memory buffer. On rooted devices or via USB debugging (`adb logcat`), any installed monitoring tool or developer process can capture customer banking credentials in plaintext.

### 4.2 Inadequate Backup and Data Extraction Restrictions
In `app/src/main/AndroidManifest.xml:6-9`:
```xml
<application
    android:allowBackup="true"
    android:dataExtractionRules="@xml/data_extraction_rules"
    android:fullBackupContent="@xml/backup_rules"
```
In `app/src/main/res/xml/data_extraction_rules.xml`:
```xml
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="sharedpref" path="auth.xml" />
    </cloud-backup>
    <device-transfer>
        <exclude domain="sharedpref" path="auth.xml" />
    </device-transfer>
</data-extraction-rules>
```
- While `sharedpref/auth.xml` is excluded, all other storage domains remain eligible for automated backup and extraction:
  - SQLite/Room database files (`/databases/*.db`, `*.db-wal`, `*.db-shm`).
  - Internal application files (`/files/*`).
  - Cache directories and biometric embeddings.
- An attacker with temporary physical access or via an authorized ADB shell can execute `adb backup -f backup.ab com.mebanking.app` and extract all customer account balances, transaction histories, and identity files.

---

## 5. Offline Transition Architecture

To achieve full compliance with `.agent/work.md` and course specifications, the application architecture must undergo the following transition in subsequent phases:

```
                  CURRENT (NETWORK-ONLY)                     TARGET (OFFLINE-FIRST)
              ┌─────────────────────────────┐             ┌─────────────────────────────┐
              │     Compose UI Screens      │             │     Compose UI Screens      │
              └──────────────┬──────────────┘             └──────────────┬──────────────┘
                             │                                           │
                             ▼                                           ▼
              ┌─────────────────────────────┐             ┌─────────────────────────────┐
              │       HomeViewModel         │             │   ViewModels + Use Cases    │
              └──────────────┬──────────────┘             └──────────────┬──────────────┘
                             │                                           │
                             ▼                                           ▼
              ┌─────────────────────────────┐             ┌─────────────────────────────┐
              │      AccountRepository      │             │  Repositories (SSOT via DB) │
              └──────────────┬──────────────┘             └──────────────┬──────────────┘
                             │                                           │
                             ▼                                           ▼
              ┌─────────────────────────────┐             ┌─────────────────────────────┐
              │ BankingApi / Retrofit / OkHttp│            │   Local Room SQLite DB      │
              └──────────────┬──────────────┘             │  (8 Tables, Seed Data, DAOs)│
                             │                            └──────────────┬──────────────┘
                             ▼                                           │
                      EXTERNAL CLOUD                                     ▼
                (api.example.com - Fails)                 ┌─────────────────────────────┐
                                                          │ Internal Kotlin Simulators  │
                                                          │ - ExternalBankSimulator     │
                                                          │ - PaymentGatewaySimulator   │
                                                          │ - UtilitySimulator          │
                                                          │ - ChatbotEngine             │
                                                          │ - Offline Map Road Graph    │
                                                          └─────────────────────────────┘
```

### Transition Steps:
1. **Local Data Persistence (Phase 1 & 2)**:
   - Add Room dependencies and KSP compiler plugin.
   - Establish `AppDatabase` with 8 tables: `User`, `Account`, `Transaction`, `UtilityBill`, `ServiceBooking`, `BranchLocation`, `FaceTemplate`, `InterestRateConfig`.
   - Implement DAOs with Room `@Transaction` for atomic balance operations.
2. **Internal Simulator Engines (Phase 4 & 5)**:
   - Build `ExternalBankSimulator` to simulate external bank routing for 24/7 transfers without internet.
   - Build `PaymentGatewaySimulator` to simulate VNPay / Stripe checkout workflows with simulated callbacks.
   - Build `UtilitySimulator` to host mock electricity, water, and telecom bills.
   - Build `ChatbotEngine` with local intent pattern matching.
   - Build local road network graph with Dijkstra's algorithm for offline branch walking directions.
3. **Security Hardening (Phase 3 & 4)**:
   - Remove or redact HTTP logging interceptor.
   - Configure `android:allowBackup="false"` in `AndroidManifest.xml` or define comprehensive extraction exclusion rules.
   - Implement `EncryptedSharedPreferences` for session tokens and PIN hashes.
