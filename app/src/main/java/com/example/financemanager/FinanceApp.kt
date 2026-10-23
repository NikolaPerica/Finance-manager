package com.example.financemanager

import android.app.Application
import com.example.financemanager.data.AppDatabase
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.data.RoomFinanceRepository
import com.example.financemanager.notifications.BudgetNotifier
import com.example.financemanager.notifications.ReminderCheck
import com.example.financemanager.notifications.ReminderNotifier
import com.example.financemanager.security.AppLock

class FinanceApp : Application() {
    val repository: FinanceRepository by lazy { RoomFinanceRepository(AppDatabase.create(this)) }

    /** Lives as long as the process, so rotating the screen doesn't lock the app. */
    val appLock = AppLock()

    override fun onCreate() {
        super.onCreate()
        ReminderNotifier(this).createChannel()
        BudgetNotifier(this).createChannel()
        ReminderCheck.schedule(this)
    }
}
