package com.mebanking.app.domain.service

import com.mebanking.app.data.local.security.PasswordHasher
import com.mebanking.app.data.repository.AuthRepository
import com.mebanking.app.domain.model.DomainError
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.SessionState
import com.mebanking.app.domain.model.User
import com.mebanking.app.domain.model.UserSession
import kotlinx.coroutines.flow.StateFlow

data class CreateCustomerRequest(
    val username: String,
    val rawPassword: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val kycVerified: Boolean = false,
)

data class UpdateProfileRequest(
    val fullName: String,
    val email: String,
    val phone: String,
    val kycVerified: Boolean? = null,
)

interface AuthService {
    val sessionState: StateFlow<SessionState>

    suspend fun login(username: String, password: CharSequence): Result<UserSession>
    suspend fun logout(): Result<Unit>

    fun requireRole(requiredRole: Role): Result<UserSession>
    fun requireCustomer(): Result<UserSession>
    fun requireOfficer(): Result<UserSession>

    suspend fun getCustomerProfile(userId: Long, callerSession: UserSession): Result<User>
    suspend fun getOfficerProfile(callerSession: UserSession): Result<User>
    suspend fun updateOwnProfile(callerSession: UserSession, request: UpdateProfileRequest): Result<User>
    suspend fun createCustomerByOfficer(officerSession: UserSession, request: CreateCustomerRequest): Result<User>
    suspend fun updateCustomerByOfficer(officerSession: UserSession, targetUserId: Long, request: UpdateProfileRequest): Result<User>
}

class DefaultAuthService(
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository? = null,
    private val passwordHasher: PasswordHasher = PasswordHasher,
) : AuthService {

    constructor(
        authRepository: AuthRepository,
        passwordHasher: PasswordHasher,
        sessionManager: SessionManager,
    ) : this(sessionManager, authRepository, passwordHasher)

    private val usernameRegex = Regex("^[a-zA-Z0-9_]{4,32}$")
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-zA-Z]{2,}$")
    private val phoneRegex = Regex("^0[0-9]{9}$")

    override val sessionState: StateFlow<SessionState> = sessionManager.sessionState

    override suspend fun login(username: String, password: CharSequence): Result<UserSession> {
        return sessionManager.login(username, password)
    }

    override suspend fun logout(): Result<Unit> {
        sessionManager.logout()
        return Result.success(Unit)
    }

    override fun requireRole(requiredRole: Role): Result<UserSession> {
        val user = sessionManager.currentSession
            ?: return when (sessionManager.sessionState.value) {
                is SessionState.Expired -> Result.failure(DomainError.Unauthorized("Session has expired. Please log in again."))
                else -> Result.failure(DomainError.Unauthorized("User is not authenticated"))
            }
        return if (user.role == requiredRole) {
            Result.success(user)
        } else {
            Result.failure(
                DomainError.Forbidden(
                    "Access denied: required role $requiredRole, but user role is ${user.role}"
                )
            )
        }
    }

    override fun requireCustomer(): Result<UserSession> = requireRole(Role.CUSTOMER)

    override fun requireOfficer(): Result<UserSession> = requireRole(Role.OFFICER)

    override suspend fun getCustomerProfile(userId: Long, callerSession: UserSession): Result<User> {
        if (callerSession.role == Role.CUSTOMER && callerSession.userId != userId) {
            return Result.failure(DomainError.Forbidden("Customers can only view their own profile"))
        }
        val repo = authRepository
            ?: return Result.failure(DomainError.SystemError("AuthRepository is not configured"))
        val user = repo.getUserById(userId)
            ?: return Result.failure(DomainError.NotFound("User not found with ID: $userId"))
        return Result.success(user)
    }

    override suspend fun getOfficerProfile(callerSession: UserSession): Result<User> {
        if (callerSession.role != Role.OFFICER) {
            return Result.failure(DomainError.Forbidden("Only bank officers can view officer profiles"))
        }
        val repo = authRepository
            ?: return Result.failure(DomainError.SystemError("AuthRepository is not configured"))
        val user = repo.getUserById(callerSession.userId)
            ?: return Result.failure(DomainError.NotFound("Officer not found"))
        return Result.success(user)
    }

    override suspend fun updateOwnProfile(callerSession: UserSession, request: UpdateProfileRequest): Result<User> {
        val trimmedName = request.fullName.trim()
        val trimmedEmail = request.email.trim()
        val trimmedPhone = request.phone.trim()

        if (trimmedName.length < 2) {
            return Result.failure(DomainError.InvalidData("Full name must be at least 2 characters"))
        }
        if (!emailRegex.matches(trimmedEmail)) {
            return Result.failure(DomainError.InvalidData("Invalid email address format"))
        }
        if (!phoneRegex.matches(trimmedPhone)) {
            return Result.failure(DomainError.InvalidData("Phone number must be a valid 10-digit number starting with 0"))
        }

        val repo = authRepository
            ?: return Result.failure(DomainError.SystemError("AuthRepository is not configured"))

        return repo.updateProfile(
            userId = callerSession.userId,
            fullName = trimmedName,
            email = trimmedEmail,
            phone = trimmedPhone,
            kycVerified = null,
        )
    }

    override suspend fun createCustomerByOfficer(
        officerSession: UserSession,
        request: CreateCustomerRequest,
    ): Result<User> {
        if (officerSession.role != Role.OFFICER) {
            return Result.failure(DomainError.Forbidden("Only bank officers can create customer accounts"))
        }

        val trimmedUsername = request.username.trim()
        val trimmedName = request.fullName.trim()
        val trimmedEmail = request.email.trim()
        val trimmedPhone = request.phone.trim()

        if (!usernameRegex.matches(trimmedUsername)) {
            return Result.failure(DomainError.InvalidData("Username must be 4-32 alphanumeric characters or underscores"))
        }
        if (request.rawPassword.length < 8) {
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

        val repo = authRepository
            ?: return Result.failure(DomainError.SystemError("AuthRepository is not configured"))

        if (repo.isUsernameTaken(trimmedUsername)) {
            return Result.failure(DomainError.InvalidData("Username '$trimmedUsername' is already taken"))
        }

        val versionedHash = passwordHasher.hashPasswordV1(request.rawPassword)

        return repo.createUser(
            username = trimmedUsername,
            passwordHash = versionedHash,
            fullName = trimmedName,
            email = trimmedEmail,
            phone = trimmedPhone,
            role = Role.CUSTOMER,
            kycVerified = request.kycVerified,
        )
    }

    override suspend fun updateCustomerByOfficer(
        officerSession: UserSession,
        targetUserId: Long,
        request: UpdateProfileRequest,
    ): Result<User> {
        if (officerSession.role != Role.OFFICER) {
            return Result.failure(DomainError.Forbidden("Only bank officers can update customer profiles"))
        }

        val repo = authRepository
            ?: return Result.failure(DomainError.SystemError("AuthRepository is not configured"))

        val existing = repo.getUserById(targetUserId)
            ?: return Result.failure(DomainError.NotFound("User not found with ID: $targetUserId"))

        if (existing.role != Role.CUSTOMER) {
            return Result.failure(DomainError.Forbidden("Officers cannot modify non-customer accounts via customer update"))
        }

        val trimmedName = request.fullName.trim()
        val trimmedEmail = request.email.trim()
        val trimmedPhone = request.phone.trim()

        if (trimmedName.length < 2) {
            return Result.failure(DomainError.InvalidData("Full name must be at least 2 characters"))
        }
        if (!emailRegex.matches(trimmedEmail)) {
            return Result.failure(DomainError.InvalidData("Invalid email address format"))
        }
        if (!phoneRegex.matches(trimmedPhone)) {
            return Result.failure(DomainError.InvalidData("Phone number must be a valid 10-digit number starting with 0"))
        }

        return repo.updateProfile(
            userId = targetUserId,
            fullName = trimmedName,
            email = trimmedEmail,
            phone = trimmedPhone,
            kycVerified = request.kycVerified,
        )
    }
}
