package com.example.financemanager

import com.example.financemanager.data.Category
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.data.PaymentPeriod
import com.example.financemanager.data.Reminder
import com.example.financemanager.data.ReminderPayment
import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import com.example.financemanager.data.toExpense
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate

class FakeRepository : FinanceRepository {
    val transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val categories = MutableStateFlow<List<Category>>(emptyList())
    val reminders = MutableStateFlow<List<Reminder>>(emptyList())

    private var nextId = 1000L

    override fun transactions() = transactions

    override suspend fun addTransaction(transaction: Transaction) {
        transactions.update { it + transaction.copy(id = transaction.id.takeIf { id -> id != 0L } ?: nextId++) }
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactions.update { list -> list.filterNot { it.id == transaction.id } }
    }

    override fun categories(type: TransactionType) = categories.map { list -> list.filter { it.type == type } }

    override suspend fun addCategory(name: String, type: TransactionType) {
        categories.update { it + Category(id = nextId++, name = name, type = type) }
    }

    override fun reminders() = reminders

    override suspend fun reminder(id: Long) = reminders.value.find { it.id == id }

    override suspend fun saveReminder(reminder: Reminder) {
        val saved = if (reminder.id == 0L) reminder.copy(id = nextId++) else reminder
        reminders.update { list -> list.filterNot { it.id == saved.id } + saved }
    }

    override suspend fun deleteReminder(reminder: Reminder) {
        reminders.update { list -> list.filterNot { it.id == reminder.id } }
    }

    override suspend fun payReminder(reminder: Reminder, paidOn: LocalDate): ReminderPayment {
        val expense = reminder.toExpense(paidOn).copy(id = nextId++)
        transactions.update { it + expense }
        if (reminder.period == PaymentPeriod.ONCE) deleteReminder(reminder)
        else saveReminder(reminder.copy(paidCount = reminder.paidCount + 1))
        return ReminderPayment(reminder, expense.id)
    }

    override suspend fun undoPayment(payment: ReminderPayment) {
        transactions.update { list -> list.filterNot { it.id == payment.transactionId } }
        saveReminder(payment.before)
    }
}
