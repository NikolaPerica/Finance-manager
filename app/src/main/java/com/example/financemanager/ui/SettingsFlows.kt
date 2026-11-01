package com.example.financemanager.ui

import com.example.financemanager.data.SettingsRepository
import com.example.financemanager.domain.BudgetMonths
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** The months that budgets, statistics and insights use, as set in the settings. */
val SettingsRepository.budgetMonths: Flow<BudgetMonths>
    get() = settings.map { BudgetMonths(it.monthStartDay) }.distinctUntilChanged()
