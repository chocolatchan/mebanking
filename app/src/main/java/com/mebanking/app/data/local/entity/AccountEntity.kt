package com.mebanking.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mebanking.app.domain.model.Account
import com.mebanking.app.domain.model.AccountType

/**
 * Room entity representing a bank account (Checking, Saving, or Mortgage).
 *
 * Invariant: Non-negative balance constraint (balance >= 0) in 64-bit Long VND minor units.
 */
@Entity(
    tableName = "accounts",
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
        Index(value = ["account_number"], unique = true),
    ],
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "account_number")
    val accountNumber: String,

    @ColumnInfo(name = "account_type")
    val accountType: AccountType,

    @ColumnInfo(name = "balance")
    val balance: Long,

    @ColumnInfo(name = "currency")
    val currency: String = "VND",

    @ColumnInfo(name = "interest_rate")
    val interestRate: Double = 0.0,

    @ColumnInfo(name = "status")
    val status: String = "ACTIVE",

    @ColumnInfo(name = "loan_amount")
    val loanAmount: Long? = null,

    @ColumnInfo(name = "monthly_payment")
    val monthlyPayment: Long? = null,

    @ColumnInfo(name = "pay_cycle")
    val payCycle: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
) {
    init {
        require(balance >= 0) { "Account balance cannot be negative: $balance" }
    }

    fun toDomainModel(): Account {
        return Account(
            id = id,
            userId = userId,
            accountNumber = accountNumber,
            accountType = accountType,
            balance = balance,
            currency = currency,
            interestRate = interestRate,
            status = status,
            loanAmount = loanAmount,
            monthlyPayment = monthlyPayment,
            payCycle = payCycle,
            createdAt = createdAt,
        )
    }

    companion object {
        fun fromDomainModel(account: Account): AccountEntity {
            return AccountEntity(
                id = account.id,
                userId = account.userId,
                accountNumber = account.accountNumber,
                accountType = account.accountType,
                balance = account.balance,
                currency = account.currency,
                interestRate = account.interestRate,
                status = account.status,
                loanAmount = account.loanAmount,
                monthlyPayment = account.monthlyPayment,
                payCycle = account.payCycle,
                createdAt = account.createdAt,
            )
        }
    }
}
