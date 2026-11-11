package com.example.financemanager

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.financemanager.domain.habits.WeekSummary
import com.example.financemanager.notifications.WeeklySummary
import com.example.financemanager.notifications.WeeklySummaryNotifier
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class WeeklySummaryTest {

    // Monday 9 to Sunday 15 November 2026.
    private val monday = LocalDate.of(2026, 11, 9)
    private val sundayEvening = LocalDateTime.of(2026, 11, 15, 20, 0)

    @Test
    fun runsOnSundayEvening() {
        val wednesday = LocalDateTime.of(2026, 11, 11, 12, 0)
        // Last week was already summed up.
        assertEquals(sundayEvening, WeeklySummary.nextRun(wednesday, lastWeek = LocalDate.of(2026, 11, 2)))
        assertEquals(sundayEvening, WeeklySummary.nextRun(sundayEvening.minusMinutes(1), lastWeek = LocalDate.of(2026, 11, 2)))
        // Once this week is done, the next is a week later.
        assertEquals(sundayEvening.plusWeeks(1), WeeklySummary.nextRun(sundayEvening.plusMinutes(5), lastWeek = monday))
    }

    @Test
    fun aMissedSummaryFollowsSoonAfter() {
        // The phone was off on Sunday evening; on Monday morning it catches up.
        val mondayMorning = LocalDateTime.of(2026, 11, 16, 8, 0)
        assertEquals(mondayMorning, WeeklySummary.nextRun(mondayMorning, lastWeek = LocalDate.of(2026, 11, 2)))
        assertEquals(monday, WeeklySummary.weekToSummarize(mondayMorning))
        // By Wednesday it is old news: wait for the coming Sunday.
        val wednesday = LocalDateTime.of(2026, 11, 18, 8, 0)
        assertEquals(sundayEvening.plusWeeks(1), WeeklySummary.nextRun(wednesday, lastWeek = LocalDate.of(2026, 11, 2)))
    }

    @Test
    fun theWeekEndingOnTheLatestSundayEvening() {
        assertEquals(monday, WeeklySummary.weekToSummarize(sundayEvening))
        // Before Sunday evening it is still the week before.
        assertEquals(monday.minusWeeks(1), WeeklySummary.weekToSummarize(sundayEvening.minusMinutes(1)))
    }

    @Test
    fun theNotificationSaysWhatAndHowItCompares() {
        val notifier = WeeklySummaryNotifier(ApplicationProvider.getApplicationContext())
        assertEquals(
            "Potrošeno 120,00 € · najviše Hrana (80,00 €)\n20 % više nego inače",
            notifier.text(WeekSummary(monday, 120.0, "Hrana" to 80.0, usual = 100.0)),
        )
        assertEquals(
            "Potrošeno 70,00 € · najviše Bez kategorije (70,00 €)\n30 % manje nego inače",
            notifier.text(WeekSummary(monday, 70.0, "" to 70.0, usual = 100.0)),
        )
        assertEquals(
            "Potrošeno 102,00 € · najviše Hrana (102,00 €)\nOtprilike kao inače",
            notifier.text(WeekSummary(monday, 102.0, "Hrana" to 102.0, usual = 100.0)),
        )
        // Without earlier weeks there is nothing to compare with.
        assertEquals("Potrošeno 5,00 € · najviše Hrana (5,00 €)", notifier.text(WeekSummary(monday, 5.0, "Hrana" to 5.0, usual = null)))
        assertEquals("Ovaj tjedan nije bilo rashoda.", notifier.text(WeekSummary(monday, 0.0, null, usual = 50.0)))
    }
}
