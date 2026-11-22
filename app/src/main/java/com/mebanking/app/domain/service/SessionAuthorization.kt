package com.mebanking.app.domain.service

import com.mebanking.app.domain.model.DomainError
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.UserSession

/**
 * Shared session and ownership guards for money-moving domain services.
 *
 * Passing a [UserSession] value is not sufficient proof of authorization: the
 * session must match the currently authenticated session held by
 * [SessionManager], and customer operations must only touch accounts owned by
 * that customer. These checks live in the domain layer so that a UI bug or a
 * direct service call cannot bypass them.
 */
internal object SessionAuthorization {

    fun validateSession(
        session: UserSession,
        sessionManager: SessionManager?,
    ): DomainError? {
        if (session.userId <= 0L) {
            return DomainError.Unauthorized("Người dùng chưa được xác thực")
        }
        if (sessionManager == null) return null

        val current = sessionManager.currentSession
            ?: return DomainError.Unauthorized("Phiên làm việc đã hết hạn hoặc chưa đăng nhập")
        if (current.userId != session.userId || current.role != session.role) {
            return DomainError.Unauthorized("Phiên làm việc không khớp với người dùng hiện tại")
        }
        return null
    }

    fun requireCustomer(session: UserSession): DomainError? {
        if (session.role != Role.CUSTOMER) {
            return DomainError.Forbidden("Chỉ khách hàng mới được thực hiện giao dịch này")
        }
        return null
    }

    fun requireOwnedAccount(
        accountOwnerId: Long,
        session: UserSession,
        resourceName: String = "tài khoản",
    ): DomainError? {
        if (accountOwnerId != session.userId) {
            return DomainError.Forbidden("Bạn không có quyền truy cập $resourceName này")
        }
        return null
    }
}
