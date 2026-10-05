package com.example.financemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.financemanager.R
import com.example.financemanager.ui.theme.FinanceTheme

/** Round badge with an arrow: down (money coming in) for income, up for expense. */
@Composable
fun TransactionTypeBadge(
    isIncome: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    container: Color = if (isIncome) FinanceTheme.colors.incomeContainer else FinanceTheme.colors.expenseContainer,
    tint: Color = if (isIncome) FinanceTheme.colors.income else FinanceTheme.colors.expense,
) {
    Box(
        modifier = modifier.size(size).background(container, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(if (isIncome) R.drawable.ic_arrow_downward else R.drawable.ic_arrow_upward),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.padding(size / 4.4f),
        )
    }
}
