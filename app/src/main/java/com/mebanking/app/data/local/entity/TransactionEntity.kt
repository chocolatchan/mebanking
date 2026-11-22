package com.mebanking.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mebanking.app.domain.model.Transaction
import com.mebanking.app.domain.model.TransactionStatus
import com.mebanking.app.domain.model.TransactionType

/**
 * Room entity representing a transaction in the financial ledger.
 *
 * Invariant: Transaction amount must be strictly positive (amount > 0) in 64-bit Long VND units.
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["from_account_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["to_account_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = ExternalBankEntity::class,
            parentColumns = ["id"],
            childColumns = ["external_bank_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["from_account_id"]),
        Index(value = ["to_account_id"]),
        Index(value = ["external_bank_id"]),
        Index(value = ["timestamp"]),
        Index(value = ["idempotency_key"], unique = true),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "from_account_id")
    val fromAccountId: Long? = null,

    @ColumnInfo(name = "to_account_id")
    val toAccountId: Long? = null,

    @ColumnInfo(name = "external_bank_id")
    val externalBankId: Long? = null,

    @ColumnInfo(name = "to_account_no")
    val toAccountNo: String? = null,

    @ColumnInfo(name = "amount")
    val amount: Long,

    @ColumnInfo(name = "type")
    val type: TransactionType,

    @ColumnInfo(name = "status")
    val status: TransactionStatus,

    @ColumnInfo(name = "note")
    val note: String? = null,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "idempotency_key")
    val idempotencyKey: String? = null,
) {
    init {
        require(amount > 0) { "Transaction amount must be strictly positive: $amount" }
    }

    fun toDomainModel(): Transaction {
        return Transaction(
            id = id,
            fromAccountId = fromAccountId,
            toAccountId = toAccountId,
            externalBankId = externalBankId,
            toAccountNo = toAccountNo,
            amount = amount,
            type = type,
            status = status,
            note = note,
            timestamp = timestamp,
            idempotencyKey = idempotencyKey,
        )
    }

    companion object {
        fun fromDomainModel(tx: Transaction): TransactionEntity {
            return TransactionEntity(
                id = tx.id,
                fromAccountId = tx.fromAccountId,
                toAccountId = tx.toAccountId,
                externalBankId = tx.externalBankId,
                toAccountNo = tx.toAccountNo,
                amount = tx.amount,
                type = tx.type,
                status = tx.status,
                note = tx.note,
                timestamp = tx.timestamp,
                idempotencyKey = tx.idempotencyKey,
            )
        }
    }
}
