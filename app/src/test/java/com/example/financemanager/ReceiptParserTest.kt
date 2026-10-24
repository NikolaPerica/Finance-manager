package com.example.financemanager

import com.example.financemanager.data.Category
import com.example.financemanager.data.TransactionType
import com.example.financemanager.receipt.OcrLine
import com.example.financemanager.receipt.ReceiptParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReceiptParserTest {

    private val today = LocalDate.of(2026, 10, 24)

    /** Lines one under the other, each on its own row, as text recognition might return them. */
    private fun rows(vararg text: String) = text.mapIndexed { i, line -> OcrLine(line, top = i * 40, bottom = i * 40 + 30, left = 20) }

    @Test
    fun typicalSupermarketReceipt() {
        val receipt = ReceiptParser.parse(
            rows(
                "KONZUM plus d.o.o.",
                "Marijana Čavića 1a, Zagreb",
                "OIB: 62226620908",
                "Račun br. 1234/PJ-123/1",
                "Mlijeko 2,8% 1L        1,29",
                "Kruh pšenični          1,99",
                "2 x 3,50               7,00",
                "UKUPNO EUR            10,28",
                "PDV 13%  osnovica 9,10  1,18",
                "Kartica               10,28",
                "Datum: 21.10.2026. 14:03",
                "JIR: 2b8e6f1c-...",
            ),
            today,
        )
        assertEquals(10.28, receipt.amount!!, 0.0)
        assertEquals(LocalDate.of(2026, 10, 21), receipt.date)
        assertEquals("KONZUM plus", receipt.merchant)
    }

    @Test
    fun cashPaidIsNotTheTotal() {
        val receipt = ReceiptParser.parse(
            rows(
                "PETROL d.o.o.",
                "EUROSUPER 95  32,15 L x 1,56   50,15",
                "ZA PLATITI:    50,15",
                "GOTOVINA:     100,00",
                "VRAĆENO:       49,85",
                "21.10.26 08:15",
            ),
            today,
        )
        assertEquals(50.15, receipt.amount!!, 0.0)
        assertEquals(LocalDate.of(2026, 10, 21), receipt.date)
        assertEquals("Petrol", receipt.merchant)
    }

    @Test
    fun amountOnTheSameRowButASeparateLine() {
        // Recognition often returns "UKUPNO" and its amount as two lines side by side.
        val lines = listOf(
            OcrLine("LIDL Hrvatska d.o.o. k.d.", 0, 30, 20),
            OcrLine("UKUPNO", 400, 440, 20),
            OcrLine("Jabuke", 300, 340, 20),
            OcrLine("2,49", 300, 340, 500),
            OcrLine("23,90", 402, 442, 500),
            OcrLine("Datum 2026-10-20", 500, 540, 20),
        )
        val receipt = ReceiptParser.parse(lines, today)
        assertEquals(23.9, receipt.amount!!, 0.0)
        assertEquals(LocalDate.of(2026, 10, 20), receipt.date)
    }

    @Test
    fun amountOnTheRowBelowTheKeyword() {
        val receipt = ReceiptParser.parse(rows("Caffe bar Mali", "Za plaćanje (EUR):", "1.234,56", "Hvala!"), today)
        assertEquals(1234.56, receipt.amount!!, 0.0)
        assertEquals("Caffe bar Mali", receipt.merchant)
    }

    @Test
    fun withoutKeywordsTheLargestNonTaxAmountIsTaken() {
        val receipt = ReceiptParser.parse(rows("Pekara Klara", "Burek 2,80", "Jogurt 1,10", "3,90", "PDV 0,45"), today)
        assertEquals(3.9, receipt.amount!!, 0.0)
    }

    @Test
    fun percentagesAndFutureDatesAreIgnored() {
        assertEquals(listOf(1.29), ReceiptParser.amounts("Mlijeko 2,80% 1,29"))
        assertEquals(emptyList<Double>(), ReceiptParser.amounts("PDV 25,00 %"))
        val receipt = ReceiptParser.parse(rows("SPAR", "Akcija vrijedi do 31.12.2026.", "Datum: 20.10.2026", "UKUPNO 5,00"), today)
        assertEquals(LocalDate.of(2026, 10, 20), receipt.date)
    }

    @Test
    fun nothingRecognised() {
        val receipt = ReceiptParser.parse(rows("~~~", "12"), today)
        assertNull(receipt.amount)
        assertNull(receipt.date)
        assertTrue(ReceiptParser.parse(emptyList(), today).isEmpty)
    }

    @Test
    fun categoryFromTheShop() {
        val categories = listOf(
            Category(1, "Namirnice", TransactionType.EXPENSE),
            Category(2, "Gorivo", TransactionType.EXPENSE),
            Category(3, "Plaća", TransactionType.INCOME),
        )
        assertEquals("Namirnice", ReceiptParser.guessCategory(rows("KONZUM plus d.o.o."), categories))
        assertEquals("Gorivo", ReceiptParser.guessCategory(rows("INA d.d.", "Benzinska postaja"), categories))
        // No matching category, or a shop name only inside another word ("Marina" isn't INA).
        assertNull(ReceiptParser.guessCategory(rows("dm drogerie markt"), categories))
        assertNull(ReceiptParser.guessCategory(rows("Restoran Marina"), categories))
    }
}
