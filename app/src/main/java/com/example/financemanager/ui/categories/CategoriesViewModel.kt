package com.example.financemanager.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financemanager.data.Category
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import com.example.financemanager.ui.components.CategoryNameError
import com.example.financemanager.ui.components.categoryNameError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A category with how much it has been used. */
data class CategoryUsage(val category: Category, val count: Int, val total: Double)

data class CategoriesUiState(
    val type: TransactionType = TransactionType.EXPENSE,
    /** By name. */
    val items: List<CategoryUsage> = emptyList(),
    val isLoading: Boolean = true,
)

fun categoryUsage(categories: List<Category>, transactions: List<Transaction>): List<CategoryUsage> {
    val byName = transactions.groupBy { it.type to it.category }
    return categories.map { category ->
        val used = byName[category.type to category.name].orEmpty()
        CategoryUsage(category, used.size, used.sumOf { it.amount })
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val type = MutableStateFlow(TransactionType.EXPENSE)

    val state: StateFlow<CategoriesUiState> = type.flatMapLatest { type ->
        combine(repository.categories(type), repository.transactions()) { categories, transactions ->
            CategoriesUiState(type, categoryUsage(categories, transactions), isLoading = false)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    fun onTypeChange(newType: TransactionType) {
        type.value = newType
    }

    /** Why [name] can't be used, or null. [except] is the category being renamed, which may keep its name. */
    fun validateName(name: String, except: Category? = null): CategoryNameError? =
        categoryNameError(name, state.value.items.map { it.category }.filter { it.id != except?.id })

    fun addCategory(name: String) {
        val trimmed = name.trim()
        if (validateName(trimmed) != null) return
        viewModelScope.launch { repository.addCategory(trimmed, type.value) }
    }

    fun update(category: Category, name: String, color: Int?) {
        val trimmed = name.trim()
        if (validateName(trimmed, except = category) != null) return
        viewModelScope.launch { repository.updateCategory(category, trimmed, color) }
    }

    /** Deletes [category], moving its transactions to [moveTo] (merging the two) or leaving them without one. */
    fun delete(category: Category, moveTo: Category?) {
        viewModelScope.launch { repository.deleteCategory(category, moveTo?.takeIf { it.id != category.id }) }
    }
}
