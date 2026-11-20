package com.mebanking.app.data.repository

import com.mebanking.app.data.local.dao.AccountDao
import com.mebanking.app.domain.model.Account
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth (SSOT) repository mediating between ViewModels/Domain and Room AccountDao.
 * Exposes Flow for continuous observation and suspend functions for one-shot operations.
 */
interface AccountRepository {
    fun observeAccountsByUserId(userId: Long): Flow<List<Account>>
    fun observeAccounts(userId: Long): Flow<List<Account>> = observeAccountsByUserId(userId)
    fun observeAllAccounts(): Flow<List<Account>>

    suspend fun getAccountById(id: Long): Account?
    suspend fun getAccount(id: Long): Account? = getAccountById(id)
    suspend fun getAccountByNumber(accountNumber: String): Account?
    suspend fun getAccountsByUserIdSync(userId: Long): List<Account>
    suspend fun getAllAccountsSync(): List<Account>

    suspend fun createAccount(account: Account): Long
    suspend fun updateAccount(account: Account): Int
    suspend fun count(): Int

    // Backward compatibility for legacy UI models
    suspend fun getAccounts(): List<com.mebanking.app.data.model.Account>
    suspend fun getAccount(id: String): com.mebanking.app.data.model.Account
}

/**
 * Factory function allowing instantiation via AccountRepository(accountDao).
 */
fun AccountRepository(
    accountDao: AccountDao,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
): AccountRepository = DefaultAccountRepository(accountDao, ioDispatcher)
