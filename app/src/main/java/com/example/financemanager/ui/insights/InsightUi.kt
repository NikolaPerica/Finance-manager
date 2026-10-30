package com.example.financemanager.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.financemanager.R
import com.example.financemanager.domain.Croatian
import com.example.financemanager.domain.DateFormat
import com.example.financemanager.domain.MoneyFormat
import com.example.financemanager.domain.insights.Insight
import com.example.financemanager.ui.theme.FinanceTheme
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

/** An insight as an icon, a short title and one or two lines of explanation. */
@Composable
fun InsightRow(insight: Insight, modifier: Modifier = Modifier) {
    val text = insightText(insight)
    Row(modifier, verticalAlignment = Alignment.Top) {
        Column(
            Modifier.size(40.dp).background(text.tint.copy(alpha = 0.14f), CircleShape),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(painterResource(text.icon), contentDescription = null, tint = text.tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(text.title, style = MaterialTheme.typography.titleSmall)
            Text(text.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private data class InsightText(val title: String, val body: String, val icon: Int, val tint: Color)

@Composable
private fun insightText(insight: Insight): InsightText = when (insight) {
    is Insight.MonthEndForecast -> {
        val basis = if (insight.basedOnMonths == 0) {
            stringResource(R.string.insight_basis_pace)
        } else {
            pluralStringResource(R.plurals.insight_basis_months, insight.basedOnMonths, insight.basedOnMonths)
        }
        InsightText(
            title = stringResource(R.string.insight_forecast_title, MoneyFormat.format(insight.projectedBalance)),
            body = when {
                insight.basedOnMonths == 0 -> stringResource(R.string.insight_forecast_pace, basis, MoneyFormat.format(insight.expectedExpense))
                // "Still comes in 0 €" says nothing; mention only what goes out.
                insight.expectedIncome < 1 -> stringResource(R.string.insight_forecast_expense_only, basis, MoneyFormat.format(insight.expectedExpense))
                else -> stringResource(
                    R.string.insight_forecast_body,
                    basis,
                    MoneyFormat.format(insight.expectedIncome),
                    MoneyFormat.format(insight.expectedExpense),
                )
            },
            icon = R.drawable.ic_chart,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
    is Insight.CategoryTrend -> {
        val more = insight.change > 0
        val percent = (abs(insight.change) * 100).roundToInt()
        InsightText(
            title = insight.category.ifBlank { stringResource(R.string.no_category) },
            // From double the usual on, "3,4 puta više" reads easier than "240 % više".
            body = if (insight.current >= 2 * insight.usual) {
                stringResource(
                    R.string.insight_trend_times,
                    Times.get()!!.format(insight.current / insight.usual),
                    MoneyFormat.format(insight.current),
                    MoneyFormat.format(insight.usual),
                )
            } else {
                stringResource(
                    if (more) R.string.insight_trend_more else R.string.insight_trend_less,
                    percent,
                    MoneyFormat.format(insight.current),
                    MoneyFormat.format(insight.usual),
                )
            },
            icon = if (more) R.drawable.ic_arrow_upward else R.drawable.ic_arrow_downward,
            // Spending more is the bad direction here.
            tint = if (more) FinanceTheme.colors.expense else FinanceTheme.colors.income,
        )
    }
    is Insight.BudgetRisk -> InsightText(
        title = stringResource(R.string.insight_budget_title, insight.category),
        body = stringResource(R.string.insight_budget_body, MoneyFormat.format(insight.projected), MoneyFormat.format(insight.budget)),
        icon = R.drawable.ic_warning,
        tint = FinanceTheme.colors.warning,
    )
    is Insight.UnusualExpense -> InsightText(
        title = stringResource(R.string.insight_unusual_title),
        body = stringResource(
            R.string.insight_unusual_body,
            insight.transaction.category.ifBlank { stringResource(R.string.no_category) },
            MoneyFormat.format(insight.transaction.amount),
            DateFormat.short(insight.transaction.date),
            MoneyFormat.format(insight.usual),
        ),
        icon = R.drawable.ic_label,
        tint = FinanceTheme.colors.warning,
    )
    is Insight.LastMonth -> {
        val month = MonthName.format(insight.month).replaceFirstChar { it.titlecase(Croatian) }
        val share = insight.savedShare?.let { (abs(it) * 100).roundToInt() }
        InsightText(
            title = if (insight.saved >= 0) {
                stringResource(R.string.insight_last_month_saved, month, MoneyFormat.format(insight.saved))
            } else {
                stringResource(R.string.insight_last_month_overspent, month, MoneyFormat.format(-insight.saved))
            },
            body = when {
                share == null -> stringResource(R.string.insight_last_month_no_income, MoneyFormat.format(insight.expense))
                insight.saved >= 0 -> stringResource(R.string.insight_last_month_share, share)
                else -> stringResource(R.string.insight_last_month_over_share, share)
            },
            icon = R.drawable.ic_savings,
            tint = if (insight.saved >= 0) FinanceTheme.colors.income else FinanceTheme.colors.expense,
        )
    }
}

private val MonthName = DateTimeFormatter.ofPattern("LLLL", Croatian)
private val Times = ThreadLocal.withInitial { DecimalFormat("0.#", DecimalFormatSymbols(Croatian)) }
