package com.example.financemanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financemanager.data.AppDatabase
import com.example.financemanager.data.Category
import com.example.financemanager.data.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

data class TransactionGroup(
    val groupName: String,
    val transactions: List<Transaction>
)

enum class GroupType {
    TYPE,
    CATEGORY,
    DATE,
    MONTH,
    YEAR
}

class GraphViewModel(private val database: AppDatabase) : ViewModel() {

    private val _transactionsByGroup: MutableStateFlow<List<TransactionGroup>> = MutableStateFlow(emptyList())
    val transactionsByGroup: Flow<List<TransactionGroup>> = _transactionsByGroup

    fun getTransactionsByGroup(groupType: GroupType) {
        viewModelScope.launch(Dispatchers.IO) {
            val transactions = database.transactionDao().getAllTransactions()
            val categories = database.categoryDao().getAllCategories()

            val groups = when (groupType) {
                GroupType.TYPE -> groupByType(transactions)
                GroupType.CATEGORY -> groupByCategory(transactions, categories)
                GroupType.DATE -> groupByDate(transactions)
                GroupType.MONTH -> groupByMonth(transactions)
                GroupType.YEAR -> groupByYear(transactions)
            }

            _transactionsByGroup.value = groups
        }
    }

    private fun groupByType(transactions: List<Transaction>): List<TransactionGroup> {
        val groupedTransactions = transactions.groupBy { it.type }
        return groupedTransactions.map { (type, transactions) ->
            TransactionGroup(type.name, transactions)
        }
    }

    private fun groupByCategory(transactions: List<Transaction>, categories: List<Category>): List<TransactionGroup> {
        val groupedTransactions = transactions.groupBy { transaction ->
            categories.find { it.name == transaction.category }
        }
        return groupedTransactions.map { (category, transactions) ->
            TransactionGroup(category?.name ?: "", transactions)
        }
    }

    private fun groupByDate(transactions: List<Transaction>): List<TransactionGroup> {
        val groupedTransactions = transactions.groupBy { it.date }
        return groupedTransactions.map { (date, transactions) ->
            TransactionGroup(date, transactions)
        }
    }

    private fun groupByMonth(transactions: List<Transaction>): List<TransactionGroup> {
        val groupedTransactions = transactions.groupBy { it.date.substring(0, 7) }
        return groupedTransactions.map { (month, transactions) ->
            TransactionGroup(month, transactions)
        }
    }

    private fun groupByYear(transactions: List<Transaction>): List<TransactionGroup> {
        val groupedTransactions = transactions.groupBy { it.date.substring(0, 4) }
        return groupedTransactions.map { (year, transactions) ->
            TransactionGroup(year, transactions)
        }
    }
}
