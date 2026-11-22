package com.mebanking.app.domain.model

data class ChatIntent(
    val id: Long = 0,
    val intentCode: String,
    val patterns: List<String>,
    val responseTemplate: String,
    val targetScreen: String,
    val priority: Int = 0,
)
