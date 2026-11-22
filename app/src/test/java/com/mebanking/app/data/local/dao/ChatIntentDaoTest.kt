package com.mebanking.app.data.local.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.entity.ChatIntentEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChatIntentDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var intentDao: ChatIntentDao

    @Before
    fun setUp() {
        database = AppDatabase.buildInMemoryDatabase(ApplicationProvider.getApplicationContext())
        intentDao = database.chatIntentDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertIntent_and_queryByCode() = runTest {
        val patterns = listOf("chuyển tiền", "bắn tiền", "transfer")
        val intent = ChatIntentEntity(
            id = 1L,
            intentCode = "transfer",
            patterns = patterns,
            responseTemplate = "Bạn muốn chuyển tiền đến ai?",
            targetScreen = "transfer",
            priority = 10,
        )

        intentDao.insert(intent)

        val retrieved = intentDao.getByIntentCode("transfer")
        assertNotNull(retrieved)
        assertEquals("transfer", retrieved?.intentCode)
        assertEquals(patterns, retrieved?.patterns)
        assertEquals(10, retrieved?.priority)
    }

    @Test
    fun getAllOrderedByPriority_emitsSorted() = runTest {
        val lowPriority = ChatIntentEntity(
            id = 1L,
            intentCode = "low",
            patterns = listOf("help"),
            responseTemplate = "Help",
            targetScreen = "help",
            priority = 1,
        )
        val highPriority = ChatIntentEntity(
            id = 2L,
            intentCode = "high",
            patterns = listOf("urgent"),
            responseTemplate = "Urgent",
            targetScreen = "urgent",
            priority = 100,
        )

        intentDao.insert(lowPriority)
        intentDao.insert(highPriority)

        val list = intentDao.getAllOrderedByPriority().first()
        assertEquals(2, list.size)
        assertEquals("high", list[0].intentCode)
        assertEquals("low", list[1].intentCode)
    }

    @Test
    fun uniqueIntentCode_duplicateThrows() = runTest {
        val intent1 = ChatIntentEntity(id = 1L, intentCode = "same_code", patterns = emptyList(), responseTemplate = "R1", targetScreen = "S1")
        val intent2 = ChatIntentEntity(id = 2L, intentCode = "same_code", patterns = emptyList(), responseTemplate = "R2", targetScreen = "S2")

        intentDao.insert(intent1)

        try {
            intentDao.insert(intent2)
            org.junit.Assert.fail("Expected SQLiteConstraintException on duplicate intent code")
        } catch (e: SQLiteConstraintException) {
            // Expected constraint violation
        }
    }
}
