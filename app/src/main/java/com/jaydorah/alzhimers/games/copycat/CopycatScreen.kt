package com.jaydorah.alzhimers.games.copycat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt
import com.jaydorah.alzhimers.R
import com.jaydorah.alzhimers.ui.components.YBadge
import com.jaydorah.alzhimers.ui.components.YButton
import com.jaydorah.alzhimers.ui.components.YButtonVariant
import com.jaydorah.alzhimers.ui.components.YCard

private val PadHues = listOf(
    Color(0xFF10B981), // emerald
    Color(0xFFF59E0B), // amber
    Color(0xFF0EA5E9), // sky
    Color(0xFFF43F5E), // rose
    Color(0xFF8B5CF6), // violet
    Color(0xFFFB923C), // orange
)

@Composable
fun CopycatScreen(
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CopycatViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    androidx.activity.compose.BackHandler {
        when (state.phase) {
            CopycatPhase.Showing, CopycatPhase.Input -> viewModel.backToOptions()
            else -> {
                viewModel.quit()
                onHome()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when (state.phase) {
            CopycatPhase.Options -> OptionsContent(
                state = state,
                onSetBoardSize = viewModel::setBoardSize,
                onSetMode = viewModel::setMode,
                onSetFixedLength = viewModel::setFixedLength,
                onSetDifficulty = viewModel::setDifficulty,
                onStart = viewModel::startGame,
                onHome = {
                    viewModel.quit()
                    onHome()
                },
            )

            CopycatPhase.Showing, CopycatPhase.Input -> GameContent(
                state = state,
                onPadTap = viewModel::onPadTap,
                onQuit = viewModel::backToOptions,
            )

            CopycatPhase.GameOver -> GameOverContent(
                state = state,
                onPlayAgain = viewModel::playAgain,
                onOptions = viewModel::backToOptions,
                onHome = {
                    viewModel.quit()
                    onHome()
                },
            )
        }
    }
}

@Composable
private fun OptionsContent(
    state: CopycatState,
    onSetBoardSize: (BoardSize) -> Unit,
    onSetMode: (CopycatMode) -> Unit,
    onSetFixedLength: (Int) -> Unit,
    onSetDifficulty: (CopycatDifficulty) -> Unit,
    onStart: () -> Unit,
    onHome: () -> Unit,
) {
    val settings = state.settings

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.game_copycat), onBack = onHome)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(R.string.cc_how_to_play),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionLabel(stringResource(R.string.cc_board_size))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                BoardSize.entries.forEach { size ->
                    val selected = settings.boardSize == size
                    YButton(
                        text = pluralStringResource(size.labelRes, size.pads, size.pads),
                        onClick = { onSetBoardSize(size) },
                        variant = if (selected) YButtonVariant.Default else YButtonVariant.Outline,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            SectionLabel(stringResource(R.string.cc_mode))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                CopycatMode.entries.forEach { mode ->
                    val selected = settings.mode == mode
                    YButton(
                        text = stringResource(mode.labelRes),
                        onClick = { onSetMode(mode) },
                        variant = if (selected) YButtonVariant.Default else YButtonVariant.Outline,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Text(
                if (settings.mode == CopycatMode.Classic) {
                    stringResource(R.string.cc_mode_classic_desc)
                } else {
                    stringResource(R.string.cc_mode_fixed_desc)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (settings.mode == CopycatMode.Fixed) {
                YCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            stringResource(R.string.cc_pattern_length, settings.fixedLength),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Slider(
                            value = settings.fixedLength.toFloat(),
                            onValueChange = { onSetFixedLength(it.roundToInt()) },
                            valueRange = 3f..15f,
                            steps = 11,
                        )
                    }
                }
            }

            SectionLabel(stringResource(R.string.cc_difficulty))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                CopycatDifficulty.entries.forEach { d ->
                    val selected = settings.difficulty == d
                    YButton(
                        text = stringResource(d.labelRes),
                        onClick = { onSetDifficulty(d) },
                        variant = if (selected) YButtonVariant.Default else YButtonVariant.Outline,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Text(
                if (settings.difficulty == CopycatDifficulty.Hard) {
                    stringResource(
                        R.string.cc_flashes_faster,
                        settings.flashOnMs.toInt(),
                        settings.flashGapMs.toInt(),
                    )
                } else {
                    stringResource(
                        R.string.cc_flashes_steady,
                        settings.flashOnMs.toInt(),
                        settings.flashGapMs.toInt(),
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(4.dp))
        }

        YButton(
            text = stringResource(R.string.btn_start),
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .height(52.dp),
        )
    }
}

@Composable
private fun TopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.content_desc_back),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 4.dp),
        )
        Spacer(Modifier.weight(1f))
        actions()
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun GameContent(
    state: CopycatState,
    onPadTap: (Int) -> Unit,
    onQuit: () -> Unit,
) {
    val padCount = state.settings.boardSize.pads
    val cols = if (padCount == 4) 2 else 3

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            title = stringResource(R.string.game_copycat),
            onBack = onQuit,
            actions = {
                if (state.roundsCleared > 0) {
                    YBadge(
                        text = pluralStringResource(
                            R.plurals.cc_cleared_count,
                            state.roundsCleared,
                            state.roundsCleared,
                        ),
                    )
                }
            },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                when (state.phase) {
                    CopycatPhase.Showing -> "👀"
                    else -> "🗣️ ${state.inputIndex + 1}"
                },
                fontSize = 26.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.weight(1f))

        // Board
        val rows = padCount / cols
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            repeat(rows) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    repeat(cols) { col ->
                        val index = row * cols + col
                        if (index < padCount) {
                            CopycatPad(
                                index = index,
                                active = state.activePad == index,
                                enabled = state.phase == CopycatPhase.Input,
                                onTap = { onPadTap(index) },
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun CopycatPad(
    index: Int,
    active: Boolean,
    enabled: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hue = PadHues[index % PadHues.size]
    val shape = RoundedCornerShape(16.dp)
    val color by animateColorAsState(
        targetValue = if (active) hue else hue.copy(alpha = 0.35f),
        animationSpec = tween(durationMillis = 120),
        label = "padColor",
    )
    val scale by animateFloatAsState(
        targetValue = if (active) 0.96f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "padScale",
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(shape)
            .background(color)
            .border(
                width = 1.dp,
                color = if (active) hue else MaterialTheme.colorScheme.outline,
                shape = shape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled && !active,
                onClick = onTap,
            ),
    )
}

@Composable
private fun GameOverContent(
    state: CopycatState,
    onPlayAgain: () -> Unit,
    onOptions: () -> Unit,
    onHome: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.top_result), onBack = onHome)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.cc_game_over),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(16.dp))
            YCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "${state.roundsCleared}",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        pluralStringResource(R.plurals.cc_rounds_cleared_label, state.roundsCleared),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(R.string.cc_longest_pattern, state.longestCleared),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            YButton(
                text = stringResource(R.string.cc_play_again),
                onClick = onPlayAgain,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                YButton(
                    text = stringResource(R.string.btn_options),
                    onClick = onOptions,
                    variant = YButtonVariant.Outline,
                    modifier = Modifier.weight(1f).height(48.dp),
                )
                YButton(
                    text = stringResource(R.string.btn_back),
                    onClick = onHome,
                    variant = YButtonVariant.Ghost,
                    modifier = Modifier.weight(1f).height(48.dp),
                )
            }
        }
    }
}
