package com.example.financemanager.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.financemanager.R
import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import com.example.financemanager.ui.Croatian
import com.example.financemanager.ui.DateFormat
import com.example.financemanager.ui.MoneyFormat
import com.example.financemanager.ui.components.CategoryDot
import com.example.financemanager.ui.components.FieldShape
import com.example.financemanager.ui.components.SwipeToDelete
import com.example.financemanager.ui.components.TransactionRow
import com.example.financemanager.ui.components.categoryColor
import com.example.financemanager.ui.components.rememberCategoryColors
import com.example.financemanager.ui.theme.FinanceTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(viewModel: TransactionsViewModel, onBack: () -> Unit, onEdit: (Transaction) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val filter = state.filter
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedText = stringResource(R.string.transaction_deleted)
    val undoText = stringResource(R.string.undo)
    val categoryColors = rememberCategoryColors(state.categories)
    var pickRange by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.transactions_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.natrag))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "search") {
                OutlinedTextField(
                    value = filter.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
                    trailingIcon = if (filter.query.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.clear_search))
                            }
                        }
                    } else {
                        null
                    },
                    singleLine = true,
                    shape = FieldShape,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Search),
                    modifier = Modifier.padded(),
                )
            }
            item(key = "type") {
                val options = listOf(null to R.string.range_all, TransactionType.INCOME to R.string.prihodi, TransactionType.EXPENSE to R.string.rashodi)
                SingleChoiceSegmentedButtonRow(Modifier.padded()) {
                    options.forEachIndexed { index, (type, label) ->
                        SegmentedButton(
                            selected = filter.type == type,
                            onClick = { viewModel.onTypeChange(type) },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        ) { Text(stringResource(label)) }
                    }
                }
            }
            item(key = "filters") {
                FilterRow(
                    state = state,
                    onPeriod = viewModel::onPeriodChange,
                    onPickRange = { pickRange = true },
                    onCategory = viewModel::onCategoryChange,
                )
            }
            item(key = "summary") {
                Summary(state, onClear = viewModel::clearFilters, modifier = Modifier.padded())
            }
            state.groups.forEach { group ->
                item(key = "day-${group.date}") {
                    Text(
                        dayLabel(group.date),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padded().padding(top = 8.dp).animateItem(),
                    )
                }
                items(group.transactions, key = { it.id }) { transaction ->
                    SwipeToDelete(
                        onDelete = {
                            viewModel.delete(transaction)
                            scope.launch {
                                snackbar.currentSnackbarData?.dismiss()
                                val result = snackbar.showSnackbar(deletedText, undoText, duration = SnackbarDuration.Short)
                                if (result == SnackbarResult.ActionPerformed) viewModel.restore(transaction)
                            }
                        },
                        modifier = Modifier.padded().animateItem(),
                    ) {
                        TransactionRow(
                            transaction = transaction,
                            onClick = { onEdit(transaction) },
                            categoryColor = categoryColors(transaction),
                            showDate = false,
                        )
                    }
                }
            }
        }
    }

    if (pickRange) {
        DateRangeDialog(
            initial = filter.period as? PeriodFilter.Range,
            onDismiss = { pickRange = false },
            onConfirm = {
                viewModel.onPeriodChange(it)
                pickRange = false
            },
        )
    }
}

private fun Modifier.padded() = padding(horizontal = 20.dp).widthIn(max = 640.dp).fillMaxWidth()

@Composable
private fun FilterRow(
    state: TransactionsUiState,
    onPeriod: (PeriodFilter) -> Unit,
    onPickRange: () -> Unit,
    onCategory: (String?) -> Unit,
) {
    val filter = state.filter
    val periods = listOf(
        PeriodFilter.All to R.string.all_time,
        PeriodFilter.ThisMonth to R.string.this_month,
        PeriodFilter.LastMonth to R.string.last_month,
        PeriodFilter.ThisYear to R.string.this_year,
    )
    var categoryMenu by remember { mutableStateOf(false) }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
    ) {
        item(key = "category") {
            Box {
                val selected = filter.category
                FilterChip(
                    selected = selected != null,
                    onClick = { if (selected != null) onCategory(null) else categoryMenu = true },
                    label = { Text(selected ?: stringResource(R.string.kategorija)) },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_label), contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = if (selected != null) {
                        {
                            Icon(
                                painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.clear_category_filter),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    } else {
                        null
                    },
                    enabled = state.categories.isNotEmpty() || selected != null,
                )
                DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                    state.categories.distinctBy { it.name }.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            leadingIcon = { CategoryDot(categoryColor(category)) },
                            onClick = {
                                categoryMenu = false
                                onCategory(category.name)
                            },
                        )
                    }
                }
            }
        }
        items(periods, key = { it.second }) { (period, label) ->
            FilterChip(
                selected = filter.period == period,
                onClick = { onPeriod(period) },
                label = { Text(stringResource(label)) },
            )
        }
        item(key = "range") {
            val range = filter.period as? PeriodFilter.Range
            FilterChip(
                selected = range != null,
                onClick = onPickRange,
                label = {
                    Text(range?.let { "${ShortDate.format(it.from)} – ${ShortDate.format(it.to)}" } ?: stringResource(R.string.date_range))
                },
                leadingIcon = { Icon(painterResource(R.drawable.ic_calendar), contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
        }
    }
}

@Composable
private fun Summary(state: TransactionsUiState, onClear: () -> Unit, modifier: Modifier) {
    if (state.isLoading) return
    if (state.count == 0) {
        Column(modifier.padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(if (state.hasAny) R.string.no_matching_transactions else R.string.empty_title),
                style = MaterialTheme.typography.titleMedium,
            )
            if (state.filter.isActive) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onClear) { Text(stringResource(R.string.clear_filters)) }
            }
        }
        return
    }
    Column(modifier) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            pluralStringResource(R.plurals.transactions_count, state.count, state.count),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (state.income > 0) {
            Text(
                MoneyFormat.signed(state.income, isIncome = true),
                style = MaterialTheme.typography.labelLarge,
                color = FinanceTheme.colors.income,
            )
        }
        if (state.income > 0 && state.expense > 0) Spacer(Modifier.size(12.dp))
        if (state.expense > 0) {
            Text(
                MoneyFormat.signed(state.expense, isIncome = false),
                style = MaterialTheme.typography.labelLarge,
                color = FinanceTheme.colors.expense,
            )
        }
    }
    if (state.filter.isActive) {
        TextButton(onClick = onClear, contentPadding = PaddingValues(0.dp)) { Text(stringResource(R.string.clear_filters)) }
    }
    }
}

@Composable
private fun dayLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> stringResource(R.string.today)
        today.minusDays(1) -> stringResource(R.string.yesterday)
        else -> DayTitle.format(date).replaceFirstChar { it.titlecase(Croatian) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeDialog(initial: PeriodFilter.Range?, onDismiss: () -> Unit, onConfirm: (PeriodFilter.Range) -> Unit) {
    // The Material date pickers work in UTC milliseconds.
    fun LocalDate.millis() = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    fun Long.date() = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initial?.from?.millis(),
        initialSelectedEndDateMillis = initial?.to?.millis(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            val start = pickerState.selectedStartDateMillis
            TextButton(
                enabled = start != null,
                onClick = {
                    if (start != null) {
                        val from = start.date()
                        onConfirm(PeriodFilter.Range(from, pickerState.selectedEndDateMillis?.date() ?: from))
                    }
                },
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.odustani)) } },
    ) {
        DateRangePicker(
            state = pickerState,
            title = {
                Text(stringResource(R.string.date_range), modifier = Modifier.padding(start = 64.dp, end = 12.dp, top = 16.dp))
            },
            modifier = Modifier.weight(1f),
        )
    }
}

private val ShortDate = DateTimeFormatter.ofPattern("d. M.", Croatian)
private val DayTitle = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy.", Croatian)
