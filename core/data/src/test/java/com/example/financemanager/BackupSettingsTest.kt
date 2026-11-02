package com.example.financemanager

import com.example.financemanager.data.FinanceData
import com.example.financemanager.data.backup.parseBackup
import com.example.financemanager.data.backup.toBackup
import com.example.financemanager.data.backup.toJson
import com.example.financemanager.data.backup.toSettings
import com.example.financemanager.model.AppCurrency
import com.example.financemanager.model.Settings
import com.example.financemanager.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

class BackupSettingsTest {

    private val empty = FinanceData(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
    private val createdAt = LocalDateTime.of(2026, 11, 2, 18, 30)

    @Test
    fun settingsGoIntoTheBackupAndComeBack() {
        val settings = Settings(
            theme = ThemeMode.DARK,
            currency = AppCurrency.CHF,
            monthStartDay = 25,
            reminderTime = LocalTime.of(7, 45),
            lockAfterMinutes = 0,
            hideWidgetAmounts = true,
        )
        val json = empty.toBackup(createdAt, settings).toJson()
        assertEquals(settings, parseBackup(json).toSettings())
    }

    @Test
    fun aBackupWithoutSettingsHasNone() {
        assertNull(parseBackup(empty.toBackup(createdAt).toJson()).toSettings())
        // As made before settings were backed up.
        val old = """{"format":"finance-manager-backup","version":1,"createdAt":"2026-10-21T10:00:00"}"""
        assertNull(parseBackup(old).toSettings())
    }

    @Test
    fun aSettingThatCantBeUsedKeepsItsDefault() {
        val json = """
            {"format":"finance-manager-backup","version":1,"createdAt":"2026-10-21T10:00:00",
             "settings":{"theme":"PURPLE","currency":"GBP","monthStartDay":31,"reminderTime":"25:99","lockAfterMinutes":-3}}
        """.trimIndent()
        assertEquals(Settings(currency = AppCurrency.GBP), parseBackup(json).toSettings())
    }
}
