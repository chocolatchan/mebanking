package com.mebanking.app.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class VietQrParserTest {

    @Test
    fun parse_validEmvCoVietQr_extractsAllFields() {
        // Standard EMVCo VietQR:
        // 00: 01 (Version)
        // 01: 12 (Dynamic QR)
        // 38: Merchant Account Info -> 00: A000000727 (NAPAS), 01: 00: 970436 (VCB), 01: 100123456789
        // 53: 704 (VND)
        // 54: 50000 (Amount)
        // 58: VN
        // 59: NGUYEN LAN ANH (Name)
        // 62: 08: Tien an trua (Note)
        val payload = "00020101021238560010A0000007270126000697043601121001234567890208QRIBFTTA53037045405500005802VN5914NGUYEN LAN ANH62160812Tien an trua6304ABCD"

        val result = VietQrParser.parse(payload)

        assertNotNull(result)
        assertEquals("100123456789", result!!.accountNumber)
        assertEquals("970436", result.bankBin)
        assertEquals("Vietcombank (VCB)", result.bankName)
        assertEquals(50000L, result.amount)
        assertEquals("NGUYEN LAN ANH", result.recipientName)
        assertEquals("Tien an trua", result.note)
    }

    @Test
    fun parse_vietQrUrl_extractsFields() {
        val url = "https://img.vietqr.io/image/970407-200987654321-compact2.png?amount=120000&addInfo=Thanh%20toan%20hoa%20don&accountName=Tran%20Van%20Binh"

        val result = VietQrParser.parse(url)

        assertNotNull(result)
        assertEquals("200987654321", result!!.accountNumber)
        assertEquals("970407", result.bankBin)
        assertEquals("Techcombank (TCB)", result.bankName)
        assertEquals(120000L, result.amount)
        assertEquals("Thanh toan hoa don", result.note)
        assertEquals("Tran Van Binh", result.recipientName)
    }

    @Test
    fun parse_labeledPlainText_extractsFields() {
        val plainText = """
            STK: 100888999111
            Ngân hàng: MBBank
            Người nhận: Lê Hoàng Yến
            Số tiền: 250000
            Nội dung: Chuc mung sinh nhat
        """.trimIndent()

        val result = VietQrParser.parse(plainText)

        assertNotNull(result)
        assertEquals("100888999111", result!!.accountNumber)
        assertEquals(250000L, result.amount)
        assertEquals("Chuc mung sinh nhat", result.note)
        assertEquals("Lê Hoàng Yến", result.recipientName)
    }

    @Test
    fun parse_delimitedText_extractsFields() {
        val delimited = "100666777222 | 80000 | Ca phe sang"

        val result = VietQrParser.parse(delimited)

        assertNotNull(result)
        assertEquals("100666777222", result!!.accountNumber)
        assertEquals(80000L, result.amount)
        assertEquals("Ca phe sang", result.note)
    }

    @Test
    fun parse_plainNumericAccountNumber_extractsAccountNumber() {
        val raw = "100123456789"

        val result = VietQrParser.parse(raw)

        assertNotNull(result)
        assertEquals("100123456789", result!!.accountNumber)
        assertNull(result.amount)
    }

    @Test
    fun parse_invalidString_returnsNull() {
        assertNull(VietQrParser.parse(""))
        assertNull(VietQrParser.parse("   "))
        assertNull(VietQrParser.parse("Hello World Not An Account"))
    }
}
