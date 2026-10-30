package com.example.financemanager

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.financemanager.data.TransactionType
import com.example.financemanager.di.appEntryPoint
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The app's main flows, through the real activity, navigation and database. Robolectric
 * runs them on the JVM, so they need no emulator and run in CI with the unit tests.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h900dp-xxhdpi")
class AppFlowsUiTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val appLock get() = rule.activity.appEntryPoint().appLock()

    /** Lets animations finish and the screen settle. */
    private fun settle() {
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
    }

    /** Waits for something that depends on the database, which works on its own threads. */
    private fun waitForText(text: String) {
        rule.waitUntil(timeoutMillis = 10_000) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun back() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        settle()
    }

    @Before
    fun unlock() {
        // Robolectric has no fingerprint sensor; unlock as if one had been accepted.
        assertTrue(appLock.locked.value)
        rule.runOnIdle { appLock.unlock() }
        settle()
        rule.onNodeWithText("Vaš pregled").assertIsDisplayed()
    }

    @Test
    fun addingAnExpenseShowsItOnTheDashboard() {
        // Categories are added beforehand: the add-category dialog focuses its text field, and
        // the blinking cursor keeps Compose from ever going idle in a test.
        runBlocking { rule.activity.appEntryPoint().repository().addCategory("Hrana", TransactionType.EXPENSE) }
        rule.onNodeWithText("Rashod").performClick()
        settle()
        rule.onNodeWithText("Novi rashod").assertIsDisplayed()

        // The amount is the first text field on the form.
        rule.onAllNodes(hasSetTextAction())[0].performTextInput("12,5")
        rule.onNodeWithText("Kategorija").performClick()
        settle()
        rule.onNodeWithText("Hrana").performClick()
        settle()
        rule.onNodeWithText("Spremi").performClick()

        // The balance on top goes negative...
        waitForText("−12,50\u00A0€")
        settle()
        rule.onNodeWithText("Vaš pregled").assertIsDisplayed()
        rule.onNodeWithText("12,50\u00A0€").assertIsDisplayed() // "Rashodi" in the balance card
        // ...and the transaction is in the list further down.
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Hrana"))
        settle()
        rule.onNodeWithText("Hrana").assertIsDisplayed()
    }

    @Test
    fun savingWithoutAnAmountShowsAnError() {
        rule.onNodeWithText("Prihod").performClick()
        settle()
        rule.onNodeWithText("Spremi").performClick()
        settle()
        rule.onNodeWithText("Unesite iznos veći od nule").assertIsDisplayed()
        rule.onNodeWithText("Novi prihod").assertIsDisplayed()
    }

    @Test
    fun everyScreenOpensFromTheDashboard() {
        rule.onNodeWithContentDescription("Otvori statistiku").performClick()
        settle()
        rule.onNodeWithText("Statistika").assertIsDisplayed()
        back()

        // Menu entry -> title of the screen it opens.
        listOf(
            "Kategorije" to "Kategorije",
            "Budžeti" to "Budžeti",
            "Ciljevi štednje" to "Ciljevi štednje",
            "Podsjetnici za plaćanje" to "Podsjetnici za plaćanje",
            "Uvidi" to "Uvidi",
            "Izvoz i kopija" to "Izvoz i sigurnosna kopija",
        ).forEach { (entry, title) ->
            rule.onNodeWithContentDescription("Više opcija").performClick()
            settle()
            // Some entries are also section titles on the dashboard; the menu item is the clickable one.
            rule.onNode(hasText(entry) and hasClickAction()).performClick()
            settle()
            // The title is shown (it may appear twice, e.g. also as a section heading).
            rule.onAllNodesWithText(title).onFirst().assertIsDisplayed()
            back()
            rule.onNodeWithText("Vaš pregled").assertIsDisplayed()
        }
    }

    @Test
    fun lockingFromTheMenuHidesTheAppUntilUnlocked() {
        rule.onNodeWithContentDescription("Više opcija").performClick()
        settle()
        rule.onNodeWithText("Zaključaj").performClick()
        settle()
        assertTrue(appLock.locked.value)
        // The dashboard is still underneath but neither visible to screen readers nor usable.
        assertTrue(rule.onAllNodesWithText("Vaš pregled").fetchSemanticsNodes().isEmpty())
        rule.onRoot().performTouchInput { click(Offset(width / 2f, height / 2f)) }
        settle()

        rule.runOnIdle { appLock.unlock() }
        settle()
        assertFalse(appLock.locked.value)
        rule.onNodeWithText("Vaš pregled").assertIsDisplayed()
    }

}
