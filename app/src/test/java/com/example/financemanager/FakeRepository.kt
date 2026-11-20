package com.example.financemanager

import com.example.financemanager.data.FinanceData
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.data.ReminderPayment
import com.example.financemanager.data.toExpense
import com.example.financemanager.model.Category
import com.example.financemanager.model.GoalContribution
import com.example.financemanager.model.GoalWithSaved
import com.example.financemanager.model.PaymentPeriod
import com.example.financemanager.model.Reminder
import com.example.financemanager.model.SavingsGoal
import com.example.financemanager.model.Template
import com.example.financemanager.model.Transaction
import com.example.financemanager.model.TransactionType
import com.example.financemanager.model.Wish
import com.example.financemanager.model.WishStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate

class FakeRepository : FinanceRepository {
    val transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val categories = MutableStateFlow<List<Category>>(emptyList())
    val reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val goals = MutableStateFlow<List<SavingsGoal>>(emptyList())
    val contributions = MutableStateFlow<List<GoalContribution>>(emptyList())
    val templates = MutableStateFlow<List<Template>>(emptyList())
    val wishes = MutableStateFlow<List<Wish>>(emptyList())

    private var nextId = 1000L

    override fun transactions() = transactions

    override suspend fun addTransaction(transaction: Transaction) {
        transactions.update { it + transaction.copy(id = transaction.id.takeIf { id -> id != 0L } ?: nextId++) }
    }

    override suspend fun transaction(id: Long) = transactions.value.find { it.id == id }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactions.update { list -> list.map { if (it.id == transaction.id) transaction else it } }
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactions.update { list -> list.filterNot { it.id == transaction.id } }
    }

    override fun categories(type: TransactionType) = categories.map { list -> list.filter { it.type == type } }

    override suspend fun addCategory(name: String, type: TransactionType) {
        categories.update { it + Category(id = nextId++, name = name, type = type) }
    }

    override suspend fun setBudget(category: Category, budget: Double?) {
        categories.update { list -> list.map { if (it.id == category.id) it.copy(monthlyBudget = budget) else it } }
    }

    override suspend fun updateCategory(category: Category, name: String, color: Int?) {
        categories.update { list -> list.map { if (it.id == category.id) it.copy(name = name, color = color) else it } }
        if (name != category.name) moveCategoryData(category, name)
    }

    override suspend fun deleteCategory(category: Category, moveTo: Category?) {
        moveCategoryData(category, moveTo?.name.orEmpty())
        categories.update { list -> list.filterNot { it.id == category.id } }
    }

    private fun moveCategoryData(category: Category, to: String) {
        transactions.update { list ->
            list.map { if (it.type == category.type && it.category == category.name) it.copy(category = to) else it }
        }
        templates.update { list ->
            list.map { if (it.type == category.type && it.category == category.name) it.copy(category = to) else it }
        }
        if (category.type == TransactionType.EXPENSE) {
            wishes.update { list -> list.map { if (it.category == category.name) it.copy(category = to) else it } }
        }
        if (category.type == TransactionType.EXPENSE) {
            reminders.update { list -> list.map { if (it.category == category.name) it.copy(category = to) else it } }
        }
    }

    override fun goals() = combine(goals, contributions) { goals, contributions ->
        goals.map { goal -> GoalWithSaved(goal, contributions.filter { it.goalId == goal.id }.sumOf { it.amount }) }
    }

    override fun contributions(goalId: Long) =
        contributions.map { list -> list.filter { it.goalId == goalId }.sortedWith(compareByDescending<GoalContribution> { it.date }.thenByDescending { it.id }) }

    override suspend fun goal(id: Long) = goals.value.find { it.id == id }

    override suspend fun saveGoal(goal: SavingsGoal): Long {
        val saved = if (goal.id == 0L) goal.copy(id = nextId++) else goal
        goals.update { list -> list.filterNot { it.id == saved.id } + saved }
        return saved.id
    }

    override suspend fun deleteGoal(goal: SavingsGoal) {
        goals.update { list -> list.filterNot { it.id == goal.id } }
        contributions.update { list -> list.filterNot { it.goalId == goal.id } }
    }

    override suspend fun addContribution(contribution: GoalContribution) {
        contributions.update { it + contribution.copy(id = contribution.id.takeIf { id -> id != 0L } ?: nextId++) }
    }

    override suspend fun deleteContribution(contribution: GoalContribution) {
        contributions.update { list -> list.filterNot { it.id == contribution.id } }
    }

    override suspend fun snapshot() = FinanceData(
        categories.value, transactions.value, reminders.value, goals.value, contributions.value, templates.value, wishes.value,
    )

    override suspend fun replaceAll(data: FinanceData) {
        categories.value = data.categories
        transactions.value = data.transactions
        reminders.value = data.reminders
        goals.value = data.goals
        contributions.value = data.contributions
        templates.value = data.templates
        wishes.value = data.wishes
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

    override fun templates() = templates.map { list -> list.sortedWith(compareByDescending<Template> { it.uses }.thenBy { it.name.lowercase() }) }

    override suspend fun addTemplate(template: Template) {
        val saved = if (template.id == 0L) template.copy(id = nextId++) else template
        templates.update { it + saved }
    }

    override suspend fun deleteTemplate(template: Template) {
        templates.update { list -> list.filterNot { it.id == template.id } }
    }

    override suspend fun useTemplate(template: Template, on: LocalDate): Transaction {
        val transaction = template.toTransaction(on).copy(id = nextId++)
        transactions.update { it + transaction }
        templates.update { list -> list.map { if (it.id == template.id) it.copy(uses = it.uses + 1) else it } }
        return transaction
    }

    override fun wishes() = wishes

    override suspend fun saveWish(wish: Wish) {
        val saved = if (wish.id == 0L) wish.copy(id = nextId++) else wish
        wishes.update { list -> list.filterNot { it.id == saved.id } + saved }
    }

    override suspend fun deleteWish(wish: Wish) {
        wishes.update { list -> list.filterNot { it.id == wish.id } }
    }

    override suspend fun buyWish(wish: Wish, on: LocalDate): Transaction {
        val expense = wish.toExpense(on).copy(id = nextId++)
        transactions.update { it + expense }
        saveWish(wish.copy(status = WishStatus.BOUGHT, decidedOn = on))
        return expense
    }

    override suspend fun skipWish(wish: Wish, on: LocalDate, goalId: Long?) {
        saveWish(wish.copy(status = WishStatus.SKIPPED, decidedOn = on))
        if (goalId != null) addContribution(GoalContribution(goalId = goalId, amount = wish.price, date = on))
    }
}
