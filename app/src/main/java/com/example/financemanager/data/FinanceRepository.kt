package com.example.financemanager.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** What [FinanceRepository.payReminder] changed, so it can be undone. */
data class ReminderPayment(val before: Reminder, val transactionId: Long)

/** Single entry point the UI uses to read and change finance data. */
interface FinanceRepository {
    fun transactions(): Flow<List<Transaction>>
    suspend fun transaction(id: Long): Transaction?
    suspend fun addTransaction(transaction: Transaction)
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)

    fun categories(type: TransactionType): Flow<List<Category>>
    suspend fun addCategory(name: String, type: TransactionType)

    /** Sets the monthly budget of a category; null removes it. */
    suspend fun setBudget(category: Category, budget: Double?)

    /**
     * Renames and recolours a category. Its transactions (and, for expenses, payment
     * reminders) follow the new name.
     */
    suspend fun updateCategory(category: Category, name: String, color: Int?)

    /**
     * Deletes a category. Its transactions and reminders move to [moveTo], or are left
     * without a category when it is null. Moving into another category merges the two.
     */
    suspend fun deleteCategory(category: Category, moveTo: Category?)

    fun reminders(): Flow<List<Reminder>>
    suspend fun reminder(id: Long): Reminder?
    suspend fun saveReminder(reminder: Reminder)
    suspend fun deleteReminder(reminder: Reminder)

    /**
     * Records the reminder's next payment as an expense dated [paidOn] and moves the
     * reminder on to its following due date (a one-off reminder is removed).
     */
    suspend fun payReminder(reminder: Reminder, paidOn: LocalDate): ReminderPayment

    /** Reverts [payReminder]: removes the expense and restores the reminder. */
    suspend fun undoPayment(payment: ReminderPayment)
}

/** The expense a payment of this reminder is recorded as. */
fun Reminder.toExpense(paidOn: LocalDate) = Transaction(
    amount = amount,
    date = paidOn,
    note = if (category.isBlank()) note else name,
    category = category.ifBlank { name },
    type = TransactionType.EXPENSE,
)

class RoomFinanceRepository(private val db: AppDatabase) : FinanceRepository {
    override fun transactions() = db.transactionDao().observeAll()

    override suspend fun transaction(id: Long) = db.transactionDao().get(id)

    override suspend fun addTransaction(transaction: Transaction) {
        db.transactionDao().insert(transaction)
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        db.transactionDao().update(transaction)
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        db.transactionDao().delete(transaction)
    }

    override fun categories(type: TransactionType) = db.categoryDao().observeByType(type)

    override suspend fun addCategory(name: String, type: TransactionType) {
        db.categoryDao().insert(Category(name = name, type = type))
    }

    override suspend fun setBudget(category: Category, budget: Double?) {
        db.categoryDao().setBudget(category.id, budget)
    }

    override suspend fun updateCategory(category: Category, name: String, color: Int?) = db.withTransaction {
        db.categoryDao().update(category.copy(name = name, color = color))
        if (name != category.name) moveCategoryData(category, name)
    }

    override suspend fun deleteCategory(category: Category, moveTo: Category?) = db.withTransaction {
        moveCategoryData(category, moveTo?.name.orEmpty())
        db.categoryDao().delete(category)
    }

    private suspend fun moveCategoryData(category: Category, to: String) {
        db.transactionDao().moveCategory(category.name, to, category.type)
        // Reminders always become expenses, so only expense categories are used there.
        if (category.type == TransactionType.EXPENSE) db.reminderDao().moveCategory(category.name, to)
    }

    override fun reminders() = db.reminderDao().observeAll()

    override suspend fun reminder(id: Long) = db.reminderDao().get(id)

    override suspend fun saveReminder(reminder: Reminder) {
        db.reminderDao().upsert(reminder)
    }

    override suspend fun deleteReminder(reminder: Reminder) {
        db.reminderDao().delete(reminder)
    }

    override suspend fun payReminder(reminder: Reminder, paidOn: LocalDate) = db.withTransaction {
        val transactionId = db.transactionDao().insert(reminder.toExpense(paidOn))
        if (reminder.period == PaymentPeriod.ONCE) {
            db.reminderDao().delete(reminder)
        } else {
            db.reminderDao().upsert(reminder.copy(paidCount = reminder.paidCount + 1))
        }
        ReminderPayment(before = reminder, transactionId = transactionId)
    }

    override suspend fun undoPayment(payment: ReminderPayment) = db.withTransaction {
        db.transactionDao().deleteById(payment.transactionId)
        db.reminderDao().upsert(payment.before)
    }
}
