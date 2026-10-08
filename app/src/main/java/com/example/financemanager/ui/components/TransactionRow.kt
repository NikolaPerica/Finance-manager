package com.example.financemanager.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.financemanager.R
import com.example.financemanager.data.Category
import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import com.example.financemanager.ui.DateFormat
import com.example.financemanager.ui.MoneyFormat
import com.example.financemanager.ui.theme.FinanceTheme
import java.time.LocalDate

/**
 * A transaction as a card: type badge, category (with a dot in [categoryColor] when known),
 * date and note, and the signed amount. [showDate] is off where rows are already grouped by day.
 */
@Composable
fun TransactionRow(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    categoryColor: Color? = null,
    showDate: Boolean = true,
) {
    val isIncome = transaction.type == TransactionType.INCOME
    val today = LocalDate.now()
    val dateText = when {
        !showDate -> ""
        transaction.date == today -> stringResource(R.string.today)
        transaction.date == today.minusDays(1) -> stringResource(R.string.yesterday)
        else -> DateFormat.short(transaction.date)
    }
    val details = listOf(dateText, transaction.note).filter { it.isNotBlank() }.joinToString(" • ")
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            TransactionTypeBadge(isIncome)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (categoryColor != null) {
                        CategoryDot(categoryColor)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        transaction.category.ifBlank { stringResource(if (isIncome) R.string.prihod else R.string.rashod) },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (details.isNotEmpty()) {
                    Text(
                        details,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                MoneyFormat.signed(transaction.amount, isIncome),
                style = MaterialTheme.typography.titleMedium,
                color = if (isIncome) FinanceTheme.colors.income else FinanceTheme.colors.expense,
            )
        }
    }
}

/** Looks up the colour of each transaction's category by type and name. */
@Composable
fun rememberCategoryColors(categories: List<Category>): (Transaction) -> Color? {
    val palette = FinanceTheme.colors.categories
    val byKey = remember(categories) { categories.associateBy { it.type to it.name } }
    return { transaction -> byKey[transaction.type to transaction.category]?.let { palette[it.colorIndex] } }
}
