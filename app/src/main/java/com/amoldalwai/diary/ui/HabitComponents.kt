package com.amoldalwai.diary.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amoldalwai.diary.data.Book
import com.amoldalwai.diary.data.BookResult
import com.amoldalwai.diary.data.BookStatus
import com.amoldalwai.diary.data.Habit
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** What journal pages need from the habit tracker. */
class HabitUi(
    val habits: List<Habit>,
    val sound: Boolean,
    val done: (LocalDate) -> Set<String>,
    val load: (LocalDate) -> Unit,
    val toggle: (habitId: String, date: LocalDate) -> Boolean,
)

/** Short chimes synthesised in memory, so no sound file is bundled. */
object Chime {
    private const val RATE = 22050

    private fun synth(notes: List<Pair<Double, Double>>, seconds: Double, decay: Double, divisor: Double): ShortArray {
        val total = (RATE * seconds).toInt()
        return ShortArray(total) { i ->
            val t = i / RATE.toDouble()
            var sample = 0.0
            for ((frequency, start) in notes) {
                val local = t - start
                if (local >= 0) sample += sin(2 * PI * frequency * local) * exp(-local * decay)
            }
            (sample / divisor * Short.MAX_VALUE).coerceIn(Short.MIN_VALUE.toDouble(), Short.MAX_VALUE.toDouble()).toInt().toShort()
        }
    }

    private val tick: ShortArray by lazy { synth(listOf(1046.5 to 0.0, 1318.5 to 0.11, 1568.0 to 0.22), 0.8, 6.5, 2.4) }

    // A rising arpeggio that lands on a held high chord: the "you did everything" sound.
    private val fanfare: ShortArray by lazy {
        synth(
            listOf(523.25 to 0.0, 659.25 to 0.12, 783.99 to 0.24, 1046.5 to 0.36, 1318.5 to 0.52, 1568.0 to 0.52, 2093.0 to 0.52),
            1.9,
            3.2,
            3.4,
        )
    }

    fun play() = play(tick)

    fun celebrate() = play(fanfare)

    // A bright rising four-note flourish for a saved journal entry.
    private val entryDone: ShortArray by lazy {
        synth(listOf(659.25 to 0.0, 830.6 to 0.09, 987.77 to 0.18, 1318.5 to 0.3, 1975.5 to 0.3), 1.5, 4.2, 3.0)
    }

    fun entrySaved() = play(entryDone)

    private fun play(pcm: ShortArray) {
        runCatching {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(pcm, 0, pcm.size)
            track.play()
            Handler(Looper.getMainLooper()).postDelayed({ runCatching { track.release() } }, 2500)
        }
    }
}

private val confettiColors = listOf(
    Color(0xFFE8806A), Color(0xFFF7C948), Color(0xFF7BC8A4), Color(0xFF6EA8E5), Color(0xFFC09AE8), Color(0xFFF28DA6),
)

private class Piece(val angle: Float, val speed: Float, val size: Float, val color: Color, val spin: Float)

/** Bursts confetti from the centre of the element each time [trigger] increases. */
@Composable
fun Modifier.confetti(trigger: Int): Modifier {
    val progress = remember { Animatable(1f) }
    val pieces = remember(trigger) {
        List(26) {
            Piece(
                angle = Random.nextFloat() * (2 * PI).toFloat(),
                speed = 140f + Random.nextFloat() * 260f,
                size = 3f + Random.nextFloat() * 4f,
                color = confettiColors.random(),
                spin = Random.nextFloat() * 720f - 360f,
            )
        }
    }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(1100, easing = LinearEasing))
        }
    }
    return this.drawWithContent {
        drawContent()
        val t = progress.value
        if (t < 1f) {
            val centre = Offset(size.width / 2f, size.height / 2f)
            pieces.forEach { p ->
                val x = centre.x + cos(p.angle) * p.speed * t * density
                val y = centre.y + sin(p.angle) * p.speed * t * density - 90f * t * density + 420f * t * t * density
                rotate(p.spin * t, Offset(x, y)) {
                    drawRect(
                        p.color.copy(alpha = (1f - t).coerceIn(0f, 1f)),
                        Offset(x - p.size * density / 2f, y - p.size * density / 2f),
                        Size(p.size * density, p.size * density * 1.6f),
                    )
                }
            }
        }
    }
}

/** Tick-able habit chips; ticking one fires confetti, a chime and a soft haptic tap, and the last one of the day throws a party. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitChips(
    habits: List<Habit>,
    doneIds: Set<String>,
    ink: Color,
    accent: Color,
    onAccent: Color,
    sound: Boolean,
    onToggle: (String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    var celebrate by remember { mutableIntStateOf(0) }
    var banner by remember { mutableStateOf(false) }
    LaunchedEffect(celebrate) {
        if (celebrate > 0) {
            banner = true
            delay(3200)
            banner = false
        }
    }
    Column(modifier.confettiRain(celebrate)) {
        AnimatedVisibility(
            visible = banner,
            enter = scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) + fadeIn(),
            exit = fadeOut() + scaleOut(),
        ) {
            Text(
                "🎉 All habits done today!",
                modifier = Modifier
                    .padding(bottom = 14.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f))
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                color = ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            habits.forEach { habit ->
                key(habit.id) {
                    val done = habit.id in doneIds
                    var burst by remember { mutableIntStateOf(0) }
                    val fill by animateColorAsState(if (done) accent else ink.copy(alpha = 0.08f), label = "habitFill")
                    val scale by animateFloatAsState(
                        if (done) 1.03f else 1f,
                        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                        label = "habitScale",
                    )
                    Row(
                        Modifier
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .confetti(burst)
                            .clip(CircleShape)
                            .background(fill)
                            .clickable {
                                if (onToggle(habit.id)) {
                                    burst++
                                    val lastOne = habits.all { it.id == habit.id || it.id in doneIds }
                                    if (lastOne) celebrate++
                                    if (sound) if (lastOne) Chime.celebrate() else Chime.play()
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(habit.emoji, fontSize = 16.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(habit.name, color = if (done) onAccent else ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        if (done) {
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Rounded.Check, null, tint = onAccent, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

private class Drop(val x: Float, val delay: Float, val fall: Float, val sway: Float, val size: Float, val color: Color, val spin: Float)

/** Confetti raining across the whole element for a couple of seconds each time [trigger] increases. */
@Composable
fun Modifier.confettiRain(trigger: Int): Modifier {
    val progress = remember { Animatable(1f) }
    val drops = remember(trigger) {
        List(90) {
            Drop(
                x = Random.nextFloat(),
                delay = Random.nextFloat() * 0.4f,
                fall = 0.8f + Random.nextFloat() * 0.5f,
                sway = 6f + Random.nextFloat() * 18f,
                size = 4f + Random.nextFloat() * 5f,
                color = confettiColors.random(),
                spin = Random.nextFloat() * 6f,
            )
        }
    }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(2600, easing = LinearEasing))
        }
    }
    return this.drawWithContent {
        drawContent()
        val t = progress.value
        if (t < 1f) {
            drops.forEach { d ->
                val local = ((t - d.delay) / (1f - d.delay)).coerceIn(0f, 1f)
                if (local > 0f) {
                    val x = d.x * size.width + sin(local * 7f + d.spin) * d.sway * density
                    val y = -16f * density + local * (size.height + 160f * density) * d.fall
                    rotate(d.spin * local * 90f, Offset(x, y)) {
                        drawRect(
                            d.color.copy(alpha = (1f - local * local).coerceIn(0f, 1f)),
                            Offset(x - d.size * density / 2f, y - d.size * density / 2f),
                            Size(d.size * density, d.size * density * 1.6f),
                        )
                    }
                }
            }
        }
    }
}
