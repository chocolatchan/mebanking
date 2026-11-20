package com.mebanking.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mebanking.app.data.local.entity.BillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(bill: BillEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(bills: List<BillEntity>): List<Long>

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getById(id: Long): BillEntity?

    @Query("SELECT * FROM bills WHERE user_id = :userId ORDER BY due_date ASC")
    fun getBillsByUserId(userId: Long): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE user_id = :userId AND status != 'PAID' ORDER BY due_date ASC")
    fun getUnpaidBillsByUserId(userId: Long): Flow<List<BillEntity>>

    @Query("UPDATE bills SET status = 'PAID', paid_at = :paidAt WHERE id = :id")
    suspend fun markPaid(id: Long, paidAt: Long = System.currentTimeMillis()): Int

    @Query("SELECT COUNT(*) FROM bills")
    suspend fun count(): Int
}
