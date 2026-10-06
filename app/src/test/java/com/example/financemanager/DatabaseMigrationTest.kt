package com.example.financemanager

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.financemanager.data.AppDatabase
import com.example.financemanager.data.MIGRATION_1_2
import com.example.financemanager.data.PaymentPeriod
import com.example.financemanager.data.Reminder
import com.example.financemanager.data.RoomFinanceRepository
import com.example.financemanager.data.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONObject
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    /** Creates the database exactly as version 1 of the app left it, from the exported schema. */
    private fun createVersion1(fill: (SQLiteDatabase) -> Unit) {
        val schema = JSONObject(File("schemas/${AppDatabase::class.java.name}/1.json").readText()).getJSONObject("database")
        val file = context.getDatabasePath(DB).apply { parentFile!!.mkdirs(); delete() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            db.version = 1
            fill(db)
        }
    }

    @Test
    fun version1DataSurvivesTheUpgrade() = runBlocking {
        createVersion1 { db ->
            db.execSQL("INSERT INTO categories (id, name, type) VALUES (1, 'Hrana', 'EXPENSE')")
            db.execSQL(
                "INSERT INTO transactions (id, amount, date, note, category, type) " +
                    "VALUES (1, 64.3, '2026-10-05', 'Konzum', 'Hrana', 'EXPENSE')",
            )
        }

        // Room checks the migrated tables against the version 2 schema when it opens.
        val db = Room.databaseBuilder(context, AppDatabase::class.java, DB)
            .addMigrations(MIGRATION_1_2)
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
