package com.mebanking.app.domain.service

import com.mebanking.app.data.repository.ChatbotRepository
import com.mebanking.app.domain.model.ActionChip
import com.mebanking.app.domain.model.ChatResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Default offline implementation of [ChatbotService].
 * Uses keyword pattern matching against seeded [ChatIntent] data from Room.
 * Falls back to unknown_intent when no pattern matches.
 */
class DefaultChatbotService(
    private val chatbotRepository: ChatbotRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ChatbotService {

    override suspend fun processQuery(query: String): ChatResponse = withContext(ioDispatcher) {
        val q = query.lowercase().trim()
        val intents = chatbotRepository.getAllIntentsSync()

        for (intent in intents.sortedByDescending { it.priority }) {
            val patterns = intent.patterns.map { it.trim().lowercase() }
            if (patterns.any { pattern -> pattern.isNotBlank() && q.contains(pattern) }) {
                return@withContext ChatResponse(
                    query = query,
                    intentCode = intent.intentCode,
                    responseText = intent.responseTemplate,
                    targetScreen = intent.targetScreen,
                    confidence = 1.0,
                    actionChips = if (intent.targetScreen.isNotBlank()) {
                        listOf(ActionChip(label = intent.intentCode, targetScreen = intent.targetScreen))
                    } else emptyList(),
                )
            }
        }

        // Fallback for unrecognized queries
        ChatResponse(
            query = query,
            intentCode = "unknown_intent",
            responseText = "Xin lỗi, tôi chưa hiểu yêu cầu của bạn. Bạn có thể thử hỏi về: số dư, chuyển tiền, hóa đơn, ATM gần nhất.",
            targetScreen = "home",
            confidence = 0.0,
        )
    }
}
