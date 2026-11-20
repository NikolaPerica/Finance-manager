package com.example.financemanager.ui.wishes

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.financemanager.R
import com.example.financemanager.domain.DateFormat
import com.example.financemanager.domain.MoneyFormat
import com.example.financemanager.domain.isValidAmountInput
import com.example.financemanager.model.Category
import com.example.financemanager.model.SavingsGoal
import com.example.financemanager.model.Wish
import com.example.financemanager.model.WishStatus
import com.example.financemanager.ui.components.AddCategoryDialog
import com.example.financemanager.ui.components.CategoryNameError
import com.example.financemanager.ui.components.SwipeToDelete
import com.example.financemanager.ui.stats.Choice
import com.example.financemanager.ui.theme.FinanceTheme
import kotlinx.coroutines.launch
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(viewModel: WishlistViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    var adding by rememberSaveable { mutableStateOf(false) }
    // The wish being given up while there are goals its price could go into.
    var skipping by remember { mutableStateOf<Wish?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            launch {
                snackbar.currentSnackbarData?.dismiss()
                when (event) {
                    is WishEvent.Bought -> {
                        val result = snackbar.showSnackbar(
                            resources.getString(R.string.wish_bought, event.before.name, MoneyFormat.format(event.expense.amount)),
                            resources.getString(R.string.undo),
                            duration = SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) viewModel.undoBuy(event)
                    }
                    is WishEvent.Skipped -> snackbar.showSnackbar(
                        event.goal?.let { resources.getString(R.string.wish_skipped_to_goal, MoneyFormat.format(event.wish.price), it.name) }
                            ?: resources.getString(R.string.wish_skipped, MoneyFormat.format(event.wish.price)),
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.wishes_title)) },
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
                onClick = { adding = true },
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(R.string.wish_add)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val list = state.list
        val deletedText = stringResource(R.string.wish_deleted)
        val undoText = stringResource(R.string.undo)
        fun delete(wish: Wish) {
            viewModel.delete(wish)
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                if (snackbar.showSnackbar(deletedText, undoText, duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) {
                    viewModel.restore(wish)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "saved") { Saved(state, Modifier.contentWidth()) }
            if (list.waiting.isNotEmpty()) {
                item(key = "waitingHeader") { Header(stringResource(R.string.wishes_waiting), Modifier.contentWidth()) }
            }
            items(list.waiting, key = { it.id }) { wish ->
                SwipeToDelete(onDelete = { delete(wish) }, modifier = Modifier.contentWidth().animateItem()) {
                    WaitingWish(
                        wish,
                        state,
                        onBuy = { viewModel.buy(wish) },
                        onSkip = { if (state.goals.isEmpty()) viewModel.skip(wish, null) else skipping = wish },
                    )
                }
            }
            if (list.decided.isNotEmpty()) {
                item(key = "decidedHeader") { Header(stringResource(R.string.wishes_decided), Modifier.contentWidth()) }
            }
            items(list.decided, key = { it.id }) { wish ->
                SwipeToDelete(onDelete = { delete(wish) }, modifier = Modifier.contentWidth().animateItem()) { DecidedWish(wish) }
            }
            if (!state.isLoading && list.waiting.isEmpty() && list.decided.isEmpty()) {
                item(key = "empty") {
                    Text(
                        stringResource(R.string.wishes_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.contentWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }

    if (adding) {
        AddWishDialog(
            categories = state.categories,
            validateCategory = viewModel::validateCategoryName,
            onAddCategory = viewModel::addCategory,
            onAdd = { name, price, wait, category -> if (viewModel.add(name, price, wait, category)) adding = false },
            onDismiss = { adding = false },
        )
    }
    skipping?.let { wish ->
        SkipDialog(
            wish,
            state.goals,
            onSkip = { goal ->
                viewModel.skip(wish, goal)
                skipping = null
            },
            onDismiss = { skipping = null },
        )
    }
}

@Composable
private fun Saved(state: WishlistUiState, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = modifier) {
        Column(Modifier.padding(20.dp)) {
            Text(
                stringResource(R.string.wishes_saved_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                MoneyFormat.format(state.list.savedBySkipping),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                stringResource(R.string.wishes_saved_counts, state.list.skipped, state.list.bought),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun Header(text: String, modifier: Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = modifier.padding(top = 8.dp))
}

@Composable
private fun WaitingWish(wish: Wish, state: WishlistUiState, onBuy: () -> Unit, onSkip: () -> Unit) {
    val ready = wish.isReady(state.today)
    val daysLeft = ChronoUnit.DAYS.between(state.today, wish.decideOn).toInt()
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(wish.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(
                        if (ready) {
                            stringResource(R.string.wish_ready)
                        } else {
                            pluralStringResource(R.plurals.wish_days_left, daysLeft, daysLeft)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(MoneyFormat.format(wish.price), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSkip, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.wish_skip)) }
                OutlinedButton(onClick = onBuy, enabled = ready, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.wish_buy)) }
            }
        }
    }
}

@Composable
private fun DecidedWish(wish: Wish) {
    val bought = wish.status == WishStatus.BOUGHT
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(wish.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                Text(
                    stringResource(
                        if (bought) R.string.wish_status_bought else R.string.wish_status_skipped,
                        wish.decidedOn?.let(DateFormat::short).orEmpty(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (bought) MaterialTheme.colorScheme.onSurfaceVariant else FinanceTheme.colors.income,
                )
            }
            Text(MoneyFormat.format(wish.price), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWishDialog(
    categories: List<Category>,
    validateCategory: (String) -> CategoryNameError?,
    onAddCategory: (String) -> Unit,
    onAdd: (String, String, Int, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var wait by rememberSaveable { mutableIntStateOf(WaitChoices[1]) }
    var category by rememberSaveable { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var addingCategory by rememberSaveable { mutableStateOf(false) }
    val none = stringResource(R.string.no_category)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.wish_add_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.wish_add_message), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.wish_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { if (isValidAmountInput(it)) price = it },
                    label = { Text(stringResource(R.string.wish_price)) },
                    suffix = { Text(MoneyFormat.symbol) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = category.ifBlank { none },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.wish_category)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        (listOf("") + categories.map { it.name }).forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.ifBlank { none }) },
                                onClick = {
                                    category = option
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.nova_kategorija), color = MaterialTheme.colorScheme.primary) },
                            leadingIcon = {
                                Icon(painterResource(R.drawable.ic_add), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            onClick = {
                                expanded = false
                                addingCategory = true
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
                Text(stringResource(R.string.wish_wait), style = MaterialTheme.typography.labelLarge)
                Choice(
                    WaitChoices.map { it to waitLabel(it) },
                    wait,
                    { wait = it },
                    Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(name, price, wait, category) }, enabled = name.isNotBlank() && price.isNotBlank()) {
                Text(stringResource(R.string.spremi))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.odustani)) } },
    )

    if (addingCategory) {
        AddCategoryDialog(
            validate = validateCategory,
            onDismiss = { addingCategory = false },
            onConfirm = {
                onAddCategory(it)
                category = it.trim()
                addingCategory = false
            },
        )
    }
}

private fun waitLabel(days: Int) = when (days) {
    1 -> R.string.wish_wait_1
    2 -> R.string.wish_wait_2
    else -> R.string.wish_wait_3
}

/** Giving a wish up: its price can go straight into a savings goal. */
@Composable
private fun SkipDialog(wish: Wish, goals: List<SavingsGoal>, onSkip: (SavingsGoal?) -> Unit, onDismiss: () -> Unit) {
    // null keeps the money in the pocket.
    var goalId by rememberSaveable { mutableStateOf<Long?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.wish_skip_title, wish.name)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(
                        if (goals.isEmpty()) R.string.wish_skip_message_no_goals else R.string.wish_skip_message,
                        MoneyFormat.format(wish.price),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (goals.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    SkipTarget(stringResource(R.string.wish_skip_keep), R.drawable.ic_wallet, goalId == null) { goalId = null }
                    goals.forEach { goal ->
                        SkipTarget(stringResource(R.string.wish_skip_to_goal, goal.name), R.drawable.ic_savings, goalId == goal.id) {
                            goalId = goal.id
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSkip(goals.firstOrNull { it.id == goalId }) }) { Text(stringResource(R.string.wish_skip_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.wish_skip_back)) } },
    )
}

@Composable
private fun SkipTarget(text: String, icon: Int, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(horizontal = 8.dp))
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

private fun Modifier.contentWidth() = widthIn(max = 640.dp).fillMaxWidth()
