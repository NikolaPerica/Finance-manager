package com.example.financemanager.ui.stats

import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import java.time.LocalDate
import java.time.YearMonth

enum class StatsRange { MONTH, YEAR, ALL }

/** The period being looked at: one month, the year that [month] is in, or all time. */
data class StatsPeriod(val range: StatsRange, val month: YearMonth) {

    operator fun contains(date: LocalDate): Boolean = when (range) {
        StatsRange.MONTH -> YearMonth.from(date) == month
        StatsRange.YEAR -> date.year == month.year
        StatsRange.ALL -> true
    }

    /** The period [steps] months or years away, or null for all time. */
    fun shift(steps: Long): StatsPeriod? = when (range) {
        StatsRange.MONTH -> copy(month = month.plusMonths(steps))
        StatsRange.YEAR -> copy(month = month.plusYears(steps))
        StatsRange.ALL -> null
    }

    /** Whether the period starts after [today], so there is nothing to show yet. */
    fun isFuture(today: LocalDate): Boolean = when (range) {
        StatsRange.MONTH -> month > YearMonth.from(today)
        StatsRange.YEAR -> month.year > today.year
        StatsRange.ALL -> false
    }

    companion object {
        fun thisMonth(today: LocalDate = LocalDate.now()) = StatsPeriod(StatsRange.MONTH, YearMonth.from(today))
    }
}

/** Everything spent or earned in one category. A blank [name] means no category. */
data class CategoryStat(val name: String, val amount: Double, val count: Int)

/** One column of the chart. [selected] marks the period currently shown above it. */
data class TrendBar(val period: StatsPeriod, val amount: Double, val selected: Boolean)

data class StatsUiState(
    val type: TransactionType = TransactionType.EXPENSE,
    val period: StatsPeriod = StatsPeriod.thisMonth(),
    val total: Double = 0.0,
    val count: Int = 0,
    /** Total of the previous month or year, null for all time. */
    val previousTotal: Double? = null,
    /** Largest first. */
    val categories: List<CategoryStat> = emptyList(),
    val trend: List<TrendBar> = emptyList(),
    val canGoForward: Boolean = false,
    val isLoading: Boolean = true,
) {
    /** Relative change against the previous period, or null when there is nothing to compare with. */
    val change: Double?
        get() = previousTotal?.takeIf { it > 0.0 }?.let { (total - it) / it }
}

private const val MONTHS_IN_TREND = 6
private const val YEARS_IN_TREND = 6

fun buildStats(
    transactions: List<Transaction>,
    type: TransactionType,
    period: StatsPeriod,
    today: LocalDate = LocalDate.now(),
): StatsUiState {
    val ofType = transactions.filter { it.type == type }
    val inPeriod = ofType.filter { it.date in period }
    fun total(p: StatsPeriod) = ofType.filter { it.date in p }.sumOf { it.amount }

    val categories = inPeriod
        .groupBy { it.category.trim() }
        .map { (name, list) -> CategoryStat(name, list.sumOf { it.amount }, list.size) }
        .sortedWith(compareByDescending<CategoryStat> { it.amount }.thenBy { it.name.lowercase() })

    val trendPeriods = when (period.range) {
        StatsRange.MONTH -> (MONTHS_IN_TREND - 1 downTo 0).map { period.copy(month = period.month.minusMonths(it.toLong())) }
        StatsRange.YEAR -> (1..12).map { StatsPeriod(StatsRange.MONTH, YearMonth.of(period.month.year, it)) }
        StatsRange.ALL -> {
            val lastYear = maxOf(today.year, ofType.maxOfOrNull { it.date.year } ?: today.year)
            val firstYear = (ofType.minOfOrNull { it.date.year } ?: lastYear).coerceAtLeast(lastYear - YEARS_IN_TREND + 1)
            (firstYear..lastYear).map { StatsPeriod(StatsRange.YEAR, YearMonth.of(it, 1)) }
        }
    }

    return StatsUiState(
        type = type,
        period = period,
        total = inPeriod.sumOf { it.amount },
        count = inPeriod.size,
        previousTotal = period.shift(-1)?.let(::total),
        categories = categories,
        trend = trendPeriods.map { TrendBar(it, total(it), selected = it == period) },
        canGoForward = period.shift(1)?.isFuture(today) == false,
        isLoading = false,
    )
}
