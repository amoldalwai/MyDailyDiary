package com.amoldalwai.diary.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Where highlighted elements are on screen, filled in by [tourTarget]. */
object TourTargets {
    val bounds = mutableStateMapOf<String, Rect>()
}

fun Modifier.tourTarget(id: String): Modifier = onGloballyPositioned { TourTargets.bounds[id] = it.boundsInRoot() }

/** What a "Try it" button does: it ends the tour and opens the matching feature. */
enum class TourAction { AddEntry, AddMovie, AddHabit }

class TourStep(val target: String?, val title: String, val body: String, val tryLabel: String? = null, val action: TourAction? = null)

val tourSteps = listOf(
    TourStep("home_hero", "Your Home", "The date, a greeting and a fresh photo each day. Tap the small refresh icon on the photo for a new one."),
    TourStep("home_profile", "Your profile and level", "Tap your avatar to see your level, rank, medals and awards, and to add your own photo."),
    TourStep("mood_card", "How are you feeling?", "Tap a mood to start a quick entry with it already chosen."),
    TourStep(
        "nav_add", "Add anything",
        "The + opens Quick Add: pick a mood, tap what you did, jot a note, and attach a book, movie or game.",
        "Try it: add an entry", TourAction.AddEntry,
    ),
    TourStep("nav_journal", "Your journal", "Every page you've written, as a list or a calendar. Posters of what you finished show up on the day you finished them."),
    TourStep(
        "nav_library", "Your library",
        "Books, games, movies and series in one place, with a search box that looks through all of them.",
        "Try it: add a movie", TourAction.AddMovie,
    ),
    TourStep(
        "nav_more", "Habits, insights and settings",
        "Add daily habits and see this week's table, check your insights, and change colours, reminders and nudges.",
        "Try it: add a habit", TourAction.AddHabit,
    ),
    TourStep(null, "You're all set", "You can replay this tour any time from Settings, under How to guide."),
)

/** A dimmed screen with a spotlight on one element and a card that explains it; the user can step, try it, or stop. */
@Composable
fun AppTourOverlay(step: Int, onStep: (Int) -> Unit, onStop: () -> Unit, onTry: (TourAction) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val index = step.coerceIn(0, tourSteps.lastIndex)
    val current = tourSteps[index]
    val last = index == tourSteps.lastIndex
    BackHandler { onStop() }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()
        val target = current.target?.let { TourTargets.bounds[it] }
        val pad = with(density) { 8.dp.toPx() }
        // With no target the hole collapses to a point in the middle.
        val hole by animateRectAsState(
            targetValue = target?.inflate(pad) ?: Rect(screenW / 2, screenH / 2, screenW / 2, screenH / 2),
            animationSpec = tween(380),
            label = "tourHole",
        )
        val pulse = rememberInfiniteTransition(label = "tourPulse")
        val ring by pulse.animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = "tourRing")

        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .pointerInput(Unit) { detectTapGestures { } },
        ) {
            drawRect(Color(0xD9101216))
            if (target != null) {
                val radius = minOf(hole.width, hole.height) / 2f
                val corner = CornerRadius(minOf(radius, 30.dp.toPx()))
                drawRoundRect(Color.Black, hole.topLeft, hole.size, corner, blendMode = BlendMode.Clear)
                val grow = 14.dp.toPx() * ring
                drawRoundRect(
                    Color.White.copy(alpha = 0.7f * (1f - ring)),
                    hole.topLeft.copy(x = hole.left - grow, y = hole.top - grow),
                    androidx.compose.ui.geometry.Size(hole.width + grow * 2, hole.height + grow * 2),
                    CornerRadius(corner.x + grow),
                    style = Stroke(2.dp.toPx()),
                )
            }
        }

        // The card goes below or above the highlight, whichever has room, and never leaves the screen.
        var cardHeight by remember { mutableIntStateOf(0) }
        val gapPx = with(density) { 14.dp.toPx() }
        val marginPx = with(density) { 16.dp.toPx() }
        val wanted = if (target == null) {
            (screenH - cardHeight) / 2f
        } else {
            val below = hole.bottom + gapPx
            val above = hole.top - gapPx - cardHeight
            val fitsBelow = below + cardHeight <= screenH - marginPx
            val fitsAbove = above >= marginPx
            when {
                fitsBelow && (target.center.y <= screenH / 2f || !fitsAbove) -> below
                fitsAbove -> above
                fitsBelow -> below
                target.center.y < screenH / 2f -> screenH - cardHeight - marginPx
                else -> marginPx
            }
        }
        val top by animateFloatAsState(
            wanted.coerceIn(marginPx, (screenH - cardHeight - marginPx).coerceAtLeast(marginPx)),
            tween(320),
            label = "tourCardY",
        )
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, top.roundToInt()) }
                .alpha(if (cardHeight == 0) 0f else 1f)
                .onSizeChanged { cardHeight = it.height }
                .padding(horizontal = 20.dp)
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(colors.surface)
                .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(26.dp))
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                tourSteps.indices.forEach { i ->
                    Box(
                        Modifier
                            .padding(end = 5.dp)
                            .size(if (i == index) 18.dp else 7.dp, 7.dp)
                            .clip(CircleShape)
                            .background(if (i == index) colors.primary else colors.outline.copy(alpha = 0.6f)),
                    )
                }
                Spacer(Modifier.weight(1f))
                Text("${index + 1} of ${tourSteps.size}", fontSize = 12.sp, color = colors.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            AnimatedContent(
                targetState = index,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                label = "tourText",
            ) { i ->
                val s = tourSteps[i]
                Column {
                    Text(s.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onSurface)
                    Spacer(Modifier.height(6.dp))
                    Text(s.body, fontSize = 14.sp, lineHeight = 20.sp, color = colors.onSurfaceVariant)
                }
            }
            val action = current.action
            if (action != null && current.tryLabel != null) {
                Spacer(Modifier.height(14.dp))
                FilledTonalButton(onClick = { onTry(action) }, modifier = Modifier.fillMaxWidth()) { Text(current.tryLabel) }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!last) TextButton(onClick = onStop) { Text("Stop tour") }
                Spacer(Modifier.weight(1f))
                if (index > 0) TextButton(onClick = { onStep(index - 1) }) { Text("Back") }
                Button(onClick = { if (last) onStop() else onStep(index + 1) }) { Text(if (last) "Done" else "Next") }
            }
        }
    }
}
