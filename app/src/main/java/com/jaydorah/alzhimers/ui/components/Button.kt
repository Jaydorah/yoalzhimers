package com.jaydorah.alzhimers.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

enum class YButtonVariant { Default, Outline, Ghost, Secondary, Destructive }

private val DefaultShape = RoundedCornerShape(10.dp)
private val DefaultContentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)

@Composable
fun YButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: YButtonVariant = YButtonVariant.Default,
    enabled: Boolean = true,
    shape: Shape = DefaultShape,
    content: @Composable RowScope.() -> Unit
) {
    when (variant) {
        YButtonVariant.Outline -> OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            contentPadding = DefaultContentPadding,
            content = content
        )

        YButtonVariant.Ghost -> Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            contentPadding = DefaultContentPadding,
            content = content
        )

        YButtonVariant.Default -> Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            contentPadding = DefaultContentPadding,
            content = content
        )

        YButtonVariant.Secondary -> Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ),
            contentPadding = DefaultContentPadding,
            content = content
        )

        YButtonVariant.Destructive -> Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = Color.White
            ),
            contentPadding = DefaultContentPadding,
            content = content
        )
    }
}

@Composable
fun YButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: YButtonVariant = YButtonVariant.Default,
    enabled: Boolean = true,
) {
    YButton(onClick = onClick, modifier = modifier, variant = variant, enabled = enabled) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
