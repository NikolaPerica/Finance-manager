package com.example.financemanager

import com.example.financemanager.data.Converters
import com.example.financemanager.ui.DateFormat
import com.example.financemanager.ui.MoneyFormat
import com.example.financemanager.ui.isValidAmountInput
import com.example.financemanager.ui.parseAmount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FormatTest {
    @Test
    fun moneyUsesCroatianSeparators() {
        assertEquals("1.234,56 €", MoneyFormat.format(1234.56))
        assertEquals("0,00 €", MoneyFormat.format(0.0))
        assertEquals("+12,50 €", MoneyFormat.signed(12.5, isIncome = true))
        assertEquals("−12,50 €", MoneyFormat.signed(12.5, isIncome = false))
    }

    @Test
    fun parsesEitherDecimalSeparator() {
        assertEquals(12.5, parseAmount("12,5")!!, 0.0)
        assertEquals(12.5, parseAmount("12.5")!!, 0.0)
        assertNull(parseAmount(""))
        assertNull(parseAmount("0"))
        assertNull(parseAmount(","))
    }

    @Test
    fun amountInputAllowsTwoDecimals() {
        assertTrue(isValidAmountInput(""))
        assertTrue(isValidAmountInput("12,"))
        assertTrue(isValidAmountInput("12,34"))
        assertFalse(isValidAmountInput("12,345"))
        assertFalse(isValidAmountInput("1,2,3"))
        assertFalse(isValidAmountInput("-5"))
    }

    @Test
    fun formatsDatesInCroatian() {
        assertEquals("5. listopada 2026.", DateFormat.long(LocalDate.of(2026, 10, 5)))
    }

    @Test
    fun storesDatesInTheOriginalTextFormat() {
        val converters = Converters()
        assertEquals("2026-10-05", converters.dateToText(LocalDate.of(2026, 10, 5)))
        assertEquals(LocalDate.of(2026, 10, 5), converters.textToDate("2026-10-05"))
    }
}
