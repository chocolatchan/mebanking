package com.mebanking.app.domain.service

import com.mebanking.app.data.local.dao.AccountDao
import com.mebanking.app.data.local.dao.UserDao
import com.mebanking.app.data.local.entity.AccountEntity
import com.mebanking.app.data.local.entity.UserEntity
import com.mebanking.app.data.local.security.PasswordHasher
import com.mebanking.app.data.local.security.NationalIdProtector
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.model.DomainError
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.SessionState

interface OfficerCustomerService {
    suspend fun searchCustomers(query: String): Result<List<com.mebanking.app.domain.model.User>>

    suspend fun createCustomer(
        username: String,
        rawPassword: String,
        fullName: String,
        email: String,
        phone: String,
        kycVerified: Boolean = false,
        nationalId: String? = null,
    ): Result<Long>

    suspend fun updateCustomerProfile(
        targetUserId: Long,
        fullName: String,
        email: String,
        phone: String,
        kycVerified: Boolean? = null,
        nationalId: String? = null,
    ): Result<Unit>

    suspend fun getCustomerAccounts(targetUserId: Long): Result<List<com.mebanking.app.domain.model.Account>> =
        Result.success(emptyList())

    suspend fun openAccountForCustomer(
        targetUserId: Long,
        accountType: AccountType,
        initialBalance: Long = 0L,
    ): Result<com.mebanking.app.domain.model.Account> =
        Result.failure(DomainError.SystemError("Tính năng mở tài khoản chưa được hỗ trợ"))
}

class DefaultOfficerCustomerService(
    private val userDao: UserDao,
    private val accountDao: AccountDao,
    private val sessionManager: SessionManager,
    private val passwordHasher: PasswordHasher = PasswordHasher,
    private val nationalIdProtector: NationalIdProtector? = null,
) : OfficerCustomerService {

    private val usernameRegex = Regex("^[a-zA-Z0-9_]{4,32}$")
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-zA-Z]{2,}$")
    private val phoneRegex = Regex("^0[0-9]{9}$")
    private val nationalIdRegex = Regex("^[0-9]{12}$")

    override suspend fun searchCustomers(query: String): Result<List<com.mebanking.app.domain.model.User>> {
        val session = sessionManager.currentSession
            ?: return Result.failure(DomainError.Unauthorized("Authentication required"))
        if (session.role != Role.OFFICER) return Result.failure(DomainError.Forbidden("Only bank officers can search customers"))
        val normalized = query.trim().take(100)
        val lookupHash = if (nationalIdRegex.matches(normalized)) nationalIdProtector?.lookupHash(normalized) else null
        return Result.success(userDao.searchCustomers(normalized, lookupHash).map { entity ->
            entity.toDomainModel().copy(nationalId = entity.nationalIdCipher?.let { nationalIdProtector?.decrypt(it) })
        })
    }

    override suspend fun createCustomer(
        username: String,
        rawPassword: String,
        fullName: String,
        email: String,
        phone: String,
        kycVerified: Boolean,
        nationalId: String?,
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
        val trimmedNationalId = nationalId?.trim()?.takeIf { it.isNotEmpty() }

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
        if (trimmedNationalId != null && !nationalIdRegex.matches(trimmedNationalId)) {
            return Result.failure(DomainError.InvalidData("CCCD phải gồm đúng 12 chữ số"))
        }

        if (userDao.getByUsername(trimmedUsername) != null) {
            return Result.failure(DomainError.InvalidData("Username '$trimmedUsername' is already taken"))
        }
        val protectedId = trimmedNationalId?.let { nationalIdProtector?.protect(it) }
        if (trimmedNationalId != null && protectedId == null) {
            return Result.failure(DomainError.InvalidData("Không thể bảo vệ CCCD trên thiết bị; hồ sơ chưa được lưu"))
        }
        if (protectedId != null && userDao.getByNationalIdHash(protectedId.lookupHash) != null) {
            return Result.failure(DomainError.InvalidData("CCCD đã được đăng ký"))
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
            nationalIdCipher = protectedId?.cipherText,
            nationalIdHash = protectedId?.lookupHash,
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
        nationalId: String?,
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
        val trimmedNationalId = nationalId?.trim()?.takeIf { it.isNotEmpty() }

        if (trimmedName.length < 2) {
            return Result.failure(DomainError.InvalidData("Full name must be at least 2 characters"))
        }
        if (!emailRegex.matches(trimmedEmail)) {
            return Result.failure(DomainError.InvalidData("Invalid email address format"))
        }
        if (!phoneRegex.matches(trimmedPhone)) {
            return Result.failure(DomainError.InvalidData("Phone number must be a valid 10-digit number starting with 0"))
        }
        if (trimmedNationalId != null && !nationalIdRegex.matches(trimmedNationalId)) {
            return Result.failure(DomainError.InvalidData("CCCD phải gồm đúng 12 chữ số"))
        }
        val protectedId = trimmedNationalId?.let { nationalIdProtector?.protect(it) }
        if (trimmedNationalId != null && protectedId == null) {
            return Result.failure(DomainError.InvalidData("Không thể bảo vệ CCCD trên thiết bị; hồ sơ chưa được lưu"))
        }
        if (protectedId != null && userDao.getByNationalIdHash(protectedId.lookupHash)?.id?.let { it != targetUserId } == true) {
            return Result.failure(DomainError.InvalidData("CCCD đã được đăng ký"))
        }

        val updated = existing.copy(
            fullName = trimmedName,
            email = trimmedEmail,
            phone = trimmedPhone,
            kycVerified = kycVerified ?: existing.kycVerified,
            nationalIdCipher = protectedId?.cipherText ?: existing.nationalIdCipher,
            nationalIdHash = protectedId?.lookupHash ?: existing.nationalIdHash,
        )
        userDao.update(updated)
        return Result.success(Unit)
    }

    override suspend fun getCustomerAccounts(targetUserId: Long): Result<List<com.mebanking.app.domain.model.Account>> {
        val session = sessionManager.currentSession
            ?: return Result.failure(DomainError.Unauthorized("Authentication required"))
        if (session.role != Role.OFFICER) {
            return Result.failure(DomainError.Forbidden("Only bank officers can view customer accounts"))
        }
        return Result.success(accountDao.getAccountsByUserIdSync(targetUserId).map { it.toDomainModel() })
    }

    override suspend fun openAccountForCustomer(
        targetUserId: Long,
        accountType: AccountType,
        initialBalance: Long,
    ): Result<com.mebanking.app.domain.model.Account> {
        val session = sessionManager.currentSession
            ?: return Result.failure(DomainError.Unauthorized("Authentication required"))
        if (session.role != Role.OFFICER) {
            return Result.failure(DomainError.Forbidden("Only bank officers can open accounts"))
        }
        val customer = userDao.getById(targetUserId)
            ?: return Result.failure(DomainError.NotFound("Không tìm thấy khách hàng"))
        if (customer.role != Role.CUSTOMER) {
            return Result.failure(DomainError.Forbidden("Chỉ mở tài khoản cho khách hàng"))
        }
        if (initialBalance < 0L) {
            return Result.failure(DomainError.InvalidData("Số dư ban đầu không hợp lệ"))
        }

        val prefix = when (accountType) {
            AccountType.CHECKING -> "100"
            AccountType.SAVING -> "200"
            AccountType.MORTGAGE -> "300"
        }
        val existingCount = accountDao.getAccountsByUserIdSync(targetUserId).size
        val accountNumber = prefix + String.format("%05d%02d", targetUserId, existingCount + 1)

        val entity = AccountEntity(
            userId = targetUserId,
            accountNumber = accountNumber,
            accountType = accountType,
            balance = initialBalance,
            currency = "VND",
            status = "ACTIVE",
        )
        val newId = accountDao.insert(entity)
        return Result.success(entity.copy(id = newId).toDomainModel())
    }
}
