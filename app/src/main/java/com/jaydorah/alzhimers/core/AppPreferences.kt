package com.jaydorah.alzhimers.core

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.app.LocaleManager
import java.time.LocalDate
import java.util.Locale

enum class ThemePref { System, Light, Dark }

/** Streak after playing on [today], given the previous play date and streak. */
internal fun nextStreak(last: LocalDate?, current: Int, today: LocalDate): Int = when (last) {
    today -> current.coerceAtLeast(1)
    today.minusDays(1) -> current + 1
    else -> 1
}

data class LanguageOption(val code: String, val name: String)

val LanguageOptions = listOf(
    LanguageOption("system", "Follow system"),
    LanguageOption("en", "English"),
    LanguageOption("de", "Deutsch"),
    LanguageOption("he", "עברית"),
    LanguageOption("ar", "العربية"),
    LanguageOption("tr", "Türkçe"),
    LanguageOption("af", "Afrikaans"),
    LanguageOption("hi", "हिन्दी"),
    LanguageOption("zh-CN", "简体中文"),
    LanguageOption("es", "Español"),
    LanguageOption("fr", "Français"),
)

/**
 * Single source of truth for app language.
 * Android 13+: stored in LocaleManager (system-persisted, shows in system settings,
 * applies without manual recreation). Older: stored in AppPreferences and applied
 * via createConfigurationContext + recreate().
 */
object Locales {
    const val SYSTEM = "system"

    fun current(context: Context): String {
        if (Build.VERSION.SDK_INT >= 33) {
            val tags = context.getSystemService(LocaleManager::class.java).applicationLocales
            if (tags.isEmpty) return SYSTEM
            val tag = tags[0].toLanguageTag()
            // Some Android versions report Hebrew with the legacy code "iw".
            return if (tag == "iw" || tag.startsWith("iw-")) "he" + tag.removePrefix("iw") else tag
        }
        return AppPreferences.get(context).languageCode
    }

    fun set(activity: Activity, code: String) {
        if (Build.VERSION.SDK_INT >= 33) {
            val manager = activity.getSystemService(LocaleManager::class.java)
            manager.applicationLocales =
                if (code == SYSTEM) android.os.LocaleList.getEmptyLocaleList()
                else android.os.LocaleList.forLanguageTags(code)
        } else {
            AppPreferences.get(activity).languageCode = code
            activity.recreate()
        }
    }

    fun wrap(context: Context, code: String): Context {
        if (code == SYSTEM || code.isEmpty() || Build.VERSION.SDK_INT >= 33) return context
        val locale = Locale.forLanguageTag(code)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // --- Appearance ---

    var themePref: ThemePref
        get() = runCatching {
            ThemePref.valueOf(prefs.getString(KEY_THEME, ThemePref.System.name) ?: ThemePref.System.name)
        }.getOrDefault(ThemePref.System)
        set(value) {
            prefs.edit().putString(KEY_THEME, value.name).apply()
        }

    var fontScale: Float
        get() = prefs.getFloat(KEY_FONT_SCALE, 1f).coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)
        set(value) {
            prefs.edit().putFloat(KEY_FONT_SCALE, value.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)).apply()
        }

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_NOTIFICATIONS, value).apply()
        }

    var languageCode: String
        get() = prefs.getString(KEY_LANGUAGE, "system") ?: "system"
        set(value) {
            prefs.edit().putString(KEY_LANGUAGE, value).apply()
        }

    // --- Streak ---

    fun streakForDisplay(): Int {
        val last = prefs.getString(KEY_LAST_PLAY_DATE, "") ?: ""
        if (last.isEmpty()) return 0
        val lastDate = runCatching { LocalDate.parse(last) }.getOrNull() ?: return 0
        val today = LocalDate.now()
        return when {
            lastDate == today || lastDate == today.minusDays(1) -> prefs.getInt(KEY_STREAK, 0)
            else -> 0
        }
    }

    /** Marks today as played; bumps or resets the streak. Returns the streak after update. */
    fun markPlayedToday(): Int {
        val today = LocalDate.now()
        val todayStr = today.toString()
        val last = prefs.getString(KEY_LAST_PLAY_DATE, "") ?: ""
        val lastDate = runCatching { LocalDate.parse(last.takeIf { it.isNotEmpty() }) }.getOrNull()
        val newStreak = nextStreak(lastDate, prefs.getInt(KEY_STREAK, 0), today)
        if (lastDate == today) {
            // Already counted today; only repair a zero streak, don't bump days active again.
            if (newStreak != prefs.getInt(KEY_STREAK, 0)) prefs.edit().putInt(KEY_STREAK, newStreak).apply()
            return newStreak
        }
        prefs.edit()
            .putString(KEY_LAST_PLAY_DATE, todayStr)
            .putInt(KEY_STREAK, newStreak)
            .putInt(KEY_DAYS_ACTIVE, prefs.getInt(KEY_DAYS_ACTIVE, 0) + 1)
            .apply()
        return newStreak
    }

    fun daysActive(): Int = prefs.getInt(KEY_DAYS_ACTIVE, 0)

    fun gamesPlayed(): Int = prefs.getInt(KEY_GAMES_PLAYED, 0)

    fun incrementGamesPlayed() {
        prefs.edit().putInt(KEY_GAMES_PLAYED, prefs.getInt(KEY_GAMES_PLAYED, 0) + 1).apply()
    }

    // --- Scores ---

    fun bestScore(game: String): Int = prefs.getInt(keyBest(game), 0)

    fun hasBest(game: String): Boolean = prefs.contains(keyBest(game))

    fun recordScore(game: String, score: Int) {
        val key = keyBest(game)
        val existing = if (prefs.contains(key)) prefs.getInt(key, 0) else null
        val higher = ScoreKeys.higherIsBetter[game] ?: true
        val better = when {
            existing == null -> true
            higher -> score > existing
            else -> score < existing
        }
        if (better) {
            prefs.edit().putInt(key, score).apply()
        }
    }

    fun recordPlayedAndScore(game: String, score: Int? = null) {
        markPlayedToday()
        incrementGamesPlayed()
        prefs.edit().putInt(keyPlays(game), playCount(game) + 1).apply()
        if (score != null) recordScore(game, score)
    }

    /** How many times [game] has been finished. Used for games without a numeric best score. */
    fun playCount(game: String): Int = prefs.getInt(keyPlays(game), 0)

    fun playedToday(): Boolean = prefs.getString(KEY_LAST_PLAY_DATE, "") == LocalDate.now().toString()

    private fun keyBest(game: String) = "best_$game"

    private fun keyPlays(game: String) = "plays_$game"

    companion object {
        const val PREFS_NAME = "app_prefs"
        const val MIN_FONT_SCALE = 0.8f
        const val MAX_FONT_SCALE = 1.4f

        private const val KEY_THEME = "theme_pref"
        private const val KEY_FONT_SCALE = "font_scale"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_LANGUAGE = "language_code"
        private const val KEY_STREAK = "streak_count"
        private const val KEY_LAST_PLAY_DATE = "last_play_date"
        private const val KEY_DAYS_ACTIVE = "days_active_total"
        private const val KEY_GAMES_PLAYED = "games_played_total"

        @Volatile
        private var instance: AppPreferences? = null

        fun get(context: Context): AppPreferences =
            instance ?: synchronized(this) {
                instance ?: AppPreferences(context).also { instance = it }
            }
    }
}
