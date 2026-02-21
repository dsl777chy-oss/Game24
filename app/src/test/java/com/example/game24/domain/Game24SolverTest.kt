package com.spx.game24.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class Game24SolverTest {

    @Test
    fun `evalExact respects operator precedence`() {
        val result = evalExact(
            nums = listOf(1, 2, 3, 4),
            ops = listOf(Op.ADD, Op.MUL, Op.ADD),
            paren = ParenMode.NONE
        )
        assertEquals(Frac(11, 1), result)
    }

    @Test
    fun `evalExact applies adjacent parentheses`() {
        val result = evalExact(
            nums = listOf(1, 2, 3, 4),
            ops = listOf(Op.ADD, Op.MUL, Op.ADD),
            paren = ParenMode.AB
        )
        assertEquals(Frac(13, 1), result)
    }

    @Test
    fun `evalExact returns null on divide by zero`() {
        val result = evalExact(
            nums = listOf(1, 2, 2, 3),
            ops = listOf(Op.DIV, Op.SUB, Op.ADD),
            paren = ParenMode.BC
        )
        assertNull(result)
    }

    @Test
    fun `findAnySolutionAllParen finds a valid expression`() {
        val solution = findAnySolutionAllParen(listOf(8, 2, 3, 6))
        assertNotNull(solution)
        assertTrue(solution!!.endsWith("= 24"))
    }

    @Test
    fun `findAnySolutionAllParen returns null for unsolved case`() {
        val solution = findAnySolutionAllParen(listOf(1, 1, 1, 1))
        assertNull(solution)
    }

    @Test
    fun `solver existence matches brute force regression`() {
        val cases = sequence {
            for (a in 1..9) {
                for (b in 1..9) {
                    if (b == a) continue
                    for (c in 1..9) {
                        if (c == a || c == b) continue
                        for (d in 1..9) {
                            if (d == a || d == b || d == c) continue
                            yield(listOf(a, b, c, d))
                        }
                    }
                }
            }
        }.take(120)

        for (nums in cases) {
            val solverHas = findAnySolutionAllParen(nums) != null
            val bruteHas = bruteForceHasSolution(nums)
            assertEquals("mismatch for nums=$nums", bruteHas, solverHas)
        }
    }

    private fun bruteForceHasSolution(nums: List<Int>): Boolean {
        for (o1 in Op.values()) {
            for (o2 in Op.values()) {
                for (o3 in Op.values()) {
                    val ops = listOf(o1, o2, o3)
                    for (paren in ParenMode.values()) {
                        val result = evalByCases(nums.map { it.toDouble() }, ops, paren)
                        if (result != null && abs(result - 24.0) < 1e-9) {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }

    private fun evalByCases(nums: List<Double>, ops: List<Op>, paren: ParenMode): Double? {
        fun apply(x: Double, op: Op, y: Double): Double? = when (op) {
            Op.ADD -> x + y
            Op.SUB -> x - y
            Op.MUL -> x * y
            Op.DIV -> if (abs(y) < 1e-12) null else x / y
        }

        fun evalNoParen(ns: List<Double>, os: List<Op>): Double? {
            val numStack = ArrayDeque<Double>()
            val opStack = ArrayDeque<Op>()

            fun prec(op: Op): Int = when (op) {
                Op.MUL, Op.DIV -> 2
                Op.ADD, Op.SUB -> 1
            }

            fun popApply(): Boolean {
                val op = opStack.removeLastOrNull() ?: return false
                val right = numStack.removeLastOrNull() ?: return false
                val left = numStack.removeLastOrNull() ?: return false
                val res = apply(left, op, right) ?: return false
                numStack.addLast(res)
                return true
            }

            numStack.addLast(ns[0])
            for (i in os.indices) {
                val op = os[i]
                val next = ns[i + 1]
                while (true) {
                    val top = opStack.lastOrNull()
                    if (top != null && prec(top) >= prec(op)) {
                        if (!popApply()) return null
                    } else {
                        break
                    }
                }
                opStack.addLast(op)
                numStack.addLast(next)
            }
            while (opStack.isNotEmpty()) {
                if (!popApply()) return null
            }
            return numStack.lastOrNull()
        }

        val (a, b, c, d) = nums
        val (o1, o2, o3) = ops
        return when (paren) {
            ParenMode.NONE -> evalNoParen(listOf(a, b, c, d), listOf(o1, o2, o3))
            ParenMode.AB -> {
                val ab = apply(a, o1, b) ?: return null
                evalNoParen(listOf(ab, c, d), listOf(o2, o3))
            }
            ParenMode.BC -> {
                val bc = apply(b, o2, c) ?: return null
                evalNoParen(listOf(a, bc, d), listOf(o1, o3))
            }
            ParenMode.CD -> {
                val cd = apply(c, o3, d) ?: return null
                evalNoParen(listOf(a, b, cd), listOf(o1, o2))
            }
        }
    }
}
