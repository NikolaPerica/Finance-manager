package com.example.financemanager.ui.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.financemanager.R
import com.example.financemanager.data.Category
import com.example.financemanager.data.PaymentPeriod
import com.example.financemanager.ui.components.DateField
import com.example.financemanager.ui.components.DatePickerSheet
import com.example.financemanager.ui.components.FieldShape
import com.example.financemanager.ui.components.shakeOn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderFormScreen(viewModel: ReminderFormViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) { viewModel.done.collect { currentOnBack() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (viewModel.isEditing) R.string.edit_reminder else R.string.new_reminder)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.natrag))
                    }
                },
                actions = {
                    if (viewModel.isEditing) {
                        IconButton(onClick = viewModel::delete) {
                            Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.delete))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            Button(
                onClick = viewModel::save,
                enabled = state.isLoaded && !state.isSaving,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .height(56.dp),
            ) {
                Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.spremi), style = MaterialTheme.typography.labelLarge)
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val field = Modifier.widthIn(max = 640.dp).fillMaxWidth()

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.reminder_name)) },
                placeholder = { Text(stringResource(R.string.reminder_name_hint)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_bell), contentDescription = null) },
                isError = state.nameError,
                supportingText = if (state.nameError) {
                    { Text(stringResource(R.string.error_reminder_name)) }
                } else {
                    null
                },
                singleLine = true,
                shape = FieldShape,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                modifier = field.shakeOn(state.nameShake),
            )

            OutlinedTextField(
                value = state.amount,
                onValueChange = viewModel::onAmountChange,
                label = { Text(stringResource(R.string.iznos)) },
                placeholder = { Text(stringResource(R.string.amount_hint)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_wallet), contentDescription = null) },
                suffix = { Text(stringResource(R.string.currency_symbol)) },
                isError = state.amountError,
                supportingText = if (state.amountError) {
                    { Text(stringResource(R.string.error_amount)) }
                } else {
                    null
                },
                singleLine = true,
                shape = FieldShape,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                modifier = field.shakeOn(state.amountShake),
            )

            DateField(
                date = state.dueDate,
                onClick = { showDatePicker = true },
                label = R.string.reminder_due_date,
                modifier = field,
            )

            Column(field) {
                Text(
                    stringResource(R.string.reminder_period),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentPeriod.entries.forEach { period ->
                        FilterChip(
                            selected = state.period == period,
                            onClick = { viewModel.onPeriodChange(period) },
                            label = { Text(stringResource(period.label)) },
                            leadingIcon = if (state.period == period) {
                                { Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else {
                                null
                            },
                        )
                    }
                }
            }

            CategoryPicker(categories, state.category, viewModel::onCategoryChange, field)

            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text(stringResource(R.string.napomena)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_notes), contentDescription = null) },
                shape = FieldShape,
                minLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = field,
            )
        }
    }

    if (showDatePicker) {
        DatePickerSheet(
            initial = state.dueDate,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                viewModel.onDueDateChange(it)
                showDatePicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPicker(
    categories: List<Category>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val none = stringResource(R.string.no_category)
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected.ifBlank { none },
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.reminder_category)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_label), contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = FieldShape,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            (listOf("") + categories.map { it.name }).forEach { name ->
                DropdownMenuItem(
                    text = { Text(name.ifBlank { none }) },
                    onClick = {
                        onSelect(name)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
