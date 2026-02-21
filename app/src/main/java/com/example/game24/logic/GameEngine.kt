package com.spx.game24.logic

import com.spx.game24.model.*

fun precedence(op: Op): Int = when (op) {
    Op.MUL, Op.DIV -> 2
    Op.ADD, Op.SUB -> 1
}

fun buildTokens(nums: List<Int>, ops: List<Op>, paren: ParenMode): List<Tok> {
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

fun tokensToString(tokens: List<Tok>): String = tokens.joinToString(" ") { tok ->
    when (tok) {
        Tok.L -> "("
        Tok.R -> ")"
        is Tok.Num -> tok.v.n.toString() + "/" + tok.v.d.toString()
        is Tok.Oper -> tok.op.label
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
                    } else break
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

fun findAnySolution(nums: List<Int>, paren: ParenMode): String? {
    val ops = Op.entries
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
    for (paren in ParenMode.entries) {
        val solution = findAnySolution(nums, paren)
        if (solution != null) return solution
    }
    return null
}

private fun distinctPermutations(nums: List<Int>): List<List<Int>> {
    val out = mutableListOf<List<Int>>()
    val used = BooleanArray(nums.size)
    val current = IntArray(nums.size)

    fun dfs(depth: Int) {
        if (depth == nums.size) {
            out.add(current.toList())
            return
        }
        val seenAtDepth = HashSet<Int>()
        for (i in nums.indices) {
            if (used[i]) continue
            val value = nums[i]
            if (!seenAtDepth.add(value)) continue
            used[i] = true
            current[depth] = value
            dfs(depth + 1)
            used[i] = false
        }
    }

    dfs(0)
    return out
}

fun findAnySolutionAllParenAnyOrder(nums: List<Int>): String? {
    for (perm in distinctPermutations(nums)) {
        val solution = findAnySolutionAllParen(perm)
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

