package com.mebanking.app.domain.model

enum class Role {
    CUSTOMER,
    OFFICER,
}

enum class AccountType {
    CHECKING,
    SAVING,
    MORTGAGE,
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
