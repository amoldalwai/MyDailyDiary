package com.amoldalwai.diary.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amoldalwai.diary.Celebration
import com.amoldalwai.diary.EntrySplash
import com.amoldalwai.diary.LevelUp
import com.amoldalwai.diary.data.Progress
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

val Gold = Color(0xFFF7C948)
private val GoldDeep = Color(0xFFE59A2B)

/** Confetti, a chime and a banner for finishing a book, game, movie or series. */
@Composable
fun CompletionCelebration(celebration: Celebration?, sound: Boolean, onDone: (Int) -> Unit) {
    if (celebration == null) return
    val view = LocalView.current
    key(celebration.token) {
        LaunchedEffect(Unit) {
            if (sound) Chime.celebrate()
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            delay(3400)
            onDone(celebration.token)
        }
        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            shown = true
            delay(2900)
            shown = false
        }
        Box(Modifier.fillMaxSize().confettiRain(1)) {
            AnimatedVisibility(
                visible = shown,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 14.dp),
                enter = scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) + fadeIn(),
                exit = fadeOut() + scaleOut(),
            ) {
                Text(
                    "🎉 ${celebration.message}",
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
        }
    }
}

/** A short banner when an award tier is earned, like "🥉 Bookaholic · Bronze". */
@Composable
fun AwardBanner(awards: List<String>, onDone: () -> Unit) {
    if (awards.isEmpty()) return
    key(awards) {
        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            shown = true
            delay(3600)
            shown = false
            delay(400)
            onDone()
        }
        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = shown,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 14.dp),
                enter = scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) + fadeIn(),
                exit = fadeOut() + scaleOut(),
            ) {
                Column(
                    Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(22.dp)).background(Color(0xFF2B2622)).padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("AWARD UNLOCKED", color = Gold, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    awards.take(3).forEach { Text(it, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                    if (awards.size > 3) Text("+${awards.size - 3} more", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }
        }
    }
}

/** Full-screen level-up moment of about three seconds: the ring fills, the badge pops, the number counts up. */
@Composable
fun LevelUpOverlay(levelUp: LevelUp?, sound: Boolean, onDone: () -> Unit) {
    if (levelUp == null) return
    val view = LocalView.current
    key(levelUp.level) {
        val rank = Progress.rankOf(levelUp.level)
        val top = Color(rank.from)
        val bottom = Color(rank.to)
        val fade = remember { Animatable(0f) }
        val sweep = remember { Animatable(0f) }
        val pop = remember { Animatable(0f) }
        val count = remember { Animatable(levelUp.from.toFloat().coerceAtLeast(1f)) }
        var burst by remember { mutableStateOf(0) }
        LaunchedEffect(Unit) {
            if (sound) Chime.celebrate()
            fade.animateTo(1f, tween(300))
        }
        LaunchedEffect(Unit) { sweep.animateTo(1f, tween(1500, easing = FastOutSlowInEasing)) }
        LaunchedEffect(Unit) {
            delay(250)
            pop.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
        }
        LaunchedEffect(Unit) {
            delay(500)
            burst = 1
            count.animateTo(levelUp.level.toFloat(), tween(900))
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
        LaunchedEffect(Unit) {
            delay(2800)
            fade.animateTo(0f, tween(300))
            onDone()
        }
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = fade.value }
                .background(Color(0xE6140F0B))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDone() }
                .confettiRain(burst),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (levelUp.rankUp) "RANK UP" else "LEVEL UP", color = top, fontSize = 14.sp, letterSpacing = 5.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))
                Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(220.dp)) {
                        val stroke = 10.dp.toPx()
                        val inset = stroke / 2
                        drawArc(
                            Color.White.copy(alpha = 0.12f), -90f, 360f, false,
                            Offset(inset, inset), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke),
                        )
                        drawArc(
                            Brush.sweepGradient(listOf(bottom, top, bottom)), -90f, 360f * sweep.value, false,
                            Offset(inset, inset), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round),
                        )
                    }
                    Box(
                        Modifier
                            .size(150.dp)
                            .graphicsLayer {
                                scaleX = pop.value
                                scaleY = pop.value
                            }
                            .clip(CircleShape)
                            .background(Brush.verticalGradient(listOf(top, bottom))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${count.value.roundToInt()}", color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.height(26.dp))
                Text(
                    "${rank.emoji} ${levelUp.title}",
                    modifier = Modifier.graphicsLayer { alpha = pop.value },
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (levelUp.rankUp) "You reached a new rank: level ${levelUp.level}" else "You reached level ${levelUp.level}",
                    modifier = Modifier.graphicsLayer { alpha = pop.value },
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 15.sp,
                )
            }
        }
    }
}
