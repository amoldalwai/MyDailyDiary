package com.amoldalwai.diary.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs

val tapeColors = listOf(
    Color(0xFFF4B9A5),
    Color(0xFFB5D3BC),
    Color(0xFFCFC0EC),
    Color(0xFFF7DE8E),
    Color(0xFFAFCFEF),
)

/** Washi-tape strip used to "stick" cards and photos onto the page. */
@Composable
fun WashiTape(color: Color, modifier: Modifier = Modifier, rotation: Float = -3f) {
    Box(
        modifier
            .graphicsLayer { rotationZ = rotation }
            .size(width = 58.dp, height = 17.dp)
            .drawBehind {
                drawRoundRect(color.copy(alpha = 0.82f), cornerRadius = CornerRadius(2.dp.toPx()))
                var x = -size.height
                while (x < size.width) {
                    drawLine(Color.White.copy(alpha = 0.32f), Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = 2.5.dp.toPx())
                    x += 9.dp.toPx()
                }
            },
    )
}

/** Notebook margin line and punched binder holes. */
@Composable
fun PaperDecor(bg: PageBackground) {
    Canvas(Modifier.fillMaxSize()) {
        val marginX = 40.dp.toPx()
        drawLine(
            Color(0xFFD9575A).copy(alpha = if (bg.isDark) 0.28f else 0.22f),
            Offset(marginX, 0f),
            Offset(marginX, size.height),
            strokeWidth = 1.dp.toPx(),
        )
        for (fraction in listOf(0.2f, 0.5f, 0.8f)) {
            val c = Offset(16.dp.toPx(), size.height * fraction)
            drawCircle(Color.Black.copy(alpha = 0.22f), radius = 7.dp.toPx(), center = c)
            drawCircle(Color.White.copy(alpha = 0.35f), radius = 7.dp.toPx(), center = c + Offset(0f, 1.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
        }
    }
}

/** Writing guides (ruled, dotted or grid) aligned to text of the given line height (in sp). */
fun Modifier.pageLines(kind: String, lineHeightSp: Float, color: Color): Modifier {
    if (kind == "blank") return this
    return this.drawBehind {
        val lineHeight = lineHeightSp.sp.toPx()
        if (lineHeight < 8f) return@drawBehind
        val stroke = 1.dp.toPx()
        val dotColor = color.copy(alpha = (color.alpha * 2.2f).coerceAtMost(0.75f))
        var y = lineHeight * 0.82f
        while (y < size.height + lineHeight * 0.5f) {
            if (kind == "dotted") {
                var x = 0f
                while (x <= size.width) {
                    drawCircle(dotColor, radius = 1.3.dp.toPx(), center = Offset(x, y))
                    x += lineHeight
                }
            } else {
                drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke)
            }
            y += lineHeight
        }
        if (kind == "grid") {
            var x = 0f
            while (x <= size.width) {
                drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = stroke)
                x += lineHeight
            }
        }
    }
}

/** Turns the screen like a book page hinged on its left edge as it enters or leaves. */
@Composable
fun AnimatedContentScope.PageTurn(content: @Composable () -> Unit) {
    val angle by transition.animateFloat(
        transitionSpec = { tween(620, easing = FastOutSlowInEasing) },
        label = "pageTurn",
    ) { state -> if (state == EnterExitState.Visible) 0f else -78f }
    val density = LocalDensity.current.density
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationY = angle
                transformOrigin = TransformOrigin(0f, 0.5f)
                cameraDistance = 16f * density
            }
            .drawWithContent {
                drawContent()
                drawRect(Color.Black.copy(alpha = (abs(angle) / 78f * 0.45f).coerceIn(0f, 0.45f)))
            },
    ) { content() }
}

/** A card dropping onto the desk: slides down, straightens and settles with a little bounce. */
@Composable
fun Modifier.paperDrop(index: Int, skip: Boolean = false): Modifier {
    val progress = remember { Animatable(if (skip) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!skip) {
            if (index < 6) delay(index * 80L)
            progress.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow))
        }
    }
    return this.graphicsLayer {
        val p = progress.value
        alpha = (p * 2f).coerceIn(0f, 1f)
        translationY = (1f - p) * 90.dp.toPx()
        rotationZ = (1f - p) * 7f
        val s = 0.92f + 0.08f * p
        scaleX = s
        scaleY = s
    }
}

/** Bookmark ribbon that swings like a pendulum when it appears. */
@Composable
fun Ribbon(modifier: Modifier = Modifier) {
    val swing = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(500)
        swing.snapTo(22f)
        swing.animateTo(0f, spring(dampingRatio = 0.22f, stiffness = 40f))
    }
    val color = MaterialTheme.colorScheme.primary
    Canvas(
        modifier
            .size(width = 20.dp, height = 40.dp)
            .graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, 0f)
                rotationZ = swing.value
            },
    ) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(size.width / 2f, size.height * 0.78f)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path, color)
        drawRect(Color.Black.copy(alpha = 0.12f), size = androidx.compose.ui.geometry.Size(size.width / 2f, size.height * 0.9f), topLeft = Offset(size.width / 2f, 0f))
    }
}

/** Pulsing placeholder shown while a page of entries is loading. */
@Composable
fun ShimmerCard(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "shimmerAlpha",
    )
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = alpha))
            .padding(18.dp),
    ) {
        Box(Modifier.width(120.dp).height(18.dp).clip(RoundedCornerShape(9.dp)).background(base))
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(base))
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth(0.7f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(base))
        Spacer(Modifier.height(16.dp))
    }
}
