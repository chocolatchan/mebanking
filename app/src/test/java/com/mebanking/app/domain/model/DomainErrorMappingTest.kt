package com.mebanking.app.domain.model

import android.database.sqlite.SQLiteConstraintException
import com.mebanking.app.domain.util.ErrorMapper
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class DomainErrorMappingTest {

    @Test
    fun sqliteConstraintException_mapsToDomainError_withoutLeakingSqlQuery() {
        val sqlEx = SQLiteConstraintException("UNIQUE constraint failed: users.username, query: INSERT INTO users...")
        val domainError = ErrorMapper.toDomainError(sqlEx)

        assertTrue(domainError is DomainError.InvalidData)
        assertFalse(domainError.message!!.contains("UNIQUE constraint failed"))
        assertFalse(domainError.message!!.contains("INSERT INTO"))
        assertEquals("Invalid data or duplicate record provided", domainError.message)
    }

    @Test
    fun ioException_mapsToNetworkError() {
        val ioEx = IOException("Failed to connect to /127.0.0.1:8080")
        val domainError = ErrorMapper.toDomainError(ioEx)

        assertTrue(domainError is DomainError.NetworkError)
        assertFalse(domainError.message!!.contains("127.0.0.1"))
    }

    @Test
    fun cancellationException_isNeverMappedAndMustBeRethrown() {
        val cancelEx = CancellationException("Job cancellation")
        assertThrows(CancellationException::class.java) {
            ErrorMapper.toDomainError(cancelEx)
        }
    }
}
