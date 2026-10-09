package com.example.financemanager.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.data.GoalContribution
import com.example.financemanager.ui.parseAmount
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class GoalDetailUiState(
    /** Null once the goal is gone (deleted), or while loading. */
    val progress: GoalProgress? = null,
    /** Newest first. */
    val contributions: List<GoalContribution> = emptyList(),
    val isLoading: Boolean = true,
)

enum class ContributionError { AMOUNT, MORE_THAN_SAVED }

sealed interface GoalEvent {
    /** A deposit took the goal to its target. */
    data object Reached : GoalEvent
    data class ContributionDeleted(val contribution: GoalContribution) : GoalEvent
    data object GoalDeleted : GoalEvent
}

class GoalDetailViewModel(
    private val goalId: Long,
    private val repository: FinanceRepository,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    val state: StateFlow<GoalDetailUiState> =
        combine(repository.goals(), repository.contributions(goalId)) { goals, contributions ->
            GoalDetailUiState(
                progress = goals.find { it.goal.id == goalId }?.progress(today()),
                contributions = contributions,
                isLoading = false,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalDetailUiState())

    private val _events = Channel<GoalEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** Why [text] can't be deposited ([withdraw] false) or taken out, or null if it can. */
    fun validate(text: String, withdraw: Boolean): ContributionError? {
        val amount = parseAmount(text) ?: return ContributionError.AMOUNT
        val saved = state.value.progress?.saved ?: 0.0
        return if (withdraw && amount > saved + 0.005) ContributionError.MORE_THAN_SAVED else null
    }

    fun contribute(text: String, withdraw: Boolean) {
        if (validate(text, withdraw) != null) return
        val progress = state.value.progress ?: return
        val amount = parseAmount(text)!!
        viewModelScope.launch {
            repository.addContribution(
                GoalContribution(goalId = goalId, amount = if (withdraw) -amount else amount, date = today()),
            )
            if (!withdraw && progress.status != GoalStatus.REACHED && progress.saved + amount >= progress.goal.target) {
                _events.send(GoalEvent.Reached)
            }
        }
    }

    fun deleteContribution(contribution: GoalContribution) {
        viewModelScope.launch {
            repository.deleteContribution(contribution)
            _events.send(GoalEvent.ContributionDeleted(contribution))
        }
    }

    fun restoreContribution(contribution: GoalContribution) {
        viewModelScope.launch { repository.addContribution(contribution) }
    }

    fun deleteGoal() {
        val goal = state.value.progress?.goal ?: return
        viewModelScope.launch {
            repository.deleteGoal(goal)
            _events.send(GoalEvent.GoalDeleted)
        }
    }
}
