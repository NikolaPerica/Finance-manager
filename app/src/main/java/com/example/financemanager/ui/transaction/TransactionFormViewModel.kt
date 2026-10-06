package com.example.financemanager.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financemanager.data.Category
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import com.example.financemanager.ui.components.CategoryNameError
import com.example.financemanager.ui.components.categoryNameError
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
import java.time.LocalDate

data class TransactionFormState(
    val amount: String = "",
    val date: LocalDate = LocalDate.now(),
    val category: String = "",
    val note: String = "",
    val amountError: Boolean = false,
    val categoryError: Boolean = false,
    /** Bumped on every failed save so the UI can shake the offending field again. */
    val amountShake: Int = 0,
    val categoryShake: Int = 0,
    val isSaving: Boolean = false,
)

sealed interface FormEvent {
    data object Saved : FormEvent
    data class CategoryAdded(val name: String) : FormEvent
}

class TransactionFormViewModel(
    val type: TransactionType,
    private val repository: FinanceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionFormState())
    val state: StateFlow<TransactionFormState> = _state.asStateFlow()

    val categories: StateFlow<List<Category>> = repository.categories(type)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _events = Channel<FormEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onAmountChange(text: String) {
        if (isValidAmountInput(text)) _state.update { it.copy(amount = text, amountError = false) }
    }

    fun onDateChange(date: LocalDate) = _state.update { it.copy(date = date) }

    fun onCategoryChange(name: String) = _state.update { it.copy(category = name, categoryError = false) }

    fun onNoteChange(note: String) = _state.update { it.copy(note = note) }

    /** Returns why [name] can't be used as a new category, or null if it can. */
    fun validateCategoryName(name: String): CategoryNameError? = categoryNameError(name, categories.value)

    fun addCategory(name: String) {
        val trimmed = name.trim()
        if (validateCategoryName(trimmed) != null) return
        viewModelScope.launch {
            repository.addCategory(trimmed, type)
            onCategoryChange(trimmed)
            _events.send(FormEvent.CategoryAdded(trimmed))
        }
    }

    fun save() {
        val current = _state.value
        if (current.isSaving) return
        val amount = parseAmount(current.amount)
        val categoryMissing = current.category.isBlank()
        if (amount == null || categoryMissing) {
            _state.update {
                it.copy(
                    amountError = amount == null,
                    categoryError = categoryMissing,
                    amountShake = if (amount == null) it.amountShake + 1 else it.amountShake,
                    categoryShake = if (categoryMissing) it.categoryShake + 1 else it.categoryShake,
                )
            }
            return
        }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            repository.addTransaction(
                Transaction(
                    amount = amount,
                    date = current.date,
                    note = current.note.trim(),
                    category = current.category,
                    type = type,
                ),
            )
            _events.send(FormEvent.Saved)
        }
    }
}
