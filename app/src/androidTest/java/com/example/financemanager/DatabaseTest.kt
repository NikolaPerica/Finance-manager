package com.example.financemanager

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.financemanager.data.AppDatabase
import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private val db = Room.inMemoryDatabaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext,
        AppDatabase::class.java,
    ).build()

    @After
    fun close() = db.close()

    @Test
    fun transactionsRoundTripNewestFirst() = runBlocking {
        val dao = db.transactionDao()
        dao.insert(Transaction(amount = 10.0, date = LocalDate.of(2026, 1, 1), note = "", category = "A", type = TransactionType.INCOME))
        dao.insert(Transaction(amount = 5.0, date = LocalDate.of(2026, 2, 1), note = "", category = "B", type = TransactionType.EXPENSE))

        val all = dao.observeAll().first()
        assertEquals(listOf("B", "A"), all.map { it.category })
        assertEquals(LocalDate.of(2026, 2, 1), all.first().date)
    }
}
