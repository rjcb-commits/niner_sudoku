# Niner ProGuard / R8 rules.
#
# Compose, AndroidX, and Kotlin all ship their own consumer rules in their AARs,
# so the defaults in `proguard-android-optimize.txt` already handle the bulk of
# what's needed. The rules below cover the app-specific surfaces that R8 can't
# infer on its own.

# ── Reflection on enum names ──────────────────────────────────────────────────
# Persistence reads back GameMode + ThemeMode + Difficulty by name via
# Enum.valueOf(...). R8 must keep the enum names + valueOf intact.
-keepclassmembers enum com.ninersudoku.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── kotlinx.serialization (not used today, but cheap to keep safe) ────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# ── org.json — used by StatsManager.serialize/parse and AchievementManager ────
# org.json is part of Android, so we just need to suppress notes about reflection.
-dontwarn org.json.**

# ── Compose: stable lambdas, navigation, runtime ──────────────────────────────
# Most of these are already in Compose's consumer rules, but pin them anyway:
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.tooling.preview.** { *; }
-dontwarn androidx.compose.**

# ── Kotlin coroutines: keep continuation classes for stack traces ─────────────
-keep class kotlin.coroutines.Continuation
-dontwarn kotlinx.coroutines.**

# ── Crash readability: keep source file names + line numbers ──────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── ViewModel: AndroidViewModel(Application) ctor needs to remain accessible ──
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(android.app.Application);
}
