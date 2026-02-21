package com.spx.game24.viewmodel

import androidx.lifecycle.ViewModel
import com.spx.game24.R
import com.spx.game24.logic.*
import com.spx.game24.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class GameUiState(
    val nums: List<Int> = random4(),
    val slots: List<Int?> = listOf(null, null, null, null),
    val selectedPoolIndex: Int? = null,
    val ops: List<Op?> = listOf(null, null, null),
    val paren: ParenMode = ParenMode.NONE,
    val roundState: RoundState = RoundState.Playing,
    val actionMessageRes: Int? = null
) {
    val locked: Boolean get() = roundState !is RoundState.Playing
    val opsComplete: Boolean get() = ops.all { it != null }
    val slotsComplete: Boolean get() = slots.all { it != null }

    val hasSolution: Boolean?
        get() = if (!opsComplete || !slotsComplete) null
        else evalExact(slots.filterNotNull(), ops.filterNotNull(), paren) == Frac(24, 1)
}

class GameViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    fun resetRound() {
        _uiState.value = GameUiState()
    }

    fun onClear() {
        val s = _uiState.value
        if (s.locked) return
        val alreadyClear = s.ops.all { it == null } && s.paren == ParenMode.NONE && s.slots.all { it == null }
        if (alreadyClear) {
            _uiState.update { it.copy(actionMessageRes = R.string.msg_already_clear) }
        } else {
            _uiState.update {
                it.copy(
                    slots = listOf(null, null, null, null),
                    selectedPoolIndex = null,
                    ops = listOf(null, null, null),
                    paren = ParenMode.NONE,
                    roundState = RoundState.Playing,
                    actionMessageRes = R.string.msg_cleared
                )
            }
        }
    }

    fun onCalculate() {
        val s = _uiState.value
        if (s.locked) return
        if (!s.slotsComplete) {
            _uiState.update { it.copy(actionMessageRes = R.string.msg_fill_slots_first) }
            return
        }
        if (s.ops.any { it == null }) {
            _uiState.update { it.copy(actionMessageRes = R.string.msg_fill_ops_first) }
            return
        }
        val chosen = s.ops.filterNotNull()
        val result = evalExact(s.slots.filterNotNull(), chosen, s.paren)
        if (result == null) {
            _uiState.update { it.copy(actionMessageRes = R.string.msg_div_by_zero) }
            return
        }
        if (result == Frac(24, 1)) {
            _uiState.update {
                it.copy(
                    roundState = RoundState.Won(formatExpr(s.slots.filterNotNull(), chosen, s.paren)),
                    actionMessageRes = null
                )
            }
        } else {
            _uiState.update { it.copy(actionMessageRes = R.string.msg_not_24) }
        }
    }

    fun onNoSolution() {
        val s = _uiState.value
        if (s.locked) return
        val solution = findAnySolutionAllParenAnyOrder(s.nums)
        _uiState.update {
            it.copy(
                actionMessageRes = null,
                roundState = if (solution == null) RoundState.NoSolutionCorrect
                else RoundState.LostWrongNoSolution(solution)
            )
        }
    }

    fun onSelectPoolNumber(index: Int) {
        val s = _uiState.value
        if (s.locked) return
        val usedNumbers = s.slots.filterNotNull().toSet()
        val candidate = s.nums[index]
        if (usedNumbers.contains(candidate)) {
            _uiState.update { it.copy(actionMessageRes = R.string.msg_number_already_used) }
        } else {
            _uiState.update {
                it.copy(
                    selectedPoolIndex = if (s.selectedPoolIndex == index) null else index,
                    actionMessageRes = null
                )
            }
        }
    }

    fun onPickOp(idx: Int, op: Op) {
        val s = _uiState.value
        if (s.locked) return
        _uiState.update {
            it.copy(
                ops = it.ops.mapIndexed { i, old -> if (i == idx) op else old },
                actionMessageRes = null
            )
        }
    }

    fun onSlotClick(slotIndex: Int) {
        val s = _uiState.value
        if (s.locked) return
        val current = s.slots[slotIndex]
        if (current != null) {
            _uiState.update {
                it.copy(
                    slots = it.slots.mapIndexed { i, old -> if (i == slotIndex) null else old },
                    actionMessageRes = R.string.msg_returned_to_pool
                )
            }
        } else if (s.selectedPoolIndex != null) {
            val candidate = s.nums[s.selectedPoolIndex]
            val usedNumbers = s.slots.filterNotNull().toSet()
            if (usedNumbers.contains(candidate)) {
                _uiState.update { it.copy(actionMessageRes = R.string.msg_number_already_used) }
            } else {
                _uiState.update {
                    it.copy(
                        slots = it.slots.mapIndexed { i, old -> if (i == slotIndex) candidate else old },
                        selectedPoolIndex = null,
                        actionMessageRes = null
                    )
                }
            }
        } else {
            _uiState.update { it.copy(actionMessageRes = R.string.msg_select_number_first) }
        }
    }

    fun onParenChange(newParen: ParenMode) {
        val s = _uiState.value
        if (s.locked) return
        _uiState.update { it.copy(paren = newParen) }
    }

    fun dismissInfo() {
        _uiState.update { it.copy(roundState = RoundState.Playing) }
    }
}

