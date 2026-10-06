package com.example.financemanager

import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import com.example.financemanager.ui.stats.CategoryStat
import com.example.financemanager.ui.stats.StatsPeriod
import com.example.financemanager.ui.stats.StatsRange
import com.example.financemanager.ui.stats.StatsViewModel
import com.example.financemanager.ui.stats.buildStats
import com.example.financemanager.ui.stats.niceCeiling
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class StatsTest {

    private val today = LocalDate.of(2026, 10, 6)
    private val october = StatsPeriod(StatsRange.MONTH, YearMonth.of(2026, 10))

    private var nextId = 1L
    private fun expense(amount: Double, category: String, date: LocalDate) =
        Transaction(nextId++, amount, date, "", category, TransactionType.EXPENSE)
    private fun income(amount: Double, category: String, date: LocalDate) =
        Transaction(nextId++, amount, date, "", category, TransactionType.INCOME)

    private val transactions = listOf(
        expense(300.0, "Stanarina", LocalDate.of(2026, 10, 1)),
        expense(40.0, "Hrana", LocalDate.of(2026, 10, 2)),
        expense(60.0, "Hrana", LocalDate.of(2026, 10, 5)),
        expense(20.0, "", LocalDate.of(2026, 10, 3)),
        expense(200.0, "Hrana", LocalDate.of(2026, 9, 10)),
        expense(100.0, "Hrana", LocalDate.of(2025, 3, 1)),
        income(1500.0, "Plaća", LocalDate.of(2026, 10, 1)),
    )

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun monthGroupsByCategoryLargestFirst() {
        val stats = buildStats(transactions, TransactionType.EXPENSE, october, today)

        assertEquals(420.0, stats.total, 0.0)
        assertEquals(4, stats.count)
        assertEquals(
            listOf(CategoryStat("Stanarina", 300.0, 1), CategoryStat("Hrana", 100.0, 2), CategoryStat("", 20.0, 1)),
            stats.categories,
        )
        // 420 against 200 in September.
        assertEquals(1.1, stats.change!!, 1e-9)
        assertFalse(stats.canGoForward)
    }

    @Test
    fun monthTrendShowsTheLastSixMonthsWithTheSelectedOneMarked() {
        val trend = buildStats(transactions, TransactionType.EXPENSE, october, today).trend

        assertEquals((5 downTo 0).map { YearMonth.of(2026, 10).minusMonths(it.toLong()) }, trend.map { it.period.month })
        assertEquals(listOf(0.0, 0.0, 0.0, 0.0, 200.0, 420.0), trend.map { it.amount })
        assertEquals(listOf(false, false, false, false, false, true), trend.map { it.selected })
    }

    @Test
    fun yearAndAllTime() {
        val year = buildStats(transactions, TransactionType.EXPENSE, october.copy(range = StatsRange.YEAR), today)
        assertEquals(620.0, year.total, 0.0)
        assertEquals(12, year.trend.size)
        assertTrue(year.trend.all { it.period.range == StatsRange.MONTH && !it.selected })
        // 620 against 100 in 2025.
        assertEquals(5.2, year.change!!, 1e-9)

        val all = buildStats(transactions, TransactionType.EXPENSE, october.copy(range = StatsRange.ALL), today)
        assertEquals(720.0, all.total, 0.0)
        assertNull(all.previousTotal)
        assertEquals(listOf(2025, 2026), all.trend.map { it.period.month.year })
        assertEquals(listOf(100.0, 620.0), all.trend.map { it.amount })
    }

    @Test
    fun incomeIsCountedSeparately() {
        val stats = buildStats(transactions, TransactionType.INCOME, october, today)
        assertEquals(1500.0, stats.total, 0.0)
        assertEquals(listOf(CategoryStat("Plaća", 1500.0, 1)), stats.categories)
        assertNull(stats.change)
    }

    @Test
    fun viewModelNeverMovesIntoTheFuture() = runTest(UnconfinedTestDispatcher()) {
        val repository = FakeRepository().apply { this.transactions.value = this@StatsTest.transactions }
        val viewModel = StatsViewModel(repository) { today }
        backgroundScope.launch { viewModel.state.collect {} }

        viewModel.shift(1)
        assertEquals(october, viewModel.state.value.period)

        viewModel.shift(-1)
        assertEquals(YearMonth.of(2026, 9), viewModel.state.value.period.month)
        assertEquals(200.0, viewModel.state.value.total, 0.0)
        assertTrue(viewModel.state.value.canGoForward)

        viewModel.onRangeChange(StatsRange.YEAR)
        viewModel.select(StatsPeriod(StatsRange.MONTH, YearMonth.of(2026, 12)))
        assertEquals(StatsRange.YEAR, viewModel.state.value.period.range)

        viewModel.select(october)
        viewModel.onTypeChange(TransactionType.INCOME)
        assertEquals(1500.0, viewModel.state.value.total, 0.0)
    }

    @Test
    fun chartScaleIsRounded() {
        assertEquals(1.0, niceCeiling(0.0), 0.0)
        assertEquals(500.0, niceCeiling(420.0), 0.0)
        assertEquals(250.0, niceCeiling(210.0), 0.0)
        assertEquals(2000.0, niceCeiling(1500.0), 0.0)
        assertEquals(100.0, niceCeiling(100.0), 0.0)
    }
}
