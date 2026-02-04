package com.example.game24

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.game24.ui.theme.Game24Theme
import kotlin.math.abs
import kotlin.math.round

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Game24Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Game24App()
                }
            }
        }
    }
}

/** =======================
 *  规则（按你最终确认）
 *  - 每题 4 个数字：1..9 不重复（顺序固定）
 *  - 运算符：+ - × ÷（必须选满 3 个）
 *  - 括号：最多一对，只能括相邻：(1-2)/(2-3)/(3-4) 或无括号
 *  - 无提示；无解按钮：点了即结束
 *      - 若其实有解（按你的括号规则）=> 失败并展示一个“符合规则”的解
 *      - 若确实无解 => 判定正确
 * ======================= */

private enum class ParenMode(val label: String) { NONE("无"), AB("(1-2)"), BC("(2-3)"), CD("(3-4)") }
private enum class Op(val label: String) { ADD("+"), SUB("−"), MUL("×"), DIV("÷") }

private const val DEBUG_TAG = "Game24"

private sealed class Tok {
    data class Num(val v: Frac) : Tok()
    data class Oper(val op: Op) : Tok()
    object L : Tok()
    object R : Tok()
}

private sealed class RoundState {
    data object Playing : RoundState()
    data class Won(val expr: String) : RoundState()
    data object NoSolutionCorrect : RoundState()
    data class LostWrongNoSolution(val solution: String) : RoundState()
}

@Composable
private fun Game24App() {
    var nums by remember { mutableStateOf(random4()) }
    var ops by remember { mutableStateOf(listOf<Op?>(null, null, null)) }
    var paren by remember { mutableStateOf(ParenMode.NONE) }
    var state by remember { mutableStateOf<RoundState>(RoundState.Playing) }

    val locked = state !is RoundState.Playing
    val opsComplete = ops.all { it != null }
    val hasSolution by remember(nums, ops, paren) {
        derivedStateOf {
            if (!opsComplete) {
                null
            } else {
                evalExact(nums, ops.filterNotNull(), paren) == Frac(24, 1)
            }
        }
    }

    LaunchedEffect(nums, ops, paren, hasSolution) {
        if (BuildConfig.DEBUG) {
            val opsSelected = ops.filterNotNull()
            val tokens = if (opsComplete) buildTokens(nums, opsSelected, paren) else emptyList()
            val tokenText = if (opsComplete) tokensToString(tokens) else "incomplete"
            Log.d(
                DEBUG_TAG,
                "nums=$nums ops=$ops paren=$paren tokens=$tokenText hasSolution=$hasSolution"
            )
        }
    }

    fun resetRound() {
        nums = random4()
        ops = listOf(null, null, null)
        paren = ParenMode.NONE
        state = RoundState.Playing
    }

    fun onClear() {
        if (locked) return
        ops = listOf(null, null, null)
        paren = ParenMode.NONE
        state = RoundState.Playing
    }

    fun onCalculate() {
        if (locked) return
        if (ops.any { it == null }) return
        val chosen = ops.filterNotNull()
        val result = evalExact(nums, chosen, paren) ?: return
        if (result == Frac(24, 1)) {
            state = RoundState.Won(formatExpr(nums, chosen, paren))
        } else {
            // 按你规则：算错不算输，不提示，继续改
        }
    }

    fun onNoSolution() {
        if (locked) return
        if (!opsComplete) return
        val chosen = ops.filterNotNull()
        val result = evalExact(nums, chosen, paren)
        state = if (result == Frac(24, 1)) {
            RoundState.LostWrongNoSolution(formatExpr(nums, chosen, paren))
        } else {
            RoundState.NoSolutionCorrect
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // 顶部：标题 + 换题（不占一大行空白）
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "24 点",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { if (!locked) resetRound() }, enabled = !locked) {
                    Text("换一题")
                }
            }

            // 数字展示（不再做 4 个大框，减少重复/占空间）
            NumbersChips(nums)

            // 7 槽算式区（数字固定 + 运算符可选 + 真括号符号）
            ExpressionPanel(
                nums = nums,
                ops = ops,
                paren = paren,
                locked = locked,
                onPickOp = { idx, op -> ops = ops.toMutableList().also { it[idx] = op } }
            )

            // 括号吸附滑条（仍保留你喜欢的“滑条吸附”）
            ParenSlider(
                value = paren,
                enabled = !locked,
                onChange = { paren = it }
            )

            // 底部按钮（不再用 Spacer(weight=1f) 撑出大空白）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onClear() },
                    enabled = !locked,
                    modifier = Modifier.weight(1f)
                ) { Text("清空") }

                OutlinedButton(
                    onClick = { onNoSolution() },
                    enabled = !locked,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) { Text("无解") }

                Button(
                    onClick = { onCalculate() },
                    enabled = !locked && opsComplete,
                    modifier = Modifier.weight(1f)
                ) { Text("计算") }
            }

            val statusText = when (hasSolution) {
                null -> "请选择三个运算符后再判断是否有解。"
                true -> "当前组合有解。"
                false -> "当前组合无解。"
            }
            val statusColor = when (hasSolution) {
                false -> MaterialTheme.colorScheme.error
                true -> MaterialTheme.colorScheme.primary
                null -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(
                statusText,
                style = MaterialTheme.typography.bodySmall,
                color = statusColor
            )

            Spacer(Modifier.height(6.dp))
            Text(
                "提示：运算符按钮点开后选择 + − × ÷，括号只允许一对相邻。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // 弹窗锁盘
    when (val s = state) {
        is RoundState.Playing -> Unit
        is RoundState.Won -> ResultDialog(
            title = "🎉 成功",
            message = s.expr,
            buttonText = "下一题",
            onClick = { resetRound() }
        )
        is RoundState.NoSolutionCorrect -> ResultDialog(
            title = "✔ 判定正确",
            message = "此题在你的括号规则下无解。",
            buttonText = "下一题",
            onClick = { resetRound() }
        )
        is RoundState.LostWrongNoSolution -> ResultDialog(
            title = "❌ 判定错误",
            message = "此题其实有解（符合你的括号规则）：\n\n${s.solution}\n\n本局失败。",
            buttonText = "换一题",
            onClick = { resetRound() }
        )
    }
}

@Composable
private fun NumbersChips(nums: List<Int>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("数字：", style = MaterialTheme.typography.bodyMedium)
        nums.forEach { n ->
            Surface(
                shape = RoundedCornerShape(999.dp),
                tonalElevation = 2.dp,
                modifier = Modifier.height(30.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(n.toString(), fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text("目标 = 24", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ExpressionPanel(
    nums: List<Int>,
    ops: List<Op?>,
    paren: ParenMode,
    locked: Boolean,
    onPickOp: (Int, Op) -> Unit
) {
    // 用 Box：左边 7 槽，右边固定 “= 24”
    Box(modifier = Modifier.fillMaxWidth()) {

        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(end = 72.dp), // 预留右侧“=24”的空间，避免挤没最后一个槽
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 真括号符号（不只是颜色）
            BracketSymbol(show = (paren == ParenMode.AB), symbol = "(")
            SlotNumber(nums[0], highlight = (paren == ParenMode.AB))
            SlotOp(0, ops[0], enabled = !locked, highlight = (paren == ParenMode.AB), onPick = { onPickOp(0, it) })
            SlotNumber(nums[1], highlight = (paren == ParenMode.AB) || (paren == ParenMode.BC))
            BracketSymbol(show = (paren == ParenMode.AB), symbol = ")")

            BracketSymbol(show = (paren == ParenMode.BC), symbol = "(")
            SlotOp(1, ops[1], enabled = !locked, highlight = (paren == ParenMode.BC), onPick = { onPickOp(1, it) })
            SlotNumber(nums[2], highlight = (paren == ParenMode.BC) || (paren == ParenMode.CD))
            BracketSymbol(show = (paren == ParenMode.BC), symbol = ")")

            BracketSymbol(show = (paren == ParenMode.CD), symbol = "(")
            SlotOp(2, ops[2], enabled = !locked, highlight = (paren == ParenMode.CD), onPick = { onPickOp(2, it) })
            SlotNumber(nums[3], highlight = (paren == ParenMode.CD))
            BracketSymbol(show = (paren == ParenMode.CD), symbol = ")")
        }

        Text(
            "= 24",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun BracketSymbol(show: Boolean, symbol: String) {
    // 占位宽度固定，不会把布局挤乱；show=false 时也占位避免跳动
    Box(
        modifier = Modifier.width(10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (show) {
            Text(symbol, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SlotNumber(n: Int, highlight: Boolean) {
    val bg = if (highlight) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 52.dp)
            .background(bg, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(n.toString(), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SlotOp(
    index: Int,
    value: Op?,
    enabled: Boolean,
    highlight: Boolean,
    onPick: (Op) -> Unit
) {
    val bg = if (highlight) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface

    var showDialog by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { if (enabled) showDialog = true },
        enabled = enabled,
        modifier = Modifier.size(width = 44.dp, height = 52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = bg),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = value?.label ?: "？",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("选择运算符") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Op.values().forEach { op ->
                        Button(
                            onClick = { showDialog = false; onPick(op) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) { Text(op.label, style = MaterialTheme.typography.titleLarge) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun ParenSlider(value: ParenMode, enabled: Boolean, onChange: (ParenMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("括号：${value.label}", style = MaterialTheme.typography.bodyMedium)

        val raw = when (value) {
            ParenMode.NONE -> 0f
            ParenMode.AB -> 1f
            ParenMode.BC -> 2f
            ParenMode.CD -> 3f
        }
        var temp by remember { mutableStateOf(raw) }
        LaunchedEffect(value) {
            temp = raw
        }

        Slider(
            value = temp,
            onValueChange = { if (enabled) temp = it },
            valueRange = 0f..3f,
            steps = 2,
            enabled = enabled,
            onValueChangeFinished = {
                val snapped = temp.roundToIntClamped(0, 3)
                onChange(
                    when (snapped) {
                        0 -> ParenMode.NONE
                        1 -> ParenMode.AB
                        2 -> ParenMode.BC
                        else -> ParenMode.CD
                    }
                )
                temp = snapped.toFloat()
            }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("无")
            Text("(1-2)")
            Text("(2-3)")
            Text("(3-4)")
        }
    }
}

@Composable
private fun ResultDialog(title: String, message: String, buttonText: String, onClick: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onClick) { Text(buttonText) }
        }
    )
}

/** =========================
 *  数学：分数（精确）
 * ========================= */
private data class Frac(val n: Int, val d: Int) {
    init { require(d != 0) }

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
    operator fun div(o: Frac): Frac? = if (o.n == 0) null else Frac(n * o.d, d * o.n).norm()
}

private fun gcd(a: Int, b: Int): Int {
    var x = a; var y = b
    while (y != 0) { val t = x % y; x = y; y = t }
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

private fun tokensToString(tokens: List<Tok>): String = tokens.joinToString(" ") { tok ->
    when (tok) {
        Tok.L -> "("
        Tok.R -> ")"
        is Tok.Num -> tok.v.n.toString() + "/" + tok.v.d.toString()
        is Tok.Oper -> tok.op.label
    }
}

/**
 * 正确求值：支持 ×÷ 优先级 + 一对括号（相邻）
 * tokens: a o1 b o2 c o3 d，括号只可能包住 (a o1 b)/(b o2 c)/(c o3 d)
 */
private fun evalExact(nums: List<Int>, ops: List<Op>, paren: ParenMode): Frac? {
    val tokens = buildTokens(nums, ops, paren)

    fun apply(x: Frac, op: Op, y: Frac): Frac? = when (op) {
        Op.ADD -> x + y
        Op.SUB -> x - y
        Op.MUL -> x * y
        Op.DIV -> x.div(y)
    }

    val vals = ArrayDeque<Frac>()
    val opsStack = ArrayDeque<Any>() // Op or Tok.L marker

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

private fun formatExpr(nums: List<Int>, ops: List<Op>, paren: ParenMode): String {
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

private fun random4(): List<Int> = (1..9).shuffled().take(4)

private fun Float.roundToIntClamped(min: Int, max: Int): Int {
    val r = round(this).toInt()
    return r.coerceIn(min, max)
}
