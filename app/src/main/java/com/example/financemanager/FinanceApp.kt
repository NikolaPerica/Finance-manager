package com.example.financemanager

import android.app.Application
import androidx.work.Configuration
import com.example.financemanager.data.AppDatabase
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.data.RoomFinanceRepository
import com.example.financemanager.notifications.ReminderNotifier
import com.example.financemanager.notifications.ReminderWorker

// WorkManager is initialised on demand from workManagerConfiguration (its startup
// initializer is removed in the manifest).
class FinanceApp : Application(), Configuration.Provider {
    val repository: FinanceRepository by lazy { RoomFinanceRepository(AppDatabase.create(this)) }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()
        ReminderNotifier(this).createChannel()
        ReminderWorker.schedule(this)
    }
}
