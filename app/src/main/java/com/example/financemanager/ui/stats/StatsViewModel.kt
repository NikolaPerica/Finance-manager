package com.example.financemanager.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.data.SettingsRepository
import com.example.financemanager.domain.BudgetMonths
import com.example.financemanager.domain.stats.StatsPeriod
import com.example.financemanager.domain.stats.StatsRange
import com.example.financemanager.domain.stats.StatsUiState
import com.example.financemanager.domain.stats.buildStats
import com.example.financemanager.model.TransactionType
import com.example.financemanager.ui.budgetMonths
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class StatsViewModel(
    repository: FinanceRepository,
    months: Flow<BudgetMonths> = flowOf(BudgetMonths()),
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    // Hilt uses this one; tests pass their own [today].
    @Inject
    constructor(repository: FinanceRepository, settings: SettingsRepository) :
        this(repository, settings.budgetMonths, LocalDate::now)

    private val type = MutableStateFlow(TransactionType.EXPENSE)

    /** Null until the user picks a period: then the current month is shown. */
    private val period = MutableStateFlow<StatsPeriod?>(null)

    val state: StateFlow<StatsUiState> = combine(repository.transactions(), type, period, months) { transactions, type, period, months ->
        // The months setting decides where every period starts, also for one picked earlier.
        val shown = period?.copy(months = months) ?: StatsPeriod.thisMonth(today(), months)
        buildStats(transactions, type, shown, today())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState(period = StatsPeriod.thisMonth(today())))

    fun onTypeChange(newType: TransactionType) {
        type.value = newType
    }

    fun onRangeChange(range: StatsRange) {
        period.value = state.value.period.copy(range = range)
    }

    /** Moves one month or year back (-1) or forward (+1), never past today. */
    fun shift(steps: Long) {
        val current = state.value.period
        current.shift(steps)?.takeUnless { it.isFuture(today()) }?.let { period.value = it }
    }

    /** Opens a period picked on the chart. */
    fun select(selected: StatsPeriod) {
        if (!selected.isFuture(today())) period.value = selected
    }
}
