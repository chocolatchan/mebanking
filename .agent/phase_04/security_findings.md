# Task 4.3 — Security Findings Log

**Date:** 2026-11-22  
**Role:** Independent Security Reviewer  

---

## Vulnerability Summary

| Severity | Count | Status |
|:---:|:---:|:---:|
| **CRITICAL** | 0 | None |
| **HIGH** | 0 | None |
| **MEDIUM** | 0 | None |
| **LOW / INFO** | 2 | Cataloged & Documented |

---

## Detailed Findings

### SEC-01 (Informational) — Unconfirmed High-Value Transaction 2FA Policy
- **Description:** Vietnamese banking regulation (Decision 2345/QĐ-NHNN) mandates biometric verification for transactions exceeding 10,000,000 VND. The system implements the mechanism via `FaceRecognitionService` and `TransactionAuthenticationPolicy`, but the exact threshold value is marked `PendingPolicy` (OQ-3.6-1).
- **Impact:** System defaults to fail-closed behavior for unconfigured policies.
- **Remediation:** Product owner must finalize the high-value transaction threshold in `DefaultTransactionAuthenticationPolicy`.

### SEC-02 (Informational) — Android Keystore Availability on JVM / Headless Runtimes
- **Description:** `FaceTemplateCipher` utilizes `AndroidKeyStore` with `AES-256-GCM`. On test runtimes without a hardware security module or keystore provider, template persistence to disk is skipped to avoid writing unencrypted vectors.
- **Impact:** Templates are held in volatile memory during local test runs.
- **Remediation:** Architecture already implements zero-disk fallback; behavior verified in `Tier2FaceBiometricBoundaryE2ETest`.
