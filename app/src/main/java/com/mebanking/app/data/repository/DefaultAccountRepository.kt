package com.mebanking.app.data.repository

import com.mebanking.app.data.local.dao.AccountDao
import com.mebanking.app.data.local.entity.AccountEntity
import com.mebanking.app.domain.model.Account
import com.mebanking.app.domain.model.DomainError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DefaultAccountRepository(
    private val accountDao: AccountDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AccountRepository {

    override fun observeAccountsByUserId(userId: Long): Flow<List<Account>> {
        return accountDao.getAccountsByUserId(userId)
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(ioDispatcher)
    }

    override fun observeAllAccounts(): Flow<List<Account>> {
        return accountDao.getAll()
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun getAccountById(id: Long): Account? = withContext(ioDispatcher) {
        accountDao.getById(id)?.toDomainModel()
    }

    override suspend fun getAccountByNumber(accountNumber: String): Account? = withContext(ioDispatcher) {
        accountDao.getByAccountNumber(accountNumber)?.toDomainModel()
    }

    override suspend fun getAccountsByUserIdSync(userId: Long): List<Account> = withContext(ioDispatcher) {
        accountDao.getAccountsByUserIdSync(userId).map { it.toDomainModel() }
    }

    override suspend fun getAllAccountsSync(): List<Account> = withContext(ioDispatcher) {
        accountDao.getAll().first().map { it.toDomainModel() }
    }

    override suspend fun createAccount(account: Account): Long = withContext(ioDispatcher) {
        accountDao.insert(AccountEntity.fromDomainModel(account))
    }

    override suspend fun updateAccount(account: Account): Int = withContext(ioDispatcher) {
        accountDao.update(AccountEntity.fromDomainModel(account))
    }

    override suspend fun count(): Int = withContext(ioDispatcher) {
        accountDao.count()
    }

    override suspend fun getAccounts(): List<com.mebanking.app.data.model.Account> = withContext(ioDispatcher) {
        accountDao.getAll().first().map { it.toDomainModel().toLegacyAccount() }
    }

    override suspend fun getAccount(id: String): com.mebanking.app.data.model.Account = withContext(ioDispatcher) {
        val longId = id.toLongOrNull() ?: throw DomainError.InvalidData("Invalid account ID format: $id")
        val entity = accountDao.getById(longId) ?: throw DomainError.NotFound("Account not found for ID: $id")
        entity.toDomainModel().toLegacyAccount()
    }
}
