package com.example.financemanager.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.financemanager.R
import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import com.example.financemanager.ui.DateFormat
import com.example.financemanager.ui.MoneyFormat
import com.example.financemanager.ui.components.TransactionTypeBadge
import com.example.financemanager.ui.components.animatedAmount
import com.example.financemanager.ui.components.staggeredEntrance
import com.example.financemanager.ui.components.SwipeToDelete
import com.example.financemanager.ui.reminders.ReminderCard
import com.example.financemanager.ui.theme.FinanceTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

private val ScreenPadding = 20.dp
private val CardShape = RoundedCornerShape(24.dp)

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onOpenReminders: () -> Unit,
    onAddReminder: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedText = stringResource(R.string.transaction_deleted)
    val undoText = stringResource(R.string.undo)

    // Intro animation plays on first open only, not when coming back from the form.
    var introPlayed by rememberSaveable { mutableStateOf(false) }
    val playIntro = !introPlayed
    LaunchedEffect(Unit) { introPlayed = true }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0),
    ) { scaffoldPadding ->
        val insets = WindowInsets.safeDrawing.asPaddingValues()
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(scaffoldPadding),
            contentPadding = PaddingValues(
                start = ScreenPadding,
                end = ScreenPadding,
                top = insets.calculateTopPadding() + 12.dp,
                bottom = insets.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "header") {
                Header(Modifier.contentWidth().staggeredEntrance(0, playIntro))
            }
            item(key = "balance") {
                BalanceCard(state, Modifier.contentWidth().padding(top = 8.dp).staggeredEntrance(1, playIntro))
            }
            item(key = "actions") {
                Row(
                    Modifier.contentWidth().padding(vertical = 8.dp).staggeredEntrance(2, playIntro),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ActionCard(true, onAddIncome, Modifier.weight(1f))
                    ActionCard(false, onAddExpense, Modifier.weight(1f))
                }
            }
            item(key = "upcomingHeader") {
                SectionHeader(
                    stringResource(R.string.upcoming_payments),
                    Modifier.contentWidth().staggeredEntrance(3, playIntro),
                ) {
                    TextButton(onClick = onOpenReminders) { Text(stringResource(R.string.see_all)) }
                }
            }
            if (!state.isLoading && state.upcoming.isEmpty()) {
                item(key = "reminderCta") {
                    ReminderCta(onAddReminder, Modifier.contentWidth().animateItem().staggeredEntrance(4, playIntro))
                }
            }
            items(state.upcoming, key = { "reminder-${it.id}" }) { reminder ->
                ReminderCard(
                    reminder = reminder,
                    today = LocalDate.now(),
                    onClick = onOpenReminders,
                    modifier = Modifier.contentWidth().animateItem().staggeredEntrance(4, playIntro),
                )
            }
            item(key = "recentHeader") {
                SectionHeader(
                    stringResource(R.string.recent_transactions),
                    Modifier.contentWidth().padding(top = 8.dp).staggeredEntrance(5, playIntro),
                ) {
                    Chip(state.transactions.size.toString())
                }
            }
            if (!state.isLoading && state.transactions.isEmpty()) {
                item(key = "empty") {
                    EmptyState(Modifier.contentWidth().animateItem().staggeredEntrance(6, playIntro))
                }
            }
            items(state.transactions, key = { it.id }) { transaction ->
                SwipeToDelete(
                    onDelete = {
                        viewModel.delete(transaction)
                        scope.launch {
                            snackbar.currentSnackbarData?.dismiss()
                            val result = snackbar.showSnackbar(deletedText, undoText, duration = SnackbarDuration.Short)
                            if (result == SnackbarResult.ActionPerformed) viewModel.restore(transaction)
                        }
                    },
                    modifier = Modifier.contentWidth().animateItem(),
                ) {
                    TransactionRow(transaction)
                }
            }
        }
    }
}

/** Keeps content readable on tablets and in landscape. */
private fun Modifier.contentWidth() = widthIn(max = 640.dp).fillMaxWidth()

@Composable
private fun Header(modifier: Modifier) {
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> R.string.greeting_morning
        in 12..17 -> R.string.greeting_day
        else -> R.string.greeting_evening
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(greeting), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.overview_title), style = MaterialTheme.typography.headlineSmall)
        }
        Chip(DateFormat.chip(LocalDate.now()))
    }
}

@Composable
private fun Chip(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun BalanceCard(state: DashboardUiState, modifier: Modifier) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier
            .clip(shape)
            .background(Brush.linearGradient(FinanceTheme.colors.gradient)),
    ) {
        // Decorative shapes, clipped by the card.
        Box(
            Modifier.align(Alignment.TopEnd).offset(x = 70.dp, y = (-70).dp).size(220.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape),
        )
        Box(
            Modifier.align(Alignment.BottomStart).offset(x = (-50).dp, y = 60.dp).size(140.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape),
        )
        Column(Modifier.padding(24.dp)) {
            Text(
                stringResource(R.string.trenutno_stanje),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                MoneyFormat.format(animatedAmount(state.balance)),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TotalPill(true, state.income, Modifier.weight(1f))
                TotalPill(false, state.expense, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TotalPill(isIncome: Boolean, amount: Double, modifier: Modifier) {
    val glass = RoundedCornerShape(20.dp)
    Row(
        modifier
            .background(Color.White.copy(alpha = 0.15f), glass)
            .border(1.dp, Color.White.copy(alpha = 0.25f), glass)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TransactionTypeBadge(isIncome, size = 32.dp, container = Color.White.copy(alpha = 0.2f), tint = Color.White)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                stringResource(if (isIncome) R.string.prihodi else R.string.rashodi),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f),
            )
            Text(
                MoneyFormat.format(amount),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ActionCard(isIncome: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        onClick = onClick,
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Column(Modifier.padding(16.dp)) {
            Box(
                Modifier.size(44.dp).background(
                    if (isIncome) FinanceTheme.colors.incomeContainer else FinanceTheme.colors.expenseContainer,
                    CircleShape,
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    tint = if (isIncome) FinanceTheme.colors.income else FinanceTheme.colors.expense,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(if (isIncome) R.string.prihod else R.string.rashod), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(if (isIncome) R.string.dodaj_prihod else R.string.dodaj_rashod),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier, trailing: @Composable () -> Unit) {
    Row(modifier.heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun ReminderCta(onClick: () -> Unit, modifier: Modifier) {
    Surface(
        onClick = onClick,
        shape = CardShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_bell),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.add_reminder_cta),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    stringResource(R.string.add_reminder_cta_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(painterResource(R.drawable.ic_add), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier) {
    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Column(Modifier.padding(vertical = 36.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(64.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_wallet),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}


@Composable
private fun TransactionRow(transaction: Transaction) {
    val isIncome = transaction.type == TransactionType.INCOME
    val today = LocalDate.now()
    val dateText = when (transaction.date) {
        today -> stringResource(R.string.today)
        today.minusDays(1) -> stringResource(R.string.yesterday)
        else -> DateFormat.short(transaction.date)
    }
    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            TransactionTypeBadge(isIncome)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    transaction.category.ifBlank { stringResource(if (isIncome) R.string.prihod else R.string.rashod) },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOf(dateText, transaction.note).filter { it.isNotBlank() }.joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
