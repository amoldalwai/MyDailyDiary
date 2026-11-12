package com.amoldalwai.diary.ui

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.amoldalwai.diary.R
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/** Scales down slightly while pressed, with a springy release. */
@Composable
fun Modifier.bouncyClick(onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "press",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(interactionSource = source, indication = null, onClick = onClick)
}

/** [bouncyClick] that also reacts to a long press with a haptic buzz. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.bouncyClick(onLongClick: () -> Unit, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val view = LocalView.current
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "press",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .combinedClickable(
            interactionSource = source,
            indication = null,
            onClick = onClick,
            onLongClick = {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                onLongClick()
            },
        )
}

/** Fades and slides content up; items further down the list start slightly later. */
@Composable
fun Modifier.staggeredEntrance(index: Int): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (index < 7) delay(index * 70L)
        progress.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow))
    }
    return this.graphicsLayer {
        alpha = progress.value.coerceIn(0f, 1f)
        translationY = (1f - progress.value) * 48.dp.toPx()
    }
}

/** Springy scale-in used for freshly added items. */
@Composable
fun Modifier.popIn(): Modifier {
    val progress = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
    }
    return this.graphicsLayer {
        scaleX = progress.value
        scaleY = progress.value
        alpha = ((progress.value - 0.6f) / 0.4f).coerceIn(0f, 1f)
    }
}

/** Full-screen page whose gradient cross-fades when the background changes. */
@Composable
fun PageSurface(
    bg: PageBackground,
    modifier: Modifier = Modifier,
    paper: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val top by animateColorAsState(bg.top, tween(500), label = "pageTop")
    val bottom by animateColorAsState(bg.bottom, tween(500), label = "pageBottom")
    Box(modifier.background(Brush.verticalGradient(listOf(top, bottom)))) {
        if (paper) PaperDecor(bg)
        content()
    }
}

/** True when the active colour scheme is dark (respects the in-app theme choice). */
@Composable
fun appIsDark(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

@Composable
fun RoundIconButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier
            .size(40.dp)
            .bouncyClick(onClick)
            .clip(CircleShape)
            .background(colors.surface.copy(alpha = 0.8f))
            .border(1.dp, colors.outline.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = colors.primary, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun SystemBarsStyle(darkIcons: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = darkIcons
            controller.isAppearanceLightNavigationBars = darkIcons
        }
    }
}

/** Postage-stamp outline: a rectangle with semicircular perforation notches along every edge. */
private class StampShape(private val radius: Float, private val spacing: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val base = Path().apply { addRect(Rect(0f, 0f, size.width, size.height)) }
        val holes = Path()

        fun notches(length: Float, place: (Float) -> Unit) {
            val count = max(1, (length / spacing).roundToInt())
            val step = length / count
            for (i in 0 until count) place((i + 0.5f) * step)
        }
        notches(size.width) { x ->
            holes.addOval(Rect(Offset(x, 0f), radius))
            holes.addOval(Rect(Offset(x, size.height), radius))
        }
        notches(size.height) { y ->
            holes.addOval(Rect(Offset(0f, y), radius))
            holes.addOval(Rect(Offset(size.width, y), radius))
        }
        return Outline.Generic(Path.combine(PathOperation.Difference, base, holes))
    }
}

/** A photo as a postage stamp: the perforation notches cut straight into the picture, no border. */
@Composable
fun Stamp(
    file: File,
    modifier: Modifier = Modifier,
    rotation: Float = 0f,
    aspect: Float = 0.8f,
    compact: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val density = LocalDensity.current
    val shape = remember(compact, density) {
        with(density) { StampShape(if (compact) 2.2.dp.toPx() else 3.4.dp.toPx(), if (compact) 7.5.dp.toPx() else 11.dp.toPx()) }
    }
    Box(
        modifier
            .rotate(rotation)
            .then(if (onClick != null) Modifier.bouncyClick(onClick) else Modifier)
            .shadow(if (compact) 3.dp else 8.dp, shape, ambientColor = Color.Black.copy(0.3f), spotColor = Color.Black.copy(0.3f))
            .clip(shape)
            .background(Color(0xFFE9E2D8)),
    ) {
        AsyncImage(
            model = file,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspect),
        )
    }
}

@Composable
fun LogoMark(modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(12.dp))) {
        Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.fillMaxSize())
        Image(
            painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "LifeLoom logo",
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = 1.5f
                scaleY = 1.5f
            },
        )
    }
}

/** Soft, slowly drifting colour glows behind the home screen. */
@Composable
fun FloatingBlobs() {
    val transition = rememberInfiniteTransition(label = "blobs")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse),
        label = "blobT",
    )
    val c1 = MaterialTheme.colorScheme.primary
    val c2 = MaterialTheme.colorScheme.tertiary
    val c3 = MaterialTheme.colorScheme.secondary
    Canvas(Modifier.fillMaxSize()) {
        fun blob(color: Color, x: Float, y: Float, r: Float) {
            val center = Offset(x, y)
            drawCircle(
                brush = Brush.radialGradient(listOf(color.copy(alpha = 0.28f), Color.Transparent), center, r),
                radius = r,
                center = center,
            )
        }
        blob(c1, size.width * (0.9f - 0.2f * t), size.height * (0.10f + 0.05f * t), size.width * 0.7f)
        blob(c2, size.width * (0.1f + 0.15f * t), size.height * (0.45f - 0.05f * t), size.width * 0.6f)
        blob(c3, size.width * (0.8f - 0.1f * t), size.height * (0.85f + 0.05f * t), size.width * 0.7f)
    }
}
