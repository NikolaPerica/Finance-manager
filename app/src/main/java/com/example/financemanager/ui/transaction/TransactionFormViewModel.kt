package com.example.financemanager.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financemanager.data.Category
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import com.example.financemanager.ui.amountInputText
import com.example.financemanager.ui.budgets.BudgetAlert
import com.example.financemanager.ui.budgets.budgetAlert
import com.example.financemanager.ui.budgets.spentByCategory
import com.example.financemanager.ui.components.CategoryNameError
import com.example.financemanager.ui.components.categoryNameError
import com.example.financemanager.ui.isValidAmountInput
import com.example.financemanager.ui.parseAmount
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

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
    /** False while an existing transaction is still being loaded. */
    val isLoaded: Boolean = true,
    val isSaving: Boolean = false,
)

sealed interface FormEvent {
    /** [budgetAlert] is set when the expense brought its category close to or over budget. */
    data class Saved(val budgetAlert: BudgetAlert? = null) : FormEvent
    data object Deleted : FormEvent
    data class CategoryAdded(val name: String) : FormEvent
}

/** Form for a new transaction ([transactionId] null) or for editing an existing one. */
class TransactionFormViewModel(
    val type: TransactionType,
    private val repository: FinanceRepository,
    private val transactionId: Long? = null,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    val isEditing: Boolean get() = transactionId != null
    private var existing: Transaction? = null

    private val _state = MutableStateFlow(TransactionFormState(isLoaded = transactionId == null))
    val state: StateFlow<TransactionFormState> = _state.asStateFlow()

    val categories: StateFlow<List<Category>> = repository.categories(type)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _events = Channel<FormEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        if (transactionId != null) {
            viewModelScope.launch {
                val transaction = repository.transaction(transactionId)
                existing = transaction
                _state.update { current ->
                    transaction?.let {
                        current.copy(
                            amount = amountInputText(it.amount),
                            date = it.date,
                            category = it.category,
                            note = it.note,
                            isLoaded = true,
                        )
                    } ?: current.copy(isLoaded = true)
                }
            }
        }
    }

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
        if (current.isSaving || !current.isLoaded) return
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
            val transaction = Transaction(
                id = existing?.id ?: 0,
                amount = amount,
                date = current.date,
                note = current.note.trim(),
                category = current.category,
                type = type,
            )
            val alert = budgetAlertFor(transaction)
            if (existing != null) repository.updateTransaction(transaction) else repository.addTransaction(transaction)
            _events.send(FormEvent.Saved(alert))
        }
    }

    fun delete() {
        val transaction = existing ?: return
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            _events.send(FormEvent.Deleted)
        }
    }

    /** Whether saving [transaction] pushes its category over a budget threshold this month. */
    private suspend fun budgetAlertFor(transaction: Transaction): BudgetAlert? {
        val month = YearMonth.from(today())
        if (type != TransactionType.EXPENSE || YearMonth.from(transaction.date) != month) return null
        val budget = repository.categories(type).first()
            .find { it.name == transaction.category }?.monthlyBudget ?: return null
        val others = repository.transactions().first().filter { it.id != existing?.id }
        val before = spentByCategory(others, month)[transaction.category] ?: 0.0
        return budgetAlert(transaction.category, budget, before, before + transaction.amount)
    }
}
