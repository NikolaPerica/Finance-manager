package com.example.financemanager

import com.example.financemanager.domain.stats.StatsPeriod
import com.example.financemanager.domain.stats.StatsRange
import com.example.financemanager.domain.transactions.PeriodFilter
import com.example.financemanager.domain.transactions.TransactionFilter
import com.example.financemanager.domain.transactions.applyFilter
import com.example.financemanager.domain.transactions.groupByDay
import com.example.financemanager.model.Category
import com.example.financemanager.model.Transaction
import com.example.financemanager.model.TransactionType
import com.example.financemanager.model.TransactionType.EXPENSE
import com.example.financemanager.model.TransactionType.INCOME
import com.example.financemanager.ui.categories.CategoriesViewModel
import com.example.financemanager.ui.components.CategoryNameError
import com.example.financemanager.ui.navigation.TransactionsRoute
import com.example.financemanager.ui.transactions.TransactionsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsAndCategoriesTest {

    private val today = LocalDate.of(2026, 10, 8)
    private val transactions = listOf(
        Transaction(1, 54.2, today, "Konzum", "Hrana", EXPENSE),
        Transaction(2, 85.0, today.minusDays(1), "struja za rujan", "Režije", EXPENSE),
        Transaction(3, 1850.0, today.minusDays(1), "", "Plaća", INCOME),
        Transaction(4, 1234.5, today.minusMonths(1), "Đački dom", "Stanarina", EXPENSE),
        Transaction(5, 30.0, today.minusYears(1), "", "Hrana", EXPENSE),
    )
    private val repository = FakeRepository()

    private fun ids(filter: TransactionFilter) = applyFilter(transactions, filter, today).map { it.id }.sorted()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository.transactions.value = transactions
        repository.categories.value = listOf(
            Category(1, "Hrana", EXPENSE),
            Category(2, "Režije", EXPENSE, monthlyBudget = 100.0),
            Category(3, "Stanarina", EXPENSE),
            Category(4, "Plaća", INCOME),
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun searchIgnoresCaseAndDiacritics() {
        assertEquals(listOf(2L), ids(TransactionFilter(query = "rezije")))
        assertEquals(listOf(2L), ids(TransactionFilter(query = "STRUJA")))
        assertEquals(listOf(4L), ids(TransactionFilter(query = "dacki")))
        assertEquals(listOf(1L, 5L), ids(TransactionFilter(query = "hrana")))
    }

    @Test
    fun searchFindsAmounts() {
        assertEquals(listOf(1L), ids(TransactionFilter(query = "54,2")))
        assertEquals(listOf(4L), ids(TransactionFilter(query = "1.234")))
        assertEquals(listOf(3L), ids(TransactionFilter(query = "1850")))
    }

    @Test
    fun filtersCombine() {
        assertEquals(listOf(3L), ids(TransactionFilter(type = INCOME)))
        assertEquals(listOf(1L, 2L, 3L), ids(TransactionFilter(period = PeriodFilter.ThisMonth)))
        assertEquals(listOf(4L), ids(TransactionFilter(period = PeriodFilter.LastMonth)))
        assertEquals(listOf(1L, 2L, 3L, 4L), ids(TransactionFilter(period = PeriodFilter.ThisYear)))
        assertEquals(listOf(1L, 5L), ids(TransactionFilter(category = "Hrana")))
        assertEquals(listOf(1L), ids(TransactionFilter(category = "Hrana", period = PeriodFilter.ThisMonth)))
        // A range picked backwards still works, both ends included.
        assertEquals(listOf(2L, 3L), ids(TransactionFilter(period = PeriodFilter.Range(today.minusDays(1), today.minusDays(5)))))
    }

    @Test
    fun groupsByDayNewestFirst() {
        val groups = groupByDay(transactions)
        assertEquals(today, groups.first().date)
        assertEquals(listOf(3L, 2L), groups[1].transactions.map { it.id })
        assertEquals(4, groups.size)
    }

    @Test
    fun switchingTypeDropsACategoryOfTheOtherType() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = TransactionsViewModel(repository) { today }
        backgroundScope.launch { viewModel.state.collect {} }

        viewModel.onCategoryChange("Hrana")
        assertEquals(2, viewModel.state.value.count)
        viewModel.onTypeChange(EXPENSE)
        assertEquals("Hrana", viewModel.state.value.filter.category)
        viewModel.onTypeChange(INCOME)
        assertNull(viewModel.state.value.filter.category)
        assertEquals(1850.0, viewModel.state.value.income, 0.0)

        viewModel.clearFilters()
        assertEquals(5, viewModel.state.value.count)
    }

    @Test
    fun categoriesScreenRenamesAndMerges() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = CategoriesViewModel(repository)
        backgroundScope.launch { viewModel.state.collect {} }

        val food = viewModel.state.value.items.first { it.category.name == "Hrana" }
        assertEquals(2, food.count)
        assertEquals(84.2, food.total, 1e-9)

        // Keeping its own name (in another case) is fine, taking another category's is not.
        assertNull(viewModel.validateName("hrana", except = food.category))
        assertEquals(CategoryNameError.EXISTS, viewModel.validateName("Režije", except = food.category))

        viewModel.update(food.category, " Namirnice ", color = 2)
        assertTrue(repository.transactions.value.filter { it.id in listOf(1L, 5L) }.all { it.category == "Namirnice" })

        val renamed = repository.categories.value.first { it.id == food.category.id }
        val bills = repository.categories.value.first { it.name == "Režije" }
        viewModel.delete(renamed, moveTo = bills)
        assertEquals(listOf("Režije", "Stanarina"), viewModel.state.value.items.map { it.category.name }.sorted())
        assertEquals(3, viewModel.state.value.items.first { it.category.name == "Režije" }.count)

        viewModel.onTypeChange(INCOME)
        assertEquals(listOf("Plaća"), viewModel.state.value.items.map { it.category.name })
    }

    @Test
    fun statisticsOpenTheListFilteredToACategoryAndPeriod() = runTest(UnconfinedTestDispatcher()) {
        val october = StatsPeriod(StatsRange.MONTH, YearMonth.of(2026, 10))
        assertEquals(LocalDate.of(2026, 10, 1)..LocalDate.of(2026, 10, 31), october.dates)
        assertEquals(LocalDate.of(2026, 1, 1)..LocalDate.of(2026, 12, 31), october.copy(range = StatsRange.YEAR).dates)
        assertNull(october.copy(range = StatsRange.ALL).dates)

        // What the statistics screen puts in the route comes back as the same filter.
        val dates = october.dates!!
        val route = TransactionsRoute("Hrana", EXPENSE.name, dates.start.toString(), dates.endInclusive.toString())
        assertEquals(
            TransactionFilter(type = EXPENSE, category = "Hrana", period = PeriodFilter.Range(dates.start, dates.endInclusive)),
            route.filter(),
        )
        assertEquals(TransactionFilter(), TransactionsRoute().filter())

        val viewModel = TransactionsViewModel(repository, route.filter()) { today }
        backgroundScope.launch { viewModel.state.collect {} }
        assertEquals(listOf(1L), viewModel.state.value.groups.flatMap { it.transactions }.map { it.id })
        assertEquals(54.2, viewModel.state.value.expense, 1e-9)
    }
}
