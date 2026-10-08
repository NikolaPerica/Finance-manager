package com.example.financemanager.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.financemanager.MainActivity
import com.example.financemanager.R
import com.example.financemanager.ui.MoneyFormat
import com.example.financemanager.ui.budgets.BudgetAlert
import com.example.financemanager.ui.budgets.BudgetLevel
import kotlin.math.roundToInt

/** Tells the user that a category is close to or over its monthly budget. */
class BudgetNotifier(private val context: Context) {

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.budget_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.budget_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun notify(alert: BudgetAlert) {
        if (!ReminderNotifier(context).canNotify()) return
        val title = when (alert.level) {
            BudgetLevel.EXCEEDED -> context.getString(R.string.budget_alert_exceeded, alert.category)
            else -> context.getString(R.string.budget_alert_warning, alert.category, (alert.spent / alert.budget * 100).roundToInt())
        }
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(
                context.getString(R.string.budget_alert_text, MoneyFormat.format(alert.spent), MoneyFormat.format(alert.budget)),
            )
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
        try {
            // One notification per category, replaced as spending goes on.
            NotificationManagerCompat.from(context).notify(TAG, alert.category.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission was revoked between the check and the post; nothing to do.
        }
    }

    private companion object {
        const val CHANNEL_ID = "budgets"
        const val TAG = "budget"
    }
}
