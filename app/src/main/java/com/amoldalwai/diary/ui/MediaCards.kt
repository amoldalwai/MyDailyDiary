package com.amoldalwai.diary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File

/** A named group of items in a library: Reading, Completed, Watchlist and so on. */
data class MediaShelf(val key: String, val label: String)

/**
 * What every library section (books, games, later movies) reduces to for display:
 * one poster, two lines of text, an optional rating and an optional progress bar.
 */
data class MediaItem(
    val id: String,
    val shelf: String,
    val title: String,
    val subtitle: String,
    val cover: File?,
    val rating: Int?,
    val progress: Float? = null,
    val badge: String? = null,
    val kind: String? = null,
    val note: String? = null,
)

/** Netflix-style "match" colouring: gold for the best, green for good, amber for middling, red for poor. */
fun ratingColor(rating: Int) = when {
    rating >= 9 -> Color(0xFFFFC94D)
    rating >= 7 -> Color(0xFF46D369)
    rating >= 5 -> Color(0xFFFFA24C)
    else -> Color(0xFFFF5C5C)
}

/** A small frosted-glass pill used for the rating on shelf covers and the platform tag on cards. */
@Composable
fun GlassChip(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        modifier
            .clip(CircleShape)
            .background(Color(0x66000000))
            .border(1.dp, Color(0x33FFFFFF), CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

@Composable
fun RatingBadge(rating: Int, modifier: Modifier = Modifier) {
    GlassChip(modifier) {
        Text("★", color = ratingColor(rating), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(" $rating", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

private val infoPanelHeight = 70.dp

/**
 * The poster. With [info] it becomes a full card: the bottom of the cover is frosted (a blurred copy of the same image,
 * so it works like backdrop blur) and carries the title, the rating and any progress. Without it, only a rating chip is shown.
 */
@Composable
fun MediaPoster(item: MediaItem, modifier: Modifier = Modifier, info: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(if (info) 14.dp else 10.dp)
    Box(
        modifier
            .aspectRatio(0.68f)
            .shadow(if (info) 10.dp else 6.dp, shape)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(colors.primaryContainer, colors.secondaryContainer)))
            .border(1.dp, Color(0x24FFFFFF), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (item.cover != null) {
            AsyncImage(
                model = item.cover,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (info) {
                // Same image again, blurred, and revealed only through the bottom panel.
                AsyncImage(
                    model = item.cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            clipRect(top = size.height - infoPanelHeight.toPx()) { this@drawWithContent.drawContent() }
                        }
                        .blur(22.dp, BlurredEdgeTreatment.Rectangle),
                )
            }
        } else {
            Text(
                item.title,
                modifier = Modifier.padding(10.dp).padding(bottom = if (info) infoPanelHeight / 2 else 0.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                color = colors.onPrimaryContainer,
            )
        }

        if (info) {
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(infoPanelHeight)
                    .background(Brush.verticalGradient(listOf(Color(0x40000000), Color(0xB3000000)))),
            ) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x38FFFFFF)))
                Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        item.title,
                        color = Color.White,
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        minLines = 2,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val rating = item.rating
                        if (rating != null) {
                            Text("★", color = ratingColor(rating), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(" $rating", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("/10", color = Color(0x99FFFFFF), fontSize = 10.sp)
                        } else {
                            Text("Not rated", color = Color(0x99FFFFFF), fontSize = 11.sp)
                        }
                        Spacer(Modifier.weight(1f))
                        val note = item.note
                        if (note != null) {
                            Text(note, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        } else {
                            item.progress?.let {
                                Text("${(it * 100).toInt()}%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            item.progress?.let {
                ProgressBar(
                    it,
                    Color(0x33FFFFFF),
                    ratingColor(10),
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                )
            }
            item.badge?.let { GlassChip(Modifier.align(Alignment.TopStart).padding(8.dp)) { Text(it, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) } }
        } else {
            item.progress?.let { progress ->
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xAA000000))))
                        .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
                ) {
                    ProgressBar(progress, Color(0x55FFFFFF), Color.White, Modifier.fillMaxWidth())
                }
            }
            item.rating?.let { RatingBadge(it, Modifier.align(Alignment.TopEnd).padding(6.dp)) }
        }
    }
}

/** Netflix-style card: one frosted-glass poster carrying the title and rating. */
@Composable
fun MediaCard(item: MediaItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    MediaPoster(item, modifier.bouncyClick(onClick), info = true)
}
