package com.example.financemanager.ui

import android.content.Context
import com.example.financemanager.R
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

val CROATIAN: Locale = Locale("hr", "HR")

/** Formats amounts as Croatian-style euro values, e.g. "1.234,56 €". */
object MoneyFormat {
    private val format = DecimalFormat("#,##0.00", DecimalFormatSymbols(CROATIAN))

    fun format(amount: Double): String = "${format.format(amount)} €"
}

/** Turns stored "yyyy-MM-dd" dates into friendly labels like "Danas" or "3. lis 2026.". */
object DateLabels {
    private val storage = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val display = SimpleDateFormat("d. MMM yyyy.", CROATIAN)

    fun relative(context: Context, date: String): String {
        if (date.isBlank()) return ""
        val parsed = try {
            storage.parse(date)
        } catch (e: ParseException) {
            null
        } ?: return date

        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        return when (date) {
            storage.format(today.time) -> context.getString(R.string.today)
            storage.format(yesterday.time) -> context.getString(R.string.yesterday)
            else -> display.format(parsed)
        }
    }
}
