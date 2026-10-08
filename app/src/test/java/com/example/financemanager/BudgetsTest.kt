package com.example.financemanager

import com.example.financemanager.data.Category
import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import com.example.financemanager.ui.budgets.BudgetAlert
import com.example.financemanager.ui.budgets.BudgetLevel
import com.example.financemanager.ui.budgets.BudgetsViewModel
import com.example.financemanager.ui.budgets.budgetAlert
import com.example.financemanager.ui.budgets.budgetLevel
import com.example.financemanager.ui.dashboard.summarize
import com.example.financemanager.ui.transaction.FormEvent
import com.example.financemanager.ui.transaction.TransactionFormViewModel
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetsTest {

    private val today = LocalDate.of(2026, 10, 8)
    private val food = Category(1, "Hrana", TransactionType.EXPENSE, monthlyBudget = 300.0)
    private val rent = Category(2, "Stanarina", TransactionType.EXPENSE)
    private val entertainment = Category(3, "Zabava", TransactionType.EXPENSE, monthlyBudget = 50.0)
    private val repository = FakeRepository()

    private fun expense(id: Long, amount: Double, category: String, date: LocalDate = today) =
        Transaction(id, amount, date, "", category, TransactionType.EXPENSE)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository.categories.value = listOf(food, rent, entertainment)
        repository.transactions.value = listOf(
            expense(1, 200.0, "Hrana"),
            expense(2, 500.0, "Stanarina"),
            expense(3, 60.0, "Zabava"),
            // Last month and income don't count.
            expense(4, 999.0, "Hrana", today.minusMonths(1)),
            Transaction(5, 1500.0, today, "", "Hrana", TransactionType.INCOME),
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun levels() {
        assertEquals(BudgetLevel.OK, budgetLevel(239.0, 300.0))
        assertEquals(BudgetLevel.WARNING, budgetLevel(240.0, 300.0))
        assertEquals(BudgetLevel.WARNING, budgetLevel(300.0, 300.0))
        assertEquals(BudgetLevel.EXCEEDED, budgetLevel(300.01, 300.0))
    }

    @Test
    fun alertsOnlyWhenALevelIsCrossed() {
        assertNull(budgetAlert("Hrana", 300.0, 100.0, 200.0))
        assertEquals(BudgetAlert("Hrana", BudgetLevel.WARNING, 250.0, 300.0), budgetAlert("Hrana", 300.0, 200.0, 250.0))
        assertEquals(BudgetLevel.EXCEEDED, budgetAlert("Hrana", 300.0, 200.0, 350.0)!!.level)
        assertNull(budgetAlert("Hrana", 300.0, 250.0, 280.0))
        assertNull(budgetAlert("Hrana", 300.0, 350.0, 400.0))
    }

    @Test
    fun budgetsScreenSplitsAndSorts() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = BudgetsViewModel(repository) { today }
        backgroundScope.launch { viewModel.state.collect {} }
        val state = viewModel.state.value

        // Zabava is 120 % used, Hrana 67 %.
        assertEquals(listOf("Zabava", "Hrana"), state.budgeted.map { it.category.name })
        assertEquals(listOf("Stanarina"), state.unbudgeted.map { it.category.name })
        assertEquals(260.0, state.summary!!.spent, 0.0)
        assertEquals(350.0, state.summary.budget, 0.0)
        assertEquals(1, state.summary.exceeded)

        assertFalse(viewModel.isValidBudget("0"))
        viewModel.setBudget(rent, "550")
        viewModel.setBudget(food, "")
        assertEquals(550.0, repository.categories.value.first { it.id == rent.id }.monthlyBudget!!, 0.0)
        assertNull(repository.categories.value.first { it.id == food.id }.monthlyBudget)
    }

    @Test
    fun dashboardShowsTheBudgetSummary() {
        val state = summarize(repository.transactions.value, categories = repository.categories.value, today = today)
        assertEquals(260.0, state.budget!!.spent, 0.0)
        assertNull(summarize(repository.transactions.value, categories = listOf(rent), today = today).budget)
    }

    @Test
    fun savingAnExpenseWarnsWhenItCrossesTheBudget() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = TransactionFormViewModel(TransactionType.EXPENSE, repository) { today }
        viewModel.onAmountChange("50")
        viewModel.onCategoryChange("Hrana")
        viewModel.save()
        assertEquals(FormEvent.Saved(BudgetAlert("Hrana", BudgetLevel.WARNING, 250.0, 300.0)), viewModel.events.first())
    }

    @Test
    fun editingReplacesTheTransactionAndCountsItOnce() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = TransactionFormViewModel(TransactionType.EXPENSE, repository, transactionId = 1) { today }
        assertTrue(viewModel.isEditing)
        assertEquals("200", viewModel.state.value.amount)
        assertEquals("Hrana", viewModel.state.value.category)

        // 200 -> 220 stays under 80 %, because the old amount isn't counted twice.
        viewModel.onAmountChange("220,5")
        viewModel.save()
        assertEquals(FormEvent.Saved(null), viewModel.events.first())
        val edited = repository.transactions.value.single { it.id == 1L }
        assertEquals(220.5, edited.amount, 0.0)
        assertEquals(5, repository.transactions.value.size)
    }

    @Test
    fun deletingFromTheForm() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = TransactionFormViewModel(TransactionType.EXPENSE, repository, transactionId = 2) { today }
        viewModel.delete()
        assertEquals(FormEvent.Deleted, viewModel.events.first())
        assertTrue(repository.transactions.value.none { it.id == 2L })
    }
}
