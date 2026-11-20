package com.mebanking.app.domain.service

import com.mebanking.app.data.local.dao.UserDao
import com.mebanking.app.data.local.security.PasswordHasher
import com.mebanking.app.data.repository.AuthRepository
import com.mebanking.app.domain.model.DomainError
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.SessionState
import com.mebanking.app.domain.model.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface SessionManager {
    val sessionState: StateFlow<SessionState>
    val currentSession: UserSession?
    suspend fun login(username: String, password: CharSequence): Result<UserSession>
    fun startSession(session: UserSession)
    fun expireSession()
    fun logout()
    fun reset()
}

class DefaultSessionManager(
    private val userDao: UserDao? = null,
    private val passwordHasher: PasswordHasher = PasswordHasher,
    private val authRepository: AuthRepository? = null,
    private val sessionTimeoutMs: Long = DEFAULT_SESSION_TIMEOUT_MS,
) : SessionManager {

    companion object {
        const val DEFAULT_SESSION_TIMEOUT_MS: Long = 15 * 60 * 1000L // 15 minutes
    }

    constructor(userDao: UserDao, passwordHasher: PasswordHasher = PasswordHasher) : this(
        userDao,
        passwordHasher,
        null,
        DEFAULT_SESSION_TIMEOUT_MS,
    )

    constructor(authRepository: AuthRepository, passwordHasher: PasswordHasher = PasswordHasher) : this(
        null,
        passwordHasher,
        authRepository,
        DEFAULT_SESSION_TIMEOUT_MS,
    )

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Unauthenticated)
    override val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    override val currentSession: UserSession?
        get() = when (val state = _sessionState.value) {
            is SessionState.Authenticated -> {
                if (System.currentTimeMillis() - state.user.loginTimestamp > sessionTimeoutMs) {
                    expireSession()
                    null
                } else {
                    state.user
                }
            }
            else -> null
        }

    override suspend fun login(username: String, password: CharSequence): Result<UserSession> {
        val trimmedUsername = username.trim()
        if (trimmedUsername.isBlank() || password.isEmpty()) {
            return Result.failure(DomainError.InvalidData("Username and password must not be empty"))
        }

        val (userId, dbUsername, role, fullName, storedHash) = if (authRepository != null) {
            val user = authRepository.getUserByUsername(trimmedUsername)
                ?: return Result.failure(DomainError.Unauthorized("Invalid username or password"))
            val hash = authRepository.getPasswordHashByUsername(trimmedUsername)
                ?: return Result.failure(DomainError.Unauthorized("Invalid username or password"))
            UserData(user.id, user.username, user.role, user.fullName, hash)
        } else if (userDao != null) {
            val entity = userDao.getByUsername(trimmedUsername)
                ?: return Result.failure(DomainError.Unauthorized("Invalid username or password"))
            UserData(entity.id, entity.username, entity.role, entity.fullName, entity.passwordHash)
        } else {
            return Result.failure(DomainError.SystemError("No repository or DAO configured for SessionManager"))
        }

        val isMatch = passwordHasher.verifyPassword(password.toString(), storedHash)
        if (!isMatch) {
            return Result.failure(DomainError.Unauthorized("Invalid username or password"))
        }

        // Automatic upgrade-on-login for legacy hashes
        if (passwordHasher.needsUpgrade(storedHash)) {
            val upgradedHash = passwordHasher.hashPasswordV1(password.toString())
            if (authRepository != null) {
                authRepository.updatePasswordHash(userId, upgradedHash)
            } else if (userDao != null) {
                val entity = userDao.getByUsername(trimmedUsername)
                if (entity != null) {
                    userDao.update(entity.copy(passwordHash = upgradedHash))
                }
            }
        }

        val session = UserSession(
            userId = userId,
            username = dbUsername,
            role = role,
            fullName = fullName,
            loginTimestamp = System.currentTimeMillis(),
        )
        startSession(session)
        return Result.success(session)
    }

    override fun startSession(session: UserSession) {
        _sessionState.value = SessionState.Authenticated(session)
    }

    override fun expireSession() {
        _sessionState.value = SessionState.Expired
    }

    override fun logout() {
        _sessionState.value = SessionState.LoggedOut
    }

    override fun reset() {
        _sessionState.value = SessionState.Unauthenticated
    }

    private data class UserData(
        val id: Long,
        val username: String,
        val role: Role,
        val fullName: String,
        val passwordHash: String,
    )
}
