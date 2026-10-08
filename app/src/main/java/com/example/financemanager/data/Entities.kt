package com.example.financemanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

// Table and column layout is the database schema: change it only together with a
// Migration in AppDatabase, so that existing installs keep their data.

enum class TransactionType { INCOME, EXPENSE }

enum class PaymentPeriod(val months: Int) {
    ONCE(0),
    MONTHLY(1),
    QUARTERLY(3),
    SEMI_ANNUALLY(6),
    YEARLY(12),
}

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TransactionType,
    /** Monthly spending limit for an expense category, or null for none. */
    val monthlyBudget: Double? = null,
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
    val period: PaymentPeriod,
    /** Date of the first payment; later ones are counted from it so month ends don't drift. */
    val firstDueDate: LocalDate,
    /** How many payments have been made so far. */
    val paidCount: Int = 0,
    /** Expense category used for the transaction created when the payment is made. */
    val category: String = "",
    val note: String = "",
) {
    /** When the next payment is due. */
    val nextDueDate: LocalDate
        get() = firstDueDate.plusMonths(period.months.toLong() * paidCount)
}
