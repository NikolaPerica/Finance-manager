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
import com.example.financemanager.domain.MoneyFormat
import com.example.financemanager.model.Wish

/** Says when the time to think a wish over has passed, once per wish. */
class WishNotifier(private val context: Context) {

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.wishes_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.wishes_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun notify(ready: List<Wish>) {
        if (ready.isEmpty() || !ReminderNotifier(context).canNotify()) return
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            ready.forEach { wish ->
                val text = context.getString(R.string.wish_notification_text, MoneyFormat.format(wish.price))
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(context.getString(R.string.wish_notification_title, wish.name))
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    .setContentIntent(open)
                    .setAutoCancel(true)
                    .build()
                // Kept apart from the reminders' ids, which are small numbers too.
                NotificationManagerCompat.from(context).notify(ID_BASE - wish.id.toInt(), notification)
            }
        } catch (_: SecurityException) {
            // Permission was revoked between the check and the post; nothing to do.
        }
    }

    private companion object {
        const val CHANNEL_ID = "wishes"
        const val ID_BASE = Int.MAX_VALUE / 2
    }
}
