package com.example.financemanager.ui.wishes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.domain.parseAmount
import com.example.financemanager.domain.wishes.WishList
import com.example.financemanager.domain.wishes.wishList
import com.example.financemanager.model.Category
import com.example.financemanager.model.SavingsGoal
import com.example.financemanager.model.Transaction
import com.example.financemanager.model.TransactionType
import com.example.financemanager.model.Wish
import com.example.financemanager.ui.components.CategoryNameError
import com.example.financemanager.ui.components.categoryNameError
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

data class WishlistUiState(
    val list: WishList = WishList(emptyList(), emptyList(), 0.0, 0, 0),
    /** Goals a given-up price can go into. */
    val goals: List<SavingsGoal> = emptyList(),
    val categories: List<Category> = emptyList(),
    val today: LocalDate = LocalDate.now(),
    val isLoading: Boolean = true,
)

sealed interface WishEvent {
    /** [wish] was bought and recorded as [expense]; [before] is the wish as it was, to undo. */
    data class Bought(val before: Wish, val expense: Transaction) : WishEvent

    /** [wish] was given up; [goal] is where its price went, if anywhere. */
    data class Skipped(val wish: Wish, val goal: SavingsGoal?) : WishEvent
}

/** Choices for how long to think a wish over, in days. */
val WaitChoices = listOf(1, 2, 3)

@HiltViewModel
class WishlistViewModel(
    private val repository: FinanceRepository,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    // Hilt uses this one; tests pass their own [today].
    @Inject
    constructor(repository: FinanceRepository) : this(repository, LocalDate::now)

    private val _events = Channel<WishEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val state: StateFlow<WishlistUiState> = combine(
        repository.wishes(),
        repository.goals(),
        repository.categories(TransactionType.EXPENSE),
    ) { wishes, goals, categories ->
        WishlistUiState(wishList(wishes), goals.map { it.goal }, categories, today(), isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WishlistUiState(today = today()))

    /** Adds a wish to think over for [waitDays]; false, with nothing saved, if the name or price won't do. */
    fun add(name: String, price: String, waitDays: Int, category: String): Boolean {
        val amount = parseAmount(price)
        if (name.isBlank() || amount == null) return false
        viewModelScope.launch {
            repository.saveWish(Wish(name = name.trim(), price = amount, category = category, addedOn = today(), waitDays = waitDays))
        }
        return true
    }

    /** Returns why [name] can't be used as a new expense category, or null if it can. */
    fun validateCategoryName(name: String): CategoryNameError? = categoryNameError(name, state.value.categories)

    /** Creates a new expense category; the add dialog selects it. */
    fun addCategory(name: String) {
        val trimmed = name.trim()
        if (validateCategoryName(trimmed) != null) return
        viewModelScope.launch { repository.addCategory(trimmed, TransactionType.EXPENSE) }
    }

    /** Only once the time to think it over has passed. */
    fun buy(wish: Wish) {
        if (!wish.isReady(today())) return
        viewModelScope.launch { _events.send(WishEvent.Bought(wish, repository.buyWish(wish, today()))) }
    }

    /** Undoes [buy]: the wish waits again and the expense goes. */
    fun undoBuy(event: WishEvent.Bought) {
        viewModelScope.launch {
            repository.deleteTransaction(event.expense)
            repository.saveWish(event.before)
        }
    }

    fun skip(wish: Wish, goal: SavingsGoal?) {
        viewModelScope.launch {
            repository.skipWish(wish, today(), goal?.id)
            _events.send(WishEvent.Skipped(wish, goal))
        }
    }

    fun delete(wish: Wish) {
        viewModelScope.launch { repository.deleteWish(wish) }
    }

    /** Puts a just-deleted wish back. */
    fun restore(wish: Wish) {
        viewModelScope.launch { repository.saveWish(wish) }
    }
}
