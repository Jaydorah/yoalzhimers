package com.jaydorah.alzhimers.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.jaydorah.alzhimers.R
import com.jaydorah.alzhimers.ui.components.ChimpArt
import com.jaydorah.alzhimers.ui.components.ColorMatchArt
import com.jaydorah.alzhimers.ui.components.CopycatArt
import com.jaydorah.alzhimers.ui.components.KimsSackArt
import com.jaydorah.alzhimers.ui.components.LightsOutArt
import com.jaydorah.alzhimers.ui.components.MentalMathArt
import com.jaydorah.alzhimers.ui.components.NumberMemoryArt
import com.jaydorah.alzhimers.ui.components.PercentagesArt
import com.jaydorah.alzhimers.ui.components.ShortTermArt
import com.jaydorah.alzhimers.ui.components.SchulteArt
import com.jaydorah.alzhimers.ui.components.WordHuntArt
import com.jaydorah.alzhimers.ui.components.YBadge
import com.jaydorah.alzhimers.ui.components.YCard

private data class GameEntry(
    val title: String,
    val art: @Composable () -> Unit,
    val onClick: () -> Unit,
)

@Composable
fun GamesScreen(
    onOpenMentalMath: () -> Unit,
    onOpenCopycat: () -> Unit,
    onOpenWordHunt: () -> Unit,
    onOpenNumberMemory: () -> Unit,
    onOpenChimp: () -> Unit,
    onOpenLightsOut: () -> Unit,
    onOpenColorMatch: () -> Unit,
    onOpenKimsSack: () -> Unit,
    onOpenShortTerm: () -> Unit,
    onOpenSchulte: () -> Unit,
    onOpenPercentages: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val games = listOf(
        GameEntry(stringResource(R.string.game_mental_math), { MentalMathArt(MaterialTheme.colorScheme.primary) }, onOpenMentalMath),
        GameEntry(stringResource(R.string.game_copycat), { CopycatArt() }, onOpenCopycat),
        GameEntry(stringResource(R.string.game_word_hunt), { WordHuntArt(MaterialTheme.colorScheme.primary) }, onOpenWordHunt),
        GameEntry(stringResource(R.string.game_number_memory), { NumberMemoryArt(MaterialTheme.colorScheme.primary) }, onOpenNumberMemory),
        GameEntry(stringResource(R.string.game_chimp), { ChimpArt(MaterialTheme.colorScheme.primary) }, onOpenChimp),
        GameEntry(stringResource(R.string.game_lights_out), { LightsOutArt(MaterialTheme.colorScheme.primary) }, onOpenLightsOut),
        GameEntry(stringResource(R.string.game_color_match), { ColorMatchArt() }, onOpenColorMatch),
        GameEntry(stringResource(R.string.game_kims_sack), { KimsSackArt(MaterialTheme.colorScheme.primary) }, onOpenKimsSack),
        GameEntry(stringResource(R.string.game_memory_maze), { ShortTermArt(MaterialTheme.colorScheme.primary) }, onOpenShortTerm),
        GameEntry(stringResource(R.string.game_schulte), { SchulteArt(MaterialTheme.colorScheme.primary) }, onOpenSchulte),
        GameEntry(stringResource(R.string.game_percentages), { PercentagesArt(MaterialTheme.colorScheme.primary) }, onOpenPercentages),
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.nav_games),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(16.dp))

        val rows = games.chunked(2)
        rows.forEach { rowGames ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowGames.forEach { game ->
                    GameBlock(
                        game = game,
                        modifier = Modifier.weight(1f),
                        onClick = game.onClick,
                    )
                }
                if (rowGames.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun GameBlock(
    game: GameEntry,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    YCard(
        modifier = modifier.heightIn(min = 120.dp).fillMaxHeight(),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                game.art()
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = game.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}
