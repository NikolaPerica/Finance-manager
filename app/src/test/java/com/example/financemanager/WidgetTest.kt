package com.example.financemanager

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import com.example.financemanager.widget.FinanceWidget
import com.example.financemanager.widget.WidgetContent
import com.example.financemanager.widget.WidgetSummary
import com.example.financemanager.widget.widgetSummary
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class WidgetTest {

    private val summary = WidgetSummary(balance = 1221.6, monthIncome = 1850.0, monthExpense = 628.4)

    @Test
    fun balanceIsAllTimeAndTotalsAreThisMonth() {
        val today = LocalDate.of(2026, 10, 23)
        val transactions = listOf(
            Transaction(1, 1000.0, today.minusMonths(1), "", "Plaća", TransactionType.INCOME),
            Transaction(2, 1850.0, today, "", "Plaća", TransactionType.INCOME),
            Transaction(3, 600.0, today.minusMonths(1), "", "Stanarina", TransactionType.EXPENSE),
            Transaction(4, 628.4, today.withDayOfMonth(1), "", "Hrana", TransactionType.EXPENSE),
        )
        assertEquals(WidgetSummary(balance = 1621.6, monthIncome = 1850.0, monthExpense = 628.4), widgetSummary(transactions, today).let {
            it.copy(balance = Math.round(it.balance * 100) / 100.0)
        })
    }

    @Test
    fun wideWidgetShowsTheMonthAndFullButtonLabels() = runGlanceAppWidgetUnitTest {
        setContext(ApplicationProvider.getApplicationContext())
        setAppWidgetSize(DpSize(250.dp, 120.dp))
        provideComposable { WidgetContent(summary) }

        onNode(hasText("Trenutno stanje")).assertExists()
        onNode(hasText("1.221,60\u00A0€")).assertExists()
        onNode(hasText("Ovaj mjesec +1.850,00\u00A0€ · −628,40\u00A0€")).assertExists()
        onNode(hasText("Dodaj prihod")).assertExists()
        onNode(hasText("Dodaj rashod")).assertExists()
    }

    @Test
    fun compactWidgetKeepsOnlyTheEssentials() = runGlanceAppWidgetUnitTest {
        setContext(ApplicationProvider.getApplicationContext())
        setAppWidgetSize(FinanceWidget.Compact)
        provideComposable { WidgetContent(summary) }

        onNode(hasText("1.221,60\u00A0€")).assertExists()
        onNode(hasText("Ovaj mjesec")).assertDoesNotExist()
        onNode(hasText("Prihod")).assertExists()
        onNode(hasText("Rashod")).assertExists()
    }
}
