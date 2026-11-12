package com.amoldalwai.diary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amoldalwai.diary.data.DiaryEntry
import java.io.File
import java.time.format.TextStyle
import java.util.Locale

private val fanTilts = listOf(-6f, 4f, 10f)
private val fanDrops = listOf(0.dp, 5.dp, 2.dp)

/** A diary page "stuck" to the desk: washi tape, tear-off date tab, stitched margin, folded corner and a fan of photos. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EntryCard(
    entry: DiaryEntry,
    imageFile: (String) -> File,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selecting: Boolean = false,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    val bg = PageStyles.background(entry.style.backgroundId)
    val font = PageStyles.font(entry.style.fontId)
    val primary = MaterialTheme.colorScheme.primary
    val hash = remember(entry.id) { entry.id.hashCode() }
    val tilt = remember(hash) { ((hash and 0xff) % 5 - 2) * 0.45f }
    val shape = RoundedCornerShape(topStart = 10.dp, topEnd = 26.dp, bottomEnd = 6.dp, bottomStart = 10.dp)
    val chipColor = if (bg.isDark) Color.Black.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.6f)
    val date = entry.date
    val preview = remember(entry.body) { entry.body.trim() }

    Box(
        modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .graphicsLayer { rotationZ = tilt }
            .bouncyClick(onLongClick = { onLongClick?.invoke() }, onClick = onClick),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .shadow(8.dp, shape, ambientColor = Color.Black.copy(alpha = 0.18f), spotColor = Color.Black.copy(alpha = 0.25f))
                .background(bg.brush, shape)
                .border(
                    if (selected) 3.dp else 1.dp,
                    if (selected) primary else bg.ink.copy(alpha = 0.08f),
                    shape,
                )
                .drawBehind {
                    val stitchX = 73.dp.toPx()
                    drawLine(
                        bg.ink.copy(alpha = 0.22f),
                        Offset(stitchX, 20.dp.toPx()),
                        Offset(stitchX, size.height - 20.dp.toPx()),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                    )
                    val fold = 26.dp.toPx()
                    val flap = Path().apply {
                        moveTo(size.width - fold, size.height)
                        lineTo(size.width, size.height - fold)
                        lineTo(size.width - fold, size.height - fold)
                        close()
                    }
                    drawPath(flap, lerp(bg.bottom, Color.White, 0.5f))
                    drawLine(
                        bg.ink.copy(alpha = 0.18f),
                        Offset(size.width - fold, size.height),
                        Offset(size.width, size.height - fold),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                .padding(start = 14.dp, top = 24.dp, end = 20.dp, bottom = 20.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                Modifier
                    .width(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(chipColor),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                    modifier = Modifier.fillMaxWidth().background(primary).padding(vertical = 3.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${date.dayOfMonth}",
                    modifier = Modifier.padding(top = 4.dp),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = bg.ink,
                )
                Text(
                    date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    modifier = Modifier.padding(bottom = 6.dp),
                    fontSize = 11.sp,
                    color = bg.inkSoft,
                )
            }

            Spacer(Modifier.width(20.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        entry.title.ifBlank { "Untitled day" },
                        modifier = Modifier.weight(1f),
                        fontFamily = font.family,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 26.sp,
                        color = bg.ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    entry.mood?.let {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier.size(36.dp).rotate(8f).clip(CircleShape).background(chipColor),
                            contentAlignment = Alignment.Center,
                        ) { MoodFace(it, 30.dp) }
                    }
                }
                if (preview.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        preview,
                        fontFamily = font.family,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = bg.ink.copy(alpha = 0.82f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (entry.tags.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        entry.tags.take(3).forEach { tag ->
                            Text(
                                "#$tag",
                                fontSize = 11.sp,
                                maxLines = 1,
                                color = bg.ink.copy(alpha = 0.75f),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(bg.ink.copy(alpha = 0.09f))
                                    .padding(horizontal = 9.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
                if (entry.images.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    PhotoFan(entry.images, imageFile, chipColor, bg.ink)
                }
            }
        }
        if (selecting) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 6.dp, top = 6.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (selected) primary else Color.White.copy(alpha = 0.85f))
                    .border(2.dp, if (selected) primary else bg.ink.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Icon(Icons.Rounded.Check, "Selected", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(17.dp))
            }
        }
    }
}

/** Up to three photos stacked like prints dropped on a desk, with a "+N" badge for the rest. */
@Composable
private fun PhotoFan(images: List<String>, imageFile: (String) -> File, badgeColor: Color, ink: Color) {
    val shown = images.take(3)
    Box(Modifier.fillMaxWidth().height(76.dp)) {
        shown.indices.reversed().forEach { i ->
            Stamp(
                imageFile(shown[i]),
                Modifier.width(58.dp).offset(x = (i * 30).dp, y = fanDrops[i]),
                rotation = fanTilts[i],
                compact = true,
            )
        }
        if (images.size > shown.size) {
            Text(
                "+${images.size - shown.size}",
                modifier = Modifier
                    .offset(x = (shown.size * 30 + 38).dp, y = 22.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = ink,
            )
        }
    }
}
