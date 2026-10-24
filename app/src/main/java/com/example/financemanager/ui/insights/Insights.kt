package com.example.financemanager.ui.insights

import com.example.financemanager.data.Category
import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

/*
 * Insights compare this month with how the previous months went, always for the same part
 * of the month: on the 10th, the 1st-10th of this month against the 1st-10th of earlier ones.
 * That keeps the comparison fair mid-month (rent paid on the 1st doesn't make the first week
 * look expensive) and lets the rest of earlier months predict the rest of this one.
 */

/** How many earlier months are compared with this one. */
const val HISTORY_MONTHS = 3

/** Smallest change against the usual amount that is worth mentioning. */
const val NOTABLE_CHANGE = 0.2

/** Amounts below this are too small to talk about, in euros. */
const val MIN_AMOUNT = 10.0

sealed interface Insight {

    /**
     * Expected balance at the end of the month: the current balance plus what usually still
     * comes in and goes out after today, from [basedOnMonths] earlier months. With no history,
     * [basedOnMonths] is 0 and only this month's spending pace is used.
     */
    data class MonthEndForecast(
        val balanceNow: Double,
        val expectedIncome: Double,
        val expectedExpense: Double,
        val basedOnMonths: Int,
    ) : Insight {
        val projectedBalance: Double get() = balanceNow + expectedIncome - expectedExpense
    }

    /** A category costs notably more (or less) so far this month than usual by this day. */
    data class CategoryTrend(val category: String, val current: Double, val usual: Double) : Insight {
        /** Relative change, e.g. 0.25 for 25 % more. */
        val change: Double get() = (current - usual) / usual
    }

    /** At the usual pace the category will end the month over its budget. */
    data class BudgetRisk(val category: String, val budget: Double, val projected: Double) : Insight

    /** One expense much bigger than the category's usual one. */
    data class UnusualExpense(val transaction: Transaction, val usual: Double) : Insight

    /** How last month ended: income minus expenses. */
    data class LastMonth(val month: YearMonth, val income: Double, val expense: Double) : Insight {
        val saved: Double get() = income - expense

        /** Saved share of income, or null without income. */
        val savedShare: Double? get() = income.takeIf { it > 0 }?.let { saved / it }
    }
}

/** Earlier months that the data fully covers, newest first (at most [HISTORY_MONTHS]). */
internal fun historyMonths(transactions: List<Transaction>, today: LocalDate): List<YearMonth> {
    val first = transactions.minOfOrNull { it.date } ?: return emptyList()
    val current = YearMonth.from(today)
    // A month counts only if the data starts on or before its first day; a half-recorded
    // first month would make "usual" look too low.
    return (1..HISTORY_MONTHS).map { current.minusMonths(it.toLong()) }.filter { !first.isAfter(it.atDay(1)) }
}

/** The same days of [month] as 1..[day] of the current month (shorter months stop at their end). */
private fun YearMonth.upTo(day: Int): ClosedRange<LocalDate> = atDay(1)..atDay(minOf(day, lengthOfMonth()))

private fun YearMonth.after(day: Int): ClosedRange<LocalDate>? =
    if (day >= lengthOfMonth()) null else atDay(day + 1)..atEndOfMonth()

private fun List<Transaction>.sum(type: TransactionType, dates: ClosedRange<LocalDate>?, category: String? = null) =
    if (dates == null) {
        0.0
    } else {
        filter { it.type == type && it.date in dates && (category == null || it.category == category) }.sumOf { it.amount }
    }

fun buildInsights(
    transactions: List<Transaction>,
    categories: List<Category>,
    today: LocalDate = LocalDate.now(),
): List<Insight> {
    if (transactions.isEmpty()) return emptyList()
    val month = YearMonth.from(today)
    val day = today.dayOfMonth
    val soFar = month.atDay(1)..today
    val history = historyMonths(transactions, today)
    val expenses = transactions.filter { it.type == TransactionType.EXPENSE }

    val insights = mutableListOf<Insight>()

    forecast(transactions, today, history)?.let(insights::add)

    if (history.isNotEmpty()) {
        // Categories that changed most, in euros, first.
        expenses.map { it.category }.distinct()
            .mapNotNull { category ->
                val current = transactions.sum(TransactionType.EXPENSE, soFar, category)
                val usual = history.map { transactions.sum(TransactionType.EXPENSE, it.upTo(day), category) }.average()
                Insight.CategoryTrend(category, current, usual).takeIf {
                    usual >= MIN_AMOUNT && maxOf(current, usual) >= MIN_AMOUNT && abs(it.change) >= NOTABLE_CHANGE
                }
            }
            .sortedByDescending { abs(it.current - it.usual) }
            .take(3)
            .let(insights::addAll)

        categories.filter { it.type == TransactionType.EXPENSE && it.monthlyBudget != null }.forEach { category ->
            val budget = category.monthlyBudget!!
            val spent = transactions.sum(TransactionType.EXPENSE, soFar, category.name)
            val rest = history.map { transactions.sum(TransactionType.EXPENSE, it.after(day), category.name) }.average()
            val projected = spent + rest
            // Only a warning while it can still be avoided.
            if (spent <= budget && projected > budget) insights += Insight.BudgetRisk(category.name, budget, projected)
        }
    }

    unusualExpense(expenses, soFar)?.let(insights::add)

    val last = month.minusMonths(1)
    val lastDates = last.atDay(1)..last.atEndOfMonth()
    if (last in history) {
        insights += Insight.LastMonth(
            last,
            transactions.sum(TransactionType.INCOME, lastDates),
            transactions.sum(TransactionType.EXPENSE, lastDates),
        )
    }
    return insights
}

private fun forecast(transactions: List<Transaction>, today: LocalDate, history: List<YearMonth>): Insight.MonthEndForecast? {
    val month = YearMonth.from(today)
    val day = today.dayOfMonth
    // The same balance the dashboard shows.
    val balance = transactions.sumOf { if (it.type == TransactionType.INCOME) it.amount else -it.amount }
    if (history.isNotEmpty()) {
        return Insight.MonthEndForecast(
            balanceNow = balance,
            expectedIncome = history.map { transactions.sum(TransactionType.INCOME, it.after(day)) }.average(),
            expectedExpense = history.map { transactions.sum(TransactionType.EXPENSE, it.after(day)) }.average(),
            basedOnMonths = history.size,
        )
    }
    // No earlier month yet: after the first week, continue this month's spending pace.
    if (day < 7) return null
    val spent = transactions.sum(TransactionType.EXPENSE, month.atDay(1)..today)
    if (spent <= 0) return null
    val daysLeft = month.lengthOfMonth() - day
    return Insight.MonthEndForecast(balance, expectedIncome = 0.0, expectedExpense = spent / day * daysLeft, basedOnMonths = 0)
}

/** This month's expense that is at least three times its category's usual one, the biggest such. */
private fun unusualExpense(expenses: List<Transaction>, soFar: ClosedRange<LocalDate>): Insight.UnusualExpense? =
    expenses.filter { it.date in soFar && it.amount >= MIN_AMOUNT }
        .mapNotNull { transaction ->
            // The usual expense in the category, from everything else (needs a few to be meaningful).
            val others = expenses.filter { it.category == transaction.category && it.id != transaction.id }
            if (others.size < 4) return@mapNotNull null
            val usual = others.map { it.amount }.sorted().let { it[it.size / 2] }
            Insight.UnusualExpense(transaction, usual).takeIf { transaction.amount >= 3 * usual }
        }
        .maxByOrNull { it.transaction.amount - it.usual }
