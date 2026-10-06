package com.example.financemanager.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financemanager.data.Category
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.data.PaymentPeriod
import com.example.financemanager.data.Reminder
import com.example.financemanager.data.TransactionType
import com.example.financemanager.ui.Croatian
import com.example.financemanager.ui.isValidAmountInput
import com.example.financemanager.ui.parseAmount
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate

data class ReminderFormState(
    val name: String = "",
    val amount: String = "",
    val dueDate: LocalDate = LocalDate.now(),
    val period: PaymentPeriod = PaymentPeriod.MONTHLY,
    val category: String = "",
    val note: String = "",
    val nameError: Boolean = false,
    val amountError: Boolean = false,
    val nameShake: Int = 0,
    val amountShake: Int = 0,
    /** False while an existing reminder is still being loaded. */
    val isLoaded: Boolean = true,
    val isSaving: Boolean = false,
)

/** Form for a new reminder ([reminderId] null) or for editing an existing one. */
class ReminderFormViewModel(
    private val reminderId: Long?,
    private val repository: FinanceRepository,
) : ViewModel() {

    val isEditing: Boolean get() = reminderId != null
    private var existing: Reminder? = null

    private val _state = MutableStateFlow(ReminderFormState(isLoaded = reminderId == null))
    val state: StateFlow<ReminderFormState> = _state.asStateFlow()

    val categories: StateFlow<List<Category>> = repository.categories(TransactionType.EXPENSE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Fires once the reminder is saved or deleted and the form should close. */
    private val _done = Channel<Unit>(Channel.BUFFERED)
    val done = _done.receiveAsFlow()

    init {
        if (reminderId != null) {
            viewModelScope.launch {
                val reminder = repository.reminder(reminderId)
                existing = reminder
                _state.update { current ->
                    reminder?.let {
                        current.copy(
                            name = it.name,
                            amount = AmountText.get()!!.format(it.amount),
                            dueDate = it.nextDueDate,
                            period = it.period,
                            category = it.category,
                            note = it.note,
                            isLoaded = true,
                        )
                    } ?: current.copy(isLoaded = true)
                }
            }
        }
    }

    fun onNameChange(name: String) = _state.update { it.copy(name = name, nameError = false) }

    fun onAmountChange(text: String) {
        if (isValidAmountInput(text)) _state.update { it.copy(amount = text, amountError = false) }
    }

    fun onDueDateChange(date: LocalDate) = _state.update { it.copy(dueDate = date) }

    fun onPeriodChange(period: PaymentPeriod) = _state.update { it.copy(period = period) }

    fun onCategoryChange(category: String) = _state.update { it.copy(category = category) }

    fun onNoteChange(note: String) = _state.update { it.copy(note = note) }

    fun save() {
        val current = _state.value
        if (current.isSaving || !current.isLoaded) return
        val amount = parseAmount(current.amount)
        val nameMissing = current.name.isBlank()
        if (amount == null || nameMissing) {
            _state.update {
                it.copy(
                    nameError = nameMissing,
                    amountError = amount == null,
                    nameShake = if (nameMissing) it.nameShake + 1 else it.nameShake,
                    amountShake = if (amount == null) it.amountShake + 1 else it.amountShake,
                )
            }
            return
        }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            // The chosen date becomes the new starting point of the schedule.
            repository.saveReminder(
                Reminder(
                    id = existing?.id ?: 0,
                    name = current.name.trim(),
                    amount = amount,
                    period = current.period,
                    firstDueDate = current.dueDate,
                    paidCount = 0,
                    category = current.category,
                    note = current.note.trim(),
                ),
            )
            _done.send(Unit)
        }
    }

    fun delete() {
        val reminder = existing ?: return
        viewModelScope.launch {
            repository.deleteReminder(reminder)
            _done.send(Unit)
        }
    }

    private companion object {
        // Plain "1234,5" so the stored amount round-trips through the amount field.
        val AmountText: ThreadLocal<DecimalFormat> =
            ThreadLocal.withInitial { DecimalFormat("0.##", DecimalFormatSymbols(Croatian)) }
    }
}
