package com.example.game24

import com.example.game24.model.Frac
import org.junit.Assert.*
import org.junit.Test

class FracTest {

    @Test
    fun `norm reduces fraction`() {
        assertEquals(Frac(1L, 2L), Frac(2, 4).norm())
        assertEquals(Frac(3L, 1L), Frac(6, 2).norm())
    }

    @Test
    fun `norm handles negative denominator`() {
        val f = Frac(3L, -4L).norm()
        assertEquals(Frac(-3L, 4L), f)
    }

    @Test
    fun `norm handles zero numerator`() {
        val f = Frac(0, 5).norm()
        assertEquals(0L, f.n)
        assertTrue(f.d > 0)
    }

    @Test
    fun `plus adds fractions`() {
        val a = Frac(1, 2)
        val b = Frac(1, 3)
        assertEquals(Frac(5L, 6L), a + b)
    }

    @Test
    fun `minus subtracts fractions`() {
        val a = Frac(3, 4)
        val b = Frac(1, 4)
        assertEquals(Frac(1L, 2L), a - b)
    }

    @Test
    fun `times multiplies fractions`() {
        val a = Frac(2, 3)
        val b = Frac(3, 5)
        assertEquals(Frac(2L, 5L), a * b)
    }

    @Test
    fun `div divides fractions`() {
        val a = Frac(2, 3)
        val b = Frac(4, 5)
        val result = a / b
        assertNotNull(result)
        assertEquals(Frac(5L, 6L), result)
    }

    @Test
    fun `div by zero returns null`() {
        val a = Frac(2, 3)
        val b = Frac(0, 1)
        assertNull(a / b)
    }

    @Test
    fun `identity operations`() {
        val a = Frac(7, 1)
        assertEquals(Frac(14L, 1L), a + a)
        assertEquals(Frac(0L, 1L), a - a)
        assertEquals(Frac(49L, 1L), a * a)
        assertEquals(Frac(1L, 1L), a / a)
    }

    @Test
    fun `long arithmetic does not overflow for large products`() {
        val a = Frac(100000L, 1L)
        val b = Frac(100000L, 1L)
        val result = a * b
        assertEquals(10000000000L, result.n)
        assertEquals(1L, result.d)
    }

    @Test
    fun `int constructor works`() {
        val f = Frac(3, 4)
        assertEquals(3L, f.n)
        assertEquals(4L, f.d)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero denominator throws`() {
        Frac(1, 0)
    }
}
