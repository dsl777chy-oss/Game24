package com.example.game24

import com.example.game24.logic.*
import com.example.game24.model.*
import org.junit.Assert.*
import org.junit.Test

class GameEngineTest {

    @Test
    fun `evalExact simple addition 1+2+3+18=24`() {
        val result = evalExact(listOf(1, 2, 3, 18), listOf(Op.ADD, Op.ADD, Op.ADD), ParenMode.NONE)
        assertNotNull(result)
        assertEquals(Frac(24, 1), result)
    }

    @Test
    fun `evalExact multiplication 1x2x3x4=24`() {
        val result = evalExact(listOf(1, 2, 3, 4), listOf(Op.MUL, Op.MUL, Op.MUL), ParenMode.NONE)
        assertNotNull(result)
        assertEquals(Frac(24, 1), result)
    }

    @Test
    fun `evalExact respects operator precedence`() {
        // 2 + 3 × 4 + 8 = 2 + 12 + 8 = 22, not 24
        val result = evalExact(listOf(2, 3, 4, 8), listOf(Op.ADD, Op.MUL, Op.ADD), ParenMode.NONE)
        assertNotNull(result)
        assertEquals(Frac(22, 1), result)
    }

    @Test
    fun `evalExact with parentheses AB`() {
        // (2+3) × 4 + 4 = 5×4+4 = 24
        val result = evalExact(listOf(2, 3, 4, 4), listOf(Op.ADD, Op.MUL, Op.ADD), ParenMode.AB)
        assertNotNull(result)
        assertEquals(Frac(24, 1), result)
    }

    @Test
    fun `evalExact with parentheses BC`() {
        // 2 × (3+4) + 9 = 14 + 9 = 23
        val result = evalExact(listOf(2, 3, 4, 9), listOf(Op.MUL, Op.ADD, Op.ADD), ParenMode.BC)
        assertNotNull(result)
        assertEquals(Frac(23, 1), result)
    }

    @Test
    fun `evalExact with parentheses CD`() {
        // 1 + 2 × (3+4) = 1 + 14 = 15
        val result = evalExact(listOf(1, 2, 3, 4), listOf(Op.ADD, Op.MUL, Op.ADD), ParenMode.CD)
        assertNotNull(result)
        assertEquals(Frac(15, 1), result)
    }

    @Test
    fun `evalExact division by zero returns null`() {
        // 1 ÷ 0 is impossible since nums are 1..9, but test: 3 - 3 = 0 in subexpr
        // (1+2) ÷ (3-3) - not directly expressible with 4 nums and 3 ops in our format
        // Instead test: 1 ÷ (2 - 2) via paren BC — but we need 4 nums
        // Simpler: just directly test with Op.DIV and a zero result sub-expression
        val result = evalExact(listOf(5, 3, 3, 1), listOf(Op.DIV, Op.SUB, Op.MUL), ParenMode.BC)
        // 5 ÷ (3-3) × 1 -> div by zero
        assertNull(result)
    }

    @Test
    fun `evalExact fractional arithmetic`() {
        // 8 ÷ (3 - 8 ÷ 3) = 8 ÷ (3 - 8/3) = 8 ÷ (1/3) = 24
        val result = evalExact(listOf(8, 3, 8, 3), listOf(Op.DIV, Op.SUB, Op.DIV), ParenMode.BC)
        assertNotNull(result)
        assertEquals(Frac(24, 1), result)
    }

    @Test
    fun `findAnySolution finds solution for 1x2x3x4`() {
        val solution = findAnySolution(listOf(1, 2, 3, 4), ParenMode.NONE)
        assertNotNull(solution)
        assertTrue(solution!!.contains("24"))
    }

    @Test
    fun `findAnySolutionAllParen finds solution for 1 2 3 4`() {
        val solution = findAnySolutionAllParen(listOf(1, 2, 3, 4))
        assertNotNull(solution)
        assertTrue(solution!!.endsWith("= 24"))
    }

    @Test
    fun `findAnySolutionAllParen keeps input order`() {
        val solution = findAnySolutionAllParen(listOf(1, 2, 3, 5))
        assertNull(solution)
    }

    @Test
    fun `findAnySolutionAllParenAnyOrder finds reordered solution`() {
        val solution = findAnySolutionAllParenAnyOrder(listOf(1, 2, 3, 5))
        assertNotNull(solution)
        assertTrue(solution!!.endsWith("= 24"))
    }

    @Test
    fun `formatExpr NONE mode`() {
        val expr = formatExpr(listOf(1, 2, 3, 4), listOf(Op.MUL, Op.MUL, Op.MUL), ParenMode.NONE)
        assertEquals("1 × 2 × 3 × 4 = 24", expr)
    }

    @Test
    fun `formatExpr AB mode`() {
        val expr = formatExpr(listOf(1, 2, 3, 4), listOf(Op.ADD, Op.MUL, Op.ADD), ParenMode.AB)
        assertEquals("(1 + 2) × 3 + 4 = 24", expr)
    }

    @Test
    fun `formatExpr BC mode`() {
        val expr = formatExpr(listOf(1, 2, 3, 4), listOf(Op.ADD, Op.ADD, Op.MUL), ParenMode.BC)
        assertEquals("1 + (2 + 3) × 4 = 24", expr)
    }

    @Test
    fun `formatExpr CD mode`() {
        val expr = formatExpr(listOf(1, 2, 3, 4), listOf(Op.MUL, Op.MUL, Op.ADD), ParenMode.CD)
        assertEquals("1 × 2 × (3 + 4) = 24", expr)
    }

    @Test
    fun `random4 returns 4 distinct numbers in 1-9`() {
        repeat(100) {
            val nums = random4()
            assertEquals(4, nums.size)
            assertEquals(4, nums.toSet().size)
            assertTrue(nums.all { it in 1..9 })
        }
    }

    @Test
    fun `buildTokens NONE returns 7 tokens`() {
        val tokens = buildTokens(listOf(1, 2, 3, 4), listOf(Op.ADD, Op.SUB, Op.MUL), ParenMode.NONE)
        assertEquals(7, tokens.size)
    }

    @Test
    fun `buildTokens AB returns 9 tokens with brackets`() {
        val tokens = buildTokens(listOf(1, 2, 3, 4), listOf(Op.ADD, Op.SUB, Op.MUL), ParenMode.AB)
        assertEquals(9, tokens.size)
        assertEquals(Tok.L, tokens[0])
        assertEquals(Tok.R, tokens[4])
    }
}
