package com.mebanking.app.domain.model

import java.io.IOException
import java.sql.SQLException
import kotlinx.coroutines.CancellationException

/**
 * Domain-level error hierarchy mapping technical, database, network, and invariant exceptions
 * into safe, sanitized domain errors suitable for user presentation.
 *
 * Extends [Exception] to interoperate seamlessly with Kotlin's standard library [Result].
 */
sealed class DomainError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    class Unauthorized(
        message: String = "Unauthorized access",
        cause: Throwable? = null,
    ) : DomainError(message, cause)

    class Forbidden(
        message: String = "Access denied: insufficient permissions",
        cause: Throwable? = null,
    ) : DomainError(message, cause)

    class NotFound(
        message: String = "Resource not found",
        cause: Throwable? = null,
    ) : DomainError(message, cause)

    class InvalidData(
        message: String = "Invalid data provided",
        cause: Throwable? = null,
    ) : DomainError(message, cause)

    class SystemError(
        message: String = "A system error occurred",
        cause: Throwable? = null,
    ) : DomainError(message, cause)

    class NetworkError(
        message: String = "Network communication failed",
        cause: Throwable? = null,
    ) : DomainError(message, cause)

    class InsufficientFunds(
        message: String = "Insufficient funds for transaction",
        cause: Throwable? = null,
    ) : DomainError(message, cause)

    class ArithmeticOverflow(
        message: String = "Transaction amount exceeds arithmetic limit",
        cause: Throwable? = null,
    ) : DomainError(message, cause)

    class DuplicateTransaction(
        message: String = "Duplicate transaction detected",
        cause: Throwable? = null,
    ) : DomainError(message, cause)

    class AccountInactive(
        message: String = "Account is currently inactive",
        cause: Throwable? = null,
    ) : DomainError(message, cause)

    class UnapprovedFormula(
        message: String = "Financial formula is not approved (OPEN/BLOCKED)",
        cause: Throwable? = null,
    ) : DomainError(message, cause)

    class CalculationBlocked(
        message: String = "Financial calculation blocked: unapproved formula",
        cause: Throwable? = null,
    ) : DomainError(message, cause)

    class PolicyRejected(
        message: String = "Transaction policy evaluation rejected",
        cause: Throwable? = null,
    ) : DomainError(message, cause)

    class BillAlreadyPaid(
        message: String = "Bill is already paid",
        cause: Throwable? = null,
    ) : DomainError(message, cause)
}

typealias UnapprovedFormulaException = DomainError.UnapprovedFormula
typealias MonetaryOverflowException = DomainError.ArithmeticOverflow

/**
 * Extension function converting raw [Throwable] instances into sanitized [DomainError].
 * Guarantees that internal database constraints, SQL exceptions, or stack traces
 * are never exposed to the UI layer.
 *
 * NOTE: [CancellationException] is NEVER mapped and is always rethrown.
 */
fun Throwable.toDomainError(): DomainError {
    if (this is CancellationException) throw this
    if (this is DomainError) return this
    return when (this) {
        is android.database.sqlite.SQLiteConstraintException -> {
            DomainError.InvalidData("Invalid data or duplicate record provided", this)
        }
        is android.database.SQLException, is SQLException -> {
            DomainError.SystemError("Database operation failed", this)
        }
        is SecurityException -> {
            DomainError.Forbidden("Access denied: insufficient permissions", this)
        }
        is ArithmeticException -> {
            DomainError.ArithmeticOverflow("Calculation overflow error", this)
        }
        is IllegalArgumentException -> {
            DomainError.InvalidData(message ?: "Invalid data provided", this)
        }
        is IllegalStateException -> {
            DomainError.InvalidData(message ?: "Invalid state", this)
        }
        is IOException -> {
            DomainError.NetworkError("Network or IO communication failed", this)
        }
        else -> {
            DomainError.SystemError("A system error occurred", this)
        }
    }
}
