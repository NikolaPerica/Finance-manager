package com.example.financemanager.domain.budgets

import com.example.financemanager.domain.BudgetMonths
import com.example.financemanager.model.Category
import com.example.financemanager.model.Reminder
import com.example.financemanager.model.Transaction
import com.example.financemanager.model.TransactionType
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * How much may still be spent today without going over the monthly budgets: what is left
 * of them at the start of today, shared out evenly over the days left in the month.
 */
data class SafeToSpend(
    /** What may still be spent today; below zero when today's share is already used up. */
    val today: Double,
    /** Today's share of what was left this morning. */
    val dailyShare: Double,
    /** Days left in the month, today included. */
    val daysLeft: Int,
    /** Budget left for the rest of the month, after reminder payments still due; below zero when over. */
    val leftThisMonth: Double,
    /** Reminder payments still due this month, already set aside from what is left. */
    val stillDue: Double,
)

/**
 * Null when no expense category has a budget, as there is nothing to share out. Only
 * spending in budgeted categories counts, as only those have a limit.
 */
fun safeToSpend(
    categories: List<Category>,
    transactions: List<Transaction>,
    reminders: List<Reminder>,
    today: LocalDate,
    months: BudgetMonths = BudgetMonths(),
): SafeToSpend? {
    val budgeted = categories.filter { it.type == TransactionType.EXPENSE && it.monthlyBudget != null }
    if (budgeted.isEmpty()) return null
    val names = budgeted.map { it.name }.toSet()
    val month = months.of(today)
    val last = months.last(month)

    val spending = transactions.filter { it.type == TransactionType.EXPENSE && it.category in names && months.of(it.date) == month }
    val spentBefore = spending.filter { it.date < today }.sumOf { it.amount }
    val spentToday = spending.filter { it.date == today }.sumOf { it.amount }
    // Payments not made yet that will land in a budgeted category by the end of the month,
    // overdue ones included.
    val stillDue = reminders
        .filter { !it.nextDueDate.isAfter(last) && it.toExpenseCategory() in names }
        .sumOf { it.amount }

    val budget = budgeted.sumOf { it.monthlyBudget!! }
    val daysLeft = ChronoUnit.DAYS.between(today, last).toInt() + 1
    val dailyShare = (budget - spentBefore - stillDue).coerceAtLeast(0.0) / daysLeft
    return SafeToSpend(
        today = dailyShare - spentToday,
        dailyShare = dailyShare,
        daysLeft = daysLeft,
        leftThisMonth = budget - spentBefore - spentToday - stillDue,
        stillDue = stillDue,
    )
}

// The same category a payment is recorded under, see FinanceRepository's toExpense.
private fun Reminder.toExpenseCategory() = category.ifBlank { name }
