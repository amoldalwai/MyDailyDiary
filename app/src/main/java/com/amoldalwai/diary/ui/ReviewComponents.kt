package com.amoldalwai.diary.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.getValue
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

const val MAX_REVIEW_LENGTH = 500

// Index 0 is "not rated"; 1..10 is the score.
private val ratingEmoji = listOf("🫥", "😖", "😣", "😕", "😐", "🙂", "😊", "😄", "😍", "🤩", "🏆")
private val ratingWord = listOf("Not rated", "Awful", "Bad", "Meh", "Okay", "Decent", "Good", "Great", "Loved it", "Amazing", "Masterpiece")

/**
 * A score ring that fills and changes colour as you drag, an emoji that bounces on every step, and a 0-10 slider with a
 * red-to-gold track and a tick of haptic feedback per step; 0 means unrated.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingSlider(rating: Int?, onRating: (Int?) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val view = LocalView.current
    val value = rating ?: 0
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(value) {
        bounce.snapTo(1.4f)
        bounce.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
    }
    val accent by animateColorAsState(if (value == 0) colors.outline else ratingColor(value), tween(300), label = "scoreColor")
    val filled by animateFloatAsState(value / 10f, spring(stiffness = Spring.StiffnessLow), label = "scoreFill")
    val ringTrack = colors.outline.copy(alpha = 0.25f)

    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 10.dp.toPx()
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    val topLeft = Offset(stroke / 2, stroke / 2)
                    drawArc(ringTrack, 135f, 270f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                    if (filled > 0f) drawArc(accent, 135f, 270f * filled, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (value == 0) "—" else "$value", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = accent)
                    Text("out of 10", fontSize = 10.sp, color = colors.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    ratingEmoji[value],
                    fontSize = 40.sp,
                    modifier = Modifier.graphicsLayer {
                        scaleX = bounce.value
                        scaleY = bounce.value
                    },
                )
                Text(ratingWord[value], fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = accent)
            }
            if (value > 0) TextButton(onClick = { onRating(null) }) { Text("Clear") }
        }
        Spacer(Modifier.height(10.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = { v ->
                val step = v.roundToInt()
                if (step != value) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onRating(step.takeIf { it > 0 })
            },
            valueRange = 0f..10f,
            steps = 9,
            thumb = {
                Box(
                    Modifier
                        .size(26.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(4.dp, accent, CircleShape),
                )
            },
            track = { state ->
                val fraction = (state.value / 10f).coerceIn(0f, 1f)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(Color(0xFFFF5C5C), Color(0xFFFFA24C), Color(0xFF46D369), Color(0xFFFFC94D))))
                        .drawWithContent {
                            drawContent()
                            // Wash out the part of the track beyond the thumb.
                            drawRect(colors.surfaceVariant.copy(alpha = 0.8f), Offset(size.width * fraction, 0f), Size(size.width * (1f - fraction), size.height))
                        },
                )
            },
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("😖", "😕", "🙂", "😍", "🏆").forEach { Text(it, fontSize = 13.sp, modifier = Modifier.alpha(0.65f)) }
        }
    }
}

/** Rating slider plus a short free-text review; the shared "my verdict" block for every library item. */
@Composable
fun ReviewSection(
    rating: Int?,
    review: String,
    onRating: (Int?) -> Unit,
    onReview: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("My rating", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        RatingSlider(rating, onRating)
        Spacer(Modifier.height(2.dp))
        OutlinedTextField(
            value = review,
            onValueChange = { onReview(it.take(MAX_REVIEW_LENGTH)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6,
            label = { Text("Review") },
            placeholder = { Text("What did you think?") },
            supportingText = { Text("${review.length}/$MAX_REVIEW_LENGTH") },
        )
    }
}
