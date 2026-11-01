package com.example.financemanager.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financemanager.data.SettingsRepository
import com.example.financemanager.domain.BudgetMonths
import com.example.financemanager.model.AppCurrency
import com.example.financemanager.model.Settings
import com.example.financemanager.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

/** Choices offered for locking again, in minutes; 0 locks as soon as the app is left. */
val LockAfterChoices = listOf(0, 1, 2, 5, 15)

@HiltViewModel
class SettingsViewModel @Inject constructor(private val repository: SettingsRepository) : ViewModel() {

    /** Null until the stored settings are read. */
    val settings: StateFlow<Settings?> =
        repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setTheme(theme: ThemeMode) = update { it.copy(theme = theme) }

    fun setCurrency(currency: AppCurrency) = update { it.copy(currency = currency) }

    fun setMonthStartDay(day: Int) = update { it.copy(monthStartDay = day.coerceIn(1, BudgetMonths.MAX_START_DAY)) }

    fun setReminderTime(time: LocalTime) = update { it.copy(reminderTime = time.withSecond(0).withNano(0)) }

    fun setLockAfterMinutes(minutes: Int) = update { it.copy(lockAfterMinutes = minutes.coerceAtLeast(0)) }

    private fun update(transform: (Settings) -> Settings) {
        viewModelScope.launch { repository.update(transform) }
    }
}
