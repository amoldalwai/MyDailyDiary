package com.amoldalwai.diary.ui

import android.view.HapticFeedbackConstants
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amoldalwai.diary.R
import com.amoldalwai.diary.data.Mood

@DrawableRes
private fun moodDrawable(mood: Mood): Int = when (mood) {
    Mood.VeryHappy -> R.drawable.mood_very_happy
    Mood.Happy -> R.drawable.mood_happy
    Mood.Calm -> R.drawable.mood_calm
    Mood.Neutral -> R.drawable.mood_neutral
    Mood.Sad -> R.drawable.mood_sad
    Mood.VerySad -> R.drawable.mood_very_sad
    Mood.Angry -> R.drawable.mood_angry
    Mood.Stressed -> R.drawable.mood_stressed
    Mood.Tired -> R.drawable.mood_tired
    Mood.Excited -> R.drawable.mood_excited
}

/** The picture for a mood. */
@Composable
fun MoodFace(mood: Mood, size: Dp, modifier: Modifier = Modifier) {
    Image(painterResource(moodDrawable(mood)), contentDescription = mood.label, modifier = modifier.size(size))
}

/**
 * A tappable mood with its name underneath. Pressing it shrinks it a little; once chosen it pops up with a ring
 * around it, its name turns bold and the others fade, plus a small tick of haptic feedback, so a tap is never unnoticed.
 */
@Composable
fun MoodPick(
    mood: Mood,
    selected: Boolean,
    othersSelected: Boolean,
    faceSize: Dp,
    accent: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val view = LocalView.current
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.16f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "moodPickScale",
    )
    val fade by animateFloatAsState(if (othersSelected && !selected) 0.45f else 1f, label = "moodPickFade")
    val ring by animateFloatAsState(if (selected) 1f else 0f, label = "moodPickRing")
    Column(
        modifier
            .width(faceSize + 14.dp)
            .bouncyClick {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onClick()
            }
            .graphicsLayer { alpha = fade },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(faceSize + 8.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(faceSize + 6.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        alpha = ring
                    }
                    .clip(CircleShape)
                    .border(2.5.dp, accent, CircleShape),
            )
            MoodFace(
                mood,
                faceSize,
                Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            mood.label,
            color = if (selected) accent else labelColor,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
