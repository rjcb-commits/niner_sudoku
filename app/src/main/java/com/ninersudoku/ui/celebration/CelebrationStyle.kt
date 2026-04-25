package com.ninersudoku.ui.celebration

import androidx.compose.ui.graphics.Color

enum class CelebrationStyle(val label: String) {
    CONFETTI("Confetti"),
    FIREWORKS("Fireworks"),
    BUBBLES("Bubbles"),
    MINIMAL("Minimal"),
    CHERRY_BLOSSOMS("Cherry Blossoms"),
    EMOJI_RAIN("Emoji Rain")
}

object CelebrationPalettes {
    val confetti = listOf(
        Color(0xFFE8705A), Color(0xFFFFB4A2), Color(0xFFF9A825), Color(0xFFFF8F00),
        Color(0xFF5C6BC0), Color(0xFF26A69A), Color(0xFF80CBC4), Color(0xFFF48FB1)
    )

    val fireworks = listOf(
        Color(0xFFFF1744), Color(0xFF2979FF), Color(0xFF00E676),
        Color(0xFFFFEA00), Color(0xFFD500F9), Color(0xFFFF9100)
    )

    val bubbles = listOf(
        Color(0x66B3E5FC), Color(0x66C8E6C9), Color(0x66F8BBD0),
        Color(0x66E1BEE7), Color(0x66FFF9C4)
    )

    val cherryBlossoms = listOf(
        Color(0xFFF8BBD0), Color(0xFFFCE4EC), Color(0xFFF48FB1),
        Color(0xFFFFFFFF), Color(0xFFFFCDD2)
    )

    val emojiRain = listOf(
        Color(0xFFFFD700), Color(0xFFFF6B6B), Color(0xFFFF8F00),
        Color(0xFF4CAF50), Color(0xFFF44336), Color(0xFF2196F3)
    )

    fun previewColors(style: CelebrationStyle): List<Color> = when (style) {
        CelebrationStyle.CONFETTI -> listOf(confetti[0], confetti[2], confetti[4])
        CelebrationStyle.FIREWORKS -> listOf(fireworks[0], fireworks[1], fireworks[2])
        CelebrationStyle.BUBBLES -> listOf(
            Color(0xFFB3E5FC), Color(0xFFC8E6C9), Color(0xFFF8BBD0)
        )
        CelebrationStyle.MINIMAL -> listOf(
            Color(0xFFBDBDBD), Color(0xFF9E9E9E), Color(0xFF757575)
        )
        CelebrationStyle.CHERRY_BLOSSOMS -> listOf(cherryBlossoms[0], cherryBlossoms[2], cherryBlossoms[3])
        CelebrationStyle.EMOJI_RAIN -> listOf(emojiRain[0], emojiRain[1], emojiRain[2])
    }
}
