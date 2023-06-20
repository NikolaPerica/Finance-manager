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

    @Query("SELECT * FROM transactions WHERE (date = :date OR :date IS NULL) AND (category = :category OR :category IS NULL) AND (type = :type OR :type IS NULL)")
    fun getFilteredTransactions(date: String?, category: String?, type: String?): List<Transaction>

    @Query("SELECT * FROM transactions WHERE date >= :weekStartDate AND date <= :weekEndDate ")
    fun getFilteredTransactionsByWeek(weekStartDate: String, weekEndDate: String): List<Transaction>


    @Query("SELECT * FROM transactions WHERE date >= :monthStartDate AND date <= :monthEndDate ")
    fun getFilteredTransactionsByMonth(monthStartDate: String, monthEndDate: String): List<Transaction>

    @Query("SELECT * FROM transactions WHERE category = :categoryName")
    fun getTransactionsByCategory(categoryName: String): List<Transaction>






}
