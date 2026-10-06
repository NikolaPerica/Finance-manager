package com.example.financemanager.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.data.Reminder
import com.example.financemanager.data.ReminderPayment
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class RemindersUiState(
    /** Soonest due first. */
    val reminders: List<Reminder> = emptyList(),
    /** Everything due by the end of this month, overdue payments included. */
    val dueThisMonth: Double = 0.0,
    val isLoading: Boolean = true,
)

fun summarizeReminders(reminders: List<Reminder>, today: LocalDate): RemindersUiState {
    val endOfMonth = today.with(TemporalAdjusters.lastDayOfMonth())
    return RemindersUiState(
        reminders = reminders.sortedWith(compareBy<Reminder> { it.nextDueDate }.thenBy { it.name.lowercase() }),
        dueThisMonth = reminders.filter { !it.nextDueDate.isAfter(endOfMonth) }.sumOf { it.amount },
        isLoading = false,
    )
}

sealed interface RemindersEvent {
    /** [next] is the reminder after the payment, or null when a one-off reminder is finished. */
    data class Paid(val payment: ReminderPayment, val next: Reminder?) : RemindersEvent
    data class Deleted(val reminder: Reminder) : RemindersEvent
}

class RemindersViewModel(
    private val repository: FinanceRepository,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    val state: StateFlow<RemindersUiState> = repository.reminders()
        .map { summarizeReminders(it, today()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemindersUiState())

    private val _events = Channel<RemindersEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun pay(reminder: Reminder) {
        viewModelScope.launch {
            val payment = repository.payReminder(reminder, paidOn = today())
            val next = repository.reminder(reminder.id)
            _events.send(RemindersEvent.Paid(payment, next))
        }
    }

    fun undoPayment(payment: ReminderPayment) {
        viewModelScope.launch { repository.undoPayment(payment) }
    }

    fun delete(reminder: Reminder) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
            _events.send(RemindersEvent.Deleted(reminder))
        }
    }

    fun restore(reminder: Reminder) {
        viewModelScope.launch { repository.saveReminder(reminder) }
    }
}
