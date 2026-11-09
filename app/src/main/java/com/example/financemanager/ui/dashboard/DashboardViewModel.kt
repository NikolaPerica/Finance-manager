package com.example.financemanager.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.data.SettingsRepository
import com.example.financemanager.domain.BudgetMonths
import com.example.financemanager.domain.budgets.BudgetAlert
import com.example.financemanager.domain.budgets.BudgetSummary
import com.example.financemanager.domain.budgets.SafeToSpend
import com.example.financemanager.domain.budgets.budgetAlertFor
import com.example.financemanager.domain.budgets.budgetItems
import com.example.financemanager.domain.budgets.budgetSummary
import com.example.financemanager.domain.budgets.safeToSpend
import com.example.financemanager.domain.goals.GoalProgress
import com.example.financemanager.domain.goals.GoalStatus
import com.example.financemanager.domain.goals.progress
import com.example.financemanager.domain.insights.Insight
import com.example.financemanager.domain.insights.buildInsights
import com.example.financemanager.model.Category
import com.example.financemanager.model.GoalWithSaved
import com.example.financemanager.model.Reminder
import com.example.financemanager.model.Template
import com.example.financemanager.model.Transaction
import com.example.financemanager.model.TransactionType
import com.example.financemanager.ui.budgetMonths
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DashboardUiState(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    /** The most recent transactions, newest first. */
    val transactions: List<Transaction> = emptyList(),
    /** How many transactions there are in total. */
    val transactionCount: Int = 0,
    /** All categories, to colour the transactions. */
    val categories: List<Category> = emptyList(),
    /** The next few payment reminders, soonest first. */
    val upcoming: List<Reminder> = emptyList(),
    /** This month's totals over categories with a budget, null when none has one. */
    val budget: BudgetSummary? = null,
    /** What may still be spent today within the budgets, null when none is set. */
    val safeToSpend: SafeToSpend? = null,
    /** Goals still being saved for, nearest deadline first (at most a few). */
    val goals: List<GoalProgress> = emptyList(),
    val hasGoals: Boolean = false,
    /** The month-end forecast first, then the most notable other insights. */
    val insights: List<Insight> = emptyList(),
    /** Quick-add templates, the most used first. */
    val templates: List<Template> = emptyList(),
    val isLoading: Boolean = true,
) {
    val balance: Double get() = income - expense
}

private const val UPCOMING_COUNT = 3
private const val RECENT_COUNT = 10
private const val GOALS_COUNT = 2
private const val INSIGHTS_COUNT = 3

fun summarize(
    transactions: List<Transaction>,
    reminders: List<Reminder> = emptyList(),
    categories: List<Category> = emptyList(),
    goals: List<GoalWithSaved> = emptyList(),
    today: LocalDate = LocalDate.now(),
    months: BudgetMonths = BudgetMonths(),
) = DashboardUiState(
    income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
    expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
    transactions = transactions.sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.id })
        .take(RECENT_COUNT),
    transactionCount = transactions.size,
    categories = categories,
    upcoming = reminders.sortedBy { it.nextDueDate }.take(UPCOMING_COUNT),
    budget = budgetSummary(budgetItems(categories.filter { it.type == TransactionType.EXPENSE }, transactions, months.of(today), months)),
    safeToSpend = safeToSpend(categories, transactions, reminders, today, months),
    goals = goals.map { it.progress(today) }.filter { it.status != GoalStatus.REACHED }.take(GOALS_COUNT),
    hasGoals = goals.isNotEmpty(),
    insights = buildInsights(transactions, categories, today, months).take(INSIGHTS_COUNT),
    isLoading = false,
)

sealed interface DashboardEvent {
    /** A template added [transaction]; [budgetAlert] is set when that crossed a budget threshold. */
    data class Added(val transaction: Transaction, val budgetAlert: BudgetAlert?) : DashboardEvent
}

@HiltViewModel
class DashboardViewModel(
    private val repository: FinanceRepository,
    private val months: Flow<BudgetMonths> = flowOf(BudgetMonths()),
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    @Inject
    constructor(repository: FinanceRepository, settings: SettingsRepository) :
        this(repository, settings.budgetMonths, LocalDate::now)

    private val _events = Channel<DashboardEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val categories = combine(
        repository.categories(TransactionType.EXPENSE),
        repository.categories(TransactionType.INCOME),
    ) { expense, income -> expense + income }

    val state: StateFlow<DashboardUiState> = combine(
        repository.transactions(),
        repository.reminders(),
        categories,
        repository.goals(),
        months,
    ) { transactions, reminders, categories, goals, months ->
        summarize(transactions, reminders, categories, goals, today(), months)
    }
        .combine(repository.templates()) { state, templates -> state.copy(templates = templates) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun delete(transaction: Transaction) {
        viewModelScope.launch { repository.deleteTransaction(transaction) }
    }

    /** Puts a just-deleted transaction back, keeping its id and position. */
    fun restore(transaction: Transaction) {
        viewModelScope.launch { repository.addTransaction(transaction) }
    }

    /** Adds the template's transaction for today; undo by deleting the transaction it reports. */
    fun useTemplate(template: Template) {
        viewModelScope.launch {
            val others = repository.transactions().first()
            val added = repository.useTemplate(template, today())
            val categories = repository.categories(TransactionType.EXPENSE).first()
            _events.send(DashboardEvent.Added(added, budgetAlertFor(added, others, categories, today(), months.first())))
        }
    }

    fun deleteTemplate(template: Template) {
        viewModelScope.launch { repository.deleteTemplate(template) }
    }

    /** Puts a just-deleted template back. */
    fun restoreTemplate(template: Template) {
        viewModelScope.launch { repository.addTemplate(template) }
    }
}
