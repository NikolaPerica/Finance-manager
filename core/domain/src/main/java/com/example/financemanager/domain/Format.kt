package com.example.financemanager.domain

import com.example.financemanager.model.AppCurrency
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

val Croatian: Locale = Locale.forLanguageTag("hr-HR")

/** Formats amounts Croatian-style with the chosen currency's sign, e.g. "1.234,56 €". */
object MoneyFormat {
    /** Set from the settings when the app starts and whenever the currency changes. */
    @Volatile
    var symbol: String = AppCurrency.EUR.symbol

    private val format = ThreadLocal.withInitial { DecimalFormat("#,##0.00", DecimalFormatSymbols(Croatian)) }

    // A no-break space keeps the currency sign on the same line as the number.
    fun format(amount: Double): String = "${format.get()!!.format(amount)}\u00A0$symbol"

    fun signed(amount: Double, isIncome: Boolean): String = (if (isIncome) "+" else "−") + format(amount)
}

object DateFormat {
    private val long = DateTimeFormatter.ofPattern("d. MMMM yyyy.", Croatian)
    private val short = DateTimeFormatter.ofPattern("d. MMM yyyy.", Croatian)
    private val chip = DateTimeFormatter.ofPattern("EEE, d. MMM", Croatian)
    private val dayMonth = DateTimeFormatter.ofPattern("d. MMM", Croatian)

    fun long(date: LocalDate): String = long.format(date)
    fun short(date: LocalDate): String = short.format(date)
    fun chip(date: LocalDate): String = chip.format(date).replaceFirstChar { it.titlecase(Croatian) }

    /** A span of days, e.g. "15. lis – 14. stu 2026." */
    fun range(dates: ClosedRange<LocalDate>): String = "${dayMonth.format(dates.start)} – ${short.format(dates.endInclusive)}"
}

/** An amount as it is typed into an amount field, e.g. "1234,5", so it round-trips through [parseAmount]. */
fun amountInputText(amount: Double): String = AmountInputFormat.get()!!.format(amount)

private val AmountInputFormat = ThreadLocal.withInitial { DecimalFormat("0.##", DecimalFormatSymbols(Croatian)) }

/**
 * Parses what the user typed in the amount field. Accepts either "," or "." as the
 * decimal separator; returns null for anything that isn't a positive number.
 */
fun parseAmount(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 && it.isFinite() }

/** Whether [text] is an acceptable in-progress amount: digits, one separator, two decimals. */
fun isValidAmountInput(text: String): Boolean = AmountInput.matches(text)

private val AmountInput = Regex("""^\d{0,9}([.,]\d{0,2})?$""")
