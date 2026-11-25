package com.jaydorah.alzhimers.games.shortterm

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jaydorah.alzhimers.R
import com.jaydorah.alzhimers.ui.components.SPRITE_NAMES
import com.jaydorah.alzhimers.ui.components.SpriteImage
import com.jaydorah.alzhimers.ui.components.YButton
import com.jaydorah.alzhimers.ui.components.YButtonVariant
import com.jaydorah.alzhimers.ui.components.YCard
import com.jaydorah.alzhimers.ui.components.YOptionRow
import com.jaydorah.alzhimers.ui.components.YSeparator
import com.jaydorah.alzhimers.ui.components.spriteLabel

private val AccentBlue = Color(0xFF2563EB)
private val Indigo600 = Color(0xFF4F46E5)
private val Green400 = Color(0xFF4ADE80)
private val Red600 = Color(0xFFDC2626)

@Composable
fun ShortTermScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShortTermViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Hold the memorize/distraction timers while the app is in the background.
    LifecycleStartEffect(viewModel) {
        viewModel.setPaused(false)
        onStopOrDispose { viewModel.setPaused(true) }
    }

    BackHandler {
        when (state.stage) {
            ShortTermStage.History -> viewModel.closeHistory()
            ShortTermStage.Idle -> onBack()
            else -> {
                viewModel.quit()
                onBack()
            }
        }
    }

    val bg = if (state.isImpossibleRun && state.stage != ShortTermStage.Idle) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.background
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bg),
    ) {
        when (state.stage) {
            ShortTermStage.Idle -> IdleStage(
                state = state,
                viewModel = viewModel,
                onBack = onBack,
                onHistory = viewModel::openHistory,
            )

            ShortTermStage.History -> HistoryStage(
                state = state,
                onBack = viewModel::closeHistory,
                onClear = viewModel::clearHistory,
            )

            ShortTermStage.Loading -> LoadingStage()

            ShortTermStage.Memorize -> MemorizeStage(
                state = state,
                onNext = viewModel::skipMemorize,
                onQuit = { viewModel.quit(); onBack() },
            )

            ShortTermStage.Distract -> DistractStage(
                state = state,
                viewModel = viewModel,
                onQuit = { viewModel.quit(); onBack() },
            )

            ShortTermStage.Recall -> RecallStage(
                state = state,
                onFinish = viewModel::submitRecall,
                onQuit = { viewModel.quit(); onBack() },
            )

            ShortTermStage.Results -> ResultsStage(
                state = state,
                onBackMenu = viewModel::backToIdle,
                onQuit = { viewModel.quit(); onBack() },
            )

            ShortTermStage.Failed -> FailedStage(
                state = state,
                onRetry = viewModel::backToIdle,
                onQuit = { viewModel.quit(); onBack() },
            )
        }
    }
}

// ---------------------------------------------------------------------------

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

// --- IDLE / options ---

@Composable
private fun IdleStage(
    state: ShortTermState,
    viewModel: ShortTermViewModel,
    onBack: () -> Unit,
    onHistory: () -> Unit,
) {
    val s = state.settings
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            stringResource(R.string.game_memory_maze),
            onBack = onBack,
            actions = {
                TextButton(onClick = onHistory) {
                    Text(
                        stringResource(R.string.stm_history),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                    )
                }
            },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.stm_mnemonic_test),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-1).sp,
                )
                Text(
                    stringResource(R.string.stm_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            YCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    YOptionRow(
                        title = stringResource(R.string.stm_distractions),
                        subtitle = stringResource(R.string.stm_distractions_desc),
                        checked = s.distractionsEnabled,
                        onToggle = { viewModel.setDistractionsEnabled(!s.distractionsEnabled) },
                    )
                    YSeparator()
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        LabeledSlider(
                            title = stringResource(R.string.stm_items_to_remember),
                            valueText = "${s.itemCount}",
                            value = s.itemCount.toFloat(),
                            range = ShortTermSettings.MIN_ITEMS.toFloat()..ShortTermSettings.MAX_ITEMS.toFloat(),
                            steps = ShortTermSettings.MAX_ITEMS - ShortTermSettings.MIN_ITEMS - 1,
                            onValue = { viewModel.setItemCount(it.toInt()) },
                        )

                        LabeledSlider(
                            title = stringResource(R.string.stm_memorize_duration),
                            valueText = stringResource(R.string.stm_duration_secs, s.memorizeDuration),
                            value = s.memorizeDuration,
                            range = ShortTermSettings.MIN_MEMORIZE..ShortTermSettings.MAX_MEMORIZE,
                            steps = 48,
                            onValue = viewModel::setMemorizeDuration,
                        )

                        if (s.distractionsEnabled) {
                            OptionButtons(
                                label = stringResource(R.string.stm_distraction_mode),
                                options = listOf(DistractionMode.TASKS, DistractionMode.TIME),
                                selected = s.distractionMode,
                                optionLabel = {
                                    if (it == DistractionMode.TASKS) stringResource(R.string.stm_task_based)
                                    else stringResource(R.string.stm_time_based)
                                },
                                onSelect = viewModel::setDistractionMode,
                            )
                        }

                        if (s.distractionMode == DistractionMode.TASKS && s.distractionsEnabled) {
                            OptionButtons(
                                label = stringResource(R.string.stm_difficulty),
                                options = Difficulty.entries,
                                selected = s.difficulty,
                                optionLabel = { difficultyLabel(it) },
                                onSelect = viewModel::setDifficulty,
                                rows = 2,
                            )
                        } else {
                            LabeledSlider(
                                title = if (s.distractionsEnabled) stringResource(R.string.stm_total_duration)
                                else stringResource(R.string.stm_zen_wait_duration),
                                valueText = stringResource(R.string.stm_secs_short, s.zenDuration),
                                value = s.zenDuration.toFloat(),
                                range = ShortTermSettings.MIN_ZEN.toFloat()..ShortTermSettings.MAX_ZEN.toFloat(),
                                steps = ShortTermSettings.MAX_ZEN - ShortTermSettings.MIN_ZEN - 1,
                                onValue = { viewModel.setZenDuration(it.toInt()) },
                            )
                        }

                        if (s.distractionsEnabled) {
                            LabeledSlider(
                                title = stringResource(R.string.stm_time_per_task),
                                valueText = stringResource(R.string.stm_duration_secs, s.timePerTask),
                                value = s.timePerTask,
                                range = ShortTermSettings.MIN_TIME_PER_TASK..ShortTermSettings.MAX_TIME_PER_TASK,
                                steps = 7,
                                onValue = viewModel::setTimePerTask,
                            )
                        }

                        OptionButtons(
                            label = stringResource(R.string.stm_vocabulary),
                            options = VocabType.entries,
                            selected = s.vocabType,
                            optionLabel = { vocabLabel(it) },
                            onSelect = viewModel::setVocabType,
                            rows = 2,
                        )
                    }
                }
            }

            YButton(
                text = stringResource(R.string.stm_start_test),
                onClick = viewModel::startTest,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .height(56.dp),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun difficultyLabel(d: Difficulty): String = when (d) {
    Difficulty.EASY -> stringResource(R.string.stm_diff_easy)
    Difficulty.MEDIUM -> stringResource(R.string.stm_diff_medium)
    Difficulty.HARD -> stringResource(R.string.stm_diff_hard)
    Difficulty.SUPER -> stringResource(R.string.stm_diff_super)
    Difficulty.ULTRA -> stringResource(R.string.stm_diff_ultra)
    Difficulty.IMPOSSIBLE -> stringResource(R.string.stm_diff_impossible)
}

@Composable
private fun vocabLabel(v: VocabType): String = when (v) {
    VocabType.FAMOUS -> stringResource(R.string.stm_vocab_famous)
    VocabType.RANDOM -> stringResource(R.string.stm_vocab_random)
    VocabType.NUMBERS -> stringResource(R.string.stm_vocab_numbers)
    VocabType.LETTERS -> stringResource(R.string.stm_vocab_letters)
    VocabType.POKEMON -> stringResource(R.string.stm_vocab_pokemon)
    VocabType.SPRITES -> stringResource(R.string.stm_vocab_sprites)
}

private fun vocabRes(name: String): Int = when (name) {
    "RANDOM" -> R.string.stm_vocab_random
    "NUMBERS" -> R.string.stm_vocab_numbers
    "LETTERS" -> R.string.stm_vocab_letters
    "POKEMON" -> R.string.stm_vocab_pokemon
    "SPRITES" -> R.string.stm_vocab_sprites
    else -> R.string.stm_vocab_famous
}

@Composable
private fun LabeledSlider(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValue: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                valueText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        Slider(value = value, onValueChange = onValue, valueRange = range, steps = steps)
    }
}

@Composable
private fun <T> OptionButtons(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    rows: Int = 1,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        options.chunked((options.size + rows - 1) / rows).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { opt ->
                    YButton(
                        text = optionLabel(opt),
                        onClick = { onSelect(opt) },
                        variant = if (selected == opt) YButtonVariant.Default else YButtonVariant.Outline,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(row.size.coerceAtMost(3) - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// --- HISTORY ---

@Composable
private fun HistoryStage(
    state: ShortTermState,
    onBack: () -> Unit,
    onClear: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.stm_history_title), onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.history.isEmpty()) {
                Text(
                    stringResource(R.string.stm_history_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 40.dp),
                )
            } else {
                YCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        state.history.forEachIndexed { i, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        pluralStringResource(
                                            R.plurals.stm_history_items,
                                            item.wordCount,
                                            stringResource(vocabRes(item.vocabType)),
                                            item.wordCount,
                                        ),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        if (item.mode == "TASKS") pluralStringResource(R.plurals.stm_history_task_count, item.configValue, item.configValue)
                                        else stringResource(R.string.stm_history_time, item.configValue),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    "${item.score}%",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            if (i < state.history.lastIndex) {
                                YSeparator(modifier = Modifier.padding(horizontal = 12.dp))
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                YButton(
                    text = stringResource(R.string.stm_back_to_menu),
                    onClick = onBack,
                    modifier = Modifier.height(48.dp),
                )
                if (state.history.isNotEmpty()) {
                    YButton(
                        text = stringResource(R.string.stm_clear_all),
                        onClick = onClear,
                        variant = YButtonVariant.Destructive,
                        modifier = Modifier.height(48.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// --- LOADING ---

@Composable
private fun LoadingStage() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(8.dp, AccentBlue, CircleShape),
            )
            Text(
                stringResource(R.string.stm_loading),
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

// --- MEMORIZE ---

@Composable
private fun MemorizeStage(
    state: ShortTermState,
    onNext: () -> Unit,
    onQuit: () -> Unit,
) {
    val phrase = state.phrase ?: return
    val s = state.settings
    val fraction by animateFloatAsState(
        targetValue = state.memorizeFraction,
        animationSpec = tween(100),
        label = "memorizeBar",
    )
    val revealIndex = state.revealIndex
    val isPokemon = s.vocabType.isImageVocab()
    val items = if (revealIndex != null) {
        listOfNotNull(phrase.words.getOrNull(revealIndex)?.let { it to (phrase.ids.getOrNull(revealIndex)) })
    } else {
        phrase.words.mapIndexed { i, w -> w to phrase.ids.getOrNull(i) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            title = if (revealIndex != null) stringResource(R.string.stm_stack_item_title, revealIndex + 1)
            else stringResource(R.string.stm_memorize),
            onBack = onQuit,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                if (revealIndex != null) stringResource(R.string.stm_stack_item, revealIndex + 1)
                else stringResource(R.string.stm_phase_memorize),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp,
            )
            Text(
                if (isPokemon) stringResource(R.string.stm_memorize_sprites)
                else pluralStringResource(R.plurals.stm_memorize_items_count, items.size, items.size),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(999.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .clip(RoundedCornerShape(999.dp))
                        .background(AccentBlue),
                )
            }

            if (isPokemon && revealIndex == null) {
                val cols = 3
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items.chunked(cols).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowItems.forEach { (word, id) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(4.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    PokemonSprite(id = id, name = word, modifier = Modifier.fillMaxSize().padding(6.dp), local = s.vocabType == VocabType.SPRITES)
                                }
                            }
                            // pad incomplete row
                            repeat(cols - rowItems.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items.forEach { (word, _) ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (items.size > 5) 50.dp else 70.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(4.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                word,
                                fontSize = when {
                                    word.length > 15 && items.size > 5 -> 16.sp
                                    word.length > 15 -> 24.sp
                                    items.size > 5 -> 20.sp
                                    else -> 36.sp
                                },
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            YButton(
                text = if (revealIndex != null) stringResource(R.string.stm_locked_in_bang)
                else stringResource(R.string.stm_got_it),
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            )
            Text(
                stringResource(R.string.stm_vanishing, state.memorizeSecondsLeft),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun PokemonSprite(id: Int?, name: String, modifier: Modifier = Modifier, local: Boolean = false) {
    if (local) {
        val spriteName = if (id != null && id in 1..SPRITE_NAMES.size) SPRITE_NAMES[id - 1] else name
        SpriteImage(name = spriteName, modifier = modifier)
        return
    }
    if (id != null && id in 1..151) {
        val url = pokemonSpriteUrl(id)
        var failed by remember(url) { mutableStateOf(false) }
        if (failed) {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                Text(
                    name,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            AsyncImage(
                model = url,
                contentDescription = name,
                contentScale = ContentScale.Fit,
                modifier = modifier,
                onError = { failed = true },
            )
        }
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                name,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// --- DISTRACT ---

@Composable
private fun DistractStage(
    state: ShortTermState,
    viewModel: ShortTermViewModel,
    onQuit: () -> Unit,
) {
    val s = state.settings
    val fraction by animateFloatAsState(
        targetValue = if (s.distractionsEnabled) state.taskTimerFraction else 1f,
        animationSpec = tween(75),
        label = "taskBar",
    )
    val barColor by animateColorAsState(
        targetValue = if (s.distractionsEnabled && state.taskTimerSec < 1f) Red600 else AccentBlue,
        animationSpec = tween(150),
        label = "taskBarColor",
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // HUD
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onQuit) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.btn_quit),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (s.distractionsEnabled) {
                        Text(
                            if (s.distractionMode == DistractionMode.TASKS) stringResource(R.string.stm_hud_task)
                            else stringResource(R.string.stm_hud_time),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val hudValue = if (s.distractionMode == DistractionMode.TASKS) {
                            "${state.currentTaskIndex + 1} / ${state.taskQueue.size}"
                        } else {
                            stringResource(R.string.stm_time_left, state.timeLeftSec)
                        }
                        // "3 / 10" has no strong characters; keep it reading left to right in RTL.
                        ForceLtr {
                            Text(
                                hudValue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            )
                        }
                    } else {
                        Text(
                            stringResource(R.string.stm_zen_wait),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            stringResource(R.string.stm_time_left, state.timeLeftSec),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Indigo600,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(48.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (s.distractionsEnabled) {
                // Timer bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(4.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(999.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .clip(RoundedCornerShape(999.dp))
                            .background(barColor),
                    )
                }
                Spacer(Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(4.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(32.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // key forces remount on each task
                    key(state.currentTaskIndex, state.taskQueue.size) {
                        TaskHost(
                            kind = state.currentTaskKind,
                            timerDuration = s.effectiveTimePerTask,
                            onComplete = viewModel::onTaskComplete,
                            onFail = viewModel::fail,
                            onScheduleGo = { delayMs, go -> viewModel.scheduleReactionGo(delayMs, go) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            } else {
                ZenWait(state.timeLeftSec)
            }
        }
    }
}

@Composable
private fun ZenWait(secondsLeft: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Indigo600.copy(alpha = 0.12f)),
            )
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Indigo600.copy(alpha = 0.2f)),
            )
            Text(
                "$secondsLeft",
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                color = Indigo600,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.stm_zen_wait),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                stringResource(R.string.stm_zen_breathe),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// --- RECALL ---

@Composable
private fun RecallStage(
    state: ShortTermState,
    onFinish: (Int) -> Unit,
    onQuit: () -> Unit,
) {
    val phrase = state.phrase ?: return
    val s = state.settings
    val isPokemon = s.vocabType.isImageVocab()

    var verticalInputs by rememberSaveable(phrase, saver = StringListStateSaver) {
        mutableStateOf(List(phrase.words.size) { "" })
    }
    var selections by rememberSaveable(phrase, saver = NullableIntListStateSaver) {
        mutableStateOf(List<Int?>(phrase.words.size) { null })
    }
    var activeSlot by rememberSaveable(phrase) { mutableIntStateOf(0) }

    fun submit() {
        val score = if (isPokemon) scorePokemon(selections, phrase) else scoreVertical(verticalInputs, phrase)
        onFinish(score)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.stm_recall), onBack = onQuit)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.stm_recall_title),
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                if (isPokemon) {
                    if (s.vocabType == VocabType.SPRITES) stringResource(R.string.stm_recall_sprites)
                    else stringResource(R.string.stm_recall_pokemon)
                } else stringResource(R.string.stm_recall_enter),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (isPokemon) {
                // slots
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(4.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(28.dp))
                        .padding(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        selections.chunked(5).forEachIndexed { rowIdx, row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEachIndexed { colIdx, sel ->
                                    val idx = rowIdx * 5 + colIdx
                                    val isActive = activeSlot == idx
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                else MaterialTheme.colorScheme.surfaceVariant,
                                            )
                                            .border(
                                                3.dp,
                                                if (isActive) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outline,
                                                RoundedCornerShape(12.dp),
                                            )
                                            .clickable { activeSlot = idx },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "#${idx + 1}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
                                        )
                                        if (sel != null) {
                                            PokemonSprite(
                                                id = sel,
                                                name = imageChoices(s.vocabType).getOrElse(sel - 1) { "" },
                                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                                local = s.vocabType == VocabType.SPRITES,
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(2.dp)
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(Red600)
                                                    .clickable {
                                                        selections = selections.toMutableList().also { it[idx] = null }
                                                    },
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text("×", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                    }
                                }
                                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }

                // Pokedex grid
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(4.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(28.dp))
                        .padding(16.dp),
                ) {
                    Column {
                        Text(
                            if (s.vocabType == VocabType.SPRITES) stringResource(R.string.stm_sprite_selection)
                            else stringResource(R.string.stm_pokedex_selection),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 3.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(12.dp))
                        val gridScroll = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(256.dp)
                                .verticalScroll(gridScroll),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val choices = imageChoices(s.vocabType)
                            choices.indices.chunked(5).forEach { rowIds ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    rowIds.map { it + 1 }.forEach { id ->
                                        val name = choices[id - 1]
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    selections = selections.toMutableList().also {
                                                        it[activeSlot] = id
                                                    }
                                                    if (activeSlot < phrase.words.size - 1) activeSlot++
                                                },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            PokemonSprite(
                                                id = id,
                                                name = name,
                                                modifier = Modifier.fillMaxSize().padding(2.dp),
                                                local = s.vocabType == VocabType.SPRITES,
                                            )
                                        }
                                    }
                                    repeat(5 - rowIds.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    phrase.words.forEachIndexed { i, _ ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "${i + 1}",
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            OutlinedTextField(
                                value = verticalInputs[i],
                                onValueChange = { v ->
                                    verticalInputs = verticalInputs.toMutableList().also { it[i] = v }
                                },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                            )
                        }
                    }
                }
            }

            YButton(
                text = stringResource(R.string.stm_locked_in),
                onClick = ::submit,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

// Recall answers survive rotation; nulls in the sprite slots are stored as 0 (ids start at 1).
private val StringListStateSaver = listSaver<MutableState<List<String>>, String>(
    save = { it.value },
    restore = { mutableStateOf(it) },
)

private val NullableIntListStateSaver = listSaver<MutableState<List<Int?>>, Int>(
    save = { state -> state.value.map { it ?: 0 } },
    restore = { saved -> mutableStateOf(saved.map { it.takeIf { id -> id > 0 } }) },
)

// --- RESULTS ---

@Composable
private fun ResultsStage(
    state: ShortTermState,
    onBackMenu: () -> Unit,
    onQuit: () -> Unit,
) {
    val score = state.finalScore
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.top_result), onBack = onQuit)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.stm_scan_complete),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Green400)
                        .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(999.dp))
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "$score%",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    stringResource(resultVerdict(score)),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            YCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        stringResource(R.string.stm_correct_items),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    state.phrase?.words?.forEachIndexed { idx, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "#" + (idx + 1).toString().padStart(2, '0'),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                item,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            YButton(
                text = stringResource(R.string.stm_back_to_menu),
                onClick = onBackMenu,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

// --- FAILED ---

@Composable
private fun FailedStage(
    state: ShortTermState,
    onRetry: () -> Unit,
    onQuit: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.stm_failed), onBack = onQuit)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.stm_test_aborted),
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = Red600,
                textAlign = TextAlign.Center,
            )
            Text(
                state.failReasonRes?.let { "\"${stringResource(it)}\"" } ?: "",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
            )
            YButton(
                text = stringResource(R.string.btn_retry),
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            )
        }
    }
}
