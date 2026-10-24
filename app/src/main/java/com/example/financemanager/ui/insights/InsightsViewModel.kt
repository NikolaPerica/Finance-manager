package com.example.financemanager.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.data.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class InsightsUiState(
    val insights: List<Insight> = emptyList(),
    /** Whether there is a full earlier month to compare with; without it most insights can't exist. */
    val hasHistory: Boolean = false,
    val isLoading: Boolean = true,
)

class InsightsViewModel(repository: FinanceRepository, today: () -> LocalDate = LocalDate::now) : ViewModel() {
    val state: StateFlow<InsightsUiState> =
        combine(repository.transactions(), repository.categories(TransactionType.EXPENSE)) { transactions, categories ->
            InsightsUiState(
                insights = buildInsights(transactions, categories, today()),
                hasHistory = historyMonths(transactions, today()).isNotEmpty(),
                isLoading = false,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState())
}
