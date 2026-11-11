package com.example.financemanager.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.domain.habits.dailySpending
import com.example.financemanager.domain.habits.noSpendDays
import com.example.financemanager.domain.habits.noSpendStreak
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val today: LocalDate = LocalDate.now(),
    /** What was spent on each day of [month] that had any spending. */
    val days: Map<LocalDate, Double> = emptyMap(),
    val monthTotal: Double = 0.0,
    /** Days of [month] up to today without spending. */
    val noSpendDays: Int = 0,
    /** Days in a row without spending, up to today; the same whatever month is shown. */
    val streak: Int = 0,
    /** The first day with any record; days before it are neither spending nor saving days. */
    val firstDay: LocalDate? = null,
    val canGoForward: Boolean = false,
    val isLoading: Boolean = true,
)

/** A calendar month of spending, day by day, with the days nothing was spent. */
@HiltViewModel
class CalendarViewModel(
    repository: FinanceRepository,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    // Hilt uses this one; tests pass their own [today].
    @Inject
    constructor(repository: FinanceRepository) : this(repository, LocalDate::now)

    private val month = MutableStateFlow(YearMonth.from(today()))

    val state: StateFlow<CalendarUiState> = combine(repository.transactions(), month) { transactions, month ->
        val today = today()
        val days = dailySpending(transactions, month)
        CalendarUiState(
            month = month,
            today = today,
            days = days,
            monthTotal = days.values.sum(),
            noSpendDays = noSpendDays(transactions, month, today),
            streak = noSpendStreak(transactions, today),
            firstDay = transactions.minOfOrNull { it.date },
            canGoForward = month < YearMonth.from(today),
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState(month = month.value, today = today()))

    /** Moves one month back (-1) or forward (+1), never past the current one. */
    fun shift(steps: Long) {
        val next = month.value.plusMonths(steps)
        if (next <= YearMonth.from(today())) month.value = next
    }
}
