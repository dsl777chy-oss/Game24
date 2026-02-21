package com.spx.game24

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spx.game24.domain.Op
import com.spx.game24.domain.ParenMode
import com.spx.game24.ui.theme.Game24Theme
import kotlin.math.round

private val ArcadeAccent = Color(0xFF7C4DFF)
private val ArcadeTeal = Color(0xFF26C6DA)
private val ArcadeBlue = Color(0xFF42A5F5)
private val ArcadeWarm = Color(0xFFFF7043)
private val ArcadeBgTop = Color(0xFFF7F4FF)
private val ArcadeBgBottom = Color(0xFFEEF9FF)

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

@Composable
private fun Game24App(viewModel: Game24ViewModel = viewModel()) {
    val uiState = viewModel.uiState
    val hasSolution = viewModel.hasSolution()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.actionMessage) {
        val msg = uiState.actionMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumeActionMessage()
    }

    val appBackground = Brush.verticalGradient(
        listOf(ArcadeBgTop, Color.White, ArcadeBgBottom)
    )

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shadowElevation = 4.dp
            ) {
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
                    TextButton(
                        onClick = { if (!uiState.locked) viewModel.resetRound() },
                        enabled = !uiState.locked,
                        modifier = Modifier.semantics { contentDescription = "换一题" }
                    ) {
                        Text("换一题")
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.clearBoard() },
                        enabled = !uiState.locked,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ArcadeBlue.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ArcadeBlue,
                            disabledContentColor = ArcadeBlue.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "清空输入" }
                    ) { Text("清空") }

                    OutlinedButton(
                        onClick = { viewModel.declareNoSolution() },
                        enabled = !uiState.locked,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ArcadeWarm.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ArcadeWarm,
                            disabledContentColor = ArcadeWarm.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "声明无解" }
                    ) { Text("无解") }

                    GradientActionButton(
                        onClick = { viewModel.calculate() },
                        enabled = !uiState.locked && uiState.slotsComplete && uiState.opsComplete,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "计算结果" },
                        text = "计算"
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(appBackground)
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, ArcadeAccent.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NumbersChips(
                        nums = uiState.nums,
                        slots = uiState.slots,
                        selectedPoolIndex = uiState.selectedPoolIndex,
                        locked = uiState.locked,
                        onSelect = viewModel::selectPoolNumber
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        ArcadeAccent.copy(alpha = 0.12f),
                                        ArcadeTeal.copy(alpha = 0.06f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(6.dp)
                    ) {
                        ExpressionPanel(
                            slots = uiState.slots,
                            ops = uiState.ops,
                            paren = uiState.paren,
                            locked = uiState.locked,
                            onPickOp = viewModel::pickOp,
                            onSlotClick = viewModel::clickSlot
                        )
                    }

                    ParenSlider(
                        value = uiState.paren,
                        enabled = !uiState.locked,
                        onChange = viewModel::setParen
                    )

                    val statusText = when (hasSolution) {
                        null -> if (!uiState.slotsComplete) {
                            "请先放入四个数字，再判断是否有解。"
                        } else {
                            "请选择三个运算符后再判断是否有解。"
                        }
                        true -> "当前组合有解。"
                        false -> "当前组合无解。"
                    }
                    val statusColor = when (hasSolution) {
                        false -> MaterialTheme.colorScheme.error
                        true -> ArcadeAccent
                        null -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor
                    )

                    Spacer(Modifier.height(4.dp))
                    Text(
                        "提示：运算符按钮点开后选择 + − × ÷，括号只允许一对相邻。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    when (val roundState = uiState.roundState) {
        is RoundState.Playing -> Unit
        is RoundState.Won -> ResultDialog(
            title = "成功",
            message = roundState.expr,
            buttonText = "下一题",
            onClick = { viewModel.resetRound() }
        )
        is RoundState.NoSolutionCorrect -> ResultDialog(
            title = "判定正确",
            message = "此题无解。",
            buttonText = "下一题",
            onClick = { viewModel.resetRound() }
        )
        is RoundState.LostWrongNoSolution -> ResultDialog(
            title = "判定错误",
            message = "此题其实有解：\n\n${roundState.solution}\n\n本局失败。",
            buttonText = "换一题",
            onClick = { viewModel.resetRound() }
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
            val chipBrush = Brush.verticalGradient(
                when {
                    used || locked -> listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                    selected -> listOf(ArcadeAccent.copy(alpha = 0.95f), ArcadeTeal.copy(alpha = 0.85f))
                    else -> listOf(ArcadeBlue.copy(alpha = 0.22f), MaterialTheme.colorScheme.surface)
                }
            )
            val contentColor = when {
                used -> MaterialTheme.colorScheme.onSurfaceVariant
                selected -> Color.White
                else -> MaterialTheme.colorScheme.onSurface
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color.Transparent,
                shadowElevation = if (selected) 10.dp else 3.dp,
                border = BorderStroke(
                    1.dp,
                    if (selected) ArcadeAccent.copy(alpha = 0.6f) else ArcadeBlue.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .height(36.dp)
                    .alpha(if (used || locked) 0.55f else 1f)
            ) {
                Box(
                    modifier = Modifier
                        .background(chipBrush, CircleShape)
                        .padding(horizontal = 14.dp)
                        .let { base ->
                            if (used || locked) {
                                base
                            } else {
                                base
                                    .clickable { onSelect(index) }
                                    .semantics { contentDescription = "数字 $n" }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(n.toString(), fontWeight = FontWeight.SemiBold, color = contentColor)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = ArcadeBlue.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, ArcadeBlue.copy(alpha = 0.32f)),
            modifier = Modifier.widthIn(min = 88.dp)
        ) {
            Text(
                "目标 = 24",
                style = MaterialTheme.typography.titleSmall,
                color = ArcadeBlue,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
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
        val available: Dp = maxWidth - safetyMargin

        fun totalWidth(slotW: Dp, spacing: Dp): Dp =
            slotW * 7 + spacing * 6 + bracketW * 2 + bracketPadding * 2

        var spacing: Dp = maxSpacing
        var slotW: Dp = ((available - (spacing * 6 + bracketW * 2 + bracketPadding * 2)) / 7f)
            .coerceIn(minSlotW, maxSlotW)
        if (totalWidth(slotW, spacing) > available) {
            spacing = minSpacing
            slotW = ((available - (spacing * 6 + bracketW * 2 + bracketPadding * 2)) / 7f)
                .coerceIn(minSlotW, maxSlotW)
        }
        if (totalWidth(slotW, spacing) > available) slotW = minSlotW
        val slotH = (slotW * 1.15f).coerceIn(44.dp, 58.dp)

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (paren) {
                    ParenMode.AB -> {
                        BracketGroup(spacing = spacing, padding = bracketPadding) {
                            BracketSymbol(symbol = "(", width = bracketW)
                            SlotNumber(value = slots[0], width = slotW, height = slotH, onClick = { onSlotClick(0) })
                            SlotOp(ops[0], width = slotW, height = slotH, enabled = !locked, onPick = { onPickOp(0, it) })
                            SlotNumber(value = slots[1], width = slotW, height = slotH, onClick = { onSlotClick(1) })
                            BracketSymbol(symbol = ")", width = bracketW)
                        }
                        SlotOp(ops[1], width = slotW, height = slotH, enabled = !locked, onPick = { onPickOp(1, it) })
                        SlotNumber(value = slots[2], width = slotW, height = slotH, onClick = { onSlotClick(2) })
                        SlotOp(ops[2], width = slotW, height = slotH, enabled = !locked, onPick = { onPickOp(2, it) })
                        SlotNumber(value = slots[3], width = slotW, height = slotH, onClick = { onSlotClick(3) })
                    }
                    ParenMode.BC -> {
                        SlotNumber(value = slots[0], width = slotW, height = slotH, onClick = { onSlotClick(0) })
                        SlotOp(ops[0], width = slotW, height = slotH, enabled = !locked, onPick = { onPickOp(0, it) })
                        BracketGroup(spacing = spacing, padding = bracketPadding) {
                            BracketSymbol(symbol = "(", width = bracketW)
                            SlotNumber(value = slots[1], width = slotW, height = slotH, onClick = { onSlotClick(1) })
                            SlotOp(ops[1], width = slotW, height = slotH, enabled = !locked, onPick = { onPickOp(1, it) })
                            SlotNumber(value = slots[2], width = slotW, height = slotH, onClick = { onSlotClick(2) })
                            BracketSymbol(symbol = ")", width = bracketW)
                        }
                        SlotOp(ops[2], width = slotW, height = slotH, enabled = !locked, onPick = { onPickOp(2, it) })
                        SlotNumber(value = slots[3], width = slotW, height = slotH, onClick = { onSlotClick(3) })
                    }
                    ParenMode.CD -> {
                        SlotNumber(value = slots[0], width = slotW, height = slotH, onClick = { onSlotClick(0) })
                        SlotOp(ops[0], width = slotW, height = slotH, enabled = !locked, onPick = { onPickOp(0, it) })
                        SlotNumber(value = slots[1], width = slotW, height = slotH, onClick = { onSlotClick(1) })
                        SlotOp(ops[1], width = slotW, height = slotH, enabled = !locked, onPick = { onPickOp(1, it) })
                        BracketGroup(spacing = spacing, padding = bracketPadding) {
                            BracketSymbol(symbol = "(", width = bracketW)
                            SlotNumber(value = slots[2], width = slotW, height = slotH, onClick = { onSlotClick(2) })
                            SlotOp(ops[2], width = slotW, height = slotH, enabled = !locked, onPick = { onPickOp(2, it) })
                            SlotNumber(value = slots[3], width = slotW, height = slotH, onClick = { onSlotClick(3) })
                            BracketSymbol(symbol = ")", width = bracketW)
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
                color = ArcadeAccent,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .background(ArcadeAccent.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .border(1.dp, ArcadeAccent.copy(alpha = 0.24f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun BracketSymbol(symbol: String, width: Dp) {
    Box(
        modifier = Modifier.width(width),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun BracketGroup(
    spacing: Dp,
    padding: Dp,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .background(ArcadeAccent.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
            .border(1.dp, ArcadeAccent.copy(alpha = 0.32f), RoundedCornerShape(14.dp))
            .padding(horizontal = padding, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun SlotNumber(value: Int?, width: Dp, height: Dp, onClick: () -> Unit) {
    val isEmpty = value == null
    val bg = if (isEmpty) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    else ArcadeTeal.copy(alpha = 0.2f)
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .background(bg, RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (isEmpty) MaterialTheme.colorScheme.outline.copy(alpha = 0.35f) else ArcadeTeal.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .semantics { contentDescription = if (isEmpty) "空数字槽位" else "数字 ${value}" }
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isEmpty) {
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
        modifier = Modifier
            .size(width = width, height = height)
            .shadow(if (value == null) 0.dp else 8.dp, RoundedCornerShape(12.dp))
            .semantics { contentDescription = "运算符选择" },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (value == null) ArcadeBlue.copy(alpha = 0.4f) else ArcadeAccent.copy(alpha = 0.58f)
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (value == null) ArcadeBlue.copy(alpha = 0.12f) else ArcadeAccent.copy(alpha = 0.18f)
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = value?.label ?: "？",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (value == null) ArcadeBlue else ArcadeAccent,
            textAlign = TextAlign.Center
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            title = { Text("选择运算符") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Op.entries.forEach { op ->
                        Button(
                            onClick = { showDialog = false; onPick(op) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) { Text(op.label, style = MaterialTheme.typography.titleLarge) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDialog = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun ParenSlider(value: ParenMode, enabled: Boolean, onChange: (ParenMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "括号",
            style = MaterialTheme.typography.labelLarge,
            color = ArcadeAccent,
            fontWeight = FontWeight.SemiBold
        )
        Text("当前：${value.label}", style = MaterialTheme.typography.bodyMedium)

        val raw = when (value) {
            ParenMode.NONE -> 0f
            ParenMode.AB -> 1f
            ParenMode.BC -> 2f
            ParenMode.CD -> 3f
        }
        var temp by remember { mutableFloatStateOf(raw) }
        LaunchedEffect(value) {
            temp = raw
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                ArcadeAccent.copy(alpha = 0.85f),
                                ArcadeTeal.copy(alpha = 0.9f),
                                ArcadeBlue.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .alpha(if (enabled) 0.45f else 0.2f)
            )
            Slider(
                value = temp,
                onValueChange = { if (enabled) temp = it },
                valueRange = 0f..3f,
                steps = 2,
                enabled = enabled,
                onValueChangeFinished = {
                    val snapped = temp.roundToParenIndex()
                    onChange(
                        when (snapped) {
                            0 -> ParenMode.NONE
                            1 -> ParenMode.AB
                            2 -> ParenMode.BC
                            else -> ParenMode.CD
                        }
                    )
                    temp = snapped.toFloat()
                },
                colors = SliderDefaults.colors(
                    thumbColor = ArcadeAccent,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                )
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("无", style = MaterialTheme.typography.labelMedium)
            Text("(1-2)", style = MaterialTheme.typography.labelMedium)
            Text("(2-3)", style = MaterialTheme.typography.labelMedium)
            Text("(3-4)", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ResultDialog(title: String, message: String, buttonText: String, onClick: () -> Unit) {
    val isSuccess = title.contains("成功") || title.contains("正确")
    val isError = title.contains("错误")
    val tint = when {
        isSuccess -> Color(0xFF2E7D32)
        isError -> MaterialTheme.colorScheme.error
        else -> ArcadeAccent
    }
    AlertDialog(
        onDismissRequest = {},
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 10.dp,
        title = { Text(title, color = tint, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = { GradientActionButton(onClick = onClick, enabled = true, text = buttonText) }
    )
}

@Composable
private fun GradientActionButton(onClick: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier, text: String) {
    val shape = RoundedCornerShape(14.dp)
    val brush = if (enabled) {
        Brush.horizontalGradient(listOf(ArcadeAccent, ArcadeTeal))
    } else {
        Brush.horizontalGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            )
        )
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush, shape)
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun Float.roundToParenIndex(): Int {
    return round(this).toInt().coerceIn(0, 3)
}
