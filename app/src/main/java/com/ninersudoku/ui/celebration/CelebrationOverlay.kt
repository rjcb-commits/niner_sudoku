package com.ninersudoku.ui.celebration

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.ninersudoku.achievements.Achievement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

@Composable
private fun TitleBlock(title: String, subtitle: String, badge: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )
        if (badge != null) {
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.tertiary
            ) {
                Text(
                    text = badge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.onTertiary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private data class CelebrationParticle(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var gravity: Float,
    var drag: Float,
    var rot: Float, var rotSpeed: Float,
    var w: Float, var h: Float,
    var alpha: Float, var alphaDecay: Float,
    var life: Float,
    val color: Color,
    val isCircle: Boolean = false,
    val emoji: String? = null
)

private val emojiChars = listOf("🎉", "✨", "💫", "🎊", "⭐", "🙌", "🎯", "🔥")

private fun createConfettiBurst(count: Int, rng: Random): List<CelebrationParticle> =
    List(count) {
        val angle = rng.nextFloat() * PI.toFloat() * 2f
        val speed = 0.6f + rng.nextFloat() * 0.9f
        CelebrationParticle(
            x = 0.4f + rng.nextFloat() * 0.2f,
            y = 0.85f + rng.nextFloat() * 0.1f,
            vx = cos(angle) * speed * 0.5f,
            vy = -(0.8f + rng.nextFloat() * 0.6f),
            gravity = 0.55f, drag = 0.999f,
            rot = rng.nextFloat() * 360f, rotSpeed = (rng.nextFloat() - 0.5f) * 300f,
            w = 0.014f + rng.nextFloat() * 0.014f,
            h = (0.014f + rng.nextFloat() * 0.014f) * (0.35f + rng.nextFloat() * 0.3f),
            alpha = 1f, alphaDecay = 0.06f + rng.nextFloat() * 0.04f,
            life = 9f + rng.nextFloat() * 4f,
            color = CelebrationPalettes.confetti[it % CelebrationPalettes.confetti.size]
        )
    }

private fun createFireworkBurst(cx: Float, cy: Float, count: Int, rng: Random, palette: List<Color>): List<CelebrationParticle> =
    List(count) {
        val angle = (it.toFloat() / count) * PI.toFloat() * 2f + rng.nextFloat() * 0.3f
        val speed = 0.4f + rng.nextFloat() * 0.3f
        CelebrationParticle(
            x = cx, y = cy,
            vx = cos(angle) * speed,
            vy = sin(angle) * speed,
            gravity = 0.6f, drag = 0.955f,
            rot = 0f, rotSpeed = 0f,
            w = 0.006f + rng.nextFloat() * 0.004f,
            h = 0.006f + rng.nextFloat() * 0.004f,
            alpha = 1f, alphaDecay = 0.8f + rng.nextFloat() * 0.4f,
            life = 0.8f + rng.nextFloat() * 0.4f,
            color = palette[rng.nextInt(palette.size)],
            isCircle = true
        )
    }

private fun createConfettiAmbient(count: Int, rng: Random): List<CelebrationParticle> =
    List(count) {
        CelebrationParticle(
            x = rng.nextFloat(), y = -rng.nextFloat() * 0.4f - 0.05f,
            vx = (rng.nextFloat() - 0.5f) * 0.15f,
            vy = 0.25f + rng.nextFloat() * 0.2f,
            gravity = 0.35f, drag = 1f,
            rot = rng.nextFloat() * 360f, rotSpeed = (rng.nextFloat() - 0.5f) * 240f,
            w = 0.018f + rng.nextFloat() * 0.012f,
            h = (0.018f + rng.nextFloat() * 0.012f) * (0.35f + rng.nextFloat() * 0.3f),
            alpha = 0.95f, alphaDecay = 0.0f,
            life = 8f + rng.nextFloat() * 3f,
            color = CelebrationPalettes.confetti[it % CelebrationPalettes.confetti.size]
        )
    }

private fun createBubbleBurst(count: Int, rng: Random): List<CelebrationParticle> =
    List(count) {
        val colors = CelebrationPalettes.bubbles
        CelebrationParticle(
            x = 0.05f + rng.nextFloat() * 0.9f,
            y = 0.85f + rng.nextFloat() * 0.2f,
            vx = (rng.nextFloat() - 0.5f) * 0.1f,
            vy = -(0.1f + rng.nextFloat() * 0.12f),
            gravity = -0.06f, drag = 0.997f,
            rot = 0f, rotSpeed = 0f,
            w = 0.04f + rng.nextFloat() * 0.04f,
            h = 0.04f + rng.nextFloat() * 0.04f,
            alpha = 0.55f, alphaDecay = 0.03f + rng.nextFloat() * 0.02f,
            life = 8f + rng.nextFloat() * 5f,
            color = colors[it % colors.size],
            isCircle = true
        )
    }

private fun createCherryBurst(count: Int, rng: Random): List<CelebrationParticle> =
    List(count) {
        val colors = CelebrationPalettes.cherryBlossoms
        CelebrationParticle(
            x = rng.nextFloat(),
            y = -rng.nextFloat() * 0.3f - 0.05f,
            vx = (rng.nextFloat() - 0.5f) * 0.12f,
            vy = 0.08f + rng.nextFloat() * 0.1f,
            gravity = 0.15f, drag = 0.995f,
            rot = rng.nextFloat() * 360f, rotSpeed = (rng.nextFloat() - 0.5f) * 120f,
            w = 0.016f + rng.nextFloat() * 0.014f,
            h = (0.016f + rng.nextFloat() * 0.014f) * (0.25f + rng.nextFloat() * 0.2f),
            alpha = 0.9f, alphaDecay = 0.02f + rng.nextFloat() * 0.02f,
            life = 10f + rng.nextFloat() * 5f,
            color = colors[it % colors.size]
        )
    }

private fun createEmojiRainBurst(count: Int, rng: Random): List<CelebrationParticle> =
    List(count) {
        CelebrationParticle(
            x = rng.nextFloat(), y = -rng.nextFloat() * 0.3f - 0.05f,
            vx = (rng.nextFloat() - 0.5f) * 0.08f,
            vy = 0.15f + rng.nextFloat() * 0.2f,
            gravity = 0.25f, drag = 0.99f,
            rot = 0f, rotSpeed = (rng.nextFloat() - 0.5f) * 60f,
            w = 0.025f + rng.nextFloat() * 0.015f,
            h = 0.025f + rng.nextFloat() * 0.015f,
            alpha = 1f, alphaDecay = 0.04f + rng.nextFloat() * 0.03f,
            life = 7f + rng.nextFloat() * 4f,
            color = Color.Transparent,
            emoji = emojiChars[rng.nextInt(emojiChars.size)]
        )
    }

private fun burstForStyle(style: CelebrationStyle, count: Int, rng: Random): List<CelebrationParticle> =
    when (style) {
        CelebrationStyle.CONFETTI -> createConfettiBurst(count, rng)
        CelebrationStyle.FIREWORKS -> createFireworkBurst(
            0.3f + rng.nextFloat() * 0.4f, 0.2f + rng.nextFloat() * 0.2f, count * 2, rng, CelebrationPalettes.fireworks
        )
        CelebrationStyle.BUBBLES -> createBubbleBurst(count, rng)
        CelebrationStyle.MINIMAL -> emptyList()
        CelebrationStyle.CHERRY_BLOSSOMS -> createCherryBurst(count, rng)
        CelebrationStyle.EMOJI_RAIN -> createEmojiRainBurst((count * 0.4f).toInt().coerceAtLeast(8), rng)
    }

private fun ambientForStyle(style: CelebrationStyle, count: Int, rng: Random): List<CelebrationParticle> =
    when (style) {
        CelebrationStyle.CONFETTI -> createConfettiAmbient(count, rng)
        CelebrationStyle.FIREWORKS -> emptyList()
        CelebrationStyle.BUBBLES -> createBubbleBurst(count, rng)
        CelebrationStyle.MINIMAL -> emptyList()
        CelebrationStyle.CHERRY_BLOSSOMS -> createCherryBurst(count, rng)
        CelebrationStyle.EMOJI_RAIN -> createEmojiRainBurst((count * 0.3f).toInt().coerceAtLeast(4), rng)
    }

private fun accentForStyle(style: CelebrationStyle, cx: Float, cy: Float, count: Int, rng: Random): List<CelebrationParticle> =
    when (style) {
        CelebrationStyle.CONFETTI -> createFireworkBurst(cx, cy, count, rng, CelebrationPalettes.confetti)
        CelebrationStyle.FIREWORKS -> createFireworkBurst(cx, cy, count * 2, rng, CelebrationPalettes.fireworks)
        CelebrationStyle.BUBBLES -> createBubbleBurst(count / 2, rng)
        CelebrationStyle.MINIMAL -> emptyList()
        CelebrationStyle.CHERRY_BLOSSOMS -> emptyList()
        CelebrationStyle.EMOJI_RAIN -> createEmojiRainBurst((count * 0.3f).toInt().coerceAtLeast(4), rng)
    }

@Composable
fun CelebrationOverlay(
    title: String,
    subtitle: String,
    stats: List<Pair<String, String>>,
    onPrimary: () -> Unit,
    primaryLabel: String = "New game",
    onSecondary: (() -> Unit)? = null,
    secondaryLabel: String = "View board",
    onTertiary: (() -> Unit)? = null,
    tertiaryLabel: String = "Main menu",
    onShare: (() -> Unit)? = null,
    emoji: String = "🎉",
    showParticles: Boolean = true,
    badge: String? = null,
    achievements: List<Achievement> = emptyList(),
    highlightStatLabel: String? = null
) {
    val rng = remember { Random(System.currentTimeMillis()) }
    val view = LocalView.current
    val style by CelebrationManager.currentStyle.collectAsState()

    val particles = remember { ArrayList<CelebrationParticle>(512) }
    var tick by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        if (showParticles) {
            particles.addAll(burstForStyle(style, 110, rng))
            particles.addAll(ambientForStyle(style, 18, rng))
        }
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        if (showParticles) {
            kotlinx.coroutines.delay(150)
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            kotlinx.coroutines.delay(300)
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    LaunchedEffect(Unit) {
        if (!showParticles) return@LaunchedEffect
        kotlinx.coroutines.delay(300)
        particles.addAll(accentForStyle(style, 0.3f + rng.nextFloat() * 0.4f, 0.2f + rng.nextFloat() * 0.3f, 28, rng))
        kotlinx.coroutines.delay(500)
        particles.addAll(accentForStyle(style, 0.2f + rng.nextFloat() * 0.6f, 0.15f + rng.nextFloat() * 0.3f, 28, rng))
        kotlinx.coroutines.delay(600)
        particles.addAll(accentForStyle(style, 0.3f + rng.nextFloat() * 0.4f, 0.25f + rng.nextFloat() * 0.2f, 32, rng))
        val maxLive = 450
        var beat = 0
        while (true) {
            kotlinx.coroutines.delay(180)
            beat++
            if (particles.size < maxLive) particles.addAll(ambientForStyle(style, 14, rng))
            if (beat % 3 == 0 && particles.size < maxLive) {
                particles.addAll(accentForStyle(style, rng.nextFloat() * 0.9f + 0.05f, 0.15f + rng.nextFloat() * 0.35f, 30, rng))
            }
            if (beat % 11 == 0 && particles.size < maxLive) {
                particles.addAll(burstForStyle(style, 80, rng))
            }
        }
    }

    LaunchedEffect(Unit) {
        var prev = withFrameMillis { it }
        while (true) {
            val now = withFrameMillis { it }
            val dt = ((now - prev) / 1000f).coerceAtMost(0.05f)
            prev = now
            var i = particles.size - 1
            while (i >= 0) {
                val p = particles[i]
                p.vy += p.gravity * dt
                p.vx *= p.drag
                p.vy *= p.drag
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.rot += p.rotSpeed * dt
                p.alpha -= p.alphaDecay * dt
                p.life -= dt
                if (p.life <= 0f || p.alpha <= 0f) {
                    val last = particles.size - 1
                    if (i != last) particles[i] = particles[last]
                    particles.removeAt(last)
                }
                i--
            }
            tick = now
        }
    }

    val flashAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        flashAlpha.snapTo(if (showParticles) 0.7f else 0.4f)
        flashAlpha.animateTo(0f, tween(450))
    }

    val emojiScale = remember { Animatable(2.5f) }
    val emojiRotation = remember { Animatable(-15f) }
    LaunchedEffect(Unit) {
        coroutineScope {
            launch { emojiScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 180f)) }
            launch { emojiRotation.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 200f)) }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow_alpha"
    )

    var showTitle by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showButtons by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(150); showTitle = true
        kotlinx.coroutines.delay(150); showStats = true
        kotlinx.coroutines.delay(150); showButtons = true
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val scrim = MaterialTheme.colorScheme.background.copy(
        alpha = if (showParticles) 0.78f else 0.96f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scrim)
            // Swallow any taps that miss the buttons so they don't fall through
            // to the difficulty cards / board behind.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* no-op */ }
    ) {
        // Floating share button — top-right corner.
        if (onShare != null) {
            IconButton(
                onClick = onShare,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share result", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = flashAlpha.value)))

        key(tick) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val sw = size.width; val sh = size.height
                val emojiPaint = android.graphics.Paint().apply {
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                particles.forEach { p ->
                    if (p.alpha <= 0) return@forEach
                    val px = p.x * sw; val py = p.y * sh
                    val pw = p.w * sw; val ph = p.h * sw
                    val a = p.alpha.coerceIn(0f, 1f)
                    if (p.emoji != null) {
                        emojiPaint.textSize = pw * 2.5f
                        emojiPaint.alpha = (a * 255).toInt()
                        drawContext.canvas.nativeCanvas.drawText(p.emoji, px, py, emojiPaint)
                    } else if (p.isCircle) {
                        drawCircle(color = p.color.copy(alpha = a), radius = pw / 2f, center = Offset(px, py))
                    } else {
                        rotate(degrees = p.rot, pivot = Offset(px, py)) {
                            drawRoundRect(
                                color = p.color.copy(alpha = a),
                                topLeft = Offset(px - pw / 2f, py - ph / 2f),
                                size = Size(pw, ph),
                                cornerRadius = CornerRadius(min(pw, ph) * 0.25f)
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            Text(
                text = emoji,
                fontSize = 72.sp,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = emojiScale.value
                        scaleY = emojiScale.value
                        rotationZ = emojiRotation.value
                    }
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(listOf(primaryColor.copy(alpha = glowAlpha), Color.Transparent)),
                            radius = size.maxDimension * 1.2f
                        )
                    }
            )

            Spacer(Modifier.height(16.dp))

            AnimatedVisibility(
                visible = showTitle,
                enter = fadeIn(tween(350)) + slideInVertically(tween(400)) { 60 }
            ) {
                if (showParticles) {
                    TitleBlock(title, subtitle, badge)
                } else {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                            TitleBlock(title, subtitle, badge)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            AnimatedVisibility(
                visible = showStats,
                enter = scaleIn(tween(300), initialScale = 0.85f) + fadeIn(tween(300))
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        stats.forEachIndexed { idx, (label, value) ->
                            if (idx > 0) Spacer(Modifier.height(8.dp))
                            StatRow(
                                label = label,
                                value = value,
                                highlight = (highlightStatLabel != null && label == highlightStatLabel)
                            )
                        }
                    }
                }
            }

            if (achievements.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                AnimatedVisibility(
                    visible = showStats,
                    enter = fadeIn(tween(400)) + slideInVertically(tween(450)) { 60 }
                ) {
                    AchievementsBlock(achievements)
                }
            }

            Spacer(Modifier.height(24.dp))

            AnimatedVisibility(
                visible = showButtons,
                enter = fadeIn(tween(400)) + slideInVertically(tween(450)) { 80 }
            ) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = onPrimary,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(primaryLabel, style = MaterialTheme.typography.labelLarge, fontSize = 16.sp)
                    }
                    if (onTertiary != null) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onTertiary,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(tertiaryLabel, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    if (onSecondary != null) {
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = onSecondary, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                secondaryLabel,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                            )
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, highlight: Boolean) {
    val cs = MaterialTheme.colorScheme
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(highlight) {
        if (highlight) {
            kotlinx.coroutines.delay(450)
            pulse.snapTo(1f)
            pulse.animateTo(0f, tween(1100, easing = LinearEasing))
        }
    }
    val highlightColor = cs.tertiary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = if (highlight) {
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(highlightColor.copy(alpha = 0.16f + pulse.value * 0.18f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        } else Modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (highlight) cs.onPrimaryContainer
                else cs.onPrimaryContainer.copy(alpha = 0.7f),
            fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        if (highlight) {
            Text("✨", fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = if (highlight) highlightColor else cs.onPrimaryContainer,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AchievementsBlock(achievements: List<Achievement>) {
    val view = LocalView.current
    var revealedCount by remember { mutableStateOf(0) }
    LaunchedEffect(achievements) {
        revealedCount = 0
        achievements.forEachIndexed { idx, _ ->
            kotlinx.coroutines.delay(if (idx == 0) 200 else 380)
            revealedCount = idx + 1
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (achievements.size == 1) "Achievement unlocked" else "${achievements.size} achievements unlocked",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.75f),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            achievements.forEachIndexed { idx, a ->
                if (idx > 0) Spacer(Modifier.height(10.dp))
                AchievementRow(achievement = a, visible = idx < revealedCount)
            }
        }
    }
}

@Composable
private fun AchievementRow(achievement: Achievement, visible: Boolean) {
    val scale = remember { Animatable(0.4f) }
    val rowAlpha = remember { Animatable(0f) }
    val glow = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            coroutineScope {
                launch { scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 220f)) }
                launch { rowAlpha.animateTo(1f, tween(280)) }
                launch {
                    glow.snapTo(1f)
                    glow.animateTo(0f, tween(900, easing = LinearEasing))
                }
            }
        }
    }
    val tertiary = MaterialTheme.colorScheme.tertiary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.graphicsLayer { alpha = rowAlpha.value }
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (glow.value > 0f) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(tertiary.copy(alpha = glow.value * 0.55f), Color.Transparent)
                                ),
                                radius = size.maxDimension / 2f
                            )
                        }
                )
            }
            if (achievement.drawableRes != null) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = achievement.drawableRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer {
                            scaleX = scale.value
                            scaleY = scale.value
                        }
                        .shadow(
                            elevation = 5.dp,
                            shape = androidx.compose.foundation.shape.CircleShape,
                            clip = false
                        )
                        .clip(androidx.compose.foundation.shape.CircleShape)
                )
            } else {
                Text(
                    achievement.icon,
                    fontSize = 28.sp,
                    modifier = Modifier.graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    }
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = achievement.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = achievement.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.75f)
            )
        }
    }
}
