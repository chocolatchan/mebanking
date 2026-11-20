package com.mebanking.app.domain.model

/**
 * Interactive action chip returned by the chatbot for deep-linking into specific app flows.
 */
data class ActionChip(
    val label: String,
    val targetScreen: String,
    val deepLink: String? = null,
)

/**
 * Response structure produced by the rule-based chatbot for user natural language queries.
 */
data class ChatResponse(
    val query: String,
    val intentCode: String,
    val responseText: String,
    val targetScreen: String,
    val confidence: Double = 1.0,
    val actionChips: List<ActionChip> = emptyList(),
)
