package com.amoldalwai.diary.ui

import androidx.compose.foundation.background
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File

/**
 * The detail page for any library item, in the style of a streaming title page: a blurred backdrop of the cover,
 * the poster floating over it, the title and facts, then the section's own controls ([content]) and Save / Remove.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MediaDetailSheet(
    title: String,
    noun: String,
    cover: File?,
    rating: Int?,
    facts: List<String>,
    notes: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var confirmDelete by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val collapse by remember { derivedStateOf { (scroll.value / 420f).coerceIn(0f, 1f) } }
    val drift = rememberInfiniteTransition(label = "backdrop")
    // Eased turnarounds keep the slow pan from visibly reversing.
    val ken by drift.animateFloat(0f, 1f, infiniteRepeatable(tween(24000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "ken")
    val sweep by drift.animateFloat(
        0f,
        1f,
        infiniteRepeatable(keyframes {
            durationMillis = 9000
            0f at 0
            1f at 3600 using FastOutSlowInEasing
            1f at 9000
        }),
        label = "sweep",
    )
    val bob by drift.animateFloat(0f, 1f, infiniteRepeatable(tween(3600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    val intro = remember { Animatable(0f) }
    LaunchedEffect(Unit) { intro.animateTo(1f, tween(1600, easing = FastOutSlowInEasing)) }
    // Each title pans in its own direction.
    val dirX = if (title.hashCode() and 1 == 0) 1f else -1f
    val dirY = if (title.hashCode() and 2 == 0) 1f else -1f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null,
    ) {
        Box(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().verticalScroll(scroll).imePadding()) {
            Box(Modifier.fillMaxWidth().height(330.dp).clipToBounds()) {
                if (cover != null) {
                    // Ken Burns: a barely blurred backdrop that pushes in on open, then keeps panning and zooming slowly.
                    AsyncImage(
                        model = cover,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer {
                                val scale = (1.3f + 0.3f * ken) * (1.35f - 0.35f * intro.value)
                                scaleX = scale
                                scaleY = scale
                                translationX = dirX * (ken - 0.5f) * size.width * 0.12f
                                translationY = dirY * (ken - 0.5f) * size.height * 0.14f + scroll.value * 0.5f
                                alpha = intro.value
                            }
                            .blur(4.dp, BlurredEdgeTreatment.Unbounded),
                    )
                } else {
                    Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(colors.primaryContainer, colors.secondaryContainer))))
                }
                Box(
                    Modifier
                        .matchParentSize()
                        .background(Brush.verticalGradient(colorStops = arrayOf(0f to Color(0x66000000), 0.55f to Color(0x1A000000), 1f to colors.surface))),
                )
                // A soft band of light that glides across every few seconds.
                Box(
                    Modifier
                        .matchParentSize()
                        .drawBehind {
                            val x = -size.width * 0.7f + sweep * size.width * 2.1f
                            drawRect(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, Color(0x0DFFFFFF), Color.Transparent),
                                    startX = x,
                                    endX = x + size.width * 0.7f,
                                ),
                            )
                        },
                )
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0x99FFFFFF)),
                )
                MediaPoster(
                    MediaItem(id = "", shelf = "", title = title, subtitle = "", cover = cover, rating = rating),
                    Modifier
                        .width(158.dp)
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                        .graphicsLayer {
                            // As the page scrolls the poster shrinks and fades while the pinned title bar fades in.
                            val shrink = 1f - 0.25f * collapse
                            scaleX = shrink
                            scaleY = shrink
                            alpha = (1f - collapse * 1.3f).coerceIn(0f, 1f)
                            translationY = (bob - 0.5f) * 10.dp.toPx() + scroll.value * 0.25f
                        },
                )
            }

            Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).staggeredEntrance(0)) {
                Spacer(Modifier.height(16.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                )
                if (facts.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        facts.forEach { fact ->
                            Text(
                                fact,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(colors.surfaceVariant.copy(alpha = 0.7f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.onSurfaceVariant,
                                maxLines = 2,
                            )
                        }
                    }
                }
                if (notes.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        notes.forEach { (label, value) ->
                            Row {
                                Text(label, modifier = Modifier.width(76.dp), fontSize = 13.sp, color = colors.onSurfaceVariant)
                                Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.onSurface)
                            }
                        }
                    }
                }
            }

            Column(
                Modifier.padding(horizontal = 22.dp).padding(top = 22.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                content()
                Button(
                    onClick = {
                        onSave()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp).staggeredEntrance(5),
                    shape = RoundedCornerShape(16.dp),
                ) { Text("Save", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Remove from library", color = colors.error)
                }
            }
            Spacer(Modifier.navigationBarsPadding().height(16.dp))
        }
        // Compact title bar that appears once the poster has scrolled away.
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .graphicsLayer { alpha = collapse }
                .background(colors.surface.copy(alpha = 0.97f))
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Remove this $noun?") },
            text = { Text("It disappears from your library and from your synced phones.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                    onDismiss()
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

/** A labelled block on a detail page; the card variant puts the controls on a soft panel. */
@Composable
fun DetailSection(title: String, card: Boolean = true, index: Int = 1, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().staggeredEntrance(index)) {
        Text(
            title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp, start = 2.dp),
        )
        if (card) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surfaceVariant.copy(alpha = 0.55f))
                    .padding(16.dp),
                content = content,
            )
        } else {
            Column(Modifier.fillMaxWidth(), content = content)
        }
    }
}
