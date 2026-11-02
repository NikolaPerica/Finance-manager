package com.example.financemanager

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.financemanager.domain.widget.WidgetSummary
import com.example.financemanager.domain.widget.widgetSummary
import com.example.financemanager.model.Transaction
import com.example.financemanager.model.TransactionType
import com.example.financemanager.widget.FinanceWidget
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class WidgetTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val summary = WidgetSummary(balance = 1221.6, monthIncome = 1850.0, monthExpense = 628.4)

    /** The widget as the launcher would draw it. */
    private fun draw(wide: Boolean, hideAmounts: Boolean = false): View =
        FinanceWidget.layout(context, summary, hideAmounts, wide).apply(context, FrameLayout(context))

    private fun View.text(id: Int) = findViewById<TextView>(id).text.toString()

    private fun View.isShown(id: Int) = findViewById<View>(id).visibility == View.VISIBLE

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
    fun wideWidgetShowsTheMonthAndFullButtonLabels() {
        val widget = draw(wide = true)
        assertEquals("1.221,60 €", widget.text(R.id.widget_balance))
        assertEquals(true, widget.isShown(R.id.widget_month))
        assertEquals("Ovaj mjesec +1.850,00 € · −628,40 €", widget.text(R.id.widget_month))
        assertEquals("Dodaj prihod", widget.text(R.id.widget_add_income_label))
        assertEquals("Dodaj rashod", widget.text(R.id.widget_add_expense_label))
    }

    @Test
    fun compactWidgetKeepsOnlyTheEssentials() {
        val widget = draw(wide = false)
        assertEquals("1.221,60 €", widget.text(R.id.widget_balance))
        assertEquals(false, widget.isShown(R.id.widget_month))
        assertEquals("Prihod", widget.text(R.id.widget_add_income_label))
        assertEquals("Rashod", widget.text(R.id.widget_add_expense_label))
    }

    @Test
    fun hiddenAmountsShowDotsInstead() {
        val widget = draw(wide = true, hideAmounts = true)
        assertEquals("•••• €", widget.text(R.id.widget_balance))
        assertEquals("Iznosi su skriveni", widget.text(R.id.widget_month))
        assertEquals("Dodaj rashod", widget.text(R.id.widget_add_expense_label))
    }
}
