package com.example.financemanager

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.financemanager.di.appEntryPoint
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.GraphicsMode

/**
 * The widget's buttons. The activity is driven by Robolectric's controller rather than an
 * ActivityScenario, because only the controller delivers a new intent the way Android does.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetQuickActionUiTest {

    @get:Rule
    val rule = createEmptyComposeRule()

    private val app get() = ApplicationProvider.getApplicationContext<FinanceApp>()
    private val appLock get() = app.appEntryPoint().appLock()

    private fun settle() {
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
    }

    @Test
    fun buttonsOpenTheFormBehindTheLockAndWhileOpen() {
        // Cold start from the "add expense" button: the form waits behind the lock screen.
        val controller = Robolectric.buildActivity(MainActivity::class.java, QuickAction.ADD_EXPENSE.intent(app)).setup()
        settle()
        assertTrue(appLock.locked.value)
        appLock.unlock()
        settle()
        rule.onNodeWithText("Novi rashod").assertIsDisplayed()

        // Back leads to the dashboard underneath.
        controller.get().onBackPressedDispatcher.onBackPressed()
        settle()
        rule.onNodeWithText("Vaš pregled").assertIsDisplayed()

        // The app is open and the "add income" button is tapped.
        controller.newIntent(QuickAction.ADD_INCOME.intent(app))
        settle()
        rule.onNodeWithText("Novi prihod").assertIsDisplayed()

        controller.pause().stop().destroy()
    }
}
