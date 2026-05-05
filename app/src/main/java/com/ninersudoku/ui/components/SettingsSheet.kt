package com.ninersudoku.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ninersudoku.prefs.DisplayPreferences
import com.ninersudoku.prefs.ThemeMode
import com.ninersudoku.sound.SoundManager
import com.ninersudoku.ui.celebration.CelebrationManager
import com.ninersudoku.ui.celebration.CelebrationPalettes
import com.ninersudoku.ui.celebration.CelebrationStyle
import com.ninersudoku.ui.theme.AppThemeVariant
import com.ninersudoku.ui.theme.ThemeManager

private data class ThemePreview(val variant: AppThemeVariant, val colors: List<Color>)

@Composable
private fun DisplayToggle(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            androidx.compose.material3.Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "Auto"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

private val themePreviews = listOf(
    ThemePreview(AppThemeVariant.EMBER, listOf(Color(0xFFE53935), Color(0xFFFF6D00), Color(0xFFFFAB00))),
    ThemePreview(AppThemeVariant.ELECTRIC, listOf(Color(0xFF2962FF), Color(0xFF00BFA5), Color(0xFFFF6D00))),
    ThemePreview(AppThemeVariant.NEON_NIGHT, listOf(Color(0xFFAA00FF), Color(0xFFFF1744), Color(0xFF00E5FF))),
    ThemePreview(AppThemeVariant.CITRUS, listOf(Color(0xFF00C853), Color(0xFFFFAB00), Color(0xFFFF6D00))),
    ThemePreview(AppThemeVariant.HYPERDRIVE, listOf(Color(0xFFD50000), Color(0xFF212121), Color(0xFFFF6D00)))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    onDismiss: () -> Unit,
    onPreviewCelebration: (CelebrationStyle) -> Unit = {},
    onOpenAbout: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState()
    val currentTheme by ThemeManager.currentTheme.collectAsState()
    val currentCelebration by CelebrationManager.currentStyle.collectAsState()
    val soundOn by SoundManager.enabled.collectAsState()
    val themeMode by DisplayPreferences.themeMode.collectAsState()
    val peerOn by DisplayPreferences.peerHighlight.collectAsState()
    val sameNumOn by DisplayPreferences.sameNumberHighlight.collectAsState()
    val largeText by DisplayPreferences.largeText.collectAsState()
    val centeredNotes by DisplayPreferences.centeredNotes.collectAsState()
    val colorBlind by DisplayPreferences.colorBlind.collectAsState()
    val autoRuleOut by DisplayPreferences.autoRuleOut.collectAsState()
    val context = LocalContext.current

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 4.dp)
        ) {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(20.dp))

            SectionHeader("Appearance")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeMode.values().forEach { mode ->
                    val isSelected = mode == themeMode
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { DisplayPreferences.setThemeMode(context, mode) }
                    ) {
                        Text(
                            text = mode.label(),
                            modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            DisplayToggle("Peer highlights", "Light up the row, column, and box of the selected cell.", peerOn) {
                DisplayPreferences.setPeerHighlight(context, it)
            }
            Spacer(Modifier.height(8.dp))
            DisplayToggle("Same-digit highlights", "Tint every cell that already has the selected digit.", sameNumOn) {
                DisplayPreferences.setSameNumberHighlight(context, it)
            }
            Spacer(Modifier.height(8.dp))
            DisplayToggle("Larger text", "Bigger digits and notes for easier reading.", largeText) {
                DisplayPreferences.setLargeText(context, it)
            }
            Spacer(Modifier.height(8.dp))
            DisplayToggle("Centered notes", "Display pencil notes in a single row instead of a 3×3 corner grid.", centeredNotes) {
                DisplayPreferences.setCenteredNotes(context, it)
            }
            Spacer(Modifier.height(8.dp))
            DisplayToggle("Color-blind palette", "Use yellow for hints and orange for errors.", colorBlind) {
                DisplayPreferences.setColorBlind(context, it)
            }
            Spacer(Modifier.height(8.dp))
            DisplayToggle(
                "Auto rule-out",
                "Dim digits that can't go in the selected cell. Off = pure deduction.",
                autoRuleOut
            ) { DisplayPreferences.setAutoRuleOut(context, it) }

            SectionDivider()
            SectionHeader("Theme")

            themePreviews.forEach { preview ->
                val isSelected = currentTheme == preview.variant
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { ThemeManager.setTheme(context, preview.variant) }
                        .then(
                            if (isSelected)
                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                            else Modifier
                        ),
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = if (isSelected) 4.dp else 1.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            preview.colors.forEach { color ->
                                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(color))
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = preview.variant.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            SectionDivider()
            SectionHeader("Celebration")

            CelebrationStyle.entries.forEach { style ->
                val isSelected = style == currentCelebration
                val previewColors = CelebrationPalettes.previewColors(style)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { CelebrationManager.setStyle(context, style) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    tonalElevation = if (isSelected) 4.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        previewColors.forEach { color ->
                            Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(color))
                            Spacer(Modifier.width(6.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(text = style.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            androidx.compose.material3.OutlinedButton(
                onClick = { onPreviewCelebration(currentCelebration) },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)
            ) {
                Text("Preview ${currentCelebration.label.lowercase()}")
            }

            SectionDivider()
            SectionHeader("Audio")

            DisplayToggle(
                label = "Sound effects",
                description = "Tap, mistake, win and completion tones",
                checked = soundOn
            ) { SoundManager.setEnabled(context, it) }

            SectionDivider()
            SectionHeader("About")

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAbout() },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "About this app",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text("›", style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
    )
}

@Composable
private fun SectionDivider() {
    Spacer(Modifier.height(20.dp))
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
        thickness = 1.dp
    )
    Spacer(Modifier.height(20.dp))
}
