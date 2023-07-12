package com.example.financemanager
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.financemanager.data.AppDatabase
import com.example.financemanager.data.Reminder
import com.example.financemanager.data.ReminderDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class DateCheckWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private lateinit var reminderDao: ReminderDao
    private lateinit var notificationManager: NotificationManagerCompat

    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        val currentDate = getCurrentDate()
        val reminders = reminderDao.getAllReminders()

        for (reminder in reminders) {
            if (reminder.date == currentDate || isMatchingPeriod(reminder, currentDate)) {
                showNotification("Reminder", "Name: ${reminder.name}, Amount: ${reminder.amount}")
            }
        }

        Result.success()
    }

    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun isMatchingPeriod(reminder: Reminder, currentDate: String): Boolean {
        val calendar = Calendar.getInstance()

        when (reminder.periodType) {
            "Once" -> return false
            "Monthly" -> {
                calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(reminder.date)
                val reminderMonth = calendar.get(Calendar.MONTH)
                calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(currentDate)
                val currentMonth = calendar.get(Calendar.MONTH)
                return reminderMonth == currentMonth
            }
            "Quarterly" -> {
                calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(reminder.date)
                val reminderQuarter = calendar.get(Calendar.MONTH) / 3
                calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(currentDate)
                val currentQuarter = calendar.get(Calendar.MONTH) / 3
                return reminderQuarter == currentQuarter
            }
            "Annually" -> {
                calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(reminder.date)
                val reminderYear = calendar.get(Calendar.YEAR)
                calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(currentDate)
                val currentYear = calendar.get(Calendar.YEAR)
                return reminderYear == currentYear
            }
            else -> return false
        }
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "date_check_channel"
        val notificationId = 1

        // Create a notification channel (required for Android Oreo and above)
        createNotificationChannel()

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your app's icon here
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            if (hasNotificationPermission()) {
                notificationManager.notify(notificationId, notification)
            } else {
                // Handle the case where the app doesn't have notification permission
                // You can show a toast, log an error, or perform appropriate error handling
            }
        } catch (e: SecurityException) {
            // Handle the security exception
            // You can show a toast, log an error, or perform appropriate error handling
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return applicationContext.checkSelfPermission(Manifest.permission.ACCESS_NOTIFICATION_POLICY) == PackageManager.PERMISSION_GRANTED
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "date_check_channel"
            val channelName = "Date Check Channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance)
            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

  /*  private fun hasNotificationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_NOTIFICATION_POLICY
        ) == PackageManager.PERMISSION_GRANTED
    }*/

    companion object {
        private const val WORK_TAG = "date_check_worker"

        fun scheduleDailyCheck(context: Context) {
            val workManager = WorkManager.getInstance(context)

            // Create a periodic work request to run daily
            val request = PeriodicWorkRequestBuilder<DateCheckWorker>(1, TimeUnit.DAYS)
                .addTag(WORK_TAG)
                .build()

            // Enqueue the work request
            workManager.enqueueUniquePeriodicWork(
                WORK_TAG,
                ExistingPeriodicWorkPolicy.REPLACE,
                request
            )
        }
    }

    init {
        val reminderDatabase = AppDatabase.getDatabase(context.applicationContext)
        reminderDao = reminderDatabase.reminderDao()
        notificationManager = NotificationManagerCompat.from(applicationContext)
    }
}
