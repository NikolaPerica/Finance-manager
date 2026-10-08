package com.example.financemanager.ui.components

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.financemanager.notifications.ReminderCheck
import com.example.financemanager.notifications.ReminderNotifier

/** Whether notifications can be shown, and a way to ask for them. */
@Stable
class NotificationAccess(enabled: () -> Boolean, private val onRequest: (openSettings: Boolean) -> Unit) {
    private val isEnabled = enabled
    val enabled: Boolean get() = isEnabled()

    /** Shows the system permission dialog where possible, otherwise opens the app's notification settings. */
    fun request() = onRequest(true)

    /** Shows the system permission dialog if it can still appear; never leaves the app. */
    fun requestQuietly() = onRequest(false)
}

@Composable
fun rememberNotificationAccess(): NotificationAccess {
    val context = LocalContext.current
    val notifier = remember(context) { ReminderNotifier(context) }
    var enabled by remember { mutableStateOf(notifier.canNotify()) }
    var asked by rememberSaveable { mutableStateOf(false) }

    fun refresh() {
        val now = notifier.canNotify()
        // Just turned on: announce today's payments instead of waiting for tomorrow's check.
        if (now && !enabled) ReminderCheck.run(context)
        enabled = now
    }

    LifecycleResumeEffect(notifier) {
        refresh()
        onPauseOrDispose {}
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }

    return remember(notifier, launcher) {
        NotificationAccess(
            enabled = { enabled },
            onRequest = { openSettings ->
                when {
                    notifier.canNotify() -> refresh()
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !asked -> {
                        asked = true
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    // Refused before, or switched off in settings: only the settings screen can help.
                    openSettings -> context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                }
            },
        )
    }
}
