package com.example.financemanager.ui.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.financemanager.R
import com.example.financemanager.data.TransactionType
import com.example.financemanager.ui.Croatian
import com.example.financemanager.ui.MoneyFormat
import com.example.financemanager.ui.components.TransactionTypeBadge
import com.example.financemanager.ui.components.animatedAmount
import com.example.financemanager.ui.theme.FinanceTheme
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

private val CardShape = RoundedCornerShape(24.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isIncome = state.type == TransactionType.INCOME
    val accent = if (isIncome) FinanceTheme.colors.income else FinanceTheme.colors.expense

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.natrag))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "type") {
                Choice(
                    options = listOf(TransactionType.EXPENSE to R.string.rashodi, TransactionType.INCOME to R.string.prihodi),
                    selected = state.type,
                    onSelect = viewModel::onTypeChange,
                    modifier = Modifier.contentWidth(),
                )
            }
            item(key = "range") {
                Choice(
                    options = listOf(
                        StatsRange.MONTH to R.string.range_month,
                        StatsRange.YEAR to R.string.range_year,
                        StatsRange.ALL to R.string.range_all,
                    ),
                    selected = state.period.range,
                    onSelect = viewModel::onRangeChange,
                    modifier = Modifier.contentWidth(),
                )
            }
            item(key = "period") {
                PeriodSwitcher(
                    period = state.period,
                    canGoForward = state.canGoForward,
                    onShift = viewModel::shift,
                    modifier = Modifier.contentWidth(),
                )
            }
            item(key = "total") {
                TotalCard(state, accent, Modifier.contentWidth())
            }
            if (state.trend.size > 1) {
                item(key = "trend") {
                    Section(
                        title = stringResource(R.string.over_time),
                        subtitle = stringResource(R.string.chart_hint),
                        modifier = Modifier.contentWidth(),
                    ) {
                        TrendChart(state.trend, accent, onSelect = viewModel::select)
                    }
                }
            }
            item(key = "categories") {
                Section(title = stringResource(R.string.by_category), modifier = Modifier.contentWidth()) {
                    if (!state.isLoading && state.categories.isEmpty()) {
                        Text(
                            stringResource(if (isIncome) R.string.stats_empty_income else R.string.stats_empty_expense),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    val largest = state.categories.firstOrNull()?.amount ?: 0.0
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        state.categories.forEach { category ->
                            CategoryRow(category, total = state.total, largest = largest, accent = accent)
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.contentWidth() = widthIn(max = 640.dp).fillMaxWidth()

@Composable
private fun <T> Choice(options: List<Pair<T, Int>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier) {
    SingleChoiceSegmentedButtonRow(modifier) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) {
                Text(stringResource(label))
            }
        }
    }
}

@Composable
private fun PeriodSwitcher(period: StatsPeriod, canGoForward: Boolean, onShift: (Long) -> Unit, modifier: Modifier) {
    val movable = period.range != StatsRange.ALL
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onShift(-1) }, enabled = movable) {
            Icon(
                painterResource(R.drawable.ic_chevron_left),
                contentDescription = stringResource(R.string.previous_period),
                tint = if (movable) MaterialTheme.colorScheme.onSurface else Color.Transparent,
            )
        }
        Text(
            periodLabel(period),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = { onShift(1) }, enabled = movable && canGoForward) {
            Icon(
                painterResource(R.drawable.ic_chevron_right),
                contentDescription = stringResource(R.string.next_period),
                tint = when {
                    !movable -> Color.Transparent
                    canGoForward -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                },
            )
        }
    }
}

@Composable
private fun periodLabel(period: StatsPeriod): String = when (period.range) {
    StatsRange.MONTH -> MonthTitle.format(period.month).replaceFirstChar { it.titlecase(Croatian) }
    StatsRange.YEAR -> "${period.month.year}."
    StatsRange.ALL -> stringResource(R.string.all_time)
}

@Composable
private fun TotalCard(state: StatsUiState, accent: Color, modifier: Modifier) {
    val isIncome = state.type == TransactionType.INCOME
    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            TransactionTypeBadge(isIncome, size = 52.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (isIncome) R.string.total_income else R.string.total_expense),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    MoneyFormat.format(animatedAmount(state.total)),
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    pluralStringResource(R.plurals.transactions_count, state.count, state.count),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.change?.let { change ->
                    Spacer(Modifier.height(6.dp))
                    ChangeLine(change, state.period.range)
                }
            }
        }
    }
}

@Composable
private fun ChangeLine(change: Double, range: StatsRange) {
    val percent = (abs(change) * 100).roundToInt()
    val compared = stringResource(if (range == StatsRange.YEAR) R.string.vs_previous_year else R.string.vs_previous_month)
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (percent != 0) {
            Icon(
                painterResource(if (change > 0) R.drawable.ic_arrow_upward else R.drawable.ic_arrow_downward),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            "${if (change > 0 && percent != 0) "+" else if (percent != 0) "−" else ""}$percent % $compared",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Section(title: String, modifier: Modifier, subtitle: String? = null, content: @Composable () -> Unit) {
    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun CategoryRow(category: CategoryStat, total: Double, largest: Double, accent: Color) {
    val share = if (total > 0) category.amount / total else 0.0
    // Bars are scaled to the largest category so small differences stay visible.
    val fill by animateFloatAsState(
        targetValue = if (largest > 0) (category.amount / largest).toFloat() else 0f,
        animationSpec = tween(600),
        label = "categoryFill",
    )
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(
                    category.name.ifBlank { stringResource(R.string.no_category) },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    pluralStringResource(R.plurals.transactions_count, category.count, category.count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(MoneyFormat.format(category.amount), style = MaterialTheme.typography.titleSmall)
                Text(
                    percentText(share),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fill.coerceIn(0f, 1f))
                    .background(accent, CircleShape),
            )
        }
    }
}

private fun percentText(share: Double): String {
    val percent = (share * 100).roundToInt()
    return if (percent == 0 && share > 0) "< 1 %" else "$percent %"
}

/**
 * Column chart of [bars]. The selected period is drawn in [accent] and the rest in
 * gray; when nothing is selected every column uses [accent]. Tapping a column opens it.
 */
@Composable
private fun TrendChart(bars: List<TrendBar>, accent: Color, onSelect: (StatsPeriod) -> Unit) {
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val valueStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurface)
    val grid = MaterialTheme.colorScheme.outlineVariant
    val baseline = MaterialTheme.colorScheme.outline
    val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
    val hasSelection = bars.any { it.selected }
    val axisMax = remember(bars) { niceCeiling(bars.maxOf { it.amount }) }
    val description = bars.joinToString { "${barLabel(it.period, long = true)}: ${MoneyFormat.format(it.amount)}" }

    val grow by animateFloatAsState(1f, tween(600), label = "chartGrow")

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .semantics { contentDescription = description }
            .pointerInput(bars) {
                detectTapGestures { offset ->
                    val axisWidth = AxisWidth.toPx()
                    val slot = (size.width - axisWidth) / bars.size
                    val index = ((offset.x - axisWidth) / slot).toInt()
                    bars.getOrNull(index)?.let { onSelect(it.period) }
                }
            },
    ) {
        val axisWidth = AxisWidth.toPx()
        val labelHeight = 22.dp.toPx()
        val valueSpace = 22.dp.toPx()
        val plotTop = valueSpace
        val plotBottom = size.height - labelHeight
        val plotHeight = plotBottom - plotTop
        val slot = (size.width - axisWidth) / bars.size
        val barWidth = minOf(slot * 0.56f, 28.dp.toPx())
        val radius = 4.dp.toPx()

        // Recessive grid: zero, half and the top of the scale.
        listOf(0.0, axisMax / 2, axisMax).forEach { value ->
            val y = plotBottom - (value / axisMax).toFloat() * plotHeight
            drawLine(
                color = if (value == 0.0) baseline else grid,
                start = Offset(axisWidth, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
            )
            if (value > 0) {
                val text = measurer.measure(AxisFormat.get()!!.format(value), labelStyle)
                drawText(text, topLeft = Offset(0f, y - text.size.height / 2f))
            }
        }

        bars.forEachIndexed { index, bar ->
            val centerX = axisWidth + slot * index + slot / 2
            val height = (bar.amount / axisMax).toFloat() * plotHeight * grow
            if (bar.amount > 0) {
                val top = plotBottom - maxOf(height, 2.dp.toPx())
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = centerX - barWidth / 2,
                            top = top,
                            right = centerX + barWidth / 2,
                            bottom = plotBottom,
                            topLeftCornerRadius = CornerRadius(radius),
                            topRightCornerRadius = CornerRadius(radius),
                        ),
                    )
                }
                drawPath(path, if (!hasSelection || bar.selected) accent else muted)
            }

            // Only the selected column gets a value label, so the chart stays readable.
            if (bar.selected) {
                val text = measurer.measure(
                    MoneyFormat.format(bar.amount),
                    valueStyle,
                    maxLines = 1,
                )
                val x = (centerX - text.size.width / 2f).coerceIn(axisWidth, size.width - text.size.width)
                val y = plotBottom - height - text.size.height - 4.dp.toPx()
                drawText(text, topLeft = Offset(x, maxOf(0f, y)))
            }

            val label = measurer.measure(
                barLabel(bar.period, long = false),
                if (bar.selected) labelStyle.copy(color = valueStyle.color) else labelStyle,
                maxLines = 1,
                constraints = Constraints(maxWidth = slot.toInt().coerceAtLeast(1)),
            )
            drawText(label, topLeft = Offset(centerX - label.size.width / 2f, plotBottom + 6.dp.toPx()))
        }
    }
}

private val AxisWidth = 52.dp

private fun barLabel(period: StatsPeriod, long: Boolean): String = when (period.range) {
    StatsRange.MONTH -> (if (long) MonthTitle else MonthShort).format(period.month).trimEnd('.')
    else -> period.month.year.toString()
}

/** Rounds [max] up to 1, 2, 2.5 or 5 times a power of ten so grid labels are round numbers. */
internal fun niceCeiling(max: Double): Double {
    if (max <= 0) return 1.0
    val magnitude = 10.0.pow(floor(log10(max)))
    val step = listOf(1.0, 2.0, 2.5, 5.0, 10.0).first { it * magnitude >= max }
    return ceil(step * magnitude)
}

private val MonthTitle = DateTimeFormatter.ofPattern("LLLL yyyy.", Croatian)
private val MonthShort = DateTimeFormatter.ofPattern("LLL", Croatian)
private val AxisFormat: ThreadLocal<DecimalFormat> =
    ThreadLocal.withInitial { DecimalFormat("#,##0 €", DecimalFormatSymbols(Croatian)) }
