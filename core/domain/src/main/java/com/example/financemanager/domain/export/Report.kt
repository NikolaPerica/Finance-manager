package com.example.financemanager.domain.export

import com.example.financemanager.domain.stats.CategoryStat
import com.example.financemanager.domain.stats.StatsPeriod
import com.example.financemanager.domain.stats.buildStats
import com.example.financemanager.model.Transaction
import com.example.financemanager.model.TransactionType
import java.time.LocalDate

/** What a financial report for one period contains. */
data class Report(
    val period: StatsPeriod,
    val income: Double,
    val expense: Double,
    /** Largest first. */
    val expenseCategories: List<CategoryStat>,
    val incomeCategories: List<CategoryStat>,
    /** Oldest first. */
    val transactions: List<Transaction>,
) {
    val balance: Double get() = income - expense
}

fun buildReport(transactions: List<Transaction>, period: StatsPeriod, today: LocalDate = LocalDate.now()): Report {
    val expenses = buildStats(transactions, TransactionType.EXPENSE, period, today)
    val income = buildStats(transactions, TransactionType.INCOME, period, today)
    return Report(
        period = period,
        income = income.total,
        expense = expenses.total,
        expenseCategories = expenses.categories,
        incomeCategories = income.categories,
        transactions = transactions.filter { it.date in period }.sortedWith(compareBy<Transaction> { it.date }.thenBy { it.id }),
    )
}
