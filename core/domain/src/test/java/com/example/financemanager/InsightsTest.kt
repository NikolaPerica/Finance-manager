package com.example.financemanager

import com.example.financemanager.domain.insights.Insight
import com.example.financemanager.domain.insights.buildInsights
import com.example.financemanager.domain.insights.historyMonths
import com.example.financemanager.model.Category
import com.example.financemanager.model.Transaction
import com.example.financemanager.model.TransactionType.EXPENSE
import com.example.financemanager.model.TransactionType.INCOME
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class InsightsTest {

    private val today = LocalDate.of(2026, 10, 24)
    private var id = 1L

    private fun t(amount: Double, category: String, date: LocalDate, type: com.example.financemanager.model.TransactionType = EXPENSE) =
        Transaction(id++, amount, date, "", category, type)

    /** July, August and September each: salary on the 1st, rent on the 2nd, food on the 10th and 25th. */
    private fun usualMonths() = listOf(7, 8, 9).flatMap { m ->
        val month = YearMonth.of(2026, m)
        listOf(
            t(2000.0, "Plaća", month.atDay(1), INCOME),
            t(500.0, "Stanarina", month.atDay(2)),
            t(100.0, "Hrana", month.atDay(10)),
            t(100.0, "Hrana", month.atDay(25)),
            t(30.0, "Prijevoz", month.atDay(12)),
        )
    }

    private inline fun <reified T : Insight> List<Insight>.all() = filterIsInstance<T>()

    @Test
    fun onlyFullyRecordedMonthsCount() {
        val data = usualMonths()
        assertEquals(listOf(YearMonth.of(2026, 9), YearMonth.of(2026, 8), YearMonth.of(2026, 7)), historyMonths(data, today))
        // Data starting mid-September: September isn't fully covered, so there's no history yet.
        assertTrue(historyMonths(listOf(t(1.0, "Hrana", LocalDate.of(2026, 9, 15))), today).isEmpty())
    }

    @Test
    fun forecastAddsWhatUsuallyHappensInTheRestOfTheMonth() {
        val october = YearMonth.of(2026, 10)
        val data = usualMonths() + listOf(
            t(2000.0, "Plaća", october.atDay(1), INCOME),
            t(500.0, "Stanarina", october.atDay(2)),
            t(100.0, "Hrana", october.atDay(10)),
        )
        val forecast = buildInsights(data, emptyList(), today).all<Insight.MonthEndForecast>().single()
        assertEquals(3, forecast.basedOnMonths)
        // After the 24th, earlier months only had the second food shop.
        assertEquals(0.0, forecast.expectedIncome, 0.0)
        assertEquals(100.0, forecast.expectedExpense, 0.0)
        // Balance now: 4 × 2000 − 3 × 730 − 600 = 5210; minus the expected 100.
        assertEquals(5210.0, forecast.balanceNow, 1e-9)
        assertEquals(5110.0, forecast.projectedBalance, 1e-9)
    }

    @Test
    fun withoutHistoryTheForecastFollowsThisMonthsPace() {
        val data = listOf(t(240.0, "Hrana", LocalDate.of(2026, 10, 2)), t(1000.0, "Plaća", LocalDate.of(2026, 10, 1), INCOME))
        val forecast = buildInsights(data, emptyList(), today).all<Insight.MonthEndForecast>().single()
        assertEquals(0, forecast.basedOnMonths)
        // 240 € in 24 days is 10 € a day, for the 7 days left.
        assertEquals(70.0, forecast.expectedExpense, 1e-9)
        // Too early in the month to guess.
        assertTrue(buildInsights(data, emptyList(), LocalDate.of(2026, 10, 3)).all<Insight.MonthEndForecast>().isEmpty())
    }

    @Test
    fun categoriesAreComparedForTheSamePartOfTheMonth() {
        val october = YearMonth.of(2026, 10)
        val data = usualMonths() + listOf(
            t(500.0, "Stanarina", october.atDay(2)),
            // Food so far: 150 against the usual 100 by the 24th.
            t(150.0, "Hrana", october.atDay(10)),
            // Transport: 33 against 30, too small a change.
            t(33.0, "Prijevoz", october.atDay(12)),
        )
        val trends = buildInsights(data, emptyList(), today).all<Insight.CategoryTrend>()
        assertEquals(listOf("Hrana"), trends.map { it.category })
        assertEquals(0.5, trends.single().change, 1e-9)
        assertEquals(100.0, trends.single().usual, 1e-9)
    }

    @Test
    fun spendingLessIsAlsoNoted() {
        val data = usualMonths() + t(500.0, "Stanarina", today.withDayOfMonth(2)) + t(40.0, "Hrana", today.withDayOfMonth(5))
        val food = buildInsights(data, emptyList(), today).all<Insight.CategoryTrend>().single { it.category == "Hrana" }
        assertEquals(-0.6, food.change, 1e-9)
    }

    @Test
    fun budgetRiskWhileStillUnder() {
        val data = usualMonths() + t(150.0, "Hrana", today.withDayOfMonth(10))
        val food = Category(1, "Hrana", EXPENSE, monthlyBudget = 220.0)
        // 150 so far plus the usual 100 after the 24th: 250 > 220.
        val risk = buildInsights(data, listOf(food), today).all<Insight.BudgetRisk>().single()
        assertEquals(250.0, risk.projected, 1e-9)
        // Already over budget: the budget screen says so, no forecast needed.
        assertTrue(buildInsights(data, listOf(food.copy(monthlyBudget = 120.0)), today).all<Insight.BudgetRisk>().isEmpty())
    }

    @Test
    fun unusuallyBigExpense() {
        val big = t(420.0, "Hrana", today.withDayOfMonth(20))
        val unusual = buildInsights(usualMonths() + big, emptyList(), today).all<Insight.UnusualExpense>().single()
        assertEquals(big, unusual.transaction)
        assertEquals(100.0, unusual.usual, 0.0)
        assertTrue(buildInsights(usualMonths() + t(150.0, "Hrana", today), emptyList(), today).all<Insight.UnusualExpense>().isEmpty())
    }

    @Test
    fun lastMonthSummary() {
        val last = buildInsights(usualMonths(), emptyList(), today).all<Insight.LastMonth>().single()
        assertEquals(YearMonth.of(2026, 9), last.month)
        assertEquals(1270.0, last.saved, 1e-9)
        assertEquals(0.635, last.savedShare!!, 1e-9)
        assertNull(Insight.LastMonth(YearMonth.of(2026, 9), 0.0, 10.0).savedShare)
    }

    @Test
    fun nothingWithoutData() {
        assertTrue(buildInsights(emptyList(), emptyList(), today).isEmpty())
    }
}
