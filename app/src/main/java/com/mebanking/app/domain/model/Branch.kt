package com.mebanking.app.domain.model

data class Branch(
    val id: Long = 0,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val phone: String,
    val openingHours: String,
    val hasAtm: Boolean = true,
)
