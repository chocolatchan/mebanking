# Financial Integrity & Recovery Audit Report (Phase 5 — Task 5.5)

**Project:** `m.ebanking`  
**Evaluation Scope:** Financial Arithmetic, Ledger Balance Invariants, Atomic Rollbacks, Crash Recovery, Concurrent Debit Handling.  
**Audit Date:** 2026-11-23  
**Integrity Status:** **VERIFIED (Strict Double-Debit & Partial Update Prevention)**

---

## 1. Core Financial Invariants

1. **Strict 64-bit Long Whole-VND Units:**
   - All account balances, transaction amounts, loan amounts, and bill amounts are strictly typed as 64-bit `Long`.
   - Floating-point representations (`Double`, `Float`) are forbidden from financial balances to prevent floating-point rounding errors (e.g. `0.1 + 0.2 != 0.3`).
   - Verified via `Tier2FinancialFormulaBoundaryE2ETest.testFractionalVndPrecisionRejection()`.

2. **SQLite Invariant Triggers ([AppDatabase.kt](file:///home/chocolat/Documents/Project/m.ebanking/app/src/main/java/com/mebanking/app/data/local/AppDatabase.kt)):**
   - `trigger_account_balance_non_negative_insert`: `WHEN NEW.balance < 0 BEGIN RAISE(ABORT, 'Account balance cannot be negative'); END;`
   - `trigger_account_balance_non_negative_update`: `BEFORE UPDATE OF balance ON accounts WHEN NEW.balance < 0 BEGIN RAISE(ABORT); END;`
   - `trigger_transaction_amount_positive_insert`: `BEFORE INSERT ON transactions WHEN NEW.amount <= 0 BEGIN RAISE(ABORT, 'Transaction amount must be strictly positive'); END;`
   - `trigger_bill_amount_positive_insert`: `BEFORE INSERT ON bills WHEN NEW.amount <= 0 BEGIN RAISE(ABORT); END;`

---

## 2. Recovery & Fault Injection Scenarios

### A. Partial Update Rollback (Atomic Ledger Consistency)
- **Scenario:** Database crash or constraint failure during a transfer after debiting the source account, but before crediting the destination or inserting the ledger entry.
- **Implementation:** Both legs of the transfer and the ledger insertion are executed strictly inside `database.withTransaction { ... }`.
- **Test Evidence:** `Phase4ProductionIntegrityTest.transfer_ledgerInsertFailure_rollsBackBalancesAndLedger()`:
  - Injected SQLite abort trigger on `transactions`.
  - Result: Source checking account balance remained exactly 50,000,000 VND, destination balance remained 100,000,000 VND, and 0 ledger entries were written.
  - Evaluation: **PASS**.

### B. Process Termination & Restart Recovery
- **Scenario:** The Android OS kills the application process while a transaction is in flight or immediately after commit. The user re-opens the app and retries the transaction.
- **Implementation:** Durable Room SQLite persistence with `idempotency_key` unique index (`index_transactions_idempotency_key`).
- **Test Evidence:** `TransactionServiceTest.idempotency_afterProcessRestart_reinstantiatedServiceReturnsPersistedResultWithoutDoubleDebit()`:
  - Tearing down and reinstantiating `DefaultTransactionService` over persistent database returns the cached transaction without double debiting.
  - Evaluation: **PASS**.

### C. Concurrent Transfers & Race Condition Defense
- **Scenario:** Two simultaneous transfer requests attempting to debit the same checking account with balance = 50,000,000 VND for 40,000,000 VND each.
- **Implementation:** SQLite transaction isolation and `RAISE(ABORT)` trigger prevent balance from dropping below 0. One transaction succeeds, the other fails with `DomainError.InsufficientFunds`.
- **Test Evidence:** `TransactionServiceConcurrencyTest`, `TransactionRepositoryTest`.
  - Evaluation: **PASS**.

### D. Booking Payment Recovery & Bill Repayment Prevention
- **Scenario:** User attempts to repay an already paid utility bill, or re-submit a hotel/flight booking.
- **Implementation:** `bill.status == BillStatus.PAID` immediately returns `DomainError.BillAlreadyPaid` without debiting. Lifestyle booking references are enforced via SQLite unique index `index_flight_bookings_booking_reference` and `index_hotel_bookings_booking_reference`.
- **Test Evidence:** `Phase4ProductionIntegrityTest.billPayment_newKeyAfterPaid_billAlreadyPaidWithoutSecondDebit()`.
  - Evaluation: **PASS**.

---

## 3. Financial Integrity Audit Verdict
All financial transactions are atomic, consistent, isolated, and durable (ACID). No partial updates or double-debit vulnerabilities exist.
**VERDICT: PASS.**
