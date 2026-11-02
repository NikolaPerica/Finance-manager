package com.example.financemanager.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.financemanager.R
import com.example.financemanager.domain.BudgetMonths
import com.example.financemanager.model.AppCurrency
import com.example.financemanager.model.ThemeMode
import com.example.financemanager.ui.stats.Choice
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private enum class SettingsDialog { CURRENCY, MONTH_START, REMINDER_TIME, LOCK_AFTER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var dialog by rememberSaveable { mutableStateOf<SettingsDialog?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.natrag))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        val current = settings ?: return@Scaffold
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Section(R.string.settings_look) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(12.dp))
                        Choice(
                            listOf(
                                ThemeMode.SYSTEM to R.string.theme_system,
                                ThemeMode.LIGHT to R.string.theme_light,
                                ThemeMode.DARK to R.string.theme_dark,
                            ),
                            current.theme,
                            viewModel::setTheme,
                            Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            item {
                Section(R.string.settings_money) {
                    SettingRow(
                        title = stringResource(R.string.settings_currency),
                        value = currencyLabel(current.currency),
                        note = stringResource(R.string.settings_currency_note),
                        onClick = { dialog = SettingsDialog.CURRENCY },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(
                        title = stringResource(R.string.settings_month_start),
                        value = monthStartLabel(current.monthStartDay),
                        note = stringResource(R.string.settings_month_start_note),
                        onClick = { dialog = SettingsDialog.MONTH_START },
                    )
                }
            }
            item {
                Section(R.string.settings_reminders) {
                    SettingRow(
                        title = stringResource(R.string.settings_reminder_time),
                        value = TimeFormat.format(current.reminderTime),
                        note = stringResource(R.string.settings_reminder_time_note),
                        onClick = { dialog = SettingsDialog.REMINDER_TIME },
                    )
                }
            }
            item {
                Section(R.string.settings_security) {
                    SettingRow(
                        title = stringResource(R.string.settings_lock_after),
                        value = lockAfterLabel(current.lockAfterMinutes),
                        note = stringResource(R.string.settings_lock_after_note),
                        onClick = { dialog = SettingsDialog.LOCK_AFTER },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SwitchRow(
                        title = stringResource(R.string.settings_hide_widget),
                        note = stringResource(R.string.settings_hide_widget_note),
                        checked = current.hideWidgetAmounts,
                        onCheckedChange = viewModel::setHideWidgetAmounts,
                    )
                }
            }
        }

        val close = { dialog = null }
        when (dialog) {
            SettingsDialog.CURRENCY -> ChoiceDialog(
                title = stringResource(R.string.settings_currency),
                options = AppCurrency.entries,
                selected = current.currency,
                label = { currencyLabel(it) },
                onSelect = viewModel::setCurrency,
                onDismiss = close,
            )
            SettingsDialog.MONTH_START -> MonthStartDialog(current.monthStartDay, viewModel::setMonthStartDay, close)
            SettingsDialog.REMINDER_TIME -> ReminderTimeDialog(current.reminderTime, viewModel::setReminderTime, close)
            SettingsDialog.LOCK_AFTER -> ChoiceDialog(
                title = stringResource(R.string.settings_lock_after),
                options = LockAfterChoices.let { if (current.lockAfterMinutes in it) it else (it + current.lockAfterMinutes).sorted() },
                selected = current.lockAfterMinutes,
                label = { lockAfterLabel(it) },
                onSelect = viewModel::setLockAfterMinutes,
                onDismiss = close,
            )
            null -> Unit
        }
    }
}

@Composable
private fun Section(title: Int, content: @Composable () -> Unit) {
    Column(Modifier.contentWidth()) {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 8.dp),
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingRow(title: String, value: String, note: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SwitchRow(title: String, note: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.size(16.dp))
        // The whole row toggles; the switch only shows the state.
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** One choice from a list; picking it saves it and closes the dialog. */
@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = option == selected,
                                role = Role.RadioButton,
                                onClick = {
                                    onSelect(option)
                                    onDismiss()
                                },
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Spacer(Modifier.size(12.dp))
                        Text(label(option), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.odustani)) }
        },
    )
}

/** Days 1–28 laid out like a calendar, a week per row. */
@Composable
private fun MonthStartDialog(selected: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_month_start)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                (1..BudgetMonths.MAX_START_DAY).chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        week.forEach { day ->
                            val isSelected = day == selected
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                        CircleShape,
                                    )
                                    .selectable(selected = isSelected, role = Role.RadioButton) {
                                        onSelect(day)
                                        onDismiss()
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "$day",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.odustani)) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(time: LocalTime, onSave: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_reminder_time)) },
        text = { TimePicker(state) },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(LocalTime.of(state.hour, state.minute))
                    onDismiss()
                },
            ) { Text(stringResource(R.string.spremi)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.odustani)) }
        },
    )
}

@Composable
private fun currencyLabel(currency: AppCurrency): String {
    val name = when (currency) {
        AppCurrency.EUR -> R.string.currency_eur
        AppCurrency.USD -> R.string.currency_usd
        AppCurrency.GBP -> R.string.currency_gbp
        AppCurrency.CHF -> R.string.currency_chf
        AppCurrency.BAM -> R.string.currency_bam
        AppCurrency.RSD -> R.string.currency_rsd
        AppCurrency.HUF -> R.string.currency_huf
        AppCurrency.CZK -> R.string.currency_czk
        AppCurrency.PLN -> R.string.currency_pln
    }
    return stringResource(R.string.currency_option, stringResource(name), currency.symbol)
}

@Composable
private fun monthStartLabel(day: Int): String =
    if (day == 1) stringResource(R.string.settings_month_start_calendar) else stringResource(R.string.settings_month_start_value, day)

@Composable
private fun lockAfterLabel(minutes: Int): String =
    if (minutes == 0) stringResource(R.string.lock_after_now) else stringResource(R.string.lock_after_minutes, minutes)

private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm")

private fun Modifier.contentWidth() = widthIn(max = 640.dp).fillMaxWidth()
