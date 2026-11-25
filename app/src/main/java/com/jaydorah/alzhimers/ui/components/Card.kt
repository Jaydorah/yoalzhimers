package com.jaydorah.alzhimers.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun YCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val base = modifier
        .shadow(1.dp, shape, clip = false)
        .clip(shape)
        .background(MaterialTheme.colorScheme.surface)
        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    Column(modifier = base, content = content)
}
