package com.mebanking.app.domain.model

data class ExternalBank(
    val id: Long = 0,
    val bankCode: String,
    val bankName: String,
    val shortName: String,
    val bin: String,
    val isSupported: Boolean = true,
)
