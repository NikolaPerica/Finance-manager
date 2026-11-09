package com.example.financemanager

import com.example.financemanager.domain.BudgetMonths
import com.example.financemanager.domain.budgets.safeToSpend
import com.example.financemanager.model.Category
import com.example.financemanager.model.PaymentPeriod
import com.example.financemanager.model.Reminder
import com.example.financemanager.model.Transaction
import com.example.financemanager.model.TransactionType.EXPENSE
import com.example.financemanager.model.TransactionType.INCOME
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SafeToSpendTest {

    // 21 November: ten days left, today included.
    private val today = LocalDate.of(2026, 11, 21)
    private val categories = listOf(
        Category(1, "Hrana", EXPENSE, monthlyBudget = 300.0),
        Category(2, "Izlasci", EXPENSE, monthlyBudget = 100.0),
        Category(3, "Stanarina", EXPENSE),
        Category(4, "Plaća", INCOME),
    )
    private var id = 1L

    private fun spend(amount: Double, category: String, date: LocalDate = today.minusDays(5)) =
        Transaction(id++, amount, date, "", category, EXPENSE)

    @Test
    fun whatIsLeftIsSharedOverTheDaysLeft() {
        val transactions = listOf(
            spend(150.0, "Hrana"),
            spend(50.0, "Izlasci"),
            // No budget, so it doesn't count; nor does income or last month.
            spend(500.0, "Stanarina"),
            Transaction(id++, 2000.0, today.minusDays(20), "", "Plaća", INCOME),
            spend(90.0, "Hrana", date = today.minusMonths(1)),
        )
        val safe = safeToSpend(categories, transactions, emptyList(), today)!!
        assertEquals(10, safe.daysLeft)
        // 400 € of budgets, 200 € spent: 20 € a day.
        assertEquals(20.0, safe.dailyShare, 0.001)
        assertEquals(20.0, safe.today, 0.001)
        assertEquals(200.0, safe.leftThisMonth, 0.001)
    }

    @Test
    fun todaysSpendingComesOffTodaysShare() {
        val transactions = listOf(spend(200.0, "Hrana"), spend(12.5, "Izlasci", date = today))
        val safe = safeToSpend(categories, transactions, emptyList(), today)!!
        assertEquals(20.0, safe.dailyShare, 0.001)
        assertEquals(7.5, safe.today, 0.001)

        // Spending more than today's share goes below zero, and tomorrow's share shrinks.
        val more = safeToSpend(categories, transactions + spend(30.0, "Hrana", date = today), emptyList(), today)!!
        assertEquals(-22.5, more.today, 0.001)
        val tomorrow = safeToSpend(categories, transactions + spend(30.0, "Hrana", date = today), emptyList(), today.plusDays(1))!!
        assertEquals((400.0 - 242.5) / 9, tomorrow.dailyShare, 0.001)
    }

    @Test
    fun paymentsStillDueAreSetAside() {
        val reminders = listOf(
            // Due this month in a budgeted category: set aside.
            Reminder(1, "Dostava", 30.0, PaymentPeriod.MONTHLY, LocalDate.of(2026, 11, 25), category = "Hrana"),
            // Overdue: still to pay, so also set aside.
            Reminder(2, "Kino", 20.0, PaymentPeriod.ONCE, LocalDate.of(2026, 11, 10), category = "Izlasci"),
            // Next month, or a category without a budget: not.
            Reminder(3, "Teretana", 40.0, PaymentPeriod.MONTHLY, LocalDate.of(2026, 12, 2), category = "Hrana"),
            Reminder(4, "Najam", 500.0, PaymentPeriod.MONTHLY, LocalDate.of(2026, 11, 28), category = "Stanarina"),
        )
        val safe = safeToSpend(categories, listOf(spend(200.0, "Hrana")), reminders, today)!!
        assertEquals(50.0, safe.stillDue, 0.001)
        assertEquals(15.0, safe.dailyShare, 0.001)
        assertEquals(150.0, safe.leftThisMonth, 0.001)
    }

    @Test
    fun overBudgetLeavesNothingForToday() {
        val safe = safeToSpend(categories, listOf(spend(450.0, "Hrana")), emptyList(), today)!!
        assertEquals(0.0, safe.dailyShare, 0.001)
        assertEquals(0.0, safe.today, 0.001)
        assertEquals(-50.0, safe.leftThisMonth, 0.001)
    }

    @Test
    fun theLastDayGetsEverythingLeft() {
        val safe = safeToSpend(categories, listOf(spend(250.0, "Hrana")), emptyList(), LocalDate.of(2026, 11, 30))!!
        assertEquals(1, safe.daysLeft)
        assertEquals(150.0, safe.today, 0.001)
    }

    @Test
    fun monthsCanStartOnPayday() {
        // With months starting on the 25th, this month runs from 25 November to 24 December.
        val payday = BudgetMonths(25)
        val transactions = listOf(spend(100.0, "Hrana", date = LocalDate.of(2026, 11, 26)), spend(80.0, "Hrana", date = LocalDate.of(2026, 11, 20)))
        val safe = safeToSpend(categories, transactions, emptyList(), LocalDate.of(2026, 11, 28), payday)!!
        assertEquals(27, safe.daysLeft)
        // Only the 100 € since the 25th count.
        assertEquals(300.0, safe.leftThisMonth, 0.001)
    }

    @Test
    fun withoutBudgetsThereIsNothingToShare() {
        assertNull(safeToSpend(categories.map { it.copy(monthlyBudget = null) }, listOf(spend(10.0, "Hrana")), emptyList(), today))
    }
}
