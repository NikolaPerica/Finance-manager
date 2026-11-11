package com.example.financemanager.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import com.example.financemanager.MainActivity
import com.example.financemanager.R
import com.example.financemanager.di.appEntryPoint
import com.example.financemanager.domain.MoneyFormat
import com.example.financemanager.domain.habits.WeekSummary
import com.example.financemanager.domain.habits.weekStart
import com.example.financemanager.domain.habits.weekSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A short look back at the week, on Sunday evening: what was spent, on what most, and how
 * that compares with the weeks before. A summary missed while the phone was off follows when
 * it is back on, as long as it is still fresh. Each week is summed up once.
 */
object WeeklySummary {
    val RUN_AT: LocalTime = LocalTime.of(20, 0)

    /** How long after Sunday evening a missed summary is still worth showing. */
    private const val CATCH_UP_DAYS = 2L

    private const val PREFS = "weekly_summary"
    private const val KEY_LAST_WEEK = "last_week"

    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** The latest Sunday evening that has passed at [now]. */
    private fun lastRun(now: LocalDateTime): LocalDateTime {
        val thisSunday = now.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(RUN_AT)
        return if (now.isBefore(thisSunday)) thisSunday.minusWeeks(1) else thisSunday
    }

    /** The Monday of the week to sum up at [now]: the one that ended on the latest Sunday evening. */
    fun weekToSummarize(now: LocalDateTime): LocalDate = weekStart(lastRun(now).toLocalDate())

    /** When to run next: now if a recent summary was missed, otherwise the coming Sunday evening. */
    fun nextRun(now: LocalDateTime, lastWeek: LocalDate?): LocalDateTime {
        val last = lastRun(now)
        val missed = lastWeek == null || lastWeek.isBefore(weekStart(last.toLocalDate()))
        return if (missed && now.isBefore(last.plusDays(CATCH_UP_DAYS))) now else last.plusWeeks(1)
    }

    /** Sets the alarm for the next summary, or clears it when summaries are off. */
    fun schedule(context: Context, onDone: () -> Unit = {}) {
        val app = context.applicationContext
        scope.launch {
            try {
                scheduleNow(app)
            } finally {
                onDone()
            }
        }
    }

    private suspend fun scheduleNow(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        if (!context.appEntryPoint().settings().settings.first().weeklySummary) {
            alarms.cancel(alarmIntent(context))
            return
        }
        val next = nextRun(LocalDateTime.now(), lastWeek(context))
        val millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // Inexact but allowed in Doze, and needs no special alarm permission.
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, alarmIntent(context))
    }

    /** Sums up the week if that is due, then schedules the next one. */
    fun run(context: Context, onDone: () -> Unit = {}) {
        val app = context.applicationContext
        scope.launch {
            try {
                mutex.withLock { summarize(app) }
            } finally {
                scheduleNow(app)
                onDone()
            }
        }
    }

    private suspend fun summarize(context: Context) {
        if (!context.appEntryPoint().settings().settings.first().weeklySummary) return
        val week = weekToSummarize(LocalDateTime.now())
        if (lastWeek(context) == week) return
        // Recorded even when nothing can be shown, so the alarm doesn't fire in a loop.
        prefs(context).edit { putString(KEY_LAST_WEEK, week.toString()) }

        val transactions = context.appEntryPoint().repository().transactions().first()
        // Nothing recorded by the end of the week: there is nothing to sum up yet.
        if (transactions.none { it.date.isBefore(week.plusWeeks(1)) }) return
        WeeklySummaryNotifier(context).notify(weekSummary(transactions, week))
    }

    private fun lastWeek(context: Context) = prefs(context).getString(KEY_LAST_WEEK, null)?.let(LocalDate::parse)

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        1,
        Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_WEEKLY_SUMMARY),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

/** Posts the weekly summary. */
class WeeklySummaryNotifier(private val context: Context) {

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.weekly_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.weekly_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun notify(week: WeekSummary) {
        if (!ReminderNotifier(context).canNotify()) return
        val text = text(week)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.weekly_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission was revoked between the check and the post; nothing to do.
        }
    }

    internal fun text(week: WeekSummary): String {
        val top = week.topCategory ?: return context.getString(R.string.weekly_nothing_spent)
        val spent = context.getString(
            R.string.weekly_spent,
            MoneyFormat.format(week.spent),
            top.first.ifBlank { context.getString(R.string.no_category) },
            MoneyFormat.format(top.second),
        )
        val change = week.change ?: return spent
        val percent = (abs(change) * 100).roundToInt()
        val compared = when {
            percent < 5 -> context.getString(R.string.weekly_as_usual)
            change > 0 -> context.getString(R.string.weekly_more, percent)
            else -> context.getString(R.string.weekly_less, percent)
        }
        return "$spent\n$compared"
    }

    private companion object {
        const val CHANNEL_ID = "weekly_summary"
        const val NOTIFICATION_ID = Int.MAX_VALUE - 2
    }
}
