package com.example.financemanager.ui.transactions

import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import com.example.financemanager.ui.MoneyFormat
import com.example.financemanager.ui.amountInputText
import java.text.Normalizer
import java.time.LocalDate
import java.time.YearMonth

/** Which dates to show. */
sealed interface PeriodFilter {
    data object All : PeriodFilter
    data object ThisMonth : PeriodFilter
    data object LastMonth : PeriodFilter
    data object ThisYear : PeriodFilter
    data class Range(val from: LocalDate, val to: LocalDate) : PeriodFilter

    /** The dates included, or null for all of them. */
    fun dates(today: LocalDate): ClosedRange<LocalDate>? = when (this) {
        All -> null
        ThisMonth -> YearMonth.from(today).let { it.atDay(1)..it.atEndOfMonth() }
        LastMonth -> YearMonth.from(today).minusMonths(1).let { it.atDay(1)..it.atEndOfMonth() }
        ThisYear -> today.withDayOfYear(1)..today.withDayOfYear(today.lengthOfYear())
        is Range -> minOf(from, to)..maxOf(from, to)
    }
}

data class TransactionFilter(
    val query: String = "",
    /** Null shows both income and expenses. */
    val type: TransactionType? = null,
    val period: PeriodFilter = PeriodFilter.All,
    /** Category name, or null for every category. */
    val category: String? = null,
) {
    val isActive: Boolean get() = this != TransactionFilter()
}

/** One day of the list, newest transactions first. */
data class DayGroup(val date: LocalDate, val transactions: List<Transaction>)

fun applyFilter(transactions: List<Transaction>, filter: TransactionFilter, today: LocalDate): List<Transaction> {
    val dates = filter.period.dates(today)
    val query = normalize(filter.query.trim())
    return transactions.filter { transaction ->
        (filter.type == null || transaction.type == filter.type) &&
            (dates == null || transaction.date in dates) &&
            (filter.category == null || transaction.category == filter.category) &&
            (query.isEmpty() || matches(transaction, query))
    }
}

fun groupByDay(transactions: List<Transaction>): List<DayGroup> = transactions
    .sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.id })
    .groupBy { it.date }
    .map { (date, list) -> DayGroup(date, list) }

/** Matches the note, the category or the amount ("45", "45,5", "1.234,00"), ignoring case and diacritics. */
private fun matches(transaction: Transaction, query: String): Boolean =
    normalize(transaction.note).contains(query) ||
        normalize(transaction.category).contains(query) ||
        MoneyFormat.format(transaction.amount).contains(query) ||
        amountInputText(transaction.amount).contains(query.replace('.', ','))

private val Marks = Regex("\\p{Mn}+")

/** Lower case without diacritics, so "rezije" finds "Režije" and "dacic" finds "Đačić". */
internal fun normalize(text: String): String =
    Marks.replace(Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD), "").replace('đ', 'd')
