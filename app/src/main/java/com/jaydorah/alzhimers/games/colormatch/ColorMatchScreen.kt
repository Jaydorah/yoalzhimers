package com.jaydorah.alzhimers.games.colormatch

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaydorah.alzhimers.R
import com.jaydorah.alzhimers.ui.components.YBadge
import com.jaydorah.alzhimers.ui.components.YButton
import com.jaydorah.alzhimers.ui.components.YButtonVariant
import com.jaydorah.alzhimers.ui.components.YCard
import kotlin.math.roundToInt

@Composable
fun ColorMatchScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ColorMatchViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    BackHandler {
        when (state.phase) {
            ColorMatchPhase.Options -> onBack()
            else -> {
                viewModel.quit()
                onBack()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (state.phase) {
            ColorMatchPhase.Options -> OptionsScreen(
                state = state,
                onToggleHidePreview = viewModel::toggleHidePreview,
                onStart = viewModel::startGame,
                onBack = onBack,
            )

            ColorMatchPhase.Matching -> MatchingScreen(
                state = state,
                onSetRed = viewModel::setRed,
                onSetGreen = viewModel::setGreen,
                onSetBlue = viewModel::setBlue,
                onCheck = viewModel::check,
                onQuit = {
                    viewModel.quit()
                    onBack()
                },
            )

            ColorMatchPhase.Result -> ResultScreen(
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
private fun OptionsScreen(
    state: ColorMatchState,
    onToggleHidePreview: () -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.game_color_match), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(R.string.cm_game_options),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Text(
                stringResource(R.string.cm_how_to_play),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            YCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToggleHidePreview)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            stringResource(R.string.cm_hide_color),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            stringResource(R.string.cm_hide_color_sub),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.settings.hidePreview,
                        onCheckedChange = { onToggleHidePreview() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                    )
                }
            }

            if (state.roundsPlayed > 0) {
                YCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            stringResource(R.string.cm_last_session),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            stringResource(
                                R.string.cm_last_session_stats,
                                state.averageScore,
                                state.bestScore,
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
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
private fun MatchingScreen(
    state: ColorMatchState,
    onSetRed: (Int) -> Unit,
    onSetGreen: (Int) -> Unit,
    onSetBlue: (Int) -> Unit,
    onCheck: () -> Unit,
    onQuit: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            title = stringResource(R.string.game_color_match),
            onBack = onQuit,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.cm_match_prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ColorSwatch(
                    label = stringResource(R.string.cm_target),
                    rgb = state.target,
                    hidden = false,
                    showRgb = false,
                    modifier = Modifier.weight(1f),
                )
                ColorSwatch(
                    label = stringResource(R.string.cm_your_color),
                    rgb = state.guess,
                    hidden = !state.showGuess,
                    modifier = Modifier.weight(1f),
                )
            }

            YCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    ChannelSlider(
                        label = stringResource(R.string.cm_red),
                        value = state.guess.r,
                        trackColor = Color(0xFFEF4444),
                        onValueChange = onSetRed,
                    )
                    ChannelSlider(
                        label = stringResource(R.string.cm_green),
                        value = state.guess.g,
                        trackColor = Color(0xFF22C55E),
                        onValueChange = onSetGreen,
                    )
                    ChannelSlider(
                        label = stringResource(R.string.cm_blue),
                        value = state.guess.b,
                        trackColor = Color(0xFF3B82F6),
                        onValueChange = onSetBlue,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
        }

        YButton(
            text = stringResource(R.string.cm_check),
            onClick = onCheck,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .height(52.dp),
        )
    }
}

@Composable
private fun ColorSwatch(
    label: String,
    rgb: Rgb,
    hidden: Boolean,
    showRgb: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(shape)
                .background(
                    if (hidden) MaterialTheme.colorScheme.surfaceVariant
                    else Color(red = rgb.r / 255f, green = rgb.g / 255f, blue = rgb.b / 255f)
                )
                .border(1.dp, MaterialTheme.colorScheme.outline, shape),
            contentAlignment = Alignment.Center,
        ) {
            if (hidden) {
                Text(
                    "?",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        val caption = when {
            hidden -> stringResource(R.string.cm_hidden)
            !showRgb -> ""
            else -> "${rgb.r} · ${rgb.g} · ${rgb.b}"
        }
        Text(
            caption,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ChannelSlider(
    label: String,
    value: Int,
    trackColor: Color,
    onValueChange: (Int) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            YBadge(
                text = value.toString(),
                containerColor = trackColor.copy(alpha = 0.18f),
                contentColor = trackColor,
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = 0f..255f,
            // 254 inner stops + both ends = all 256 values 0..255.
            steps = 254,
            colors = SliderDefaults.colors(
                thumbColor = trackColor,
                activeTrackColor = trackColor,
                inactiveTrackColor = trackColor.copy(alpha = 0.2f),
            ),
        )
    }
}

@Composable
private fun ResultScreen(
    state: ColorMatchState,
    onPlayAgain: () -> Unit,
    onOptions: () -> Unit,
    onHome: () -> Unit,
) {
    val score = state.score ?: 0

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.top_result), onBack = onHome)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))

            YCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        stringResource(scoreLabelRes(score)),
                        style = MaterialTheme.typography.headlineSmall,
                        color = scoreColor(score),
                    )
                    Text(
                        "$score%",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        stringResource(R.string.cm_how_close),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.roundsPlayed > 1) {
                        YBadge(
                            text = stringResource(
                                R.string.cm_avg_best,
                                state.averageScore,
                                state.bestScore,
                            ),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ColorSwatch(
                    label = stringResource(R.string.cm_target),
                    rgb = state.target,
                    hidden = false,
                    modifier = Modifier.weight(1f),
                )
                ColorSwatch(
                    label = stringResource(R.string.cm_your_color),
                    rgb = state.guess,
                    hidden = false,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(16.dp))

            YCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        stringResource(R.string.cm_channel_diff),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        DeltaChip("R", state.deltaR, Color(0xFFEF4444), Modifier.weight(1f))
                        DeltaChip("G", state.deltaG, Color(0xFF22C55E), Modifier.weight(1f))
                        DeltaChip("B", state.deltaB, Color(0xFF3B82F6), Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            YButton(
                text = stringResource(R.string.cm_play_again),
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

@Composable
private fun DeltaChip(label: String, delta: Int, color: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    val text = if (delta == 0) "$label ±0"
    else "$label ${if (delta > 0) "+" else ""}$delta"
    Column(
        modifier = modifier
            .clip(shape)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), shape)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            textAlign = TextAlign.Center,
        )
    }
}

@StringRes
private fun scoreLabelRes(score: Int): Int = when {
    score >= 95 -> R.string.cm_score_nailed
    score >= 85 -> R.string.cm_score_very_close
    score >= 70 -> R.string.cm_score_close
    score >= 50 -> R.string.cm_score_not_bad
    score >= 30 -> R.string.cm_score_way_off
    else -> R.string.cm_score_not_close
}

private fun scoreColor(score: Int): Color = when {
    score >= 85 -> Color(0xFF22C55E)
    score >= 70 -> Color(0xFFF59E0B)
    else -> Color(0xFFEF4444)
}
