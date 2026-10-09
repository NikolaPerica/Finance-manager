package com.example.financemanager.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.data.GoalContribution
import com.example.financemanager.data.SavingsGoal
import com.example.financemanager.ui.amountInputText
import com.example.financemanager.ui.isValidAmountInput
import com.example.financemanager.ui.parseAmount
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class GoalFormState(
    val name: String = "",
    val target: String = "",
    val deadline: LocalDate? = null,
    /** Palette slot; null until the user picks one (a new goal then gets one from its id). */
    val color: Int? = null,
    /** Money already put aside, only asked for a new goal. */
    val alreadySaved: String = "",
    val nameError: Boolean = false,
    val targetError: Boolean = false,
    val nameShake: Int = 0,
    val targetShake: Int = 0,
    val isLoaded: Boolean = true,
    val isSaving: Boolean = false,
)

/** Form for a new goal ([goalId] null) or for editing one. */
class GoalFormViewModel(
    private val goalId: Long?,
    private val repository: FinanceRepository,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    val isEditing: Boolean get() = goalId != null
    private var existing: SavingsGoal? = null

    private val _state = MutableStateFlow(GoalFormState(isLoaded = goalId == null))
    val state: StateFlow<GoalFormState> = _state.asStateFlow()

    /** Fires with the goal's id once it is saved. */
    private val _saved = Channel<Long>(Channel.BUFFERED)
    val saved = _saved.receiveAsFlow()

    init {
        if (goalId != null) {
            viewModelScope.launch {
                val goal = repository.goal(goalId)
                existing = goal
                _state.update { current ->
                    goal?.let {
                        current.copy(
                            name = it.name,
                            target = amountInputText(it.target),
                            deadline = it.deadline,
                            color = it.colorIndex,
                            isLoaded = true,
                        )
                    } ?: current.copy(isLoaded = true)
                }
            }
        }
    }

    fun onNameChange(name: String) = _state.update { it.copy(name = name, nameError = false) }

    fun onTargetChange(text: String) {
        if (isValidAmountInput(text)) _state.update { it.copy(target = text, targetError = false) }
    }

    fun onDeadlineChange(date: LocalDate?) = _state.update { it.copy(deadline = date) }

    fun onColorChange(color: Int) = _state.update { it.copy(color = color) }

    fun onAlreadySavedChange(text: String) {
        if (isValidAmountInput(text)) _state.update { it.copy(alreadySaved = text) }
    }

    fun save() {
        val current = _state.value
        if (current.isSaving || !current.isLoaded) return
        val target = parseAmount(current.target)
        val nameMissing = current.name.isBlank()
        if (target == null || nameMissing) {
            _state.update {
                it.copy(
                    nameError = nameMissing,
                    targetError = target == null,
                    nameShake = if (nameMissing) it.nameShake + 1 else it.nameShake,
                    targetShake = if (target == null) it.targetShake + 1 else it.targetShake,
                )
            }
            return
        }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val goal = (existing ?: SavingsGoal(name = "", target = 0.0)).copy(
                name = current.name.trim(),
                target = target,
                deadline = current.deadline,
                color = current.color,
            )
            val id = repository.saveGoal(goal)
            val start = parseAmount(current.alreadySaved)
            if (existing == null && start != null) {
                repository.addContribution(GoalContribution(goalId = id, amount = start, date = today()))
            }
            _saved.send(id)
        }
    }
}
