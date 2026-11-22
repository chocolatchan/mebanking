# Implementation Status & Feature Specification Matrix

**Phase**: Phase 0 — Repository Reconnaissance  
**Task**: Task 0.1 Repository Inventory & Architectural Gap Survey  
**Date**: 2026-11-19  
**Target Repository**: `/home/chocolat/Documents/Project/m.ebanking`  

---

## 1. Baseline Feature Audit Matrix

This matrix compares the current state of the repository against the full specification set defined in `FinalProject_2026_2027.pdf`, `Sơ đồ thiết kế app ngân hàng.md`, and `.agent/work.md`:

| # | Feature Domain | Specification Requirements | Current Codebase Status | Existing Code / Files | Missing Components |
|---|---|---|---|---|---|
| **F01** | **User Profiles & Authentication** | - Dual roles: Customer and Bank Officer.<br>- Login with username/password.<br>- Secure password hashing.<br>- Session state & profile edit. | **Missing (0%)** | `NetworkModule.kt:30` has `// TODO: attach the auth token` | - Login screen UI.<br>- Customer / Officer role routing.<br>- AuthRepository & SessionManager.<br>- Password hashing & user profile DB table. |
| **F02** | **eKYC, Biometrics & 2FA** | - Face capture & landmark recognition (MediaPipe / MobileFaceNet).<br>- Mandatory 2FA OTP for all transactions.<br>- Android Device Authentication for normal transactions.<br>- In-app face recognition for high-value transactions. | **Missing (0%)** | None | - Camera capture & ML pipeline.<br>- BiometricPrompt integration.<br>- OTP generation & validation simulator.<br>- Face embedding database storage. |
| **F03** | **Account Management** | - 3 Account types: Checking, Saving, Mortgage.<br>- Monthly profit calculation for Saving.<br>- Bi-weekly/monthly mortgage repayment schedule.<br>- Interest rate adjustments by Officer.<br>- Deposit and withdrawal operations. | **Stub (~10%)** | `Account.kt`<br>`HomeScreen.kt:126-159`<br>`HomeViewModel.kt` | - Account type hierarchy.<br>- Interest & repayment calculator.<br>- Deposit/Withdraw UI and actions.<br>- Officer rate update screen.<br>- Room AccountEntity. |
| **F04** | **Transaction Management** | - Internal bank transfers (atomic balance swap).<br>- Interbank transfers via `ExternalBankSimulator`.<br>- Payment gateway integration via `PaymentGatewaySimulator` (VNPay/Stripe).<br>- Transaction history ledger. | **Missing (0%)** | None | - Transfer money UI.<br>- Transaction history screen.<br>- ExternalBankSimulator.<br>- PaymentGatewaySimulator.<br>- Room TransactionEntity & atomic DAO transactions. |
| **F05** | **Utilities & Services** | - Electricity, water, internet bill lookup & payment.<br>- Phone balance top-up.<br>- Flight, hotel, cinema ticket booking.<br>- Simulated billers and inventory catalogs. | **Missing (0%)** | None | - Utility payment UI.<br>- Booking catalog UI.<br>- UtilitySimulator engine.<br>- Biller & receipt database models. |
| **F06** | **Branch Navigation & Map** | - User current location detection.<br>- Offline map display of bank branches & ATMs.<br>- Shortest walking path routing algorithm. | **Missing (0%)** | None | - Location permissions & GPS provider.<br>- Offline map composable.<br>- Branch coordinate database.<br>- Dijkstra / A* pathfinding graph solver. |
| **F07** | **AI Banking Chatbot** | - In-app chat interface.<br>- Rule-based & intent-based NLU engine.<br>- Action deep links directly into banking screens. | **Missing (0%)** | None | - Chat UI composable.<br>- Intent matching engine.<br>- Deep link navigation triggers.<br>- Chat history persistence. |
| **F08** | **Bank Officer Operations** | - Setup customer accounts upon request.<br>- Modify customer data.<br>- Adjust interest rates.<br>- Verify customer eKYC status. | **Missing (0%)** | None | - Officer dashboard UI.<br>- Customer registration form.<br>- Interest rate configuration UI.<br>- Officer audit logs. |
| **F09** | **Local Database (Room/SQLite)** | - Offline-first architecture.<br>- 8 relational tables.<br>- Seed data with demo accounts, bills, branches. | **Missing (0%)** | `AccountRepository.kt:8` comment: `"Add caching (Room) here later"` | - Room runtime, KTX, compiler dependencies.<br>- Database class & 8 DAOs.<br>- Database pre-population callback. |
| **F10** | **Automated Testing Suite** | - Unit tests for ViewModels, Repositories, DAOs.<br>- UI/Compose tests for core flows.<br>- End-to-end transaction test scenarios. | **Missing (0%)** | None | - `src/test/` directory.<br>- `src/androidTest/` directory.<br>- Testing libraries (JUnit4, MockK, Coroutines-Test, Compose Test). |

---

## 2. Analysis of Implemented Skeleton Code

The current repository contains exactly 9 Kotlin source files. Below is an exhaustive audit of each implemented component:

### 2.1 `MainActivity.kt`
- **Location**: `app/src/main/java/com/mebanking/app/MainActivity.kt`
- **Lines**: 20 lines.
- **Role**: Standard Android `ComponentActivity`. Enables edge-to-edge window insets and sets `MEbankingApp` as the Compose root.
- **Completeness**: Minimal valid entry point. Does not handle splash screen, deep links, or permission requests.

### 2.2 `MEbankingApp.kt`
- **Location**: `app/src/main/java/com/mebanking/app/ui/MEbankingApp.kt`
- **Lines**: 22 lines.
- **Role**: Composable root wrapping `HomeScreen` inside `MEbankingTheme` and Material 3 `Surface`.
- **Completeness**: Lacks Compose Navigation (`NavHost`). The app cannot navigate between screens.

### 2.3 `HomeScreen.kt`
- **Location**: `app/src/main/java/com/mebanking/app/ui/home/HomeScreen.kt`
- **Lines**: 249 lines.
- **Role**: Material 3 UI rendering a top app bar, total balance card, list of account cards (`AccountCard`), loading indicator (`CircularProgressIndicator`), and error banner (`ErrorContent`).
- **Defects Present**:
  - Uses `formatAmount(amount: Double, currency: String)` with `Locale.US` and 2 decimal places (`%,.2f`), which violates Rule A2.10 (Finding C01).
  - Displays total balance from `uiState.totalBalance` with currency from `uiState.currency`, compounding multi-currency errors (Finding C06).
  - Instantiates `HomeViewModel` via default `viewModel()` with no factory (Finding C03).

### 2.4 `HomeUiState.kt`
- **Location**: `app/src/main/java/com/mebanking/app/ui/home/HomeUiState.kt`
- **Lines**: 15 lines.
- **Role**: Data class representing UI state (`isLoading`, `accounts`, `errorMessage`).
- **Defects Present**:
  - Encapsulates `totalBalance: Double get() = accounts.sumOf { it.balance }` — performs raw floating-point sum across potentially differing currencies (Findings C01, C06).
  - Encapsulates `currency: String get() = accounts.firstOrNull()?.currency ?: "EUR"` — defaults to European currency rather than Vietnamese Dong (Finding C06).

### 2.5 `HomeViewModel.kt`
- **Location**: `app/src/main/java/com/mebanking/app/ui/home/HomeViewModel.kt`
- **Lines**: 42 lines.
- **Role**: ViewModel managing `StateFlow<HomeUiState>` and loading accounts in `init { refresh() }`.
- **Defects Present**:
  - Hardcodes default constructor parameter `repository: AccountRepository = AccountRepository()` (Finding C03).
  - Wraps network call in `runCatching`, swallowing `CancellationException` and corrupting coroutine structured concurrency (Finding C04).
  - Exposes raw `throwable.message` to `errorMessage` (Finding C05).

### 2.6 `Theme.kt`, `Color.kt`, `Type.kt`
- **Location**: `app/src/main/java/com/mebanking/app/ui/theme/`
- **Lines**: 78, 22, and 36 lines.
- **Role**: Establishes Material Design 3 theme with dynamic color support (Android 12+) and fallback palette (Deep Teal `#006874`, Soft Sand `#FBFDF9`, Amber Gold `#FFB74D`).
- **Completeness**: Well-structured design system tokens.

### 2.7 `Account.kt`
- **Location**: `app/src/main/java/com/mebanking/app/data/model/Account.kt`
- **Lines**: 19 lines.
- **Role**: Data class model with fields `id`, `name`, `iban`, `balance`, `currency`. Annotated with Gson `@SerializedName`.
- **Defects Present**:
  - `balance: Double` violates integer VND rule A2.10 (Finding C01).
  - Does not support account types (Checking, Saving, Mortgage).

### 2.8 `BankingApi.kt` & `NetworkModule.kt`
- **Location**: `app/src/main/java/com/mebanking/app/data/remote/`
- **Lines**: 17 and 51 lines.
- **Role**: Retrofit REST endpoints (`@GET("accounts")`, `@GET("accounts/{id}")`) and OkHttp singleton provider with `HttpLoggingInterceptor`.
- **Defects Present**:
  - Logs HTTP requests/responses at `Level.BODY` in debug builds (Finding C07).
  - Network-only architecture without local caching (Finding C02).

### 2.9 `AccountRepository.kt`
- **Location**: `app/src/main/java/com/mebanking/app/data/repository/AccountRepository.kt`
- **Lines**: 17 lines.
- **Role**: Passthrough repository delegating `getAccounts()` directly to `BankingApi`.
- **Defects Present**:
  - Hardcodes `NetworkModule.api` (Finding C03).
  - Lacks Room caching (Finding C02).

---

## 3. Inventory of Mock and Placeholder Elements

1. **Dummy API Host**: `app/build.gradle.kts:17` sets `buildConfigField("String", "API_BASE_URL", "\"https://api.example.com/\"")`. This is a non-functional placeholder domain.
2. **Missing Auth Token Placeholder**: `NetworkModule.kt:30` contains a comment: `// TODO: attach the auth token to requests once authentication is implemented`.
3. **Missing Room Cache Placeholder**: `AccountRepository.kt:8` contains a doc comment: `/** Single source of truth for account data. Add caching (Room) here later without touching the UI layer. */`.
4. **Hardcoded European Currency**: `HomeUiState.kt:14` hardcodes `"EUR"` as the default fallback currency string.
5. **International Bank Account Number (IBAN)**: `Account.kt:14` defines `val iban: String`, referencing European banking standards rather than Vietnamese account numbers.

---

## 4. Monetary Data Type & Calculation Audit (C01 & C06)

| Parameter | Current Codebase Implementation | Course Specification Mandate | Discrepancy / Risk |
|---|---|---|---|
| **Data Type** | `Double` (64-bit IEEE 754) | `Long` (64-bit integer in VND) | Severe. Floating-point imprecision in financial transactions. |
| **Currency Unit** | Undefined; defaults to `"EUR"` | Vietnamese Dong (`VND`) | Contextual mismatch. App must default to VND. |
| **Decimal Formatting** | `%,.2f` (US locale, 2 decimal places) | Integer formatting (`#,##0 ₫` / `1.000.000 VND`) | VND has no minor cents. Format shows spurious `.00`. |
| **Balance Summation** | `accounts.sumOf { it.balance }` in UI State getter | Encapsulated domain use case converting to base currency | Adds mixed currencies directly without conversion rates. |

---

## 5. Implementation Roadmap for Subsequent Phases

```
Phase 0: Reconnaissance & Baseline Audit (CURRENT)
  └── Complete repository inventory, gap audit C01-C07, baseline build/test logs.
       │
Phase 1: Domain Modeling & Database Schema
  ├── Define 8 relational tables (User, Account, Transaction, Utility, Branch, etc.).
  ├── Establish integer Long VND monetary types.
  └── Formalize offline simulator contracts.
       │
Phase 2: Local Persistence & Room Implementation
  ├── Implement Room Database, DAOs, Entities, and TypeConverters.
  ├── Seed demo data (Customer, Officer, Accounts, Utilities, Branches).
  └── Build manual DI container (`AppContainer`).
       │
Phase 3: Auth, Navigation & Account Management
  ├── Navigation Compose setup (NavHost).
  ├── Login screen with Customer & Officer RBAC.
  ├── Refactor Home & Account screens to Room SSOT.
  └── Add Unit Tests for ViewModels & DAOs.
       │
Phase 4: Transactions, Security & Payment Simulators
  ├── Atomic internal transfer & interbank simulator (`ExternalBankSimulator`).
  ├── Mandatory 2FA OTP & High-value Face Recognition (MediaPipe + MobileFaceNet).
  └── Payment gateway simulator (VNPay/Stripe).
       │
Phase 5: Utilities, Offline Navigation & AI Chatbot
  ├── Bill payment & booking simulator (`UtilitySimulator`).
  ├── Offline branch map with shortest walking route solver.
  └── Rule-based Banking Chatbot with actionable deep links.
       │
Phase 6: Final Hardening, Integration & Verification
  └── Full test coverage, lint cleanup, Proguard verification, course demo build.
```
