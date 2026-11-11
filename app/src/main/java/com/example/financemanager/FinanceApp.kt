package com.example.financemanager

import android.app.Application
import com.example.financemanager.data.SettingsRepository
import com.example.financemanager.domain.MoneyFormat
import com.example.financemanager.model.Settings
import com.example.financemanager.notifications.BudgetNotifier
import com.example.financemanager.notifications.ReminderCheck
import com.example.financemanager.notifications.ReminderNotifier
import com.example.financemanager.notifications.WeeklySummary
import com.example.financemanager.notifications.WeeklySummaryNotifier
import com.example.financemanager.security.AppLock
import com.example.financemanager.widget.WidgetSync
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

// The repository, the settings and the lock come from Hilt, see di/AppModule.
@HiltAndroidApp
class FinanceApp : Application() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var appLock: AppLock

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Read before anything is drawn, so amounts never show up in the wrong currency.
        var settings = runBlocking { settingsRepository.settings.first() }
        apply(settings)
        ReminderNotifier(this).createChannel()
        BudgetNotifier(this).createChannel()
        WeeklySummaryNotifier(this).createChannel()
        ReminderCheck.schedule(this)
        WeeklySummary.schedule(this)
        WidgetSync.startIfPlaced(this)

        scope.launch {
            settingsRepository.settings.collect { new ->
                val old = settings
                settings = new
                apply(new)
                if (new.reminderTime != old.reminderTime) ReminderCheck.schedule(this@FinanceApp)
                if (new.weeklySummary != old.weeklySummary) WeeklySummary.schedule(this@FinanceApp)
                val widgetChanged = new.currency != old.currency || new.monthStartDay != old.monthStartDay ||
                    new.hideWidgetAmounts != old.hideWidgetAmounts
                if (widgetChanged) WidgetSync.refresh(this@FinanceApp)
            }
        }
    }

    private fun apply(settings: Settings) {
        MoneyFormat.symbol = settings.currency.symbol
        appLock.lockAfter = settings.lockAfterMinutes.minutes
    }
}
