package com.ninersudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ninersudoku.achievements.AchievementManager
import com.ninersudoku.daily.DailyManager
import com.ninersudoku.onboarding.OnboardingManager
import com.ninersudoku.persistence.SaveManager
import com.ninersudoku.prefs.DisplayPreferences
import com.ninersudoku.sound.SoundManager
import com.ninersudoku.stats.StatsManager
import com.ninersudoku.ui.SudokuApp
import com.ninersudoku.ui.celebration.CelebrationManager
import com.ninersudoku.ui.theme.SudokuAppTheme
import com.ninersudoku.ui.theme.ThemeManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        ThemeManager.init(this)
        DisplayPreferences.init(this)
        CelebrationManager.init(this)
        StatsManager.init(this)
        SaveManager.init(this)
        AchievementManager.init(this)
        DailyManager.init(this)
        SoundManager.init(this)
        OnboardingManager.init(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SudokuAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SudokuApp()
                }
            }
        }
    }

    override fun onDestroy() {
        // Release ToneGenerator + sequencing scope. Without this, repeatedly
        // opening + closing the app accumulates audio HAL handles on low-end
        // devices. Guard with isFinishing so we don't release on rotation.
        if (isFinishing) {
            SoundManager.release()
        }
        super.onDestroy()
    }
}
