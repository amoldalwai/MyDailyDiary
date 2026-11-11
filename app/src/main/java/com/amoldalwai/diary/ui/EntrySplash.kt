package com.amoldalwai.diary.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amoldalwai.diary.EntrySplash
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val splashGreen = Color(0xFF7BC8A4)
private val splashGreenDeep = Color(0xFF2F8F6B)

/** About two seconds after saving an entry: a ring closes around a tick, the points count up and the streak shows. */
@Composable
fun EntrySavedSplash(splash: EntrySplash?, sound: Boolean, onDone: () -> Unit) {
    if (splash == null) return
    val view = LocalView.current
    key(splash.token) {
        val fade = remember { Animatable(0f) }
        val ring = remember { Animatable(0f) }
        val tick = remember { Animatable(0f) }
        val pop = remember { Animatable(0f) }
        val points = remember { Animatable(0f) }
        var burst by remember { mutableIntStateOf(0) }

        LaunchedEffect(Unit) {
            if (sound) Chime.entrySaved()
            fade.animateTo(1f, tween(220))
        }
        LaunchedEffect(Unit) {
            ring.animateTo(1f, tween(550, easing = FastOutSlowInEasing))
            tick.animateTo(1f, tween(300))
            burst = 1
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
        LaunchedEffect(Unit) {
            delay(500)
            pop.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
        }
        LaunchedEffect(Unit) {
            delay(700)
            points.animateTo(splash.points.toFloat(), tween(650))
        }
        LaunchedEffect(Unit) {
            delay(2300)
            fade.animateTo(0f, tween(260))
            onDone()
        }

        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = fade.value }
                .background(Color(0xF2101814))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDone() },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(150.dp).confetti(burst), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(150.dp)) {
                        val stroke = 9.dp.toPx()
                        val inset = stroke / 2
                        val box = Size(size.width - stroke, size.height - stroke)
                        drawArc(Color.White.copy(alpha = 0.1f), -90f, 360f, false, Offset(inset, inset), box, style = Stroke(stroke))
                        drawArc(
                            Brush.sweepGradient(listOf(splashGreenDeep, splashGreen, splashGreenDeep)), -90f, 360f * ring.value, false,
                            Offset(inset, inset), box, style = Stroke(stroke, cap = StrokeCap.Round),
                        )
                        val check = Path().apply {
                            moveTo(size.width * 0.29f, size.height * 0.53f)
                            lineTo(size.width * 0.44f, size.height * 0.67f)
                            lineTo(size.width * 0.72f, size.height * 0.37f)
                        }
                        val measure = PathMeasure().apply { setPath(check, false) }
                        val part = Path()
                        measure.getSegment(0f, measure.length * tick.value, part, true)
                        drawPath(part, Color.White, style = Stroke(11.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
                Spacer(Modifier.height(26.dp))
                androidx.compose.material3.Text("Entry saved", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.Text(
                    "+${points.value.roundToInt()} XP",
                    modifier = Modifier.graphicsLayer {
                        scaleX = 0.6f + 0.4f * pop.value
                        scaleY = 0.6f + 0.4f * pop.value
                        alpha = pop.value
                    },
                    color = Gold,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                if (splash.streak >= 1) {
                    Spacer(Modifier.height(10.dp))
                    androidx.compose.material3.Text(
                        "🔥 ${splash.streak}-day streak",
                        modifier = Modifier.graphicsLayer { alpha = pop.value },
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                androidx.compose.material3.Text(
                    "${splash.total} ${if (splash.total == 1) "page" else "pages"} in your diary",
                    modifier = Modifier.graphicsLayer { alpha = pop.value },
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 13.sp,
                )
            }
        }
    }
}
