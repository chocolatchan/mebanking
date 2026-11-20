package com.mebanking.app.domain.service

import com.mebanking.app.data.local.dao.AccountDao
import com.mebanking.app.data.local.dao.UserDao
import com.mebanking.app.data.local.entity.AccountEntity
import com.mebanking.app.data.local.entity.UserEntity
import com.mebanking.app.data.local.security.PasswordHasher
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.model.DomainError
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.SessionState

interface OfficerCustomerService {
    suspend fun createCustomer(
        username: String,
        rawPassword: String,
        fullName: String,
        email: String,
        phone: String,
        kycVerified: Boolean = false,
    ): Result<Long>

    suspend fun updateCustomerProfile(
        targetUserId: Long,
        fullName: String,
        email: String,
        phone: String,
        kycVerified: Boolean? = null,
    ): Result<Unit>
}

class DefaultOfficerCustomerService(
    private val userDao: UserDao,
    private val accountDao: AccountDao,
    private val sessionManager: SessionManager,
    private val passwordHasher: PasswordHasher = PasswordHasher,
) : OfficerCustomerService {

    private val usernameRegex = Regex("^[a-zA-Z0-9_]{4,32}$")
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-zA-Z]{2,}$")
    private val phoneRegex = Regex("^0[0-9]{9}$")

    override suspend fun createCustomer(
        username: String,
        rawPassword: String,
        fullName: String,
        email: String,
        phone: String,
        kycVerified: Boolean,
    ): Result<Long> {
        val session = sessionManager.currentSession
            ?: return Result.failure(
                if (sessionManager.sessionState.value is SessionState.Expired)
                    DomainError.Unauthorized("Session has expired. Please log in again.")
                else
                    DomainError.Unauthorized("Authentication required")
            )

        if (session.role != Role.OFFICER) {
            return Result.failure(DomainError.Forbidden("Only bank officers can create customer accounts"))
        }

        val trimmedUsername = username.trim()
        val trimmedName = fullName.trim()
        val trimmedEmail = email.trim()
        val trimmedPhone = phone.trim()

        if (!usernameRegex.matches(trimmedUsername)) {
            return Result.failure(DomainError.InvalidData("Username must be 4-32 alphanumeric characters or underscores"))
        }
        if (rawPassword.length < 8) {
            return Result.failure(DomainError.InvalidData("Password must be at least 8 characters"))
        }
        if (trimmedName.length < 2) {
            return Result.failure(DomainError.InvalidData("Full name must be at least 2 characters"))
        }
        if (!emailRegex.matches(trimmedEmail)) {
            return Result.failure(DomainError.InvalidData("Invalid email address format"))
        }
        if (!phoneRegex.matches(trimmedPhone)) {
            return Result.failure(DomainError.InvalidData("Phone number must be a valid 10-digit number starting with 0"))
        }

        if (userDao.getByUsername(trimmedUsername) != null) {
            return Result.failure(DomainError.InvalidData("Username '$trimmedUsername' is already taken"))
        }

        val versionedHash = passwordHasher.hashPasswordV1(rawPassword)

        val userEntity = UserEntity(
            username = trimmedUsername,
            passwordHash = versionedHash,
            fullName = trimmedName,
            email = trimmedEmail,
            phone = trimmedPhone,
            role = Role.CUSTOMER,
            kycVerified = kycVerified,
            createdAt = System.currentTimeMillis(),
        )
        val newUserId = userDao.insert(userEntity)

        // Automatically create initial checking account
        val accountNumber = "100" + String.format("%07d", newUserId)
        accountDao.insert(
            AccountEntity(
                userId = newUserId,
                accountNumber = accountNumber,
                accountType = AccountType.CHECKING,
                balance = 0L,
                currency = "VND",
                status = "ACTIVE",
            )
        )

        return Result.success(newUserId)
    }

    override suspend fun updateCustomerProfile(
        targetUserId: Long,
        fullName: String,
        email: String,
        phone: String,
        kycVerified: Boolean?,
    ): Result<Unit> {
        val session = sessionManager.currentSession
            ?: return Result.failure(
                if (sessionManager.sessionState.value is SessionState.Expired)
                    DomainError.Unauthorized("Session has expired. Please log in again.")
                else
                    DomainError.Unauthorized("Authentication required")
            )

        if (session.role != Role.OFFICER) {
            return Result.failure(DomainError.Forbidden("Only bank officers can update customer profiles"))
        }

        val existing = userDao.getById(targetUserId)
            ?: return Result.failure(DomainError.NotFound("User not found with ID: $targetUserId"))

        if (existing.role != Role.CUSTOMER) {
            return Result.failure(DomainError.Forbidden("Officers cannot modify non-customer accounts via customer update"))
        }

        val trimmedName = fullName.trim()
        val trimmedEmail = email.trim()
        val trimmedPhone = phone.trim()

        if (trimmedName.length < 2) {
            return Result.failure(DomainError.InvalidData("Full name must be at least 2 characters"))
        }
        if (!emailRegex.matches(trimmedEmail)) {
            return Result.failure(DomainError.InvalidData("Invalid email address format"))
        }
        if (!phoneRegex.matches(trimmedPhone)) {
            return Result.failure(DomainError.InvalidData("Phone number must be a valid 10-digit number starting with 0"))
        }

        val updated = existing.copy(
            fullName = trimmedName,
            email = trimmedEmail,
            phone = trimmedPhone,
            kycVerified = kycVerified ?: existing.kycVerified,
        )
        userDao.update(updated)
        return Result.success(Unit)
    }
}
