package com.example.financemanager.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financemanager.data.Category
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TransactionsUiState(
    val filter: TransactionFilter = TransactionFilter(),
    val groups: List<DayGroup> = emptyList(),
    val count: Int = 0,
    val income: Double = 0.0,
    val expense: Double = 0.0,
    /** Categories that can be picked in the filter, for the selected type. */
    val categories: List<Category> = emptyList(),
    /** Whether there are any transactions at all, filtered or not. */
    val hasAny: Boolean = false,
    val isLoading: Boolean = true,
)

class TransactionsViewModel(
    private val repository: FinanceRepository,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    private val filter = MutableStateFlow(TransactionFilter())

    val state: StateFlow<TransactionsUiState> = combine(
        repository.transactions(),
        repository.categories(TransactionType.EXPENSE),
        repository.categories(TransactionType.INCOME),
        filter,
    ) { transactions, expenseCategories, incomeCategories, filter ->
        val shown = applyFilter(transactions, filter, today())
        TransactionsUiState(
            filter = filter,
            groups = groupByDay(shown),
            count = shown.size,
            income = shown.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
            expense = shown.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
            categories = when (filter.type) {
                TransactionType.EXPENSE -> expenseCategories
                TransactionType.INCOME -> incomeCategories
                null -> (expenseCategories + incomeCategories).sortedBy { it.name.lowercase() }
            },
            hasAny = transactions.isNotEmpty(),
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    fun onQueryChange(query: String) = filter.update { it.copy(query = query) }

    fun onTypeChange(type: TransactionType?) = filter.update { current ->
        // A category of the other type would hide everything.
        val keepCategory = type == null || current.category == null ||
            state.value.categories.any { it.name == current.category && it.type == type }
        current.copy(type = type, category = current.category.takeIf { keepCategory })
    }

    fun onPeriodChange(period: PeriodFilter) = filter.update { it.copy(period = period) }

    fun onCategoryChange(category: String?) = filter.update { it.copy(category = category) }

    fun clearFilters() {
        filter.value = TransactionFilter()
    }

    fun delete(transaction: Transaction) {
        viewModelScope.launch { repository.deleteTransaction(transaction) }
    }

    fun restore(transaction: Transaction) {
        viewModelScope.launch { repository.addTransaction(transaction) }
    }
}
