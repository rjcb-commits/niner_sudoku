package com.ninersudoku.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ninersudoku.onboarding.OnboardingManager
import com.ninersudoku.viewmodel.GameViewModel
import com.ninersudoku.viewmodel.Screen

@Composable
fun SudokuApp(viewModel: GameViewModel = viewModel()) {
    val seenOnboarding by OnboardingManager.seen.collectAsState()
    val context = LocalContext.current

    if (!seenOnboarding) {
        OnboardingScreen(onDone = { OnboardingManager.markSeen(context) })
        return
    }

    when (viewModel.state.screen) {
        Screen.DIFFICULTY -> DifficultyScreen(
            onPick = { difficulty, mode -> viewModel.newGame(difficulty, mode) },
            onResume = viewModel::resumeSavedGame,
            onPlayDaily = viewModel::startDailyGame,
            onOpenStats = viewModel::goToStats,
            onOpenAbout = viewModel::goToAbout
        )
        Screen.GAME -> GameScreen(viewModel = viewModel)
        Screen.STATS -> StatsScreen(onBack = viewModel::backToDifficulty)
        Screen.ABOUT -> AboutScreen(onBack = viewModel::backToDifficulty)
    }
}
