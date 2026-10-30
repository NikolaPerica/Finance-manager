package com.example.financemanager.ui.transaction

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.financemanager.R
import com.example.financemanager.domain.DateFormat
import com.example.financemanager.domain.MoneyFormat
import com.example.financemanager.domain.receipt.ReceiptParser
import com.example.financemanager.model.Category
import com.example.financemanager.model.TransactionType
import com.example.financemanager.notifications.BudgetNotifier
import com.example.financemanager.receipt.ReceiptScanner
import com.example.financemanager.ui.components.AddCategoryDialog
import com.example.financemanager.ui.components.DateField
import com.example.financemanager.ui.components.DatePickerSheet
import com.example.financemanager.ui.components.FieldShape
import com.example.financemanager.ui.components.TransactionTypeBadge
import com.example.financemanager.ui.components.shakeOn
import com.example.financemanager.ui.components.staggeredEntrance
import com.example.financemanager.ui.theme.FinanceTheme
import com.google.mlkit.common.MlKitException
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(viewModel: TransactionFormViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val isIncome = viewModel.type == TransactionType.INCOME
    val snackbar = remember { SnackbarHostState() }
    val currentOnBack by rememberUpdatedState(onBack)
    val resources = LocalResources.current
    val context = LocalContext.current
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is FormEvent.Saved -> {
                    event.budgetAlert?.let { BudgetNotifier(context).notify(it) }
                    currentOnBack()
                }
                FormEvent.Deleted -> currentOnBack()
                is FormEvent.CategoryAdded -> snackbar.showSnackbar(resources.getString(R.string.category_added, event.name))
                is FormEvent.ReceiptRead -> {
                    val receipt = event.receipt
                    val found = listOfNotNull(
                        receipt.amount?.let(MoneyFormat::format),
                        receipt.date?.let(DateFormat::short),
                        receipt.merchant,
                    )
                    snackbar.currentSnackbarData?.dismiss()
                    snackbar.showSnackbar(
                        if (receipt.amount == null) {
                            resources.getString(R.string.scan_no_amount)
                        } else {
                            resources.getString(R.string.scan_found, found.joinToString(" · "))
                        },
                        duration = SnackbarDuration.Long,
                    )
                }
            }
        }
    }

    // Receipt scanning: a photo from the camera app or the gallery, read on the phone.
    val scope = rememberCoroutineScope()
    var scanning by remember { mutableStateOf(false) }
    var chooseSource by rememberSaveable { mutableStateOf(false) }
    var photoPath by rememberSaveable { mutableStateOf<String?>(null) }

    fun scan(uri: Uri, deleteAfter: File?) {
        scanning = true
        scope.launch {
            try {
                val lines = withContext(Dispatchers.Default) { ReceiptScanner(context).read(uri) }
                viewModel.applyReceipt(
                    ReceiptParser.parse(lines),
                    ReceiptParser.guessCategory(lines, categories),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: MlKitException) {
                val message = if (e.errorCode == MlKitException.UNAVAILABLE) R.string.scan_model_downloading else R.string.scan_failed
                snackbar.showSnackbar(resources.getString(message))
            } catch (_: Exception) {
                snackbar.showSnackbar(resources.getString(R.string.scan_failed))
            } finally {
                scanning = false
                deleteAfter?.delete()
            }
        }
    }

    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val file = photoPath?.let(::File)
        if (saved && file != null) scan(Uri.fromFile(file), deleteAfter = file) else file?.delete()
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scan(uri, deleteAfter = null)
    }

    var introPlayed by rememberSaveable { mutableStateOf(false) }
    val playIntro = !introPlayed
    LaunchedEffect(Unit) { introPlayed = true }

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showAddCategory by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(
                                when {
                                    viewModel.isEditing && isIncome -> R.string.uredi_prihod
                                    viewModel.isEditing -> R.string.uredi_rashod
                                    isIncome -> R.string.novi_prihod
                                    else -> R.string.novi_rashod
                                },
                            ),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.natrag))
                        }
                    },
                    actions = {
                        if (!isIncome) {
                            IconButton(onClick = { chooseSource = true }, enabled = !scanning) {
                                Icon(painterResource(R.drawable.ic_receipt), contentDescription = stringResource(R.string.scan_receipt))
                            }
                        }
                        if (viewModel.isEditing) {
                            IconButton(onClick = { confirmDelete = true }) {
                                Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.delete))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
                // Reading a receipt takes a moment.
                if (scanning) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
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
        snackbarHost = { SnackbarHost(snackbar) },
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

            AmountCard(
                isIncome = isIncome,
                amount = state.amount,
                error = state.amountError,
                onAmountChange = viewModel::onAmountChange,
                modifier = field.shakeOn(state.amountShake).staggeredEntrance(0, playIntro, startDelay = 120, step = 50),
            )

            DateField(
                date = state.date,
                onClick = { showDatePicker = true },
                modifier = field.staggeredEntrance(1, playIntro, startDelay = 120, step = 50),
            )

            Row(
                modifier = field.staggeredEntrance(2, playIntro, startDelay = 120, step = 50),
                verticalAlignment = Alignment.Top,
            ) {
                CategoryField(
                    categories = categories,
                    selected = state.category,
                    error = state.categoryError,
                    onSelect = viewModel::onCategoryChange,
                    modifier = Modifier.weight(1f).shakeOn(state.categoryShake),
                )
                Spacer(Modifier.width(12.dp))
                FilledTonalIconButton(
                    onClick = { showAddCategory = true },
                    modifier = Modifier.padding(top = 8.dp).size(56.dp),
                    shape = FieldShape,
                ) {
                    Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.nova_kategorija))
                }
            }

            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text(stringResource(R.string.napomena)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_notes), contentDescription = null) },
                shape = FieldShape,
                minLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = field.staggeredEntrance(3, playIntro, startDelay = 120, step = 50),
            )
        }
    }

    if (showDatePicker) {
        DatePickerSheet(
            initial = state.date,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                viewModel.onDateChange(it)
                showDatePicker = false
            },
        )
    }
    if (chooseSource) {
        AlertDialog(
            onDismissRequest = { chooseSource = false },
            icon = { Icon(painterResource(R.drawable.ic_receipt), contentDescription = null) },
            title = { Text(stringResource(R.string.scan_receipt)) },
            text = { Text(stringResource(R.string.scan_receipt_text)) },
            confirmButton = {
                TextButton(onClick = {
                    chooseSource = false
                    // The camera app saves the photo into the app's cache, shared through FileProvider.
                    val file = File(context.cacheDir, "receipts").apply { mkdirs() }.let { File(it, "receipt-${System.currentTimeMillis()}.jpg") }
                    photoPath = file.path
                    try {
                        takePhoto.launch(FileProvider.getUriForFile(context, "${context.packageName}.files", file))
                    } catch (_: ActivityNotFoundException) {
                        scope.launch { snackbar.showSnackbar(resources.getString(R.string.scan_no_camera)) }
                    }
                }) { Text(stringResource(R.string.scan_take_photo)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    chooseSource = false
                    pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text(stringResource(R.string.scan_pick_image)) }
            },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_transaction_title)) },
            text = { Text(stringResource(R.string.delete_transaction_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.odustani)) } },
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

@Composable
private fun AmountCard(
    isIncome: Boolean,
    amount: String,
    error: Boolean,
    onAmountChange: (String) -> Unit,
    modifier: Modifier,
) {
    val accent = if (isIncome) FinanceTheme.colors.income else FinanceTheme.colors.expense
    Column(modifier) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (isIncome) FinanceTheme.colors.incomeContainer else FinanceTheme.colors.expenseContainer,
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TransactionTypeBadge(isIncome, size = 32.dp, container = MaterialTheme.colorScheme.surfaceContainer)
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.iznos), style = MaterialTheme.typography.titleMedium, color = accent)
                }
                TextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    placeholder = { Text(stringResource(R.string.amount_hint), style = MaterialTheme.typography.displaySmall) },
                    suffix = { Text(stringResource(R.string.currency_symbol), style = MaterialTheme.typography.headlineSmall, color = accent) },
                    textStyle = MaterialTheme.typography.displaySmall,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = accent,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        AnimatedVisibility(error) {
            Text(
                stringResource(R.string.error_amount),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 6.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryField(
    categories: List<Category>,
    selected: String,
    error: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it && categories.isNotEmpty() },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.kategorija)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_label), contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            isError = error,
            supportingText = when {
                error -> { { Text(stringResource(R.string.error_category)) } }
                categories.isEmpty() -> { { Text(stringResource(R.string.no_categories_hint)) } }
                else -> null
            },
            shape = FieldShape,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onSelect(category.name)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
