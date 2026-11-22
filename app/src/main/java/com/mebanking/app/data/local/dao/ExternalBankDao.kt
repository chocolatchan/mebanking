package com.mebanking.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mebanking.app.data.local.entity.ExternalBankEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExternalBankDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(bank: ExternalBankEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(banks: List<ExternalBankEntity>): List<Long>

    @Query("SELECT * FROM external_banks WHERE id = :id")
    suspend fun getById(id: Long): ExternalBankEntity?

    @Query("SELECT * FROM external_banks WHERE bank_code = :bankCode LIMIT 1")
    suspend fun getByCode(bankCode: String): ExternalBankEntity?

    @Query("SELECT * FROM external_banks WHERE bin = :bin LIMIT 1")
    suspend fun getByBin(bin: String): ExternalBankEntity?

    @Query("SELECT * FROM external_banks ORDER BY short_name ASC")
    fun getAll(): Flow<List<ExternalBankEntity>>

    @Query("SELECT * FROM external_banks WHERE is_supported = 1 ORDER BY short_name ASC")
    fun getSupportedBanks(): Flow<List<ExternalBankEntity>>

    @Query("SELECT COUNT(*) FROM external_banks")
    suspend fun count(): Int
}
