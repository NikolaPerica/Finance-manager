package com.example.financemanager.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.financemanager.MainActivity
import com.example.financemanager.R
import com.example.financemanager.data.Reminder
import com.example.financemanager.ui.MoneyFormat
import com.example.financemanager.ui.reminders.DueStatus

/** Posts "payment due" notifications, one per reminder, grouped together. */
class ReminderNotifier(private val context: Context) {

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.reminder_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Whether notifications would actually be shown (permission on Android 13+, and not switched off). */
    fun canNotify(): Boolean {
        val permitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permitted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun notify(due: List<Pair<Reminder, DueStatus>>) {
        if (due.isEmpty() || !canNotify()) return
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val manager = NotificationManagerCompat.from(context)
        try {
            due.forEach { (reminder, status) ->
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(reminder.name)
                    .setContentText("${MoneyFormat.format(reminder.amount)} · ${statusText(status)}")
                    .setContentIntent(open)
                    .setAutoCancel(true)
                    .setGroup(GROUP)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .build()
                manager.notify(reminder.id.toInt(), notification)
            }
            if (due.size > 1) {
                val summary = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(context.resources.getQuantityString(R.plurals.reminders_due, due.size, due.size))
                    .setContentIntent(open)
                    .setAutoCancel(true)
                    .setGroup(GROUP)
                    .setGroupSummary(true)
                    .build()
                manager.notify(SUMMARY_ID, summary)
            }
        } catch (_: SecurityException) {
            // Permission was revoked between the check and the post; nothing to do.
        }
    }

    private fun statusText(status: DueStatus): String = when (status) {
        is DueStatus.Overdue -> context.resources.getQuantityString(R.plurals.due_overdue, status.days, status.days)
        DueStatus.Today -> context.getString(R.string.due_today)
        DueStatus.Tomorrow -> context.getString(R.string.due_tomorrow)
        is DueStatus.InDays -> context.resources.getQuantityString(R.plurals.due_in_days, status.days, status.days)
    }

    private companion object {
        const val CHANNEL_ID = "payment_reminders"
        const val GROUP = "payment_reminders"
        const val SUMMARY_ID = Int.MAX_VALUE
    }
}
