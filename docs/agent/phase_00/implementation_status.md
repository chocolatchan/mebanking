# Implementation Status (Phase 0 Snapshot)

- Inspected source ref: `master`
- Inspected commit: `efc15e138cb0bc4f3420e8b2caedb29fdfb36299`
- Assessment basis: tracked source inspection; no runtime verification.
- This report describes the inspected repository snapshot, not local untracked changes.

Status meanings:
- **IMPLEMENTED (narrow):** concrete code exists for the narrow behavior described.
- **PARTIAL:** a slice exists, but the project requirement is not met end-to-end.
- **MISSING:** no corresponding implementation was found.
- **UNVERIFIED:** evidence is insufficient.

| Feature / capability | Source evidence | Status | Gap / caveat |
|---|---|---|---|
| Android entry | `MainActivity.kt` | IMPLEMENTED | Single activity |
| Compose Material 3 theme | `ui/theme/*` | IMPLEMENTED | Visual foundation only |
| Home screen | `HomeScreen.kt` | PARTIAL | Displays remote account data |
| Loading/error/empty states | `HomeScreen.kt`, `HomeUiState.kt` | IMPLEMENTED | Error mapping remains raw |
| Account DTO | `Account.kt` | PARTIAL | API DTO, not domain account model; balance is Double (C01) |
| Account API | `BankingApi.kt` | PARTIAL | Two GET endpoints; backend not included |
| HTTP client | `NetworkModule.kt` | IMPLEMENTED | Placeholder base URL; debug BODY logging (C07) |
| Account repository | `AccountRepository.kt` | PARTIAL | Remote-only; no local data source (C02) |
| Home ViewModel | `HomeViewModel.kt` | PARTIAL | Loads accounts; direct default repository wiring (C03); cancellation/error issues (C04/C05) |
| Balance formatting | `HomeScreen.kt` | PARTIAL | UI-local Double formatter; not a shared VND policy (C01) |
| Total balance | `HomeUiState.kt` | PARTIAL / INCORRECT FOR MIXED CURRENCIES | Blindly sums balances and labels with first account currency (C06) |
| Navigation | `MEbankingApp.kt` | MISSING | Root renders Home directly |
| Authentication / authorization | No implementation found | MISSING | No login, session, Customer/Officer role model |
| eKYC | No implementation found | MISSING | No identity verification flow |
| Room / SQLite | No implementation or dependency found | MISSING | No entity, DAO, database, migration |
| Checking / Saving / Mortgage rules | No implementation found | MISSING | Account display DTO is not account-product logic |
| Deposit / withdraw / transfer | No implementation found | MISSING | No transaction domain/service/ledger |
| Transaction 2FA | No implementation found | MISSING | Policy is not implemented |
| Android Device Authentication | No implementation found | MISSING | No BiometricPrompt/device credential integration |
| In-app face recognition | No implementation/dependency found | MISSING | No MediaPipe/MobileFaceNet integration |
| Utility simulators | No implementation found | MISSING | No bill/top-up/booking/product workflows |
| Interbank simulator | No implementation found | MISSING | No local external-bank simulation |
| Offline maps / local road graph | No implementation found | MISSING | No map dataset or routing |
| Rule/intent chatbot | No implementation found | MISSING | No intent catalog or chat flow |
| Unit tests | No tracked test source found | MISSING IN INSPECTED SNAPSHOT | Gradle test task execution not verified |
| Instrumentation tests | No tracked androidTest source found | MISSING IN INSPECTED SNAPSHOT | No device/emulator test evidence |

## C01 - Currency representation

- `Account.balance` is `Double`.
- `HomeUiState.totalBalance` is `Double`.
- `.agent/work.md` A2.10 requires `Long` in VND and forbids Float/Double for money.
- `HomeScreen.kt` contains a private `formatAmount(Double, currency)` helper using Locale.US and two decimal places.
- The design reference ERD/class diagram also uses real/double for monetary columns/fields. That design artifact conflicts with A2.10 and needs reconciliation; Phase 0 does not change either source or design.

## C06 - Balance aggregation

`totalBalance` sums all account amounts regardless of currency. The displayed currency is taken from the first account, or defaults to EUR. There is no exchange-rate conversion or invariant enforcing a common currency. Mixed-currency totals are therefore invalid. The preview data happens to use EUR for both examples, but preview fixtures do not establish a general business rule.

## Security note C07

Debug network BODY logging is enabled. Manifest backup is enabled with exclusions for only `sharedpref/auth.xml`; future banking database/file backup policy is not comprehensively demonstrated.

## Transaction-authentication decisions to preserve

The project decision recorded in prior design context is:
- Ordinary transfers use Android Device Authentication (the device's configured biometric or device credential).
- High-value transfers use the app's in-app face-recognition flow based on MediaPipe Face Detection/Face Landmarker plus MobileFaceNet.
- High-value threshold remains TBD.
- Whether device authentication is also required for high-value transfers remains TBD.
- The assignment's 2FA requirement must not be considered satisfied by a single biometric mechanism without an approved factor mapping.

These decisions are not implemented in the inspected source.

No application source or configuration was changed in this Phase 0 documentation update.
