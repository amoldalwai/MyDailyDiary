package com.amoldalwai.diary.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amoldalwai.diary.data.InsightStats
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InsightsScreen(stats: InsightStats?, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    SystemBarsStyle(darkIcons = !appIsDark())
    val progress = remember { Animatable(0f) }
    LaunchedEffect(stats != null) {
        if (stats != null) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(1000, easing = FastOutSlowInEasing))
        }
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(colors.background, colors.surfaceVariant)))) {
        FloatingBlobs()
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = colors.onBackground) }
                Text("Insights", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = colors.onBackground)
            }

            if (stats == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.primary)
                }
            } else if (stats.total == 0) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("📊", fontSize = 56.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Write a few entries to see insights", color = colors.onSurfaceVariant)
                }
            } else {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth().staggeredEntrance(0), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Tile("📖", stats.total, "Entries", progress.value, Modifier.weight(1f))
                        Tile("✍️", stats.words, "Words written", progress.value, Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth().staggeredEntrance(1), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Tile("🔥", stats.current, "Current streak", progress.value, Modifier.weight(1f))
                        Tile("🏆", stats.longest, "Longest streak", progress.value, Modifier.weight(1f))
                    }

                    InsightCard("Moods", Modifier.staggeredEntrance(2)) {
                        if (stats.moods.isEmpty()) {
                            Text("Pick a mood on your entries to see them here.", color = colors.onSurfaceVariant, fontSize = 14.sp)
                        } else {
                            val top = stats.moods.first().second
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                stats.moods.forEach { (mood, count) ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(mood.emoji, fontSize = 20.sp)
                                        Spacer(Modifier.width(8.dp))
                                        Text(mood.label, modifier = Modifier.width(72.dp), fontSize = 14.sp, color = colors.onSurface)
                                        Box(
                                            Modifier
                                                .weight(1f)
                                                .height(14.dp)
                                                .clip(CircleShape)
                                                .background(colors.surfaceVariant),
                                        ) {
                                            Box(
                                                Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(count.toFloat() / top * progress.value)
                                                    .clip(CircleShape)
                                                    .background(Brush.horizontalGradient(listOf(colors.primary, Color(0xFFE59A6B)))),
                                            )
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Text("$count", fontWeight = FontWeight.SemiBold, color = colors.onSurface, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }

                    InsightCard("Last 8 weeks", Modifier.staggeredEntrance(3)) {
                        val maxCount = max(stats.weekly.max(), 1)
                        Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            stats.weekly.forEachIndexed { i, count ->
                                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(if (count == 0) 0.03f else (count.toFloat() / maxCount * progress.value).coerceAtLeast(0.03f))
                                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                                .background(if (i == 7) colors.primary else colors.primary.copy(alpha = 0.45f)),
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        if (i == 7) "now" else "${7 - i}w",
                                        fontSize = 10.sp,
                                        color = colors.onSurfaceVariant,
                                    )
                                    Text(
                                        "${(count * progress.value).roundToInt()}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.onSurface,
                                    )
                                }
                            }
                        }
                    }

                    if (stats.tags.isNotEmpty()) {
                        InsightCard("Top tags", Modifier.staggeredEntrance(4)) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                stats.tags.forEach { (tag, count) ->
                                    Text(
                                        "#$tag · $count",
                                        fontSize = 13.sp,
                                        color = colors.onPrimaryContainer,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(colors.primaryContainer)
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        "📷 ${stats.photos} ${if (stats.photos == 1) "photo" else "photos"} saved in your memories",
                        modifier = Modifier.padding(horizontal = 4.dp),
                        color = colors.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun Tile(emoji: String, value: Int, label: String, progress: Float, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface.copy(alpha = 0.8f))
            .border(1.dp, colors.outline.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
            .padding(16.dp),
    ) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "${(value * progress).roundToInt()}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface,
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun InsightCard(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(colors.surface.copy(alpha = 0.8f))
            .border(1.dp, colors.outline.copy(alpha = 0.4f), RoundedCornerShape(26.dp))
            .padding(18.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
        Spacer(Modifier.height(14.dp))
        content()
    }
}
