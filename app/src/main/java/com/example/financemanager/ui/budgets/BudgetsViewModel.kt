package com.example.financemanager.ui.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financemanager.data.Category
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.data.TransactionType
import com.example.financemanager.ui.components.CategoryNameError
import com.example.financemanager.ui.components.categoryNameError
import com.example.financemanager.ui.parseAmount
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class BudgetsUiState(
    val month: YearMonth = YearMonth.now(),
    /** Categories with a budget, the most used-up first. */
    val budgeted: List<BudgetItem> = emptyList(),
    /** Expense categories without a budget, by name. */
    val unbudgeted: List<BudgetItem> = emptyList(),
    val summary: BudgetSummary? = null,
    val isLoading: Boolean = true,
)

class BudgetsViewModel(
    private val repository: FinanceRepository,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    val state: StateFlow<BudgetsUiState> =
        combine(repository.categories(TransactionType.EXPENSE), repository.transactions()) { categories, transactions ->
            val month = YearMonth.from(today())
            val items = budgetItems(categories, transactions, month)
            BudgetsUiState(
                month = month,
                budgeted = items.filter { it.budget != null }.sortedByDescending { it.fraction },
                unbudgeted = items.filter { it.budget == null },
                summary = budgetSummary(items),
                isLoading = false,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetsUiState(month = YearMonth.from(today())))

    /** Whether [text] can be saved as a budget: a positive amount, or blank to remove it. */
    fun isValidBudget(text: String): Boolean = text.isBlank() || parseAmount(text) != null

    /** Sets the budget from what was typed; blank removes it. */
    fun setBudget(category: Category, text: String) {
        if (!isValidBudget(text)) return
        viewModelScope.launch { repository.setBudget(category, parseAmount(text)) }
    }

    fun validateCategoryName(name: String): CategoryNameError? =
        categoryNameError(name, (state.value.budgeted + state.value.unbudgeted).map { it.category })

    fun addCategory(name: String) {
        val trimmed = name.trim()
        if (validateCategoryName(trimmed) != null) return
        viewModelScope.launch { repository.addCategory(trimmed, TransactionType.EXPENSE) }
    }
}
