package com.example.financemanager

import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType.EXPENSE
import com.example.financemanager.data.TransactionType.INCOME
import com.example.financemanager.ui.dashboard.summarize
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DashboardViewModelTest {
    private fun tx(id: Long, amount: Double, day: Int, type: com.example.financemanager.data.TransactionType) =
        Transaction(id, amount, LocalDate.of(2026, 10, day), "", "Test", type)

    @Test
    fun totalsAndBalance() {
        val state = summarize(listOf(tx(1, 1000.0, 1, INCOME), tx(2, 250.5, 2, EXPENSE), tx(3, 49.5, 3, EXPENSE)))
        assertEquals(1000.0, state.income, 0.0)
        assertEquals(300.0, state.expense, 0.0)
        assertEquals(700.0, state.balance, 0.0)
    }

    @Test
    fun newestFirstThenByInsertionOrder() {
        val state = summarize(listOf(tx(1, 1.0, 1, INCOME), tx(2, 1.0, 3, INCOME), tx(3, 1.0, 3, EXPENSE)))
        assertEquals(listOf(3L, 2L, 1L), state.transactions.map { it.id })
    }
}
