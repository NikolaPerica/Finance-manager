package com.example.financemanager.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.financemanager.model.AppCurrency
import com.example.financemanager.model.Settings
import com.example.financemanager.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalTime

interface SettingsRepository {
    val settings: Flow<Settings>

    suspend fun update(transform: (Settings) -> Settings)
}

// One store per process, as DataStore requires.
private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Settings kept with DataStore. A missing or unreadable value falls back to its default. */
class DataStoreSettingsRepository(private val store: DataStore<Preferences>) : SettingsRepository {

    constructor(context: Context) : this(context.applicationContext.settingsStore)

    override val settings: Flow<Settings> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it.toSettings() }
        .distinctUntilChanged()

    override suspend fun update(transform: (Settings) -> Settings) {
        store.edit { prefs ->
            val new = transform(prefs.toSettings())
            prefs[THEME] = new.theme.name
            prefs[CURRENCY] = new.currency.name
            prefs[MONTH_START_DAY] = new.monthStartDay
            prefs[REMINDER_MINUTE] = new.reminderTime.toSecondOfDay() / 60
            prefs[LOCK_AFTER_MINUTES] = new.lockAfterMinutes
            prefs[HIDE_WIDGET_AMOUNTS] = new.hideWidgetAmounts
        }
    }

    private fun Preferences.toSettings(): Settings {
        val defaults = Settings()
        return Settings(
            theme = this[THEME]?.let { name -> ThemeMode.entries.find { it.name == name } } ?: defaults.theme,
            currency = this[CURRENCY]?.let { name -> AppCurrency.entries.find { it.name == name } } ?: defaults.currency,
            monthStartDay = this[MONTH_START_DAY]?.takeIf { it in 1..28 } ?: defaults.monthStartDay,
            reminderTime = this[REMINDER_MINUTE]?.takeIf { it in 0 until 24 * 60 }
                ?.let { LocalTime.of(it / 60, it % 60) } ?: defaults.reminderTime,
            lockAfterMinutes = this[LOCK_AFTER_MINUTES]?.takeIf { it >= 0 } ?: defaults.lockAfterMinutes,
            hideWidgetAmounts = this[HIDE_WIDGET_AMOUNTS] ?: defaults.hideWidgetAmounts,
        )
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val CURRENCY = stringPreferencesKey("currency")
        val MONTH_START_DAY = intPreferencesKey("month_start_day")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val LOCK_AFTER_MINUTES = intPreferencesKey("lock_after_minutes")
        val HIDE_WIDGET_AMOUNTS = booleanPreferencesKey("hide_widget_amounts")
    }
}
