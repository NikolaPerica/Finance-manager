package com.example.financemanager

import com.example.financemanager.data.Category
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeRepository : FinanceRepository {
    val transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val categories = MutableStateFlow<List<Category>>(emptyList())

    override fun transactions() = transactions

    override suspend fun addTransaction(transaction: Transaction) {
        transactions.update { it + transaction.copy(id = transaction.id.takeIf { id -> id != 0L } ?: (it.size + 1L)) }
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactions.update { list -> list.filterNot { it.id == transaction.id } }
    }

    override fun categories(type: TransactionType) = categories.map { list -> list.filter { it.type == type } }

    override suspend fun addCategory(name: String, type: TransactionType) {
        categories.update { it + Category(id = it.size + 1L, name = name, type = type) }
    }
}
