package com.example.financemanager.domain.budgets

import com.example.financemanager.domain.BudgetMonths
import com.example.financemanager.model.Category
import com.example.financemanager.model.Transaction
import com.example.financemanager.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth

/** Share of the budget from which a category is reported as getting close to its limit. */
const val BUDGET_WARNING_SHARE = 0.8

enum class BudgetLevel { OK, WARNING, EXCEEDED }

fun budgetLevel(spent: Double, budget: Double): BudgetLevel = when {
    spent > budget -> BudgetLevel.EXCEEDED
    spent >= budget * BUDGET_WARNING_SHARE -> BudgetLevel.WARNING
    else -> BudgetLevel.OK
}

/** An expense category with what has been spent in it this month. */
data class BudgetItem(val category: Category, val spent: Double) {
    val budget: Double? get() = category.monthlyBudget
    val level: BudgetLevel get() = budget?.let { budgetLevel(spent, it) } ?: BudgetLevel.OK

    /** Spent share of the budget, 0 when there is no budget. Can be above 1. */
    val fraction: Double get() = budget?.takeIf { it > 0 }?.let { spent / it } ?: 0.0
    val remaining: Double get() = (budget ?: 0.0) - spent
}

/** Totals over the categories that have a budget. */
data class BudgetSummary(val spent: Double, val budget: Double, val exceeded: Int) {
    val fraction: Double get() = if (budget > 0) spent / budget else 0.0
    val level: BudgetLevel get() = budgetLevel(spent, budget)
}

/** Expenses in [month], summed per category name. */
fun spentByCategory(
    transactions: List<Transaction>,
    month: YearMonth,
    months: BudgetMonths = BudgetMonths(),
): Map<String, Double> = transactions
    .filter { it.type == TransactionType.EXPENSE && months.of(it.date) == month }
    .groupBy { it.category }
    .mapValues { (_, list) -> list.sumOf { it.amount } }

fun budgetItems(
    categories: List<Category>,
    transactions: List<Transaction>,
    month: YearMonth,
    months: BudgetMonths = BudgetMonths(),
): List<BudgetItem> {
    val spent = spentByCategory(transactions, month, months)
    return categories
        .filter { it.type == TransactionType.EXPENSE }
        .map { BudgetItem(it, spent[it.name] ?: 0.0) }
}

/** Null when no category has a budget. */
fun budgetSummary(items: List<BudgetItem>): BudgetSummary? {
    val budgeted = items.filter { it.budget != null }
    if (budgeted.isEmpty()) return null
    return BudgetSummary(
        spent = budgeted.sumOf { it.spent },
        budget = budgeted.sumOf { it.budget!! },
        exceeded = budgeted.count { it.level == BudgetLevel.EXCEEDED },
    )
}

/** Raised when an expense pushes its category close to or over the monthly budget. */
data class BudgetAlert(val category: String, val level: BudgetLevel, val spent: Double, val budget: Double)

/** The alert to show after spending moved from [before] to [after], or null if nothing new happened. */
fun budgetAlert(category: String, budget: Double, before: Double, after: Double): BudgetAlert? {
    val from = budgetLevel(before, budget)
    val to = budgetLevel(after, budget)
    return if (to > from) BudgetAlert(category, to, after, budget) else null
}

/**
 * The alert for adding [transaction] to the [others] already saved: set when it is an expense
 * of this month that brings its category close to or over budget, otherwise null.
 */
fun budgetAlertFor(
    transaction: Transaction,
    others: List<Transaction>,
    categories: List<Category>,
    today: LocalDate,
    months: BudgetMonths = BudgetMonths(),
): BudgetAlert? {
    val month = months.of(today)
    if (transaction.type != TransactionType.EXPENSE || months.of(transaction.date) != month) return null
    val budget = categories.find { it.type == TransactionType.EXPENSE && it.name == transaction.category }?.monthlyBudget
        ?: return null
    val before = spentByCategory(others, month, months)[transaction.category] ?: 0.0
    return budgetAlert(transaction.category, budget, before, before + transaction.amount)
}
