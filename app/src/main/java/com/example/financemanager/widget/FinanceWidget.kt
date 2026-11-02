package com.example.financemanager.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.example.financemanager.MainActivity
import com.example.financemanager.QuickAction
import com.example.financemanager.R
import com.example.financemanager.di.appEntryPoint
import com.example.financemanager.domain.BudgetMonths
import com.example.financemanager.domain.MoneyFormat
import com.example.financemanager.domain.widget.WidgetSummary
import com.example.financemanager.domain.widget.widgetSummary
import kotlinx.coroutines.flow.first

/**
 * Home screen widget: the balance, this month's totals and buttons to add income or an expense.
 *
 * Drawn with plain RemoteViews and handed straight to the launcher, so a change shows at once.
 * (Jetpack Glance, used before, redraws through a session that it starts with WorkManager; on
 * Samsung phones that could take half a minute, or an update was lost altogether.)
 */
object FinanceWidget {
    /** Two cells wide: balance and buttons only, in dp. */
    val Compact = SizeF(110f, 110f)

    /** Three or more cells: also this month's income and expenses. */
    val Wide = SizeF(220f, 110f)

    /** Redraws every placed widget with the current data and settings. */
    suspend fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, FinanceWidgetReceiver::class.java))
        if (ids.isEmpty()) return
        val app = context.appEntryPoint()
        val settings = app.settings().settings.first()
        val summary = widgetSummary(app.repository().transactions().first(), months = BudgetMonths(settings.monthStartDay))
        ids.forEach { id ->
            manager.updateAppWidget(id, views(context, summary, settings.hideWidgetAmounts, manager.getAppWidgetOptions(id)))
        }
    }

    fun isPlaced(context: Context): Boolean =
        AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, FinanceWidgetReceiver::class.java)).isNotEmpty()

    private fun views(context: Context, summary: WidgetSummary, hideAmounts: Boolean, options: Bundle?): RemoteViews =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // The launcher picks the layout that fits as the widget is resized.
            RemoteViews(
                mapOf(
                    Compact to layout(context, summary, hideAmounts, wide = false),
                    Wide to layout(context, summary, hideAmounts, wide = true),
                ),
            )
        } else {
            val width = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 0
            layout(context, summary, hideAmounts, wide = width >= Wide.width)
        }

    /** One size of the widget. With [hideAmounts], dots stand in for every amount. */
    internal fun layout(context: Context, summary: WidgetSummary, hideAmounts: Boolean, wide: Boolean) =
        RemoteViews(context.packageName, R.layout.widget_finance).apply {
            setTextViewText(R.id.widget_balance, if (hideAmounts) hiddenAmount() else MoneyFormat.format(summary.balance))
            setTextViewTextSize(R.id.widget_balance, TypedValue.COMPLEX_UNIT_SP, if (wide) 26f else 20f)
            setViewVisibility(R.id.widget_month, if (wide) View.VISIBLE else View.GONE)
            setTextViewText(
                R.id.widget_month,
                if (hideAmounts) {
                    context.getString(R.string.widget_amounts_hidden)
                } else {
                    context.getString(
                        R.string.widget_this_month,
                        MoneyFormat.signed(summary.monthIncome, isIncome = true),
                        MoneyFormat.signed(summary.monthExpense, isIncome = false),
                    )
                },
            )
            setTextViewText(R.id.widget_add_income_label, context.getString(if (wide) R.string.dodaj_prihod else R.string.prihod))
            setTextViewText(R.id.widget_add_expense_label, context.getString(if (wide) R.string.dodaj_rashod else R.string.rashod))

            val openApp = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            setOnClickPendingIntent(R.id.widget_root, activity(context, 0, openApp))
            setOnClickPendingIntent(R.id.widget_add_income, activity(context, 1, QuickAction.ADD_INCOME.intent(context)))
            setOnClickPendingIntent(R.id.widget_add_expense, activity(context, 2, QuickAction.ADD_EXPENSE.intent(context)))
        }

    /** Takes the place of an amount, keeping the currency so it still reads as money. */
    private fun hiddenAmount() = "•••• ${MoneyFormat.symbol}"

    // Each button needs its own request code, or they would share one PendingIntent.
    private fun activity(context: Context, requestCode: Int, intent: Intent): PendingIntent =
        PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
}

class FinanceWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = redraw(context)

    // Below Android 12 the layout is chosen here, from the width the launcher reports.
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) = redraw(context)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // The first widget was just placed: keep it up to date from now on.
        WidgetSync.start(context)
    }

    private fun redraw(context: Context) {
        val pending = goAsync()
        WidgetSync.refresh(context) { pending.finish() }
    }
}
