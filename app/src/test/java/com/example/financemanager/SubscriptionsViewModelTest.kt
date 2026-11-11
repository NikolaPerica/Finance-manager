package com.example.financemanager

import com.example.financemanager.model.PaymentPeriod
import com.example.financemanager.model.Reminder
import com.example.financemanager.model.Transaction
import com.example.financemanager.model.TransactionType
import com.example.financemanager.ui.subscriptions.SubscriptionsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionsViewModelTest {
    private val today = LocalDate.of(2026, 11, 20)
    private val repository = FakeRepository().apply {
        transactions.value = (9..11).flatMap { month ->
            listOf(
                Transaction(month * 10L, 13.99, LocalDate.of(2026, month, 5), "Netflix", "Zabava", TransactionType.EXPENSE),
                Transaction(month * 10L + 1, 35.0, LocalDate.of(2026, month, 1), "Teretana", "Sport", TransactionType.EXPENSE),
            )
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun subscriptionsWithTheirTotals() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = SubscriptionsViewModel(repository) { today }
        backgroundScope.launch { viewModel.state.collect {} }
        val state = viewModel.state.value
        assertEquals(listOf("Teretana", "Netflix"), state.subscriptions.map { it.name })
        assertEquals(48.99, state.monthly, 0.001)
        assertEquals(48.99 * 12, state.yearly, 0.001)
    }

    @Test
    fun oneTurnsIntoAMonthlyReminder() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = SubscriptionsViewModel(repository) { today }
        backgroundScope.launch { viewModel.state.collect {} }
        val netflix = viewModel.state.value.subscriptions.first { it.name == "Netflix" }
        assertFalse(netflix.hasReminder)

        viewModel.addReminder(netflix)
        assertEquals(netflix, viewModel.reminderAdded.first())
        val reminder = repository.reminders.value.single()
        assertEquals(
            Reminder(reminder.id, "Netflix", 13.99, PaymentPeriod.MONTHLY, LocalDate.of(2026, 12, 5), category = "Zabava"),
            reminder,
        )
        assertTrue(viewModel.state.value.subscriptions.first { it.name == "Netflix" }.hasReminder)
    }
}
