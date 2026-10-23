package com.example.financemanager.data.backup

import com.example.financemanager.data.Category
import com.example.financemanager.data.FinanceData
import com.example.financemanager.data.GoalContribution
import com.example.financemanager.data.PaymentPeriod
import com.example.financemanager.data.Reminder
import com.example.financemanager.data.SavingsGoal
import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalDateTime

/*
 * Backup file format: plain JSON, readable and independent of the database layout,
 * so a backup made by one version of the app can be restored by a later one.
 * Dates are ISO strings ("2026-10-21").
 */

const val BACKUP_FORMAT = "finance-manager-backup"
const val BACKUP_VERSION = 1

@Serializable
data class BackupFile(
    val format: String = BACKUP_FORMAT,
    val version: Int = BACKUP_VERSION,
    /** When the backup was made, ISO local date-time. */
    val createdAt: String,
    val categories: List<CategoryDto> = emptyList(),
    val transactions: List<TransactionDto> = emptyList(),
    val reminders: List<ReminderDto> = emptyList(),
    val goals: List<GoalDto> = emptyList(),
    @SerialName("goalContributions") val contributions: List<ContributionDto> = emptyList(),
)

@Serializable
data class CategoryDto(val id: Long, val name: String, val type: String, val monthlyBudget: Double? = null, val color: Int? = null)

@Serializable
data class TransactionDto(val id: Long, val amount: Double, val date: String, val note: String = "", val category: String = "", val type: String)

@Serializable
data class ReminderDto(
    val id: Long,
    val name: String,
    val amount: Double,
    val period: String,
    val firstDueDate: String,
    val paidCount: Int = 0,
    val category: String = "",
    val note: String = "",
)

@Serializable
data class GoalDto(val id: Long, val name: String, val target: Double, val deadline: String? = null, val color: Int? = null)

@Serializable
data class ContributionDto(val id: Long, val goalId: Long, val amount: Double, val date: String)

/** Why a file could not be restored. */
class InvalidBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun FinanceData.toBackup(createdAt: LocalDateTime = LocalDateTime.now()) = BackupFile(
    createdAt = createdAt.withNano(0).toString(),
    categories = categories.map { CategoryDto(it.id, it.name, it.type.name, it.monthlyBudget, it.color) },
    transactions = transactions.map { TransactionDto(it.id, it.amount, it.date.toString(), it.note, it.category, it.type.name) },
    reminders = reminders.map {
        ReminderDto(it.id, it.name, it.amount, it.period.name, it.firstDueDate.toString(), it.paidCount, it.category, it.note)
    },
    goals = goals.map { GoalDto(it.id, it.name, it.target, it.deadline?.toString(), it.color) },
    contributions = contributions.map { ContributionDto(it.id, it.goalId, it.amount, it.date.toString()) },
)

fun BackupFile.toJson(): String = json.encodeToString(BackupFile.serializer(), this)

/** Parses and checks a backup; throws [InvalidBackupException] if it isn't one or is damaged. */
fun parseBackup(text: String): BackupFile {
    val file = try {
        json.decodeFromString(BackupFile.serializer(), text)
    } catch (e: SerializationException) {
        throw InvalidBackupException("Not a backup file", e)
    } catch (e: IllegalArgumentException) {
        throw InvalidBackupException("Not a backup file", e)
    }
    if (file.format != BACKUP_FORMAT) throw InvalidBackupException("Unknown format ${file.format}")
    if (file.version > BACKUP_VERSION) throw InvalidBackupException("Made by a newer version of the app (${file.version})")
    return file
}

/** Converts back to entities, checking every value; throws [InvalidBackupException] on anything wrong. */
fun BackupFile.toData(): FinanceData = try {
    val goalIds = goals.map { it.id }.toSet()
    FinanceData(
        categories = categories.map { Category(it.id, it.name, TransactionType.valueOf(it.type), it.monthlyBudget, it.color) },
        transactions = transactions.map {
            Transaction(it.id, it.amount, LocalDate.parse(it.date), it.note, it.category, TransactionType.valueOf(it.type))
        },
        reminders = reminders.map {
            Reminder(it.id, it.name, it.amount, PaymentPeriod.valueOf(it.period), LocalDate.parse(it.firstDueDate), it.paidCount, it.category, it.note)
        },
        goals = goals.map { SavingsGoal(it.id, it.name, it.target, it.deadline?.let(LocalDate::parse), it.color) },
        contributions = contributions.map {
            if (it.goalId !in goalIds) throw InvalidBackupException("Contribution ${it.id} belongs to no goal")
            GoalContribution(it.id, it.goalId, it.amount, LocalDate.parse(it.date))
        },
    )
} catch (e: InvalidBackupException) {
    throw e
} catch (e: Exception) {
    // Unknown enum names, malformed dates and the like.
    throw InvalidBackupException("Damaged backup: ${e.message}", e)
}

/** The date-time the backup was made, if readable. */
val BackupFile.created: LocalDateTime? get() = runCatching { LocalDateTime.parse(createdAt) }.getOrNull()
