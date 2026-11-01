package com.example.financemanager

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.financemanager.data.SettingsRepository
import com.example.financemanager.domain.MoneyFormat
import com.example.financemanager.model.ThemeMode
import com.example.financemanager.security.AppLock
import com.example.financemanager.ui.navigation.FinanceNavHost
import com.example.financemanager.ui.theme.FinanceTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

// FragmentActivity rather than ComponentActivity because BiometricPrompt needs one.
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var appLock: AppLock

    @Inject
    lateinit var settingsRepository: SettingsRepository

    /** A shortcut from the widget waiting to be carried out, see [QuickAction]. */
    private val quickAction = MutableStateFlow<QuickAction?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Keep balances out of the recent apps overview. On Android 13+ only that preview is
        // hidden; older versions have no such switch, see onPause.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) setRecentsScreenshotEnabled(false)
        // After a rotation the shortcut has already been handled.
        if (savedInstanceState == null) quickAction.value = QuickAction.from(intent)

        // Already read when the app started, so this doesn't wait.
        val initial = runBlocking { settingsRepository.settings.first() }
        // Amounts are formatted as they are drawn, so a new currency needs everything redrawn.
        // Recreating keeps the screen the user is on, and it doesn't count as leaving the app.
        val drawnSymbol = MoneyFormat.symbol
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsRepository.settings.collect { settings ->
                    if (settings.currency.symbol != drawnSymbol) {
                        MoneyFormat.symbol = settings.currency.symbol
                        recreate()
                    }
                }
            }
        }
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(initial)
            val darkTheme = when (settings.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            FinanceTheme(darkTheme) {
                FinanceNavHost(appLock, quickAction, onQuickActionHandled = { quickAction.value = null })
            }
        }
    }

    // The app was already open (launchMode singleTop) and a widget button was tapped.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        QuickAction.from(intent)?.let { quickAction.value = it }
    }

    override fun onStart() {
        super.onStart()
        appLock.onForeground()
    }

    override fun onStop() {
        super.onStop()
        // Rotating the screen also stops the activity, but the user never left.
        if (!isChangingConfigurations) appLock.onBackground()
    }

    override fun onPause() {
        super.onPause()
        // Before Android 13 the overview snapshot is taken after onPause; a secure window
        // shows up blank there. It is cleared again on resume so screenshots keep working.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
