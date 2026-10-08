package com.example.financemanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Category::class, Transaction::class, Reminder::class],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        const val NAME = "app_database"

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }
}

/**
 * Version 2 gives reminders a due date, period, category and note. Version 1 had a
 * reminders table that no screen ever wrote to, so it is recreated rather than copied.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `reminders`")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `reminders` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`amount` REAL NOT NULL, " +
                "`period` TEXT NOT NULL, " +
                "`firstDueDate` TEXT NOT NULL, " +
                "`paidCount` INTEGER NOT NULL, " +
                "`category` TEXT NOT NULL, " +
                "`note` TEXT NOT NULL)",
        )
    }
}

/** Version 3 adds an optional monthly budget to categories. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `categories` ADD COLUMN `monthlyBudget` REAL")
    }
}

/** Version 4 lets the user pick a colour for each category. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `categories` ADD COLUMN `color` INTEGER")
    }
}
