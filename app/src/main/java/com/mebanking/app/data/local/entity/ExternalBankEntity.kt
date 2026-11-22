package com.mebanking.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mebanking.app.domain.model.ExternalBank

/**
 * Room entity representing an external bank for local offline interbank simulation.
 */
@Entity(
    tableName = "external_banks",
    indices = [
        Index(value = ["bank_code"], unique = true),
        Index(value = ["bin"], unique = true),
    ],
)
data class ExternalBankEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "bank_code")
    val bankCode: String,

    @ColumnInfo(name = "bank_name")
    val bankName: String,

    @ColumnInfo(name = "short_name")
    val shortName: String,

    @ColumnInfo(name = "bin")
    val bin: String,

    @ColumnInfo(name = "is_supported")
    val isSupported: Boolean = true,
) {
    fun toDomainModel(): ExternalBank {
        return ExternalBank(
            id = id,
            bankCode = bankCode,
            bankName = bankName,
            shortName = shortName,
            bin = bin,
            isSupported = isSupported,
        )
    }

    companion object {
        fun fromDomainModel(bank: ExternalBank): ExternalBankEntity {
            return ExternalBankEntity(
                id = bank.id,
                bankCode = bank.bankCode,
                bankName = bank.bankName,
                shortName = bank.shortName,
                bin = bank.bin,
                isSupported = bank.isSupported,
            )
        }
    }
}
