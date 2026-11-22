package com.mebanking.app.domain.model

data class User(
    val id: Long = 0,
    val username: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val role: Role,
    val kycVerified: Boolean = false,
    val faceImagePath: String? = null,
    val nationalId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
