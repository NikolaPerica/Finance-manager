package com.example.financemanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

// Table and column layout must stay as it was in database version 1, so that
// existing installs keep their data. Change it only together with a Migration.

enum class TransactionType { INCOME, EXPENSE }

enum class PaymentPeriod { ONCE, MONTHLY, QUARTERLY, YEARLY }

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TransactionType,
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    /** Stored as "yyyy-MM-dd" text, see [Converters]. */
    val date: LocalDate,
    val note: String,
    /** Category name; categories are referenced by name, not id. */
    val category: String,
    val type: TransactionType,
)

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val periodType: String,
    val period: Int,
)
