package com.example.financemanager.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.financemanager.R
import com.example.financemanager.domain.Croatian
import com.example.financemanager.domain.DateFormat
import com.example.financemanager.domain.MoneyFormat
import com.example.financemanager.ui.theme.FinanceTheme
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel, onBack: () -> Unit, onOpenDay: (LocalDate) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.calendar_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.natrag))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Row(Modifier.contentWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        value = pluralStringResource(R.plurals.days_count, state.streak, state.streak),
                        label = stringResource(R.string.calendar_streak),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        value = pluralStringResource(R.plurals.days_count, state.noSpendDays, state.noSpendDays),
                        label = stringResource(R.string.calendar_no_spend_days),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.contentWidth(),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        MonthSwitcher(state, viewModel::shift)
                        Spacer(Modifier.height(8.dp))
                        MonthGrid(state, onOpenDay)
                    }
                }
            }
            item { Legend(Modifier.contentWidth()) }
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = FinanceTheme.colors.income)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MonthSwitcher(state: CalendarUiState, onShift: (Long) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onShift(-1) }) {
            Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = stringResource(R.string.previous_period))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                MonthTitle.format(state.month).replaceFirstChar { it.titlecase(Croatian) },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(R.string.calendar_month_total, MoneyFormat.format(state.monthTotal)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = { onShift(1) }, enabled = state.canGoForward) {
            Icon(
                painterResource(R.drawable.ic_chevron_right),
                contentDescription = stringResource(R.string.next_period),
                tint = if (state.canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            )
        }
    }
}

/** Weeks from Monday to Sunday; the darker a day, the more was spent on it. */
@Composable
private fun MonthGrid(state: CalendarUiState, onOpenDay: (LocalDate) -> Unit) {
    val first = state.month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val last = state.month.atEndOfMonth().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    val most = state.days.values.maxOrNull() ?: 0.0

    Row(Modifier.fillMaxWidth()) {
        DayOfWeek.entries.forEach { day ->
            Text(
                day.getDisplayName(TextStyle.SHORT, Croatian).take(3),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
    Spacer(Modifier.height(4.dp))
    generateSequence(first) { it.plusWeeks(1) }.takeWhile { !it.isAfter(last) }.forEach { week ->
        Row(Modifier.fillMaxWidth()) {
            (0L until 7L).map { week.plusDays(it) }.forEach { date ->
                Box(Modifier.weight(1f).aspectRatio(0.85f).padding(2.dp)) {
                    if (date.month == state.month.month) DayCell(date, state, most, onOpenDay)
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, state: CalendarUiState, most: Double, onOpenDay: (LocalDate) -> Unit) {
    val spent = state.days[date]
    val future = date.isAfter(state.today)
    val recorded = state.firstDay != null && !date.isBefore(state.firstDay)
    val noSpend = spent == null && recorded && !future
    val shape = RoundedCornerShape(10.dp)
    // Square root so that small days still show next to one big one.
    val strength = if (spent == null || most <= 0) 0f else (0.2f + 0.65f * sqrt(spent / most).toFloat())
    val fill = if (spent == null) MaterialTheme.colorScheme.surfaceContainerHighest else FinanceTheme.colors.expense.copy(alpha = strength)
    val onFill = if (strength > 0.55f) Color.White else MaterialTheme.colorScheme.onSurface
    val description = when {
        spent != null -> stringResource(R.string.calendar_day_spent, DateFormat.long(date), MoneyFormat.format(spent))
        noSpend -> stringResource(R.string.calendar_day_no_spend, DateFormat.long(date))
        else -> DateFormat.long(date)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(if (future) Color.Transparent else fill)
            .then(if (date == state.today) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .then(if (recorded && !future) Modifier.clickable { onOpenDay(date) } else Modifier)
            .semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "${date.dayOfMonth}",
            style = MaterialTheme.typography.bodySmall,
            color = if (future) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else onFill,
        )
        when {
            spent != null -> Text(WholeAmount.get()!!.format(spent), fontSize = 9.sp, color = onFill, maxLines = 1)
            noSpend -> Box(Modifier.padding(top = 3.dp).size(6.dp).background(FinanceTheme.colors.income, CircleShape))
        }
    }
}

@Composable
private fun Legend(modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(FinanceTheme.colors.income, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(R.string.calendar_legend_no_spend),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(stringResource(R.string.calendar_legend_less), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        listOf(0.25f, 0.45f, 0.65f, 0.85f).forEach { alpha ->
            Box(Modifier.padding(horizontal = 2.dp).size(12.dp).clip(RoundedCornerShape(3.dp)).background(FinanceTheme.colors.expense.copy(alpha = alpha)))
        }
        Text(stringResource(R.string.calendar_legend_more), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun Modifier.contentWidth() = widthIn(max = 640.dp).fillMaxWidth()

private val MonthTitle = DateTimeFormatter.ofPattern("LLLL yyyy.", Croatian)

/** Whole euros, e.g. "1.234", to fit in a day. */
private val WholeAmount: ThreadLocal<DecimalFormat> = ThreadLocal.withInitial { DecimalFormat("#,##0", DecimalFormatSymbols(Croatian)) }
