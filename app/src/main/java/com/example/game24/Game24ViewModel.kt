package com.spx.game24

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.spx.game24.domain.Frac
import com.spx.game24.domain.Op
import com.spx.game24.domain.ParenMode
import com.spx.game24.domain.evalExact
import com.spx.game24.domain.findAnySolutionAllParen
import com.spx.game24.domain.formatExpr
import com.spx.game24.domain.random4

sealed class RoundState {
    data object Playing : RoundState()
    data class Won(val expr: String) : RoundState()
    data object NoSolutionCorrect : RoundState()
    data class LostWrongNoSolution(val solution: String) : RoundState()
}

data class Game24UiState(
    val nums: List<Int> = random4(),
    val slots: List<Int?> = listOf(null, null, null, null),
    val selectedPoolIndex: Int? = null,
    val ops: List<Op?> = listOf(null, null, null),
    val paren: ParenMode = ParenMode.NONE,
    val roundState: RoundState = RoundState.Playing,
    val actionMessage: String? = null
) {
    val locked: Boolean get() = roundState !is RoundState.Playing
    val opsComplete: Boolean get() = ops.all { it != null }
    val slotsComplete: Boolean get() = slots.all { it != null }
}

class Game24ViewModel : ViewModel() {
    var uiState by mutableStateOf(Game24UiState())
        private set

    fun hasSolution(): Boolean? {
        val state = uiState
        if (!state.slotsComplete || !state.opsComplete) return null
        return evalExact(state.slots.filterNotNull(), state.ops.filterNotNull(), state.paren) == Frac(24, 1)
    }

    fun resetRound() {
        uiState = Game24UiState(nums = random4())
    }

    fun clearBoard() {
        val state = uiState
        if (state.locked) return
        val alreadyClear = state.ops.all { it == null } &&
            state.paren == ParenMode.NONE &&
            state.slots.all { it == null }
        uiState = state.copy(
            slots = listOf(null, null, null, null),
            selectedPoolIndex = null,
            ops = listOf(null, null, null),
            paren = ParenMode.NONE,
            roundState = RoundState.Playing,
            actionMessage = if (alreadyClear) "当前已是空白。" else "已清空。"
        )
    }

    fun selectPoolNumber(index: Int) {
        val state = uiState
        if (state.locked) return
        val usedNumbers = state.slots.filterNotNull().toSet()
        val candidate = state.nums[index]
        if (usedNumbers.contains(candidate)) {
            uiState = state.copy(actionMessage = "该数字已在算式中。")
            return
        }
        val selected = if (state.selectedPoolIndex == index) null else index
        uiState = state.copy(selectedPoolIndex = selected, actionMessage = null)
    }

    fun clickSlot(slotIndex: Int) {
        val state = uiState
        if (state.locked) return
        val current = state.slots[slotIndex]
        if (current != null) {
            val next = state.slots.toMutableList().also { it[slotIndex] = null }
            uiState = state.copy(slots = next, actionMessage = "已移回数字池。")
            return
        }
        val selectedPoolIndex = state.selectedPoolIndex
        if (selectedPoolIndex == null) {
            uiState = state.copy(actionMessage = "请先在数字池中选择一个数字。")
            return
        }
        val candidate = state.nums[selectedPoolIndex]
        val usedNumbers = state.slots.filterNotNull().toSet()
        if (usedNumbers.contains(candidate)) {
            uiState = state.copy(actionMessage = "该数字已在算式中。")
            return
        }
        val next = state.slots.toMutableList().also { it[slotIndex] = candidate }
        uiState = state.copy(slots = next, selectedPoolIndex = null, actionMessage = null)
    }

    fun pickOp(index: Int, op: Op) {
        val state = uiState
        if (state.locked) return
        val next = state.ops.toMutableList().also { it[index] = op }
        uiState = state.copy(ops = next, actionMessage = null)
    }

    fun setParen(mode: ParenMode) {
        val state = uiState
        if (state.locked) return
        uiState = state.copy(paren = mode, actionMessage = null)
    }

    fun calculate() {
        val state = uiState
        if (state.locked) return
        if (!state.slotsComplete) {
            uiState = state.copy(actionMessage = "请先把四个数字放入算式槽位。")
            return
        }
        if (state.ops.any { it == null }) {
            uiState = state.copy(actionMessage = "请先选满三个运算符。")
            return
        }
        val chosen = state.ops.filterNotNull()
        val result = evalExact(state.slots.filterNotNull(), chosen, state.paren)
        if (result == null) {
            uiState = state.copy(actionMessage = "算式包含除零，无法计算。")
            return
        }
        if (result == Frac(24, 1)) {
            uiState = state.copy(
                roundState = RoundState.Won(formatExpr(state.slots.filterNotNull(), chosen, state.paren)),
                actionMessage = null
            )
            return
        }
        uiState = state.copy(actionMessage = "结果不是 24，可继续调整。")
    }

    fun declareNoSolution() {
        val state = uiState
        if (state.locked) return
        val solution = findAnySolutionAllParen(state.nums)
        uiState = if (solution == null) {
            state.copy(roundState = RoundState.NoSolutionCorrect, actionMessage = null)
        } else {
            state.copy(roundState = RoundState.LostWrongNoSolution(solution), actionMessage = null)
        }
    }

    fun consumeActionMessage() {
        val state = uiState
        if (state.actionMessage != null) {
            uiState = state.copy(actionMessage = null)
        }
    }
}
