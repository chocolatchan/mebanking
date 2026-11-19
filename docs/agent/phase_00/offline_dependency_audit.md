# Dependency & Offline Audit

- Task: 0.3 - Dependency & Offline Audit
- Inspected ref: `master`
- Commit: `efc15e138cb0bc4f3420e8b2caedb29fdfb36299`
- Status: **CONDITIONAL PASS for static source audit; runtime offline behavior NOT VERIFIED**

## 1. Declared application dependencies

Versions are from `gradle/libs.versions.toml`.

| Dependency | Version | Role | Runtime Internet required by library itself? |
|---|---:|---|---|
| AndroidX Core KTX | 1.18.0 | Android/Kotlin extensions | No |
| Activity Compose | 1.13.0 | Compose activity integration | No |
| Lifecycle Runtime KTX | 2.11.0 | Lifecycle integration | No |
| Lifecycle Runtime Compose | 2.11.0 | Lifecycle-aware Compose state | No |
| Lifecycle ViewModel Compose | 2.11.0 | ViewModel integration in Compose | No |
| Compose BOM | 2026.09.00 | Compose version alignment | No |
| Compose UI / UI Graphics / Preview | BOM-managed | UI toolkit | No |
| Material 3 | BOM-managed | UI components | No |
| Compose UI Tooling | BOM-managed | Debug tooling | No |
| OkHttp | 5.5.1 | HTTP client | No, but performs network I/O when used |
| OkHttp Logging Interceptor | 5.5.1 | HTTP logging | No, but can log network traffic |
| Retrofit | 3.0.0 | HTTP API abstraction | No, but invokes remote endpoints when used |
| Retrofit Gson Converter | 3.0.0 | JSON conversion | No |
| Gson | 2.13.2 | JSON serialization/deserialization | No |

The Android Gradle Plugin is 9.5.1 and Kotlin Compose plugin version is 2.4.21. These are build-time plugins, not runtime services.

No Room, SQLite abstraction library, Firebase, cloud SDK, payment SDK, map SDK, MediaPipe, or MobileFaceNet dependency was found in the version catalog or app dependency block.

## 2. Manifest permissions and components

Manifest: `app/src/main/AndroidManifest.xml`

| Item | Observed | Assessment |
|---|---|---|
| INTERNET | Declared | Enables network access; conflicts with a strict no-network runtime goal unless removed or justified |
| Camera | Not declared | No camera capability integrated in current manifest |
| Location | Not declared | No location capability integrated |
| Biometric/device authentication | No related permission/component found | No device authentication integration found |
| Services/receivers/providers | No custom components found | Only launcher activity is declared |
| Backup | Enabled | Review backup/data-extraction policy before storing sensitive banking data |

## 3. Network call path

Static source inspection found this active call path:

`HomeScreen -> HomeViewModel.init/refresh() -> AccountRepository.getAccounts() -> BankingApi.getAccounts() -> Retrofit/OkHttp`

- `API_BASE_URL` is injected through BuildConfig and currently set to `https://api.example.com/`.
- The URL is a placeholder, not a verified production endpoint.
- The Home ViewModel invokes account loading during initialization.
- `NetworkModule` configures OkHttp with connect/read/write timeouts of 30 seconds.
- Debug builds enable HTTP BODY logging.

Conclusion: the current Home screen's account-loading path is network-dependent. A successful offline account load is not implemented or demonstrated. The networking libraries do not independently require Internet merely by being packaged, but this app actively invokes them on the Home loading path.

## 4. External services

No Firebase, remote database, payment gateway, map provider, or chatbot provider SDK/API integration was found in the inspected tree. This is a source-level observation only; runtime traffic was not captured.

## 5. Offline classification

### A. Android OS APIs
- Activity / lifecycle / UI integration: used locally.
- Camera, Location, BiometricPrompt/device authentication: not currently integrated.

### B. Local libraries
- AndroidX, Compose, Lifecycle, Gson: local runtime libraries.
- Retrofit/OkHttp: local libraries that enable external HTTP requests; their current use is not offline-first.

### C. External services
- Placeholder account API base URL: configured as `https://api.example.com/`.
- Actual backend service availability and behavior: unknown.
- No local database fallback was found.

## 6. Findings and recommended disposition

1. **HIGH:** Home account retrieval is remote-only. Replace with an approved local-first repository backed by Room/SQLite in a later task; do not silently change it during Phase 0.
2. **HIGH:** `INTERNET` permission and Retrofit/OkHttp account calls contradict the stated no-external-service runtime target unless explicitly retained for a justified purpose.
3. **MEDIUM:** Debug BODY logging can expose sensitive banking payloads once real data is used. Redact or disable sensitive logging in a later security task.
4. **MEDIUM:** Backup is enabled. Reassess backup rules before storing credentials, account details, transaction history, or biometric templates.
5. **MEDIUM:** No local database, local ML models, offline map dataset, or local road graph exists in the inspected snapshot.
6. **MEDIUM:** No camera/location/device-auth integrations exist yet, so offline behavior for those capabilities cannot be evaluated.

## 7. Verification limits

No airplane-mode/device test, network capture, Gradle dependency report, or runtime inspection was executed. A static absence of additional SDKs does not prove the whole application is offline-capable.
