package com.mebanking.app.domain.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Centralized currency formatting utility for Vietnamese Dong (VND).
 * VND uses integer minor units (1 VND = 1 unit) with zero decimal places.
 */
object CurrencyFormatter {

    private val vietnameseLocale = Locale.Builder().setLanguage("vi").setRegion("VN").build()

    private val symbols = DecimalFormatSymbols(vietnameseLocale).apply {
        groupingSeparator = '.'
    }

    private val numberFormat = DecimalFormat("#,##0", symbols)

    /**
     * Formats a Long VND amount into a localized string with the "₫" symbol (e.g. "1.500.000 ₫").
     */
    fun formatVnd(amount: Long): String {
        return "${numberFormat.format(amount)} ₫"
    }

    /**
     * Alias for [formatVnd].
     */
    fun format(amount: Long): String = formatVnd(amount)

    /**
     * Formats a Long VND amount with explicit currency code (e.g. "1.500.000 VND").
     */
    fun formatVndWithCode(amount: Long): String {
        return "${numberFormat.format(amount)} VND"
    }

    /**
     * Formats a Long VND amount without currency symbol/code (e.g. "1.500.000").
     */
    fun formatRawVnd(amount: Long): String {
        return numberFormat.format(amount)
    }
}
