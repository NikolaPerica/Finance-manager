package com.example.financemanager

import com.example.financemanager.model.SavingsGoal
import com.example.financemanager.model.Wish
import com.example.financemanager.model.WishStatus
import com.example.financemanager.ui.components.CategoryNameError
import com.example.financemanager.ui.wishes.WishEvent
import com.example.financemanager.ui.wishes.WishlistViewModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class WishlistViewModelTest {
    private var today = LocalDate.of(2026, 11, 20)
    private val repository = FakeRepository()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun aWishNeedsANameAndAPrice() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = WishlistViewModel(repository) { today }
        assertFalse(viewModel.add(" ", "120", 2, ""))
        assertFalse(viewModel.add("Slušalice", "", 2, ""))
        assertTrue(viewModel.add(" Slušalice ", "119,99", 2, "Tehnika"))
        assertEquals(Wish(repository.wishes.value.single().id, "Slušalice", 119.99, "Tehnika", today, 2), repository.wishes.value.single())
    }

    @Test
    fun buyingWaitsForTheTimeToThinkAndCanBeUndone() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = WishlistViewModel(repository) { today }
        backgroundScope.launch { viewModel.state.collect {} }
        viewModel.add("Slušalice", "120", 2, "Tehnika")
        val wish = viewModel.state.value.list.waiting.single()

        // Too early: nothing happens.
        viewModel.buy(wish)
        assertTrue(repository.transactions.value.isEmpty())

        today = today.plusDays(2)
        viewModel.buy(wish)
        val event = viewModel.events.first() as WishEvent.Bought
        assertEquals(120.0, repository.transactions.value.single().amount, 0.0)
        assertEquals(WishStatus.BOUGHT, viewModel.state.value.list.decided.single().status)

        viewModel.undoBuy(event)
        assertTrue(repository.transactions.value.isEmpty())
        assertEquals(wish, viewModel.state.value.list.waiting.single())
    }

    @Test
    fun givingUpCanFillAGoal() = runTest(UnconfinedTestDispatcher()) {
        repository.goals.value = listOf(SavingsGoal(7, "More", 1000.0))
        val viewModel = WishlistViewModel(repository) { today }
        backgroundScope.launch { viewModel.state.collect {} }
        viewModel.add("Tenisice", "90", 3, "")
        val wish = viewModel.state.value.list.waiting.single()

        // Giving up doesn't wait.
        viewModel.skip(wish, viewModel.state.value.goals.single())
        assertEquals(WishEvent.Skipped(wish, SavingsGoal(7, "More", 1000.0)), viewModel.events.first())
        assertEquals(90.0, viewModel.state.value.list.savedBySkipping, 0.0)
        assertEquals(90.0, repository.contributions.value.single { it.goalId == 7L }.amount, 0.0)
    }

    @Test
    fun aNewCategoryCanBeMadeWhileAdding() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = WishlistViewModel(repository) { today }
        backgroundScope.launch { viewModel.state.collect {} }
        viewModel.addCategory(" Tehnika ")
        assertEquals(listOf("Tehnika"), viewModel.state.value.categories.map { it.name })
        assertEquals(CategoryNameError.EXISTS, viewModel.validateCategoryName("tehnika"))
    }
}
