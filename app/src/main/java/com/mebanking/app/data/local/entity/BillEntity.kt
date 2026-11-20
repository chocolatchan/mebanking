package com.mebanking.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mebanking.app.domain.model.Bill
import com.mebanking.app.domain.model.BillStatus
import com.mebanking.app.domain.model.BillType

/**
 * Room entity representing a utility bill.
 *
 * Invariant: Bill amount must be strictly positive (amount > 0) in 64-bit Long VND units.
 */
@Entity(
    tableName = "bills",
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
        Index(value = ["status"]),
    ],
)
data class BillEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "provider")
    val provider: String,

    @ColumnInfo(name = "bill_type")
    val billType: BillType,

    @ColumnInfo(name = "amount")
    val amount: Long,

    @ColumnInfo(name = "due_date")
    val dueDate: Long,

    @ColumnInfo(name = "status")
    val status: BillStatus,

    @ColumnInfo(name = "paid_at")
    val paidAt: Long? = null,
) {
    init {
        require(amount > 0) { "Bill amount must be strictly positive: $amount" }
    }

    fun toDomainModel(): Bill {
        return Bill(
            id = id,
            userId = userId,
            provider = provider,
            billType = billType,
            amount = amount,
            dueDate = dueDate,
            status = status,
            paidAt = paidAt,
        )
    }

    companion object {
        fun fromDomainModel(bill: Bill): BillEntity {
            return BillEntity(
                id = bill.id,
                userId = bill.userId,
                provider = bill.provider,
                billType = bill.billType,
                amount = bill.amount,
                dueDate = bill.dueDate,
                status = bill.status,
                paidAt = bill.paidAt,
            )
        }
    }
}
