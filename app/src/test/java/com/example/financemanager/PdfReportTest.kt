package com.example.financemanager

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import com.example.financemanager.export.PageSink
import com.example.financemanager.export.PdfReportWriter
import com.example.financemanager.export.buildReport
import com.example.financemanager.ui.stats.StatsPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** Robolectric has no PDF engine, so the pages are drawn into bitmaps instead. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PdfReportTest {

    private class BitmapPages : PageSink {
        val pages = mutableListOf<Bitmap>()
        var finished = 0

        override fun startPage(number: Int, width: Int, height: Int): Canvas {
            assertEquals(pages.size + 1, number)
            return Canvas(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { pages += it }).apply { drawColor(Color.WHITE) }
        }

        override fun finishPage() {
            finished++
        }
    }

    @Test
    fun longReportsContinueOnNewPages() {
        val today = LocalDate.of(2026, 10, 21)
        val transactions = (1L..80L).map {
            Transaction(it, it * 1.5, today.minusDays(it % 20), "Napomena $it", if (it % 3 == 0L) "Hrana" else "Režije", TransactionType.EXPENSE)
        } + Transaction(100, 1850.0, today, "", "Plaća", TransactionType.INCOME)
        val sink = BitmapPages()

        val pages = PdfReportWriter(ApplicationProvider.getApplicationContext())
            .render(buildReport(transactions, StatsPeriod.thisMonth(today), today), today, sink)

        assertTrue("expected more than one page, got $pages", pages > 1)
        assertEquals(pages, sink.pages.size)
        assertEquals(pages, sink.finished)
        // Something was drawn on every page.
        sink.pages.forEach { page ->
            val pixels = IntArray(page.width * page.height).also { page.getPixels(it, 0, page.width, 0, 0, page.width, page.height) }
            assertTrue(pixels.any { it != Color.WHITE })
        }
    }
}
