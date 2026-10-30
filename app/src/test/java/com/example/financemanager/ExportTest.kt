package com.example.financemanager

import com.example.financemanager.data.FinanceData
import com.example.financemanager.data.backup.InvalidBackupException
import com.example.financemanager.data.backup.parseBackup
import com.example.financemanager.data.backup.toBackup
import com.example.financemanager.data.backup.toData
import com.example.financemanager.data.backup.toJson
import com.example.financemanager.domain.export.CsvLabels
import com.example.financemanager.domain.export.buildReport
import com.example.financemanager.domain.export.transactionsCsv
import com.example.financemanager.domain.stats.StatsPeriod
import com.example.financemanager.domain.stats.StatsRange
import com.example.financemanager.model.Category
import com.example.financemanager.model.GoalContribution
import com.example.financemanager.model.PaymentPeriod
import com.example.financemanager.model.Reminder
import com.example.financemanager.model.SavingsGoal
import com.example.financemanager.model.Transaction
import com.example.financemanager.model.TransactionType.EXPENSE
import com.example.financemanager.model.TransactionType.INCOME
import com.example.financemanager.ui.export.ExportViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class ExportTest {

    private val today = LocalDate.of(2026, 10, 21)
    private val labels = CsvLabels("Datum", "Vrsta", "Kategorija", "Iznos", "Napomena", "Prihod", "Rashod")

    private val data = FinanceData(
        categories = listOf(Category(1, "Hrana", EXPENSE, monthlyBudget = 300.0, color = 3), Category(2, "Plaća", INCOME)),
        transactions = listOf(
            Transaction(5, 1234.5, LocalDate.of(2026, 10, 3), "Konzum; veliki \"tjedni\" šoping", "Hrana", EXPENSE),
            Transaction(4, 1850.0, LocalDate.of(2026, 10, 1), "", "Plaća", INCOME),
            Transaction(6, 20.0, LocalDate.of(2026, 9, 30), "", "Hrana", EXPENSE),
        ),
        reminders = listOf(Reminder(7, "Struja", 40.0, PaymentPeriod.MONTHLY, LocalDate.of(2026, 10, 15), 2, "Režije", "HEP")),
        goals = listOf(SavingsGoal(8, "Ljetovanje", 1500.0, LocalDate.of(2027, 6, 1), 1), SavingsGoal(9, "Laptop", 900.0)),
        contributions = listOf(GoalContribution(10, 8, 300.0, LocalDate.of(2026, 10, 2)), GoalContribution(11, 8, -50.0, today)),
    )

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun csvOpensInCroatianExcel() {
        val csv = transactionsCsv(data.transactions, labels)
        assertTrue(csv.startsWith("\uFEFF"))
        val rows = csv.removePrefix("\uFEFF").split("\r\n").filter { it.isNotEmpty() }
        assertEquals(
            listOf(
                "Datum;Vrsta;Kategorija;Iznos;Napomena",
                "30.09.2026.;Rashod;Hrana;-20,00;",
                "01.10.2026.;Prihod;Plaća;1850,00;",
                // The separator and quotes inside the note are escaped.
                "03.10.2026.;Rashod;Hrana;-1234,50;\"Konzum; veliki \"\"tjedni\"\" šoping\"",
            ),
            rows,
        )
    }

    @Test
    fun reportCoversOnlyThePeriod() {
        val report = buildReport(data.transactions, StatsPeriod(StatsRange.MONTH, YearMonth.of(2026, 10)), today)
        assertEquals(1850.0, report.income, 0.0)
        assertEquals(1234.5, report.expense, 0.0)
        assertEquals(615.5, report.balance, 1e-9)
        assertEquals(listOf("Hrana"), report.expenseCategories.map { it.name })
        assertEquals(listOf(4L, 5L), report.transactions.map { it.id })
    }

    @Test
    fun backupRoundTripKeepsEverything() {
        val json = data.toBackup(LocalDateTime.of(2026, 10, 21, 14, 3, 9, 123)).toJson()
        assertTrue(json.contains("\"format\": \"finance-manager-backup\""))
        val file = parseBackup(json)
        assertEquals("2026-10-21T14:03:09", file.createdAt)
        assertEquals(data, file.toData())
    }

    @Test
    fun badFilesAreRejected() {
        val good = data.toBackup(LocalDateTime.of(2026, 10, 21, 14, 3)).toJson()
        assertThrows(InvalidBackupException::class.java) { parseBackup("not json") }
        assertThrows(InvalidBackupException::class.java) { parseBackup("""{"some": "other json"}""") }
        assertThrows(InvalidBackupException::class.java) { parseBackup(good.replace("finance-manager-backup", "something-else")) }
        assertThrows(InvalidBackupException::class.java) { parseBackup(good.replace("\"version\": 1", "\"version\": 99")) }
        assertThrows(InvalidBackupException::class.java) { parseBackup(good.replace("\"EXPENSE\"", "\"SPENDING\"")).toData() }
        assertThrows(InvalidBackupException::class.java) { parseBackup(good.replace("2026-10-03", "3. 10. 2026.")).toData() }
        assertThrows(InvalidBackupException::class.java) {
            data.copy(goals = emptyList()).toBackup().let { parseBackup(it.toJson()).toData() }
        }
    }

    @Test
    fun restoringReplacesAllData() = runTest(UnconfinedTestDispatcher()) {
        val repository = FakeRepository().apply {
            transactions.value = listOf(Transaction(99, 1.0, today, "", "Stara", EXPENSE))
        }
        val viewModel = ExportViewModel(repository) { today }
        backgroundScope.launch { viewModel.state.collect {} }
        assertEquals(1, viewModel.state.value.count)
        assertEquals("2026-10", viewModel.periodSlug("sve"))
        viewModel.onRangeChange(StatsRange.ALL)
        assertEquals("sve", viewModel.periodSlug("sve"))

        val preview = viewModel.readBackup(data.toBackup().toJson())
        viewModel.restore(preview)
        assertEquals(data, repository.snapshot())
        assertEquals(3, viewModel.state.value.count)

        // And a backup made now gives the same data back.
        assertEquals(data, viewModel.readBackup(viewModel.backupJson()).data)
    }
}
