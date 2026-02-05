package com.example.game24

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.unit.Dp
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
    data class Info(val message: String) : RoundState()
}

@Composable
private fun Game24App() {
    var nums by remember { mutableStateOf(random4()) }
    var slots by remember { mutableStateOf(listOf<Int?>(null, null, null, null)) }
    var selectedPoolIndex by remember { mutableStateOf<Int?>(null) }
    var ops by remember { mutableStateOf(listOf<Op?>(null, null, null)) }
    var paren by remember { mutableStateOf(ParenMode.NONE) }
    var state by remember { mutableStateOf<RoundState>(RoundState.Playing) }
    var actionMessage by remember { mutableStateOf<String?>(null) }

    val locked = state !is RoundState.Playing
    val opsComplete = ops.all { it != null }
    val slotsComplete = slots.all { it != null }
    val hasSolution by remember(slots, ops, paren) {
        derivedStateOf {
            if (!opsComplete || !slotsComplete) {
                null
            } else {
                evalExact(slots.filterNotNull(), ops.filterNotNull(), paren) == Frac(24, 1)
            }
        }
    }

    LaunchedEffect(nums, slots, ops, paren, hasSolution) {
        if (BuildConfig.DEBUG) {
            val opsSelected = ops.filterNotNull()
            val tokens = if (opsComplete && slotsComplete) {
                buildTokens(slots.filterNotNull(), opsSelected, paren)
            } else {
                emptyList()
            }
            val tokenText = if (opsComplete && slotsComplete) tokensToString(tokens) else "incomplete"
            Log.d(
                DEBUG_TAG,
                "nums=$nums slots=$slots ops=$ops paren=$paren tokens=$tokenText hasSolution=$hasSolution"
            )
        }
    }

    fun resetRound() {
        nums = random4()
        slots = listOf(null, null, null, null)
        selectedPoolIndex = null
        ops = listOf(null, null, null)
        paren = ParenMode.NONE
        state = RoundState.Playing
        actionMessage = null
    }

    fun onClear() {
        if (locked) return
        val alreadyClear = ops.all { it == null } && paren == ParenMode.NONE && slots.all { it == null }
        if (alreadyClear) {
            actionMessage = "当前已是空白。"
        } else {
            slots = listOf(null, null, null, null)
            selectedPoolIndex = null
            ops = listOf(null, null, null)
            paren = ParenMode.NONE
            state = RoundState.Playing
            actionMessage = "已清空。"
        }
    }

    fun onCalculate() {
        if (locked) return
        if (!slotsComplete) {
            actionMessage = "请先把四个数字放入算式槽位。"
            return
        }
        if (ops.any { it == null }) {
            actionMessage = "请先选满三个运算符。"
            return
        }
        val chosen = ops.filterNotNull()
        val result = evalExact(slots.filterNotNull(), chosen, paren)
        if (result == null) {
            actionMessage = "算式包含除零，无法计算。"
            return
        }
        if (result == Frac(24, 1)) {
            state = RoundState.Won(formatExpr(slots.filterNotNull(), chosen, paren))
            actionMessage = null
        } else {
            // 按你规则：算错不算输，不提示，继续改
            actionMessage = "结果不是 24，可继续调整。"
        }
    }

    fun onNoSolution() {
        if (locked) return
        actionMessage = null
        val solution = findAnySolutionAllParen(nums)
        state = if (solution == null) RoundState.NoSolutionCorrect else RoundState.LostWrongNoSolution(solution)
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
            }
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
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
                        enabled = !locked && slotsComplete && opsComplete,
                        modifier = Modifier.weight(1f)
                    ) { Text("计算") }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // 数字池（可点选）
            NumbersChips(
                nums = nums,
                slots = slots,
                selectedPoolIndex = selectedPoolIndex,
                locked = locked,
                onSelect = { index ->
                    if (locked) return@NumbersChips
                    val usedNumbers = slots.filterNotNull().toSet()
                    val candidate = nums[index]
                    if (usedNumbers.contains(candidate)) {
                        actionMessage = "该数字已在算式中。"
                    } else {
                        selectedPoolIndex = if (selectedPoolIndex == index) null else index
                        actionMessage = null
                    }
                }
            )

            // 7 槽算式区（数字可放置 + 运算符可选 + 真括号符号）
            ExpressionPanel(
                slots = slots,
                ops = ops,
                paren = paren,
                locked = locked,
                onPickOp = { idx, op ->
                    actionMessage = null
                    ops = ops.toMutableList().also { it[idx] = op }
                },
                onSlotClick = { slotIndex ->
                    if (locked) return@ExpressionPanel
                    val current = slots[slotIndex]
                    if (current != null) {
                        slots = slots.toMutableList().also { it[slotIndex] = null }
                        actionMessage = "已移回数字池。"
                    } else if (selectedPoolIndex != null) {
                        val candidate = nums[selectedPoolIndex!!]
                        val usedNumbers = slots.filterNotNull().toSet()
                        if (usedNumbers.contains(candidate)) {
                            actionMessage = "该数字已在算式中。"
                        } else {
                            slots = slots.toMutableList().also { it[slotIndex] = candidate }
                            selectedPoolIndex = null
                            actionMessage = null
                        }
                    } else {
                        actionMessage = "请先在数字池中选择一个数字。"
                    }
                }
            )

            // 括号吸附滑条（仍保留你喜欢的“滑条吸附”）
            ParenSlider(
                value = paren,
                enabled = !locked,
                onChange = { paren = it }
            )

            val statusText = when (hasSolution) {
                null -> if (!slotsComplete) "请先放入四个数字，再判断是否有解。" else "请选择三个运算符后再判断是否有解。"
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
            actionMessage?.let { message ->
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(4.dp))
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
            message = "此题无解。",
            buttonText = "下一题",
            onClick = { resetRound() }
        )
        is RoundState.LostWrongNoSolution -> ResultDialog(
            title = "❌ 判定错误",
            message = "此题其实有解：\n\n${s.solution}\n\n本局失败。",
            buttonText = "换一题",
            onClick = { resetRound() }
        )
        is RoundState.Info -> ResultDialog(
            title = "提示",
            message = s.message,
            buttonText = "知道了",
            onClick = { state = RoundState.Playing }
        )
    }
}

@Composable
private fun NumbersChips(
    nums: List<Int>,
    slots: List<Int?>,
    selectedPoolIndex: Int?,
    locked: Boolean,
    onSelect: (Int) -> Unit
) {
    val usedNumbers = slots.filterNotNull().toSet()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("数字：", style = MaterialTheme.typography.bodyMedium)
        nums.forEachIndexed { index, n ->
            val used = usedNumbers.contains(n)
            val selected = selectedPoolIndex == index
            val bg = when {
                used -> MaterialTheme.colorScheme.surfaceVariant
                selected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
            val contentColor = when {
                used -> MaterialTheme.colorScheme.onSurfaceVariant
                selected -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                tonalElevation = if (selected) 4.dp else 2.dp,
                color = bg,
                modifier = Modifier.height(30.dp)
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .let { base ->
                            if (used || locked) {
                                base
                            } else {
                                base.clickable { onSelect(index) }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(n.toString(), fontWeight = FontWeight.SemiBold, color = contentColor)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            "目标 = 24",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 72.dp)
        )
    }
}

@Composable
private fun ExpressionPanel(
    slots: List<Int?>,
    ops: List<Op?>,
    paren: ParenMode,
    locked: Boolean,
    onPickOp: (Int, Op) -> Unit,
    onSlotClick: (Int) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val safetyMargin: Dp = 8.dp
        val minSlotW: Dp = 34.dp
        val maxSlotW: Dp = 48.dp
        val minSpacing: Dp = 2.dp
        val maxSpacing: Dp = 8.dp
        val bracketW: Dp = 9.dp
        val bracketPadding: Dp = 2.dp
        val maxW: Dp = this.maxWidth
        val available: Dp = maxW - safetyMargin

        fun totalWidth(slotW: Dp, spacing: Dp): Dp =
            slotW * 7 + spacing * 6 + bracketW * 2 + bracketPadding * 2

        var spacing: Dp = maxSpacing
        var slotW: Dp = ((available - (spacing * 6 + bracketW * 2 + bracketPadding * 2)) / 7f)
            .coerceIn(minSlotW, maxSlotW)
        var total: Dp = totalWidth(slotW, spacing)
        if (total > available) {
            spacing = minSpacing
            slotW = ((available - (spacing * 6 + bracketW * 2 + bracketPadding * 2)) / 7f)
                .coerceIn(minSlotW, maxSlotW)
            total = totalWidth(slotW, spacing)
        }
        if (total > available) {
            slotW = minSlotW
            total = totalWidth(slotW, spacing)
        }
        val slotH = (slotW * 1.15f).coerceIn(44.dp, 58.dp)

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (paren) {
                    ParenMode.AB -> {
                        BracketGroup(highlight = true, spacing = spacing, padding = bracketPadding) {
                            BracketSymbol(show = true, symbol = "(", width = bracketW)
                            SlotNumber(value = slots[0], width = slotW, height = slotH, onClick = { onSlotClick(0) })
                            SlotOp(ops[0], width = slotW, height = slotH, enabled = !locked, onPick = { onPickOp(0, it) })
                            SlotNumber(value = slots[1], width = slotW, height = slotH, onClick = { onSlotClick(1) })
                            BracketSymbol(show = true, symbol = ")", width = bracketW)
                        }
                        SlotOp(ops[1], width = slotW, height = slotH, enabled = !locked, onPick = { onPickOp(1, it) })
                        SlotNumber(value = slots[2], width = slotW, height = slotH, onClick = { onSlotClick(2) })
                        SlotOp(ops[2], width = slotW, height = slotH, enabled = !locked, onPick = { onPickOp(2, it) })
                        SlotNumber(value = slots[3], width = slotW, height = slotH, onClick = { onSlotClick(3) })
                    }
                    ParenMode.BC -> {
                        SlotNumber(value = slots[0], width = slotW, height = slotH, onClick = { onSlotClick(0) })
                        SlotOp(ops[0], width = slotW, height = slotH, enabled = !locked, onPick = { onPickOp(0, it) })
                        BracketGroup(highlight = true, spacing = spacing, padding = bracketPadding) {
                            BracketSymbol(show = true, symbol = "(", width = bracketW)
                            SlotNumber(value = slots[1], width = slotW, height = slotH, onClick = { onSlotClick(1) })
                            SlotOp(ops[1], width = slotW, height = slotH, enabled = !locked, onPick = { onPickOp(1, it) })
                            SlotNumber(value = slots[2], width = slotW, height = slotH, onClick = { onSlotClick(2) })
                            BracketSymbol(show = true, symbol = ")", width = bracketW)
                        }
                        SlotOp(ops[2], width = slotW, height = slotH, enabled = !locked, onPick = { onPickOp(2, it) })
                        SlotNumber(value = slots[3], width = slotW, height = slotH, onClick = { onSlotClick(3) })
                    }
                    ParenMode.CD -> {
                        SlotNumber(value = slots[0], width = slotW, height = slotH, onClick = { onSlotClick(0) })
                        SlotOp(ops[0], width = slotW, height = slotH, enabled = !locked, onPick = { onPickOp(0, it) })
                        SlotNumber(value = slots[1], width = slotW, height = slotH, onClick = { onSlotClick(1) })
                        SlotOp(ops[1], width = slotW, height = slotH, enabled = !locked, onPick = { onPickOp(1, it) })
                        BracketGroup(highlight = true, spacing = spacing, padding = bracketPadding) {
                            BracketSymbol(show = true, symbol = "(", width = bracketW)
                            SlotNumber(value = slots[2], width = slotW, height = slotH, onClick = { onSlotClick(2) })
                            SlotOp(ops[2], width = slotW, height = slotH, enabled = !locked, onPick = { onPickOp(2, it) })
                            SlotNumber(value = slots[3], width = slotW, height = slotH, onClick = { onSlotClick(3) })
                            BracketSymbol(show = true, symbol = ")", width = bracketW)
                        }
                    }
                    ParenMode.NONE -> {
                        SlotNumber(value = slots[0], width = slotW, height = slotH, onClick = { onSlotClick(0) })
                        SlotOp(ops[0], width = slotW, height = slotH, enabled = !locked, onPick = { onPickOp(0, it) })
                        SlotNumber(value = slots[1], width = slotW, height = slotH, onClick = { onSlotClick(1) })
                        SlotOp(ops[1], width = slotW, height = slotH, enabled = !locked, onPick = { onPickOp(1, it) })
                        SlotNumber(value = slots[2], width = slotW, height = slotH, onClick = { onSlotClick(2) })
                        SlotOp(ops[2], width = slotW, height = slotH, enabled = !locked, onPick = { onPickOp(2, it) })
                        SlotNumber(value = slots[3], width = slotW, height = slotH, onClick = { onSlotClick(3) })
                    }
                }
            }
            Text(
                "= 24",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun BracketSymbol(show: Boolean, symbol: String, width: Dp) {
    // 占位宽度固定，不会把布局挤乱；show=false 时也占位避免跳动
    Box(
        modifier = Modifier.width(width),
        contentAlignment = Alignment.Center
    ) {
        if (show) {
            Text(symbol, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun BracketGroup(
    highlight: Boolean,
    spacing: Dp,
    padding: Dp,
    content: @Composable RowScope.() -> Unit
) {
    val bg = if (highlight) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
    Row(
        modifier = Modifier
            .background(bg, RoundedCornerShape(14.dp))
            .padding(horizontal = padding, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun SlotNumber(value: Int?, width: Dp, height: Dp, onClick: () -> Unit) {
    val bg = if (value == null) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .background(bg, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (value == null) {
            Text("空", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(value.toString(), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SlotOp(
    value: Op?,
    width: Dp,
    height: Dp,
    enabled: Boolean,
    onPick: (Op) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { if (enabled) showDialog = true },
        enabled = enabled,
        modifier = Modifier.size(width = width, height = height),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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

private fun findAnySolutionAllParen(nums: List<Int>): String? {
    for (paren in ParenMode.values()) {
        val solution = findAnySolution(nums, paren)
        if (solution != null) return solution
    }
    return null
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
