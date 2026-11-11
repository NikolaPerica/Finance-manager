package com.example.financemanager.domain.habits

import com.example.financemanager.model.Transaction
import com.example.financemanager.model.TransactionType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/*
 * Spending habits day by day and week by week. Days before the first transaction don't count
 * as days without spending: nothing was recorded on them yet.
 */

/** Expenses of each day of [month]; days without any are left out. */
fun dailySpending(transactions: List<Transaction>, month: YearMonth): Map<LocalDate, Double> = transactions
    .filter { it.type == TransactionType.EXPENSE && YearMonth.from(it.date) == month }
    .groupBy { it.date }
    .mapValues { (_, list) -> list.sumOf { it.amount } }

/**
 * Days in a row without any expense, counting back from [today]. Today counts while nothing
 * has been spent yet, so an expense today brings it back to 0.
 */
fun noSpendStreak(transactions: List<Transaction>, today: LocalDate): Int {
    val first = transactions.minOfOrNull { it.date } ?: return 0
    val spendingDays = transactions.filter { it.type == TransactionType.EXPENSE }.map { it.date }.toSet()
    var day = today
    var streak = 0
    while (!day.isBefore(first) && day !in spendingDays) {
        streak++
        day = day.minusDays(1)
    }
    return streak
}

/** Days of [month] up to [today] on which nothing was spent. */
fun noSpendDays(transactions: List<Transaction>, month: YearMonth, today: LocalDate): Int {
    val first = transactions.minOfOrNull { it.date } ?: return 0
    val spendingDays = dailySpending(transactions, month).keys
    val from = maxOf(month.atDay(1), first)
    val to = minOf(month.atEndOfMonth(), today)
    return generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.count { it !in spendingDays }
}

/** The Monday of the week [date] is in. */
fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/** How many earlier weeks a week is compared with. */
const val COMPARED_WEEKS = 4

/** One week, Monday to Sunday, and how its spending compares with the weeks before it. */
data class WeekSummary(
    val start: LocalDate,
    val spent: Double,
    /** The category with the most spending and how much, or null without any spending. */
    val topCategory: Pair<String, Double>?,
    /** Average spending of up to [COMPARED_WEEKS] earlier weeks, or null with none recorded. */
    val usual: Double?,
) {
    /** Relative change against [usual], e.g. 0.2 for 20 % more; null when there is nothing to compare with. */
    val change: Double? get() = usual?.takeIf { it > 0 }?.let { (spent - it) / it }
}

fun weekSummary(transactions: List<Transaction>, start: LocalDate): WeekSummary {
    val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
    fun spentIn(from: LocalDate) = expenses.filter { !it.date.isBefore(from) && it.date.isBefore(from.plusWeeks(1)) }
    val week = spentIn(start)
    val top = week.groupBy { it.category.trim() }
        .mapValues { (_, list) -> list.sumOf { it.amount } }
        .maxByOrNull { it.value }
        ?.toPair()
    // Only weeks the data fully covers: a half-recorded first week would look cheap.
    val first = transactions.minOfOrNull { it.date }
    val earlier = (1..COMPARED_WEEKS).map { start.minusWeeks(it.toLong()) }.filter { first != null && !first.isAfter(it) }
    return WeekSummary(
        start = start,
        spent = week.sumOf { it.amount },
        topCategory = top,
        usual = earlier.takeIf { it.isNotEmpty() }?.map { from -> spentIn(from).sumOf { it.amount } }?.average(),
    )
}
