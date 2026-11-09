package com.example.financemanager

import com.example.financemanager.domain.budgets.BudgetAlert
import com.example.financemanager.domain.budgets.BudgetLevel
import com.example.financemanager.model.Category
import com.example.financemanager.model.Template
import com.example.financemanager.model.Transaction
import com.example.financemanager.model.TransactionType
import com.example.financemanager.ui.dashboard.DashboardEvent
import com.example.financemanager.ui.dashboard.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class QuickAddTest {
    private val today = LocalDate.of(2026, 11, 6)
    private val repository = FakeRepository().apply {
        categories.value = listOf(Category(1, "Hrana", TransactionType.EXPENSE, monthlyBudget = 10.0))
        transactions.value = listOf(Transaction(1, 7.0, today, "", "Hrana", TransactionType.EXPENSE))
        templates.value = listOf(
            Template(2, "Kava", 2.0, TransactionType.EXPENSE, "Hrana", "Kava"),
            Template(3, "Gorivo", 50.0, TransactionType.EXPENSE, "Gorivo", "", uses = 4),
        )
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun aTemplateAddsTodaysTransactionAndCanBeUndone() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = DashboardViewModel(repository) { today }
        backgroundScope.launch { viewModel.state.collect {} }
        // The most used first.
        assertEquals(listOf("Gorivo", "Kava"), viewModel.state.value.templates.map { it.name })

        viewModel.useTemplate(repository.templates.value.first { it.name == "Kava" })
        val event = viewModel.events.first() as DashboardEvent.Added
        assertEquals(Transaction(event.transaction.id, 2.0, today, "Kava", "Hrana", TransactionType.EXPENSE), event.transaction)
        // 7 € + 2 € of a 10 € budget is past 80 %.
        assertEquals(BudgetAlert("Hrana", BudgetLevel.WARNING, 9.0, 10.0), event.budgetAlert)
        assertEquals(2, viewModel.state.value.transactionCount)
        assertEquals(listOf("Gorivo" to 4, "Kava" to 1), viewModel.state.value.templates.map { it.name to it.uses })

        viewModel.delete(event.transaction)
        assertEquals(1, viewModel.state.value.transactionCount)
    }

    @Test
    fun removingATemplateCanBeUndone() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = DashboardViewModel(repository) { today }
        backgroundScope.launch { viewModel.state.collect {} }
        val fuel = viewModel.state.value.templates.first { it.name == "Gorivo" }

        viewModel.deleteTemplate(fuel)
        assertTrue(viewModel.state.value.templates.none { it.name == "Gorivo" })
        viewModel.restoreTemplate(fuel)
        assertEquals(fuel, viewModel.state.value.templates.first { it.name == "Gorivo" })
    }
}
