package com.mebanking.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mebanking.app.data.local.entity.ChatIntentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatIntentDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(intent: ChatIntentEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(intents: List<ChatIntentEntity>): List<Long>

    @Query("SELECT * FROM chat_intents WHERE id = :id")
    suspend fun getById(id: Long): ChatIntentEntity?

    @Query("SELECT * FROM chat_intents WHERE intent_code = :intentCode LIMIT 1")
    suspend fun getByIntentCode(intentCode: String): ChatIntentEntity?

    @Query("SELECT * FROM chat_intents ORDER BY priority DESC")
    fun getAllOrderedByPriority(): Flow<List<ChatIntentEntity>>

    @Query("SELECT * FROM chat_intents ORDER BY priority DESC")
    suspend fun getIntentsOrderedByPriority(): List<ChatIntentEntity>

    @Query("SELECT COUNT(*) FROM chat_intents")
    suspend fun count(): Int
}
