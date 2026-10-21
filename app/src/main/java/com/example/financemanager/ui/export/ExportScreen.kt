package com.example.financemanager.ui.export

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.financemanager.R
import com.example.financemanager.data.backup.InvalidBackupException
import com.example.financemanager.data.backup.created
import com.example.financemanager.export.CsvLabels
import com.example.financemanager.export.PdfReportWriter
import com.example.financemanager.ui.Croatian
import com.example.financemanager.ui.DateFormat
import com.example.financemanager.ui.stats.Choice
import com.example.financemanager.ui.stats.PeriodSwitcher
import com.example.financemanager.ui.stats.StatsRange
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val CardShape = RoundedCornerShape(24.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(viewModel: ExportViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var busy by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<BackupPreview?>(null) }

    /** Runs [work] off the main thread, then reports success (optionally offering to open [open]) or failure. */
    fun run(done: Int, failed: Int, open: Pair<Uri, String>? = null, work: suspend () -> Unit) {
        busy = true
        scope.launch {
            val ok = try {
                withContext(Dispatchers.IO) { work() }
                true
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // File not writable, database error...: report it, nothing else to do.
                false
            } finally {
                busy = false
            }
            snackbar.currentSnackbarData?.dismiss()
            if (!ok) {
                snackbar.showSnackbar(resources.getString(failed))
            } else if (open == null) {
                snackbar.showSnackbar(resources.getString(done))
            } else {
                val result = snackbar.showSnackbar(resources.getString(done), resources.getString(R.string.open), duration = SnackbarDuration.Long)
                if (result == SnackbarResult.ActionPerformed) openFile(context, open.first, open.second, scope, snackbar, resources.getString(R.string.no_app_to_open))
            }
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(PDF)) { uri ->
        if (uri != null) {
            run(R.string.export_done, R.string.export_failed, open = uri to PDF) {
                val report = viewModel.report()
                context.contentResolver.openOutputStream(uri)?.use { PdfReportWriter(context).write(report, it) }
                    ?: throw IOException("Cannot open $uri")
            }
        }
    }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(CSV)) { uri ->
        if (uri != null) {
            run(R.string.export_done, R.string.export_failed, open = uri to CSV) {
                val csv = viewModel.csv(
                    CsvLabels(
                        date = resources.getString(R.string.datum),
                        type = resources.getString(R.string.csv_type),
                        category = resources.getString(R.string.kategorija),
                        amount = resources.getString(R.string.iznos),
                        note = resources.getString(R.string.report_note),
                        income = resources.getString(R.string.prihod),
                        expense = resources.getString(R.string.rashod),
                    ),
                )
                context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
                    ?: throw IOException("Cannot open $uri")
            }
        }
    }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(JSON)) { uri ->
        if (uri != null) {
            run(R.string.backup_done, R.string.backup_failed) {
                val json = viewModel.backupJson()
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                    ?: throw IOException("Cannot open $uri")
            }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            busy = true
            scope.launch {
                val result = try {
                    withContext(Dispatchers.IO) {
                        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                            ?: throw IOException("Cannot open $uri")
                        viewModel.readBackup(text)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Unreadable file or not a valid backup: nothing is changed.
                    null
                } finally {
                    busy = false
                }
                if (result != null) {
                    preview = result
                } else {
                    snackbar.currentSnackbarData?.dismiss()
                    snackbar.showSnackbar(resources.getString(R.string.restore_invalid))
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.export_title)) },
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
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val width = Modifier.widthIn(max = 640.dp).fillMaxWidth()
            val allTime = stringResource(R.string.file_all_time)

            Section(
                icon = R.drawable.ic_file_download,
                title = stringResource(R.string.report_section),
                text = stringResource(R.string.report_section_text),
                modifier = width,
            ) {
                Choice(
                    options = listOf(
                        StatsRange.MONTH to R.string.range_month,
                        StatsRange.YEAR to R.string.range_year,
                        StatsRange.ALL to R.string.range_all,
                    ),
                    selected = state.period.range,
                    onSelect = viewModel::onRangeChange,
                    modifier = Modifier.fillMaxWidth(),
                )
                PeriodSwitcher(state.period, state.canGoForward, viewModel::shift, Modifier.fillMaxWidth())
                Text(
                    pluralStringResource(R.plurals.transactions_count, state.count, state.count),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { pdfLauncher.launch("izvjestaj-${viewModel.periodSlug(allTime)}.pdf") },
                        enabled = !busy && !state.isLoading,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) { Text(stringResource(R.string.export_pdf)) }
                    OutlinedButton(
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        onClick = { csvLauncher.launch("transakcije-${viewModel.periodSlug(allTime)}.csv") },
                        enabled = !busy && state.count > 0,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) { Text(stringResource(R.string.export_csv)) }
                }
            }

            Section(
                icon = R.drawable.ic_backup,
                title = stringResource(R.string.backup_section),
                text = stringResource(R.string.backup_section_text),
                modifier = width,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { backupLauncher.launch("finance-manager-kopija-${LocalDate.now()}.json") },
                        enabled = !busy,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) { Text(stringResource(R.string.backup_save)) }
                    OutlinedButton(
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        // Cloud providers often don't label JSON as such, so accept plain text and binary too.
                        onClick = { restoreLauncher.launch(arrayOf(JSON, "text/plain", "application/octet-stream")) },
                        enabled = !busy,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) { Text(stringResource(R.string.backup_restore)) }
                }
            }
        }
    }

    preview?.let { backup ->
        val data = backup.data
        AlertDialog(
            onDismissRequest = { preview = null },
            icon = { Icon(painterResource(R.drawable.ic_restore), contentDescription = null) },
            title = { Text(stringResource(R.string.restore_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.restore_message,
                        backup.file.created?.let { "${DateFormat.long(it.toLocalDate())} ${it.format(Time)}" } ?: "?",
                        pluralStringResource(R.plurals.transactions_count, data.transactions.size, data.transactions.size),
                        data.categories.size,
                        data.reminders.size,
                        data.goals.size,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    preview = null
                    run(R.string.restore_done, R.string.restore_failed) { viewModel.restore(backup) }
                }) { Text(stringResource(R.string.backup_restore)) }
            },
            dismissButton = { TextButton(onClick = { preview = null }) { Text(stringResource(R.string.odustani)) } },
        )
    }
}

@Composable
private fun Section(icon: Int, title: String, text: String, modifier: Modifier, content: @Composable () -> Unit) {
    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

/** Opens a file the user just saved in an app that can show (and print or share) it. */
private fun openFile(context: Context, uri: Uri, mime: String, scope: CoroutineScope, snackbar: SnackbarHostState, noApp: String) {
    val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        scope.launch { snackbar.showSnackbar(noApp) }
    }
}

private const val PDF = "application/pdf"
private const val CSV = "text/csv"
private const val JSON = "application/json"
private val Time: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Croatian)
