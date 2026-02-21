package com.spx.game24.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spx.game24.R
import com.spx.game24.model.Op
import com.spx.game24.model.ParenMode
import com.spx.game24.ui.theme.LocalGameColors

@Composable
fun NumbersChips(
    nums: List<Int>,
    slots: List<Int?>,
    selectedPoolIndex: Int?,
    locked: Boolean,
    onSelect: (Int) -> Unit
) {
    val gameColors = LocalGameColors.current
    val usedNumbers = slots.filterNotNull().toSet()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.label_numbers), style = MaterialTheme.typography.bodyMedium)
        nums.forEachIndexed { index, n ->
            val used = usedNumbers.contains(n)
            val selected = selectedPoolIndex == index
            val chipBrush = Brush.verticalGradient(
                when {
                    used || locked -> listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                    selected -> listOf(gameColors.accent.copy(alpha = 0.95f), gameColors.teal.copy(alpha = 0.85f))
                    else -> listOf(gameColors.blue.copy(alpha = 0.22f), MaterialTheme.colorScheme.surface)
                }
            )
            val contentColor = when {
                used -> MaterialTheme.colorScheme.onSurfaceVariant
                selected -> Color.White
                else -> MaterialTheme.colorScheme.onSurface
            }
            val desc = if (selected) stringResource(R.string.a11y_number_selected, n)
            else if (used) stringResource(R.string.a11y_number_used, n)
            else stringResource(R.string.a11y_number_available, n)
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color.Transparent,
                shadowElevation = if (selected) 10.dp else 3.dp,
                border = BorderStroke(
                    1.dp,
                    if (selected) gameColors.accent.copy(alpha = 0.6f) else gameColors.blue.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .height(36.dp)
                    .alpha(if (used || locked) 0.55f else 1f)
                    .semantics { contentDescription = desc }
            ) {
                Box(
                    modifier = Modifier
                        .background(chipBrush, CircleShape)
                        .padding(horizontal = 14.dp)
                        .let { base ->
                            if (used || locked) base
                            else base.clickable { onSelect(index) }
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
            color = gameColors.blue.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, gameColors.blue.copy(alpha = 0.32f)),
            modifier = Modifier.widthIn(min = 88.dp)
        ) {
            Text(
                stringResource(R.string.target_24),
                style = MaterialTheme.typography.titleSmall,
                color = gameColors.blue,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun ExpressionPanel(
    slots: List<Int?>,
    ops: List<Op?>,
    paren: ParenMode,
    locked: Boolean,
    onPickOp: (Int, Op) -> Unit,
    onSlotClick: (Int) -> Unit
) {
    val gameColors = LocalGameColors.current
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
        if (totalWidth(slotW, spacing) > available) {
            slotW = minSlotW
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
                color = gameColors.accent,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .background(gameColors.accent.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .border(1.dp, gameColors.accent.copy(alpha = 0.24f), RoundedCornerShape(12.dp))
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
    val gameColors = LocalGameColors.current
    Row(
        modifier = Modifier
            .background(gameColors.accent.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
            .border(1.dp, gameColors.accent.copy(alpha = 0.32f), RoundedCornerShape(14.dp))
            .padding(horizontal = padding, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun SlotNumber(value: Int?, width: Dp, height: Dp, onClick: () -> Unit) {
    val gameColors = LocalGameColors.current
    val isEmpty = value == null
    val bg = if (isEmpty) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    else gameColors.teal.copy(alpha = 0.2f)
    val desc = if (isEmpty) stringResource(R.string.a11y_slot_empty)
    else stringResource(R.string.a11y_slot_number, value!!)
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .background(bg, RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (isEmpty) MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                else gameColors.teal.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(2.dp)
            .semantics { contentDescription = desc },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                fadeIn(tween(200)) + scaleIn(tween(200)) togetherWith
                        fadeOut(tween(150)) + scaleOut(tween(150))
            },
            label = "slot_anim"
        ) { v ->
            if (v == null) {
                Text(
                    stringResource(R.string.slot_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(v.toString(), fontWeight = FontWeight.SemiBold)
            }
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
    val gameColors = LocalGameColors.current
    var showDialog by remember { mutableStateOf(false) }
    val desc = if (value != null) stringResource(R.string.a11y_operator, value.label)
    else stringResource(R.string.a11y_operator_empty)

    OutlinedButton(
        onClick = { if (enabled) showDialog = true },
        enabled = enabled,
        modifier = Modifier
            .size(width = width, height = height)
            .shadow(if (value == null) 0.dp else 8.dp, RoundedCornerShape(12.dp))
            .semantics { contentDescription = desc },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (value == null) gameColors.blue.copy(alpha = 0.4f) else gameColors.accent.copy(alpha = 0.58f)
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (value == null) gameColors.blue.copy(alpha = 0.12f)
            else gameColors.accent.copy(alpha = 0.18f)
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = value?.label ?: "？",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (value == null) gameColors.blue else gameColors.accent,
            textAlign = TextAlign.Center
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            title = { Text(stringResource(R.string.dialog_select_operator)) },
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
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParenSelector(value: ParenMode, enabled: Boolean, onChange: (ParenMode) -> Unit) {
    val gameColors = LocalGameColors.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(R.string.label_parenthesis),
            style = MaterialTheme.typography.labelLarge,
            color = gameColors.accent,
            fontWeight = FontWeight.SemiBold
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ParenMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = value == mode,
                    onClick = { if (enabled) onChange(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = ParenMode.entries.size),
                    enabled = enabled,
                    modifier = Modifier.semantics {
                        contentDescription = mode.label
                    }
                ) {
                    Text(mode.label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun ResultDialog(title: String, message: String, buttonText: String, onClick: () -> Unit) {
    val gameColors = LocalGameColors.current
    val isSuccess = title.contains("成功") || title.contains("正确")
    val isError = title.contains("错误")
    val tint = when {
        isSuccess -> gameColors.successGreen
        isError -> MaterialTheme.colorScheme.error
        else -> gameColors.accent
    }
    AlertDialog(
        onDismissRequest = {},
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 10.dp,
        title = { Text(title, color = tint, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            GradientActionButton(onClick = onClick, enabled = true, text = buttonText)
        }
    )
}

@Composable
fun GradientActionButton(onClick: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier, text: String) {
    val gameColors = LocalGameColors.current
    val shape = RoundedCornerShape(14.dp)
    val brush = if (enabled) {
        Brush.horizontalGradient(listOf(gameColors.accent, gameColors.teal))
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


