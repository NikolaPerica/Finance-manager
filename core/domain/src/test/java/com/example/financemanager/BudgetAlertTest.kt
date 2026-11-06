package com.example.financemanager

import com.example.financemanager.domain.BudgetMonths
import com.example.financemanager.domain.budgets.BudgetAlert
import com.example.financemanager.domain.budgets.BudgetLevel
import com.example.financemanager.domain.budgets.budgetAlertFor
import com.example.financemanager.model.Category
import com.example.financemanager.model.Transaction
import com.example.financemanager.model.TransactionType.EXPENSE
import com.example.financemanager.model.TransactionType.INCOME
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BudgetAlertTest {

    private val today = LocalDate.of(2026, 11, 20)
    private val categories = listOf(Category(1, "Hrana", EXPENSE, monthlyBudget = 100.0), Category(2, "Gorivo", EXPENSE))
    private val spent = listOf(Transaction(1, 70.0, today.minusDays(3), "", "Hrana", EXPENSE))

    private fun expense(amount: Double, category: String = "Hrana", date: LocalDate = today) =
        Transaction(9, amount, date, "", category, EXPENSE)

    @Test
    fun crossingAThresholdThisMonthRaisesAnAlert() {
        assertEquals(BudgetAlert("Hrana", BudgetLevel.WARNING, 85.0, 100.0), budgetAlertFor(expense(15.0), spent, categories, today))
        assertEquals(BudgetAlert("Hrana", BudgetLevel.EXCEEDED, 110.0, 100.0), budgetAlertFor(expense(40.0), spent, categories, today))
    }

    @Test
    fun nothingToSayOtherwise() {
        // Stays under 80 %.
        assertNull(budgetAlertFor(expense(5.0), spent, categories, today))
        // No budget, an income, or another month.
        assertNull(budgetAlertFor(expense(500.0, category = "Gorivo"), spent, categories, today))
        assertNull(budgetAlertFor(expense(40.0).copy(type = INCOME), spent, categories, today))
        assertNull(budgetAlertFor(expense(40.0, date = today.minusMonths(1)), spent, categories, today))
    }

    @Test
    fun theMonthStartsOnTheChosenDay() {
        // With months starting on the 25th, the 70 € from the 17th belong to the previous month.
        assertNull(budgetAlertFor(expense(40.0, date = LocalDate.of(2026, 11, 26)), spent, categories, LocalDate.of(2026, 11, 26), BudgetMonths(25)))
    }
}
