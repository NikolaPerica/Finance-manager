package com.example.financemanager.ui.budgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.financemanager.R
import com.example.financemanager.ui.Croatian
import com.example.financemanager.ui.MoneyFormat
import com.example.financemanager.ui.amountInputText
import com.example.financemanager.ui.components.AddCategoryDialog
import com.example.financemanager.ui.components.FieldShape
import com.example.financemanager.ui.isValidAmountInput
import com.example.financemanager.ui.theme.FinanceTheme
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val CardShape = RoundedCornerShape(24.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(viewModel: BudgetsViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<BudgetItem?>(null) }
    var showAddCategory by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.budgets_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.natrag))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddCategory = true },
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(R.string.nova_kategorija)) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "summary") {
                val summary = state.summary
                if (summary != null) {
                    SummaryCard(state.month, summary, Modifier.contentWidth())
                } else if (!state.isLoading) {
                    IntroCard(Modifier.contentWidth())
                }
            }
            if (state.budgeted.isNotEmpty()) {
                item(key = "budgetedHeader") { SectionTitle(stringResource(R.string.with_budget), Modifier.contentWidth()) }
                items(state.budgeted, key = { it.category.id }) { item ->
                    BudgetCard(item, onClick = { editing = item }, modifier = Modifier.contentWidth().animateItem())
                }
            }
            if (state.unbudgeted.isNotEmpty()) {
                item(key = "unbudgetedHeader") {
                    SectionTitle(stringResource(R.string.without_budget), Modifier.contentWidth().padding(top = 8.dp))
                }
                items(state.unbudgeted, key = { it.category.id }) { item ->
                    UnbudgetedRow(item, onSet = { editing = item }, modifier = Modifier.contentWidth().animateItem())
                }
            }
            if (!state.isLoading && state.budgeted.isEmpty() && state.unbudgeted.isEmpty()) {
                item(key = "empty") {
                    Text(
                        stringResource(R.string.budgets_no_categories),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.contentWidth().padding(vertical = 8.dp),
                    )
                }
            }
        }
    }

    editing?.let { item ->
        BudgetDialog(
            item = item,
            isValid = viewModel::isValidBudget,
            onDismiss = { editing = null },
            onSave = {
                viewModel.setBudget(item.category, it)
                editing = null
            },
        )
    }
    if (showAddCategory) {
        AddCategoryDialog(
            validate = viewModel::validateCategoryName,
            onDismiss = { showAddCategory = false },
            onConfirm = {
                viewModel.addCategory(it)
                showAddCategory = false
            },
        )
    }
}

private fun Modifier.contentWidth() = widthIn(max = 640.dp).fillMaxWidth()

@Composable
private fun SectionTitle(text: String, modifier: Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = modifier)
}

@Composable
private fun SummaryCard(month: YearMonth, summary: BudgetSummary, modifier: Modifier) {
    val white = Color.White
    Column(
        modifier
            .clip(CardShape)
            .background(Brush.linearGradient(FinanceTheme.colors.gradient))
            .padding(20.dp),
    ) {
        Text(
            stringResource(R.string.budget_for_month, MonthTitle.format(month)),
            style = MaterialTheme.typography.bodyLarge,
            color = white.copy(alpha = 0.75f),
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(MoneyFormat.format(summary.spent), style = MaterialTheme.typography.headlineSmall, color = white)
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.budget_of, MoneyFormat.format(summary.budget)),
                style = MaterialTheme.typography.bodyLarge,
                color = white.copy(alpha = 0.75f),
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        BudgetBar(summary.fraction, color = white, track = white.copy(alpha = 0.25f))
        Spacer(Modifier.height(10.dp))
        Text(
            remainingText(summary.budget - summary.spent),
            style = MaterialTheme.typography.bodyMedium,
            color = white,
        )
        if (summary.exceeded > 0) {
            Text(
                pluralStringResource(R.plurals.budgets_exceeded, summary.exceeded, summary.exceeded),
                style = MaterialTheme.typography.bodyMedium,
                color = white.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun IntroCard(modifier: Modifier) {
    Surface(shape = CardShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = modifier) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_wallet),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                stringResource(R.string.budgets_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun BudgetCard(item: BudgetItem, onClick: () -> Unit, modifier: Modifier) {
    val color = levelColor(item.level)
    Surface(
        onClick = onClick,
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.category.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                LevelChip(item.level)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.budget_spent_of, MoneyFormat.format(item.spent), MoneyFormat.format(item.budget ?: 0.0)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            BudgetBar(item.fraction, color = color, track = MaterialTheme.colorScheme.surfaceContainerHighest)
            Spacer(Modifier.height(8.dp))
            Text(
                remainingText(item.remaining),
                style = MaterialTheme.typography.labelMedium,
                color = if (item.level == BudgetLevel.EXCEEDED) color else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UnbudgetedRow(item: BudgetItem, onSet: () -> Unit, modifier: Modifier) {
    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.category.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(R.string.spent_this_month, MoneyFormat.format(item.spent)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onSet) { Text(stringResource(R.string.set_budget)) }
        }
    }
}

/** "Close to the limit" / "Over budget" with an icon, so the state never relies on colour alone. */
@Composable
fun LevelChip(level: BudgetLevel, modifier: Modifier = Modifier) {
    if (level == BudgetLevel.OK) return
    val color = levelColor(level)
    val container = if (level == BudgetLevel.EXCEEDED) FinanceTheme.colors.expenseContainer else FinanceTheme.colors.warningContainer
    Row(
        modifier.background(container, CircleShape).padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_warning), contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            stringResource(if (level == BudgetLevel.EXCEEDED) R.string.budget_exceeded else R.string.budget_warning),
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

@Composable
fun levelColor(level: BudgetLevel): Color = when (level) {
    BudgetLevel.OK -> MaterialTheme.colorScheme.primary
    BudgetLevel.WARNING -> FinanceTheme.colors.warning
    BudgetLevel.EXCEEDED -> FinanceTheme.colors.expense
}

@Composable
fun BudgetBar(fraction: Double, color: Color, track: Color, modifier: Modifier = Modifier) {
    val fill by animateFloatAsState(fraction.toFloat().coerceIn(0f, 1f), tween(600), label = "budgetFill")
    Box(modifier.fillMaxWidth().height(8.dp).background(track, CircleShape)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fill).background(color, CircleShape))
    }
}

@Composable
private fun remainingText(remaining: Double): String =
    if (remaining >= 0) {
        stringResource(R.string.budget_remaining, MoneyFormat.format(remaining))
    } else {
        stringResource(R.string.budget_over_by, MoneyFormat.format(-remaining))
    }

@Composable
private fun BudgetDialog(item: BudgetItem, isValid: (String) -> Boolean, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(item.budget?.let(::amountInputText).orEmpty()) }
    var error by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    fun submit() {
        if (isValid(text)) onSave(text) else error = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.category.name) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        if (isValidAmountInput(it)) {
                            text = it
                            error = false
                        }
                    },
                    label = { Text(stringResource(R.string.monthly_budget)) },
                    placeholder = { Text(stringResource(R.string.amount_hint)) },
                    suffix = { Text(stringResource(R.string.currency_symbol)) },
                    isError = error,
                    supportingText = {
                        Text(
                            if (error) {
                                stringResource(R.string.error_amount)
                            } else {
                                stringResource(R.string.spent_this_month, MoneyFormat.format(item.spent))
                            },
                        )
                    },
                    singleLine = true,
                    shape = FieldShape,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.focusRequester(focus),
                )
            }
        },
        confirmButton = { TextButton(onClick = ::submit) { Text(stringResource(R.string.spremi)) } },
        dismissButton = {
            Row {
                if (item.budget != null) {
                    TextButton(onClick = { onSave("") }) { Text(stringResource(R.string.remove_budget)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.odustani)) }
            }
        },
    )
}

private val MonthTitle = DateTimeFormatter.ofPattern("LLLL yyyy.", Croatian)
