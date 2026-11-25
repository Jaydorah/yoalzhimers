package com.jaydorah.alzhimers.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SimonColors = listOf(
    Color(0xFF10B981),
    Color(0xFFF59E0B),
    Color(0xFF0EA5E9),
    Color(0xFFF43F5E),
)

@Composable
fun MentalMathArt(color: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(36.dp)) {
        Text(
            "2",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.align(Alignment.TopStart),
        )
        Text(
            "+",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.align(Alignment.Center),
        )
        Text(
            "5",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}

@Composable
fun CopycatArt(modifier: Modifier = Modifier) {
    val hole = MaterialTheme.colorScheme.surface
    Canvas(modifier = modifier.size(34.dp)) {
        val inset = 1.dp.toPx()
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        val gap = 6f
        SimonColors.forEachIndexed { index, c ->
            drawArc(
                color = c,
                startAngle = -90f + index * 90f + gap / 2,
                sweepAngle = 90f - gap,
                useCenter = true,
                topLeft = Offset(inset, inset),
                size = arcSize,
            )
        }
        drawCircle(
            color = hole,
            radius = size.minDimension * 0.16f,
            center = center,
        )
    }
}

@Composable
fun WordHuntArt(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(34.dp)) {
        val u = size.minDimension / 100f
        fun x(v: Float) = v * u
        val path = Path().apply {
            moveTo(x(8f), x(8f))
            lineTo(x(38f), x(8f))
            cubicTo(x(38f), x(28f), x(62f), x(28f), x(62f), x(8f))
            lineTo(x(74f), x(8f))
            lineTo(x(74f), x(38f))
            cubicTo(x(96f), x(38f), x(96f), x(62f), x(74f), x(62f))
            lineTo(x(74f), x(92f))
            lineTo(x(8f), x(92f))
            close()
        }
        drawPath(path, color)
    }
}

@Composable
fun NumberMemoryArt(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 36.dp, height = 26.dp)
            .border(2.dp, color, RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "482",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
fun ChimpArt(color: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(36.dp)) {
        ChimpDot("1", color, Modifier.align(Alignment.TopStart))
        ChimpDot("2", color, Modifier.align(Alignment.TopEnd))
        ChimpDot("3", color, Modifier.align(Alignment.BottomStart))
        ChimpDot("4", color, Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
private fun ChimpDot(label: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            lineHeight = 9.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun LightsOutArt(color: Color, modifier: Modifier = Modifier) {
    val lit = listOf(true, false, true, false, true, false, true, false, true)
    Canvas(modifier = modifier.size(34.dp)) {
        val gap = 3.dp.toPx()
        val cell = (size.minDimension - gap * 2) / 3f
        val radius = 3.dp.toPx()
        lit.forEachIndexed { i, on ->
            val row = i / 3
            val col = i % 3
            val topLeft = Offset(col * (cell + gap), row * (cell + gap))
            if (on) {
                drawRoundRect(
                    color = color,
                    topLeft = topLeft,
                    size = Size(cell, cell),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
                )
            } else {
                drawRoundRect(
                    color = color.copy(alpha = 0.35f),
                    topLeft = topLeft,
                    size = Size(cell, cell),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
                )
            }
        }
    }
}

@Composable
fun ColorMatchArt(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(34.dp)) {
        val r = size.minDimension * 0.30f
        val dx = r * 0.55f
        val dy = r * 0.8f
        drawCircle(Color(0xFFF43F5E).copy(alpha = 0.75f), r, Offset(center.x, center.y - dy))
        drawCircle(Color(0xFF10B981).copy(alpha = 0.75f), r, Offset(center.x - dx, center.y + dy * 0.6f))
        drawCircle(Color(0xFF0EA5E9).copy(alpha = 0.75f), r, Offset(center.x + dx, center.y + dy * 0.6f))
    }
}

/** Soft handbag / purse with handle and clasp. */
@Composable
fun KimsSackArt(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(36.dp)) {
        val u = size.minDimension / 100f
        fun x(v: Float) = v * u
        val w = x(5f)

        // Handle arcs
        drawArc(
            color = color,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(x(28f), x(8f)),
            size = Size(x(44f), x(44f)),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w),
        )

        // Purse body (rounded trapezoid / soft bag)
        val body = Path().apply {
            moveTo(x(18f), x(40f))
            lineTo(x(82f), x(40f))
            cubicTo(x(86f), x(64f), x(82f), x(84f), x(70f), x(90f))
            lineTo(x(30f), x(90f))
            cubicTo(x(18f), x(84f), x(14f), x(64f), x(18f), x(40f))
            close()
        }
        drawPath(body, color.copy(alpha = 0.35f))
        drawPath(body, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = w))

        // Flap
        val flap = Path().apply {
            moveTo(x(18f), x(40f))
            lineTo(x(82f), x(40f))
            cubicTo(x(80f), x(58f), x(70f), x(66f), x(50f), x(66f))
            cubicTo(x(30f), x(66f), x(20f), x(58f), x(18f), x(40f))
            close()
        }
        drawPath(flap, color.copy(alpha = 0.55f))

        // Clasp
        drawCircle(color, radius = x(6f), center = Offset(x(50f), x(62f)))
        drawCircle(Color.White.copy(alpha = 0.7f), radius = x(2.5f), center = Offset(x(50f), x(62f)))
    }
}

/** Schulte table: mini numbered grid. */
@Composable
fun SchulteArt(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(34.dp)) {
        val gap = 2.dp.toPx()
        val cell = (size.minDimension - gap * 2) / 3f
        for (i in 0 until 9) {
            val row = i / 3
            val col = i % 3
            val topLeft = Offset(col * (cell + gap), row * (cell + gap))
            drawRoundRect(
                color = if (i == 4) color else color.copy(alpha = 0.35f),
                topLeft = topLeft,
                size = Size(cell, cell),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            )
        }
    }
}

/** Percentages: ring filled to roughly 60%. */
@Composable
fun PercentagesArt(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(34.dp)) {
        val stroke = Stroke(width = size.minDimension * 0.16f, cap = StrokeCap.Round)
        drawArc(
            color = color.copy(alpha = 0.28f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = stroke,
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 216f,
            useCenter = false,
            style = stroke,
        )
    }
}

/** Brain / maze glyph for Memory Maze (ShortTerm). */
@Composable
fun ShortTermArt(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(34.dp)) {
        val u = size.minDimension / 100f
        fun x(v: Float) = v * u
        val w = x(7f)
        // outer maze square with gaps
        drawLine(color, Offset(x(10f), x(10f)), Offset(x(70f), x(10f)), w)
        drawLine(color, Offset(x(10f), x(10f)), Offset(x(10f), x(55f)), w)
        drawLine(color, Offset(x(10f), x(90f)), Offset(x(90f), x(90f)), w)
        drawLine(color, Offset(x(90f), x(45f)), Offset(x(90f), x(90f)), w)
        drawLine(color, Offset(x(35f), x(35f)), Offset(x(75f), x(35f)), w)
        drawLine(color, Offset(x(35f), x(35f)), Offset(x(35f), x(65f)), w)
        drawLine(color, Offset(x(35f), x(65f)), Offset(x(65f), x(65f)), w)
        drawLine(color, Offset(x(65f), x(65f)), Offset(x(65f), x(45f)), w)
        // center dot
        drawCircle(color, radius = x(8f), center = Offset(x(50f), x(50f)))
    }
}
