package com.ninersudoku.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import com.ninersudoku.game.Board
import com.ninersudoku.game.KillerCage
import com.ninersudoku.prefs.DisplayPreferences
import com.ninersudoku.viewmodel.CompletedFlash

@Composable
fun BoardView(
    board: Board,
    selected: Int?,
    highlightDigit: Int?,
    conflicts: Set<Int>,
    completedFlash: CompletedFlash?,
    cages: List<KillerCage> = emptyList(),
    onCellTap: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val measurer = rememberTextMeasurer()
    val selectedValue = selected?.let { board.cells[it].value }.takeIf { it != null && it != 0 }
    // Either filter mode (no selection, user tapped a digit on the pad) or same-number-as-selection.
    val sameValueDigit: Int? = highlightDigit ?: selectedValue

    val selRow = selected?.let { it / 9 }
    val selCol = selected?.let { it % 9 }
    val selBox = if (selRow != null && selCol != null) (selRow / 3) * 3 + (selCol / 3) else null

    val cs = MaterialTheme.colorScheme
    val peerHighlightOn by DisplayPreferences.peerHighlight.collectAsState()
    val sameNumberHighlightOn by DisplayPreferences.sameNumberHighlight.collectAsState()
    val largeText by DisplayPreferences.largeText.collectAsState()
    val centeredNotes by DisplayPreferences.centeredNotes.collectAsState()
    val colorBlind by DisplayPreferences.colorBlind.collectAsState()

    val boardBg = cs.surface
    val gridThick = cs.onSurface.copy(alpha = 0.85f)
    val gridThin = cs.outline.copy(alpha = 0.5f)
    val selectedFill = cs.primary.copy(alpha = 0.30f)
    val peerFill = cs.primary.copy(alpha = 0.07f)
    val sameFill = cs.primary.copy(alpha = 0.18f)
    val hintCellFill = cs.tertiary.copy(alpha = 0.30f)
    val flashColor = cs.tertiary
    val givenColor = cs.onSurface
    val userColor = cs.primary
    val hintColor = if (colorBlind) Color(0xFFFFC107) else cs.tertiary  // amber/yellow for CB mode
    val errorColor = if (colorBlind) Color(0xFFFF6F00) else cs.error  // dark orange for CB mode
    val noteColor = cs.onSurfaceVariant
    val cellValueFontSize = if (largeText) 28.sp else 22.sp
    val noteFontSize = if (largeText) 11.sp else 9.sp
    // Killer cage colors — a distinct accent so cages don't blend into the sudoku grid lines.
    val cageBorder = cs.primary.copy(alpha = 0.55f)
    val cageTint = cs.primary.copy(alpha = 0.04f)
    val cageLabelColor = cs.primary

    // Animate the brief flash for newly-completed rows/cols/boxes.
    val flashAlpha = remember { Animatable(0f) }
    LaunchedEffect(completedFlash?.tick) {
        val flash = completedFlash ?: return@LaunchedEffect
        if (flash.tick == 0L) return@LaunchedEffect
        if (flash.rows.isEmpty() && flash.cols.isEmpty() && flash.boxes.isEmpty()) return@LaunchedEffect
        flashAlpha.snapTo(0f)
        flashAlpha.animateTo(0.65f, tween(120))
        flashAlpha.animateTo(0f, tween(550))
    }

    val cageOfCellForA11y = remember(cages) {
        IntArray(81) { -1 }.also { arr ->
            for ((ci, cage) in cages.withIndex()) for (cell in cage.cells) arr[cell] = ci
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val cellSize = size.width / 9f
                        val col = (offset.x / cellSize).toInt().coerceIn(0, 8)
                        val row = (offset.y / cellSize).toInt().coerceIn(0, 8)
                        onCellTap(row, col)
                    }
                }
        ) {
        val cellSize = size.width / 9f
        drawRect(boardBg, size = Size(size.width, size.height))

        for (i in 0 until 81) {
            val r = i / 9; val c = i % 9
            val box = (r / 3) * 3 + (c / 3)
            val v = board.cells[i].value
            val cell = board.cells[i]
            val highlight = when {
                i == selected -> selectedFill
                peerHighlightOn && selected != null && (r == selRow || c == selCol || box == selBox) -> peerFill
                sameNumberHighlightOn && sameValueDigit != null && v == sameValueDigit -> sameFill
                cell.isHint -> hintCellFill
                else -> null
            }
            if (highlight != null) {
                drawRect(
                    color = highlight,
                    topLeft = Offset(c * cellSize, r * cellSize),
                    size = Size(cellSize, cellSize)
                )
            }
        }

        // Completion flash overlay (drawn under the grid lines so the lines stay visible).
        if (flashAlpha.value > 0f && completedFlash != null) {
            val tinted = flashColor.copy(alpha = flashAlpha.value)
            for (r in completedFlash.rows) {
                drawRect(tinted, topLeft = Offset(0f, r * cellSize), size = Size(size.width, cellSize))
            }
            for (c in completedFlash.cols) {
                drawRect(tinted, topLeft = Offset(c * cellSize, 0f), size = Size(cellSize, size.height))
            }
            for (b in completedFlash.boxes) {
                val br = (b / 3) * 3; val bc = (b % 3) * 3
                drawRect(
                    tinted,
                    topLeft = Offset(bc * cellSize, br * cellSize),
                    size = Size(cellSize * 3, cellSize * 3)
                )
            }
        }

        val thinPx = 1.dp.toPx()
        for (i in 0..9) {
            val pos = i * cellSize
            drawLine(gridThin, Offset(pos, 0f), Offset(pos, size.height), thinPx)
            drawLine(gridThin, Offset(0f, pos), Offset(size.width, pos), thinPx)
        }
        val thickPx = 2.5.dp.toPx()
        for (i in 0..3) {
            val pos = i * cellSize * 3
            drawLine(gridThick, Offset(pos, 0f), Offset(pos, size.height), thickPx)
            drawLine(gridThick, Offset(0f, pos), Offset(size.width, pos), thickPx)
        }

        // Killer cage borders (solid, color-matched) and sum labels — drawn on top of grid lines.
        // Solid cage-color lines at a lower alpha read as their own layer without the
        // muddy dashed look at small sizes.
        if (cages.isNotEmpty()) {
            val cageOfCell = IntArray(81) { -1 }
            for ((ci, cage) in cages.withIndex()) for (cell in cage.cells) cageOfCell[cell] = ci

            val inset = 3.dp.toPx()
            val cageLineWidth = 1.4.dp.toPx()
            val cageBorderColor = cageBorder
            val cageFill = cageTint

            // Tint each cage area subtly to reinforce grouping (very low alpha so values stay readable).
            for (cell in 0 until 81) {
                val ci = cageOfCell[cell]
                if (ci < 0) continue
                // Only fill cells that are NOT the selected/peer/same highlighted ones (those already tint).
                // We draw cage tint as a thin border-hugging rect so it doesn't compete with cell highlights.
                val r = cell / 9; val c = cell % 9
                drawRect(
                    color = cageFill,
                    topLeft = Offset(c * cellSize + inset, r * cellSize + inset),
                    size = Size(cellSize - 2 * inset, cellSize - 2 * inset)
                )
            }

            for (cell in 0 until 81) {
                val ci = cageOfCell[cell]
                if (ci < 0) continue
                val r = cell / 9; val c = cell % 9
                val x0 = c * cellSize + inset
                val y0 = r * cellSize + inset
                val x1 = (c + 1) * cellSize - inset
                val y1 = (r + 1) * cellSize - inset
                // Top
                if (r == 0 || cageOfCell[cell - 9] != ci) {
                    drawLine(cageBorderColor, Offset(x0, y0), Offset(x1, y0), cageLineWidth)
                }
                // Bottom
                if (r == 8 || cageOfCell[cell + 9] != ci) {
                    drawLine(cageBorderColor, Offset(x0, y1), Offset(x1, y1), cageLineWidth)
                }
                // Left
                if (c == 0 || cageOfCell[cell - 1] != ci) {
                    drawLine(cageBorderColor, Offset(x0, y0), Offset(x0, y1), cageLineWidth)
                }
                // Right
                if (c == 8 || cageOfCell[cell + 1] != ci) {
                    drawLine(cageBorderColor, Offset(x1, y0), Offset(x1, y1), cageLineWidth)
                }
            }

            // Sum labels in top-left corner of each cage's label cell — distinct color so they
            // don't blend into the grid, and scale up with the largeText preference.
            val labelFontSize = if (largeText) 12.sp else 10.sp
            val labelStyle = TextStyle(
                color = cageLabelColor,
                fontSize = labelFontSize,
                fontWeight = FontWeight.Bold
            )
            for (cage in cages) {
                if (cage.cells.size < 2) continue  // single-cell cages: value is already shown as a given
                val labelCell = cage.labelCell
                val r = labelCell / 9; val c = labelCell % 9
                val layout = measurer.measure(cage.targetSum.toString(), labelStyle)
                drawText(
                    layout,
                    topLeft = Offset(
                        c * cellSize + 4.dp.toPx(),
                        r * cellSize + 2.dp.toPx()
                    )
                )
            }
        }

        for (i in 0 until 81) {
            val cell = board.cells[i]
            val r = i / 9; val c = i % 9
            val cx = c * cellSize
            val cy = r * cellSize
            if (cell.value != 0) {
                val color = when {
                    i in conflicts -> errorColor
                    cell.isGiven -> givenColor
                    cell.isHint -> hintColor
                    else -> userColor
                }
                drawCenteredText(
                    measurer = measurer,
                    text = cell.value.toString(),
                    rect = Rect(Offset(cx, cy), Size(cellSize, cellSize)),
                    style = TextStyle(color = color, fontSize = cellValueFontSize, fontWeight = FontWeight.Medium)
                )
            } else if (cell.notes.isNotEmpty()) {
                if (centeredNotes) {
                    // Render all notes as a single horizontal string in the centered position.
                    val text = cell.notes.sorted().joinToString("")
                    drawCenteredText(
                        measurer = measurer,
                        text = text,
                        rect = Rect(Offset(cx, cy), Size(cellSize, cellSize)),
                        style = TextStyle(color = noteColor, fontSize = noteFontSize)
                    )
                } else {
                    val noteSize = cellSize / 3f
                    for (n in 1..9) {
                        if (n in cell.notes) {
                            val nr = (n - 1) / 3
                            val nc = (n - 1) % 3
                            drawCenteredText(
                                measurer = measurer,
                                text = n.toString(),
                                rect = Rect(
                                    Offset(cx + nc * noteSize, cy + nr * noteSize),
                                    Size(noteSize, noteSize)
                                ),
                                style = TextStyle(color = noteColor, fontSize = noteFontSize)
                            )
                        }
                    }
                }
            }
        }
        }

        // Transparent a11y overlay: 9x9 grid of invisible, focusable, clickable cells so
        // screen-reader users can navigate and act on the board. The Canvas draws the visuals;
        // this layer carries the semantics that TalkBack reads.
        Column(modifier = Modifier.fillMaxSize()) {
            for (r in 0..8) {
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    for (c in 0..8) {
                        val idx = r * 9 + c
                        val cell = board.cells[idx]
                        val cageIdx = cageOfCellForA11y[idx]
                        val description = buildA11yDescription(
                            row = r,
                            col = c,
                            cell = cell,
                            isSelected = selected == idx,
                            cage = cageIdx.takeIf { it >= 0 }?.let { cages[it] }
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .semantics {
                                    contentDescription = description
                                    role = Role.Button
                                }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onCellTap(r, c) }
                        )
                    }
                }
            }
        }
    }
}

private fun buildA11yDescription(
    row: Int,
    col: Int,
    cell: com.ninersudoku.game.Cell,
    isSelected: Boolean,
    cage: KillerCage?
): String {
    val loc = "Row ${row + 1}, column ${col + 1}"
    val state = when {
        cell.value == 0 && cell.notes.isEmpty() -> "empty"
        cell.value == 0 -> "notes: ${cell.notes.sorted().joinToString(", ")}"
        cell.isGiven -> "given ${cell.value}"
        cell.isHint -> "hint ${cell.value}"
        else -> "value ${cell.value}"
    }
    val selectedTag = if (isSelected) ", selected" else ""
    val cageTag = if (cage != null && cage.cells.size > 1) ", cage sum ${cage.targetSum}" else ""
    return "$loc, $state$cageTag$selectedTag"
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCenteredText(
    measurer: TextMeasurer,
    text: String,
    rect: Rect,
    style: TextStyle
) {
    val layout = measurer.measure(text, style)
    val x = rect.left + (rect.width - layout.size.width) / 2f
    val y = rect.top + (rect.height - layout.size.height) / 2f
    drawText(layout, topLeft = Offset(x, y))
}
