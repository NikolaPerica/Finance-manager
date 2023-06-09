package com.example.financemanager.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.financemanager.data.TransactionType.*

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions")
    fun getAllTransactions(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE date = :selectedDate")
    fun getTransactionsByDate(selectedDate: String): List<Transaction>

    @Insert
    fun insertTransaction(transaction: Transaction)

    @Update
    fun updateTransaction(transaction: Transaction)

    @Delete
    fun deleteTransaction(transaction: Transaction)





}
