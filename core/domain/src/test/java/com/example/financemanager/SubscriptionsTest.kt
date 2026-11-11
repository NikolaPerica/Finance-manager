package com.example.financemanager

import com.example.financemanager.domain.subscriptions.findSubscriptions
import com.example.financemanager.model.PaymentPeriod
import com.example.financemanager.model.Reminder
import com.example.financemanager.model.Transaction
import com.example.financemanager.model.TransactionType.EXPENSE
import com.example.financemanager.model.TransactionType.INCOME
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SubscriptionsTest {

    private val today = LocalDate.of(2026, 11, 20)
    private var id = 1L

    private fun pay(amount: Double, date: LocalDate, note: String = "", category: String = "Zabava") =
        Transaction(id++, amount, date, note, category, EXPENSE)

    /** Paid on the [day] of each of [months] in 2026. */
    private fun monthly(amount: Double, day: Int, months: IntRange, note: String, category: String = "Zabava") =
        months.map { pay(amount, LocalDate.of(2026, it, day), note, category) }

    @Test
    fun theSamePaymentEveryMonthIsASubscription() {
        val netflix = monthly(13.99, 5, 8..11, "Netflix")
        val found = findSubscriptions(netflix, emptyList(), today).single()
        assertEquals("Netflix", found.name)
        assertEquals(13.99, found.amount, 0.0)
        assertEquals(4, found.payments)
        assertEquals(13.99 * 12, found.yearly, 0.001)
        assertEquals(LocalDate.of(2026, 12, 5), found.nextExpected)
        assertFalse(found.hasReminder)
    }

    @Test
    fun aPriceChangeAndAFewDaysEitherWayStillCount() {
        val gym = listOf(
            pay(35.0, LocalDate.of(2026, 8, 1), "Teretana"),
            pay(35.0, LocalDate.of(2026, 9, 3), "teretana"),
            pay(38.0, LocalDate.of(2026, 9, 30), "Teretana "),
            pay(38.0, LocalDate.of(2026, 11, 2), "Teretana"),
        )
        val found = findSubscriptions(gym, emptyList(), today).single()
        assertEquals(38.0, found.amount, 0.0)
        assertEquals(4, found.payments)
    }

    @Test
    fun noteLessPaymentsAreKnownByTheirCategory() {
        val found = findSubscriptions(monthly(9.99, 12, 9..11, note = "", category = "Spotify"), emptyList(), today).single()
        assertEquals("Spotify", found.name)
    }

    @Test
    fun everydaySpendingIsNot() {
        val groceries = (8..11).flatMap { month -> listOf(5, 12, 19).map { pay(40.0 + it, LocalDate.of(2026, month, it), "", "Hrana") } }
        // Twice: too few.
        val twice = monthly(10.0, 1, 10..11, "Disney+")
        // Very different amounts.
        val bills = listOf(60.0, 120.0, 45.0).mapIndexed { i, amount -> pay(amount, LocalDate.of(2026, 9 + i, 10), "Struja") }
        // Income.
        val salary = (8..11).map { Transaction(id++, 2000.0, LocalDate.of(2026, it, 1), "Plaća", "Plaća", INCOME) }
        assertTrue(findSubscriptions(groceries + twice + bills + salary, emptyList(), today).isEmpty())
    }

    @Test
    fun aCancelledOneDropsOff() {
        assertTrue(findSubscriptions(monthly(8.0, 1, 5..9, "HBO"), emptyList(), today).isEmpty())
    }

    @Test
    fun anExtraPaymentInAMonthBreaksTheRun() {
        // Only the last three monthly payments run on; the extra one in September doesn't fit.
        val payments = monthly(5.0, 10, 8..11, "Kava") + pay(5.0, LocalDate.of(2026, 9, 20), "Kava")
        assertTrue(findSubscriptions(payments, emptyList(), today).isEmpty())
    }

    @Test
    fun aReminderAlreadyCoversIt() {
        val reminder = Reminder(1, "Netflix", 13.99, PaymentPeriod.MONTHLY, LocalDate.of(2026, 12, 5), category = "Zabava")
        assertTrue(findSubscriptions(monthly(13.99, 5, 9..11, "netflix"), listOf(reminder), today).single().hasReminder)
    }

    @Test
    fun theMostExpensiveFirst() {
        val payments = monthly(9.99, 12, 9..11, "Spotify") + monthly(35.0, 1, 9..11, "Teretana") + monthly(13.99, 5, 9..11, "Netflix")
        assertEquals(listOf("Teretana", "Netflix", "Spotify"), findSubscriptions(payments, emptyList(), today).map { it.name })
    }
}
