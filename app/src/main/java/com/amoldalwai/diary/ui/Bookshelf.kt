package com.amoldalwai.diary.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val woodLight = Color(0xFFB98254)
private val woodDark = Color(0xFF7A4B2A)
private val wallTop = Color(0xFF5B4030).copy(alpha = 0.14f)
private val wallBottom = Color(0xFF5B4030).copy(alpha = 0.30f)

/**
 * A wooden bookshelf holding [items] [perRow] to a row; each row stands on its own plank.
 * Pressing a cover slides it up off the plank like pulling a book out.
 */
@Composable
fun Bookshelf(
    label: String?,
    items: List<MediaItem>,
    firstIndex: Int,
    onClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    perRow: Int = 3,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(wallTop, wallBottom)))
            .padding(bottom = 8.dp),
    ) {
        if (label != null) {
            Text(
                label.uppercase(),
                modifier = Modifier
                    .padding(start = 14.dp, top = 14.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Brush.horizontalGradient(listOf(woodLight, woodDark)))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                color = Color(0xFFFFF3E0),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            )
        }
        items.chunked(perRow).forEachIndexed { rowIndex, rowItems ->
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                rowItems.forEachIndexed { i, item ->
                    ShelfBook(item, firstIndex + rowIndex * perRow + i, Modifier.weight(1f)) { onClick(item) }
                }
                repeat(perRow - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
            Plank()
        }
    }
}

@Composable
private fun Plank() {
    val shape = RoundedCornerShape(3.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .height(14.dp)
            .shadow(5.dp, shape)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(woodLight, woodDark))),
    ) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0x33FFFFFF)))
    }
}

@Composable
private fun ShelfBook(item: MediaItem, index: Int, modifier: Modifier, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val spec = spring<Float>(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
    val lift by animateDpAsState(
        if (pressed) (-14).dp else 0.dp,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "bookLift",
    )
    val scale by animateFloatAsState(if (pressed) 1.07f else 1f, spec, label = "bookScale")
    // A tiny per-book tilt so the row looks shelved by hand rather than stamped out.
    val tilt = ((item.id.hashCode() % 5) - 2) * 0.5f
    Box(
        modifier
            .staggeredEntrance(index)
            .offset(y = lift)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = if (pressed) 0f else tilt
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
            .pointerInput(item.id) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { onClick() },
                )
            },
    ) {
        MediaPoster(item, Modifier.fillMaxWidth())
    }
}
