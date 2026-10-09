package com.example.financemanager

import com.example.financemanager.data.GoalContribution
import com.example.financemanager.data.GoalWithSaved
import com.example.financemanager.data.SavingsGoal
import com.example.financemanager.ui.dashboard.summarize
import com.example.financemanager.ui.goals.ContributionError
import com.example.financemanager.ui.goals.GoalDetailViewModel
import com.example.financemanager.ui.goals.GoalEvent
import com.example.financemanager.ui.goals.GoalFormViewModel
import com.example.financemanager.ui.goals.GoalProgress
import com.example.financemanager.ui.goals.GoalStatus
import com.example.financemanager.ui.goals.GoalsViewModel
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class GoalsTest {

    private val today = LocalDate.of(2026, 10, 9)
    private val trip = SavingsGoal(1, "Ljetovanje", 1500.0, deadline = LocalDate.of(2027, 6, 1))
    private val laptop = SavingsGoal(2, "Laptop", 900.0)
    private val repository = FakeRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository.goals.value = listOf(trip, laptop)
        repository.contributions.value = listOf(
            GoalContribution(10, 1, 300.0, today.minusDays(5)),
            GoalContribution(11, 2, 900.0, today.minusDays(2)),
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun monthlyAmountSpreadsWhatIsLeftOverTheMonthsBeforeTheDeadline() {
        val progress = GoalProgress(trip, saved = 300.0, today = today)
        assertEquals(GoalStatus.SAVING, progress.status)
        assertEquals(1200.0, progress.remaining, 0.0)
        assertEquals(0.2, progress.fraction, 1e-9)
        // October to May: 8 months.
        assertEquals(8, progress.monthsLeft)
        assertEquals(150.0, progress.monthlyNeeded!!, 1e-9)

        // A deadline this month still leaves one month.
        assertEquals(1, GoalProgress(trip.copy(deadline = today.plusDays(3)), 0.0, today).monthsLeft)
        // No deadline: nothing to spread.
        assertNull(GoalProgress(laptop, 100.0, today).monthlyNeeded)
    }

    @Test
    fun statuses() {
        assertEquals(GoalStatus.REACHED, GoalProgress(laptop, 900.0, today).status)
        assertNull(GoalProgress(laptop, 950.0, today).monthlyNeeded)
        assertEquals(1.0, GoalProgress(laptop, 950.0, today).fraction, 0.0)
        val late = GoalProgress(trip.copy(deadline = today.minusDays(1)), 100.0, today)
        assertEquals(GoalStatus.OVERDUE, late.status)
        assertNull(late.monthlyNeeded)
    }

    @Test
    fun listPutsReachedGoalsLast() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = GoalsViewModel(repository) { today }
        backgroundScope.launch { viewModel.state.collect {} }
        val state = viewModel.state.value
        assertEquals(listOf("Ljetovanje", "Laptop"), state.goals.map { it.goal.name })
        assertEquals(1200.0, state.summary!!.saved, 0.0)
        assertEquals(2400.0, state.summary.target, 0.0)
    }

    @Test
    fun depositsWithdrawalsAndReachingTheGoal() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = GoalDetailViewModel(1, repository) { today }
        backgroundScope.launch { viewModel.state.collect {} }

        assertEquals(ContributionError.AMOUNT, viewModel.validate("0", withdraw = false))
        assertEquals(ContributionError.MORE_THAN_SAVED, viewModel.validate("300,01", withdraw = true))
        assertNull(viewModel.validate("300", withdraw = true))

        viewModel.contribute("100", withdraw = true)
        assertEquals(200.0, viewModel.state.value.progress!!.saved, 0.0)
        assertEquals(-100.0, viewModel.state.value.contributions.first().amount, 0.0)

        viewModel.contribute("1300", withdraw = false)
        assertEquals(GoalEvent.Reached, viewModel.events.first())
        assertEquals(GoalStatus.REACHED, viewModel.state.value.progress!!.status)

        val last = viewModel.state.value.contributions.first()
        viewModel.deleteContribution(last)
        assertEquals(GoalEvent.ContributionDeleted(last), viewModel.events.first())
        assertEquals(200.0, viewModel.state.value.progress!!.saved, 0.0)

        viewModel.deleteGoal()
        assertEquals(GoalEvent.GoalDeleted, viewModel.events.first())
        assertTrue(repository.contributions.value.none { it.goalId == 1L })
    }

    @Test
    fun newGoalWithAStartingAmount() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = GoalFormViewModel(null, repository) { today }
        viewModel.save()
        assertTrue(viewModel.state.value.nameError)
        assertTrue(viewModel.state.value.targetError)

        viewModel.onNameChange(" Auto ")
        viewModel.onTargetChange("8000")
        viewModel.onAlreadySavedChange("500")
        viewModel.onDeadlineChange(LocalDate.of(2028, 1, 1))
        viewModel.save()
        val id = viewModel.saved.first()

        val goal = repository.goal(id)!!
        assertEquals("Auto", goal.name)
        assertEquals(8000.0, goal.target, 0.0)
        assertEquals(500.0, repository.contributions.value.single { it.goalId == id }.amount, 0.0)
    }

    @Test
    fun editingKeepsTheHistory() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = GoalFormViewModel(1, repository) { today }
        assertEquals("1500", viewModel.state.value.target)
        viewModel.onTargetChange("2000")
        viewModel.onDeadlineChange(null)
        viewModel.save()
        viewModel.saved.first()
        assertEquals(2000.0, repository.goal(1)!!.target, 0.0)
        assertNull(repository.goal(1)!!.deadline)
        assertEquals(1, repository.contributions.value.count { it.goalId == 1L })
    }

    @Test
    fun dashboardShowsUnfinishedGoals() {
        val goals = listOf(GoalWithSaved(trip, 300.0), GoalWithSaved(laptop, 900.0))
        val state = summarize(emptyList(), goals = goals, today = today)
        assertEquals(listOf("Ljetovanje"), state.goals.map { it.goal.name })
        assertTrue(state.hasGoals)
    }
}
