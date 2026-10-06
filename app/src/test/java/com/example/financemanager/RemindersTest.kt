package com.example.financemanager

import com.example.financemanager.data.Category
import com.example.financemanager.data.PaymentPeriod
import com.example.financemanager.data.Reminder
import com.example.financemanager.data.TransactionType
import com.example.financemanager.data.toExpense
import com.example.financemanager.ui.components.CategoryNameError
import com.example.financemanager.ui.reminders.DueStatus
import com.example.financemanager.ui.reminders.ReminderFormViewModel
import com.example.financemanager.ui.reminders.RemindersEvent
import com.example.financemanager.ui.reminders.RemindersViewModel
import com.example.financemanager.ui.reminders.summarizeReminders
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class RemindersTest {
    private val today = LocalDate.of(2026, 10, 6)
    private val repository = FakeRepository()

    private fun reminder(
        id: Long = 1,
        period: PaymentPeriod = PaymentPeriod.MONTHLY,
        first: LocalDate = today,
        paid: Int = 0,
        amount: Double = 100.0,
        category: String = "",
    ) = Reminder(id, "Stanarina", amount, period, first, paid, category, "")

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun monthlyScheduleKeepsTheDayOfMonth() {
        val r = reminder(first = LocalDate.of(2026, 1, 31))
        assertEquals(LocalDate.of(2026, 2, 28), r.copy(paidCount = 1).nextDueDate)
        // No drift to the 28th after a short month.
        assertEquals(LocalDate.of(2026, 3, 31), r.copy(paidCount = 2).nextDueDate)
        assertEquals(LocalDate.of(2027, 1, 31), reminder(period = PaymentPeriod.YEARLY, first = LocalDate.of(2026, 1, 31), paid = 1).nextDueDate)
        assertEquals(LocalDate.of(2026, 7, 31), reminder(period = PaymentPeriod.SEMI_ANNUALLY, first = LocalDate.of(2026, 1, 31), paid = 1).nextDueDate)
    }

    @Test
    fun dueStatus() {
        assertEquals(DueStatus.Overdue(3), DueStatus.of(today.minusDays(3), today))
        assertEquals(DueStatus.Today, DueStatus.of(today, today))
        assertEquals(DueStatus.Tomorrow, DueStatus.of(today.plusDays(1), today))
        assertEquals(DueStatus.InDays(30), DueStatus.of(today.plusDays(30), today))
        assertTrue(DueStatus.Tomorrow.isUrgent)
        assertFalse(DueStatus.InDays(2).isUrgent)
    }

    @Test
    fun paymentBecomesAnExpenseInTheReminderCategory() {
        val withCategory = reminder(category = "Stanovanje").toExpense(today)
        assertEquals("Stanovanje", withCategory.category)
        assertEquals("Stanarina", withCategory.note)
        assertEquals(TransactionType.EXPENSE, withCategory.type)
        assertEquals("Stanarina", reminder().toExpense(today).category)
    }

    @Test
    fun monthTotalIncludesOverdueButNotNextMonth() {
        val state = summarizeReminders(
            listOf(
                reminder(id = 1, first = today.plusDays(10), amount = 50.0),
                reminder(id = 2, first = today.minusDays(40), amount = 20.0, period = PaymentPeriod.ONCE),
                reminder(id = 3, first = LocalDate.of(2026, 11, 1), amount = 999.0),
            ),
            today,
        )
        assertEquals(70.0, state.dueThisMonth, 0.0)
        assertEquals(listOf(2L, 1L, 3L), state.reminders.map { it.id })
    }

    @Test
    fun payingMovesToTheNextDateAndCanBeUndone() = runTest {
        repository.reminders.value = listOf(reminder())
        val vm = RemindersViewModel(repository) { today }

        vm.pay(repository.reminders.value.single())
        val paid = vm.events.first() as RemindersEvent.Paid
        assertEquals(LocalDate.of(2026, 11, 6), paid.next!!.nextDueDate)
        assertEquals(100.0, repository.transactions.value.single().amount, 0.0)

        vm.undoPayment(paid.payment)
        assertTrue(repository.transactions.value.isEmpty())
        assertEquals(0, repository.reminders.value.single().paidCount)
    }

    @Test
    fun payingAOneOffReminderFinishesIt() = runTest {
        repository.reminders.value = listOf(reminder(period = PaymentPeriod.ONCE))
        val vm = RemindersViewModel(repository) { today }
        vm.pay(repository.reminders.value.single())
        assertNull((vm.events.first() as RemindersEvent.Paid).next)
        assertTrue(repository.reminders.value.isEmpty())
    }

    @Test
    fun formValidatesAndSaves() = runTest {
        val vm = ReminderFormViewModel(null, repository)
        vm.save()
        assertTrue(vm.state.value.nameError)
        assertTrue(vm.state.value.amountError)

        vm.onNameChange(" Netflix ")
        vm.onAmountChange("12,99")
        vm.onPeriodChange(PaymentPeriod.MONTHLY)
        vm.onDueDateChange(LocalDate.of(2026, 10, 20))
        vm.save()
        vm.done.first()

        val saved = repository.reminders.value.single()
        assertEquals("Netflix", saved.name)
        assertEquals(12.99, saved.amount, 0.0)
        assertEquals(LocalDate.of(2026, 10, 20), saved.nextDueDate)
    }

    @Test
    fun editingStartsFromTheNextDueDate() = runTest {
        repository.reminders.value = listOf(reminder(id = 7, first = LocalDate.of(2026, 1, 15), paid = 9, amount = 520.5))
        val vm = ReminderFormViewModel(7, repository)
        assertEquals("520,5", vm.state.value.amount)
        assertEquals(LocalDate.of(2026, 10, 15), vm.state.value.dueDate)

        vm.onAmountChange("530")
        vm.save()
        vm.done.first()
        val saved = repository.reminders.value.single()
        assertEquals(7L, saved.id)
        assertEquals(530.0, saved.amount, 0.0)
        assertEquals(LocalDate.of(2026, 10, 15), saved.nextDueDate)
    }

    @Test
    fun formCanAddAnExpenseCategory() = runTest(UnconfinedTestDispatcher()) {
        repository.categories.value = listOf(
            Category(1, "Režije", TransactionType.EXPENSE),
            Category(2, "Plaća", TransactionType.INCOME),
        )
        val viewModel = ReminderFormViewModel(null, repository)
        backgroundScope.launch { viewModel.categories.collect {} }

        assertEquals(CategoryNameError.EMPTY, viewModel.validateCategoryName(" "))
        assertEquals(CategoryNameError.EXISTS, viewModel.validateCategoryName("režije"))
        // Only expense categories count, reminders always become expenses.
        assertNull(viewModel.validateCategoryName("Plaća"))

        viewModel.addCategory(" Pretplate ")
        assertEquals("Pretplate", viewModel.state.value.category)
        assertEquals("Pretplate", viewModel.categoryAdded.first())
        assertTrue(viewModel.categories.value.any { it.name == "Pretplate" && it.type == TransactionType.EXPENSE })
    }
}
