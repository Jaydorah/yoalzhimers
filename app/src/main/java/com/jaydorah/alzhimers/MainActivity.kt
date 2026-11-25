package com.jaydorah.alzhimers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.jaydorah.alzhimers.core.AppPreferences
import com.jaydorah.alzhimers.core.Locales
import com.jaydorah.alzhimers.core.ThemePref
import com.jaydorah.alzhimers.ui.theme.YoAlzhimersTheme

private val AppScreenByName = listOf(
    AppScreen.Home,
    AppScreen.Games,
    AppScreen.Settings,
    AppScreen.MentalMath,
    AppScreen.Copycat,
    AppScreen.WordHunt,
    AppScreen.NumberMemory,
    AppScreen.Chimp,
    AppScreen.LightsOut,
    AppScreen.ColorMatch,
    AppScreen.KimsSack,
    AppScreen.ShortTerm,
    AppScreen.Schulte,
    AppScreen.Percentages,
).associateBy { it::class.simpleName ?: it.toString() }

private val AppScreenSaver = Saver<AppScreen, String>(
    save = { it::class.simpleName ?: AppScreen.Home::class.simpleName!! },
    restore = { name -> AppScreenByName[name] ?: AppScreen.Home },
)

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(Locales.wrap(newBase, Locales.current(newBase)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = AppPreferences.get(this)

        setContent {
            val systemDark = isSystemInDarkTheme()
            var themePref by rememberSaveable { mutableStateOf(prefs.themePref) }
            var fontScale by rememberSaveable { mutableFloatStateOf(prefs.fontScale) }
            var notificationsEnabled by rememberSaveable { mutableStateOf(prefs.notificationsEnabled) }
            // Not saveable: a locale change recreates the activity, and the fresh value must win
            // over a stale saved one (e.g. language changed from system settings).
            var languageCode by remember { mutableStateOf(Locales.current(this@MainActivity)) }

            var screen by rememberSaveable(stateSaver = AppScreenSaver) {
                mutableStateOf<AppScreen>(AppScreen.Home)
            }

            val isDark = when (themePref) {
                ThemePref.System -> systemDark
                ThemePref.Light -> false
                ThemePref.Dark -> true
            }

            YoAlzhimersTheme(darkTheme = isDark, fontScale = fontScale) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppRoot(
                        screen = screen,
                        themePref = themePref,
                        fontScale = fontScale,
                        notificationsEnabled = notificationsEnabled,
                        languageCode = languageCode,
                        onSetTheme = { pref ->
                            themePref = pref
                            prefs.themePref = pref
                        },
                        onSetFontScale = { scale ->
                            fontScale = scale
                            prefs.fontScale = scale
                        },
                        onSetNotifications = { enabled ->
                            notificationsEnabled = enabled
                            prefs.notificationsEnabled = enabled
                        },
                        onSetLanguage = { code ->
                            if (code != languageCode) {
                                languageCode = code
                                Locales.set(this@MainActivity, code)
                            }
                        },
                        onNavigate = { screen = it },
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}
