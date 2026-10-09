package com.example.financemanager.ui.goals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.financemanager.R
import com.example.financemanager.data.GoalContribution
import com.example.financemanager.ui.DateFormat
import com.example.financemanager.ui.MoneyFormat
import com.example.financemanager.ui.budgets.BudgetBar
import com.example.financemanager.ui.components.FieldShape
import com.example.financemanager.ui.components.SwipeToDelete
import com.example.financemanager.ui.components.TransactionTypeBadge
import com.example.financemanager.ui.isValidAmountInput
import com.example.financemanager.ui.theme.FinanceTheme
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(viewModel: GoalDetailViewModel, onBack: () -> Unit, onEdit: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val progress = state.progress
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val currentOnBack by rememberUpdatedState(onBack)
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    // null: no dialog; false: deposit; true: withdrawal.
    var moneyDialog by rememberSaveable { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                GoalEvent.Reached -> {
                    snackbar.currentSnackbarData?.dismiss()
                    snackbar.showSnackbar(resources.getString(R.string.goal_reached_message))
                }
                is GoalEvent.ContributionDeleted -> {
                    snackbar.currentSnackbarData?.dismiss()
                    val result = snackbar.showSnackbar(
                        resources.getString(R.string.contribution_deleted),
                        resources.getString(R.string.undo),
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.restoreContribution(event.contribution)
                }
                GoalEvent.GoalDeleted -> currentOnBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(progress?.goal?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.natrag))
                    }
                },
                actions = {
                    if (progress != null) {
                        IconButton(onClick = onEdit) {
                            Icon(painterResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.edit_goal))
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.delete))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (progress == null) return@Scaffold
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "progress") { ProgressCard(progress, Modifier.contentWidth()) }
            item(key = "actions") {
                Row(Modifier.contentWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { moneyDialog = false },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.deposit))
                    }
                    OutlinedButton(
                        onClick = { moneyDialog = true },
                        enabled = progress.saved > 0,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) {
                        Text(stringResource(R.string.withdraw))
                    }
                }
            }
            item(key = "historyTitle") {
                Text(
                    stringResource(R.string.goal_history),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.contentWidth().padding(top = 8.dp),
                )
            }
            if (!state.isLoading && state.contributions.isEmpty()) {
                item(key = "noHistory") {
                    Text(
                        stringResource(R.string.goal_history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.contentWidth(),
                    )
                }
            }
            items(state.contributions, key = { it.id }) { contribution ->
                SwipeToDelete(
                    onDelete = { viewModel.deleteContribution(contribution) },
                    modifier = Modifier.contentWidth().animateItem(),
                ) {
                    ContributionRow(contribution)
                }
            }
        }
    }

    moneyDialog?.let { withdraw ->
        MoneyDialog(
            withdraw = withdraw,
            validate = { viewModel.validate(it, withdraw) },
            onDismiss = { moneyDialog = null },
            onConfirm = {
                viewModel.contribute(it, withdraw)
                moneyDialog = null
            },
        )
    }
    if (confirmDelete && progress != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_goal_title, progress.goal.name)) },
            text = { Text(stringResource(R.string.delete_goal_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteGoal()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.odustani)) } },
        )
    }
}

private fun Modifier.contentWidth() = widthIn(max = 640.dp).fillMaxWidth()

@Composable
private fun ProgressCard(progress: GoalProgress, modifier: Modifier) {
    val color = goalColor(progress.goal)
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.saved_so_far),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                GoalStatusChip(progress.status)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(MoneyFormat.format(progress.saved), style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.budget_of, MoneyFormat.format(progress.goal.target)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${(progress.fraction * 100).roundToInt()} %",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            BudgetBar(progress.fraction, color = color, track = MaterialTheme.colorScheme.surfaceContainerHighest)
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))
            InfoRow(
                stringResource(R.string.goal_deadline),
                progress.goal.deadline?.let { DateFormat.long(it) } ?: stringResource(R.string.no_deadline),
            )
            InfoRow(stringResource(R.string.goal_remaining), MoneyFormat.format(progress.remaining))
            progress.monthlyNeeded?.let {
                InfoRow(stringResource(R.string.goal_per_month), MoneyFormat.format(it), emphasize = true)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, emphasize: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(
            value,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ContributionRow(contribution: GoalContribution) {
    val deposit = contribution.amount >= 0
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Money going into the goal points down, like income; taking it out points up.
            TransactionTypeBadge(isIncome = deposit, size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(if (deposit) R.string.deposit_noun else R.string.withdrawal_noun), style = MaterialTheme.typography.titleSmall)
                Text(
                    DateFormat.short(contribution.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                MoneyFormat.signed(abs(contribution.amount), isIncome = deposit),
                style = MaterialTheme.typography.titleMedium,
                color = if (deposit) FinanceTheme.colors.income else FinanceTheme.colors.expense,
            )
        }
    }
}

@Composable
private fun MoneyDialog(
    withdraw: Boolean,
    validate: (String) -> ContributionError?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<ContributionError?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    fun submit() {
        error = validate(text)
        if (error == null) onConfirm(text)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (withdraw) R.string.withdraw_title else R.string.deposit_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    if (isValidAmountInput(it)) {
                        text = it
                        error = null
                    }
                },
                label = { Text(stringResource(R.string.iznos)) },
                placeholder = { Text(stringResource(R.string.amount_hint)) },
                suffix = { Text(stringResource(R.string.currency_symbol)) },
                isError = error != null,
                supportingText = error?.let {
                    {
                        Text(
                            stringResource(
                                when (it) {
                                    ContributionError.AMOUNT -> R.string.error_amount
                                    ContributionError.MORE_THAN_SAVED -> R.string.error_more_than_saved
                                },
                            ),
                        )
                    }
                },
                singleLine = true,
                shape = FieldShape,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.focusRequester(focus),
            )
        },
        confirmButton = {
            TextButton(onClick = ::submit) { Text(stringResource(if (withdraw) R.string.withdraw else R.string.deposit)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.odustani)) } },
    )
}
