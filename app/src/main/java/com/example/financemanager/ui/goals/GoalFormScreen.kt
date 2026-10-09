package com.example.financemanager.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.financemanager.R
import com.example.financemanager.ui.DateFormat
import com.example.financemanager.ui.components.ColorPicker
import com.example.financemanager.ui.components.DatePickerSheet
import com.example.financemanager.ui.components.FieldShape
import com.example.financemanager.ui.components.shakeOn
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalFormScreen(viewModel: GoalFormViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)
    var pickDeadline by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) { viewModel.saved.collect { currentOnBack() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (viewModel.isEditing) R.string.edit_goal else R.string.new_goal)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.natrag))
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
                label = { Text(stringResource(R.string.goal_name)) },
                placeholder = { Text(stringResource(R.string.goal_name_hint)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_savings), contentDescription = null) },
                isError = state.nameError,
                supportingText = if (state.nameError) {
                    { Text(stringResource(R.string.error_goal_name)) }
                } else {
                    null
                },
                singleLine = true,
                shape = FieldShape,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                modifier = field.shakeOn(state.nameShake),
            )

            OutlinedTextField(
                value = state.target,
                onValueChange = viewModel::onTargetChange,
                label = { Text(stringResource(R.string.goal_target)) },
                placeholder = { Text(stringResource(R.string.amount_hint)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_wallet), contentDescription = null) },
                suffix = { Text(stringResource(R.string.currency_symbol)) },
                isError = state.targetError,
                supportingText = if (state.targetError) {
                    { Text(stringResource(R.string.error_amount)) }
                } else {
                    null
                },
                singleLine = true,
                shape = FieldShape,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                modifier = field.shakeOn(state.targetShake),
            )

            DeadlineField(
                deadline = state.deadline,
                onPick = { pickDeadline = true },
                onClear = { viewModel.onDeadlineChange(null) },
                modifier = field,
            )

            if (!viewModel.isEditing) {
                OutlinedTextField(
                    value = state.alreadySaved,
                    onValueChange = viewModel::onAlreadySavedChange,
                    label = { Text(stringResource(R.string.goal_already_saved)) },
                    placeholder = { Text(stringResource(R.string.amount_hint)) },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                    suffix = { Text(stringResource(R.string.currency_symbol)) },
                    singleLine = true,
                    shape = FieldShape,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    modifier = field,
                )
            }

            Column(field) {
                Text(
                    stringResource(R.string.category_color),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
                // Until the user picks one, a new goal gets its colour from its id when saved.
                ColorPicker(selected = state.color ?: -1, onSelect = viewModel::onColorChange)
            }
        }
    }

    if (pickDeadline) {
        DatePickerSheet(
            initial = state.deadline ?: LocalDate.now().plusMonths(6),
            onDismiss = { pickDeadline = false },
            onConfirm = {
                viewModel.onDeadlineChange(it)
                pickDeadline = false
            },
        )
    }
}

/** Optional date: "No deadline" until one is picked, with a button to remove it again. */
@Composable
private fun DeadlineField(deadline: LocalDate?, onPick: () -> Unit, onClear: () -> Unit, modifier: Modifier) {
    Box(modifier) {
        OutlinedTextField(
            value = deadline?.let(DateFormat::long) ?: stringResource(R.string.no_deadline),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.goal_deadline_optional)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_calendar), contentDescription = null) },
            shape = FieldShape,
            modifier = Modifier.fillMaxWidth(),
        )
        // Catches taps on the field (but not on the clear button) to open the picker.
        Surface(
            onClick = onPick,
            color = Color.Transparent,
            shape = FieldShape,
            modifier = Modifier.matchParentSize().padding(top = 8.dp, end = if (deadline != null) 56.dp else 0.dp),
        ) {}
        if (deadline != null) {
            IconButton(onClick = onClear, modifier = Modifier.align(Alignment.CenterEnd).padding(top = 8.dp, end = 4.dp)) {
                Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.remove_deadline))
            }
        }
    }
}
