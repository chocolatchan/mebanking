package com.mebanking.app.domain.util

import com.mebanking.app.domain.model.DomainError
import com.mebanking.app.domain.model.toDomainError

object ErrorMapper {
    /**
     * Sanitizes [throwable] into a safe [DomainError].
     * Rethrows [kotlinx.coroutines.CancellationException] immediately.
     */
    fun toDomainError(throwable: Throwable): DomainError = throwable.toDomainError()
}
