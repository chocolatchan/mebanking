package com.mebanking.app.ui.chatbot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebanking.app.domain.model.ActionChip
import com.mebanking.app.domain.model.toDomainError
import com.mebanking.app.domain.service.ChatbotService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: Long,
    val fromUser: Boolean,
    val text: String,
    val chips: List<ActionChip> = emptyList(),
)

data class ChatbotUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isSending: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * State holder for the offline rule-based assistant.
 *
 * The chatbot never executes a transaction: it only answers and offers
 * navigation chips. Every chip leads to a screen whose own confirmation and
 * authentication flow still applies.
 */
class ChatbotViewModel(
    private val chatbotService: ChatbotService,
) : ViewModel() {

    private var nextMessageId = 0L

    private val _uiState = MutableStateFlow(ChatbotUiState())
    val uiState: StateFlow<ChatbotUiState> = _uiState.asStateFlow()

    init {
        appendBotMessage(
            text = "Xin chào! Tôi là trợ lý ảo M.Ebanking. Bạn có thể hỏi về số dư, chuyển tiền, hóa đơn hoặc chi nhánh gần nhất.",
        )
    }

    fun send(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty() || _uiState.value.isSending) return

        _uiState.update { state ->
            state.copy(
                messages = state.messages + ChatMessage(
                    id = nextMessageId++,
                    fromUser = true,
                    text = trimmed,
                ),
                isSending = true,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            try {
                val response = chatbotService.processQuery(trimmed)
                _uiState.update { state ->
                    state.copy(
                        isSending = false,
                        messages = state.messages + ChatMessage(
                            id = nextMessageId++,
                            fromUser = false,
                            text = response.responseText,
                            chips = response.actionChips.map { it.withDisplayLabel() },
                        ),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSending = false,
                        errorMessage = e.toDomainError().message ?: "Trợ lý ảo tạm thời không phản hồi",
                    )
                }
            }
        }
    }

    private fun appendBotMessage(text: String) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages + ChatMessage(
                    id = nextMessageId++,
                    fromUser = false,
                    text = text,
                ),
            )
        }
    }

    private fun ActionChip.withDisplayLabel(): ActionChip = copy(
        label = when (targetScreen) {
            "transfer" -> "Chuyển tiền"
            "bills" -> "Thanh toán hóa đơn"
            "branches" -> "Tìm chi nhánh / ATM"
            "home" -> "Về trang chủ"
            else -> label
        },
    )
}
