package com.example.financemanager.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    /** Newest first. */
    val transactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = true,
) {
    val balance: Double get() = income - expense
}

fun summarize(transactions: List<Transaction>) = DashboardUiState(
    income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
    expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
    transactions = transactions.sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.id }),
    isLoading = false,
)

class DashboardViewModel(private val repository: FinanceRepository) : ViewModel() {

    val state: StateFlow<DashboardUiState> = repository.transactions()
        .map(::summarize)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun delete(transaction: Transaction) {
        viewModelScope.launch { repository.deleteTransaction(transaction) }
    }

    /** Puts a just-deleted transaction back, keeping its id and position. */
    fun restore(transaction: Transaction) {
        viewModelScope.launch { repository.addTransaction(transaction) }
    }
}
