package com.jaydorah.alzhimers.games.kimsack

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.annotation.StringRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaydorah.alzhimers.R
import com.jaydorah.alzhimers.ui.components.YBadge
import com.jaydorah.alzhimers.ui.components.YButton
import com.jaydorah.alzhimers.ui.components.YButtonVariant
import com.jaydorah.alzhimers.ui.components.YCard
import com.jaydorah.alzhimers.ui.components.spriteLabel
import com.jaydorah.alzhimers.ui.theme.TimerGreen
import com.jaydorah.alzhimers.ui.theme.TimerRed
import com.jaydorah.alzhimers.ui.theme.TimerYellow
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// Warm table tones
private val TableWood = Color(0xFFC4A574)
private val TableEdge = Color(0xFFA88858)
private val TableSlot = Color(0xFFE8D5B5)
private val TableSlotEmpty = Color(0xFFD9C4A0)

@Composable
fun KimsSackScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: KimsSackViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onAppBackgrounded() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onAppForegrounded() }

    BackHandler {
        if (viewModel.state.value.phase != KimsSackPhase.Options) {
            viewModel.quit()
        }
        onBack()
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (state.phase) {
            KimsSackPhase.Options -> OptionsScreen(
                state = state,
                onSetDifficulty = viewModel::setDifficulty,
                onSetPreciseLocation = viewModel::setPreciseLocation,
                onStart = viewModel::startGame,
                onBack = onBack,
            )

            KimsSackPhase.Memorize -> MemorizeScreen(
                state = state,
                onHide = viewModel::hideNow,
                onQuit = { viewModel.quit(); onBack() },
            )

            KimsSackPhase.Collect -> CollectScreen(
                state = state,
                onQuit = { viewModel.quit(); onBack() },
            )

            KimsSackPhase.Recall -> RecallScreen(
                state = state,
                onPlace = viewModel::placeItem,
                onRemove = viewModel::removeItem,
                onSubmit = viewModel::submit,
                onQuit = { viewModel.quit(); onBack() },
                precise = state.settings.preciseLocation,
            )

            KimsSackPhase.Result -> ResultScreen(
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
private fun OptionsScreen(
    state: KimsSackState,
    onSetDifficulty: (KimsSackDifficulty) -> Unit,
    onSetPreciseLocation: (Boolean) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.game_kims_sack), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(R.string.ks_game_options),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Text(
                stringResource(R.string.ks_how_to_play),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            YCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        stringResource(R.string.ks_difficulty),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        stringResource(R.string.ks_difficulty_sub),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        KimsSackDifficulty.entries.forEach { d ->
                            DifficultyRow(
                                difficulty = d,
                                selected = state.settings.difficulty == d,
                                onSelect = { onSetDifficulty(d) },
                            )
                        }
                    }
                }
            }

            YCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSetPreciseLocation(!state.settings.preciseLocation) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            stringResource(R.string.ks_precise_location),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            stringResource(R.string.ks_precise_sub),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.settings.preciseLocation,
                        onCheckedChange = { onSetPreciseLocation(it) },
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
private fun DifficultyRow(
    difficulty: KimsSackDifficulty,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val border = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            )
            .border(if (selected) 2.dp else 1.dp, border, shape)
            .clickable(onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(difficulty.labelRes),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            )
            Text(
                stringResource(
                    R.string.ks_diff_stats_line,
                    pluralStringResource(R.plurals.score_items, difficulty.itemCount, difficulty.itemCount),
                    difficulty.seconds,
                    pluralStringResource(R.plurals.ks_decoys_count, difficulty.decoys, difficulty.decoys),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected) {
            YBadge(text = stringResource(R.string.ks_selected))
        }
    }
}

// ── Wooden table surface ─────────────────────────────────────────

@Composable
private fun TableSurface(
    modifier: Modifier = Modifier,
    rows: Int,
    itemForSlot: @Composable (Int) -> ItemType?,
    slotBounds: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, Rect>? = null,
    onSlotTap: ((Int) -> Unit)? = null,
    showLabels: Boolean = true,
    highlightSlot: (Int, Color) -> Color = { _, base -> base },
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(TableWood, TableEdge),
                ),
            )
            .border(3.dp, TableEdge, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        repeat(rows) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            ) {
                repeat(TABLE_COLS) { col ->
                    val slot = row * TABLE_COLS + col
                    val item = itemForSlot(slot)
                    val base = if (item == null) TableSlotEmpty else TableSlot
                    val bg = highlightSlot(slot, base)
                    val slotShape = RoundedCornerShape(14.dp)

                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .size(88.dp)
                            .clip(slotShape)
                            .background(bg)
                            .border(
                                if (item == null) 1.5.dp else 2.dp,
                                if (item == null) TableEdge.copy(alpha = 0.5f) else TableEdge,
                                slotShape,
                            )
                            .onGloballyPositioned { coords ->
                                slotBounds?.put(slot, coords.boundsInRoot())
                            }
                            .then(
                                if (onSlotTap != null && item != null) {
                                    Modifier.clickable { onSlotTap(slot) }
                                } else {
                                    Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (item != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                ItemIcon(
                                    type = item,
                                    color = MaterialTheme.colorScheme.primary,
                                    size = 44.dp,
                                )
                                if (showLabels) {
                                    Text(
                                        text = spriteLabel(item),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DeeperText,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        fontSize = 10.sp,
                                    )
                                }
                            }
                        } else if (onSlotTap != null) {
                            // empty drop target hint
                            Text(
                                text = "·",
                                fontSize = 28.sp,
                                color = TableEdge.copy(alpha = 0.45f),
                            )
                        }
                    }
                }
            }
        }
    }
}

private val DeeperText = Color(0xFF6B5E54)

// ── Memorize ─────────────────────────────────────────────────────

@Composable
private fun MemorizeScreen(
    state: KimsSackState,
    onHide: () -> Unit,
    onQuit: () -> Unit,
) {
    val fraction by animateFloatAsState(
        targetValue = state.memorizeFraction,
        animationSpec = tween(durationMillis = 80),
        label = "timer",
    )
    val barColor by animateColorAsState(
        targetValue = timerColor(state.memorizeFraction),
        animationSpec = tween(durationMillis = 200),
        label = "barColor",
    )
    val rows = tableRows(state.trayItems.size)

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            title = stringResource(R.string.game_kims_sack),
            onBack = onQuit,
            actions = {
                Text(
                    pluralStringResource(
                        R.plurals.score_items,
                        state.settings.difficulty.itemCount,
                        state.settings.difficulty.itemCount,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 4.dp),
                )
            },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.ks_memorize),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TableSurface(
                    rows = rows,
                    itemForSlot = { slot -> state.trayItems.getOrNull(slot) },
                    showLabels = true,
                )
            }

            Spacer(Modifier.weight(1f))

            YButton(
                text = stringResource(R.string.ks_hide_now),
                onClick = onHide,
                variant = YButtonVariant.Outline,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            )
            Spacer(Modifier.height(12.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(999.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(barColor.copy(alpha = 0.85f), barColor),
                            ),
                        ),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.ks_seconds, state.secondsLeft),
                style = MaterialTheme.typography.labelMedium,
                color = barColor,
            )
        }
    }
}

// ── Collect interstitial ────────────────────────────────────────

@Composable
private fun CollectScreen(
    state: KimsSackState,
    onQuit: () -> Unit,
) {
    // Drive the stagger: 0..1 over 1200ms.
    var progress by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        while (true) {
            val elapsed = System.currentTimeMillis() - start
            progress = (elapsed / 1200f).coerceIn(0f, 1f)
            if (progress >= 1f) break
            delay(16)
        }
    }

    val pursePulse by animateFloatAsState(
        targetValue = if (progress > 0.5f) 1.05f else 1f,
        animationSpec = tween(400),
        label = "purse",
    )

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(title = stringResource(R.string.game_kims_sack), onBack = onQuit)

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Items flying into purse (staggered by index).
                Box(
                    modifier = Modifier.size(220.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    state.trayItems.forEachIndexed { i, item ->
                        val stagger = (i * 0.07f).coerceAtMost(0.5f)
                        val local = ((progress - stagger) / 0.5f).coerceIn(0f, 1f)
                        // start scattered around edge, end at center (into purse)
                        val angle = (i.toFloat() / state.trayItems.size) * 2f * Math.PI.toFloat()
                        val cos = Math.cos(angle.toDouble()).toFloat()
                        val sin = Math.sin(angle.toDouble()).toFloat()
                        val scale = 1f - local * 0.7f
                        val alpha = 1f - local * 0.85f

                        Box(
                            modifier = Modifier
                                .offset {
                                    // Box content is centered already; radius is in dp.
                                    val radius = 90.dp.toPx() * (1f - local)
                                    IntOffset(
                                        x = (cos * radius).roundToInt(),
                                        y = (sin * radius).roundToInt(),
                                    )
                                }
                                .size(48.dp)
                                .graphicsLayer {
                                    this.scaleX = scale
                                    this.scaleY = scale
                                    this.alpha = alpha
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            ItemIcon(
                                type = item,
                                color = MaterialTheme.colorScheme.primary,
                                size = 44.dp,
                            )
                        }
                    }

                    // Purse sits in the middle and grows slightly.
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .graphicsLayer {
                                scaleX = pursePulse
                                scaleY = pursePulse
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        KimsSackPurse(color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(Modifier.height(28.dp))
                Text(
                    stringResource(R.string.ks_collecting),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.ks_get_ready),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Larger purse drawing for the interstitial. */
@Composable
private fun KimsSackPurse(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(100.dp)) {
        val u = size.minDimension / 100f
        fun x(v: Float) = v * u
        val w = x(4f)

        // Handle
        drawArc(
            color = color,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(x(28f), x(6f)),
            size = Size(x(44f), x(48f)),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w),
        )
        // Body
        val body = Path().apply {
            moveTo(x(16f), x(38f))
            lineTo(x(84f), x(38f))
            cubicTo(x(88f), x(64f), x(84f), x(86f), x(70f), x(92f))
            lineTo(x(30f), x(92f))
            cubicTo(x(16f), x(86f), x(12f), x(64f), x(16f), x(38f))
            close()
        }
        drawPath(body, color.copy(alpha = 0.35f))
        drawPath(body, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = w))
        // Flap
        val flap = Path().apply {
            moveTo(x(16f), x(38f))
            lineTo(x(84f), x(38f))
            cubicTo(x(82f), x(58f), x(70f), x(68f), x(50f), x(68f))
            cubicTo(x(30f), x(68f), x(18f), x(58f), x(16f), x(38f))
            close()
        }
        drawPath(flap, color.copy(alpha = 0.55f))
        // Clasp
        drawCircle(color, radius = x(6f), center = Offset(x(50f), x(64f)))
        drawCircle(Color.White.copy(alpha = 0.7f), radius = x(2.5f), center = Offset(x(50f), x(64f)))
    }
}

// ── Recall: side rails + empty table ─────────────────────────────

@Composable
private fun RecallScreen(
    state: KimsSackState,
    onPlace: (Int, ItemType) -> Unit,
    onRemove: (Int) -> Unit,
    onSubmit: () -> Unit,
    onQuit: () -> Unit,
    precise: Boolean,
) {
    val slotBounds = remember { mutableStateMapOf<Int, Rect>() }
    var dragItem by remember { mutableStateOf<ItemType?>(null) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }

    val remaining = state.remainingPool
    val (leftItems, rightItems) = splitSides(remaining)
    val placedCount = state.placed.size
    val total = state.settings.difficulty.itemCount
    val rows = tableRows(total)
    val primary = MaterialTheme.colorScheme.primary
    val stageOrigin = remember { mutableStateOf(Offset.Zero) }
    val railCapacity = splitSides(state.pool).first.size

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            title = stringResource(R.string.ks_your_recall),
            onBack = onQuit,
            actions = {
                // "3 / 12" has no strong characters; keep it reading left to right in RTL.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    YBadge(text = "$placedCount / $total")
                }
            },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(
                    if (precise) R.string.ks_precise_prompt else R.string.ks_drag_prompt
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.ks_tap_filled),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onGloballyPositioned { stageOrigin.value = it.positionInRoot() },
            ) {
                // Left rail
                SideRail(
                    items = leftItems,
                    capacity = railCapacity,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(72.dp)
                        .fillMaxHeight(0.9f),
                    dragItem = dragItem,
                    onPick = { dragItem = it },
                    onDragTo = { dragPos = it },
                    onDrop = {
                        val slot = slotBounds.entries
                            .firstOrNull { it.value.contains(dragPos) }?.key
                        val item = dragItem
                        if (slot != null && item != null && slot !in state.placed) {
                            onPlace(slot, item)
                        }
                        dragItem = null
                    },
                    onCancel = { dragItem = null },
                )

                // Center table
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 76.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    TableSurface(
                        rows = rows,
                        itemForSlot = { slot -> state.placed[slot] },
                        slotBounds = slotBounds,
                        onSlotTap = { slot ->
                            if (dragItem == null && state.placed.containsKey(slot)) {
                                onRemove(slot)
                            }
                        },
                        showLabels = true,
                        highlightSlot = { slot, base ->
                            val over = dragItem != null &&
                                slotBounds[slot]?.contains(dragPos) == true
                            if (over) primary.copy(alpha = 0.45f) else base
                        },
                    )
                }

                // Right rail
                SideRail(
                    items = rightItems,
                    capacity = railCapacity,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(72.dp)
                        .fillMaxHeight(0.9f),
                    dragItem = dragItem,
                    onPick = { dragItem = it },
                    onDragTo = { dragPos = it },
                    onDrop = {
                        val slot = slotBounds.entries
                            .firstOrNull { it.value.contains(dragPos) }?.key
                        val item = dragItem
                        if (slot != null && item != null && slot !in state.placed) {
                            onPlace(slot, item)
                        }
                        dragItem = null
                    },
                    onCancel = { dragItem = null },
                )

                // Drag ghost
                val item = dragItem
                if (item != null) {
                    val local = dragPos - stageOrigin.value
                    Box(
                        modifier = Modifier
                            .zIndex(2f)
                            // dragPos/stageOrigin are physical root coordinates, so place
                            // the ghost from the physical top-left even in RTL.
                            .align(AbsoluteAlignment.TopLeft)
                            .absoluteOffset {
                                val half = 36.dp.roundToPx()
                                IntOffset(
                                    x = local.x.roundToInt() - half,
                                    y = local.y.roundToInt() - half,
                                )
                            }
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                2.dp,
                                primary,
                                RoundedCornerShape(16.dp),
                            )
                            .graphicsLayer { alpha = 0.95f },
                        contentAlignment = Alignment.Center,
                    ) {
                        ItemIcon(type = item, color = primary, size = 44.dp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            if (remaining.isEmpty()) {
                Text(
                    stringResource(R.string.ks_all_placed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            YButton(
                text = stringResource(R.string.btn_submit),
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            )
        }
    }
}

@Composable
private fun SideRail(
    items: List<ItemType>,
    capacity: Int,
    modifier: Modifier = Modifier,
    dragItem: ItemType?,
    onPick: (ItemType) -> Unit,
    onDragTo: (Offset) -> Unit,
    onDrop: () -> Unit,
    onCancel: () -> Unit,
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        // Shrink chips so a full rail (up to 10 on Hard) fits instead of spilling
        // over the prompt and the Submit button.
        val gap = 8.dp
        val n = capacity.coerceAtLeast(1)
        val chipSize = ((maxHeight - gap * (n - 1)) / n).coerceIn(36.dp, 64.dp)
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(gap, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items.forEach { item ->
                SideChip(
                    item = item,
                    size = chipSize,
                    dimmed = dragItem == item,
                    onPick = { onPick(item) },
                    onDragTo = onDragTo,
                    onDrop = onDrop,
                    onCancel = onCancel,
                )
            }
        }
    }
}

@Composable
private fun SideChip(
    item: ItemType,
    size: Dp,
    dimmed: Boolean,
    onPick: () -> Unit,
    onDragTo: (Offset) -> Unit,
    onDrop: () -> Unit,
    onCancel: () -> Unit,
) {
    val rootPos = remember { mutableStateOf(Offset.Zero) }
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(
                if (dimmed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surface,
            )
            .border(1.5.dp, MaterialTheme.colorScheme.outline, shape)
            .onGloballyPositioned { rootPos.value = it.positionInRoot() }
            .pointerInput(item) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onPick()
                        onDragTo(rootPos.value + offset)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        onDragTo(rootPos.value + change.position)
                    },
                    onDragEnd = { onDrop() },
                    onDragCancel = onCancel,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        ItemIcon(
            type = item,
            color = MaterialTheme.colorScheme.primary,
            size = size * 0.7f,
        )
    }
}

// ── Result ───────────────────────────────────────────────────────

@Composable
private fun ResultScreen(
    state: KimsSackState,
    onPlayAgain: () -> Unit,
    onOptions: () -> Unit,
    onHome: () -> Unit,
) {
    val total = state.trayItems.size
    val correct = state.correctCount
    val wrong = state.wrongCount
    val missed = total - correct

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.top_result), onBack = onHome)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
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
                        stringResource(resultLabelRes(correct, total)),
                        style = MaterialTheme.typography.headlineSmall,
                        color = resultColor(correct, total),
                    )
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            "$correct / $total",
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        stringResource(R.string.ks_items_remembered),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.settings.preciseLocation) {
                        YBadge(
                            text = stringResource(R.string.ks_precise_location),
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (wrong > 0 || missed > 0) {
                        YBadge(
                            text = stringResource(
                                R.string.ks_wrong_missed_line,
                                pluralStringResource(R.plurals.ks_wrong_count, wrong, wrong),
                                pluralStringResource(R.plurals.ks_missed_count, missed, missed),
                            ),
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                stringResource(
                    if (state.settings.preciseLocation) R.string.ks_back_in_place
                    else R.string.ks_purse_held
                ),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            val uniqueTray = state.trayItems.distinct()
            val precise = state.settings.preciseLocation
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                uniqueTray.forEach { item ->
                    val inPlace = state.trayItems.indices.any { i ->
                        state.trayItems[i] == item && state.placed[i] == item
                    }
                    val ok = if (precise) inPlace else state.placed.containsValue(item)
                    val note = if (precise && !ok && state.placed.containsValue(item)) {
                        stringResource(R.string.ks_wrong_slot)
                    } else {
                        null
                    }
                    ResultRow(item = item, ok = ok, note = note)
                }
            }

            val extras = state.placed.values
                .distinct()
                .filter { it !in uniqueTray }
            if (extras.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.ks_extra),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    extras.forEach { item ->
                        ResultRow(item = item, ok = false, struck = true)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            YButton(
                text = stringResource(R.string.ks_play_again),
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
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ResultRow(
    item: ItemType,
    ok: Boolean,
    struck: Boolean = false,
    note: String? = null,
) {
    val shape = RoundedCornerShape(10.dp)
    val color = if (ok) TimerGreen else MaterialTheme.colorScheme.error
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.40f), shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ItemIcon(type = item, color = color, size = 28.dp)
        Text(
            text = spriteLabel(item),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textDecoration = if (struck || !ok) TextDecoration.LineThrough else null,
        )
        if (note != null) {
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = color,
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = if (ok) "✓" else "✗",
            style = MaterialTheme.typography.titleMedium,
            color = color,
        )
    }
}

@StringRes
private fun resultLabelRes(correct: Int, total: Int): Int {
    if (total == 0) return R.string.ks_result_ready
    val ratio = correct.toFloat() / total
    return when {
        ratio >= 0.95f -> R.string.ks_result_perfect
        ratio >= 0.8f -> R.string.ks_result_sharp
        ratio >= 0.6f -> R.string.ks_result_not_bad
        ratio >= 0.4f -> R.string.ks_result_shaky
        else -> R.string.ks_result_lost
    }
}

private fun resultColor(correct: Int, total: Int): Color {
    if (total == 0) return TimerGreen
    val ratio = correct.toFloat() / total
    return when {
        ratio >= 0.8f -> TimerGreen
        ratio >= 0.5f -> TimerYellow
        else -> TimerRed
    }
}

private fun timerColor(fraction: Float): Color = when {
    fraction > 0.5f -> lerpColor(TimerGreen, TimerYellow, (1f - fraction) * 2f)
    else -> lerpColor(TimerYellow, TimerRed, ((0.5f - fraction) * 2f).coerceIn(0f, 1f))
}

private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val c = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * c,
        green = a.green + (b.green - a.green) * c,
        blue = a.blue + (b.blue - a.blue) * c,
        alpha = a.alpha + (b.alpha - a.alpha) * c,
    )
}
