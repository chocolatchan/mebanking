package com.mebanking.app.data.repository

import com.mebanking.app.data.local.dao.UserDao
import com.mebanking.app.data.local.entity.UserEntity
import com.mebanking.app.data.local.security.NationalIdProtector
import com.mebanking.app.domain.model.DomainError
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.User
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface AuthRepository {
    suspend fun getUserByUsername(username: String): User?
    suspend fun getUserById(userId: Long): User?
    suspend fun getPasswordHashByUsername(username: String): String?
    suspend fun createUser(
        username: String,
        passwordHash: String,
        fullName: String,
        email: String,
        phone: String,
        role: Role,
        kycVerified: Boolean = false,
    ): Result<User>
    suspend fun updateProfile(
        userId: Long,
        fullName: String,
        email: String,
        phone: String,
        kycVerified: Boolean? = null,
    ): Result<User>
    suspend fun updateCustomerEkyc(
        userId: Long,
        fullName: String? = null,
        nationalIdCipher: String? = null,
        nationalIdHash: String? = null,
        kycVerified: Boolean = true,
        faceImagePath: String? = null,
    ): Result<User> = Result.failure(DomainError.SystemError("Unsupported operation"))
    suspend fun updatePasswordHash(userId: Long, newHash: String): Result<Unit>
    suspend fun isUsernameTaken(username: String): Boolean
    fun observeAllUsers(): Flow<List<User>>
}

class DefaultAuthRepository(
    private val userDao: UserDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val nationalIdProtector: NationalIdProtector? = null,
) : AuthRepository {

    private fun UserEntity.toRichDomainModel(): User {
        val decryptedNationalId = nationalIdCipher?.let { cipher ->
            runCatching { nationalIdProtector?.decrypt(cipher) }.getOrNull()
        }
        return toDomainModel().copy(nationalId = decryptedNationalId)
    }

    override suspend fun getUserByUsername(username: String): User? = withContext(ioDispatcher) {
        userDao.getByUsername(username)?.toRichDomainModel()
    }

    override suspend fun getUserById(userId: Long): User? = withContext(ioDispatcher) {
        userDao.getById(userId)?.toRichDomainModel()
    }

    override suspend fun getPasswordHashByUsername(username: String): String? = withContext(ioDispatcher) {
        userDao.getByUsername(username)?.passwordHash
    }

    override suspend fun createUser(
        username: String,
        passwordHash: String,
        fullName: String,
        email: String,
        phone: String,
        role: Role,
        kycVerified: Boolean,
    ): Result<User> = withContext(ioDispatcher) {
        try {
            val entity = UserEntity(
                username = username,
                passwordHash = passwordHash,
                fullName = fullName,
                email = email,
                phone = phone,
                role = role,
                kycVerified = kycVerified,
                createdAt = System.currentTimeMillis(),
            )
            val id = userDao.insert(entity)
            Result.success(entity.copy(id = id).toDomainModel())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(DomainError.SystemError("Database insertion error: ${e.message}", e))
        }
    }

    override suspend fun updateProfile(
        userId: Long,
        fullName: String,
        email: String,
        phone: String,
        kycVerified: Boolean?,
    ): Result<User> = withContext(ioDispatcher) {
        try {
            val existing = userDao.getById(userId)
                ?: return@withContext Result.failure(DomainError.NotFound("User not found with ID: $userId"))
            val updated = existing.copy(
                fullName = fullName,
                email = email,
                phone = phone,
                kycVerified = kycVerified ?: existing.kycVerified,
            )
            userDao.update(updated)
            Result.success(updated.toRichDomainModel())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(DomainError.SystemError("Database update error: ${e.message}", e))
        }
    }

    override suspend fun updateCustomerEkyc(
        userId: Long,
        fullName: String?,
        nationalIdCipher: String?,
        nationalIdHash: String?,
        kycVerified: Boolean,
        faceImagePath: String?,
    ): Result<User> = withContext(ioDispatcher) {
        try {
            val existing = userDao.getById(userId)
                ?: return@withContext Result.failure(DomainError.NotFound("User not found with ID: $userId"))
            if (!nationalIdHash.isNullOrBlank()) {
                val conflict = userDao.getByNationalIdHash(nationalIdHash)
                if (conflict != null && conflict.id != userId) {
                    return@withContext Result.failure(DomainError.InvalidData("CCCD đã được đăng ký cho tài khoản khác"))
                }
            }
            val updated = existing.copy(
                fullName = fullName?.trim()?.takeIf { it.length >= 2 } ?: existing.fullName,
                nationalIdCipher = nationalIdCipher ?: existing.nationalIdCipher,
                nationalIdHash = nationalIdHash ?: existing.nationalIdHash,
                kycVerified = kycVerified,
                faceImagePath = faceImagePath ?: existing.faceImagePath,
            )
            userDao.update(updated)
            Result.success(updated.toRichDomainModel())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(DomainError.SystemError("Database update error: ${e.message}", e))
        }
    }

    override suspend fun updatePasswordHash(userId: Long, newHash: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val existing = userDao.getById(userId)
                ?: return@withContext Result.failure(DomainError.NotFound("User not found with ID: $userId"))
            userDao.update(existing.copy(passwordHash = newHash))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(DomainError.SystemError("Failed to update password hash: ${e.message}", e))
        }
    }

    override suspend fun isUsernameTaken(username: String): Boolean = withContext(ioDispatcher) {
        userDao.getByUsername(username) != null
    }

    override fun observeAllUsers(): Flow<List<User>> {
        return userDao.getAll().map { list -> list.map { it.toDomainModel() } }
    }
}
