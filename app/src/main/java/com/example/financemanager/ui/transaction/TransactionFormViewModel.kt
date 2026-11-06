package com.example.financemanager.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.data.SettingsRepository
import com.example.financemanager.domain.BudgetMonths
import com.example.financemanager.domain.amountInputText
import com.example.financemanager.domain.budgets.BudgetAlert
import com.example.financemanager.domain.budgets.budgetAlertFor
import com.example.financemanager.domain.isValidAmountInput
import com.example.financemanager.domain.parseAmount
import com.example.financemanager.domain.receipt.ReceiptData
import com.example.financemanager.model.Category
import com.example.financemanager.model.Template
import com.example.financemanager.model.Transaction
import com.example.financemanager.model.TransactionType
import com.example.financemanager.ui.budgetMonths
import com.example.financemanager.ui.components.CategoryNameError
import com.example.financemanager.ui.components.categoryNameError
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
    /** False while an existing transaction is still being loaded. */
    val isLoaded: Boolean = true,
    val isSaving: Boolean = false,
)

sealed interface FormEvent {
    /** [budgetAlert] is set when the expense brought its category close to or over budget. */
    data class Saved(val budgetAlert: BudgetAlert? = null) : FormEvent
    data object Deleted : FormEvent

    /** A receipt was read; the fields that were found have been filled in. */
    data class ReceiptRead(val receipt: ReceiptData) : FormEvent
    data class CategoryAdded(val name: String) : FormEvent
    data class TemplateSaved(val name: String) : FormEvent
}

/** Form for a new transaction ([transactionId] null) or for editing an existing one. */
@HiltViewModel(assistedFactory = TransactionFormViewModel.Factory::class)
class TransactionFormViewModel(
    val type: TransactionType,
    private val repository: FinanceRepository,
    private val transactionId: Long? = null,
    private val months: Flow<BudgetMonths> = flowOf(BudgetMonths()),
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    // Hilt uses this one; tests pass their own [today].
    @AssistedInject
    constructor(
        @Assisted type: TransactionType,
        repository: FinanceRepository,
        settings: SettingsRepository,
        @Assisted transactionId: Long?,
    ) : this(type, repository, transactionId, settings.budgetMonths, LocalDate::now)

    @AssistedFactory
    interface Factory {
        fun create(type: TransactionType, transactionId: Long?): TransactionFormViewModel
    }

    val isEditing: Boolean get() = transactionId != null
    private var existing: Transaction? = null

    private val _state = MutableStateFlow(TransactionFormState(date = today(), isLoaded = transactionId == null))
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

    /**
     * Fills the form from a scanned receipt: the amount and date when found, the shop as the
     * note (unless one was typed) and [category] when none is chosen yet.
     */
    fun applyReceipt(receipt: ReceiptData, category: String?) {
        _state.update { current ->
            current.copy(
                amount = receipt.amount?.let(::amountInputText) ?: current.amount,
                amountError = if (receipt.amount != null) false else current.amountError,
                date = receipt.date ?: current.date,
                note = current.note.ifBlank { receipt.merchant.orEmpty() },
                category = current.category.ifBlank { category.orEmpty() },
                categoryError = if (current.category.isBlank() && category != null) false else current.categoryError,
            )
        }
        viewModelScope.launch { _events.send(FormEvent.ReceiptRead(receipt)) }
    }

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
            val alert = alertFor(transaction)
            if (existing != null) repository.updateTransaction(transaction) else repository.addTransaction(transaction)
            _events.send(FormEvent.Saved(alert))
        }
    }

    /**
     * Saves what is filled in as a quick-add template called [name]. Needs the same amount and
     * category as saving the transaction; the form itself stays open and unsaved.
     */
    fun saveAsTemplate(name: String) {
        val current = _state.value
        val amount = parseAmount(current.amount)
        val categoryMissing = current.category.isBlank()
        if (amount == null || categoryMissing || name.isBlank()) {
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
        viewModelScope.launch {
            repository.addTemplate(Template(name = name.trim(), amount = amount, type = type, category = current.category, note = current.note.trim()))
            _events.send(FormEvent.TemplateSaved(name.trim()))
        }
    }

    /** A name for a new template: the note if there is one, otherwise the category. */
    fun suggestedTemplateName(): String = _state.value.let { it.note.trim().ifBlank { it.category } }

    fun delete() {
        val transaction = existing ?: return
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            _events.send(FormEvent.Deleted)
        }
    }

    /** Whether saving [transaction] pushes its category over a budget threshold this month. */
    private suspend fun alertFor(transaction: Transaction): BudgetAlert? = budgetAlertFor(
        transaction,
        others = repository.transactions().first().filter { it.id != existing?.id },
        categories = repository.categories(type).first(),
        today = today(),
        months = months.first(),
    )
}
