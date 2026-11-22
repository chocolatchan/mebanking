package com.mebanking.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mebanking.app.data.local.entity.OtpCodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OtpCodeDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(otpCode: OtpCodeEntity): Long

    @Query("SELECT * FROM otp_codes WHERE id = :id")
    suspend fun getById(id: Long): OtpCodeEntity?

    @Query("SELECT * FROM otp_codes WHERE user_id = :userId AND is_used = 0 AND expires_at > :currentTime ORDER BY created_at DESC LIMIT 1")
    suspend fun getValidOtp(userId: Long, currentTime: Long = System.currentTimeMillis()): OtpCodeEntity?

    @Query("SELECT * FROM otp_codes WHERE user_id = :userId AND is_used = 0 AND expires_at > :currentTime ORDER BY created_at DESC")
    fun getActiveOtpsForUser(userId: Long, currentTime: Long = System.currentTimeMillis()): Flow<List<OtpCodeEntity>>

    @Query("UPDATE otp_codes SET is_used = 1 WHERE id = :id")
    suspend fun markUsed(id: Long): Int

    @Query("SELECT COUNT(*) FROM otp_codes")
    suspend fun count(): Int
}
