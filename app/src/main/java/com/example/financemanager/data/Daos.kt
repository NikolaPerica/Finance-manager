package com.example.financemanager.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name COLLATE NOCASE")
    fun observeByType(type: TransactionType): Flow<List<Category>>

    @Insert
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Query("UPDATE categories SET monthlyBudget = :budget WHERE id = :id")
    suspend fun setBudget(id: Long, budget: Double?)

    @Delete
    suspend fun delete(category: Category)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun get(id: Long): Transaction?

    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    /** Moves every transaction of one category to another (or to none, with an empty name). */
    @Query("UPDATE transactions SET category = :to WHERE category = :from AND type = :type")
    suspend fun moveCategory(from: String, to: String, type: TransactionType)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Delete
    suspend fun delete(transaction: Transaction)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders")
    fun observeAll(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders")
    suspend fun getAll(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun get(id: Long): Reminder?

    @Query("UPDATE reminders SET category = :to WHERE category = :from")
    suspend fun moveCategory(from: String, to: String)

    /** Inserts a new reminder or replaces the one with the same id. */
    @Upsert
    suspend fun upsert(reminder: Reminder)

    @Delete
    suspend fun delete(reminder: Reminder)
}

@Dao
interface GoalDao {
    @Query(
        "SELECT goals.*, COALESCE(SUM(goal_contributions.amount), 0) AS saved FROM goals " +
            "LEFT JOIN goal_contributions ON goal_contributions.goalId = goals.id " +
            "GROUP BY goals.id ORDER BY goals.deadline IS NULL, goals.deadline, goals.name COLLATE NOCASE",
    )
    fun observeAll(): Flow<List<GoalWithSaved>>

    @Query("SELECT * FROM goal_contributions WHERE goalId = :goalId ORDER BY date DESC, id DESC")
    fun observeContributions(goalId: Long): Flow<List<GoalContribution>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun get(id: Long): SavingsGoal?

    @Insert
    suspend fun insert(goal: SavingsGoal): Long

    @Update
    suspend fun update(goal: SavingsGoal)

    @Delete
    suspend fun delete(goal: SavingsGoal)

    @Insert
    suspend fun insertContribution(contribution: GoalContribution): Long

    @Delete
    suspend fun deleteContribution(contribution: GoalContribution)
}
