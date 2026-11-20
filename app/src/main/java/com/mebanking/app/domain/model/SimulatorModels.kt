package com.mebanking.app.domain.model

/**
 * Explicit lifecycle states for simulated transactions and bill payments.
 * Enforces state machine progression: Created -> PendingPayment -> Paid -> Completed / Failed.
 */
sealed interface SimulatorState {
    data object Created : SimulatorState
    data object PendingPayment : SimulatorState
    data object Paid : SimulatorState
    data object Completed : SimulatorState
    data class Failed(val reason: String) : SimulatorState
}

/**
 * Result of a simulated utility bill payment with explicit lifecycle history.
 */
data class BillPaymentResult(
    val billId: Long,
    val transactionId: Long,
    val amount: Long,
    val paidAt: Long,
    val status: SimulatorState,
    val lifecycleHistory: List<SimulatorState> = listOf(
        SimulatorState.Created,
        SimulatorState.PendingPayment,
        SimulatorState.Paid,
        SimulatorState.Completed,
    ),
)
