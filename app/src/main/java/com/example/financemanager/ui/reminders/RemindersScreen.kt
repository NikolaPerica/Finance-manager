package com.example.financemanager.ui.reminders

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.financemanager.R
import com.example.financemanager.data.Reminder
import com.example.financemanager.notifications.ReminderNotifier
import com.example.financemanager.ui.DateFormat
import com.example.financemanager.ui.MoneyFormat
import com.example.financemanager.ui.components.SwipeToDelete
import com.example.financemanager.ui.theme.FinanceTheme
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: RemindersViewModel,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Reminder) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val today = LocalDate.now()
    var confirmPay by remember { mutableStateOf<Reminder?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            snackbar.currentSnackbarData?.dismiss()
            when (event) {
                is RemindersEvent.Paid -> {
                    val message = event.next?.let { resources.getString(R.string.paid_next, DateFormat.short(it.nextDueDate)) }
                        ?: resources.getString(R.string.paid_done)
                    val result = snackbar.showSnackbar(message, resources.getString(R.string.undo), duration = SnackbarDuration.Short)
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoPayment(event.payment)
                }
                is RemindersEvent.Deleted -> {
                    val result = snackbar.showSnackbar(
                        resources.getString(R.string.reminder_deleted),
                        resources.getString(R.string.undo),
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.restore(event.reminder)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reminders_title)) },
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
                onClick = onAdd,
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(R.string.new_reminder)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "permission") { NotificationBanner(Modifier.contentWidth()) }
            if (state.reminders.isNotEmpty()) {
                item(key = "summary") { MonthSummary(state.dueThisMonth, Modifier.contentWidth()) }
            } else if (!state.isLoading) {
                item(key = "empty") { EmptyReminders(Modifier.contentWidth().animateItem()) }
            }
            items(state.reminders, key = { it.id }) { reminder ->
                SwipeToDelete(onDelete = { viewModel.delete(reminder) }, modifier = Modifier.contentWidth().animateItem()) {
                    ReminderCard(
                        reminder = reminder,
                        today = today,
                        onClick = { onEdit(reminder) },
                        onPay = { confirmPay = reminder },
                    )
                }
            }
        }
    }

    confirmPay?.let { reminder ->
        AlertDialog(
            onDismissRequest = { confirmPay = null },
            title = { Text(stringResource(R.string.pay_title)) },
            text = { Text(stringResource(R.string.pay_message, reminder.name, MoneyFormat.format(reminder.amount))) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.pay(reminder)
                    confirmPay = null
                }) { Text(stringResource(R.string.pay)) }
            },
            dismissButton = { TextButton(onClick = { confirmPay = null }) { Text(stringResource(R.string.odustani)) } },
        )
    }
}

private fun Modifier.contentWidth() = widthIn(max = 640.dp).fillMaxWidth()

@Composable
private fun MonthSummary(amount: Double, modifier: Modifier) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .clip(shape)
            .background(Brush.linearGradient(FinanceTheme.colors.gradient))
            .padding(20.dp),
    ) {
        Text(stringResource(R.string.due_this_month), style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.7f))
        Text(MoneyFormat.format(amount), style = MaterialTheme.typography.headlineSmall, color = Color.White)
    }
}

/** Explains that reminders can't arrive while notifications are off, with a way to turn them on. */
@Composable
private fun NotificationBanner(modifier: Modifier) {
    val context = LocalContext.current
    val notifier = remember(context) { ReminderNotifier(context) }
    var enabled by remember { mutableStateOf(notifier.canNotify()) }
    var asked by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(notifier) {
        enabled = notifier.canNotify()
        onPauseOrDispose {}
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        enabled = notifier.canNotify()
    }
    if (enabled) return

    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = modifier) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_bell), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.notifications_off),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !asked) {
                    asked = true
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    // Already refused once (or switched off in settings): only the settings screen can help.
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                }
            }) { Text(stringResource(R.string.enable)) }
        }
    }
}

@Composable
private fun EmptyReminders(modifier: Modifier) {
    Surface(
        shape = RoundedCornerShape(24.dp),
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
                    painterResource(R.drawable.ic_bell),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.reminders_empty_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.reminders_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
