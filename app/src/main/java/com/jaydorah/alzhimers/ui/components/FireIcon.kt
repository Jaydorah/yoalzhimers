package com.jaydorah.alzhimers.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val FireOuter = Color(0xFFF97316)
private val FireMid = Color(0xFFFB923C)
private val FireInner = Color(0xFFFDE68A)

@Composable
fun FireIcon(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    active: Boolean = true,
) {
    val outer = if (active) FireOuter else Color(0xFF71717A)
    val mid = if (active) FireMid else Color(0xFFA1A1AA)
    val inner = if (active) FireInner else Color(0xFFD4D4D8)

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val outerPath = Path().apply {
            moveTo(w * 0.52f, h * 0.04f)
            cubicTo(w * 0.86f, h * 0.26f, w * 0.98f, h * 0.52f, w * 0.78f, h * 0.76f)
            cubicTo(w * 0.62f, h * 0.96f, w * 0.30f, h * 1.02f, w * 0.16f, h * 0.82f)
            cubicTo(w * 0.02f, h * 0.60f, w * 0.12f, h * 0.34f, w * 0.52f, h * 0.04f)
            close()
        }
        drawPath(
            path = outerPath,
            brush = Brush.verticalGradient(listOf(outer, mid)),
        )

        val innerPath = Path().apply {
            moveTo(w * 0.50f, h * 0.38f)
            cubicTo(w * 0.72f, h * 0.50f, w * 0.78f, h * 0.68f, w * 0.64f, h * 0.84f)
            cubicTo(w * 0.52f, h * 0.96f, w * 0.34f, h * 0.96f, w * 0.28f, h * 0.82f)
            cubicTo(w * 0.20f, h * 0.66f, w * 0.32f, h * 0.50f, w * 0.50f, h * 0.38f)
            close()
        }
        drawPath(
            path = innerPath,
            brush = Brush.verticalGradient(
                listOf(inner, mid.copy(alpha = 0.9f)),
                startY = h * 0.35f,
                endY = h * 0.95f,
            ),
        )
    }
}
