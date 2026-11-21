package com.mebanking.app.ui.chatbot

import com.mebanking.app.domain.model.ActionChip
import com.mebanking.app.domain.model.ChatResponse
import com.mebanking.app.domain.service.ChatbotService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatbotViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeChatbotService(private val response: ChatResponse) : ChatbotService {
        override suspend fun processQuery(query: String): ChatResponse = response
    }

    @Test
    fun send_appendsUserAndAssistantMessages_withLocalizedChips() = runTest {
        val service = FakeChatbotService(
            ChatResponse(
                query = "chuyển tiền",
                intentCode = "transfer_money",
                responseText = "Bạn muốn chuyển tiền nội bộ hay liên ngân hàng?",
                targetScreen = "transfer",
                actionChips = listOf(ActionChip(label = "transfer", targetScreen = "transfer")),
            ),
        )
        val viewModel = ChatbotViewModel(service)

        viewModel.send("chuyển tiền")
        advanceUntilIdle()

        val messages = viewModel.uiState.value.messages
        assertEquals(3, messages.size)
        assertTrue(messages[1].fromUser)
        assertEquals("chuyển tiền", messages[1].text)
        assertFalse(messages[2].fromUser)
        assertEquals("Chuyển tiền", messages[2].chips.single().label)
        assertFalse(viewModel.uiState.value.isSending)
    }

    @Test
    fun send_ignoresBlankInput() = runTest {
        val service = FakeChatbotService(
            ChatResponse(
                query = "",
                intentCode = "unknown",
                responseText = "Không hiểu",
                targetScreen = "home",
            ),
        )
        val viewModel = ChatbotViewModel(service)
        val initialCount = viewModel.uiState.value.messages.size

        viewModel.send("   ")
        advanceUntilIdle()

        assertEquals(initialCount, viewModel.uiState.value.messages.size)
    }
}
