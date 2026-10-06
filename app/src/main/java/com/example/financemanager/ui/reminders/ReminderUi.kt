package com.example.financemanager.ui.reminders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.financemanager.R
import com.example.financemanager.data.PaymentPeriod
import com.example.financemanager.data.Reminder
import com.example.financemanager.ui.DateFormat
import com.example.financemanager.ui.MoneyFormat
import com.example.financemanager.ui.theme.FinanceTheme
import java.time.LocalDate

val PaymentPeriod.label: Int
    get() = when (this) {
        PaymentPeriod.ONCE -> R.string.period_once
        PaymentPeriod.MONTHLY -> R.string.period_monthly
        PaymentPeriod.QUARTERLY -> R.string.period_quarterly
        PaymentPeriod.SEMI_ANNUALLY -> R.string.period_semi_annually
        PaymentPeriod.YEARLY -> R.string.period_yearly
    }

@Composable
fun dueStatusText(status: DueStatus): String = when (status) {
    is DueStatus.Overdue -> pluralStringResource(R.plurals.due_overdue, status.days, status.days)
    DueStatus.Today -> stringResource(R.string.due_today)
    DueStatus.Tomorrow -> stringResource(R.string.due_tomorrow)
    is DueStatus.InDays -> pluralStringResource(R.plurals.due_in_days, status.days, status.days)
}

@Composable
private fun DueStatus.color(): Color = when (this) {
    is DueStatus.Overdue -> FinanceTheme.colors.expense
    DueStatus.Today, DueStatus.Tomorrow -> MaterialTheme.colorScheme.primary
    is DueStatus.InDays -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** A reminder as a card: name, amount, due status and, optionally, a "pay" button. */
@Composable
fun ReminderCard(
    reminder: Reminder,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onPay: (() -> Unit)? = null,
) {
    val status = DueStatus.of(reminder.nextDueDate, today)
    val accent = status.color()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, if (status is DueStatus.Overdue) accent.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).background(
                    if (status.isUrgent) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.primaryContainer,
                    CircleShape,
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_bell),
                    contentDescription = null,
                    tint = if (status.isUrgent) accent else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(reminder.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${dueStatusText(status)} · ${DateFormat.short(reminder.nextDueDate)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(R.drawable.ic_repeat),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(reminder.period.label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(MoneyFormat.format(reminder.amount), style = MaterialTheme.typography.titleMedium)
                if (onPay != null) {
                    Spacer(Modifier.size(6.dp))
                    FilledTonalButton(
                        onClick = onPay,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    ) { Text(stringResource(R.string.pay)) }
                }
            }
        }
    }
}
