package com.example.financemanager

import com.example.financemanager.domain.habits.dailySpending
import com.example.financemanager.domain.habits.noSpendDays
import com.example.financemanager.domain.habits.noSpendStreak
import com.example.financemanager.domain.habits.weekStart
import com.example.financemanager.domain.habits.weekSummary
import com.example.financemanager.model.Transaction
import com.example.financemanager.model.TransactionType.EXPENSE
import com.example.financemanager.model.TransactionType.INCOME
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class HabitsTest {

    private var id = 1L
    private fun spend(amount: Double, date: LocalDate, category: String = "Hrana") = Transaction(id++, amount, date, "", category, EXPENSE)
    private fun earn(amount: Double, date: LocalDate) = Transaction(id++, amount, date, "", "Plaća", INCOME)
    private fun nov(day: Int) = LocalDate.of(2026, 11, day)

    @Test
    fun eachDayAddsUpItsExpenses() {
        val transactions = listOf(spend(10.0, nov(3)), spend(5.5, nov(3)), spend(20.0, nov(4)), earn(1000.0, nov(3)), spend(7.0, LocalDate.of(2026, 10, 31)))
        assertEquals(mapOf(nov(3) to 15.5, nov(4) to 20.0), dailySpending(transactions, YearMonth.of(2026, 11)))
    }

    @Test
    fun theStreakCountsBackFromToday() {
        val transactions = listOf(earn(1000.0, nov(1)), spend(10.0, nov(5)), spend(3.0, nov(8)))
        // 9th, 10th and today the 11th: nothing spent.
        assertEquals(3, noSpendStreak(transactions, nov(11)))
        // Spending today ends it.
        assertEquals(0, noSpendStreak(transactions + spend(2.0, nov(11)), nov(11)))
        // Income doesn't break it.
        assertEquals(3, noSpendStreak(transactions + earn(50.0, nov(10)), nov(11)))
    }

    @Test
    fun daysBeforeTheFirstRecordDontCount() {
        assertEquals(3, noSpendStreak(listOf(earn(1000.0, nov(9))), nov(11)))
        assertEquals(0, noSpendStreak(emptyList(), nov(11)))
    }

    @Test
    fun noSpendDaysOfTheMonthSoFar() {
        val transactions = listOf(spend(10.0, LocalDate.of(2026, 10, 20)), spend(5.0, nov(2)), spend(5.0, nov(2)), spend(8.0, nov(6)))
        // 1 to 11 November, less the 2nd and the 6th.
        assertEquals(9, noSpendDays(transactions, YearMonth.of(2026, 11), nov(11)))
        // A month the records start in only counts from their first day.
        assertEquals(10, noSpendDays(listOf(spend(1.0, nov(20))), YearMonth.of(2026, 11), nov(30)))
    }

    @Test
    fun weeksRunMondayToSunday() {
        assertEquals(nov(9), weekStart(nov(9)))
        assertEquals(nov(9), weekStart(nov(15)))
        assertEquals(nov(16), weekStart(nov(16)))
    }

    @Test
    fun aWeekComparedWithTheOnesBefore() {
        val transactions = listOf(
            // Four earlier weeks of 100 €, 100 €, 60 € and 140 €.
            spend(100.0, LocalDate.of(2026, 10, 12)),
            spend(100.0, LocalDate.of(2026, 10, 19)),
            spend(60.0, LocalDate.of(2026, 10, 26)),
            spend(140.0, nov(2)),
            // This week: 120 €, most of it on food.
            spend(80.0, nov(9)),
            spend(40.0, nov(15), category = "Gorivo"),
            earn(1500.0, nov(10)),
            // Next week doesn't count.
            spend(500.0, nov(16)),
        )
        val week = weekSummary(transactions, nov(9))
        assertEquals(120.0, week.spent, 0.001)
        assertEquals("Hrana" to 80.0, week.topCategory)
        assertEquals(100.0, week.usual!!, 0.001)
        assertEquals(0.2, week.change!!, 0.001)
    }

    @Test
    fun onlyFullyRecordedWeeksAreCompared() {
        // Records start on a Wednesday: that week isn't a fair comparison.
        val week = weekSummary(listOf(spend(30.0, LocalDate.of(2026, 11, 4)), spend(50.0, nov(10))), nov(9))
        assertNull(week.usual)
        assertNull(week.change)

        val quiet = weekSummary(listOf(spend(30.0, nov(2))), nov(9))
        assertEquals(0.0, quiet.spent, 0.0)
        assertNull(quiet.topCategory)
        assertEquals(-1.0, quiet.change!!, 0.001)
    }
}
