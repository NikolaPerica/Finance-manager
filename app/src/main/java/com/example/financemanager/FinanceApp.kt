package com.example.financemanager

import android.app.Application
import com.example.financemanager.data.AppDatabase
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.data.RoomFinanceRepository

class FinanceApp : Application() {
    val repository: FinanceRepository by lazy { RoomFinanceRepository(AppDatabase.create(this)) }
}
