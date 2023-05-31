package com.example.financemanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val date: String,
    val note: String,
    val category: String,
    val type: TransactionType
)

