# Comprehensive Security Audit Report (Phase 5 — Task 5.4)

**Project:** `m.ebanking`  
**Security Classification:** Level-3 Core Mobile Banking & Cryptographic Security  
**Audit Date:** 2026-11-23  
**Status:** **CLEARED (No Critical or High Vulnerabilities Remaining)**

---

## 1. Security Architecture Summary

The ME Banking security posture was designed to prevent unauthorized financial access, data leakage, and cryptographic replay attacks in a pure offline-first architecture.

```
+-----------------------------------------------------------------------------------+
|                               Security Architecture                               |
+-----------------------------------------------------------------------------------+
|  [ User Input / Authentication ]                                                  |
|     |                                                                             |
|     +--> PasswordHasher: PBKDF2WithHmacSHA256 (64-byte salt, 10,000 iterations)   |
|     +--> DeviceAuthenticator: AndroidX BiometricPrompt (Strong Biometric / Pass)  |
|     +--> In-App OTP: CSPRNG 6-digits (30s rotation, 60s expiry, constant-time eq)  |
|                                                                                   |
|  [ Data Protection at Rest ]                                                      |
|     |                                                                             |
|     +--> FaceTemplateCipher: AES-256 GCM in Android Keystore + Memory Zeroization |
|     +--> NationalIdProtector: AES-256 GCM (`cipher`) + HMAC-SHA256 (`hash` index) |
|     +--> Room SQLite DB: SQLite Invariant Triggers (Balance >= 0, Amount > 0)     |
|     +--> Android Manifest: allowBackup=false, fullBackupContent=false             |
|                                                                                   |
|  [ Transaction Authorization & Anti-Bypass ]                                      |
|     |                                                                             |
|     +--> Single-Use AuthorizationGrant (60s lifetime, consumed immediately)       |
|     +--> Durable Idempotency: Unique Index `index_transactions_idempotency_key`   |
|     +--> Role-Based Access Control (RBAC): Customer vs Officer strict isolation   |
+-----------------------------------------------------------------------------------+
```

---

## 2. In-Depth Security Audit by Category

### A. Password & Credential Security
- **Algorithm:** PBKDF2WithHmacSHA256 with 10,000 iterations and 64-byte CSPRNG salt.
- **Format:** Versioned format `$v1$<salt>$<hash>`.
- **Finding:** No plaintext passwords stored in database or memory. ViewModels zero out password fields upon successful login or customer creation.
- **Verdict:** **PASS**.

### B. Biometric Face Template Encryption (DEF-Phase 3/4 Fix)
- **Previous Vulnerability in Legacy Branch:** The original branch used a trivial XOR obfuscation: `(userId.toInt() xor 0x5A)` which represented a High vulnerability.
- **Remediation in Current Implementation:** Replaced with [FaceTemplateCipher.kt](file:///home/chocolat/Documents/Project/m.ebanking/app/src/main/java/com/mebanking/app/domain/service/FaceTemplateCipher.kt):
  - Uses **AES-256 GCM** with keys generated and stored inside the hardware-backed **Android Keystore** (`AndroidKeyStore`).
  - Stored in app-private storage (`context.filesDir/biometrics`).
  - **Memory Zeroization:** `bytes.fill(0)` and `FloatArray.fill(0f)` immediately executed after template read/write/delete operations.
- **Verdict:** **PASS**.

### C. Sensitive Customer Identity Data (National ID / CCCD)
- **Protection Mechanism:** [NationalIdProtector.kt](file:///home/chocolat/Documents/Project/m.ebanking/app/src/main/java/com/mebanking/app/data/local/security/NationalIdProtector.kt):
  - **At Rest:** AES-256 GCM authenticated encryption stored in `national_id_cipher`.
  - **Search & Uniqueness:** Keyed HMAC-SHA256 digest stored in `national_id_hash` with SQLite unique index (`index_users_national_id_hash`).
  - Prevents plaintext exposure in SQLite database dumps or memory inspection while preserving exact-match customer lookup for officers.
- **Verdict:** **PASS**.

### D. Transaction Authorization & Anti-Bypass (QĐ 2345/QĐ-NHNN)
- **Policy Enforcement:**
  - Ordinary transactions `<= 10.000.000 VND`: Gated by System Auth + In-App OTP challenge.
  - High-value transactions `> 10.000.000 VND`: Gated by System Auth + Face Verification against enrolled biometric template.
- **Anti-Replay / Anti-Bypass Grant Mechanism:**
  - Upon face match, `DefaultTransactionAuthenticationPolicy` issues an `AuthorizationGrant` bound to `(idempotencyKey, userId, amount)`.
  - Grant expires after **60 seconds**.
  - When `executeTransfer()` is called, the grant is **consumed and removed immediately**. It cannot be re-used, replayed, or transferred to another transaction.
- **Verdict:** **PASS**.

### E. Idempotency & Financial Double-Debit Prevention
- **Durable Persistence (Room Schema v2):**
  - Database schema includes unique index `index_transactions_idempotency_key` on the `transactions` table.
  - Dedup occurs within SQLite `@Transaction`: if a transaction with the same key already exists, `DefaultTransactionRepository` immediately returns the existing record without second debit.
  - Verified across process termination and service re-instantiation in `TransactionServiceTest`.
- **Verdict:** **PASS**.

### F. Role-Based Access Control (RBAC)
- **Enforcement:**
  - Customers cannot access officer dashboard or customer management methods (`DomainError.Forbidden`).
  - Customers can only access accounts owned by their `userId` (`session.role == Role.CUSTOMER && account.userId != session.userId` returns `Forbidden`).
  - Officers cannot execute personal customer transfers.
- **Verdict:** **PASS**.

### G. Android App Backup & Extraction Rules
- **Configuration:** [AndroidManifest.xml](file:///home/chocolat/Documents/Project/m.ebanking/app/src/main/AndroidManifest.xml):
  - `android:allowBackup="false"`
  - `android:fullBackupContent="false"`
  - `data_extraction_rules.xml` excludes cloud backup and device-to-device transfer of database and biometric files.
- **Verdict:** **PASS**.

---

## 3. Vulnerability Status Table

| Vulnerability ID | Description | Severity | Remediation Status |
| :--- | :--- | :---: | :---: |
| **SEC-01** | Biometric face template XOR obfuscation | High | **RESOLVED** (Replaced with AES-256 GCM in Android Keystore) |
| **SEC-02** | Plaintext National ID (CCCD) in SQLite | High | **RESOLVED** (Encrypted with AES-256 GCM + HMAC index) |
| **SEC-03** | In-memory only transaction idempotency | High | **RESOLVED** (SQLite unique index + Room migration v2) |
| **SEC-04** | High-value transfer authentication bypass | High | **RESOLVED** (Mandatory face match + 60s single-use grant) |
| **SEC-05** | Negative balance / negative amount injection | High | **RESOLVED** (SQLite RAISE(ABORT) triggers + domain validation) |
| **SEC-06** | Cross-customer account access | Medium | **RESOLVED** (Session ownership checks in all services) |

---

## 4. Security Audit Verdict
**AUDIT PASSED: ZERO CRITICAL / HIGH VULNERABILITIES REMAINING.**
