# Phase 3 — Open Questions & Blockers

**Last Updated**: 2026-11-21

These questions must be answered before the corresponding features can be considered complete. All affected code paths are currently marked with `OPEN` or `BLOCKED` comments in source.

---

## Financial Formula Questions (Task 3.4)

### OQ-3.4-1: Saving interest formula

**Affects:** `FinancialFormulas.kt`, `AccountService` interest calculation  
**Question:** Which interest formula should be used for Saving accounts?
- Simple interest: `I = P × r × t`
- Compound interest: `A = P × (1 + r/n)^(nt)`

**Current state:** `FinancialFormulas.kt` returns `NOT_IMPLEMENTED_PENDING_APPROVAL`. Saving interest display is blocked.

---

### OQ-3.4-2: Saving compounding frequency

**Affects:** `FinancialFormulas.kt`  
**Question:** If compound interest is chosen (OQ-3.4-1), what is the compounding period?
- Daily / Monthly / Quarterly / Annually / At maturity

**Current state:** Blocked on OQ-3.4-1.

---

### OQ-3.4-3: Rounding rule for Saving interest

**Affects:** `FinancialFormulas.kt`, `SafeMoneyMath`  
**Question:** How should fractional VND be handled in interest calculations?
- Round down (floor) to nearest VND
- Round to nearest VND (half-up)
- Truncate

**Current state:** No rounding applied until formula approved. All VND stored as `Long`.

---

### OQ-3.4-4: Mortgage amortization formula

**Affects:** `FinancialFormulas.kt`, `AccountService`  
**Question:** Which mortgage repayment method?
- Standard amortization (fixed payment, decreasing interest portion)
- Equal principal payments (decreasing total payment)
- Interest-only with balloon payment

**Current state:** `FinancialFormulas.kt` returns `NOT_IMPLEMENTED_PENDING_APPROVAL`. Monthly payment calculation blocked.

---

### OQ-3.4-5: Mortgage rate type

**Affects:** `FinancialFormulas.kt`  
**Question:** Fixed rate or variable rate?  
If variable: What is the adjustment trigger? What cap applies?

**Current state:** Blocked on OQ-3.4-4.

---

## Transaction Authentication Questions (Task 3.6)

### OQ-3.6-1: High-value transaction threshold

**Affects:** `DefaultTransactionAuthenticationPolicy`  
**Question:** At what VND amount does a transaction become "high-value" and require Face Recognition instead of (or in addition to) Android Biometric Prompt?

**Example values to decide:** 5,000,000 ₫ / 10,000,000 ₫ / 50,000,000 ₫

**Current state:** `PendingPolicy` returned for all threshold-dependent decisions.

---

### OQ-3.6-2: Additional device auth for high-value transactions

**Affects:** `DefaultTransactionAuthenticationPolicy`  
**Question:** For high-value transactions, is Face Recognition *alone* sufficient, or is an additional Android BiometricPrompt / Device PIN required afterward?

**Current state:** `PendingPolicy` — no combined auth flow implemented.

---

### OQ-3.6-3: 2FA compliance mapping

**Affects:** `DefaultTransactionAuthenticationPolicy`, security review  
**Question:** Does the combination of Face Recognition + Android BiometricPrompt constitute compliant 2FA for the purposes of this application's security requirements?

**Note:** These are two biometric factors from the same device (same possession factor). True 2FA typically requires factors from different categories (possession + inherence, or possession + knowledge).

**Current state:** No 2FA compliance claim made. `PendingPolicy` used.

---

## Simulator Business Rules (Task 3.7)

### OQ-3.7-1: Utility / ticket pricing and fees

**Affects:** `DefaultSimulatorService`, `SimulatorRepository`  
**Question:** What are the prices, service fees, and billing amounts for each simulated utility?
- Electricity: pricing tier structure?
- Water: price per cubic meter?
- Phone top-up: face value = charged amount?
- Event tickets: price per ticket, booking fee?
- Shopping: product catalog / pricing?

**Current state:** Simulators accept user-provided amounts. No server-authoritative pricing applied.

---

### OQ-3.7-2: Refund and cancellation policy

**Affects:** `DefaultSimulatorService`, order lifecycle  
**Question:**
- Can a PAID order be cancelled and refunded?
- If yes: within what window? What fee applies?
- Does a COMPLETED order allow any recourse?

**Current state:** Order lifecycle supports `CREATED → PENDING_PAYMENT → PAID → COMPLETED`. No cancellation/refund transition implemented.

---

## Resolution Process

When an open question is resolved:
1. Update this document with the approved answer and date.
2. Remove the `OPEN`/`BLOCKED` annotation in source.
3. Implement the approved behavior.
4. Add or update tests to cover the implemented behavior.
5. Update `phase3_progress.md` task status.
