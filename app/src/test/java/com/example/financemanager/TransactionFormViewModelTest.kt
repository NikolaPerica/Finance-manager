package com.example.financemanager

import com.example.financemanager.data.Category
import com.example.financemanager.data.TransactionType
import com.example.financemanager.receipt.ReceiptData
import com.example.financemanager.ui.components.CategoryNameError
import com.example.financemanager.ui.transaction.FormEvent
import com.example.financemanager.ui.transaction.TransactionFormViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionFormViewModelTest {
    private val repository = FakeRepository()
    private lateinit var viewModel: TransactionFormViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = TransactionFormViewModel(TransactionType.EXPENSE, repository)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun emptyFormIsRejected() {
        viewModel.save()
        val state = viewModel.state.value
        assertTrue(state.amountError)
        assertTrue(state.categoryError)
        assertEquals(1, state.amountShake)
        assertTrue(repository.transactions.value.isEmpty())
    }

    @Test
    fun ignoresMalformedAmountInput() {
        viewModel.onAmountChange("12,5")
        viewModel.onAmountChange("12,555")
        assertEquals("12,5", viewModel.state.value.amount)
    }

    @Test
    fun validFormIsSaved() = runTest {
        viewModel.onAmountChange("12,50")
        viewModel.onCategoryChange("Hrana")
        viewModel.onNoteChange("  ručak ")
        viewModel.save()

        assertEquals(FormEvent.Saved(), viewModel.events.first())
        val saved = repository.transactions.value.single()
        assertEquals(12.5, saved.amount, 0.0)
        assertEquals("Hrana", saved.category)
        assertEquals("ručak", saved.note)
        assertEquals(TransactionType.EXPENSE, saved.type)
        assertFalse(viewModel.state.value.amountError)
    }

    @Test
    fun categoryNamesMustBeNewAndNonBlank() = runTest(UnconfinedTestDispatcher()) {
        repository.categories.value = listOf(Category(1, "Hrana", TransactionType.EXPENSE))
        backgroundScope.launch { viewModel.categories.collect {} }

        assertEquals(CategoryNameError.EMPTY, viewModel.validateCategoryName("  "))
        assertEquals(CategoryNameError.EXISTS, viewModel.validateCategoryName("hrana"))
        assertNull(viewModel.validateCategoryName("Stanarina"))

        viewModel.addCategory(" Stanarina ")
        assertEquals("Stanarina", viewModel.state.value.category)
        assertEquals(FormEvent.CategoryAdded("Stanarina"), viewModel.events.first())
    }

    @Test
    fun aScannedReceiptFillsWhatWasFoundAndKeepsWhatWasTyped() = runTest(UnconfinedTestDispatcher()) {
        viewModel.onNoteChange("tjedna kupnja")
        viewModel.save() // marks the empty amount and category as errors
        val receipt = ReceiptData(amount = 23.9, date = LocalDate.of(2026, 10, 21), merchant = "Konzum Plus")

        viewModel.applyReceipt(receipt, category = "Hrana")

        val state = viewModel.state.value
        assertEquals("23,9", state.amount)
        assertEquals(LocalDate.of(2026, 10, 21), state.date)
        assertEquals("tjedna kupnja", state.note)
        assertEquals("Hrana", state.category)
        assertFalse(state.amountError)
        assertFalse(state.categoryError)
        assertEquals(FormEvent.ReceiptRead(receipt), viewModel.events.first())

        // Nothing found: the form stays as it was.
        viewModel.applyReceipt(ReceiptData(null, null, null), category = null)
        assertEquals("23,9", viewModel.state.value.amount)
    }
}
