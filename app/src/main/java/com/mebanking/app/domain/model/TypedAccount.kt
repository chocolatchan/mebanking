package com.mebanking.app.domain.model

/**
 * Domain-level typed account representation distinguishing Checking, Saving, and Mortgage accounts.
 * Isolates presentation and business calculation rules per account discipline.
 */
sealed interface TypedAccount {
    val id: Long
    val userId: Long
    val accountNumber: String
    val currency: String
    val status: String

    data class Checking(
        override val id: Long,
        override val userId: Long,
        override val accountNumber: String,
        val availableBalance: Long,
        override val currency: String = "VND",
        override val status: String = "ACTIVE",
    ) : TypedAccount {
        init {
            require(availableBalance >= 0) { "Checking available balance cannot be negative: $availableBalance" }
        }
    }

    data class Saving(
        override val id: Long,
        override val userId: Long,
        override val accountNumber: String,
        val principalBalance: Long,
        val interestRate: Double,
        override val currency: String = "VND",
        override val status: String = "ACTIVE",
    ) : TypedAccount {
        init {
            require(principalBalance >= 0) { "Saving principal balance cannot be negative: $principalBalance" }
            require(interestRate >= 0.0) { "Saving interest rate cannot be negative: $interestRate" }
        }
    }

    data class Mortgage(
        override val id: Long,
        override val userId: Long,
        override val accountNumber: String,
        val balance: Long,
        val loanAmount: Long,
        val monthlyPayment: Long,
        val payCycle: String,
        val interestRate: Double = 0.0,
        override val currency: String = "VND",
        override val status: String = "ACTIVE",
    ) : TypedAccount {
        init {
            require(balance >= 0) { "Mortgage balance cannot be negative: $balance" }
            require(loanAmount >= 0) { "Loan amount cannot be negative: $loanAmount" }
            require(monthlyPayment >= 0) { "Monthly payment cannot be negative: $monthlyPayment" }
        }
    }
}

/**
 * Detailed projection of a Mortgage account containing loan schedule and periodic obligations.
 */
data class MortgageAccountDetails(
    val accountId: Long,
    val accountNumber: String,
    val loanAmount: Long,
    val monthlyPayment: Long,
    val payCycle: String,
    val remainingBalance: Long = 0L,
    val currency: String = "VND",
)

/**
 * Maps raw generic [Account] entity into a type-safe [TypedAccount] domain model.
 */
fun Account.toTypedAccount(): TypedAccount = when (accountType) {
    AccountType.CHECKING -> TypedAccount.Checking(
        id = id,
        userId = userId,
        accountNumber = accountNumber,
        availableBalance = balance,
        currency = currency,
        status = status,
    )
    AccountType.SAVING -> TypedAccount.Saving(
        id = id,
        userId = userId,
        accountNumber = accountNumber,
        principalBalance = balance,
        interestRate = interestRate,
        currency = currency,
        status = status,
    )
    AccountType.MORTGAGE -> TypedAccount.Mortgage(
        id = id,
        userId = userId,
        accountNumber = accountNumber,
        balance = balance,
        loanAmount = loanAmount ?: 0L,
        monthlyPayment = monthlyPayment ?: 0L,
        payCycle = payCycle ?: "MONTHLY",
        interestRate = interestRate,
        currency = currency,
        status = status,
    )
}
