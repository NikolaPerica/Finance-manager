package com.example.financemanager.ui.categories

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.financemanager.R
import com.example.financemanager.data.Category
import com.example.financemanager.data.TransactionType
import com.example.financemanager.ui.MoneyFormat
import com.example.financemanager.ui.components.AddCategoryDialog
import com.example.financemanager.ui.components.CategoryAvatar
import com.example.financemanager.ui.components.CategoryNameError
import com.example.financemanager.ui.components.FieldShape
import com.example.financemanager.ui.theme.FinanceTheme

private val CardShape = RoundedCornerShape(24.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(viewModel: CategoriesViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Category?>(null) }
    var deleting by remember { mutableStateOf<CategoryUsage?>(null) }
    var showAdd by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.categories_title)) },
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
                onClick = { showAdd = true },
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(R.string.nova_kategorija)) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "type") {
                val options = listOf(TransactionType.EXPENSE to R.string.rashodi, TransactionType.INCOME to R.string.prihodi)
                SingleChoiceSegmentedButtonRow(Modifier.contentWidth().padding(bottom = 6.dp)) {
                    options.forEachIndexed { index, (type, label) ->
                        SegmentedButton(
                            selected = state.type == type,
                            onClick = { viewModel.onTypeChange(type) },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        ) { Text(stringResource(label)) }
                    }
                }
            }
            if (!state.isLoading && state.items.isEmpty()) {
                item(key = "empty") {
                    Text(
                        stringResource(R.string.categories_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.contentWidth().padding(vertical = 8.dp),
                    )
                }
            }
            items(state.items, key = { it.category.id }) { usage ->
                CategoryRow(
                    usage = usage,
                    onClick = { editing = usage.category },
                    onDelete = { deleting = usage },
                    modifier = Modifier.contentWidth().animateItem(),
                )
            }
        }
    }

    editing?.let { category ->
        EditCategoryDialog(
            category = category,
            validate = { viewModel.validateName(it, except = category) },
            onDismiss = { editing = null },
            onSave = { name, color ->
                viewModel.update(category, name, color)
                editing = null
            },
        )
    }
    deleting?.let { usage ->
        DeleteCategoryDialog(
            usage = usage,
            others = state.items.map { it.category }.filter { it.id != usage.category.id },
            onDismiss = { deleting = null },
            onConfirm = { moveTo ->
                viewModel.delete(usage.category, moveTo)
                deleting = null
            },
        )
    }
    if (showAdd) {
        AddCategoryDialog(
            validate = { viewModel.validateName(it) },
            onDismiss = { showAdd = false },
            onConfirm = {
                viewModel.addCategory(it)
                showAdd = false
            },
        )
    }
}

private fun Modifier.contentWidth() = widthIn(max = 640.dp).fillMaxWidth()

@Composable
private fun CategoryRow(usage: CategoryUsage, onClick: () -> Unit, onDelete: () -> Unit, modifier: Modifier) {
    Surface(
        onClick = onClick,
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Row(Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            CategoryAvatar(usage.category)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(usage.category.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(
                        pluralStringResource(R.plurals.transactions_count, usage.count, usage.count),
                        MoneyFormat.format(usage.total).takeIf { usage.count > 0 },
                        usage.category.monthlyBudget?.let { stringResource(R.string.category_budget, MoneyFormat.format(it)) },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.delete_category, usage.category.name),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EditCategoryDialog(
    category: Category,
    validate: (String) -> CategoryNameError?,
    onDismiss: () -> Unit,
    onSave: (name: String, color: Int) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(category.name) }
    var color by rememberSaveable { mutableStateOf(category.colorIndex) }
    var error by remember { mutableStateOf<CategoryNameError?>(null) }

    fun submit() {
        error = validate(name)
        if (error == null) onSave(name, color)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_category)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    label = { Text(stringResource(R.string.naziv_kategorije)) },
                    isError = error != null,
                    supportingText = error?.let {
                        {
                            Text(
                                stringResource(
                                    when (it) {
                                        CategoryNameError.EMPTY -> R.string.error_category_name
                                        CategoryNameError.EXISTS -> R.string.error_category_exists
                                    },
                                ),
                            )
                        }
                    },
                    singleLine = true,
                    shape = FieldShape,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.category_color),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                ColorPicker(selected = color, onSelect = { color = it })
            }
        },
        confirmButton = { TextButton(onClick = ::submit) { Text(stringResource(R.string.spremi)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.odustani)) } },
    )
}

@Composable
private fun ColorPicker(selected: Int, onSelect: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FinanceTheme.colors.categories.forEachIndexed { index, color ->
            val isSelected = index == selected
            val description = stringResource(R.string.color_n, index + 1)
            Box(
                Modifier
                    .size(40.dp)
                    .border(2.dp, if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                    .padding(4.dp)
                    .background(color, CircleShape)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(index) })
                    .semantics { contentDescription = description },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun DeleteCategoryDialog(
    usage: CategoryUsage,
    others: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (moveTo: Category?) -> Unit,
) {
    var targetId by rememberSaveable { mutableStateOf<Long?>(null) }
    val target = others.find { it.id == targetId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_category_title, usage.category.name)) },
        text = {
            Column {
                if (usage.count == 0) {
                    Text(stringResource(R.string.delete_category_unused))
                } else {
                    Text(
                        stringResource(
                            R.string.delete_category_used,
                            pluralStringResource(R.plurals.transactions_count, usage.count, usage.count),
                        ),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.move_to),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState()).selectableGroup()) {
                        (listOf<Category?>(null) + others).forEach { option ->
                            TargetOption(
                                category = option,
                                selected = option?.id == targetId,
                                onSelect = { targetId = option?.id },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(target) }) {
                Text(stringResource(if (target != null) R.string.merge_category else R.string.delete))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.odustani)) } },
    )
}

@Composable
private fun TargetOption(category: Category?, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        if (category != null) {
            CategoryAvatar(category, size = 24.dp)
            Spacer(Modifier.width(10.dp))
        }
        Text(category?.name ?: stringResource(R.string.leave_uncategorized), style = MaterialTheme.typography.bodyLarge)
    }
}
