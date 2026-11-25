package com.jaydorah.alzhimers.games.kimsack

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jaydorah.alzhimers.ui.components.SpriteImage

@Composable
fun ItemIcon(
    type: ItemType,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
) {
    SpriteImage(name = type, modifier = modifier.size(size))
}
