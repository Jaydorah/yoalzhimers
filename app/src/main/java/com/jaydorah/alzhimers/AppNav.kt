package com.jaydorah.alzhimers

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.jaydorah.alzhimers.R
import com.jaydorah.alzhimers.core.ThemePref
import com.jaydorah.alzhimers.games.chimp.ChimpScreen
import com.jaydorah.alzhimers.games.colormatch.ColorMatchScreen
import com.jaydorah.alzhimers.games.copycat.CopycatScreen
import com.jaydorah.alzhimers.games.lightsout.LightsOutScreen
import com.jaydorah.alzhimers.games.mentalmath.MentalMathScreen
import com.jaydorah.alzhimers.games.numbermemory.NumberMemoryScreen
import com.jaydorah.alzhimers.games.percentages.PercentagesScreen
import com.jaydorah.alzhimers.games.kimsack.KimsSackScreen
import com.jaydorah.alzhimers.games.shortterm.ShortTermScreen
import com.jaydorah.alzhimers.games.schulte.SchulteScreen
import com.jaydorah.alzhimers.games.wordhunt.WordHuntScreen
import com.jaydorah.alzhimers.home.GamesScreen
import com.jaydorah.alzhimers.home.HomeScreen
import com.jaydorah.alzhimers.settings.SettingsScreen

sealed interface AppScreen {
    data object Home : AppScreen
    data object Games : AppScreen
    data object Settings : AppScreen
    data object MentalMath : AppScreen
    data object Copycat : AppScreen
    data object WordHunt : AppScreen
    data object NumberMemory : AppScreen
    data object Chimp : AppScreen
    data object LightsOut : AppScreen
    data object ColorMatch : AppScreen
    data object KimsSack : AppScreen
    data object ShortTerm : AppScreen
    data object Schulte : AppScreen
    data object Percentages : AppScreen
}

private sealed interface RootTab {
    data object Home : RootTab
    data object Games : RootTab
    data object Settings : RootTab
}

private fun rootTabOf(screen: AppScreen): RootTab = when (screen) {
    AppScreen.MentalMath, AppScreen.Copycat, AppScreen.WordHunt,
    AppScreen.NumberMemory, AppScreen.Chimp, AppScreen.LightsOut,
    AppScreen.ColorMatch, AppScreen.KimsSack, AppScreen.ShortTerm,
    AppScreen.Schulte, AppScreen.Percentages,
    -> RootTab.Games

    AppScreen.Games -> RootTab.Games
    AppScreen.Settings -> RootTab.Settings
    else -> RootTab.Home
}

private data class NavItem(
    val tab: RootTab,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun AppRoot(
    screen: AppScreen,
    themePref: ThemePref,
    fontScale: Float,
    notificationsEnabled: Boolean,
    languageCode: String,
    onSetTheme: (ThemePref) -> Unit,
    onSetFontScale: (Float) -> Unit,
    onSetNotifications: (Boolean) -> Unit,
    onSetLanguage: (String) -> Unit,
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isGame = when (screen) {
        AppScreen.Home, AppScreen.Games, AppScreen.Settings -> false
        else -> true
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (!isGame) {
                val current = rootTabOf(screen)
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    val items = listOf(
                        NavItem(RootTab.Home, stringResource(R.string.nav_home), Icons.Default.Home),
                        NavItem(RootTab.Games, stringResource(R.string.nav_games), Icons.AutoMirrored.Filled.List),
                        NavItem(RootTab.Settings, stringResource(R.string.nav_settings), Icons.Default.Settings),
                    )
                    items.forEach { item ->
                        NavigationBarItem(
                            selected = current == item.tab,
                            onClick = {
                                val target = when (item.tab) {
                                    RootTab.Home -> AppScreen.Home
                                    RootTab.Games -> AppScreen.Games
                                    RootTab.Settings -> AppScreen.Settings
                                }
                                onNavigate(target)
                            },
                            icon = {
                                Icon(item.icon, contentDescription = item.label)
                            },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            AppNav(
                screen = screen,
                themePref = themePref,
                fontScale = fontScale,
                notificationsEnabled = notificationsEnabled,
                languageCode = languageCode,
                onSetTheme = onSetTheme,
                onSetFontScale = onSetFontScale,
                onSetNotifications = onSetNotifications,
                onSetLanguage = onSetLanguage,
                onNavigate = onNavigate,
            )
        }
    }
}

@Composable
fun AppNav(
    screen: AppScreen,
    themePref: ThemePref,
    fontScale: Float,
    notificationsEnabled: Boolean,
    languageCode: String,
    onSetTheme: (ThemePref) -> Unit,
    onSetFontScale: (Float) -> Unit,
    onSetNotifications: (Boolean) -> Unit,
    onSetLanguage: (String) -> Unit,
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (screen) {
        AppScreen.Home, AppScreen.Games, AppScreen.Settings -> {
            val tabs = listOf(AppScreen.Home, AppScreen.Games, AppScreen.Settings)
            val pagerState = rememberPagerState(initialPage = tabs.indexOf(screen)) { tabs.size }
            // Page we are animating toward from an external nav tap (bottom bar / back).
            // -1 when no programmatic scroll is in flight, so swipe settles aren't swallowed
            // and intermediate settles can't fight the animation.
            var animatingTo by remember { mutableIntStateOf(-1) }

            LaunchedEffect(pagerState.settledPage) {
                if (animatingTo != -1 || pagerState.isScrollInProgress) return@LaunchedEffect
                val target = tabs[pagerState.settledPage]
                if (screen != target) onNavigate(target)
            }
            LaunchedEffect(screen) {
                val index = tabs.indexOf(screen)
                if (index >= 0 && index != pagerState.settledPage) {
                    animatingTo = index
                    try {
                        pagerState.animateScrollToPage(index)
                    } finally {
                        animatingTo = -1
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                key = { tabs[it].toString() },
                modifier = modifier.fillMaxSize(),
            ) { page ->
                when (tabs[page]) {
                    AppScreen.Home -> HomeScreen(modifier = Modifier.fillMaxSize())

                    AppScreen.Games -> GamesScreen(
                        onOpenMentalMath = { onNavigate(AppScreen.MentalMath) },
                        onOpenCopycat = { onNavigate(AppScreen.Copycat) },
                        onOpenWordHunt = { onNavigate(AppScreen.WordHunt) },
                        onOpenNumberMemory = { onNavigate(AppScreen.NumberMemory) },
                        onOpenChimp = { onNavigate(AppScreen.Chimp) },
                        onOpenLightsOut = { onNavigate(AppScreen.LightsOut) },
                        onOpenColorMatch = { onNavigate(AppScreen.ColorMatch) },
                        onOpenKimsSack = { onNavigate(AppScreen.KimsSack) },
                        onOpenShortTerm = { onNavigate(AppScreen.ShortTerm) },
                        onOpenSchulte = { onNavigate(AppScreen.Schulte) },
                        onOpenPercentages = { onNavigate(AppScreen.Percentages) },
                        modifier = Modifier.fillMaxSize(),
                    )

                    else -> SettingsScreen(
                        themePref = themePref,
                        fontScale = fontScale,
                        notificationsEnabled = notificationsEnabled,
                        languageCode = languageCode,
                        onSetTheme = onSetTheme,
                        onSetFontScale = onSetFontScale,
                        onSetNotifications = onSetNotifications,
                        onSetLanguage = onSetLanguage,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        AppScreen.MentalMath -> MentalMathScreen(
            onBack = { onNavigate(AppScreen.Games) },
            modifier = modifier,
        )

        AppScreen.Copycat -> CopycatScreen(
            onHome = { onNavigate(AppScreen.Games) },
            modifier = modifier,
        )

        AppScreen.WordHunt -> WordHuntScreen(
            onBack = { onNavigate(AppScreen.Games) },
            modifier = modifier,
        )

        AppScreen.NumberMemory -> NumberMemoryScreen(
            onBack = { onNavigate(AppScreen.Games) },
            modifier = modifier,
        )

        AppScreen.Chimp -> ChimpScreen(
            onHome = { onNavigate(AppScreen.Games) },
            modifier = modifier,
        )

        AppScreen.LightsOut -> LightsOutScreen(
            onBack = { onNavigate(AppScreen.Games) },
            modifier = modifier,
        )

        AppScreen.ColorMatch -> ColorMatchScreen(
            onBack = { onNavigate(AppScreen.Games) },
            modifier = modifier,
        )

        AppScreen.KimsSack -> KimsSackScreen(
            onBack = { onNavigate(AppScreen.Games) },
            modifier = modifier,
        )

        AppScreen.ShortTerm -> ShortTermScreen(
            onBack = { onNavigate(AppScreen.Games) },
            modifier = modifier,
        )

        AppScreen.Schulte -> SchulteScreen(
            onHome = { onNavigate(AppScreen.Games) },
            modifier = modifier,
        )

        AppScreen.Percentages -> PercentagesScreen(
            onBack = { onNavigate(AppScreen.Games) },
            modifier = modifier,
        )
    }
}
