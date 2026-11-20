package com.mebanking.app.domain.service

import com.mebanking.app.domain.model.ChatResponse

/**
 * Domain service for the offline rule-based chatbot.
 * Processes natural language queries and returns structured responses with deep-link targets.
 */
interface ChatbotService {
    suspend fun processQuery(query: String): ChatResponse
}
