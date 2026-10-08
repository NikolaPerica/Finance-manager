package com.example.financemanager.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financemanager.data.Category
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.data.Reminder
import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import com.example.financemanager.ui.budgets.BudgetSummary
import com.example.financemanager.ui.budgets.budgetItems
import com.example.financemanager.ui.budgets.budgetSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class DashboardUiState(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    /** Newest first. */
    val transactions: List<Transaction> = emptyList(),
    /** The next few payment reminders, soonest first. */
    val upcoming: List<Reminder> = emptyList(),
    /** This month's totals over categories with a budget, null when none has one. */
    val budget: BudgetSummary? = null,
    val isLoading: Boolean = true,
) {
    val balance: Double get() = income - expense
}

private const val UPCOMING_COUNT = 3

fun summarize(
    transactions: List<Transaction>,
    reminders: List<Reminder> = emptyList(),
    categories: List<Category> = emptyList(),
    today: LocalDate = LocalDate.now(),
) = DashboardUiState(
    income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
    expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
    transactions = transactions.sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.id }),
    upcoming = reminders.sortedBy { it.nextDueDate }.take(UPCOMING_COUNT),
    budget = budgetSummary(budgetItems(categories, transactions, YearMonth.from(today))),
    isLoading = false,
)

class DashboardViewModel(private val repository: FinanceRepository) : ViewModel() {

    val state: StateFlow<DashboardUiState> = combine(
        repository.transactions(),
        repository.reminders(),
        repository.categories(TransactionType.EXPENSE),
    ) { transactions, reminders, categories -> summarize(transactions, reminders, categories) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun delete(transaction: Transaction) {
        viewModelScope.launch { repository.deleteTransaction(transaction) }
    }

    /** Puts a just-deleted transaction back, keeping its id and position. */
    fun restore(transaction: Transaction) {
        viewModelScope.launch { repository.addTransaction(transaction) }
    }
}
