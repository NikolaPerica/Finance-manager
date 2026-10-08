package com.example.financemanager

import android.app.Application
import com.example.financemanager.data.AppDatabase
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.data.RoomFinanceRepository
import com.example.financemanager.notifications.BudgetNotifier
import com.example.financemanager.notifications.ReminderCheck
import com.example.financemanager.notifications.ReminderNotifier

class FinanceApp : Application() {
    val repository: FinanceRepository by lazy { RoomFinanceRepository(AppDatabase.create(this)) }

    override fun onCreate() {
        super.onCreate()
        ReminderNotifier(this).createChannel()
        BudgetNotifier(this).createChannel()
        ReminderCheck.schedule(this)
    }
}
