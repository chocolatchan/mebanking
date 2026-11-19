# Repository Inventory

- Task: 0.1 - Repository Inventory
- Source repository: `chocolatchan/mebanking`
- Inspected ref: `master`
- Inspected commit: `efc15e138cb0bc4f3420e8b2caedb29fdfb36299` (2026-11-19)
- Scope: read-only inspection of tracked repository files through GitHub.
- Gate status: **CONDITIONAL PASS**. Source inventory is available; local-only reference documents and local working-tree state were not accessible.

## 1. Repository overview

The repository is a small, single-module Android application skeleton. GitHub reports Kotlin as its primary language. The root project includes one Gradle application module, `:app`, using Kotlin DSL and a version catalog.

| Item | Observed value |
|---|---|
| Repository visibility | Public |
| Default branch | `master` |
| Android module | `:app` |
| Namespace | `com.mebanking.app` |
| Application ID | `com.mebanking.app` |
| Debug application ID suffix | `.debug` |
| minSdk | 24 |
| compileSdk | 37 |
| targetSdk | 37 |
| Java source/target compatibility | 17 |
| UI | Jetpack Compose, Material 3 |
| Networking | Retrofit, OkHttp, Gson |
| Persistence | No Room/SQLite implementation found |
| Dependency injection | Manual construction/default constructor wiring |
| Tests | No `src/test` or `src/androidTest` files in the inspected tree |

## 2. Technology and architecture

- Kotlin Android application with a single `MainActivity`.
- Jetpack Compose Material 3 UI.
- A preliminary UI/data separation resembling MVVM: `HomeScreen`, `HomeUiState`, `HomeViewModel`, and `AccountRepository`.
- Repository accesses a Retrofit interface through `NetworkModule`.
- No Domain/use-case layer, Room DAO/database, migration, DI framework, or navigation graph was found.
- `MEbankingApp` renders `HomeScreen` directly. The README itself lists adding Navigation Compose as a next step.

## 3. Manifest and platform integration

The manifest declares only `android.permission.INTERNET`. The launcher activity is `.MainActivity`, exported with MAIN/LAUNCHER intent filters. No camera, location, biometric, notification, or other runtime permission is declared in the manifest.

The application enables Android backup and data extraction rules. These files should be reviewed again when sensitive banking data is introduced.

## 4. Implemented data path observed

The current Home screen obtains accounts through:

`HomeScreen -> HomeViewModel -> AccountRepository -> BankingApi -> NetworkModule (Retrofit/OkHttp)`

The ViewModel starts a refresh during initialization. The repository delegates directly to the remote API. No local cache or database fallback is present.

## 5. Important observations

1. `Account.balance` and the aggregate balance use Kotlin `Double`. This conflicts with `.agent/work.md` A2 rule 10, which requires `Long` for VND monetary values.
2. The sample preview uses EUR and German IBAN examples. These are sample UI data, not evidence of Vietnam banking rules or production account data.
3. `API_BASE_URL` is currently set to `https://api.example.com/`, a placeholder.
4. The debug OkHttp logging interceptor uses BODY level. Before real customer data is introduced, sensitive request/response bodies must not be logged.
5. `HomeViewModel` catches failures and displays the throwable message. User-safe error mapping has not been demonstrated.
6. No authentication, role model, transaction logic, local persistence, or offline fallback was found in the inspected source files.

## 6. Inputs and limitations

Repository documents read: `README.md` and `.agent/work.md`.

The following external/local reference files named by `.agent/work.md` were not present in the repository and were not accessible in this inspection environment:
- `FinalProject_2026_2027.pdf` (the task pack also refers to a filename variant `FinalProject_2026_2027(1).pdf`)
- `Sơ đồ thiết kế app ngân hàng.md`

No conclusions about their detailed requirements are made here. No Gradle build, test, Android runtime, or local `git status` command was executed.

## 7. Files changed

No application source, Gradle configuration, dependency declaration, manifest, or resource file was changed for this inventory. This report is documentation only. Local uncommitted changes cannot be inspected through the GitHub repository view.
