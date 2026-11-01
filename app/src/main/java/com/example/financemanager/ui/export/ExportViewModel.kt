package com.example.financemanager.ui.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financemanager.data.FinanceData
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.data.SettingsRepository
import com.example.financemanager.data.backup.BackupFile
import com.example.financemanager.data.backup.parseBackup
import com.example.financemanager.data.backup.toBackup
import com.example.financemanager.data.backup.toData
import com.example.financemanager.data.backup.toJson
import com.example.financemanager.domain.BudgetMonths
import com.example.financemanager.domain.export.CsvLabels
import com.example.financemanager.domain.export.Report
import com.example.financemanager.domain.export.buildReport
import com.example.financemanager.domain.export.transactionsCsv
import com.example.financemanager.domain.stats.StatsPeriod
import com.example.financemanager.domain.stats.StatsRange
import com.example.financemanager.ui.budgetMonths
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
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
    months: Flow<BudgetMonths> = flowOf(BudgetMonths()),
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    // Hilt uses this one; tests pass their own [today].
    @Inject
    constructor(repository: FinanceRepository, settings: SettingsRepository) :
        this(repository, settings.budgetMonths, LocalDate::now)

    /** Null until the user picks a period: then the current month is used. */
    private val picked = MutableStateFlow<StatsPeriod?>(null)

    val state: StateFlow<ExportUiState> = combine(repository.transactions(), picked, months) { transactions, picked, months ->
        val period = picked?.copy(months = months) ?: StatsPeriod.thisMonth(today(), months)
        ExportUiState(
            period = period,
            canGoForward = period.shift(1)?.isFuture(today()) == false,
            count = transactions.count { it.date in period },
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExportUiState(period = StatsPeriod.thisMonth(today())))

    /** The period the export is for. */
    private val period: StatsPeriod get() = state.value.period

    fun onRangeChange(range: StatsRange) {
        picked.value = period.copy(range = range)
    }

    /** Moves one month or year back (-1) or forward (+1), never past today. */
    fun shift(steps: Long) {
        period.shift(steps)?.takeUnless { it.isFuture(today()) }?.let { picked.value = it }
    }

    /** Suggested file name part for the period: "2026-10", "2026" or "sve". */
    fun periodSlug(allTime: String): String {
        val period = period
        return when (period.range) {
            StatsRange.MONTH -> period.month.toString()
            StatsRange.YEAR -> period.month.year.toString()
            StatsRange.ALL -> allTime
        }
    }

    suspend fun csv(labels: CsvLabels): String {
        val period = period
        return transactionsCsv(repository.transactions().first().filter { it.date in period }, labels)
    }

    suspend fun report(): Report = buildReport(repository.transactions().first(), period, today())

    suspend fun backupJson(now: LocalDateTime = LocalDateTime.now()): String = repository.snapshot().toBackup(now).toJson()

    /** Reads and checks a backup without changing anything; throws InvalidBackupException if it can't be used. */
    fun readBackup(text: String): BackupPreview {
        val file = parseBackup(text)
        return BackupPreview(file, file.toData())
    }

    /** Replaces all data with the backup's. */
    suspend fun restore(preview: BackupPreview) = repository.replaceAll(preview.data)
}
