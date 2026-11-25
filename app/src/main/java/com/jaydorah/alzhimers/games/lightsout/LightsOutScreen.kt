package com.jaydorah.alzhimers.games.lightsout

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaydorah.alzhimers.R
import com.jaydorah.alzhimers.ui.components.YBadge
import com.jaydorah.alzhimers.ui.components.YButton
import com.jaydorah.alzhimers.ui.components.YButtonVariant
import com.jaydorah.alzhimers.ui.components.YCard

private val LitCandle = Color(0xFFF59E0B)
private val UnlitCandle = Color(0xFF3F3F46)

@Composable
fun LightsOutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LightsOutViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.onAppBackground()
                Lifecycle.Event.ON_START -> viewModel.onAppForeground()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    androidx.activity.compose.BackHandler {
        when (state.phase) {
            LightsOutPhase.Playing, LightsOutPhase.Paused -> viewModel.backToOptions()
            LightsOutPhase.Result -> viewModel.backToOptions()
            else -> {
                viewModel.quit()
                onBack()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (state.phase) {
            LightsOutPhase.Options -> OptionsScreen(
                state = state,
                onSetDifficulty = viewModel::setDifficulty,
                onStart = viewModel::startGame,
                onBack = {
                    viewModel.quit()
                    onBack()
                },
            )

            LightsOutPhase.Playing, LightsOutPhase.Paused -> PlayingScreen(
                state = state,
                onCellTap = viewModel::onCellTap,
                onPause = viewModel::pause,
                onResume = viewModel::resume,
                onQuit = viewModel::backToOptions,
            )

            LightsOutPhase.Result -> ResultScreen(
                state = state,
                onPlayAgain = viewModel::playAgain,
                onOptions = viewModel::backToOptions,
                onHome = {
                    viewModel.quit()
                    onBack()
                },
            )
        }
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
private fun PauseGlyph(tint: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(6.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(tint),
        )
        Box(
            modifier = Modifier
                .width(6.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(tint),
        )
    }
}

@Composable
private fun OptionsScreen(
    state: LightsOutState,
    onSetDifficulty: (LightsOutDifficulty) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    val settings = state.settings

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.game_lights_out), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(R.string.lo_game_options),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Text(
                stringResource(R.string.lo_how_to_play),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                stringResource(R.string.lo_difficulty),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            LightsOutDifficulty.entries.forEach { d ->
                val selected = settings.difficulty == d
                YCard(
                    onClick = { onSetDifficulty(d) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (selected) Modifier.border(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(12.dp),
                                ) else Modifier
                            )
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(d.labelRes),
                                style = MaterialTheme.typography.titleLarge,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                stringResource(d.descriptionRes, d.gridSize),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (selected) {
                            YBadge(
                                text = stringResource(R.string.lo_selected),
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }

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
private fun PlayingScreen(
    state: LightsOutState,
    onCellTap: (Int, Int) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onQuit: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                title = stringResource(R.string.game_lights_out),
                onBack = onQuit,
                actions = {
                    Text(
                        state.timeLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    if (state.phase == LightsOutPhase.Playing) {
                        val pauseDesc = stringResource(R.string.lo_pause)
                        IconButton(onClick = onPause) {
                            PauseGlyph(
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .size(22.dp)
                                    .semantics { contentDescription = pauseDesc },
                            )
                        }
                    }
                },
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatCard(
                        label = stringResource(R.string.lo_time),
                        value = state.timeLabel,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = stringResource(R.string.lo_moves),
                        value = "${state.moves}",
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = stringResource(R.string.lo_lit),
                        value = "${state.litCount}",
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.weight(1f))

                CandleBoard(
                    board = state.board,
                    size = state.gridSize,
                    enabled = state.phase == LightsOutPhase.Playing,
                    onCellTap = onCellTap,
                )

                Spacer(Modifier.weight(1f))

                Text(
                    stringResource(R.string.lo_tap_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        if (state.phase == LightsOutPhase.Paused) {
            PauseOverlay(onResume = onResume, onQuit = onQuit)
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    YCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CandleBoard(
    board: List<Boolean>,
    size: Int,
    enabled: Boolean,
    onCellTap: (Int, Int) -> Unit,
) {
    val spacing = if (size <= 3) 12.dp else if (size <= 5) 8.dp else 6.dp

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        repeat(size) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                repeat(size) { col ->
                    val index = row * size + col
                    val lit = board.getOrNull(index) ?: false
                    CandleCell(
                        lit = lit,
                        enabled = enabled,
                        onTap = { onCellTap(row, col) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CandleCell(
    lit: Boolean,
    enabled: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    val bg by animateColorAsState(
        targetValue = if (lit) LitCandle.copy(alpha = 0.2f) else UnlitCandle.copy(alpha = 0.12f),
        animationSpec = tween(durationMillis = 120),
        label = "candleBg",
    )
    val flame by animateColorAsState(
        targetValue = if (lit) LitCandle else UnlitCandle,
        animationSpec = tween(durationMillis = 120),
        label = "candleFlame",
    )
    val border by animateColorAsState(
        targetValue = if (lit) LitCandle else MaterialTheme.colorScheme.outline,
        animationSpec = tween(durationMillis = 120),
        label = "candleBorder",
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onTap,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = 0.42f)
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(flame),
        )
    }
}

@Composable
private fun PauseOverlay(
    onResume: () -> Unit,
    onQuit: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        YCard(modifier = Modifier.padding(32.dp).fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.lo_paused),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                YButton(
                    text = stringResource(R.string.lo_resume),
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth(),
                )
                YButton(
                    text = stringResource(R.string.lo_quit_round),
                    onClick = onQuit,
                    variant = YButtonVariant.Outline,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ResultScreen(
    state: LightsOutState,
    onPlayAgain: () -> Unit,
    onOptions: () -> Unit,
    onHome: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.top_result), onBack = onHome)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))
            YCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.lo_all_out),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "${state.moves}",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = pluralStringResource(R.plurals.lo_moves_label, state.moves),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(
                            R.string.lo_result_line,
                            state.timeLabel,
                            stringResource(state.settings.difficulty.labelRes),
                            state.gridSize,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            YButton(
                text = stringResource(R.string.lo_play_again),
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
            Spacer(Modifier.height(8.dp))
        }
    }
}
