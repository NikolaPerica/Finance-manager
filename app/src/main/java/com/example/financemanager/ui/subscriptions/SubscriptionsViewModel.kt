package com.example.financemanager.ui.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.domain.subscriptions.Subscription
import com.example.financemanager.domain.subscriptions.findSubscriptions
import com.example.financemanager.model.PaymentPeriod
import com.example.financemanager.model.Reminder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class SubscriptionsUiState(
    /** The most expensive first. */
    val subscriptions: List<Subscription> = emptyList(),
    val isLoading: Boolean = true,
) {
    val monthly: Double get() = subscriptions.sumOf { it.amount }
    val yearly: Double get() = subscriptions.sumOf { it.yearly }
}

@HiltViewModel
class SubscriptionsViewModel(
    private val repository: FinanceRepository,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    // Hilt uses this one; tests pass their own [today].
    @Inject
    constructor(repository: FinanceRepository) : this(repository, LocalDate::now)

    private val _reminderAdded = Channel<Subscription>(Channel.BUFFERED)

    /** Each subscription a reminder was just made for, to confirm it. */
    val reminderAdded = _reminderAdded.receiveAsFlow()

    val state: StateFlow<SubscriptionsUiState> = combine(repository.transactions(), repository.reminders()) { transactions, reminders ->
        SubscriptionsUiState(findSubscriptions(transactions, reminders, today()), isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubscriptionsUiState())

    /** A monthly payment reminder from the next expected payment on, so it can be paid from Podsjetnici. */
    fun addReminder(subscription: Subscription) {
        viewModelScope.launch {
            repository.saveReminder(
                Reminder(
                    name = subscription.name,
                    amount = subscription.amount,
                    period = PaymentPeriod.MONTHLY,
                    firstDueDate = subscription.nextExpected,
                    category = subscription.category,
                ),
            )
            _reminderAdded.send(subscription)
        }
    }
}
