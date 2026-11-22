package com.mebanking.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.User

/**
 * Room entity representing an application user (Customer or Officer).
 *
 * Security Invariant: Plaintext passwords, Android device PINs, and raw biometric
 * data are strictly FORBIDDEN from being stored in Room.
 */
@Entity(
    tableName = "users",
    indices = [
        Index(value = ["username"], unique = true),
        Index(value = ["national_id_hash"], unique = true),
    ],
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "username")
    val username: String,

    @ColumnInfo(name = "password_hash")
    val passwordHash: String,

    @ColumnInfo(name = "full_name")
    val fullName: String,

    @ColumnInfo(name = "email")
    val email: String,

    @ColumnInfo(name = "phone")
    val phone: String,

    @ColumnInfo(name = "role")
    val role: Role,

    @ColumnInfo(name = "kyc_verified")
    val kycVerified: Boolean = false,

    @ColumnInfo(name = "face_image_path")
    val faceImagePath: String? = null,

    @ColumnInfo(name = "national_id_cipher")
    val nationalIdCipher: String? = null,

    @ColumnInfo(name = "national_id_hash")
    val nationalIdHash: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun toDomainModel(): User {
        return User(
            id = id,
            username = username,
            fullName = fullName,
            email = email,
            phone = phone,
            role = role,
            kycVerified = kycVerified,
            faceImagePath = faceImagePath,
            createdAt = createdAt,
        )
    }

    companion object {
        fun fromDomainModel(user: User, passwordHash: String): UserEntity {
            return UserEntity(
                id = user.id,
                username = user.username,
                passwordHash = passwordHash,
                fullName = user.fullName,
                email = user.email,
                phone = user.phone,
                role = user.role,
                kycVerified = user.kycVerified,
                faceImagePath = user.faceImagePath,
                createdAt = user.createdAt,
            )
        }
    }
}
