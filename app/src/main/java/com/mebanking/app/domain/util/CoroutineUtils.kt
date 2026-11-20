package com.mebanking.app.domain.util

import kotlinx.coroutines.CancellationException

/**
 * Re-throws [CancellationException] to uphold cooperative coroutine cancellation discipline,
 * wrapping all other exceptions in [Result.failure].
 */
inline fun <T> runSuspendCatching(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
}
