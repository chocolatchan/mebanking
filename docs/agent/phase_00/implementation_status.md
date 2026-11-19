# Implementation Status (Phase 0 Snapshot)

- Inspected ref: `master`
- Commit: `efc15e138cb0bc4f3420e8b2caedb29fdfb36299`
- Assessment basis: source inspection, not runtime verification.

Status meanings:
- **IMPLEMENTED (preliminary):** concrete code exists for the narrow behavior described.
- **PARTIAL:** a slice exists, but the project requirement is not met end-to-end.
- **MISSING:** no corresponding implementation was found in the inspected tree.
- **UNVERIFIED:** code or environment evidence is insufficient.

| Feature / capability | Source evidence | Preliminary status | Gap / caveat |
|---|---|---|---|
| Android app entry | `MainActivity.kt` | IMPLEMENTED | Single activity only |
| Compose Material 3 theme | `ui/theme/*` | IMPLEMENTED | Visual foundation, not feature completion |
| Home screen | `HomeScreen.kt` | PARTIAL | Displays account data; no complete banking workflow |
| Home loading/error/empty states | `HomeScreen.kt`, `HomeUiState.kt` | IMPLEMENTED | Error policy not audited |
| Account DTO | `Account.kt` | PARTIAL | API DTO only; uses Double and sample currency conventions |
| Account API contract | `BankingApi.kt` | PARTIAL | Two GET endpoints only; backend not included |
| HTTP client setup | `NetworkModule.kt` | IMPLEMENTED | Uses placeholder base URL; debug BODY logging |
| Account repository | `AccountRepository.kt` | PARTIAL | Remote-only delegation; no local source/cache |
| ViewModel state flow | `HomeViewModel.kt` | PARTIAL | Loads account list; no broader domain use cases |
| App navigation | `MEbankingApp.kt` | MISSING | Home screen is rendered directly |
| Authentication / authorization | No matching implementation found | MISSING | No login, session, Customer/Officer roles |
| eKYC | No matching implementation found | MISSING | No identity verification flow |
| Room / SQLite | No local database files or dependencies found | MISSING | No entities, DAO, database, migration |
| Checking / Saving / Mortgage business logic | No implementation found | MISSING | Account display DTO does not implement account products |
| Deposit / withdraw / transfer | No implementation found | MISSING | No transaction domain/service/ledger |
| Transaction 2FA policy | No implementation found | MISSING | Project policy decisions remain governed by work.md |
| Android Device Authentication | No implementation found | MISSING | No BiometricPrompt/device credential integration |
| In-app face recognition | No implementation/dependency found | MISSING | No MediaPipe or MobileFaceNet integration |
| Utility simulators | No implementation found | MISSING | No bill/top-up/booking/product workflows |
| Interbank simulator | No implementation found | MISSING | No external-bank simulation |
| Offline map / local road graph | No implementation/dependency found | MISSING | No map dataset or routing implementation |
| Rule/intent chatbot | No implementation found | MISSING | No intent catalog or chat flow |
| Unit tests | No tracked test sources found | MISSING in repository snapshot | Test task execution still unverified |
| Instrumentation tests | No tracked androidTest sources found | MISSING in repository snapshot | Device/emulator execution not performed |

## Important correctness and policy observations

1. `.agent/work.md` A2.10 says money must be represented as `Long` in VND units. Current `Account.balance` and `HomeUiState.totalBalance` use `Double`. This is a concrete design mismatch to resolve in a later approved task, not changed during Phase 0.
2. The project task pack requires offline-first Room/SQLite. Current account reads are network-only.
3. The project task pack states all transactions must satisfy the assignment's 2FA requirement and prohibits assuming a single biometric mechanism is automatically 2FA. No transaction-auth implementation exists to assess.
4. Current API URL is a placeholder. The existence of Retrofit code is not evidence of a working backend.
5. Home screen preview data is not production/test-fixture evidence for business rules.

No feature is marked complete merely because a UI or class name exists.
