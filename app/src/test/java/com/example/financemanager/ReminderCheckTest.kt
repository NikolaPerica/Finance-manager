package com.example.financemanager

import com.example.financemanager.data.PaymentPeriod
import com.example.financemanager.data.Reminder
import com.example.financemanager.notifications.ReminderCheck
import com.example.financemanager.ui.reminders.DueStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ReminderCheckTest {

    private val today = LocalDate.of(2026, 10, 8)

    private fun reminder(id: Long, due: LocalDate) =
        Reminder(id = id, name = "R$id", amount = 10.0, period = PaymentPeriod.MONTHLY, firstDueDate = due)

    @Test
    fun beforeNineTheCheckRunsAtNine() {
        val now = today.atTime(7, 30)
        assertEquals(today.atTime(9, 0), ReminderCheck.nextRun(now, lastCheck = today.minusDays(1)))
    }

    @Test
    fun aMissedCheckRunsRightAway() {
        val now = today.atTime(15, 0)
        assertEquals(now, ReminderCheck.nextRun(now, lastCheck = today.minusDays(3)))
        assertEquals(now, ReminderCheck.nextRun(now, lastCheck = null))
    }

    @Test
    fun afterTodaysCheckTheNextOneIsTomorrow() {
        val now = LocalDateTime.of(2026, 10, 8, 15, 0)
        assertEquals(LocalDateTime.of(2026, 10, 9, 9, 0), ReminderCheck.nextRun(now, lastCheck = today))
    }

    @Test
    fun onlyUrgentRemindersNotYetAnnouncedToday() {
        val reminders = listOf(
            reminder(1, today.plusDays(1)),
            reminder(2, today),
            reminder(3, today.minusDays(2)),
            reminder(4, today.plusDays(5)),
            reminder(5, today),
        )
        val due = ReminderCheck.dueForNotification(reminders, today, alreadyNotified = setOf(5))
        assertEquals(listOf(3L, 2L, 1L), due.map { it.first.id })
        assertEquals(listOf(DueStatus.Overdue(2), DueStatus.Today, DueStatus.Tomorrow), due.map { it.second })
    }
}
