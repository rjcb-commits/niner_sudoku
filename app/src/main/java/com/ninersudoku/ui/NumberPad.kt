package com.ninersudoku.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ninersudoku.prefs.DisplayPreferences
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NumberPad(
    enabled: Boolean,
    remainingCounts: IntArray,
    activeDigit: Int?,
    legalDigits: Set<Int>?,  // null = no selection / no constraint info
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val autoRuleOut by DisplayPreferences.autoRuleOut.collectAsState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (n in 1..9) {
            val remaining = remainingCounts[n]
            val sated = remaining == 0
            // A digit is "illegal" for the currently-selected cell if legalDigits
            // tells us so — but we only style it that way when the user has opted in
            // via the auto-rule-out preference (default off). Without the toggle, the
            // player has to do the row/col/box deduction themselves.
            val isIllegal = autoRuleOut && legalDigits != null && n !in legalDigits
            val effectivelyEnabled = enabled && !sated
            val isActive = activeDigit == n

            // Tap-pulse: bump pressTick on tap, scale animates briefly
            var pressTick by remember { mutableIntStateOf(0) }
            val pressedNow = pressTick % 2 == 1
            val scale by animateFloatAsState(
                targetValue = if (pressedNow) 0.92f else 1f,
                animationSpec = tween(durationMillis = 90),
                finishedListener = { if (pressedNow) pressTick++ },
                label = "pad_scale_$n"
            )

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val a11yDescription = when {
                    sated -> "Digit $n, placed 9 of 9"
                    !enabled -> "Digit $n"
                    else -> "Digit $n, $remaining remaining"
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .scale(scale)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isActive && enabled -> cs.primary.copy(alpha = 0.22f)
                                sated -> cs.surfaceVariant.copy(alpha = 0.5f)
                                isIllegal && enabled -> cs.surfaceVariant.copy(alpha = 0.45f)
                                enabled -> cs.primary.copy(alpha = 0.10f)
                                else -> cs.surfaceVariant
                            }
                        )
                        .let { mod ->
                            if (isActive && enabled) {
                                mod.border(2.dp, cs.primary, RoundedCornerShape(8.dp))
                            } else mod
                        }
                        .semantics {
                            contentDescription = a11yDescription
                            role = Role.Button
                        }
                        .combinedClickable(
                            enabled = effectivelyEnabled,
                            onClick = { pressTick++; onTap(n) },
                            onLongClick = { pressTick++; onLongPress(n) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = n.toString(),
                        color = when {
                            sated -> cs.onSurfaceVariant.copy(alpha = 0.35f)
                            isIllegal && enabled -> cs.onSurfaceVariant.copy(alpha = 0.42f)
                            enabled -> cs.primary
                            else -> cs.onSurfaceVariant.copy(alpha = 0.6f)
                        },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = if (sated) "✓" else remaining.toString(),
                    color = cs.onBackground.copy(alpha = if (sated) 0.45f else 0.55f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
