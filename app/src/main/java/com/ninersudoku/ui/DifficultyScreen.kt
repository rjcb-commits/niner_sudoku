package com.ninersudoku.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ninersudoku.daily.DailyManager
import com.ninersudoku.game.Difficulty
import com.ninersudoku.game.GameMode
import com.ninersudoku.persistence.SaveManager
import com.ninersudoku.persistence.SavedGameSummary
import com.ninersudoku.stats.StatsManager
import com.ninersudoku.ui.celebration.CelebrationOverlay
import com.ninersudoku.ui.celebration.CelebrationStyle
import com.ninersudoku.ui.components.SettingsSheet
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private data class DifficultyMeta(
    val difficulty: Difficulty,
    val tagline: String,
    val level: Int,
    val icon: ImageVector
)

private val difficultyMeta = listOf(
    DifficultyMeta(Difficulty.BEGINNER, "Warm up", 1, Icons.Default.Spa),
    DifficultyMeta(Difficulty.EASY, "Steady pace", 2, Icons.Default.WbSunny),
    DifficultyMeta(Difficulty.MEDIUM, "Solid challenge", 3, Icons.Default.Bolt),
    DifficultyMeta(Difficulty.HARD, "Real workout", 4, Icons.Default.LocalFireDepartment),
    DifficultyMeta(Difficulty.EXPERT, "For pros", 5, Icons.Default.AutoAwesome)
)

@Composable
fun DifficultyScreen(
    onPick: (Difficulty, GameMode) -> Unit,
    onResume: () -> Unit,
    onPlayDaily: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenAbout: () -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }
    var celebrationPreview by remember { mutableStateOf<CelebrationStyle?>(null) }
    var selectedMode by remember { mutableStateOf(GameMode.CLASSIC) }
    var showModeSheet by remember { mutableStateOf(false) }
    val statsMap by StatsManager.stats.collectAsState()
    val savedSummary by SaveManager.summary.collectAsState()
    val dailyState by DailyManager.state.collectAsState()
    val today = remember { DailyManager.todayEpochDay() }
    val solvedToday = dailyState.solvedToday(today)
    val streak = dailyState.displayStreak(today)
    val totalPlayed = statsMap.values.sumOf { it.played }
    val totalWon = statsMap.values.sumOf { it.won }
    val totalTimeSeconds = statsMap.values.sumOf { it.totalWinTimeSeconds }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Top action row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onOpenStats) {
                Icon(
                    Icons.Default.BarChart,
                    contentDescription = "Statistics",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = { showSettings = true }) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }

        // Hero
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HeroMark()
            // Show a stats chip once the player has finished a game; otherwise let the
            // structure below (Daily / Continue / difficulty cards) speak for itself.
            if (totalPlayed > 0 && savedSummary == null) {
                Spacer(Modifier.height(12.dp))
                StatsChip(text = "$totalPlayed game${if (totalPlayed == 1) "" else "s"} · $totalWon won")
            }
            if (streak > 0) {
                Spacer(Modifier.height(8.dp))
                StreakChip(streak = streak)
            }
        }

        Spacer(Modifier.height(20.dp))

        // Daily card
        DailyCard(
            solvedToday = solvedToday,
            streak = streak,
            onClick = onPlayDaily
        )

        // Continue card (if a saved game exists)
        savedSummary?.let { summary ->
            Spacer(Modifier.height(10.dp))
            ContinueCard(summary = summary, onClick = onResume)
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "OR START NEW",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))

        // Mode selector — opens bottom sheet on tap
        ModeButton(
            selectedMode = selectedMode,
            onClick = { showModeSheet = true },
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(12.dp))

        // Difficulty cards
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            difficultyMeta.forEach { meta ->
                DifficultyCard(
                    meta = meta,
                    bestTime = statsMap[meta.difficulty]?.bestTime(selectedMode),
                    onClick = { onPick(meta.difficulty, selectedMode) }
                )
            }
        }

        if (totalTimeSeconds > 0) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Total time solving: ${formatLongTime(totalTimeSeconds)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(32.dp))
    }

    if (showSettings) {
        SettingsSheet(
            onDismiss = { showSettings = false },
            onPreviewCelebration = { style ->
                showSettings = false
                celebrationPreview = style
            },
            onOpenAbout = {
                showSettings = false
                onOpenAbout()
            }
        )
    }

    if (showModeSheet) {
        ModeSheet(
            selected = selectedMode,
            onSelect = {
                selectedMode = it
                showModeSheet = false
            },
            onDismiss = { showModeSheet = false }
        )
    }

    celebrationPreview?.let { style ->
        CelebrationOverlay(
            title = "Preview",
            subtitle = style.label,
            stats = emptyList(),
            primaryLabel = "Done",
            onPrimary = { celebrationPreview = null },
            secondaryLabel = "",
            onSecondary = null,
            emoji = "🎉",
            showParticles = style != CelebrationStyle.MINIMAL,
            badge = null
        )
    }
}

private fun formatLongTime(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    return if (h > 0) "%dh %dm".format(h, m) else "%dm".format(m.coerceAtLeast(1))
}

@Composable
private fun HeroMark() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(
                    id = com.ninersudoku.R.drawable.niner_hero
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Text(
                text = "NINER",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black
            )
        }
        Text(
            text = "the daily sudoku",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 2.dp, start = 60.dp)
        )
    }
}

@Composable
private fun MiniGridIcon(size: androidx.compose.ui.unit.Dp) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(cs.primary.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(6.dp), verticalArrangement = Arrangement.SpaceBetween) {
            repeat(3) { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    repeat(3) { col ->
                        val filled = (row + col) % 2 == 0
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (filled) cs.primary else cs.primary.copy(alpha = 0.25f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsChip(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ModeButton(
    selectedMode: GameMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val isClassic = selectedMode == GameMode.CLASSIC
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isClassic) cs.surfaceVariant.copy(alpha = 0.4f)
            else cs.primary.copy(alpha = 0.12f),
        border = if (isClassic) null else BorderStroke(1.5.dp, cs.primary.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(modeIcon(selectedMode), fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mode",
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = selectedMode.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isClassic) cs.onSurface else cs.primary
                )
            }
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Change mode",
                tint = cs.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeSheet(
    selected: GameMode,
    onSelect: (GameMode) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val cs = MaterialTheme.colorScheme
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text(
                "Game mode",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(16.dp))
            GameMode.values().forEach { mode ->
                val isSelected = mode == selected
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) cs.primaryContainer.copy(alpha = 0.5f)
                        else cs.surfaceVariant.copy(alpha = 0.3f),
                    border = if (isSelected) BorderStroke(2.dp, cs.primary) else null,
                    tonalElevation = if (isSelected) 2.dp else 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelect(mode) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(modeIcon(mode), fontSize = 26.sp)
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) cs.onPrimaryContainer else cs.onSurface
                            )
                            Text(
                                text = mode.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) cs.onPrimaryContainer.copy(alpha = 0.75f)
                                    else cs.onSurface.copy(alpha = 0.65f)
                            )
                        }
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = cs.primary
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun modeIcon(mode: GameMode): String = when (mode) {
    GameMode.CLASSIC -> "🎯"
    GameMode.NO_MISTAKES -> "🚫"
    GameMode.COACH -> "🎓"
    GameMode.SPEED -> "⏱"
    GameMode.KILLER -> "🧩"
}

@Composable
private fun StreakChip(streak: Int) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cs.tertiaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("🔥", fontSize = 14.sp)
            Text(
                text = "$streak day${if (streak == 1) "" else "s"} in a row",
                color = cs.onTertiaryContainer,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DailyCard(solvedToday: Boolean, streak: Int, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val dateLabel = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d")) }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (solvedToday) cs.tertiaryContainer else cs.tertiary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = !solvedToday) { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (solvedToday) cs.onTertiaryContainer.copy(alpha = 0.15f)
                        else cs.onTertiary.copy(alpha = 0.18f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(if (solvedToday) "✓" else "🔥", fontSize = 22.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (solvedToday) "Daily complete" else "Daily Sudoku",
                    color = if (solvedToday) cs.onTertiaryContainer.copy(alpha = 0.85f)
                        else cs.onTertiary.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = dateLabel,
                    color = if (solvedToday) cs.onTertiaryContainer else cs.onTertiary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (solvedToday) "Come back tomorrow" else "One puzzle, everyone, every day",
                    color = if (solvedToday) cs.onTertiaryContainer.copy(alpha = 0.85f)
                        else cs.onTertiary.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (!solvedToday) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = cs.onTertiary
                )
            }
        }
    }
}

@Composable
private fun ContinueCard(summary: SavedGameSummary, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cs.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(cs.onPrimary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = cs.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Continue",
                    color = cs.onPrimary.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = summary.difficulty.displayName,
                    color = cs.onPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${formatMenuTime(summary.elapsedSeconds)} · ${summary.cellsRemaining} cells left",
                    color = cs.onPrimary.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = cs.onPrimary
            )
        }
    }
}

@Composable
private fun DifficultyCard(meta: DifficultyMeta, bestTime: Int?, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = cs.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(cs.primary.copy(alpha = 0.08f + meta.level * 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = meta.icon,
                    contentDescription = null,
                    tint = cs.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meta.difficulty.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface
                )
                Text(
                    text = meta.tagline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurface.copy(alpha = 0.6f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                DifficultyPips(level = meta.level)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = bestTime?.let { "Best ${formatMenuTime(it)}" } ?: "Best —",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (bestTime != null) cs.primary else cs.onSurface.copy(alpha = 0.45f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun DifficultyPips(level: Int) {
    val cs = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 1..5) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (i <= level) cs.primary else cs.outline.copy(alpha = 0.30f))
            )
        }
    }
}

private fun formatMenuTime(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%d:%02d".format(m, s)
}
