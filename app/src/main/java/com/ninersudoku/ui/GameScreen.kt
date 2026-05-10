package com.ninersudoku.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.ninersudoku.daily.DailyManager
import com.ninersudoku.game.GameMode
import com.ninersudoku.sound.SoundManager
import com.ninersudoku.ui.celebration.CelebrationOverlay
import com.ninersudoku.viewmodel.GameViewModel
import com.ninersudoku.viewmodel.LossReason
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val state = viewModel.state
    val board = state.board
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    var moreMenuOpen by remember { mutableStateOf(false) }
    var showGiveUpConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        viewModel.backToDifficulty()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> viewModel.pauseTimer()
                Lifecycle.Event.ON_RESUME -> viewModel.resumeTimer()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Drive the board shake when the user tries to modify a locked cell or makes a mistake.
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(state.rejectionTick) {
        if (state.rejectionTick > 0) {
            shakeOffset.snapTo(0f)
            for (target in listOf(22f, -22f, 16f, -16f, 8f, -8f, 0f)) {
                shakeOffset.animateTo(target, tween(45))
            }
        }
    }
    // Strong haptic specifically on a mistake (in addition to the per-tap haptic).
    LaunchedEffect(state.mistakeTick) {
        if (state.mistakeTick > 0) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    // Pulse the mistake counter when it changes.
    val mistakePulse = remember { Animatable(1f) }
    LaunchedEffect(state.mistakes) {
        if (state.mistakes > 0) {
            mistakePulse.snapTo(1.4f)
            mistakePulse.animateTo(1f, tween(280))
        }
    }

    // Speed mode: haptic pulses at 30s, 10s, 5s remaining so focused players don't miss it.
    val speedWarningTriggered = remember { mutableStateOf(setOf<Int>()) }
    LaunchedEffect(state.screen, state.isSpeed) {
        if (state.screen != com.ninersudoku.viewmodel.Screen.GAME || !state.isSpeed) {
            speedWarningTriggered.value = emptySet()
        }
    }
    if (state.isSpeed && !state.isFinished && !state.isPaused) {
        val remaining = state.secondsRemaining
        LaunchedEffect(remaining) {
            val thresholds = listOf(30, 10, 5)
            for (t in thresholds) {
                if (remaining == t && t !in speedWarningTriggered.value) {
                    speedWarningTriggered.value = speedWarningTriggered.value + t
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (t <= 5) {
                        // Double pulse in the final seconds.
                        kotlinx.coroutines.delay(120)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
            }
        }
    }

    // Win-reveal: a brief scale pulse + tertiary tint flash on the board, then the
    // celebration overlay fades in (after a short delay so the reveal can be seen).
    val winScale = remember { Animatable(1f) }
    val winFlash = remember { Animatable(0f) }
    var celebrationReady by remember { mutableStateOf(false) }
    LaunchedEffect(state.isComplete, state.isGameOver) {
        if (state.isComplete && !state.isGameOver) {
            celebrationReady = false
            winFlash.snapTo(0f)
            winScale.snapTo(1f)
            coroutineScope {
                launch {
                    winScale.animateTo(1.05f, tween(280))
                    winScale.animateTo(1f, tween(380))
                }
                launch {
                    winFlash.animateTo(0.55f, tween(180))
                    winFlash.animateTo(0f, tween(900))
                }
            }
            celebrationReady = true
        } else {
            celebrationReady = state.isGameOver  // losses go straight to overlay
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        TopBar(
            difficulty = state.difficulty?.displayName.orEmpty(),
            mode = state.mode,
            isDaily = state.isDaily,
            isPaused = state.isPaused,
            canRestart = !state.isDaily,
            mistakes = state.mistakes,
            mistakeLimit = state.mistakeLimit,
            mistakeScale = mistakePulse.value,
            elapsed = state.elapsedSeconds,
            countdownSeconds = if (state.isSpeed) state.secondsRemaining else null,
            onBack = viewModel::backToDifficulty,
            onPauseToggle = viewModel::togglePause,
            menuOpen = moreMenuOpen,
            onMenuOpen = { moreMenuOpen = true },
            onMenuDismiss = { moreMenuOpen = false },
            onRestart = {
                moreMenuOpen = false
                viewModel.restartCurrentDifficulty()
            },
            onGiveUp = {
                moreMenuOpen = false
                showGiveUpConfirm = true
            }
        )

        if (state.isLoading || board == null) {
            LoadingState(
                difficulty = state.difficulty?.displayName.orEmpty(),
                mode = state.mode.displayName,
                isKiller = state.mode == GameMode.KILLER
            )
            return@Column
        }

        val solution = state.solution
        val conflicts = remember(board) { board.conflicts() }
        // Only count CORRECT placements toward the per-digit "remaining" badge.
        val remaining = remember(board, solution) {
            val counts = IntArray(10)
            if (solution != null) {
                for (i in 0 until 81) {
                    val v = board.cells[i].value
                    if (v in 1..9 && v == solution[i]) counts[v]++
                }
            }
            IntArray(10) { d -> if (d == 0) 0 else (9 - counts[d]).coerceAtLeast(0) }
        }

        val boardContent: @Composable (Modifier) -> Unit = { boardModifier ->
            Box(modifier = boardModifier) {
                BoardView(
                    board = board,
                    selected = state.selected,
                    highlightDigit = state.highlightDigit,
                    conflicts = conflicts,
                    completedFlash = state.completedFlash,
                    cages = state.cages,
                    onCellTap = { row, col ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        SoundManager.playTap()
                        viewModel.selectCell(row, col)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = shakeOffset.value
                            scaleX = winScale.value
                            scaleY = winScale.value
                        }
                )
                if (winFlash.value > 0f) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(8.dp)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = winFlash.value * 0.4f))
                    )
                }
            }
        }

        val actionRowContent: @Composable () -> Unit = {
            ActionRow(
                notesMode = state.notesMode,
                hintsUsed = state.hintsUsed,
                hintLimit = state.hintLimit,
                hintsRemaining = state.hintsRemaining,
                canUndo = state.canUndo,
                canErase = state.canErase,
                isFinished = state.isFinished,
                onUndo = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.undo()
                },
                onErase = {
                    if (selectedCellIsGivenOrFinished(state)) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.signalRejection()
                    } else {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.erase()
                    }
                },
                onNotes = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.toggleNotesMode()
                },
                onHint = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.useHint()
                }
            )
        }

        val numberPadContent: @Composable () -> Unit = {
            NumberPad(
                // Enabled whenever the game is active — with no cell selected, taps act as filter
                // highlight instead of value entry.
                enabled = !state.isFinished,
                remainingCounts = remaining,
                activeDigit = state.activeDigit,
                legalDigits = state.legalDigitsForSelection,
                onTap = { n ->
                    when {
                        state.selected == null -> {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            SoundManager.playTap()
                            viewModel.toggleDigitHighlight(n)
                        }
                        selectedCellIsGivenOrFinished(state) -> {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.signalRejection()
                        }
                        else -> {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.enterValue(n)
                        }
                    }
                },
                onLongPress = { n ->
                    when {
                        state.selected == null -> {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            SoundManager.playTap()
                            viewModel.toggleDigitHighlight(n)
                        }
                        selectedCellIsGivenOrFinished(state) -> {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.signalRejection()
                        }
                        else -> {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.enterValue(n, asNote = !state.notesMode)
                        }
                    }
                }
            )
        }

        val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                boardContent(
                    Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                )
                Spacer(Modifier.width(16.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 520.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = com.ninersudoku.R.drawable.niner_hero),
                        contentDescription = null,
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(18.dp))
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "NINER",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        letterSpacing = 4.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(24.dp))
                    actionRowContent()
                    Spacer(Modifier.height(16.dp))
                    numberPadContent()
                }
            }
        } else {
            boardContent(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
            Spacer(Modifier.height(12.dp))
            actionRowContent()
            Spacer(Modifier.height(16.dp))
            numberPadContent()
            Spacer(Modifier.height(16.dp))
        }
    }

    if (state.isPaused && !state.isFinished) {
        PauseOverlay(
            elapsedSeconds = state.elapsedSeconds,
            difficultyName = state.difficulty?.displayName.orEmpty(),
            modeName = if (state.mode == GameMode.CLASSIC) "" else state.mode.displayName,
            onResume = viewModel::togglePause
        )
    }

    state.pendingHint?.let { hint ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissPendingHint(applyValue = false) },
            title = null,
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "R${hint.row + 1} · C${hint.col + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        hint.technique,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        hint.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Try yourself — free. Show me uses 1 hint.",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissPendingHint(applyValue = true) }) {
                    Text("Show me", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPendingHint(applyValue = false) }) {
                    Text("Try yourself")
                }
            }
        )
    }

    if (showGiveUpConfirm) {
        AlertDialog(
            onDismissRequest = { showGiveUpConfirm = false },
            title = { Text("Give up?") },
            text = { Text("The solution will be revealed and this game will end as a loss.") },
            confirmButton = {
                TextButton(onClick = {
                    showGiveUpConfirm = false
                    viewModel.giveUp()
                }) {
                    Text("Give up", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGiveUpConfirm = false }) { Text("Keep playing") }
            }
        )
    }

    if (state.isFinished && !state.celebrationDismissed && celebrationReady) {
        val outcome = state.finishOutcome
        val won = state.isComplete
        val statsLines = mutableListOf<Pair<String, String>>()
        statsLines += "Time" to formatTime(state.elapsedSeconds)
        if (state.mode != GameMode.COACH) {
            statsLines += "Mistakes" to "${state.mistakes}/${state.mistakeLimit}"
        }
        if (state.hintLimit > 0 && state.hintLimit != Int.MAX_VALUE) {
            statsLines += "Hints" to "${state.hintsUsed}/${state.hintLimit}"
        }
        if (won && outcome != null) {
            outcome.previousBest?.let { statsLines += "Previous best" to formatTime(it) }
            statsLines += "Wins on ${state.difficulty?.displayName}" to outcome.gamesWonThisDifficulty.toString()
        }

        // Daily streak (computed for the title block + share text).
        val dailyState = DailyManager.state.collectAsState().value
        val today = remember { DailyManager.todayEpochDay() }

        CelebrationOverlay(
            title = when {
                won && state.isDaily -> "Daily solved!"
                won -> "Solved!"
                state.lossReason == LossReason.GAVE_UP -> "Game revealed"
                state.lossReason == LossReason.SPEED_TIMEOUT -> "Time's up"
                else -> "Game over"
            },
            subtitle = when {
                !won && state.lossReason == LossReason.GAVE_UP -> "The full solution is on the board — take a look."
                !won && state.lossReason == LossReason.SPEED_TIMEOUT -> "The clock ran out this round."
                !won -> "Hit the ${state.mistakeLimit}-mistake limit"
                state.isDaily -> "Daily · ${LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d"))}"
                else -> state.difficulty?.displayName.orEmpty()
            },
            stats = statsLines.also {
                if (won && state.isDaily) it.add("Streak" to "🔥 ${dailyState.displayStreak(today)}")
            },
            // For daily, "New game" doesn't make sense (one per day) — promote Main menu instead.
            primaryLabel = if (state.isDaily) "Main menu" else "New game",
            onPrimary = if (state.isDaily) viewModel::backToDifficulty else viewModel::restartCurrentDifficulty,
            tertiaryLabel = "Main menu",
            onTertiary = if (state.isDaily) null else viewModel::backToDifficulty,
            secondaryLabel = when {
                won -> "View board"
                state.lossReason == LossReason.GAVE_UP -> "See solution"
                else -> "View board"
            },
            onSecondary = { viewModel.dismissCelebration() },
            emoji = when {
                won && state.isDaily -> "🔥"
                won -> "🎉"
                state.lossReason == LossReason.GAVE_UP -> "🙈"
                state.lossReason == LossReason.SPEED_TIMEOUT -> "⏱️"
                else -> "💪"
            },
            showParticles = won,
            badge = when {
                won && state.isDaily -> "DAILY"
                won && outcome?.newBest == true -> "NEW BEST"
                else -> null
            },
            achievements = state.pendingAchievements,
            highlightStatLabel = if (won && outcome?.newBest == true) "Time" else null,
            onShare = if (won) ({
                val text = buildShareText(
                    isDaily = state.isDaily,
                    difficulty = state.difficulty?.displayName.orEmpty(),
                    timeSeconds = state.elapsedSeconds,
                    mistakes = state.mistakes,
                    hints = state.hintsUsed,
                    streak = if (state.isDaily) dailyState.displayStreak(today) else 0
                )
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(send, "Share result"))
            }) else null
        )
    }
}

private fun selectedCellIsGivenOrFinished(state: com.ninersudoku.viewmodel.GameState): Boolean {
    val board = state.board ?: return true
    val idx = state.selected ?: return true
    if (state.isFinished) return true
    return board.cells[idx].isGiven
}

private fun buildShareText(
    isDaily: Boolean,
    difficulty: String,
    timeSeconds: Int,
    mistakes: Int,
    hints: Int,
    streak: Int
): String {
    val timeStr = "%d:%02d".format(timeSeconds / 60, timeSeconds % 60)
    return buildString {
        if (isDaily) {
            val date = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d"))
            append("Sudoku Daily — $date\n")
        } else {
            append("Sudoku $difficulty\n")
        }
        append("⏱️ $timeStr · ")
        append(if (mistakes == 0) "🎯 perfect" else "❌ $mistakes mistake${if (mistakes == 1) "" else "s"}")
        if (hints > 0) append(" · 💡 $hints hint${if (hints == 1) "" else "s"}")
        if (isDaily && streak > 0) append("\n🔥 $streak day streak")
    }
}

@Composable
private fun TopBar(
    difficulty: String,
    mode: GameMode,
    isDaily: Boolean,
    isPaused: Boolean,
    canRestart: Boolean,
    mistakes: Int,
    mistakeLimit: Int,
    mistakeScale: Float,
    elapsed: Int,
    countdownSeconds: Int? = null,
    onBack: () -> Unit,
    onPauseToggle: () -> Unit,
    menuOpen: Boolean,
    onMenuOpen: () -> Unit,
    onMenuDismiss: () -> Unit,
    onRestart: () -> Unit,
    onGiveUp: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        IconButton(onClick = onPauseToggle) {
            Icon(
                if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                contentDescription = if (isPaused) "Resume" else "Pause"
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = difficulty, style = MaterialTheme.typography.titleLarge)
            if (mode != GameMode.CLASSIC || isDaily) {
                val tag = if (isDaily) "Daily" else mode.displayName
                Text(
                    text = tag,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        if (mode != GameMode.COACH) {
            Stat(
                label = "Mistakes",
                value = "$mistakes/$mistakeLimit",
                highlight = mistakes >= (mistakeLimit - 1).coerceAtLeast(1),
                scale = mistakeScale
            )
            Spacer(Modifier.width(12.dp))
        }
        if (countdownSeconds != null) {
            Stat(
                label = "Time left",
                value = formatTime(countdownSeconds),
                highlight = countdownSeconds < 30
            )
        } else {
            Stat(label = "Time", value = formatTime(elapsed))
        }
        Box {
            IconButton(onClick = onMenuOpen) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = onMenuDismiss) {
                if (canRestart) {
                    DropdownMenuItem(
                        text = { Text("New puzzle") },
                        onClick = onRestart
                    )
                }
                DropdownMenuItem(
                    text = { Text("Give up") },
                    onClick = onGiveUp
                )
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, highlight: Boolean = false, scale: Float = 1f) {
    Column(horizontalAlignment = Alignment.End) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Text(
            value,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (highlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        )
    }
}

@Composable
private fun ActionRow(
    notesMode: Boolean,
    hintsUsed: Int,
    hintLimit: Int,
    hintsRemaining: Int,
    canUndo: Boolean,
    canErase: Boolean,
    isFinished: Boolean,
    onUndo: () -> Unit,
    onErase: () -> Unit,
    onNotes: () -> Unit,
    onHint: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActionButton(
            icon = Icons.AutoMirrored.Filled.Undo,
            label = "Undo",
            enabled = canUndo && !isFinished,
            onClick = onUndo
        )
        ActionButton(
            icon = Icons.Filled.Backspace,
            label = "Erase",
            enabled = canErase,
            onClick = onErase
        )
        ActionButton(
            icon = Icons.Filled.EditNote,
            label = "Notes",
            highlighted = notesMode,
            enabled = !isFinished,
            onClick = onNotes
        )
        ActionButton(
            icon = Icons.Filled.Lightbulb,
            label = if (hintLimit == 0) "Hint" else "Hint $hintsUsed/$hintLimit",
            enabled = hintsRemaining > 0 && !isFinished,
            onClick = onHint
        )
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    highlighted: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val bg = when {
        !enabled -> cs.surfaceVariant.copy(alpha = 0.5f)
        highlighted -> cs.primary
        else -> cs.primary.copy(alpha = 0.10f)
    }
    val tint = when {
        !enabled -> cs.onSurfaceVariant.copy(alpha = 0.4f)
        highlighted -> cs.onPrimary
        else -> cs.primary
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            fontSize = 11.sp,
            color = cs.onBackground.copy(alpha = if (enabled) 0.7f else 0.4f)
        )
    }
}

@Composable
private fun PauseOverlay(
    elapsedSeconds: Int,
    difficultyName: String,
    modeName: String,
    onResume: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background.copy(alpha = 0.96f))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onResume() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = cs.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 36.dp, vertical = 32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(cs.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Pause,
                            contentDescription = null,
                            tint = cs.primary,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Paused",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        formatTime(elapsedSeconds),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Light,
                        color = cs.primary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (modeName.isEmpty()) difficultyName else "$difficultyName · $modeName",
                        style = MaterialTheme.typography.labelLarge,
                        color = cs.onSurface.copy(alpha = 0.55f)
                    )
                    Spacer(Modifier.height(20.dp))
                    androidx.compose.material3.Button(
                        onClick = onResume,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Resume", style = MaterialTheme.typography.labelLarge, fontSize = 16.sp)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Tap anywhere to resume",
                style = MaterialTheme.typography.labelMedium,
                color = cs.onBackground.copy(alpha = 0.5f)
            )
        }
    }
}


@Composable
private fun LoadingState(difficulty: String, mode: String, isKiller: Boolean) {
    val cs = MaterialTheme.colorScheme
    // 9-cell grid where each cell pulses in sequence.
    val infinite = androidx.compose.animation.core.rememberInfiniteTransition(label = "loader")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 9f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            tween(1800, easing = androidx.compose.animation.core.LinearEasing)
        ),
        label = "phase"
    )
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(120.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(3) { col ->
                                val idx = row * 3 + col
                                val active = (phase.toInt() % 9) == idx
                                val intensity = if (active) 1f
                                    else (1f - ((phase - idx + 9f) % 9f) / 4f).coerceIn(0f, 1f) * 0.4f
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(cs.primary.copy(alpha = 0.15f + intensity * 0.55f))
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = if (difficulty.isEmpty()) "Generating puzzle…"
                    else "Generating $difficulty $mode…",
                style = MaterialTheme.typography.titleMedium,
                color = cs.onBackground,
                fontWeight = FontWeight.SemiBold
            )
            if (isKiller) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Killer cages take a moment to lay out",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onBackground.copy(alpha = 0.55f)
                )
            }
        }
    }
}

private fun formatTime(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%d:%02d".format(m, s)
}
