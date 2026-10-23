package com.example.financemanager.widget

import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import java.time.LocalDate
import java.time.YearMonth

/** What the home screen widget shows. */
data class WidgetSummary(
    /** All income minus all expenses, like "Trenutno stanje" on the dashboard. */
    val balance: Double,
    val monthIncome: Double,
    val monthExpense: Double,
)

fun widgetSummary(transactions: List<Transaction>, today: LocalDate = LocalDate.now()): WidgetSummary {
    val month = YearMonth.from(today)
    fun List<Transaction>.total(type: TransactionType) = filter { it.type == type }.sumOf { it.amount }
    val thisMonth = transactions.filter { YearMonth.from(it.date) == month }
    return WidgetSummary(
        balance = transactions.total(TransactionType.INCOME) - transactions.total(TransactionType.EXPENSE),
        monthIncome = thisMonth.total(TransactionType.INCOME),
        monthExpense = thisMonth.total(TransactionType.EXPENSE),
    )
}
