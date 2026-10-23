package com.example.financemanager.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.financemanager.FinanceApp
import com.example.financemanager.QuickAction
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import com.example.financemanager.security.AppLock
import com.example.financemanager.ui.budgets.BudgetsScreen
import com.example.financemanager.ui.budgets.BudgetsViewModel
import com.example.financemanager.ui.categories.CategoriesScreen
import com.example.financemanager.ui.categories.CategoriesViewModel
import com.example.financemanager.ui.components.EmphasizedDecelerate
import com.example.financemanager.ui.dashboard.DashboardScreen
import com.example.financemanager.ui.dashboard.DashboardViewModel
import com.example.financemanager.ui.export.ExportScreen
import com.example.financemanager.ui.export.ExportViewModel
import com.example.financemanager.ui.goals.GoalDetailScreen
import com.example.financemanager.ui.goals.GoalDetailViewModel
import com.example.financemanager.ui.goals.GoalFormScreen
import com.example.financemanager.ui.goals.GoalFormViewModel
import com.example.financemanager.ui.goals.GoalsScreen
import com.example.financemanager.ui.goals.GoalsViewModel
import com.example.financemanager.ui.login.LoginScreen
import com.example.financemanager.ui.reminders.ReminderFormScreen
import com.example.financemanager.ui.reminders.ReminderFormViewModel
import com.example.financemanager.ui.reminders.RemindersScreen
import com.example.financemanager.ui.reminders.RemindersViewModel
import com.example.financemanager.ui.stats.StatsScreen
import com.example.financemanager.ui.stats.StatsViewModel
import com.example.financemanager.ui.transaction.TransactionFormScreen
import com.example.financemanager.ui.transaction.TransactionFormViewModel
import com.example.financemanager.ui.transactions.PeriodFilter
import com.example.financemanager.ui.transactions.TransactionFilter
import com.example.financemanager.ui.transactions.TransactionsScreen
import com.example.financemanager.ui.transactions.TransactionsViewModel
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data object DashboardRoute

/** [id] of the transaction to edit, or [NEW] for a new one. */
@Serializable
data class TransactionFormRoute(val isIncome: Boolean, val id: Long = NEW) {
    companion object {
        const val NEW = -1L
    }
}

@Serializable
data object RemindersRoute

@Serializable
data object StatsRoute

@Serializable
data object BudgetsRoute

/**
 * The transaction list, optionally opened with filters already set: a category name
 * ("" for none), [type] as a [TransactionType] name, and an ISO date range.
 */
@Serializable
data class TransactionsRoute(
    val category: String? = null,
    val type: String? = null,
    val from: String? = null,
    val to: String? = null,
) {
    fun filter() = TransactionFilter(
        type = type?.let(TransactionType::valueOf),
        category = category,
        period = if (from != null && to != null) PeriodFilter.Range(LocalDate.parse(from), LocalDate.parse(to)) else PeriodFilter.All,
    )
}

@Serializable
data object CategoriesRoute

@Serializable
data object GoalsRoute

@Serializable
data object ExportRoute

@Serializable
data class GoalRoute(val id: Long)

/** [id] of the goal to edit, or [NEW] for a new one. */
@Serializable
data class GoalFormRoute(val id: Long = NEW) {
    companion object {
        const val NEW = -1L
    }
}

/** [id] of the reminder to edit, or [NEW] for a new one. */
@Serializable
data class ReminderFormRoute(val id: Long = NEW) {
    companion object {
        const val NEW = -1L
    }
}

private fun Transaction.editRoute() = TransactionFormRoute(isIncome = type == TransactionType.INCOME, id = id)

private fun CreationExtras.repository(): FinanceRepository = (this[APPLICATION_KEY] as FinanceApp).repository

/**
 * The app's screens with the fingerprint/PIN screen on top of them while [appLock] is
 * locked, so unlocking returns to wherever the user was.
 */
@Composable
fun FinanceNavHost(
    appLock: AppLock,
    quickAction: StateFlow<QuickAction?> = MutableStateFlow(null),
    onQuickActionHandled: () -> Unit = {},
) {
    val locked by appLock.locked.collectAsStateWithLifecycle()
    // Nothing behind the lock screen until the first unlock, so the dashboard's intro
    // plays when it can be seen.
    var opened by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(locked) { if (!locked) opened = true }
    val activity = LocalActivity.current

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (opened) {
            // While locked, screen readers mustn't read the content underneath.
            AppNavHost(
                onLock = appLock::lockNow,
                quickAction = quickAction,
                onQuickActionHandled = onQuickActionHandled,
                modifier = if (locked) Modifier.clearAndSetSemantics {} else Modifier,
            )
        }
        AnimatedVisibility(visible = locked, enter = fadeIn(), exit = fadeOut(tween(300))) {
            // Back leaves the app instead of going back in the screens underneath.
            BackHandler { activity?.moveTaskToBack(true) }
            Box(Modifier.fillMaxSize().blockTouches()) {
                LoginScreen(onUnlocked = appLock::unlock)
            }
        }
    }
}

/** Swallows every touch so nothing underneath can be used. */
private fun Modifier.blockTouches() = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) awaitPointerEvent().changes.forEach { it.consume() }
    }
}

@Composable
private fun AppNavHost(
    onLock: () -> Unit,
    quickAction: StateFlow<QuickAction?>,
    onQuickActionHandled: () -> Unit,
    modifier: Modifier,
) {
    val navController = rememberNavController()
    // A widget button opens the form on top of the dashboard. While locked this happens
    // underneath the lock screen, so the form is there once the user unlocks.
    val action by quickAction.collectAsStateWithLifecycle()
    LaunchedEffect(action) {
        val pending = action ?: return@LaunchedEffect
        navController.navigate(TransactionFormRoute(isIncome = pending == QuickAction.ADD_INCOME)) {
            popUpTo<DashboardRoute>()
        }
        onQuickActionHandled()
    }
    NavHost(
        navController = navController,
        startDestination = DashboardRoute,
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        enterTransition = {
            slideInHorizontally(tween(400, easing = EmphasizedDecelerate)) { it / 4 } + fadeIn(tween(300))
        },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(300)) },
        popExitTransition = {
            slideOutHorizontally(tween(300)) { it / 4 } + fadeOut(tween(200))
        },
    ) {
        composable<DashboardRoute>(enterTransition = { fadeIn(tween(400)) }) {
            DashboardScreen(
                viewModel = viewModel(factory = viewModelFactory { initializer { DashboardViewModel(repository()) } }),
                onAddIncome = { navController.navigate(TransactionFormRoute(isIncome = true)) },
                onAddExpense = { navController.navigate(TransactionFormRoute(isIncome = false)) },
                onOpenReminders = { navController.navigate(RemindersRoute) },
                onAddReminder = { navController.navigate(ReminderFormRoute()) },
                onOpenStats = { navController.navigate(StatsRoute) },
                onOpenBudgets = { navController.navigate(BudgetsRoute) },
                onEditTransaction = { navController.navigate(it.editRoute()) },
                onOpenTransactions = { navController.navigate(TransactionsRoute()) },
                onOpenCategories = { navController.navigate(CategoriesRoute) },
                onOpenGoals = { navController.navigate(GoalsRoute) },
                onOpenExport = { navController.navigate(ExportRoute) },
                onLock = onLock,
                onOpenGoal = { navController.navigate(GoalRoute(it)) },
                onAddGoal = { navController.navigate(GoalFormRoute()) },
            )
        }
        composable<TransactionFormRoute> { entry ->
            val route = entry.toRoute<TransactionFormRoute>()
            val type = if (route.isIncome) TransactionType.INCOME else TransactionType.EXPENSE
            val id = route.id.takeIf { it != TransactionFormRoute.NEW }
            TransactionFormScreen(
                viewModel = viewModel(factory = viewModelFactory { initializer { TransactionFormViewModel(type, repository(), id) } }),
                // Back to wherever the form was opened from (dashboard or the transaction list).
                onBack = { if (navController.currentDestination?.hasRoute<TransactionFormRoute>() == true) navController.popBackStack() },
            )
        }
        composable<TransactionsRoute> { entry ->
            val filter = entry.toRoute<TransactionsRoute>().filter()
            TransactionsScreen(
                viewModel = viewModel(factory = viewModelFactory { initializer { TransactionsViewModel(repository(), filter) } }),
                // Back to the dashboard or to the statistics it was opened from.
                onBack = { if (navController.currentDestination?.hasRoute<TransactionsRoute>() == true) navController.popBackStack() },
                onEdit = { navController.navigate(it.editRoute()) },
            )
        }
        composable<CategoriesRoute> {
            CategoriesScreen(
                viewModel = viewModel(factory = viewModelFactory { initializer { CategoriesViewModel(repository()) } }),
                onBack = { navController.popBackStack(DashboardRoute, inclusive = false) },
            )
        }
        composable<ExportRoute> {
            ExportScreen(
                viewModel = viewModel(factory = viewModelFactory { initializer { ExportViewModel(repository()) } }),
                onBack = { navController.popBackStack(DashboardRoute, inclusive = false) },
            )
        }
        composable<GoalsRoute> {
            GoalsScreen(
                viewModel = viewModel(factory = viewModelFactory { initializer { GoalsViewModel(repository()) } }),
                onBack = { navController.popBackStack(DashboardRoute, inclusive = false) },
                onOpen = { navController.navigate(GoalRoute(it)) },
                onAdd = { navController.navigate(GoalFormRoute()) },
            )
        }
        composable<GoalRoute> { entry ->
            val id = entry.toRoute<GoalRoute>().id
            GoalDetailScreen(
                viewModel = viewModel(factory = viewModelFactory { initializer { GoalDetailViewModel(id, repository()) } }),
                onBack = { if (navController.currentDestination?.hasRoute<GoalRoute>() == true) navController.popBackStack() },
                onEdit = { navController.navigate(GoalFormRoute(id)) },
            )
        }
        composable<GoalFormRoute> { entry ->
            val id = entry.toRoute<GoalFormRoute>().id.takeIf { it != GoalFormRoute.NEW }
            GoalFormScreen(
                viewModel = viewModel(factory = viewModelFactory { initializer { GoalFormViewModel(id, repository()) } }),
                onBack = { if (navController.currentDestination?.hasRoute<GoalFormRoute>() == true) navController.popBackStack() },
            )
        }
        composable<BudgetsRoute> {
            BudgetsScreen(
                viewModel = viewModel(factory = viewModelFactory { initializer { BudgetsViewModel(repository()) } }),
                onBack = { navController.popBackStack(DashboardRoute, inclusive = false) },
            )
        }
        composable<StatsRoute> {
            StatsScreen(
                viewModel = viewModel(factory = viewModelFactory { initializer { StatsViewModel(repository()) } }),
                onBack = { navController.popBackStack(DashboardRoute, inclusive = false) },
                onOpenCategory = { category, type, period ->
                    val dates = period.dates
                    navController.navigate(
                        TransactionsRoute(category, type.name, dates?.start?.toString(), dates?.endInclusive?.toString()),
                    )
                },
            )
        }
        composable<RemindersRoute> {
            RemindersScreen(
                viewModel = viewModel(factory = viewModelFactory { initializer { RemindersViewModel(repository()) } }),
                onBack = { navController.popBackStack(DashboardRoute, inclusive = false) },
                onAdd = { navController.navigate(ReminderFormRoute()) },
                onEdit = { navController.navigate(ReminderFormRoute(it.id)) },
            )
        }
        composable<ReminderFormRoute> { entry ->
            val id = entry.toRoute<ReminderFormRoute>().id.takeIf { it != ReminderFormRoute.NEW }
            ReminderFormScreen(
                viewModel = viewModel(factory = viewModelFactory { initializer { ReminderFormViewModel(id, repository()) } }),
                onBack = { if (navController.currentDestination?.hasRoute<ReminderFormRoute>() == true) navController.popBackStack() },
            )
        }
    }
}
