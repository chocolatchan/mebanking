package com.mebanking.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mebanking.app.domain.model.OtpCode

/**
 * Room entity representing an auxiliary OTP code.
 *
 * Scrutinized auxiliary entity:
 * Normal transactions authenticate via Android Device Authentication;
 * high-value transactions authenticate via in-app Face Recognition.
 * OTP is strictly an auxiliary/fallback mechanism, never the default.
 * Plaintext OTP codes are never stored; only cryptographic hashes are persisted.
 */
@Entity(
    tableName = "otp_codes",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["user_id"]),
    ],
)
data class OtpCodeEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "code_hash")
    val codeHash: String,

    @ColumnInfo(name = "expires_at")
    val expiresAt: Long,

    @ColumnInfo(name = "is_used")
    val isUsed: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun toDomainModel(): OtpCode {
        return OtpCode(
            id = id,
            userId = userId,
            codeHash = codeHash,
            expiresAt = expiresAt,
            isUsed = isUsed,
            createdAt = createdAt,
        )
    }

    companion object {
        fun fromDomainModel(otp: OtpCode): OtpCodeEntity {
            return OtpCodeEntity(
                id = otp.id,
                userId = otp.userId,
                codeHash = otp.codeHash,
                expiresAt = otp.expiresAt,
                isUsed = otp.isUsed,
                createdAt = otp.createdAt,
            )
        }
    }
}
