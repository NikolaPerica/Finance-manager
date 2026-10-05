package com.example.financemanager.data

import kotlinx.coroutines.flow.Flow

/** Single entry point the UI uses to read and change finance data. */
interface FinanceRepository {
    fun transactions(): Flow<List<Transaction>>
    suspend fun addTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)

    fun categories(type: TransactionType): Flow<List<Category>>
    suspend fun addCategory(name: String, type: TransactionType)
}

class RoomFinanceRepository(private val db: AppDatabase) : FinanceRepository {
    override fun transactions() = db.transactionDao().observeAll()

    override suspend fun addTransaction(transaction: Transaction) {
        db.transactionDao().insert(transaction)
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        db.transactionDao().delete(transaction)
    }

    override fun categories(type: TransactionType) = db.categoryDao().observeByType(type)

    override suspend fun addCategory(name: String, type: TransactionType) {
        db.categoryDao().insert(Category(name = name, type = type))
    }
}
