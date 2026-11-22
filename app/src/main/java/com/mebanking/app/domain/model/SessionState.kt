package com.mebanking.app.domain.model

sealed interface SessionState {
    data object Unauthenticated : SessionState
    data class Authenticated(val user: UserSession) : SessionState
    data object Expired : SessionState
    data object LoggedOut : SessionState
}
