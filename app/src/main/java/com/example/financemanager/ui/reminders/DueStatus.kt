package com.example.financemanager.ui.reminders

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** How a reminder's next due date relates to today. */
sealed interface DueStatus {
    data class Overdue(val days: Int) : DueStatus
    data object Today : DueStatus
    data object Tomorrow : DueStatus
    data class InDays(val days: Int) : DueStatus

    /** Today, tomorrow or overdue: worth a notification and a highlight. */
    val isUrgent: Boolean get() = this !is InDays

    companion object {
        fun of(due: LocalDate, today: LocalDate): DueStatus {
            val days = ChronoUnit.DAYS.between(today, due).toInt()
            return when {
                days < 0 -> Overdue(-days)
                days == 0 -> Today
                days == 1 -> Tomorrow
                else -> InDays(days)
            }
        }
    }
}
