package com.jaydorah.alzhimers.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jaydorah.alzhimers.R
import com.jaydorah.alzhimers.core.AppPreferences
import com.jaydorah.alzhimers.core.ScoreKeys
import com.jaydorah.alzhimers.games.percentages.QUESTION_COUNT
import com.jaydorah.alzhimers.ui.components.FireIcon
import com.jaydorah.alzhimers.ui.components.YBadge
import com.jaydorah.alzhimers.ui.components.YCard

private data class StatRow(
    val title: String,
    val icon: ImageVector,
    val yourBest: String,
    val average: String?,
    val aboveAverage: Boolean?,
)

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = AppPreferences.get(context)
    val streak = prefs.streakForDisplay()
    val gamesPlayed = prefs.gamesPlayed()
    val daysActive = prefs.daysActive()

    val nmBest = prefs.bestScore(ScoreKeys.NumberMemory)
    val chimpBest = prefs.bestScore(ScoreKeys.Chimp)
    // Mental Math has no numeric score, so its card shows how many games were finished.
    val mmPlays = prefs.playCount(ScoreKeys.MentalMath)
    val copycatBest = prefs.bestScore(ScoreKeys.Copycat)
    val whBest = prefs.bestScore(ScoreKeys.WordHunt)
    val loBest = prefs.bestScore(ScoreKeys.LightsOut)
    val cmBest = prefs.bestScore(ScoreKeys.ColorMatch)
    val ksBest = prefs.bestScore(ScoreKeys.KimsSack)
    val stBest = prefs.bestScore(ScoreKeys.ShortTerm)
    val schulteBest = prefs.bestScore(ScoreKeys.Schulte)
    val pctBest = prefs.bestScore(ScoreKeys.Percentages)

    val stats = buildList {
        add(
            StatRow(
                title = stringResource(R.string.game_number_memory),
                icon = Icons.Default.Star,
                yourBest = if (nmBest > 0) pluralStringResource(R.plurals.score_digits, nmBest, nmBest) else "—",
                average = pluralStringResource(R.plurals.score_digits, 7, 7),
                aboveAverage = when {
                    nmBest == 0 -> null
                    else -> nmBest >= 7
                },
            ),
        )
        add(
            StatRow(
                title = stringResource(R.string.game_chimp),
                icon = Icons.AutoMirrored.Filled.List,
                yourBest = if (chimpBest > 0) pluralStringResource(R.plurals.score_rounds, chimpBest, chimpBest) else "—",
                average = pluralStringResource(R.plurals.score_rounds, 4, 4),
                aboveAverage = when {
                    chimpBest == 0 -> null
                    else -> chimpBest >= 4
                },
            ),
        )
        add(
            StatRow(
                title = stringResource(R.string.game_mental_math),
                icon = Icons.Default.Add,
                yourBest = if (mmPlays > 0) pluralStringResource(R.plurals.score_played, mmPlays, mmPlays) else "—",
                average = null,
                aboveAverage = null,
            ),
        )
        add(
            StatRow(
                title = stringResource(R.string.game_copycat),
                icon = Icons.Default.PlayArrow,
                yourBest = if (copycatBest > 0) pluralStringResource(R.plurals.score_cleared, copycatBest, copycatBest) else "—",
                average = null,
                aboveAverage = null,
            ),
        )
        if (whBest > 0) {
            val totalSec = whBest / 1000
            add(
                StatRow(
                    title = stringResource(R.string.game_word_hunt),
                    icon = Icons.Default.Search,
                    yourBest = "%d:%02d".format(totalSec / 60, totalSec % 60),
                    average = null,
                    aboveAverage = null,
                ),
            )
        }
        if (loBest > 0) {
            add(
                StatRow(
                    title = stringResource(R.string.game_lights_out),
                    icon = Icons.Default.Lock,
                    yourBest = pluralStringResource(R.plurals.score_taps_best, loBest, loBest),
                    average = null,
                    aboveAverage = null,
                ),
            )
        }
        if (cmBest > 0) {
            add(
                StatRow(
                    title = stringResource(R.string.game_color_match),
                    icon = Icons.Default.Warning,
                    yourBest = stringResource(R.string.score_percent, cmBest),
                    average = null,
                    aboveAverage = null,
                ),
            )
        }
        if (ksBest > 0) {
            add(
                StatRow(
                    title = stringResource(R.string.game_kims_sack),
                    icon = Icons.Default.Star,
                    yourBest = pluralStringResource(R.plurals.score_items, ksBest, ksBest),
                    average = null,
                    aboveAverage = null,
                ),
            )
        }
        if (stBest > 0) {
            add(
                StatRow(
                    title = stringResource(R.string.game_memory_maze),
                    icon = Icons.Default.Star,
                    yourBest = stringResource(R.string.score_percent, stBest),
                    average = null,
                    aboveAverage = null,
                ),
            )
        }
        if (schulteBest > 0) {
            add(
                StatRow(
                    title = stringResource(R.string.game_schulte),
                    icon = Icons.AutoMirrored.Filled.List,
                    yourBest = com.jaydorah.alzhimers.games.schulte.formatSchulteTime(schulteBest.toLong()),
                    average = null,
                    aboveAverage = null,
                ),
            )
        }
        if (pctBest > 0) {
            add(
                StatRow(
                    title = stringResource(R.string.game_percentages),
                    icon = Icons.Default.Star,
                    yourBest = stringResource(
                        R.string.pct_score_out_of,
                        pctBest,
                        QUESTION_COUNT,
                    ),
                    average = null,
                    aboveAverage = null,
                ),
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Text(
            text = stringResource(R.string.home_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))

        // Streak banner
        YCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FireIcon(size = 44.dp, active = streak > 0)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = if (streak > 0) stringResource(R.string.home_day, streak) else stringResource(R.string.home_play_today),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = when {
                            streak > 0 -> stringResource(R.string.home_streak_active)
                            else -> stringResource(R.string.home_streak_inactive)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.weight(1f))
                if (streak > 0) {
                    YBadge(
                        text = stringResource(R.string.home_day, streak),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Lifetime counters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CounterCard(
                label = stringResource(R.string.home_games_played),
                value = "$gamesPlayed",
                modifier = Modifier.weight(1f),
            )
            CounterCard(
                label = stringResource(R.string.home_days_active),
                value = "$daysActive",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            stringResource(R.string.home_best_scores),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(10.dp))

        stats.forEach { stat ->
            StatCard(stat = stat)
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun CounterCard(label: String, value: String, modifier: Modifier = Modifier) {
    YCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatCard(stat: StatRow) {
    YCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = stat.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stat.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (stat.average != null) {
                    Text(
                        text = stringResource(R.string.home_avg_person, stat.average),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stat.yourBest,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                when (stat.aboveAverage) {
                    true -> YBadge(
                        text = stringResource(R.string.home_above_avg),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                    false -> YBadge(text = stringResource(R.string.home_keep_going))
                    null -> Unit
                }
            }
        }
    }
}
