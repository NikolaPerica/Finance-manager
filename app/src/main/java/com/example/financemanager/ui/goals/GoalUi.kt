package com.example.financemanager.ui.goals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.financemanager.R
import com.example.financemanager.data.SavingsGoal
import com.example.financemanager.ui.DateFormat
import com.example.financemanager.ui.MoneyFormat
import com.example.financemanager.ui.budgets.BudgetBar
import com.example.financemanager.ui.theme.FinanceTheme
import kotlin.math.roundToInt

@Composable
fun goalColor(goal: SavingsGoal): Color = FinanceTheme.colors.categories[goal.colorIndex]

/** Goal with its progress bar and what is left to do; used in the goal list and on the dashboard. */
@Composable
fun GoalCard(progress: GoalProgress, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = goalColor(progress.goal)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.size(10.dp).background(color, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(
                    progress.goal.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                GoalStatusChip(progress.status)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    stringResource(R.string.budget_spent_of, MoneyFormat.format(progress.saved), MoneyFormat.format(progress.goal.target)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${(progress.fraction * 100).roundToInt()} %",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Spacer(Modifier.height(10.dp))
            BudgetBar(progress.fraction, color = color, track = MaterialTheme.colorScheme.surfaceContainerHighest)
            Spacer(Modifier.height(8.dp))
            Text(
                goalHint(progress),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One line on what is left to do: how much per month, how much is missing, or that it's done. */
@Composable
fun goalHint(progress: GoalProgress): String {
    val deadline = progress.goal.deadline
    val monthly = progress.monthlyNeeded
    return when {
        progress.status == GoalStatus.REACHED -> stringResource(R.string.goal_reached_hint)
        progress.status == GoalStatus.OVERDUE -> stringResource(R.string.goal_missing, MoneyFormat.format(progress.remaining))
        monthly != null && deadline != null ->
            stringResource(R.string.goal_monthly_until, MoneyFormat.format(monthly), DateFormat.short(deadline))
        else -> stringResource(R.string.goal_missing, MoneyFormat.format(progress.remaining))
    }
}

/** "Reached" / "Deadline passed" with an icon, so the state never relies on colour alone. */
@Composable
fun GoalStatusChip(status: GoalStatus, modifier: Modifier = Modifier) {
    if (status == GoalStatus.SAVING) return
    val reached = status == GoalStatus.REACHED
    val color = if (reached) FinanceTheme.colors.income else FinanceTheme.colors.warning
    Row(
        modifier
            .background(if (reached) FinanceTheme.colors.incomeContainer else FinanceTheme.colors.warningContainer, CircleShape)
            .padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(if (reached) R.drawable.ic_check else R.drawable.ic_warning),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            stringResource(if (reached) R.string.goal_reached else R.string.goal_overdue),
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}
