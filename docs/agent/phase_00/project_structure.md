# Project Structure & Architecture Mapping

**Phase**: Phase 0 — Repository Reconnaissance  
**Task**: Task 0.1 Repository Inventory & Architectural Gap Survey  
**Date**: 2026-11-19  
**Target Repository**: `/home/chocolat/Documents/Project/m.ebanking`  

---

## 1. Overview & Repository Topology

The `m.ebanking` repository is currently configured as a single-module Android application project (`:app`). It is built using Gradle Kotlin DSL (`*.gradle.kts`), Android Gradle Plugin (AGP) 9.5.1, Kotlin 2.4.21, and Jetpack Compose with Material 3.

This document maps the repository's directory layout, source package hierarchy, Android resources, current runtime data flow, and the target offline-first architecture required for full course specification compliance.

---

## 2. Comprehensive Repository Directory Tree

```
/home/chocolat/Documents/Project/m.ebanking/
├── build.gradle.kts                                    # Root Gradle build script (declares AGP & Compose plugins)
├── settings.gradle.kts                                 # Project settings (repositories & module includes)
├── gradle.properties                                   # JVM options (-Xmx2048m), AndroidX, caching flags
├── gradlew                                             # Gradle Unix wrapper script
├── gradlew.bat                                         # Gradle Windows wrapper script
├── gradle/
│   ├── libs.versions.toml                              # Version catalog (AGP 9.5.1, Kotlin 2.4.21, Jetpack, Retrofit)
│   └── wrapper/
│       ├── gradle-wrapper.jar                          # Gradle wrapper bootstrap JAR
│       └── gradle-wrapper.properties                   # Gradle 9.6.0 distribution URL
├── .agent/
│   └── work.md                                         # Authoritative execution pack & phase specifications
├── docs/
│   └── agent/
│       ├── project_status.md                           # Master project & gate tracking document
│       └── phase_00/                                   # Phase 0 reconnaissance deliverable reports
│           ├── repository_inventory.md                 # Tech stack, file inventory & C01-C07 audit
│           ├── project_structure.md                    # Directory tree & architectural mapping (this document)
│           ├── implementation_status.md                # Feature status matrix & placeholder audit
│           ├── baseline_build_report.md                # Verbatim build commands, exit codes & lint
│           ├── baseline_test_report.md                 # Test execution commands & 0-test root cause analysis
│           └── offline_dependency_audit.md             # 3-tier dependency classification & offline audit
└── app/
    ├── build.gradle.kts                                # Module build script (SDK 37, Compose, Retrofit, OkHttp)
    ├── proguard-rules.pro                              # Proguard release rules
    └── src/
        └── main/
            ├── AndroidManifest.xml                     # App manifest (INTERNET permission, MainActivity, Backup rules)
            ├── java/
            │   └── com/
            │       └── mebanking/
            │           └── app/
            │               ├── MainActivity.kt         # Single ComponentActivity host entry point
            │               ├── ui/
            │               │   ├── MEbankingApp.kt     # Top-level composable surface
            │               │   ├── home/
            │               │   │   ├── HomeScreen.kt   # Composable home screen (Account list, Total balance)
            │               │   │   ├── HomeUiState.kt  # UI state data class (computed balance & currency)
            │               │   │   └── HomeViewModel.kt# MVVM ViewModel fetching accounts
            │               │   └── theme/
            │               │       ├── Color.kt        # Material 3 color definitions
            │               │       ├── Theme.kt        # Application dynamic & static theme setup
            │               │       └── Type.kt         # Typography configuration
            │               └── data/
            │                   ├── model/
            │                   │   └── Account.kt      # Account data model (balance, IBAN, currency)
            │                   ├── remote/
            │                   │   ├── BankingApi.kt   # Retrofit HTTP GET endpoints
            │                   │   └── NetworkModule.kt# Retrofit & OkHttp client object provider
            │                   └── repository/
            │                       └── AccountRepository.kt # Repository pass-through to BankingApi
            └── res/
                ├── drawable/
                │   ├── ic_launcher_background.xml      # Background vector for adaptive launcher icon
                │   └── ic_launcher_foreground.xml      # Foreground vector for adaptive launcher icon
                ├── mipmap-anydpi-v26/
                │   ├── ic_launcher.xml                 # Adaptive icon declaration
                │   └── ic_launcher_round.xml           # Adaptive round icon declaration
                ├── mipmap-hdpi/, mipmap-mdpi/, mipmap-xhdpi/, mipmap-xxhdpi/, mipmap-xxxhdpi/
                │   ├── ic_launcher.webp                # Standard launcher icons
                │   └── ic_launcher_round.webp          # Round launcher icons
                ├── values/
                │   ├── colors.xml                      # XML color values
                │   ├── strings.xml                     # String resources (app name, UI labels)
                │   └── themes.xml                      # XML styles (Theme.MEbanking)
                └── xml/
                    ├── backup_rules.xml                # Legacy cloud backup rules
                    └── data_extraction_rules.xml       # Android 12+ device transfer and cloud rules
```

---

## 3. Source Code Architecture & Package Mapping

The existing source code adheres to standard Android clean architecture package naming, though it covers only the skeleton of the Home feature:

| Package | Files | Core Responsibilities | Architectural Defects Present |
|---|---|---|---|
| `com.mebanking.app` | `MainActivity.kt` | Host activity; initializes window edge-to-edge and sets `MEbankingApp` content. | None. |
| `com.mebanking.app.ui` | `MEbankingApp.kt` | Root Composable wrapper rendering `MEbankingTheme` and `HomeScreen`. | Missing Compose Navigation (`NavHost`). |
| `com.mebanking.app.ui.home` | `HomeScreen.kt`<br>`HomeUiState.kt`<br>`HomeViewModel.kt` | Displays accounts, balance card, and refresh action. ViewModel manages UI state via `MutableStateFlow`. | **C01** (Double in formatting), **C03** (hardcoded repo), **C04** (`runCatching` cancellation), **C05** (raw error), **C06** (multi-currency total sum). |
| `com.mebanking.app.ui.theme` | `Color.kt`<br>`Theme.kt`<br>`Type.kt` | Material Design 3 styling, color scheme, typography definitions. | None. |
| `com.mebanking.app.data.model` | `Account.kt` | Account entity with Gson `@SerializedName` annotations. | **C01** (`balance: Double`). |
| `com.mebanking.app.data.remote` | `BankingApi.kt`<br>`NetworkModule.kt` | Retrofit interface for remote bank endpoints; OkHttp client configuration. | **C02** (Remote only), **C07** (`Level.BODY` HTTP logging). |
| `com.mebanking.app.data.repository`| `AccountRepository.kt` | Data abstraction layer delegating calls to `BankingApi`. | **C02** (Zero local caching/Room), **C03** (hardcoded `NetworkModule.api`). |

---

## 4. Current vs. Target Architectural Layer Mapping

### 4.1 Current Architecture (Network-Only Flow)

```
[ MainActivity ]
       │
       ▼
[ MEbankingApp (Root Composable) ]
       │
       ▼
[ HomeScreen (Compose UI) ]
       │ observes StateFlow<HomeUiState>
       ▼
[ HomeViewModel ]
       │ calls suspend functions
       ▼
[ AccountRepository ]
       │ delegates directly
       ▼
[ BankingApi (Retrofit) ]
       │ calls HTTP endpoints
       ▼
[ OkHttp Client + LoggingInterceptor ]
       │ sends network requests (requires internet)
       ▼
[ Remote Server: https://api.example.com/ ]
```

**Key Failure Points in Current Architecture**:
1. Single point of failure: If network is offline or remote host is down, the entire app fails.
2. Inversion of control broken: ViewModel instantiates concrete Repository, which accesses singleton OkHttp client.
3. No local persistence: No Room, no SQLite, no SharedPreferences data store.

---

### 4.2 Target Offline-First Clean Architecture

To fulfill `.agent/work.md` Section A1 and course requirements, the application must transition to an offline-first clean architecture:

```
┌────────────────────────────────────────────────────────────────────────┐
│                               UI LAYER                                 │
│  Compose Screens: Auth, Home, Transfer, BillPay, Map, Chatbot, Officer │
│  State Management: Jetpack ViewModels (StateFlow / UI State Models)    │
│  Navigation: Navigation Compose (NavHost / Navigation Graph)           │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
                                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                              DOMAIN LAYER                              │
│  Use Cases: AuthenticateUser, ExecuteTransfer, PayBill, CalculateProfit│
│  Business Models: User, Account (Long VND), Transaction, Bill, Branch  │
│  Repository Interfaces: IAccountRepository, ITransactionRepository     │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
                                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                               DATA LAYER                               │
│  Repositories: AccountRepositoryImpl, TransactionRepositoryImpl, etc.  │
│                                                                        │
│  ┌───────────────────────────────┐   ┌──────────────────────────────┐  │
│  │     Local Data Source         │   │   Internal Simulators        │  │
│  │  - Room Database (8 tables)   │   │  - ExternalBankSimulator     │  │
│  │  - DAOs (User, Account, Tx)   │   │  - PaymentGatewaySimulator   │  │
│  │  - EncryptedPrefs / Seed Data │   │  - UtilityProviderSimulator  │  │
│  │  - FaceEmbeddingStore         │   │  - ChatbotEngine (Rule-based)│  │
│  │                               │   │  - Offline Road Graph        │  │
│  └───────────────────────────────┘   └──────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

**Architectural Improvements in Target State**:
1. **Offline Persistence**: Room SQLite database acts as the single source of truth for all customer records, accounts, and transactions.
2. **Zero External Network Dependencies**: All external banking interactions, payment gateways (VNPay/Stripe), utility bill lookups, and chatbot responses are executed via deterministic internal Kotlin simulators.
3. **Strict Monetary Typing**: All financial quantities stored and calculated using integer `Long` in VND.
4. **Structured Concurrency**: Proper coroutine exception handling preserving `CancellationException`.

---

## 5. Structural Deficiencies & Proposed Expansion Plan

### 5.1 Missing Source Sets
- `app/src/test/java/`: **Missing entirely**. Must be created to host automated unit tests for ViewModels, Use Cases, Repositories, and Simulators.
- `app/src/androidTest/java/`: **Missing entirely**. Must be created for Room database migration tests and Compose UI interaction tests.

### 5.2 Proposed Package Reorganization for Future Phases

```
com.mebanking.app/
├── MEbankingApplication.kt                  # Application class hosting AppContainer
├── di/                                      # Dependency Injection container & factories
│   ├── AppContainer.kt                      # Manual DI container providing DAOs & Repositories
│   └── ViewModelFactory.kt                  # Generic ViewModelProvider.Factory
├── data/
│   ├── local/                               # Room Database & DAOs
│   │   ├── AppDatabase.kt                   # Room database singleton (version 1)
│   │   ├── dao/                             # UserDao, AccountDao, TransactionDao, UtilityDao...
│   │   ├── entity/                          # UserEntity, AccountEntity, TransactionEntity...
│   │   └── security/                        # EncryptedKeyStore, BiometricHelper
│   ├── simulator/                           # Offline Simulator Engines
│   │   ├── ExternalBankSimulator.kt         # Interbank transfer simulation
│   │   ├── PaymentGatewaySimulator.kt       # VNPay / Stripe simulator
│   │   ├── UtilitySimulator.kt              # Electricity / Water / Telecom billing simulator
│   │   ├── ChatbotEngine.kt                 # Rule-based intent matcher
│   │   └── NavigationGraph.kt               # Local branch graph & shortest path solver
│   └── repository/                          # Repository implementations
├── domain/                                  # Pure Kotlin business rules & interfaces
│   ├── model/                               # Domain models (Long VND currency)
│   ├── repository/                          # Repository interfaces (IAccountRepository...)
│   └── usecase/                             # TransferMoneyUseCase, CalculateInterestUseCase...
└── ui/                                      # Jetpack Compose UI
    ├── navigation/                          # NavHost, Screen routes, Navigation arguments
    ├── auth/                                # Login, Officer login, eKYC Face registration
    ├── home/                                # Customer home dashboard, balance overview
    ├── account/                             # Account details, profit/mortgage schedules
    ├── transfer/                            # Internal transfer, interbank transfer, 2FA dialog
    ├── utility/                             # Bill payment, booking, services
    ├── branch/                              # Offline map, branch list, walking directions
    ├── chatbot/                             # AI assistant chat interface
    ├── officer/                             # Bank officer customer management & rate adjustments
    └── common/                              # Reusable Compose components, CurrencyFormatter
```

---

## 6. Verification and Validation

- **File System Inspection**: Full directory hierarchy audited against disk contents.
- **Git State Verification**: Zero files in `app/` modified or created.
- **Next Steps**: Document implementation status in `docs/agent/phase_00/implementation_status.md`.
