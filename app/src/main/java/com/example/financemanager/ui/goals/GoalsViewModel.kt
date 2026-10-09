package com.example.financemanager.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financemanager.data.FinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class GoalsUiState(
    /** Unfinished goals first (nearest deadline first), reached ones last. */
    val goals: List<GoalProgress> = emptyList(),
    val summary: GoalsSummary? = null,
    val isLoading: Boolean = true,
)

class GoalsViewModel(repository: FinanceRepository, today: () -> LocalDate = LocalDate::now) : ViewModel() {
    val state: StateFlow<GoalsUiState> = repository.goals().map { goals ->
        val progress = goals.map { it.progress(today()) }.sortedBy { it.status == GoalStatus.REACHED }
        GoalsUiState(progress, goalsSummary(progress), isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalsUiState())
}
