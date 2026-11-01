package com.example.financemanager.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.data.SettingsRepository
import com.example.financemanager.domain.BudgetMonths
import com.example.financemanager.domain.insights.Insight
import com.example.financemanager.domain.insights.buildInsights
import com.example.financemanager.domain.insights.historyMonths
import com.example.financemanager.model.TransactionType
import com.example.financemanager.ui.budgetMonths
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class InsightsUiState(
    val insights: List<Insight> = emptyList(),
    /** Whether there is a full earlier month to compare with; without it most insights can't exist. */
    val hasHistory: Boolean = false,
    /** Today's day of the month, counted from the day months start on. */
    val day: Int = 1,
    val isLoading: Boolean = true,
)

@HiltViewModel
class InsightsViewModel(
    repository: FinanceRepository,
    months: Flow<BudgetMonths> = flowOf(BudgetMonths()),
    today: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    // Hilt uses this one; tests pass their own [today].
    @Inject
    constructor(repository: FinanceRepository, settings: SettingsRepository) :
        this(repository, settings.budgetMonths, LocalDate::now)

    val state: StateFlow<InsightsUiState> = combine(
        repository.transactions(),
        repository.categories(TransactionType.EXPENSE),
        months,
    ) { transactions, categories, months ->
        InsightsUiState(
            insights = buildInsights(transactions, categories, today(), months),
            hasHistory = historyMonths(transactions, today(), months).isNotEmpty(),
            day = months.dayOf(today()),
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState())
}
