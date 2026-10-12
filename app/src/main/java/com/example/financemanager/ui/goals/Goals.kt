package com.example.financemanager.ui.goals

import com.example.financemanager.data.GoalWithSaved
import com.example.financemanager.data.SavingsGoal
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

enum class GoalStatus {
    /** Saved at least the target. */
    REACHED,

    /** The deadline has passed without reaching the target. */
    OVERDUE,

    /** Still saving, with or without a deadline. */
    SAVING,
}

/** A goal and how far along it is on [today]. */
data class GoalProgress(val goal: SavingsGoal, val saved: Double, val today: LocalDate) {
    val remaining: Double get() = (goal.target - saved).coerceAtLeast(0.0)

    /** Saved share of the target, 0..1. */
    val fraction: Double get() = if (goal.target > 0) (saved / goal.target).coerceIn(0.0, 1.0) else 0.0

    val status: GoalStatus
        get() = when {
            saved >= goal.target -> GoalStatus.REACHED
            goal.deadline != null && goal.deadline.isBefore(today) -> GoalStatus.OVERDUE
            else -> GoalStatus.SAVING
        }

    /**
     * Months left to save in, counting the current one and stopping before the deadline's
     * month (money for a June deadline has to be there by June). At least 1.
     */
    val monthsLeft: Int?
        get() = goal.deadline?.let { deadline ->
            ChronoUnit.MONTHS.between(YearMonth.from(today), YearMonth.from(deadline)).toInt().coerceAtLeast(1)
        }

    /** How much to put aside each month to make the deadline, or null without one (or when done). */
    val monthlyNeeded: Double?
        get() = monthsLeft?.takeIf { status == GoalStatus.SAVING }?.let { remaining / it }
}

fun GoalWithSaved.progress(today: LocalDate) = GoalProgress(goal, saved, today)

/** Totals over all goals for the dashboard. */
data class GoalsSummary(val saved: Double, val target: Double, val count: Int)

fun goalsSummary(goals: List<GoalProgress>): GoalsSummary? = goals.takeIf { it.isNotEmpty() }?.let { list ->
    GoalsSummary(list.sumOf { it.saved }, list.sumOf { it.goal.target }, list.size)
}
