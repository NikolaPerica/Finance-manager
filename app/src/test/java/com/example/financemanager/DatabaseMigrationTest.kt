package com.example.financemanager

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.financemanager.data.AppDatabase
import com.example.financemanager.data.MIGRATION_1_2
import com.example.financemanager.data.MIGRATION_2_3
import com.example.financemanager.data.MIGRATION_3_4
import com.example.financemanager.data.MIGRATION_4_5
import com.example.financemanager.data.PaymentPeriod
import com.example.financemanager.data.GoalContribution
import com.example.financemanager.data.Reminder
import com.example.financemanager.data.RoomFinanceRepository
import com.example.financemanager.data.SavingsGoal
import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONObject
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    /** Creates the database exactly as [version] of the app left it, from the exported schema. */
    private fun createVersion(version: Int, fill: (SQLiteDatabase) -> Unit) {
        val schema = JSONObject(File("schemas/${AppDatabase::class.java.name}/$version.json").readText()).getJSONObject("database")
        val file = context.getDatabasePath(DB).apply { parentFile!!.mkdirs(); delete() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            db.version = version
            fill(db)
        }
    }

    @Test
    fun version1DataSurvivesTheUpgrade() = runBlocking {
        createVersion(1) { db ->
            db.execSQL("INSERT INTO categories (id, name, type) VALUES (1, 'Hrana', 'EXPENSE')")
            db.execSQL(
                "INSERT INTO transactions (id, amount, date, note, category, type) " +
                    "VALUES (1, 64.3, '2026-10-05', 'Konzum', 'Hrana', 'EXPENSE')",
            )
        }

        // Room checks the migrated tables against the current schema when it opens.
        val db = Room.databaseBuilder(context, AppDatabase::class.java, DB)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
        try {
            val transaction = db.transactionDao().observeAll().first().single()
            assertEquals(LocalDate.of(2026, 10, 5), transaction.date)
            assertEquals("Konzum", transaction.note)
            assertEquals("Hrana", db.categoryDao().observeByType(TransactionType.EXPENSE).first().single().name)
            assertTrue(db.reminderDao().getAll().isEmpty())
            // The new reminders table is usable.
            db.reminderDao().upsert(
                Reminder(name = "Struja", amount = 40.0, period = PaymentPeriod.MONTHLY, firstDueDate = LocalDate.of(2026, 10, 15)),
            )
            assertEquals("Struja", db.reminderDao().getAll().single().name)
        } finally {
            db.close()
        }
    }

    @Test
    fun version2GetsEmptyBudgets() = runBlocking {
        createVersion(2) { db ->
            db.execSQL("INSERT INTO categories (id, name, type) VALUES (1, 'Hrana', 'EXPENSE')")
            db.execSQL(
                "INSERT INTO reminders (id, name, amount, period, firstDueDate, paidCount, category, note) " +
                    "VALUES (1, 'Struja', 40.0, 'MONTHLY', '2026-10-15', 2, 'Režije', '')",
            )
        }

        val db = Room.databaseBuilder(context, AppDatabase::class.java, DB)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
        try {
            val repository = RoomFinanceRepository(db)
            val category = repository.categories(TransactionType.EXPENSE).first().single()
            assertNull(category.monthlyBudget)
            assertEquals(2, repository.reminder(1)!!.paidCount)

            repository.setBudget(category, 300.0)
            assertEquals(300.0, repository.categories(TransactionType.EXPENSE).first().single().monthlyBudget!!, 0.0)
            repository.setBudget(category, null)
            assertNull(repository.categories(TransactionType.EXPENSE).first().single().monthlyBudget)
        } finally {
            db.close()
        }
    }

    @Test
    fun version3KeepsBudgetsAndGetsColours() = runBlocking {
        createVersion(3) { db ->
            db.execSQL("INSERT INTO categories (id, name, type, monthlyBudget) VALUES (3, 'Hrana', 'EXPENSE', 250.0)")
        }
        val db = Room.databaseBuilder(context, AppDatabase::class.java, DB)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
        try {
            val category = db.categoryDao().observeByType(TransactionType.EXPENSE).first().single()
            assertEquals(250.0, category.monthlyBudget!!, 0.0)
            assertNull(category.color)
            assertEquals(3, category.colorIndex)
        } finally {
            db.close()
        }
    }

    @Test
    fun renamingAndMergingCategoriesMovesTheirData() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = RoomFinanceRepository(db)
            repository.addCategory("Hrana", TransactionType.EXPENSE)
            repository.addCategory("Namirnice", TransactionType.EXPENSE)
            repository.addCategory("Hrana", TransactionType.INCOME)
            repository.addTransaction(Transaction(amount = 10.0, date = LocalDate.of(2026, 10, 1), note = "", category = "Hrana", type = TransactionType.EXPENSE))
            repository.addTransaction(Transaction(amount = 99.0, date = LocalDate.of(2026, 10, 1), note = "", category = "Hrana", type = TransactionType.INCOME))
            repository.saveReminder(
                Reminder(name = "Dostava", amount = 20.0, period = PaymentPeriod.MONTHLY, firstDueDate = LocalDate.of(2026, 10, 9), category = "Hrana"),
            )
            val (food, groceries) = repository.categories(TransactionType.EXPENSE).first()

            repository.updateCategory(food, "Hrana i piće", color = 5)
            val renamed = repository.categories(TransactionType.EXPENSE).first().first { it.id == food.id }
            assertEquals("Hrana i piće", renamed.name)
            assertEquals(5, renamed.colorIndex)
            // Only the expense transaction and the reminder follow; the income category of the same name is separate.
            val byType = repository.transactions().first().associate { it.type to it.category }
            assertEquals("Hrana i piće", byType[TransactionType.EXPENSE])
            assertEquals("Hrana", byType[TransactionType.INCOME])
            assertEquals("Hrana i piće", repository.reminders().first().single().category)

            repository.deleteCategory(renamed, moveTo = groceries)
            assertEquals(listOf("Namirnice"), repository.categories(TransactionType.EXPENSE).first().map { it.name })
            assertEquals("Namirnice", repository.transactions().first().first { it.type == TransactionType.EXPENSE }.category)
            assertEquals("Namirnice", repository.reminders().first().single().category)

            repository.deleteCategory(groceries, moveTo = null)
            assertEquals("", repository.transactions().first().first { it.type == TransactionType.EXPENSE }.category)
        } finally {
            db.close()
        }
    }

    @Test
    fun version4GetsGoalsThatAddUpAndDeleteWithTheirHistory() = runBlocking {
        createVersion(4) { db ->
            db.execSQL("INSERT INTO categories (id, name, type) VALUES (1, 'Hrana', 'EXPENSE')")
        }
        val db = Room.databaseBuilder(context, AppDatabase::class.java, DB)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
        try {
            val repository = RoomFinanceRepository(db)
            assertEquals("Hrana", repository.categories(TransactionType.EXPENSE).first().single().name)
            assertTrue(repository.goals().first().isEmpty())

            val trip = repository.saveGoal(SavingsGoal(name = "Ljetovanje", target = 1500.0, deadline = LocalDate.of(2027, 6, 1)))
            val laptop = repository.saveGoal(SavingsGoal(name = "Laptop", target = 900.0))
            repository.addContribution(GoalContribution(goalId = trip, amount = 300.0, date = LocalDate.of(2026, 10, 1)))
            repository.addContribution(GoalContribution(goalId = trip, amount = -50.0, date = LocalDate.of(2026, 10, 2)))

            // The goal with a deadline comes first; a goal without contributions has saved nothing.
            val goals = repository.goals().first()
            assertEquals(listOf("Ljetovanje", "Laptop"), goals.map { it.goal.name })
            assertEquals(250.0, goals[0].saved, 0.0)
            assertEquals(0.0, goals[1].saved, 0.0)
            assertEquals(LocalDate.of(2027, 6, 1), repository.goal(trip)!!.deadline)
            assertNull(repository.goal(laptop)!!.deadline)

            repository.deleteGoal(repository.goal(trip)!!)
            assertTrue(repository.contributions(trip).first().isEmpty())
            assertEquals(listOf("Laptop"), repository.goals().first().map { it.goal.name })
        } finally {
            db.close()
        }
    }

    @Test
    fun payingAndUndoingAReminderInTheDatabase() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = RoomFinanceRepository(db)
            repository.saveReminder(
                Reminder(name = "Stanarina", amount = 520.0, period = PaymentPeriod.MONTHLY, firstDueDate = LocalDate.of(2026, 10, 1)),
            )
            val reminder = repository.reminders().first().single()

            val payment = repository.payReminder(reminder, LocalDate.of(2026, 10, 2))
            assertEquals(LocalDate.of(2026, 11, 1), repository.reminder(reminder.id)!!.nextDueDate)
            assertEquals(520.0, repository.transactions().first().single().amount, 0.0)

            repository.undoPayment(payment)
            assertEquals(LocalDate.of(2026, 10, 1), repository.reminder(reminder.id)!!.nextDueDate)
            assertTrue(repository.transactions().first().isEmpty())
        } finally {
            db.close()
        }
    }

    private companion object {
        const val DB = "migration-test"
    }
}
