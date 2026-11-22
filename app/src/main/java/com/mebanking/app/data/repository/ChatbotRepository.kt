package com.mebanking.app.data.repository

import com.mebanking.app.domain.model.ChatIntent
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for rule-based chatbot intent configuration and querying.
 */
interface ChatbotRepository {
    fun getAllIntentsOrderedByPriority(): Flow<List<ChatIntent>>
    suspend fun getAllIntentsSync(): List<ChatIntent>
    suspend fun getIntentByCode(intentCode: String): ChatIntent?
    suspend fun insertIntent(intent: ChatIntent): Long
    suspend fun count(): Int
}
