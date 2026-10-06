package com.example.financemanager.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.data.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate

class StatsViewModel(
    repository: FinanceRepository,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    private val type = MutableStateFlow(TransactionType.EXPENSE)
    private val period = MutableStateFlow(StatsPeriod.thisMonth(today()))

    val state: StateFlow<StatsUiState> = combine(repository.transactions(), type, period) { transactions, type, period ->
        buildStats(transactions, type, period, today())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState(period = period.value))

    fun onTypeChange(newType: TransactionType) {
        type.value = newType
    }

    fun onRangeChange(range: StatsRange) = period.update { it.copy(range = range) }

    /** Moves one month or year back (-1) or forward (+1), never past today. */
    fun shift(steps: Long) = period.update { current ->
        current.shift(steps)?.takeUnless { it.isFuture(today()) } ?: current
    }

    /** Opens a period picked on the chart. */
    fun select(selected: StatsPeriod) {
        if (!selected.isFuture(today())) period.value = selected
    }
}
