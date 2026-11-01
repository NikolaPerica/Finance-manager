package com.example.financemanager.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * Months that start on [startDay] instead of the 1st, e.g. on payday. Each one is named
 * after the calendar month it starts in: with [startDay] 15, "October" runs from 15 October
 * to 14 November. A [startDay] of 1 gives plain calendar months.
 */
data class BudgetMonths(val startDay: Int = 1) {
    init {
        // Every month has a 28th, so a month never has to start on a day that doesn't exist.
        require(startDay in 1..MAX_START_DAY) { "startDay must be 1..$MAX_START_DAY" }
    }

    val isCalendar: Boolean get() = startDay == 1

    /** The month [date] belongs to. */
    fun of(date: LocalDate): YearMonth = YearMonth.from(date.minusDays(startDay - 1L))

    fun first(month: YearMonth): LocalDate = month.atDay(startDay)

    fun last(month: YearMonth): LocalDate = first(month.plusMonths(1)).minusDays(1)

    fun dates(month: YearMonth): ClosedRange<LocalDate> = first(month)..last(month)

    fun length(month: YearMonth): Int = ChronoUnit.DAYS.between(first(month), first(month.plusMonths(1))).toInt()

    /** Which day of its month [date] is, counting from 1. */
    fun dayOf(date: LocalDate): Int = ChronoUnit.DAYS.between(first(of(date)), date).toInt() + 1

    companion object {
        const val MAX_START_DAY = 28
    }
}
