package com.ninersudoku.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.ninersudoku.prefs.DisplayPreferences
import com.ninersudoku.prefs.ThemeMode

@Composable
fun SudokuAppTheme(
    content: @Composable () -> Unit
) {
    val themeVariant by ThemeManager.currentTheme.collectAsState()
    val themeModePref by DisplayPreferences.themeMode.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeModePref) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when (themeVariant) {
        AppThemeVariant.EMBER -> if (darkTheme) EmberDark else EmberLight
        AppThemeVariant.ELECTRIC -> if (darkTheme) ElectricDark else ElectricLight
        AppThemeVariant.NEON_NIGHT -> if (darkTheme) NeonNightDark else NeonNightLight
        AppThemeVariant.CITRUS -> if (darkTheme) CitrusDark else CitrusLight
        AppThemeVariant.HYPERDRIVE -> if (darkTheme) HyperdriveDark else HyperdriveLight
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
