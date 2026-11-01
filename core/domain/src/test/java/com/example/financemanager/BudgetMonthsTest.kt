package com.example.financemanager

import com.example.financemanager.domain.BudgetMonths
import com.example.financemanager.domain.budgets.spentByCategory
import com.example.financemanager.domain.insights.Insight
import com.example.financemanager.domain.insights.buildInsights
import com.example.financemanager.domain.insights.historyMonths
import com.example.financemanager.domain.stats.StatsPeriod
import com.example.financemanager.domain.stats.StatsRange
import com.example.financemanager.domain.stats.buildStats
import com.example.financemanager.domain.transactions.PeriodFilter
import com.example.financemanager.domain.widget.widgetSummary
import com.example.financemanager.model.Transaction
import com.example.financemanager.model.TransactionType.EXPENSE
import com.example.financemanager.model.TransactionType.INCOME
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class BudgetMonthsTest {

    private val payday = BudgetMonths(15)
    private var id = 1L

    private fun t(amount: Double, date: LocalDate, type: com.example.financemanager.model.TransactionType = EXPENSE, category: String = "Hrana") =
        Transaction(id++, amount, date, "", category, type)

    @Test
    fun theFirstGivesCalendarMonths() {
        val calendar = BudgetMonths()
        assertTrue(calendar.isCalendar)
        assertEquals(YearMonth.of(2026, 10), calendar.of(LocalDate.of(2026, 10, 1)))
        assertEquals(LocalDate.of(2026, 10, 1)..LocalDate.of(2026, 10, 31), calendar.dates(YearMonth.of(2026, 10)))
        assertEquals(24, calendar.dayOf(LocalDate.of(2026, 10, 24)))
    }

    @Test
    fun aMonthRunsFromItsStartDayToTheDayBeforeTheNext() {
        val october = YearMonth.of(2026, 10)
        assertEquals(LocalDate.of(2026, 10, 15)..LocalDate.of(2026, 11, 14), payday.dates(october))
        assertEquals(31, payday.length(october))
        // February's month is short, and the year turns over inside December's.
        assertEquals(28, payday.length(YearMonth.of(2026, 2)))
        assertEquals(LocalDate.of(2027, 1, 14), payday.last(YearMonth.of(2026, 12)))
    }

    @Test
    fun aDateBelongsToTheMonthItsStartDayLastPassed() {
        assertEquals(YearMonth.of(2026, 9), payday.of(LocalDate.of(2026, 10, 14)))
        assertEquals(YearMonth.of(2026, 10), payday.of(LocalDate.of(2026, 10, 15)))
        assertEquals(YearMonth.of(2026, 12), payday.of(LocalDate.of(2027, 1, 3)))
        assertEquals(1, payday.dayOf(LocalDate.of(2026, 10, 15)))
        assertEquals(31, payday.dayOf(LocalDate.of(2026, 11, 14)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun aMonthCannotStartOnADayNotEveryMonthHas() {
        BudgetMonths(29)
    }

    @Test
    fun statisticsBudgetsFiltersAndTheWidgetUseTheSameMonths() {
        val today = LocalDate.of(2026, 11, 3)
        val transactions = listOf(
            t(2000.0, LocalDate.of(2026, 10, 15), INCOME, "Plaća"),
            t(40.0, LocalDate.of(2026, 10, 20)),
            t(60.0, LocalDate.of(2026, 11, 2)),
            // Before payday: still September's month.
            t(500.0, LocalDate.of(2026, 10, 10)),
        )

        val period = StatsPeriod.thisMonth(today, payday)
        assertEquals(YearMonth.of(2026, 10), period.month)
        assertEquals(100.0, buildStats(transactions, EXPENSE, period, today).total, 0.001)
        assertFalse(LocalDate.of(2026, 11, 15) in period)

        assertEquals(mapOf("Hrana" to 100.0), spentByCategory(transactions, YearMonth.of(2026, 10), payday))
        assertEquals(LocalDate.of(2026, 10, 15)..LocalDate.of(2026, 11, 14), PeriodFilter.ThisMonth.dates(today, payday))
        assertEquals(LocalDate.of(2026, 9, 15)..LocalDate.of(2026, 10, 14), PeriodFilter.LastMonth.dates(today, payday))

        val widget = widgetSummary(transactions, today, payday)
        assertEquals(2000.0, widget.monthIncome, 0.001)
        assertEquals(100.0, widget.monthExpense, 0.001)
    }

    @Test
    fun aYearOfPaydayMonthsStartsOnTheFirstPayday() {
        val year = StatsPeriod(StatsRange.YEAR, YearMonth.of(2026, 1), payday)
        assertEquals(LocalDate.of(2026, 1, 15)..LocalDate.of(2027, 1, 14), year.dates)
        assertTrue(LocalDate.of(2027, 1, 10) in year)
        assertFalse(LocalDate.of(2026, 1, 10) in year)
    }

    @Test
    fun insightsCompareTheSameDaysSincePayday() {
        // Three earlier payday months: salary on payday, food on its 5th day.
        val history = listOf(7, 8, 9).flatMap { m ->
            val start = payday.first(YearMonth.of(2026, m))
            listOf(t(2000.0, start, INCOME, "Plaća"), t(100.0, start.plusDays(4)))
        }
        // This month: food on its 5th day is three times as much.
        val october = payday.first(YearMonth.of(2026, 10))
        val today = october.plusDays(6)
        val transactions = history + t(2000.0, october, INCOME, "Plaća") + t(300.0, october.plusDays(4))

        assertEquals(listOf(9, 8, 7).map { YearMonth.of(2026, it) }, historyMonths(transactions, today, payday))
        val trend = buildInsights(transactions, emptyList(), today, payday).filterIsInstance<Insight.CategoryTrend>().single()
        assertEquals("Hrana", trend.category)
        assertEquals(300.0, trend.current, 0.001)
        assertEquals(100.0, trend.usual, 0.001)
    }
}
