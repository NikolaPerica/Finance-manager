package com.example.financemanager.domain.export

import com.example.financemanager.domain.Croatian
import com.example.financemanager.model.Transaction
import com.example.financemanager.model.TransactionType
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.format.DateTimeFormatter

/** Column titles and type names, from string resources so they follow the app's language. */
data class CsvLabels(
    val date: String,
    val type: String,
    val category: String,
    val amount: String,
    val note: String,
    val income: String,
    val expense: String,
)

/**
 * Transactions as CSV that Excel and LibreOffice open correctly with Croatian settings:
 * semicolon-separated, decimal comma, a UTF-8 byte order mark so "č" and "đ" survive,
 * and expenses as negative amounts so a column sum gives the balance. Oldest first.
 */
fun transactionsCsv(transactions: List<Transaction>, labels: CsvLabels): String = buildString {
    append('\uFEFF')
    appendRow(listOf(labels.date, labels.type, labels.category, labels.amount, labels.note))
    transactions.sortedWith(compareBy<Transaction> { it.date }.thenBy { it.id }).forEach {
        val isIncome = it.type == TransactionType.INCOME
        appendRow(
            listOf(
                CsvDate.format(it.date),
                if (isIncome) labels.income else labels.expense,
                it.category,
                CsvAmount.get()!!.format(if (isIncome) it.amount else -it.amount),
                it.note,
            ),
        )
    }
}

private fun StringBuilder.appendRow(cells: List<String>) {
    cells.joinTo(this, ";") { escape(it) }
    append("\r\n")
}

/** Quotes a cell that contains the separator, a quote or a line break. */
private fun escape(cell: String): String =
    if (cell.any { it == ';' || it == '"' || it == '\n' || it == '\r' }) "\"" + cell.replace("\"", "\"\"") + "\"" else cell

private val CsvDate = DateTimeFormatter.ofPattern("dd.MM.yyyy.")
// Croatian formatting uses a Unicode minus sign, which spreadsheets don't read as a number.
private val CsvAmount = ThreadLocal.withInitial {
    DecimalFormat("0.00", DecimalFormatSymbols(Croatian).apply { minusSign = '-' })
}
