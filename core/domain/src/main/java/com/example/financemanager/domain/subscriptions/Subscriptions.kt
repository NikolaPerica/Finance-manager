package com.example.financemanager.domain.subscriptions

import com.example.financemanager.domain.transactions.normalize
import com.example.financemanager.model.Reminder
import com.example.financemanager.model.Transaction
import com.example.financemanager.model.TransactionType
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/*
 * Subscriptions and other monthly costs, found in the transactions themselves: the same
 * expense, about the same amount, once a month. Groceries or fuel come up every month too, but
 * several times a month and at different amounts, so they don't count.
 */

/** How many monthly payments in a row make a subscription. */
const val MIN_PAYMENTS = 3

/** Days between two monthly payments; months differ in length and payments shift a little. */
private val MONTHLY_GAP = 25..35

/** How much the amount may vary between payments, e.g. after a price change. */
private const val AMOUNT_TOLERANCE = 0.15

/** With no payment for this long, a subscription counts as cancelled. */
private const val ACTIVE_DAYS = 40L

data class Subscription(
    /** As the latest payment was written: its note, or its category without one. */
    val name: String,
    val category: String,
    /** The latest payment; the next one is expected to be the same. */
    val amount: Double,
    val lastPaid: LocalDate,
    val payments: Int,
    /** Whether a payment reminder already covers it. */
    val hasReminder: Boolean,
) {
    val yearly: Double get() = amount * 12
    val nextExpected: LocalDate get() = lastPaid.plusMonths(1)
}

/** Active monthly costs, the most expensive first. */
fun findSubscriptions(transactions: List<Transaction>, reminders: List<Reminder>, today: LocalDate): List<Subscription> =
    transactions
        .filter { it.type == TransactionType.EXPENSE && !it.date.isAfter(today) }
        .groupBy { key(it.note.ifBlank { it.category }) }
        .filterKeys { it.isNotBlank() }
        .values
        .mapNotNull { payments -> subscription(payments.sortedBy { it.date }, reminders, today) }
        .sortedByDescending { it.amount }

private fun subscription(payments: List<Transaction>, reminders: List<Reminder>, today: LocalDate): Subscription? {
    val latest = payments.last()
    if (ChronoUnit.DAYS.between(latest.date, today) > ACTIVE_DAYS) return null
    // The run of monthly payments that ends with the latest one; something paid twice in one
    // month, or at a very different amount, breaks it.
    val run = mutableListOf(latest)
    for (payment in payments.dropLast(1).asReversed()) {
        val next = run.last()
        val gap = ChronoUnit.DAYS.between(payment.date, next.date).toInt()
        if (gap !in MONTHLY_GAP || !similar(payment.amount, next.amount)) break
        run += payment
    }
    if (run.size < MIN_PAYMENTS) return null
    // Anything else in between, e.g. a second coffee the same month, means it isn't monthly.
    val from = run.last().date
    if (payments.count { !it.date.isBefore(from) } != run.size) return null

    val name = latest.note.ifBlank { latest.category }.trim()
    return Subscription(
        name = name,
        category = latest.category,
        amount = latest.amount,
        lastPaid = latest.date,
        payments = run.size,
        hasReminder = reminders.any { reminder ->
            (key(reminder.name) == key(name) || key(reminder.note) == key(name)) && similar(reminder.amount, latest.amount)
        },
    )
}

private fun key(text: String) = normalize(text.trim())

private fun similar(a: Double, b: Double) = kotlin.math.abs(a - b) <= maxOf(a, b) * AMOUNT_TOLERANCE
