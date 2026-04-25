package com.ninersudoku.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Five color schemes ported from StupidSmall — each with light + dark variants.
 * The user picks one via the settings sheet; choice persists across launches.
 */
enum class AppThemeVariant(val label: String) {
    EMBER("Ember"),
    ELECTRIC("Electric"),
    NEON_NIGHT("Neon Night"),
    CITRUS("Citrus Punch"),
    HYPERDRIVE("Hyperdrive");
}

// EMBER
val EmberLight = lightColorScheme(
    primary = Color(0xFFE53935), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFCDD2), onPrimaryContainer = Color(0xFF410003),
    secondary = Color(0xFFFF6D00), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE0B2), onSecondaryContainer = Color(0xFF311400),
    tertiary = Color(0xFFFFAB00), onTertiary = Color(0xFF1F1300),
    tertiaryContainer = Color(0xFFFFECB3), onTertiaryContainer = Color(0xFF1F1300),
    background = Color(0xFFFFFBF8), onBackground = Color(0xFF1C1B1A),
    surface = Color(0xFFFFFBF8), onSurface = Color(0xFF1C1B1A),
    surfaceVariant = Color(0xFFF5E8E4), onSurfaceVariant = Color(0xFF534340),
    outline = Color(0xFF857371)
)

val EmberDark = darkColorScheme(
    primary = Color(0xFFFF8A80), onPrimary = Color(0xFF5F1412),
    primaryContainer = Color(0xFF8C1D18), onPrimaryContainer = Color(0xFFFFDAD5),
    secondary = Color(0xFFFFAB40), onSecondary = Color(0xFF422C00),
    secondaryContainer = Color(0xFF5E3F00), onSecondaryContainer = Color(0xFFFFE0B2),
    tertiary = Color(0xFFFFD54F), onTertiary = Color(0xFF3A2F00),
    tertiaryContainer = Color(0xFF544500), onTertiaryContainer = Color(0xFFFFECB3),
    background = Color(0xFF1A1110), onBackground = Color(0xFFF5DDDA),
    surface = Color(0xFF201614), onSurface = Color(0xFFF5DDDA),
    surfaceVariant = Color(0xFF332523), onSurfaceVariant = Color(0xFFDBC2BE),
    outline = Color(0xFFA38C89)
)

// ELECTRIC
val ElectricLight = lightColorScheme(
    primary = Color(0xFF2962FF), onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF), onPrimaryContainer = Color(0xFF001A49),
    secondary = Color(0xFF00BFA5), onSecondary = Color.White,
    secondaryContainer = Color(0xFFA7FFEB), onSecondaryContainer = Color(0xFF002018),
    tertiary = Color(0xFFFF6D00), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE0B2), onTertiaryContainer = Color(0xFF311400),
    background = Color(0xFFF6F9FF), onBackground = Color(0xFF181C22),
    surface = Color(0xFFF6F9FF), onSurface = Color(0xFF181C22),
    surfaceVariant = Color(0xFFDDE4F0), onSurfaceVariant = Color(0xFF414750),
    outline = Color(0xFF717880)
)

val ElectricDark = darkColorScheme(
    primary = Color(0xFF82B1FF), onPrimary = Color(0xFF002D6E),
    primaryContainer = Color(0xFF004098), onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFF64FFDA), onSecondary = Color(0xFF00382B),
    secondaryContainer = Color(0xFF00503E), onSecondaryContainer = Color(0xFFA7FFEB),
    tertiary = Color(0xFFFFAB40), onTertiary = Color(0xFF422C00),
    tertiaryContainer = Color(0xFF5E3F00), onTertiaryContainer = Color(0xFFFFE0B2),
    background = Color(0xFF0D1117), onBackground = Color(0xFFE0E6EE),
    surface = Color(0xFF131920), onSurface = Color(0xFFE0E6EE),
    surfaceVariant = Color(0xFF252D38), onSurfaceVariant = Color(0xFFC0C8D4),
    outline = Color(0xFF8A929E)
)

// NEON NIGHT
val NeonNightLight = lightColorScheme(
    primary = Color(0xFFAA00FF), onPrimary = Color.White,
    primaryContainer = Color(0xFFF3E0FF), onPrimaryContainer = Color(0xFF24005A),
    secondary = Color(0xFFFF1744), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD9), onSecondaryContainer = Color(0xFF410008),
    tertiary = Color(0xFF00E5FF), onTertiary = Color(0xFF001F26),
    tertiaryContainer = Color(0xFFB0F0FF), onTertiaryContainer = Color(0xFF001F26),
    background = Color(0xFFFCF5FF), onBackground = Color(0xFF1E1A20),
    surface = Color(0xFFFCF5FF), onSurface = Color(0xFF1E1A20),
    surfaceVariant = Color(0xFFEDE0F4), onSurfaceVariant = Color(0xFF4B454F),
    outline = Color(0xFF7C757F)
)

val NeonNightDark = darkColorScheme(
    primary = Color(0xFFE040FB), onPrimary = Color(0xFF3A0066),
    primaryContainer = Color(0xFF5600A5), onPrimaryContainer = Color(0xFFF3E0FF),
    secondary = Color(0xFFFF5272), onSecondary = Color(0xFF680014),
    secondaryContainer = Color(0xFF8E001F), onSecondaryContainer = Color(0xFFFFDAD9),
    tertiary = Color(0xFF18FFFF), onTertiary = Color(0xFF003038),
    tertiaryContainer = Color(0xFF004850), onTertiaryContainer = Color(0xFFB0F0FF),
    background = Color(0xFF0E0A12), onBackground = Color(0xFFEDE0F4),
    surface = Color(0xFF150F1A), onSurface = Color(0xFFEDE0F4),
    surfaceVariant = Color(0xFF2C2530), onSurfaceVariant = Color(0xFFD0C4DA),
    outline = Color(0xFF988EA2)
)

// CITRUS
val CitrusLight = lightColorScheme(
    primary = Color(0xFF00C853), onPrimary = Color.White,
    primaryContainer = Color(0xFFB9F6CA), onPrimaryContainer = Color(0xFF002108),
    secondary = Color(0xFFFFAB00), onSecondary = Color(0xFF1F1300),
    secondaryContainer = Color(0xFFFFECB3), onSecondaryContainer = Color(0xFF1F1300),
    tertiary = Color(0xFFFF6D00), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE0B2), onTertiaryContainer = Color(0xFF311400),
    background = Color(0xFFF7FFF5), onBackground = Color(0xFF1A1E18),
    surface = Color(0xFFF7FFF5), onSurface = Color(0xFF1A1E18),
    surfaceVariant = Color(0xFFDEEDD8), onSurfaceVariant = Color(0xFF404940),
    outline = Color(0xFF707970)
)

val CitrusDark = darkColorScheme(
    primary = Color(0xFF69F0AE), onPrimary = Color(0xFF003916),
    primaryContainer = Color(0xFF005222), onPrimaryContainer = Color(0xFFB9F6CA),
    secondary = Color(0xFFFFD740), onSecondary = Color(0xFF3A2F00),
    secondaryContainer = Color(0xFF544500), onSecondaryContainer = Color(0xFFFFECB3),
    tertiary = Color(0xFFFFAB40), onTertiary = Color(0xFF422C00),
    tertiaryContainer = Color(0xFF5E3F00), onTertiaryContainer = Color(0xFFFFE0B2),
    background = Color(0xFF0E1410), onBackground = Color(0xFFDDE8D8),
    surface = Color(0xFF141A16), onSurface = Color(0xFFDDE8D8),
    surfaceVariant = Color(0xFF242E26), onSurfaceVariant = Color(0xFFBEC9BD),
    outline = Color(0xFF889388)
)

// HYPERDRIVE
val HyperdriveLight = lightColorScheme(
    primary = Color(0xFFD50000), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD5), onPrimaryContainer = Color(0xFF410001),
    secondary = Color(0xFF212121), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E0E0), onSecondaryContainer = Color(0xFF1A1A1A),
    tertiary = Color(0xFFFF6D00), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE0B2), onTertiaryContainer = Color(0xFF311400),
    background = Color(0xFFFAFAFA), onBackground = Color(0xFF121212),
    surface = Color(0xFFFAFAFA), onSurface = Color(0xFF121212),
    surfaceVariant = Color(0xFFECECEC), onSurfaceVariant = Color(0xFF444444),
    outline = Color(0xFF767676)
)

val HyperdriveDark = darkColorScheme(
    primary = Color(0xFFFF5252), onPrimary = Color(0xFF5F0000),
    primaryContainer = Color(0xFF930000), onPrimaryContainer = Color(0xFFFFDAD5),
    secondary = Color(0xFFBDBDBD), onSecondary = Color(0xFF1A1A1A),
    secondaryContainer = Color(0xFF424242), onSecondaryContainer = Color(0xFFE0E0E0),
    tertiary = Color(0xFFFFAB40), onTertiary = Color(0xFF422C00),
    tertiaryContainer = Color(0xFF5E3F00), onTertiaryContainer = Color(0xFFFFE0B2),
    background = Color(0xFF0A0A0A), onBackground = Color(0xFFEEEEEE),
    surface = Color(0xFF121212), onSurface = Color(0xFFEEEEEE),
    surfaceVariant = Color(0xFF1E1E1E), onSurfaceVariant = Color(0xFFCCCCCC),
    outline = Color(0xFF888888)
)
