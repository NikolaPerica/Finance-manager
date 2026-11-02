package com.example.financemanager.model

import java.time.LocalTime

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * The sign shown after amounts. Only the label changes: amounts are stored as plain numbers
 * and are never converted.
 */
enum class AppCurrency(val symbol: String) {
    EUR("€"),
    USD("$"),
    GBP("£"),
    CHF("CHF"),
    BAM("KM"),
    RSD("din."),
    HUF("Ft"),
    CZK("Kč"),
    PLN("zł"),
}

data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val currency: AppCurrency = AppCurrency.EUR,
    /** Day of the month (1–28) on which budgets, statistics and insights start a new month. */
    val monthStartDay: Int = 1,
    /** When the daily payment reminder check runs. */
    val reminderTime: LocalTime = LocalTime.of(9, 0),
    /** How long the app may stay in the background before it asks for the fingerprint again; 0 locks right away. */
    val lockAfterMinutes: Int = 2,
    /** The home screen widget shows dots instead of amounts, as anyone holding the phone can see it. */
    val hideWidgetAmounts: Boolean = false,
)
