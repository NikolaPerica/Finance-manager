package com.example.financemanager.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import com.example.financemanager.FinanceApp
import com.example.financemanager.data.Reminder
import com.example.financemanager.ui.reminders.DueStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Daily check for payments that are due tomorrow, today or overdue.
 *
 * An alarm fires at [NOTIFY_AT]. If that was missed (phone off, app force-stopped),
 * the next app start or reboot catches up. The check also runs right after the user
 * turns notifications on. Each reminder is announced at most once a day.
 */
object ReminderCheck {
    val NOTIFY_AT: LocalTime = LocalTime.of(9, 0)

    private const val PREFS = "reminder_check"
    private const val KEY_LAST_CHECK = "last_check"
    private const val KEY_NOTIFIED_DAY = "notified_day"
    private const val KEY_NOTIFIED_IDS = "notified_ids"

    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** When the next check should run: now if today's was missed, otherwise the next [NOTIFY_AT]. */
    fun nextRun(now: LocalDateTime, lastCheck: LocalDate?): LocalDateTime {
        val todayAt = now.toLocalDate().atTime(NOTIFY_AT)
        return when {
            now.isBefore(todayAt) -> todayAt
            lastCheck == null || lastCheck.isBefore(now.toLocalDate()) -> now
            else -> todayAt.plusDays(1)
        }
    }

    /** Reminders worth a notification today that haven't had one yet, soonest first. */
    fun dueForNotification(
        reminders: List<Reminder>,
        today: LocalDate,
        alreadyNotified: Set<Long>,
    ): List<Pair<Reminder, DueStatus>> = reminders
        .filter { it.id !in alreadyNotified }
        .map { it to DueStatus.of(it.nextDueDate, today) }
        .filter { (_, status) -> status.isUrgent }
        .sortedBy { (reminder, _) -> reminder.nextDueDate }

    /** Sets the alarm for the next check. Safe to call any number of times. */
    fun schedule(context: Context) {
        val lastCheck = prefs(context).getString(KEY_LAST_CHECK, null)?.let(LocalDate::parse)
        val next = nextRun(LocalDateTime.now(), lastCheck)
        val millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // Inexact but allowed in Doze, and needs no special alarm permission.
        context.getSystemService(AlarmManager::class.java)
            .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, alarmIntent(context))
    }

    /** Runs the check now, then schedules the next one. */
    fun run(context: Context, onDone: () -> Unit = {}) {
        val app = context.applicationContext
        scope.launch {
            try {
                mutex.withLock { check(app) }
            } finally {
                schedule(app)
                onDone()
            }
        }
    }

    private suspend fun check(context: Context) {
        val today = LocalDate.now()
        val prefs = prefs(context)
        // Recorded even when nothing can be shown, so a missing permission can't make the alarm fire in a loop.
        prefs.edit { putString(KEY_LAST_CHECK, today.toString()) }

        val notifier = ReminderNotifier(context)
        if (!notifier.canNotify()) return

        val notified = if (prefs.getString(KEY_NOTIFIED_DAY, null) == today.toString()) {
            prefs.getStringSet(KEY_NOTIFIED_IDS, emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()
        } else {
            emptySet()
        }
        val reminders = (context as FinanceApp).repository.reminders().first()
        val due = dueForNotification(reminders, today, notified)
        notifier.notify(due)

        prefs.edit {
            putString(KEY_NOTIFIED_DAY, today.toString())
            putStringSet(KEY_NOTIFIED_IDS, (notified + due.map { it.first.id }).map(Long::toString).toSet())
        }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_CHECK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

/** Runs the daily check when its alarm fires, and re-arms the alarm after a reboot, update or clock change. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_CHECK -> {
                val pending = goAsync()
                ReminderCheck.run(context) { pending.finish() }
            }
            // Alarms are cleared on reboot; schedule() also catches up if today's check was missed.
            else -> ReminderCheck.schedule(context)
        }
    }

    companion object {
        const val ACTION_CHECK = "com.example.financemanager.action.CHECK_REMINDERS"
    }
}
