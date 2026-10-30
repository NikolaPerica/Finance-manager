package com.example.financemanager.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.financemanager.MainActivity
import com.example.financemanager.QuickAction
import com.example.financemanager.R
import com.example.financemanager.di.appEntryPoint
import com.example.financemanager.ui.MoneyFormat
import kotlinx.coroutines.flow.first

/** Home screen widget: the balance, this month's totals and buttons to add income or an expense. */
class FinanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(Compact, Wide))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val summary = widgetSummary(context.appEntryPoint().repository().transactions().first())
        provideContent { WidgetContent(summary) }
    }

    companion object {
        /** Two cells wide: balance and buttons only. */
        val Compact = DpSize(110.dp, 110.dp)

        /** Three or more cells: also this month's income and expenses. */
        val Wide = DpSize(220.dp, 110.dp)
    }
}

class FinanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FinanceWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // The first widget was just placed: keep it up to date from now on.
        WidgetSync.start(context)
    }
}

private val White = ColorProvider(Color.White)
private val WhiteSoft = ColorProvider(Color.White.copy(alpha = 0.75f))

@Composable
internal fun WidgetContent(summary: WidgetSummary) {
    val context = LocalContext.current
    val wide = LocalSize.current.width >= FinanceWidget.Wide.width
    val openApp = actionStartActivity(
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
    )

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .cornerRadius(24.dp)
            .padding(16.dp)
            .clickable(openApp),
    ) {
        Text(context.getString(R.string.trenutno_stanje), style = TextStyle(color = WhiteSoft, fontSize = 13.sp), maxLines = 1)
        Text(
            MoneyFormat.format(summary.balance),
            style = TextStyle(color = White, fontSize = if (wide) 26.sp else 20.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
        if (wide) {
            Spacer(GlanceModifier.height(2.dp))
            Text(
                context.getString(
                    R.string.widget_this_month,
                    MoneyFormat.signed(summary.monthIncome, isIncome = true),
                    MoneyFormat.signed(summary.monthExpense, isIncome = false),
                ),
                style = TextStyle(color = WhiteSoft, fontSize = 12.sp),
                maxLines = 1,
            )
        }
        Spacer(GlanceModifier.defaultWeight())
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            QuickButton(
                label = context.getString(if (wide) R.string.dodaj_prihod else R.string.prihod),
                action = actionStartActivity(QuickAction.ADD_INCOME.intent(context)),
                modifier = GlanceModifier.defaultWeight(),
            )
            Spacer(GlanceModifier.width(8.dp))
            QuickButton(
                label = context.getString(if (wide) R.string.dodaj_rashod else R.string.rashod),
                action = actionStartActivity(QuickAction.ADD_EXPENSE.intent(context)),
                modifier = GlanceModifier.defaultWeight(),
            )
        }
    }
}

@Composable
private fun QuickButton(label: String, action: Action, modifier: GlanceModifier) {
    Row(
        modifier = modifier
            .background(ImageProvider(R.drawable.widget_button))
            .cornerRadius(16.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .clickable(action),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_widget_add),
            contentDescription = null,
            colorFilter = ColorFilter.tint(White),
            modifier = GlanceModifier.size(16.dp),
        )
        Spacer(GlanceModifier.width(4.dp))
        Text(label, style = TextStyle(color = White, fontSize = 13.sp, fontWeight = FontWeight.Medium), maxLines = 1)
    }
}
