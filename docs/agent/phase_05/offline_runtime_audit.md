# Offline Runtime Audit Report (Phase 5 — Task 5.6)

**Project:** `m.ebanking`  
**Execution Environment:** Pure Offline Mobile Runtime (Airplane Mode Compliant)  
**Audit Date:** 2026-11-23  
**Status:** **100% OFFLINE-FIRST COMPLIANT (Zero External Network Dependencies)**

---

## 1. Offline Architecture Design

Per project requirements, ME Banking operates entirely offline without connecting to external cloud services, third-party payment gateways, remote authentication APIs, or external tile map servers.

```
+-----------------------------------------------------------------------------------+
|                         Pure Offline-First Architecture                           |
+-----------------------------------------------------------------------------------+
|  [ Presentation Layer (Jetpack Compose) ]                                         |
|     |                                                                             |
|     +--> Cash / Transfer / History / Officer / Lifestyle Screens                  |
|     +--> Local StateFlow and ViewModel Lifecycle Management                       |
|                                                                                   |
|  [ Domain Layer (Pure Local Algorithms) ]                                         |
|     |                                                                             |
|     +--> DeviceAuthenticator: Android BiometricPrompt (On-Device Keyguard)         |
|     +--> OtpService: Local CSPRNG (SecureRandom 6-digit rotation)                 |
|     +--> FaceRecognitionService: Local Cosine Similarity Matching                 |
|     +--> ChatbotService: Offline Rule-Based Intent Matching                       |
|     +--> PathFinding: Offline Dijkstra / A* Routing on Local GeoJSON Graph        |
|                                                                                   |
|  [ Data Layer (App-Private Storage) ]                                             |
|     |                                                                             |
|     +--> SQLite / Room: In-Memory / File Database (`mebanking.db`)                |
|     +--> Android Keystore: AES-256 GCM Keys for Face & National ID                |
|     +--> Assets: Pre-bundled Seed Data (`banks.json`, `hanoi_roads.json`, etc.)   |
+-----------------------------------------------------------------------------------+
```

---

## 2. Component-by-Component Offline Audit

### A. Core Banking Workflows
- **Operation:** Account creation, deposit, withdrawal, internal transfer, interbank transfer.
- **Offline Implementation:**
  - Balances and ledger entries are stored in SQLite via Room.
  - Interbank transfers use local `ExternalBankEntity` seeded from `banks.json` asset.
  - Zero HTTP client calls or remote REST endpoints (`Retrofit`/`OkHttp` network calls removed).
- **Offline Status:** **PASS**.

### B. Device Authentication & In-App OTP
- **Operation:** Screen lock / Biometric authentication & transaction OTP challenge.
- **Offline Implementation:**
  - `DeviceAuthenticator` queries the local Android Keyguard and BiometricManager.
  - `OtpService` generates 6-digit OTP using `java.security.SecureRandom`, checks against local in-memory/Room state.
  - Requires no SMS gateway or network server.
- **Offline Status:** **PASS**.

### C. eKYC & Biometric Face Verification
- **Operation:** Face detection, template extraction, matching, and template storage.
- **Offline Implementation:**
  - Operates on local probe byte array.
  - Pre-registered template read from app-private files encrypted via Android Keystore.
  - Matching computed locally via cosine similarity vector math.
- **Offline Status:** **PASS**.

### D. Offline Map Navigation & Branch Locator
- **Operation:** Finding nearest bank branch and computing route.
- **Offline Implementation:**
  - Branch locations stored in Room `branches` table.
  - Road graph loaded from local asset `hanoi_roads.json` via `RoadGraphLoader`.
  - Shortest path computed on-device using Dijkstra / A* algorithm in `DijkstraRouter.kt` and `AStarPathFinder.kt`.
- **Offline Status:** **PASS**.

### E. AI Chatbot Assistant
- **Operation:** Natural language banking FAQ and intent resolution.
- **Offline Implementation:**
  - Pre-seeded intent catalog in Room `chat_intents` table.
  - Rule-based keyword and regex classifier in `DefaultChatbotService.kt`.
  - Operates without cloud LLM or network requests.
- **Offline Status:** **PASS**.

### F. Lifestyle Simulators
- **Operation:** Flights, Hotels, Cinema, Shopping, Mobile Top-up.
- **Offline Implementation:**
  - Catalogs seeded into Room by [LifestyleDatabaseSeeder.kt](file:///home/chocolat/Documents/Project/m.ebanking/app/src/main/java/com/mebanking/app/data/local/seed/LifestyleDatabaseSeeder.kt).
  - All bookings, seat reservations, and cart checkouts operate strictly in SQLite.
- **Offline Status:** **PASS**.

---

## 3. Fresh Installation & Airplane Mode Verification
- **Fresh Install Boot:** `DatabaseSeeder` and `LifestyleDatabaseSeeder` automatically populate default accounts, demo users (customer1, officer1), branches, products, and flight schedules upon first database launch.
- **Airplane Mode Simulation:** Verified in `OfflineVerificationTest`: all services initialize and run successfully when network is completely disabled.
- **Verdict:** **PASS**.
