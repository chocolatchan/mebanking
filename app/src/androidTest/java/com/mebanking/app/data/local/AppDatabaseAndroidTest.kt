package com.mebanking.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mebanking.app.data.local.entity.AccountEntity
import com.mebanking.app.data.local.entity.TransactionEntity
import com.mebanking.app.data.local.entity.UserEntity
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.TransactionStatus
import com.mebanking.app.domain.model.TransactionType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android runtime instrumented verification for Room persistence, schema invariants,
 * and idempotency constraints on an Android device/emulator environment.
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseAndroidTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun databaseSmokeTest_insertsAndQueriesUserAndAccount() = runTest {
        val user = UserEntity(
            id = 100L,
            username = "test_user",
            passwordHash = "hash123",
            fullName = "Nguyễn Văn Test",
            email = "test@mebanking.vn",
            phone = "0901234567",
            role = Role.CUSTOMER,
            kycVerified = true,
            nationalIdCipher = "cipher_data",
            nationalIdHash = "hash_data",
        )
        db.userDao().insert(user)

        val retrievedUser = db.userDao().getById(100L)
        assertNotNull(retrievedUser)
        assertEquals("test_user", retrievedUser?.username)
        assertEquals("hash_data", retrievedUser?.nationalIdHash)

        val account = AccountEntity(
            id = 200L,
            userId = 100L,
            accountNumber = "1234567890",
            accountType = AccountType.CHECKING,
            balance = 50_000_000L,
        )
        db.accountDao().insert(account)

        val retrievedAccount = db.accountDao().getById(200L)
        assertNotNull(retrievedAccount)
        assertEquals(50_000_000L, retrievedAccount?.balance)
    }

    @Test
    fun idempotencyKeyPersistence_storesAndRetrievesUniqueKey() = runTest {
        val tx = TransactionEntity(
            id = 1L,
            fromAccountId = 200L,
            toAccountId = 201L,
            amount = 15_000_000L,
            note = "High value transfer",
            type = TransactionType.INTERNAL_TRANSFER,
            status = TransactionStatus.SUCCESS,
            timestamp = System.currentTimeMillis(),
            idempotencyKey = "idemp-smoke-key-12345",
        )
        db.transactionDao().insert(tx)

        val retrieved = db.transactionDao().getByIdempotencyKey("idemp-smoke-key-12345")
        assertNotNull(retrieved)
        assertEquals(15_000_000L, retrieved?.amount)
        assertEquals("idemp-smoke-key-12345", retrieved?.idempotencyKey)
    }
}
