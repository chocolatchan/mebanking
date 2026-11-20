package com.mebanking.app.domain

import com.mebanking.app.domain.util.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun formatVnd_standardAmounts_formatsWithDotsAndSymbol() {
        assertEquals("1.500.000 ₫", CurrencyFormatter.formatVnd(1_500_000L))
        assertEquals("50.000 ₫", CurrencyFormatter.formatVnd(50_000L))
        assertEquals("100.000.000 ₫", CurrencyFormatter.formatVnd(100_000_000L))
    }

    @Test
    fun formatVnd_zeroAmount_formatsCorrectly() {
        assertEquals("0 ₫", CurrencyFormatter.formatVnd(0L))
    }

    @Test
    fun formatVnd_negativeAmount_formatsWithMinus() {
        assertEquals("-50.000 ₫", CurrencyFormatter.formatVnd(-50_000L))
    }

    @Test
    fun formatVndWithCode_formatsWithVndCode() {
        assertEquals("1.500.000 VND", CurrencyFormatter.formatVndWithCode(1_500_000L))
        assertEquals("0 VND", CurrencyFormatter.formatVndWithCode(0L))
    }

    @Test
    fun formatRawVnd_formatsWithoutSymbol() {
        assertEquals("1.500.000", CurrencyFormatter.formatRawVnd(1_500_000L))
        assertEquals("0", CurrencyFormatter.formatRawVnd(0L))
    }
}
