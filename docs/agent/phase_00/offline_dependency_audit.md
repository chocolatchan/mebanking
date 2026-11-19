# Dependency and Offline Architecture Audit

- Task: 0.3 - Dependency & Offline Audit
- Inspected source ref: `master`
- Inspected commit: `efc15e138cb0bc4f3420e8b2caedb29fdfb36299`
- Static audit status: **COMPLETE**
- Formal task status: **BLOCKED by Task 0.2 dependency and unverified runtime behavior**
- No project configuration or dependencies were changed.

## 1. Declared dependencies

Versions are from `gradle/libs.versions.toml` and `app/build.gradle.kts`.

| Dependency | Version | Classification | Runtime Internet required by library itself? | Current use |
|---|---:|---|---|---|
| AndroidX Core KTX | 1.18.0 | Local Android library | No | Android/Kotlin extensions |
| Activity Compose | 1.13.0 | Local Android library / OS integration | No | Compose activity |
| Lifecycle Runtime KTX | 2.11.0 | Local Android library | No | Lifecycle runtime |
| Lifecycle Runtime Compose | 2.11.0 | Local Android library | No | Lifecycle-aware Compose state |
| Lifecycle ViewModel Compose | 2.11.0 | Local Android library | No | ViewModel integration |
| Compose BOM | 2026.09.00 | Build-time version alignment | No | Aligns Compose artifacts |
| Compose UI / UI Graphics / Preview | BOM-managed | Local Android library | No | UI toolkit and preview |
| Material 3 | BOM-managed | Local Android library | No | UI components |
| Compose UI Tooling | BOM-managed | Local debug tooling | No | Debug preview/tooling |
| OkHttp | 5.5.1 | Local networking library | No, not merely by being packaged | Makes HTTP requests when invoked |
| OkHttp Logging Interceptor | 5.5.1 | Local networking library | No | Logs request/response traffic |
| Retrofit | 3.0.0 | Local networking library | No, not merely by being packaged | Invokes remote API when repository methods are called |
| Retrofit Gson Converter | 3.0.0 | Local serialization adapter | No | JSON conversion |
| Gson | 2.13.2 | Local serialization library | No | JSON serialization/deserialization |

Build-time tooling:
- Android Gradle Plugin: 9.5.1.
- Kotlin Compose plugin: 2.4.21.
- Gradle Wrapper: 9.6.0.
These are build/dependency-resolution components, not runtime cloud services. Their initial download/build resolution may require network unless artifacts are cached.

No Room, SQLite persistence library, Firebase/cloud SDK, payment SDK, map SDK, MediaPipe, MobileFaceNet, chatbot SDK, or ML runtime dependency was found in the inspected catalog/app dependency block.

## 2. Android OS APIs and manifest

Manifest: `app/src/main/AndroidManifest.xml`.

| Component / permission | Observed | Offline assessment |
|---|---|---|
| INTERNET | Declared | Allows app network access; current account flow actively uses it |
| Camera | Not declared | No camera feature integrated |
| Location | Not declared | No location feature integrated |
| Biometric/device authentication | No integration found | Android OS capability is not currently used |
| Activity | Launcher activity only | Local OS component |
| Services / receivers / providers | No custom declarations found | No custom background component found |
| Backup | `allowBackup=true` | Privacy/data-extraction policy requires review |

Backup rules:
- `backup_rules.xml` excludes `sharedpref/auth.xml`.
- `data_extraction_rules.xml` excludes the same shared preference path for cloud backup and device transfer.
- No comprehensive exclusions for a future Room database or banking files are shown.

## 3. Network path and runtime Internet dependency (C02)

Observed path:

`HomeScreen → HomeViewModel → AccountRepository → BankingApi → Retrofit → OkHttp`

- HomeViewModel calls refresh() on initialization.
- AccountRepository delegates directly to BankingApi.
- BankingApi exposes GET `accounts` and GET `accounts/{id}`.
- NetworkModule creates Retrofit/OkHttp and uses BuildConfig.API_BASE_URL.
- API base URL is currently `https://api.example.com/`, a placeholder.
- No Room/SQLite DAO, local repository source, cache, synchronization strategy, or offline fallback exists.

Conclusion:
- Retrofit and OkHttp do not require Internet merely because they are packaged.
- The current Home account-loading behavior does attempt remote HTTP and is not offline-first.
- If the placeholder URL is unavailable, the repository has no local source to serve account data.
- The project design reference explicitly describes Repository → Room DAO → SQLite and local simulators; this architecture is not implemented.

Required later architecture direction: local-first repository with Room/SQLite as the source of truth; network must not be required for core banking workflows. Phase 0 does not implement this.

## 4. External services and SDK survey

| Area | Source evidence | Classification | Current conclusion |
|---|---|---|---|
| Banking backend | Placeholder HTTPS base URL and Retrofit endpoints | External service if endpoint is real | No verified backend; current call path is remote |
| Firebase / cloud database | No dependency or SDK found | External | Not integrated in inspected source |
| Payment gateway | No payment SDK/API found | External | Not integrated; later simulators should remain local |
| Map provider | No map SDK found | External | Not integrated; design reference's Google Maps box conflicts with strict offline requirement |
| Chatbot API | No SDK/API found | External | Not integrated; design reference says ChatbotEngine/local intent table |
| Face recognition | No MediaPipe/MobileFaceNet dependency found | Local ML target, not integrated | No model/runtime implementation found |
| Camera / location / biometric | No manifest or code integration found | Android OS APIs | Not integrated |
| Room/SQLite | No dependency or source found | Local persistence | Not integrated |

## 5. Classification summary

### A. Android OS APIs
Activity/lifecycle/Compose integration is local. Camera, Location and Android Device Authentication are not currently integrated. These OS APIs do not inherently require a third-party cloud service, though location availability may depend on device signals and permissions.

### B. Local on-device libraries
AndroidX, Compose, Lifecycle and Gson are local runtime libraries. Retrofit/OkHttp are also local libraries, but the current application uses them to access a remote endpoint. A future Room database, local ML models, local map dataset and rule-based chatbot are not present.

### C. External services
The only configured remote integration is the placeholder account API endpoint. No actual backend availability or runtime traffic was verified. No Firebase, payment gateway, map service or chatbot cloud API integration was found.

## 6. Security and privacy finding C07

**BODY logging**
- `NetworkModule.kt` uses `HttpLoggingInterceptor.Level.BODY` in debug builds and NONE in release.
- Request/response bodies may contain account identifiers, balances, personal data, tokens or transaction details once real APIs are connected.
- No current real customer-data exposure is asserted. This is a high-impact risk before real sensitive data enters the app.
- Later work should remove or redact sensitive fields and ensure production/debug policy is deliberate.

**Android backup**
- Manifest enables `android:allowBackup="true"`.
- Existing rules exclude only `sharedpref/auth.xml` for cloud backup and device transfer.
- Future Room/SQLite databases, files, exports and biometric templates are not covered by a demonstrated comprehensive policy.
- Reassess backup eligibility and explicit exclusions before introducing credentials, account records, transaction history or face templates.

## 7. Findings

- **HIGH C02:** Remote-only Home repository; no local persistence or offline fallback.
- **HIGH:** INTERNET permission and active HTTP call path do not meet a strict no-network core workflow requirement.
- **MEDIUM C07:** Debug BODY logging may expose sensitive payloads.
- **MEDIUM C07:** Backup is enabled and future sensitive local data is not comprehensively covered by current rules.
- **MEDIUM:** Design reference includes Google Maps and Camera + ML Kit in a diagram; these are not implementation evidence and Google Maps is inconsistent with an offline map mandate unless replaced by local map data/routing.
- **MEDIUM:** No local ML models, offline map dataset/road graph or local chatbot intent data are implemented.

## 8. Verification limits and status

Not performed in this environment:
- Gradle dependency report or dependencyInsight.
- Runtime network capture.
- Airplane-mode/offline device testing.
- Android backup/restore test.
- Gradle build/test/lint execution.
- Local working-tree integrity check.

Because Task 0.3 depends on Tasks 0.1 and 0.2 in `.agent/work.md`, and Task 0.2 has no verified execution evidence in this environment, formal Task 0.3 remains **BLOCKED** despite the static source audit being documented. Do not mark offline capability PASS without runtime evidence.
