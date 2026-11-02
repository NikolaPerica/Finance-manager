package com.example.financemanager

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.financemanager.data.DataStoreSettingsRepository
import com.example.financemanager.model.AppCurrency
import com.example.financemanager.model.Settings
import com.example.financemanager.model.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val scope = TestScope(UnconfinedTestDispatcher())
    private val store = PreferenceDataStoreFactory.create(scope = scope.backgroundScope) {
        File(folder.root, "settings.preferences_pb")
    }
    private val repository = DataStoreSettingsRepository(store)

    @Test
    fun startsWithTheDefaults() = scope.runTest {
        assertEquals(Settings(), repository.settings.first())
    }

    @Test
    fun keepsWhatWasSet() = scope.runTest {
        repository.update {
            it.copy(
                theme = ThemeMode.DARK,
                currency = AppCurrency.BAM,
                monthStartDay = 15,
                reminderTime = LocalTime.of(7, 30),
                lockAfterMinutes = 0,
                hideWidgetAmounts = true,
            )
        }
        assertEquals(
            Settings(
                ThemeMode.DARK,
                AppCurrency.BAM,
                monthStartDay = 15,
                reminderTime = LocalTime.of(7, 30),
                lockAfterMinutes = 0,
                hideWidgetAmounts = true,
            ),
            repository.settings.first(),
        )
    }

    @Test
    fun valuesItCannotUseFallBackToTheDefaults() = scope.runTest {
        store.edit {
            it[stringPreferencesKey("theme")] = "PURPLE"
            it[stringPreferencesKey("currency")] = "XYZ"
            it[intPreferencesKey("month_start_day")] = 31
            it[intPreferencesKey("reminder_minute")] = 5000
            it[intPreferencesKey("lock_after_minutes")] = -1
        }
        assertEquals(Settings(), repository.settings.first())
    }
}
