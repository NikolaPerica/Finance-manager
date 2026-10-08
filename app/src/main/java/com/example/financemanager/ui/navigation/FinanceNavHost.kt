package com.example.financemanager.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import com.example.financemanager.FinanceApp
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import com.example.financemanager.ui.budgets.BudgetsScreen
import com.example.financemanager.ui.budgets.BudgetsViewModel
import com.example.financemanager.ui.categories.CategoriesScreen
import com.example.financemanager.ui.categories.CategoriesViewModel
import com.example.financemanager.ui.components.EmphasizedDecelerate
import com.example.financemanager.ui.dashboard.DashboardScreen
import com.example.financemanager.ui.dashboard.DashboardViewModel
import com.example.financemanager.ui.login.LoginScreen
import com.example.financemanager.ui.reminders.ReminderFormScreen
import com.example.financemanager.ui.reminders.ReminderFormViewModel
import com.example.financemanager.ui.reminders.RemindersScreen
import com.example.financemanager.ui.reminders.RemindersViewModel
import com.example.financemanager.ui.stats.StatsScreen
import com.example.financemanager.ui.stats.StatsViewModel
import com.example.financemanager.ui.transaction.TransactionFormScreen
import com.example.financemanager.ui.transaction.TransactionFormViewModel
import com.example.financemanager.ui.transactions.TransactionsScreen
import com.example.financemanager.ui.transactions.TransactionsViewModel
import kotlinx.serialization.Serializable

@Serializable
data object LoginRoute

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

@Serializable
data object TransactionsRoute

@Serializable
data object CategoriesRoute

/** [id] of the reminder to edit, or [NEW] for a new one. */
@Serializable
data class ReminderFormRoute(val id: Long = NEW) {
    companion object {
        const val NEW = -1L
    }
}

private fun Transaction.editRoute() = TransactionFormRoute(isIncome = type == TransactionType.INCOME, id = id)

private fun CreationExtras.repository(): FinanceRepository = (this[APPLICATION_KEY] as FinanceApp).repository

@Composable
fun FinanceNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = LoginRoute,
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        enterTransition = {
            slideInHorizontally(tween(400, easing = EmphasizedDecelerate)) { it / 4 } + fadeIn(tween(300))
        },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(300)) },
        popExitTransition = {
            slideOutHorizontally(tween(300)) { it / 4 } + fadeOut(tween(200))
        },
    ) {
        composable<LoginRoute>(
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut(tween(300)) },
        ) {
            LoginScreen(onUnlocked = {
                navController.navigate(DashboardRoute) {
                    popUpTo<LoginRoute> { inclusive = true }
                }
            })
        }
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
                onOpenTransactions = { navController.navigate(TransactionsRoute) },
                onOpenCategories = { navController.navigate(CategoriesRoute) },
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
        composable<TransactionsRoute> {
            TransactionsScreen(
                viewModel = viewModel(factory = viewModelFactory { initializer { TransactionsViewModel(repository()) } }),
                onBack = { navController.popBackStack(DashboardRoute, inclusive = false) },
                onEdit = { navController.navigate(it.editRoute()) },
            )
        }
        composable<CategoriesRoute> {
            CategoriesScreen(
                viewModel = viewModel(factory = viewModelFactory { initializer { CategoriesViewModel(repository()) } }),
                onBack = { navController.popBackStack(DashboardRoute, inclusive = false) },
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
