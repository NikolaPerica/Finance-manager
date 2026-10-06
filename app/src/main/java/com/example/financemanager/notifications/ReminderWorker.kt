package com.example.financemanager.notifications

import android.content.Context
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.financemanager.FinanceApp
import com.example.financemanager.ui.reminders.DueStatus
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Once a day, notifies about payments that are due tomorrow, today or overdue. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        // Periodic work may run more than once in a day; notify only on the first run.
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_LAST_RUN, null) == today.toString()) return Result.success()

        val reminders = (applicationContext as FinanceApp).repository.reminders().first()
        val due = reminders
            .map { it to DueStatus.of(it.nextDueDate, today) }
            .filter { (_, status) -> status.isUrgent }
            .sortedBy { (reminder, _) -> reminder.nextDueDate }
        ReminderNotifier(applicationContext).notify(due)

        prefs.edit { putString(KEY_LAST_RUN, today.toString()) }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "payment_reminders"
        private const val PREFS = "reminder_worker"
        private const val KEY_LAST_RUN = "last_run"
        private val NOTIFY_AT: LocalTime = LocalTime.of(9, 0)

        /** Schedules the daily check, around [NOTIFY_AT]. Safe to call on every app start. */
        fun schedule(context: Context) {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(NOTIFY_AT)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(Duration.between(now, next).toMinutes(), TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
