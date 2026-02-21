package com.spx.game24.domain

import kotlin.math.abs

enum class ParenMode(val label: String) { NONE("无"), AB("(1-2)"), BC("(2-3)"), CD("(3-4)") }
enum class Op(val label: String) { ADD("+"), SUB("−"), MUL("×"), DIV("÷") }

data class Frac(val n: Int, val d: Int) {
    init {
        require(d != 0)
    }

    fun norm(): Frac {
        var nn = n
        var dd = d
        if (dd < 0) {
            nn = -nn
            dd = -dd
        }
        val g = gcd(abs(nn), dd)
        return Frac(nn / g, dd / g)
    }

    operator fun plus(o: Frac) = Frac(n * o.d + o.n * d, d * o.d).norm()
    operator fun minus(o: Frac) = Frac(n * o.d - o.n * d, d * o.d).norm()
    operator fun times(o: Frac) = Frac(n * o.n, d * o.d).norm()
    operator fun div(o: Frac): Frac? = if (o.n == 0) null else Frac(n * o.d, d * o.n).norm()
}

private sealed class Tok {
    data class Num(val v: Frac) : Tok()
    data class Oper(val op: Op) : Tok()
    data object L : Tok()
    data object R : Tok()
}

private fun gcd(a: Int, b: Int): Int {
    var x = a
    var y = b
    while (y != 0) {
        val t = x % y
        x = y
        y = t
    }
    return if (x == 0) 1 else x
}

private fun precedence(op: Op): Int = when (op) {
    Op.MUL, Op.DIV -> 2
    Op.ADD, Op.SUB -> 1
}

private fun buildTokens(nums: List<Int>, ops: List<Op>, paren: ParenMode): List<Tok> {
    val a = Frac(nums[0], 1)
    val b = Frac(nums[1], 1)
    val c = Frac(nums[2], 1)
    val d = Frac(nums[3], 1)
    val (o1, o2, o3) = ops
    return when (paren) {
        ParenMode.NONE -> listOf(
            Tok.Num(a), Tok.Oper(o1), Tok.Num(b), Tok.Oper(o2), Tok.Num(c), Tok.Oper(o3), Tok.Num(d)
        )
        ParenMode.AB -> listOf(
            Tok.L, Tok.Num(a), Tok.Oper(o1), Tok.Num(b), Tok.R, Tok.Oper(o2), Tok.Num(c), Tok.Oper(o3), Tok.Num(d)
        )
        ParenMode.BC -> listOf(
            Tok.Num(a), Tok.Oper(o1), Tok.L, Tok.Num(b), Tok.Oper(o2), Tok.Num(c), Tok.R, Tok.Oper(o3), Tok.Num(d)
        )
        ParenMode.CD -> listOf(
            Tok.Num(a), Tok.Oper(o1), Tok.Num(b), Tok.Oper(o2), Tok.L, Tok.Num(c), Tok.Oper(o3), Tok.Num(d), Tok.R
        )
    }
}

fun evalExact(nums: List<Int>, ops: List<Op>, paren: ParenMode): Frac? {
    val tokens = buildTokens(nums, ops, paren)

    fun apply(x: Frac, op: Op, y: Frac): Frac? = when (op) {
        Op.ADD -> x + y
        Op.SUB -> x - y
        Op.MUL -> x * y
        Op.DIV -> x.div(y)
    }

    val vals = ArrayDeque<Frac>()
    val opsStack = ArrayDeque<Any>()

    fun popApply(): Boolean {
        val top = opsStack.removeLastOrNull() ?: return false
        val op = top as? Op ?: return false
        val right = vals.removeLastOrNull() ?: return false
        val left = vals.removeLastOrNull() ?: return false
        val r = apply(left, op, right) ?: return false
        vals.addLast(r)
        return true
    }

    for (t in tokens) {
        when (t) {
            is Tok.Num -> vals.addLast(t.v)
            is Tok.Oper -> {
                val cur = t.op
                while (true) {
                    val top = opsStack.lastOrNull()
                    val topOp = top as? Op
                    if (topOp != null && precedence(topOp) >= precedence(cur)) {
                        if (!popApply()) return null
                    } else {
                        break
                    }
                }
                opsStack.addLast(cur)
            }
            Tok.L -> opsStack.addLast(Tok.L)
            Tok.R -> {
                while (true) {
                    val top = opsStack.lastOrNull()
                    if (top == Tok.L) {
                        opsStack.removeLast()
                        break
                    }
                    if (top !is Op) return null
                    if (!popApply()) return null
                }
            }
        }
    }

    while (opsStack.isNotEmpty()) {
        val top = opsStack.lastOrNull()
        if (top == Tok.L) return null
        if (!popApply()) return null
    }

    return vals.lastOrNull()
}

private fun findAnySolution(nums: List<Int>, paren: ParenMode): String? {
    val ops = Op.values()
    for (o1 in ops) {
        for (o2 in ops) {
            for (o3 in ops) {
                val chosen = listOf(o1, o2, o3)
                val result = evalExact(nums, chosen, paren)
                if (result == Frac(24, 1)) {
                    return formatExpr(nums, chosen, paren)
                }
            }
        }
    }
    return null
}

fun findAnySolutionAllParen(nums: List<Int>): String? {
    for (paren in ParenMode.values()) {
        val solution = findAnySolution(nums, paren)
        if (solution != null) return solution
    }
    return null
}

fun formatExpr(nums: List<Int>, ops: List<Op>, paren: ParenMode): String {
    val a = nums[0].toString()
    val b = nums[1].toString()
    val c = nums[2].toString()
    val d = nums[3].toString()
    val o1 = ops[0].label
    val o2 = ops[1].label
    val o3 = ops[2].label

    return when (paren) {
        ParenMode.NONE -> "$a $o1 $b $o2 $c $o3 $d = 24"
        ParenMode.AB -> "($a $o1 $b) $o2 $c $o3 $d = 24"
        ParenMode.BC -> "$a $o1 ($b $o2 $c) $o3 $d = 24"
        ParenMode.CD -> "$a $o1 $b $o2 ($c $o3 $d) = 24"
    }
}

fun random4(): List<Int> = (1..9).shuffled().take(4)
