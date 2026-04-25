package com.ninersudoku.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import com.ninersudoku.achievements.Achievement
import com.ninersudoku.achievements.AchievementManager
import com.ninersudoku.daily.DailyManager
import com.ninersudoku.game.Difficulty
import com.ninersudoku.game.GameMode
import com.ninersudoku.stats.DifficultyStats
import com.ninersudoku.stats.StatsManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

@Composable
fun StatsScreen(onBack: () -> Unit) {
    val statsMap by StatsManager.stats.collectAsState()
    val unlockedIds by AchievementManager.unlocked.collectAsState()
    val unlockDates by AchievementManager.unlockDates.collectAsState()
    val dailyState by DailyManager.state.collectAsState()
    val context = LocalContext.current
    var showResetConfirm by remember { mutableStateOf(false) }
    var detailAchievement by remember { mutableStateOf<Achievement?>(null) }

    BackHandler { onBack() }

    val totalPlayed = statsMap.values.sumOf { it.played }
    val totalWon = statsMap.values.sumOf { it.won }
    val totalSecondsSolving = statsMap.values.sumOf { it.totalWinTimeSeconds }

    // Find the best time across every difficulty + mode, plus which difficulty/mode owns it.
    data class BestEver(val difficulty: Difficulty, val mode: GameMode, val seconds: Int)
    val bestEver: BestEver? = statsMap.entries.flatMap { (d, s) ->
        s.bestTimePerMode.map { (m, t) -> BestEver(d, m, t) }
    }.minByOrNull { it.seconds }

    // Aggregate "favorite mode" — the mode appearing most across difficulties' best-time maps.
    val favoriteMode: GameMode? = statsMap.values
        .flatMap { it.bestTimePerMode.keys }
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key

    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    "Statistics",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Empty state — first launch with no plays yet.
            if (totalPlayed == 0) {
                EmptyStatsState(unlocked = unlockedIds.size, total = Achievement.values().size)
                Spacer(Modifier.height(20.dp))
            }

            // Best-ever hero card — only when there's at least one win.
            if (bestEver != null) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚡", fontSize = 28.sp)
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Personal best",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                formatStatTime(bestEver.seconds),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                bestEver.difficulty.displayName,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                bestEver.mode.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Overall summary
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SummaryStat("Played", totalPlayed.toString())
                        SummaryStat("Won", totalWon.toString())
                        SummaryStat(
                            "Win rate",
                            if (totalPlayed == 0) "—" else "${(totalWon * 100 / totalPlayed)}%"
                        )
                    }
                    if (totalSecondsSolving > 0) {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Total time solving",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                formatLongTime(totalSecondsSolving),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (favoriteMode != null && totalWon > 0) {
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Favorite mode",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                favoriteMode.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Daily heatmap (last 12 weeks) — show empty-state nudge when nothing yet
            // but only if the user has played other puzzles (otherwise the top empty state covers it).
            if (dailyState.solvedDates.isNotEmpty()) {
                Text(
                    "Daily activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(8.dp))
                DailyHeatmap(
                    solvedDates = dailyState.solvedDates,
                    streak = dailyState.displayStreak(DailyManager.todayEpochDay()),
                    bestStreak = dailyState.bestStreak,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(20.dp))
            } else if (totalPlayed > 0) {
                Text(
                    "Daily activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(8.dp))
                EmptyHeatmapPreview(modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(20.dp))
            }

            // Per-difficulty cards
            Difficulty.values().forEach { difficulty ->
                DifficultyCard(
                    difficulty = difficulty,
                    stats = statsMap[difficulty] ?: DifficultyStats()
                )
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(20.dp))

            // Achievements section
            Text(
                "Achievements (${unlockedIds.size}/${Achievement.values().size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(10.dp))

            val rows = Achievement.values().toList().chunked(3)
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { a ->
                            AchievementCell(
                                achievement = a,
                                unlocked = a.id in unlockedIds,
                                onClick = { detailAchievement = a },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(3 - rowItems.size) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Reset button
            OutlinedButton(
                onClick = { showResetConfirm = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .height(48.dp)
            ) {
                Text("Reset all data", color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset everything?") },
            text = { Text("This clears every difficulty's record AND all unlocked achievements. The action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    StatsManager.reset(context)
                    AchievementManager.reset(context)
                    showResetConfirm = false
                }) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
    }

    detailAchievement?.let { a ->
        AchievementDetailDialog(
            achievement = a,
            unlocked = a.id in unlockedIds,
            unlockedDay = unlockDates[a.id],
            onDismiss = { detailAchievement = null }
        )
    }
}

@Composable
private fun AchievementDetailDialog(
    achievement: Achievement,
    unlocked: Boolean,
    unlockedDay: Long?,
    onDismiss: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = null,
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                if (achievement.drawableRes != null) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = achievement.drawableRes),
                        contentDescription = null,
                        modifier = Modifier
                            .size(96.dp)
                            .shadow(
                                elevation = if (unlocked) 8.dp else 0.dp,
                                shape = androidx.compose.foundation.shape.CircleShape,
                                clip = false
                            )
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .alpha(if (unlocked) 1f else 0.30f)
                    )
                } else {
                    Text(
                        text = if (unlocked) achievement.icon else "🔒",
                        fontSize = 64.sp,
                        modifier = Modifier.alpha(if (unlocked) 1f else 0.5f)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    achievement.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (unlocked) cs.onSurface else cs.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    achievement.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurface.copy(alpha = 0.75f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (unlocked) cs.tertiaryContainer else cs.surfaceVariant
                ) {
                    Text(
                        text = when {
                            !unlocked -> "Locked"
                            unlockedDay != null -> "Unlocked ${formatUnlockDate(unlockedDay)}"
                            else -> "Unlocked"
                        },
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (unlocked) cs.onTertiaryContainer else cs.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    )
}

private fun formatUnlockDate(epochDay: Long): String {
    val date = LocalDate.ofEpochDay(epochDay)
    val today = LocalDate.now()
    val days = today.toEpochDay() - epochDay
    return when {
        days == 0L -> "today"
        days == 1L -> "yesterday"
        days < 7L -> "$days days ago"
        else -> date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }
}

@Composable
private fun EmptyStatsState(unlocked: Int, total: Int) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cs.primaryContainer.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📊", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "No games yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = cs.onPrimaryContainer
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Solve a puzzle to start tracking your best times, streaks, and achievements.",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onPrimaryContainer.copy(alpha = 0.75f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (unlocked == 0 && total > 0) {
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = cs.tertiary.copy(alpha = 0.2f)
                ) {
                    Text(
                        "0 of $total achievements unlocked",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = cs.onPrimaryContainer.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementCell(
    achievement: Achievement,
    unlocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (unlocked) cs.tertiaryContainer else cs.surfaceVariant.copy(alpha = 0.4f),
        tonalElevation = if (unlocked) 2.dp else 0.dp,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .defaultMinSize(minHeight = 116.dp)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (achievement.drawableRes != null) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = achievement.drawableRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(
                            elevation = if (unlocked) 5.dp else 0.dp,
                            shape = androidx.compose.foundation.shape.CircleShape,
                            clip = false
                        )
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .alpha(if (unlocked) 1f else 0.30f)
                )
            } else {
                Text(
                    text = if (unlocked) achievement.icon else "🔒",
                    fontSize = 26.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = achievement.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (unlocked) cs.onTertiaryContainer else cs.onSurface.copy(alpha = 0.6f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(label, fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
    }
}

@Composable
private fun DifficultyCard(difficulty: Difficulty, stats: DifficultyStats) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    difficulty.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${stats.won}/${stats.played} won",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.height(8.dp))

            // Per-mode best times — show only modes that have a win.
            if (stats.bestTimePerMode.isEmpty()) {
                StatRow("Best time", "—")
            } else {
                stats.bestTimePerMode.forEach { (mode, time) ->
                    StatRow("Best ${mode.displayName}", formatStatTime(time))
                }
            }
            StatRow("Average win time", stats.averageWinTime?.let { formatStatTime(it) } ?: "—")
            StatRow(
                "Win rate",
                if (stats.played == 0) "—" else "${(stats.winRate * 100).toInt()}%"
            )
            if (stats.won > 0) {
                StatRow("Time on this level", formatLongTime(stats.totalWinTimeSeconds))
            }
            if (stats.played > 0 && (stats.totalMistakes > 0 || stats.totalHints > 0)) {
                val avgMistakes = "%.1f".format(stats.totalMistakes.toFloat() / stats.played)
                val avgHints = "%.1f".format(stats.totalHints.toFloat() / stats.played)
                StatRow("Avg per game", "$avgMistakes mistake${if (avgMistakes == "1.0") "" else "s"} · $avgHints hint${if (avgHints == "1.0") "" else "s"}")
            }
            if (stats.recentWinTimes.size >= 3) {
                Spacer(Modifier.height(10.dp))
                BestTimesSparkline(
                    times = stats.recentWinTimes,
                    bestTime = stats.bestTimeSeconds
                )
            }
        }
    }
}

@Composable
private fun DailyHeatmap(
    solvedDates: Set<Long>,
    streak: Int,
    bestStreak: Int,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val colorBlind by com.ninersudoku.prefs.DisplayPreferences.colorBlind.collectAsState()
    val today = LocalDate.now()
    // Show 12 columns × 7 rows (84 days). Right-most column is current week.
    val weeks = 12
    val columns = (0 until weeks).map { weekIdx ->
        // Each column is one week. Order: Mon..Sun.
        (0..6).map { dow ->
            // Date for this cell: today - (weeks - 1 - weekIdx)*7 + (dayOfWeek shift)
            val daysFromToday = (weeks - 1 - weekIdx) * 7 + (today.dayOfWeek.value - 1 - dow)
            today.minusDays(daysFromToday.toLong())
        }
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cs.surfaceVariant.copy(alpha = 0.4f),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔥", fontSize = 18.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    "$streak day${if (streak == 1) "" else "s"} · best $bestStreak",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                columns.forEach { weekDays ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        weekDays.forEach { date ->
                            val solved = date.toEpochDay() in solvedDates
                            val isFuture = date.isAfter(today)
                            Box(
                                modifier = Modifier
                                    .height(14.dp)
                                    .width(14.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        when {
                                            isFuture -> cs.outline.copy(alpha = 0.08f)
                                            solved -> cs.tertiary
                                            else -> cs.outline.copy(alpha = 0.18f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                // Pattern cue for CB users — a small dot inside solved cells
                                // so the distinction doesn't rely on hue alone.
                                if (solved && colorBlind) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(cs.onTertiary)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHeatmapPreview(modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val weeks = 12
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cs.surfaceVariant.copy(alpha = 0.4f),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🔥", fontSize = 28.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Start your daily streak",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Solve today's puzzle to light up a square",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurface.copy(alpha = 0.65f)
            )
            Spacer(Modifier.height(14.dp))
            // Muted preview grid — every cell empty, same shape as the live heatmap.
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(weeks) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(7) {
                            Box(
                                modifier = Modifier
                                    .height(12.dp)
                                    .width(12.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(cs.outline.copy(alpha = 0.18f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BestTimesSparkline(times: List<Int>, bestTime: Int?) {
    val cs = MaterialTheme.colorScheme
    val minT = (bestTime ?: times.min()).coerceAtLeast(1)
    val maxT = times.max().coerceAtLeast(minT + 1)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Last ${times.size} wins",
                style = MaterialTheme.typography.labelMedium,
                color = cs.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.weight(1f)
            )
            Text(
                "${formatStatTime(maxT)} → ${formatStatTime(minT)}",
                style = MaterialTheme.typography.labelMedium,
                color = cs.onSurface.copy(alpha = 0.6f)
            )
        }
        Spacer(Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            val w = size.width; val h = size.height
            val pad = 2f
            val n = times.size
            if (n < 2) return@Canvas
            val xStep = (w - pad * 2) / (n - 1)
            val span = (maxT - minT).toFloat().coerceAtLeast(1f)
            val points = times.mapIndexed { i, t ->
                val nx = pad + i * xStep
                val ny = pad + (1f - (t - minT) / span) * (h - pad * 2)
                Offset(nx, ny)
            }
            for (i in 0 until points.size - 1) {
                drawLine(
                    color = cs.primary,
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 3f
                )
            }
            for (p in points) {
                drawCircle(color = cs.primary, radius = 3f, center = p)
            }
        }
    }
}


private fun formatLongTime(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%dh %dm".format(h, m)
        else if (m > 0) "%dm %ds".format(m, s)
        else "%ds".format(s)
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

private fun formatStatTime(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%d:%02d".format(m, s)
}
