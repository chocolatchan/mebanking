package com.mebanking.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mebanking.app.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(users: List<UserEntity>): List<Long>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE national_id_hash = :nationalIdHash LIMIT 1")
    suspend fun getByNationalIdHash(nationalIdHash: String): UserEntity?

    @Query("SELECT * FROM users WHERE role = 'CUSTOMER' AND (:query = '' OR full_name LIKE '%' || :query || '%') OR (role = 'CUSTOMER' AND :nationalIdHash IS NOT NULL AND national_id_hash = :nationalIdHash) ORDER BY full_name COLLATE NOCASE LIMIT 100")
    suspend fun searchCustomers(query: String, nationalIdHash: String?): List<UserEntity>

    @Query("SELECT * FROM users ORDER BY id ASC")
    fun getAll(): Flow<List<UserEntity>>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int

    @Update
    suspend fun update(user: UserEntity): Int

    @Delete
    suspend fun delete(user: UserEntity): Int
}
