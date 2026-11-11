package com.example.financemanager

import com.example.financemanager.model.Transaction
import com.example.financemanager.model.TransactionType
import com.example.financemanager.ui.calendar.CalendarViewModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {
    private val today = LocalDate.of(2026, 11, 11)
    private val repository = FakeRepository().apply {
        transactions.value = listOf(
            Transaction(1, 40.0, LocalDate.of(2026, 10, 30), "", "Hrana", TransactionType.EXPENSE),
            Transaction(2, 12.0, LocalDate.of(2026, 11, 3), "", "Hrana", TransactionType.EXPENSE),
            Transaction(3, 8.0, LocalDate.of(2026, 11, 3), "", "Kava", TransactionType.EXPENSE),
            Transaction(4, 1500.0, LocalDate.of(2026, 11, 5), "", "Plaća", TransactionType.INCOME),
            Transaction(5, 30.0, LocalDate.of(2026, 11, 7), "", "Gorivo", TransactionType.EXPENSE),
        )
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun theMonthDayByDay() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = CalendarViewModel(repository) { today }
        backgroundScope.launch { viewModel.state.collect {} }
        val state = viewModel.state.value
        assertEquals(YearMonth.of(2026, 11), state.month)
        assertEquals(mapOf(LocalDate.of(2026, 11, 3) to 20.0, LocalDate.of(2026, 11, 7) to 30.0), state.days)
        assertEquals(50.0, state.monthTotal, 0.001)
        // 1 to 11 November, less the 3rd and the 7th.
        assertEquals(9, state.noSpendDays)
        // 8th to the 11th.
        assertEquals(4, state.streak)
        assertFalse(state.canGoForward)
    }

    @Test
    fun earlierMonthsButNotTheFuture() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = CalendarViewModel(repository) { today }
        backgroundScope.launch { viewModel.state.collect {} }
        viewModel.shift(1)
        assertEquals(YearMonth.of(2026, 11), viewModel.state.value.month)

        viewModel.shift(-1)
        val october = viewModel.state.value
        assertEquals(YearMonth.of(2026, 10), october.month)
        assertEquals(40.0, october.monthTotal, 0.001)
        // The records start on the 30th, which had spending; the 31st didn't.
        assertEquals(1, october.noSpendDays)
        assertTrue(october.canGoForward)
    }
}
