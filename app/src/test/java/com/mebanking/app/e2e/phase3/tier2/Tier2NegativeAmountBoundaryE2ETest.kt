package com.mebanking.app.e2e.phase3.tier2

import android.database.sqlite.SQLiteConstraintException
import com.mebanking.app.e2e.phase3.harness.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Tier 2 Boundary Category 2: Zero & Negative Amounts
 * Total: 5 test cases
 */
class Tier2NegativeAmountBoundaryE2ETest : BasePhase3E2ETest() {

    @Test
    fun testTransferZeroAmountRejection() = runTest {
        val initialChecking = container.accountDao.getById(
            CUSTOMER_CHECKING_ACCOUNT_ID
        )?.balance ?: 0L

        val request = TransferRequest(
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            toAccountId = CUSTOMER_SAVING_ACCOUNT_ID,
            amount = 0L,
            idempotencyKey = "ZERO-XFR-1",
        )
        val result = container.transactionService.executeTransfer(request, customerSession)
        assertTrue("Transfer with 0 VND must fail", result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.InvalidData)

        val balanceAfter = container.accountDao.getById(
            CUSTOMER_CHECKING_ACCOUNT_ID
        )?.balance
        assertEquals(initialChecking, balanceAfter)
    }

    @Test
    fun testTransferNegativeAmountRejection() = runTest {
        val initialChecking = container.accountDao.getById(
            CUSTOMER_CHECKING_ACCOUNT_ID
        )?.balance ?: 0L

        val request = TransferRequest(
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            toAccountId = CUSTOMER_SAVING_ACCOUNT_ID,
            amount = -500_000L,
            idempotencyKey = "NEG-XFR-1",
        )
        val result = container.transactionService.executeTransfer(request, customerSession)
        assertTrue("Transfer with negative VND amount must fail", result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.InvalidData)

        val balanceAfter = container.accountDao.getById(
            CUSTOMER_CHECKING_ACCOUNT_ID
        )?.balance
        assertEquals(initialChecking, balanceAfter)
    }

    @Test
    fun testDepositZeroAmountRejection() = runTest {
        val initialChecking = container.accountDao.getById(
            CUSTOMER_CHECKING_ACCOUNT_ID
        )?.balance ?: 0L

        val result = container.transactionService.executeDeposit(
            accountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            amount = 0L,
            idempotencyKey = "ZERO-DEP-1",
            session = customerSession,
        )
        assertTrue("Deposit with 0 VND must fail", result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.InvalidData)

        val balanceAfter = container.accountDao.getById(
            CUSTOMER_CHECKING_ACCOUNT_ID
        )?.balance
        assertEquals(initialChecking, balanceAfter)
    }

    @Test
    fun testSqliteTriggerRejectsNegativeBalanceOnUpdate() {
        try {
            db.openHelper.writableDatabase.execSQL(
                "UPDATE accounts SET balance = -100 WHERE id = 1"
            )
            fail("Expected SQLiteConstraintException from trigger_account_balance_non_negative_update")
        } catch (e: Exception) {
            assertTrue(
                "Must be SQLiteConstraintException",
                e is SQLiteConstraintException || e.cause is SQLiteConstraintException ||
                    e.message?.contains("negative", ignoreCase = true) == true,
            )
        }
    }

    @Test
    fun testSqliteTriggerRejectsNegativeBalanceOnInsert() {
        try {
            db.openHelper.writableDatabase.execSQL(
                """
                INSERT INTO accounts (user_id, account_number, account_type, balance, currency, status, created_at)
                VALUES (1, '999_NEG_INSERT', 'CHECKING', -500000, 'VND', 'ACTIVE', 1000)
                """.trimIndent()
            )
            fail("Expected SQLiteConstraintException from trigger_account_balance_non_negative_insert")
        } catch (e: Exception) {
            assertTrue(
                "Must be SQLiteConstraintException",
                e is SQLiteConstraintException || e.cause is SQLiteConstraintException ||
                    e.message?.contains("negative", ignoreCase = true) == true,
            )
        }
    }
}
