package com.example.financemanager.receipt

import com.example.financemanager.data.Category
import com.example.financemanager.data.TransactionType
import java.text.Normalizer
import java.time.DateTimeException
import java.time.LocalDate

/** One line of text found on the photo, with where it is (pixels, top-left origin). */
data class OcrLine(val text: String, val top: Int, val bottom: Int, val left: Int) {
    val centerY: Int get() = (top + bottom) / 2
}

/** What could be read from a receipt; anything not found is null. */
data class ReceiptData(val amount: Double?, val date: LocalDate?, val merchant: String?) {
    val isEmpty: Boolean get() = amount == null && date == null && merchant == null
}

/**
 * Reads Croatian fiscal receipts (and most others): the total, the date and the shop.
 *
 * Text recognition returns lines in no reliable order and often splits a row in two, with
 * "UKUPNO" on the left and the amount on the right, so lines are matched by position: an
 * amount on the same row as a total keyword, or else on the next row below it.
 */
object ReceiptParser {

    /** Words that introduce the total, best first. Compared without case and diacritics. */
    private val TotalKeywords = listOf(
        listOf("ZA PLATITI", "ZA PLACANJE", "ZA PLATIT"),
        listOf("SVEUKUPNO", "UKUPNO", "UKUPAN IZNOS", "IZNOS RACUNA", "IZNOS ZA NAPLATU"),
        listOf("TOTAL", "SUMA"),
    )

    /** Rows with these are never the total: tax, cash handed over, change. */
    private val NotTotal = listOf("PDV", "OSNOVICA", "POREZ", "GOTOVINA", "PRIMLJENO", "VRACENO", "POVRAT", "OSTATAK", "PNP")

    /** Lines near the top that name the receipt, not the shop. */
    private val NotMerchant = listOf("OIB", "RACUN", "BROJ", "DATUM", "TEL", "FAX", "WWW", "HTTP", "@", "ADRESA", "BLAGAJN", "IBAN")

    // 1.234,56 · 1234,56 · 12.34 ; not part of a longer number or a percentage. Spaces as
    // thousands separators are left out: "2 239,00" is more likely a quantity and a price.
    private val Amount = Regex("""(?<![\d.,])(\d{1,3}(?:\.\d{3})+|\d+)[,.](\d{2})(?![\d%]|\s?%)""")
    private val DottedDate = Regex("""(?<!\d)(\d{1,2})\.\s?(\d{1,2})\.\s?(\d{4}|\d{2})(?!\d)""")
    private val IsoDate = Regex("""(?<!\d)(\d{4})-(\d{2})-(\d{2})(?!\d)""")

    fun parse(lines: List<OcrLine>, today: LocalDate = LocalDate.now()): ReceiptData {
        val sorted = lines.filter { it.text.isNotBlank() }.sortedWith(compareBy<OcrLine> { it.top }.thenBy { it.left })
        return ReceiptData(total(sorted), date(sorted, today), merchant(sorted))
    }

    internal fun amounts(text: String): List<Double> = Amount.findAll(text).mapNotNull { match ->
        val whole = match.groupValues[1].replace(".", "")
        "$whole.${match.groupValues[2]}".toDoubleOrNull()?.takeIf { it > 0 }
    }.toList()

    private fun total(lines: List<OcrLine>): Double? {
        for (keywords in TotalKeywords) {
            val candidates = lines
                .filter { line -> normalize(line.text).let { text -> keywords.any { it in text } && NotTotal.none { it in text } } }
                .mapNotNull { amountFor(it, lines) }
            // Several "UKUPNO" rows (per tax rate, say) — the grand total is the biggest.
            candidates.maxOrNull()?.let { return it }
        }
        // No keyword found: the biggest amount that isn't tax or cash handed over.
        return lines.filter { line -> NotTotal.none { it in normalize(line.text) } }.flatMap { amounts(it.text) }.maxOrNull()
    }

    /** The amount belonging to a total keyword: on its line, on the same row, or on the row below. */
    private fun amountFor(keyword: OcrLine, lines: List<OcrLine>): Double? {
        amounts(keyword.text).lastOrNull()?.let { return it }
        val height = (keyword.bottom - keyword.top).coerceAtLeast(1)
        val sameRow = lines.filter { it !== keyword && it.left > keyword.left && it.centerY in keyword.top..keyword.bottom }
        sameRow.flatMap { amounts(it.text) }.lastOrNull()?.let { return it }
        return lines
            .filter { it.top >= keyword.bottom && it.top - keyword.bottom <= 2 * height }
            .firstNotNullOfOrNull { line -> amounts(line.text).takeIf { NotTotal.none { it in normalize(line.text) } }?.lastOrNull() }
    }

    private fun date(lines: List<OcrLine>, today: LocalDate): LocalDate? = lines.asSequence()
        .flatMap { line ->
            DottedDate.findAll(line.text).map { m ->
                val year = m.groupValues[3].toInt().let { if (it < 100) 2000 + it else it }
                Triple(year, m.groupValues[2].toInt(), m.groupValues[1].toInt())
            } + IsoDate.findAll(line.text).map { m ->
                Triple(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())
            }
        }
        .mapNotNull { (year, month, day) ->
            try {
                LocalDate.of(year, month, day)
            } catch (_: DateTimeException) {
                null
            }
        }
        // A receipt is from the past (a day of slack for time zones), and not ancient.
        .firstOrNull { it.year >= 2000 && !it.isAfter(today.plusDays(1)) }

    private fun merchant(lines: List<OcrLine>): String? = lines.take(6).firstNotNullOfOrNull { line ->
        val text = line.text.trim()
        val normalized = normalize(text)
        val letters = text.count { it.isLetter() }
        if (letters < 3 || letters < text.length / 2 || NotMerchant.any { it in normalized }) return@firstNotNullOfOrNull null
        tidyName(text)
    }

    /** "KONZUM PLUS d.o.o." becomes "Konzum Plus". */
    private fun tidyName(text: String): String {
        val withoutForm = text.replace(Regex("""[,\s]+(d\.?\s?o\.?\s?o\.?|j\.?\s?d\.?\s?o\.?\s?o\.?|d\.?\s?d\.?|obrt)\s*$""", RegexOption.IGNORE_CASE), "")
        val shouting = withoutForm.none { it.isLowerCase() }
        return if (!shouting) withoutForm.trim() else withoutForm.trim().lowercase().split(' ').joinToString(" ") { word ->
            word.replaceFirstChar { it.titlecase() }
        }
    }

    /** Upper case without diacritics, so "Za plaćanje" matches "ZA PLACANJE". */
    internal fun normalize(text: String): String =
        Normalizer.normalize(text.uppercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").replace('Đ', 'D')

    /** Kinds of shops, and words that the user's matching category would contain. */
    private val ShopKinds = listOf(
        listOf("KONZUM", "LIDL", "SPAR", "INTERSPAR", "KAUFLAND", "PLODINE", "TOMMY", "STUDENAC", "EUROSPIN", "KTC", "RIBOLA", "TRGOVINA", "MARKET", "PEKARA", "MLINAR") to
            listOf("HRAN", "NAMIRN", "TRGOVIN", "MARKET"),
        listOf("INA", "PETROL", "TIFON", "CRODUX", "LUKOIL", "SHELL", "MOL", "BENZINSKA") to listOf("GORIV", "AUTO", "PRIJEVOZ"),
        listOf("DM", "MULLER", "BIPA") to listOf("DROGERIJ", "HIGIJEN", "KOZMETIK"),
        listOf("LJEKARNA", "LJEKARNE") to listOf("ZDRAVLJ", "LJEK"),
        listOf("RESTORAN", "PIZZERIA", "BISTRO", "CAFFE", "CAFE", "KAFIC", "MCDONALD", "BURGER") to listOf("RESTORAN", "IZLAS", "KAFIC", "HRAN"),
    )

    /** An existing expense category that suits the shop on the receipt, if there is one. */
    fun guessCategory(lines: List<OcrLine>, categories: List<Category>): String? {
        val words = lines.joinToString(" ") { normalize(it.text) }.split(Regex("[^A-Z0-9]+")).toSet()
        val expense = categories.filter { it.type == TransactionType.EXPENSE }
        return ShopKinds.firstNotNullOfOrNull { (shops, names) ->
            if (shops.none { it in words }) return@firstNotNullOfOrNull null
            names.firstNotNullOfOrNull { name -> expense.firstOrNull { name in normalize(it.name) }?.name }
        }
    }
}
