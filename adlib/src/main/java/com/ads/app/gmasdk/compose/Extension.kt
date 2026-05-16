package com.ads.app.gmasdk.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Stable
class ShimmerState internal constructor(val progress: Float)

@Composable
fun rememberShimmerState(durationMillis: Int = 900): ShimmerState {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val anim by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )
    return remember(anim) { ShimmerState(anim) }
}

fun Modifier.shimmer(
    state: ShimmerState,
    visible: Boolean,
    alpha: Float = 1f,
    shape: Shape = RectangleShape,
    baseColor: Color = Color.LightGray.copy(alpha = 0.5f),
    highlightColor: Color = Color.LightGray.copy(alpha = 0.18f),
    angleDegrees: Float = 20f,
    bandWidthFraction: Float = 0.45f
): Modifier = this.then(
    if (!visible) Modifier else Modifier.drawWithCache {
        if (size.minDimension <= 0f || alpha <= 0f) {
            onDrawWithContent { drawContent() }
        } else {
            val (dx, dy) = run {
                val r = Math.toRadians(angleDegrees.toDouble())
                cos(r).toFloat() to sin(r).toFloat()
            }
            val w = size.width
            val band = bandWidthFraction.coerceIn(0.1f, 0.5f) * w
            val outline = shape.createOutline(size, layoutDirection, this)
            val cachedPath: Path? = when (outline) {
                is Outline.Generic -> outline.path
                is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
                is Outline.Rectangle -> null
            }
            val baseA = baseColor.copy(alpha = baseColor.alpha * alpha)
            val hiA = highlightColor.copy(alpha = highlightColor.alpha * alpha)
            val colors = listOf(baseA, hiA, baseA)
            onDrawWithContent {
                drawContent()
                val startX = -band + state.progress * (w + band * 2)
                val start = Offset(startX * dx, startX * dy)
                val end = Offset(start.x + band * dx, start.y + band * dy)
                val brush = Brush.linearGradient(colors = colors, start = start, end = end)
                if (cachedPath != null) drawPath(cachedPath, brush) else drawRect(brush)
            }
        }
    }
)

@Composable
private inline fun Modifier.drawEdgeLine(
    width: Dp,
    color: Color?,
    brush: Brush?,
    crossinline computeStartEnd: DrawScope.(strokePx: Float) -> Pair<Offset, Offset>
): Modifier = drawWithContent {
    drawContent()
    val strokePx = width.toPx()
    if (strokePx <= 0f) return@drawWithContent
    val (start, end) = computeStartEnd(strokePx)
    when {
        brush != null -> drawLine(brush = brush, start = start, end = end, strokeWidth = strokePx)
        color != null && color != Color.Unspecified -> drawLine(color = color, start = start, end = end, strokeWidth = strokePx)
    }
}

@Composable
fun Modifier.borderTop(width: Dp = 1.dp, color: Color? = Color.Black, brush: Brush? = null): Modifier = composed {
    this.then(Modifier.drawEdgeLine(width, color, brush) { strokePx ->
        val y = strokePx / 2f
        Offset(0f, y) to Offset(size.width, y)
    })
}

@Composable
fun Modifier.borderBottom(width: Dp = 1.dp, color: Color? = Color.Black, brush: Brush? = null): Modifier = composed {
    this.then(Modifier.drawEdgeLine(width, color, brush) { strokePx ->
        val y = size.height - strokePx / 2f
        Offset(0f, y) to Offset(size.width, y)
    })
}

@Composable
fun Modifier.borderStart(width: Dp = 1.dp, color: Color? = Color.Black, brush: Brush? = null): Modifier = composed {
    val layoutDir = LocalLayoutDirection.current
    this.then(Modifier.drawEdgeLine(width, color, brush) { strokePx ->
        val x = if (layoutDir == LayoutDirection.Ltr) strokePx / 2f else size.width - strokePx / 2f
        Offset(x, 0f) to Offset(x, size.height)
    })
}

@Composable
fun Modifier.borderEnd(width: Dp = 1.dp, color: Color? = Color.Black, brush: Brush? = null): Modifier = composed {
    val layoutDir = LocalLayoutDirection.current
    this.then(Modifier.drawEdgeLine(width, color, brush) { strokePx ->
        val x = if (layoutDir == LayoutDirection.Ltr) size.width - strokePx / 2f else strokePx / 2f
        Offset(x, 0f) to Offset(x, size.height)
    })
}

@Composable
fun Modifier.borderHorizontal(width: Dp = 1.dp, color: Color? = Color.Black, brush: Brush? = null): Modifier =
    this.borderTop(width, color, brush).borderBottom(width, color, brush)

@Composable
fun Modifier.borderVertical(width: Dp = 1.dp, color: Color? = Color.Black, brush: Brush? = null): Modifier =
    this.borderStart(width, color, brush).borderEnd(width, color, brush)
