package com.example.financemanager.ui.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financemanager.data.FinanceData
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.data.backup.BackupFile
import com.example.financemanager.data.backup.parseBackup
import com.example.financemanager.data.backup.toBackup
import com.example.financemanager.data.backup.toData
import com.example.financemanager.data.backup.toJson
import com.example.financemanager.export.CsvLabels
import com.example.financemanager.export.Report
import com.example.financemanager.export.buildReport
import com.example.financemanager.export.transactionsCsv
import com.example.financemanager.ui.stats.StatsPeriod
import com.example.financemanager.ui.stats.StatsRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class ExportUiState(
    val period: StatsPeriod = StatsPeriod.thisMonth(),
    val canGoForward: Boolean = false,
    /** Transactions in [period]. */
    val count: Int = 0,
    val isLoading: Boolean = true,
)

/** A backup that was read and checked, waiting for the user to confirm the restore. */
data class BackupPreview(val file: BackupFile, val data: FinanceData)

@HiltViewModel
class ExportViewModel(
    private val repository: FinanceRepository,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    // Hilt uses this one; tests pass their own [today].
    @Inject
    constructor(repository: FinanceRepository) : this(repository, LocalDate::now)

    private val period = MutableStateFlow(StatsPeriod.thisMonth(today()))

    val state: StateFlow<ExportUiState> = combine(repository.transactions(), period) { transactions, period ->
        ExportUiState(
            period = period,
            canGoForward = period.shift(1)?.isFuture(today()) == false,
            count = transactions.count { it.date in period },
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExportUiState(period = period.value))

    fun onRangeChange(range: StatsRange) = period.update { it.copy(range = range) }

    /** Moves one month or year back (-1) or forward (+1), never past today. */
    fun shift(steps: Long) = period.update { current ->
        current.shift(steps)?.takeUnless { it.isFuture(today()) } ?: current
    }

    /** Suggested file name part for the period: "2026-10", "2026" or "sve". */
    fun periodSlug(allTime: String): String {
        val period = period.value
        return when (period.range) {
            StatsRange.MONTH -> period.month.toString()
            StatsRange.YEAR -> period.month.year.toString()
            StatsRange.ALL -> allTime
        }
    }

    suspend fun csv(labels: CsvLabels): String {
        val period = period.value
        return transactionsCsv(repository.transactions().first().filter { it.date in period }, labels)
    }

    suspend fun report(): Report = buildReport(repository.transactions().first(), period.value, today())

    suspend fun backupJson(now: LocalDateTime = LocalDateTime.now()): String = repository.snapshot().toBackup(now).toJson()

    /** Reads and checks a backup without changing anything; throws InvalidBackupException if it can't be used. */
    fun readBackup(text: String): BackupPreview {
        val file = parseBackup(text)
        return BackupPreview(file, file.toData())
    }

    /** Replaces all data with the backup's. */
    suspend fun restore(preview: BackupPreview) = repository.replaceAll(preview.data)
}
