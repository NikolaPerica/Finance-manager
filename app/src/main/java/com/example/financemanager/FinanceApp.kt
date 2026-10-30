package com.example.financemanager

import android.app.Application
import com.example.financemanager.notifications.BudgetNotifier
import com.example.financemanager.notifications.ReminderCheck
import com.example.financemanager.notifications.ReminderNotifier
import com.example.financemanager.widget.WidgetSync
import dagger.hilt.android.HiltAndroidApp

// The repository and the lock come from Hilt, see di/AppModule.
@HiltAndroidApp
class FinanceApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderNotifier(this).createChannel()
        BudgetNotifier(this).createChannel()
        ReminderCheck.schedule(this)
        WidgetSync.startIfPlaced(this)
    }
}
