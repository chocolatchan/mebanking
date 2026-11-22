package com.mebanking.app.domain.model

/**
 * Authentication decisions resulting from evaluating step-up, biometric, or risk policies.
 */
sealed interface AuthenticationDecision {
    data object Approved : AuthenticationDecision
    data class Rejected(val reason: String) : AuthenticationDecision
    data object Cancelled : AuthenticationDecision
    data object TimedOut : AuthenticationDecision
    data object Unavailable : AuthenticationDecision
    data object PendingPolicy : AuthenticationDecision
}
