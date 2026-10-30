package com.example.financemanager.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.financemanager.R
import com.example.financemanager.domain.Croatian
import com.example.financemanager.domain.DateFormat
import com.example.financemanager.domain.MoneyFormat
import com.example.financemanager.domain.export.Report
import com.example.financemanager.domain.stats.CategoryStat
import com.example.financemanager.domain.stats.StatsPeriod
import com.example.financemanager.domain.stats.StatsRange
import com.example.financemanager.model.TransactionType
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/** Where report pages are drawn: a PDF document, or bitmaps in tests. */
interface PageSink {
    /** Starts page [number] (from 1) of [width] x [height] points and returns its canvas. */
    fun startPage(number: Int, width: Int, height: Int): Canvas
    fun finishPage()
}

/** Draws a [Report] as an A4 PDF that can be shared or printed. */
class PdfReportWriter(private val context: Context) {

    fun write(report: Report, out: OutputStream, generatedOn: LocalDate = LocalDate.now()) {
        val document = PdfDocument()
        try {
            render(report, generatedOn, object : PageSink {
                private var page: PdfDocument.Page? = null

                override fun startPage(number: Int, width: Int, height: Int): Canvas =
                    document.startPage(PdfDocument.PageInfo.Builder(width, height, number).create()).also { page = it }.canvas

                override fun finishPage() {
                    page?.let(document::finishPage)
                    page = null
                }
            })
            document.writeTo(out)
        } finally {
            document.close()
        }
    }

    /** Draws the report page by page into [sink]; returns the number of pages. */
    fun render(report: Report, generatedOn: LocalDate, sink: PageSink): Int = Pages(sink).run {
        header(report.period, generatedOn)
        summary(report)
        categories(context.getString(R.string.report_expense_categories), report.expenseCategories, report.expense, Expense)
        categories(context.getString(R.string.report_income_categories), report.incomeCategories, report.income, Income)
        transactions(report)
        finish()
    }

    /** Fills pages top to bottom, starting a new one when the next block doesn't fit. */
    private inner class Pages(private val sink: PageSink) {
        private var number = 0
        private var open = false
        private lateinit var canvas: Canvas
        private var y = 0f

        init {
            newPage()
        }

        private fun newPage() {
            if (open) finishPage()
            number++
            canvas = sink.startPage(number, PAGE_WIDTH, PAGE_HEIGHT)
            open = true
            y = MARGIN
        }

        private fun finishPage() {
            canvas.drawText(
                context.getString(R.string.report_page, number),
                PAGE_WIDTH - MARGIN,
                PAGE_HEIGHT - MARGIN / 2,
                paint(9f, Muted, align = Paint.Align.RIGHT),
            )
            sink.finishPage()
            open = false
        }

        /** Finishes the last page and returns how many there are. */
        fun finish(): Int {
            if (open) finishPage()
            return number
        }

        /** Makes sure [height] points are left on this page. */
        private fun ensure(height: Float) {
            if (y + height > PAGE_HEIGHT - MARGIN) newPage()
        }

        fun header(period: StatsPeriod, generatedOn: LocalDate) {
            canvas.drawText(context.getString(R.string.app_name).uppercase(Croatian), MARGIN, y + 10f, paint(9f, Accent, bold = true))
            y += 34f
            canvas.drawText(context.getString(R.string.report_title), MARGIN, y, paint(22f, Ink, bold = true))
            y += 22f
            canvas.drawText(periodTitle(period), MARGIN, y, paint(13f, Ink))
            canvas.drawText(
                context.getString(R.string.report_generated, DateFormat.long(generatedOn)),
                PAGE_WIDTH - MARGIN,
                y,
                paint(9f, Muted, align = Paint.Align.RIGHT),
            )
            y += 24f
        }

        fun summary(report: Report) {
            val gap = 12f
            val width = (CONTENT_WIDTH - 2 * gap) / 3
            listOf(
                Triple(context.getString(R.string.prihodi), report.income, Income),
                Triple(context.getString(R.string.rashodi), report.expense, Expense),
                Triple(context.getString(R.string.report_balance), report.balance, Ink),
            ).forEachIndexed { index, (label, amount, color) ->
                val left = MARGIN + index * (width + gap)
                canvas.drawRoundRect(RectF(left, y, left + width, y + 58f), 10f, 10f, paint(0f, Panel))
                canvas.drawText(label, left + 12f, y + 20f, paint(10f, Muted))
                canvas.drawText(MoneyFormat.format(amount), left + 12f, y + 43f, paint(15f, color, bold = true))
            }
            y += 58f + 28f
        }

        fun categories(title: String, categories: List<CategoryStat>, total: Double, color: Int) {
            if (categories.isEmpty()) return
            ensure(40f + ROW * minOf(categories.size, 3))
            sectionTitle(title)
            val largest = categories.first().amount
            val barLeft = MARGIN + 190f
            val barWidth = CONTENT_WIDTH - 190f - 150f
            categories.forEach { category ->
                ensure(ROW)
                val baseline = y + 13f
                canvas.drawText(
                    ellipsize(category.name.ifBlank { context.getString(R.string.no_category) }, 180f, 10.5f),
                    MARGIN,
                    baseline,
                    paint(10.5f, Ink),
                )
                // Track and fill, scaled to the largest category like on the statistics screen.
                canvas.drawRoundRect(RectF(barLeft, y + 6f, barLeft + barWidth, y + 13f), 3.5f, 3.5f, paint(0f, Panel))
                val fill = (category.amount / largest).toFloat() * barWidth
                canvas.drawRoundRect(RectF(barLeft, y + 6f, barLeft + fill.coerceAtLeast(3f), y + 13f), 3.5f, 3.5f, paint(0f, color))
                val share = if (total > 0) (category.amount / total * 100).roundToInt() else 0
                canvas.drawText("$share %", PAGE_WIDTH - MARGIN - 100f, baseline, paint(10f, Muted, align = Paint.Align.RIGHT))
                canvas.drawText(MoneyFormat.format(category.amount), PAGE_WIDTH - MARGIN, baseline, paint(10.5f, Ink, align = Paint.Align.RIGHT))
                y += ROW
            }
            y += 18f
        }

        fun transactions(report: Report) {
            ensure(60f)
            sectionTitle(context.getString(R.string.report_transactions))
            if (report.transactions.isEmpty()) {
                canvas.drawText(context.getString(R.string.report_no_transactions), MARGIN, y + 12f, paint(10.5f, Muted))
                y += ROW
                return
            }
            columnTitles()
            report.transactions.forEachIndexed { index, transaction ->
                if (y + ROW > PAGE_HEIGHT - MARGIN) {
                    newPage()
                    columnTitles()
                }
                if (index % 2 == 1) canvas.drawRect(MARGIN - 4f, y, PAGE_WIDTH - MARGIN + 4f, y + ROW, paint(0f, Stripe))
                val baseline = y + 13f
                val isIncome = transaction.type == TransactionType.INCOME
                canvas.drawText(TableDate.format(transaction.date), MARGIN, baseline, paint(10f, Ink))
                canvas.drawText(
                    ellipsize(transaction.category.ifBlank { context.getString(R.string.no_category) }, 140f, 10f),
                    MARGIN + COL_CATEGORY,
                    baseline,
                    paint(10f, Ink),
                )
                canvas.drawText(ellipsize(transaction.note, 190f, 10f), MARGIN + COL_NOTE, baseline, paint(10f, Muted))
                canvas.drawText(
                    MoneyFormat.signed(transaction.amount, isIncome),
                    PAGE_WIDTH - MARGIN,
                    baseline,
                    paint(10f, if (isIncome) Income else Expense, align = Paint.Align.RIGHT),
                )
                y += ROW
            }
        }

        private fun sectionTitle(title: String) {
            canvas.drawText(title, MARGIN, y + 12f, paint(13f, Ink, bold = true))
            y += 26f
        }

        private fun columnTitles() {
            val labels = paint(9f, Muted, bold = true)
            canvas.drawText(context.getString(R.string.datum).uppercase(Croatian), MARGIN, y + 10f, labels)
            canvas.drawText(context.getString(R.string.kategorija).uppercase(Croatian), MARGIN + COL_CATEGORY, y + 10f, labels)
            canvas.drawText(context.getString(R.string.report_note).uppercase(Croatian), MARGIN + COL_NOTE, y + 10f, labels)
            canvas.drawText(
                context.getString(R.string.iznos).uppercase(Croatian),
                PAGE_WIDTH - MARGIN,
                y + 10f,
                paint(9f, Muted, bold = true, align = Paint.Align.RIGHT),
            )
            y += 16f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint(0f, Line).apply { strokeWidth = 0.8f })
            y += 2f
        }
    }

    private fun periodTitle(period: StatsPeriod): String = when (period.range) {
        StatsRange.MONTH -> MonthTitle.format(period.month).replaceFirstChar { it.titlecase(Croatian) }
        StatsRange.YEAR -> "${period.month.year}."
        StatsRange.ALL -> context.getString(R.string.all_time)
    }

    private fun paint(size: Float, color: Int, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            textAlign = align
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

    /** Shortens [text] with "…" so it fits in [width] points. */
    private fun ellipsize(text: String, width: Float, size: Float): String {
        val measure = paint(size, Ink)
        if (measure.measureText(text) <= width) return text
        var end = text.length
        while (end > 0 && measure.measureText(text.take(end) + "…") > width) end--
        return text.take(end).trimEnd() + "…"
    }

    companion object {
        // A4 in PostScript points.
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        private const val MARGIN = 40f
        private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN
        private const val ROW = 20f
        private const val COL_CATEGORY = 80f
        private const val COL_NOTE = 230f

        // Same colours as the app's light theme.
        private val Ink = 0xFF12131A.toInt()
        private val Muted = 0xFF6B7080.toInt()
        private val Accent = 0xFF5B5BF6.toInt()
        private val Income = 0xFF12A150.toInt()
        private val Expense = 0xFFE5484D.toInt()
        private val Panel = 0xFFEEF0F6.toInt()
        private val Stripe = 0xFFF6F7FB.toInt()
        private val Line = 0xFFDADDE7.toInt()

        private val MonthTitle: DateTimeFormatter = DateTimeFormatter.ofPattern("LLLL yyyy.", Croatian)
        private val TableDate: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy.")
    }
}
