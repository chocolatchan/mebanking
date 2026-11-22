package com.mebanking.app.domain.model

data class UserSession(
    val userId: Long,
    val username: String,
    val role: Role,
    val fullName: String,
    val loginTimestamp: Long = System.currentTimeMillis(),
)
