package com.mebanking.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mebanking.app.data.local.entity.BranchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BranchDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(branch: BranchEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(branches: List<BranchEntity>): List<Long>

    @Query("SELECT * FROM branches WHERE id = :id")
    suspend fun getById(id: Long): BranchEntity?

    @Query("SELECT * FROM branches ORDER BY name ASC")
    fun getAll(): Flow<List<BranchEntity>>

    @Query("SELECT * FROM branches ORDER BY name ASC")
    suspend fun getAllBranches(): List<BranchEntity>

    @Query("SELECT * FROM branches WHERE has_atm = 1 ORDER BY name ASC")
    fun getAtmBranches(): Flow<List<BranchEntity>>

    @Query("SELECT COUNT(*) FROM branches")
    suspend fun count(): Int
}
