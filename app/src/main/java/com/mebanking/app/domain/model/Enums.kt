package com.mebanking.app.domain.model

enum class Role {
    CUSTOMER,
    OFFICER,
}

enum class AccountType {
    CHECKING,
    SAVING,
    MORTGAGE;

    fun vietnameseName(): String = when (this) {
        CHECKING -> "Thanh toán"
        SAVING -> "Tiết kiệm"
        MORTGAGE -> "Thế chấp"
    }

    fun fullVietnameseName(): String = when (this) {
        CHECKING -> "Tài khoản thanh toán"
        SAVING -> "Tài khoản tiết kiệm"
        MORTGAGE -> "Tài khoản thế chấp"
    }
}

enum class TransactionType {
    INTERNAL_TRANSFER,
    INTERBANK_TRANSFER,
    BILL_PAYMENT,
    DEPOSIT,
    WITHDRAWAL,
}

enum class TransactionStatus {
    PENDING,
    SUCCESS,
    FAILED,
}

enum class BillType {
    ELECTRICITY,
    WATER,
    INTERNET,
    PHONE,
}

enum class BillStatus {
    UNPAID,
    PAID,
    OVERDUE,
}
