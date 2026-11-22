package com.mebanking.app.data.local

import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.model.BillStatus
import com.mebanking.app.domain.model.BillType
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.TransactionStatus
import com.mebanking.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun roleConversion_preservesEnum() {
        assertEquals("CUSTOMER", converters.fromRole(Role.CUSTOMER))
        assertEquals("OFFICER", converters.fromRole(Role.OFFICER))
        assertEquals(Role.CUSTOMER, converters.toRole("CUSTOMER"))
        assertEquals(Role.OFFICER, converters.toRole("OFFICER"))
        assertNull(converters.fromRole(null))
        assertNull(converters.toRole(null))
    }

    @Test
    fun accountTypeConversion_preservesEnum() {
        assertEquals("CHECKING", converters.fromAccountType(AccountType.CHECKING))
        assertEquals("SAVING", converters.fromAccountType(AccountType.SAVING))
        assertEquals("MORTGAGE", converters.fromAccountType(AccountType.MORTGAGE))
        assertEquals(AccountType.CHECKING, converters.toAccountType("CHECKING"))
        assertEquals(AccountType.SAVING, converters.toAccountType("SAVING"))
        assertEquals(AccountType.MORTGAGE, converters.toAccountType("MORTGAGE"))
    }

    @Test
    fun transactionTypeAndStatus_preservesEnum() {
        assertEquals("INTERNAL_TRANSFER", converters.fromTransactionType(TransactionType.INTERNAL_TRANSFER))
        assertEquals(TransactionType.INTERNAL_TRANSFER, converters.toTransactionType("INTERNAL_TRANSFER"))

        assertEquals("SUCCESS", converters.fromTransactionStatus(TransactionStatus.SUCCESS))
        assertEquals(TransactionStatus.SUCCESS, converters.toTransactionStatus("SUCCESS"))
    }

    @Test
    fun billTypeAndStatus_preservesEnum() {
        assertEquals("ELECTRICITY", converters.fromBillType(BillType.ELECTRICITY))
        assertEquals(BillType.ELECTRICITY, converters.toBillType("ELECTRICITY"))

        assertEquals("UNPAID", converters.fromBillStatus(BillStatus.UNPAID))
        assertEquals(BillStatus.UNPAID, converters.toBillStatus("UNPAID"))
    }

    @Test
    fun stringListConversion_serializesAndDeserializes() {
        val original = listOf("chuyển tiền", "bắn tiền", "chuyển khoản")
        val json = converters.fromStringList(original)
        val deserialized = converters.toStringList(json)
        assertEquals(original, deserialized)
        assertNull(converters.fromStringList(null))
        assertNull(converters.toStringList(null))
    }
}
