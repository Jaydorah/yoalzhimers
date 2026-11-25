package com.jaydorah.alzhimers.games.wordhunt

import android.app.Application
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
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

@Composable
fun WordHuntScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WordHuntViewModel = viewModel(),
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
        if (viewModel.state.value.phase != WordHuntPhase.SelectDifficulty) {
            viewModel.quit()
        }
        onBack()
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (state.phase) {
            WordHuntPhase.SelectDifficulty -> OptionsScreen(
                state = state,
                onSetDifficulty = viewModel::setDifficulty,
                onToggleAllowPause = viewModel::toggleAllowPause,
                onStart = viewModel::startGame,
                onBack = onBack,
            )

            WordHuntPhase.Playing, WordHuntPhase.Paused, WordHuntPhase.Revealed -> PlayingScreen(
                state = state,
                onSelectStart = viewModel::onSelectStart,
                onSelectMove = viewModel::onSelectMove,
                onSelectEnd = viewModel::onSelectEnd,
                onPause = viewModel::pause,
                onResume = viewModel::resume,
                onQuit = { viewModel.quit(); onBack() },
                onGiveUp = viewModel::giveUp,
                onHint = viewModel::useHint,
                onPlayAgain = viewModel::playAgain,
                onOptions = viewModel::backToOptions,
            )

            WordHuntPhase.Result -> ResultScreen(
                state = state,
                onPlayAgain = viewModel::playAgain,
                onOptions = viewModel::backToOptions,
                onHome = { viewModel.quit(); onBack() },
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
    state: WordHuntState,
    onSetDifficulty: (Difficulty) -> Unit,
    onToggleAllowPause: () -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    val settings = state.settings

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.game_word_hunt), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(R.string.wh_game_options),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            YCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            stringResource(R.string.wh_allow_pause),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            stringResource(R.string.wh_allow_pause_sub),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = settings.allowPause,
                        onCheckedChange = { onToggleAllowPause() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                            disabledCheckedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            disabledCheckedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            disabledCheckedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            disabledUncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledUncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            disabledUncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        ),
                    )
                }
            }

            Text(
                stringResource(R.string.wh_difficulty),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Difficulty.entries.forEach { d ->
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
                                pluralStringResource(d.descriptionRes, d.wordCount, d.gridSize, d.wordCount),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (selected) {
                            YBadge(
                                text = stringResource(R.string.wh_selected),
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
    state: WordHuntState,
    onSelectStart: (Cell) -> Unit,
    onSelectMove: (Cell) -> Unit,
    onSelectEnd: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onQuit: () -> Unit,
    onGiveUp: () -> Unit,
    onHint: () -> Unit,
    onPlayAgain: () -> Unit,
    onOptions: () -> Unit,
) {
    val round = state.round ?: return
    val selectionSet = state.selection.toSet()
    val revealed = state.phase == WordHuntPhase.Revealed

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                title = stringResource(R.string.game_word_hunt),
                onBack = onQuit,
                actions = {
                    Text(
                        state.timeLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    if (state.settings.allowPause && state.phase == WordHuntPhase.Playing) {
                        val pauseDesc = stringResource(R.string.wh_pause)
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // English letter grid: rows must read left to right in every locale, and the
                // drag hit-testing below maps x to columns from the left edge.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    WordGrid(
                        round = round,
                        selection = selectionSet,
                        foundPlacements = round.placements.filter { it.word in state.foundWords },
                        missCells = if (state.elapsedMs - state.lastMissAtMs < 400L) state.missCells.toSet() else emptySet(),
                        hintCell = state.hintCell,
                        onSelectStart = onSelectStart,
                        onSelectMove = onSelectMove,
                        onSelectEnd = onSelectEnd,
                        enabled = state.phase == WordHuntPhase.Playing,
                    )
                }

                WordList(round = round, found = state.foundWords)

                if (revealed) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        YButton(
                            text = stringResource(R.string.wh_play_again),
                            onClick = onPlayAgain,
                            modifier = Modifier.weight(1f).height(48.dp),
                        )
                        YButton(
                            text = stringResource(R.string.btn_options),
                            onClick = onOptions,
                            variant = YButtonVariant.Outline,
                            modifier = Modifier.weight(1f).height(48.dp),
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        YButton(
                            text = stringResource(R.string.wh_hint),
                            onClick = onHint,
                            variant = YButtonVariant.Outline,
                            enabled = state.phase == WordHuntPhase.Playing,
                            modifier = Modifier.weight(1f).height(48.dp),
                        )
                        YButton(
                            text = stringResource(R.string.wh_give_up),
                            onClick = onGiveUp,
                            variant = YButtonVariant.Destructive,
                            enabled = state.phase == WordHuntPhase.Playing,
                            modifier = Modifier.weight(1f).height(48.dp),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        if (state.phase == WordHuntPhase.Paused) {
            PauseOverlay(onResume = onResume, onQuit = onQuit)
        }
    }
}

@Composable
private fun WordGrid(
    round: Round,
    selection: Set<Cell>,
    foundPlacements: List<Placement>,
    missCells: Set<Cell>,
    hintCell: Cell?,
    onSelectStart: (Cell) -> Unit,
    onSelectMove: (Cell) -> Unit,
    onSelectEnd: () -> Unit,
    enabled: Boolean,
) {
    val foundCells = foundPlacements.flatMap { it.cells }.toSet()
    val size = round.size

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .pointerInput(size, enabled) {
                if (!enabled) return@pointerInput
                fun cellAt(x: Float, y: Float): Cell? {
                    val cellW = this.size.width / size.toFloat()
                    val cellH = this.size.height / size.toFloat()
                    val c = (x / cellW).toInt().coerceIn(0, size - 1)
                    val r = (y / cellH).toInt().coerceIn(0, size - 1)
                    return Cell(r, c)
                }
                detectDragGestures(
                    onDragStart = { offset ->
                        cellAt(offset.x, offset.y)?.let(onSelectStart)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        cellAt(change.position.x, change.position.y)?.let(onSelectMove)
                    },
                    onDragEnd = { onSelectEnd() },
                    onDragCancel = { onSelectEnd() },
                )
            },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            repeat(size) { r ->
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    repeat(size) { c ->
                        val cell = Cell(r, c)
                        val letter = round.grid[r][c]
                        val inSelection = cell in selection
                        val isMiss = cell in missCells
                        val isFound = cell in foundCells
                        val isHint = cell == hintCell
                        val bg = when {
                            isMiss -> MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
                            inSelection -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            isHint -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
                            isFound -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else -> Color.Transparent
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .then(if (isHint) Modifier.shake() else Modifier)
                                .background(bg)
                                .border(
                                    0.5.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = letter.uppercaseChar().toString(),
                                style = if (isHint) {
                                    MaterialTheme.typography.headlineSmall
                                } else {
                                    MaterialTheme.typography.bodyLarge
                                },
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    isMiss -> MaterialTheme.colorScheme.error
                                    inSelection -> MaterialTheme.colorScheme.primary
                                    isHint -> MaterialTheme.colorScheme.tertiary
                                    isFound -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Modifier.shake(): Modifier {
    val transition = rememberInfiniteTransition(label = "shake")
    val offset by transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 90, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shakeOffset",
    )
    return graphicsLayer {
        translationX = offset * density
    }
}

@Composable
private fun WordList(round: Round, found: Set<String>) {
    val columns = when {
        round.words.size <= 5 -> 2
        round.words.size <= 7 -> 3
        else -> 4
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        round.words.chunked(columns).forEach { rowWords ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowWords.forEach { word ->
                    val isFound = word in found
                    YCard(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = word.uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                textDecoration = if (isFound) TextDecoration.LineThrough else null,
                                color = if (isFound) {
                                    MaterialTheme.colorScheme.primary
                                } else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                repeat(columns - rowWords.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
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
                    stringResource(R.string.wh_paused),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                YButton(
                    text = stringResource(R.string.wh_resume),
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth(),
                )
                YButton(
                    text = stringResource(R.string.wh_quit_round),
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
    state: WordHuntState,
    onPlayAgain: () -> Unit,
    onOptions: () -> Unit,
    onHome: () -> Unit,
) {
    val round = state.round ?: return

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
                        text = stringResource(R.string.wh_all_found),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = state.timeLabel,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(
                            R.string.wh_result_line,
                            pluralStringResource(R.plurals.wh_word_count, round.placements.size, round.placements.size),
                            stringResource(R.string.wh_grid_label, round.size),
                            pluralStringResource(R.plurals.wh_hints_used, state.hintsUsed, state.hintsUsed),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            YCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        stringResource(R.string.wh_words),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        round.words.joinToString(", ").uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            YButton(
                text = stringResource(R.string.wh_play_again),
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
