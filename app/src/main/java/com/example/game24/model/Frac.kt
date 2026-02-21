package com.example.game24.model

import kotlin.math.abs

data class Frac(val n: Long, val d: Long) {
    init { require(d != 0L) }

    constructor(n: Int, d: Int) : this(n.toLong(), d.toLong())

    fun norm(): Frac {
        var nn = n
        var dd = d
        if (dd < 0) { nn = -nn; dd = -dd }
        val g = gcd(abs(nn), dd)
        return Frac(nn / g, dd / g)
    }

    operator fun plus(o: Frac) = Frac(n * o.d + o.n * d, d * o.d).norm()
    operator fun minus(o: Frac) = Frac(n * o.d - o.n * d, d * o.d).norm()
    operator fun times(o: Frac) = Frac(n * o.n, d * o.d).norm()
    operator fun div(o: Frac): Frac? = if (o.n == 0L) null else Frac(n * o.d, d * o.n).norm()
}

fun gcd(a: Long, b: Long): Long {
    var x = a; var y = b
    while (y != 0L) { val t = x % y; x = y; y = t }
    return if (x == 0L) 1L else x
}
