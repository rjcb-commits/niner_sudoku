package com.ninersudoku.achievements

import com.ninersudoku.R
import com.ninersudoku.game.Difficulty

/**
 * An achievement. The [icon] emoji is the always-available fallback; if [drawableRes] is non-null,
 * the UI prefers rendering the bundled badge image instead.
 */
enum class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val drawableRes: Int? = null
) {
    FIRST_WIN("first_win", "First Win", "Solve any puzzle", "🎉", R.drawable.badge_first_win),
    BEGINNER_MASTER("beginner_master", "Beginner Master", "Win 10 Beginner puzzles", "🥉", R.drawable.badge_beginner_master),
    EASY_MASTER("easy_master", "Easy Master", "Win 10 Easy puzzles", "🥈", R.drawable.badge_easy_master),
    MEDIUM_MASTER("medium_master", "Medium Master", "Win 10 Medium puzzles", "🥇", R.drawable.badge_medium_master),
    HARD_MASTER("hard_master", "Hard Master", "Win 5 Hard puzzles", "💪", R.drawable.badge_hard_master),
    EXPERT_MASTER("expert_master", "Grandmaster", "Win 3 Expert puzzles", "👑", R.drawable.badge_expert_master),
    SPEEDRUN_EASY("speedrun_easy", "Quick Mind", "Win Easy in under 4:00", "⚡", R.drawable.badge_speedrun_easy),
    SPEEDRUN_MEDIUM("speedrun_medium", "Sharp Eye", "Win Medium in under 6:00", "🔥", R.drawable.badge_speedrun_medium),
    HINT_FREE("hint_free", "On Your Own", "Solve a puzzle without hints", "🧠", R.drawable.badge_hint_free),
    PERFECTIONIST("perfectionist", "Perfectionist", "Solve a puzzle with zero mistakes", "⭐", R.drawable.badge_perfectionist),
    FLAWLESS("flawless", "Flawless", "No mistakes AND no hints in one game", "💎", R.drawable.badge_flawless),
    PERSISTENT("persistent", "Devoted", "Win 50 puzzles total", "🏆", R.drawable.badge_persistent),
    SPEED_DEMON("speed_demon", "Speed Demon", "Win any Speed mode game", "🏎️", R.drawable.badge_speed_demon),
    DEMOLISHER("demolisher", "Demolisher", "Win Speed in under 2:00", "💥", R.drawable.badge_demolisher),
    CAGEY("cagey", "Cagey", "Win your first Killer puzzle", "🧩", R.drawable.badge_cagey),
    CAGE_MASTER("cage_master", "Cage Master", "Win 10 Killer puzzles", "🔐", R.drawable.badge_cage_master);

    companion object {
        fun byId(id: String): Achievement? = values().firstOrNull { it.id == id }
    }
}
