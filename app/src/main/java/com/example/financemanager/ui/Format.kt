package com.example.financemanager.ui

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

val Croatian: Locale = Locale.forLanguageTag("hr-HR")

/** Formats amounts as Croatian-style euro values, e.g. "1.234,56 €". */
object MoneyFormat {
    private val format = ThreadLocal.withInitial { DecimalFormat("#,##0.00", DecimalFormatSymbols(Croatian)) }

    fun format(amount: Double): String = "${format.get()!!.format(amount)} €"

    fun signed(amount: Double, isIncome: Boolean): String = (if (isIncome) "+" else "−") + format(amount)
}

object DateFormat {
    private val long = DateTimeFormatter.ofPattern("d. MMMM yyyy.", Croatian)
    private val short = DateTimeFormatter.ofPattern("d. MMM yyyy.", Croatian)
    private val chip = DateTimeFormatter.ofPattern("EEE, d. MMM", Croatian)

    fun long(date: LocalDate): String = long.format(date)
    fun short(date: LocalDate): String = short.format(date)
    fun chip(date: LocalDate): String = chip.format(date).replaceFirstChar { it.titlecase(Croatian) }
}

/**
 * Parses what the user typed in the amount field. Accepts either "," or "." as the
 * decimal separator; returns null for anything that isn't a positive number.
 */
fun parseAmount(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 && it.isFinite() }

/** Whether [text] is an acceptable in-progress amount: digits, one separator, two decimals. */
fun isValidAmountInput(text: String): Boolean = AmountInput.matches(text)

private val AmountInput = Regex("""^\d{0,9}([.,]\d{0,2})?$""")
