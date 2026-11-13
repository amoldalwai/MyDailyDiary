package com.amoldalwai.diary.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amoldalwai.diary.data.LevelInfo
import com.amoldalwai.diary.data.Progress
import java.io.File
import kotlin.math.sin

private sealed interface PathRow {
    data class Header(val rankIndex: Int) : PathRow
    data class Node(val level: Int) : PathRow
}

private val ROW_HEIGHT = 92.dp

private fun nodeX(level: Int): Float = 0.5f + 0.27f * sin(level * 0.9f)

/** A Candy Crush style map: a winding path of levels that climbs through ten ranks, with you marked on it. */
@Composable
fun LevelPathScreen(level: LevelInfo, emoji: String, photo: File?, pokemonEnabled: Boolean, onPokemon: () -> Unit, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    SystemBarsStyle(darkIcons = !appIsDark())
    val rows = remember {
        buildList {
            for (l in 1..Progress.MAX_LEVEL) {
                if ((l - 1) % Progress.LEVELS_PER_RANK == 0) add(PathRow.Header(Progress.rankIndex(l)))
                add(PathRow.Node(l))
            }
        }
    }
    val state = rememberLazyListState()
    LaunchedEffect(level.level) {
        val index = rows.indexOfFirst { it is PathRow.Node && it.level == level.level }
        state.scrollToItem((index - 2).coerceAtLeast(0))
    }
    val rank = Progress.rankOf(level.level)

    Box(Modifier.fillMaxSize().background(colors.background)) {
        FloatingBlobs()
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = colors.onBackground) }
                Column(Modifier.weight(1f)) {
                    Text("Level journey", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onBackground)
                    Text(
                        "${rank.emoji} ${rank.name} · ${level.points} XP" + (level.nextAt?.let { " · ${it - level.points} to next" } ?: ""),
                        fontSize = 12.sp,
                        color = colors.onSurfaceVariant,
                    )
                }
                if (pokemonEnabled) {
                    Row(
                        Modifier.clip(CircleShape).background(colors.surfaceVariant).bouncyClick(onPokemon).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PokeballIcon(20.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("Poké Balls", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                    }
                    Spacer(Modifier.width(8.dp))
                }
            }
            LazyColumn(Modifier.fillMaxSize(), state = state, reverseLayout = true) {
                items(rows.size) { i ->
                    when (val row = rows[i]) {
                        is PathRow.Header -> RankHeader(row.rankIndex, reached = row.rankIndex <= Progress.rankIndex(level.level))
                        is PathRow.Node -> LevelNode(row.level, level, emoji, photo)
                    }
                }
            }
        }
    }
}

@Composable
private fun RankHeader(rankIndex: Int, reached: Boolean) {
    val rank = Progress.ranks[rankIndex]
    val from = rankIndex * Progress.LEVELS_PER_RANK + 1
    Box(Modifier.fillMaxWidth().height(86.dp).alpha(if (reached) 1f else 0.5f), contentAlignment = Alignment.Center) {
        Column(
            Modifier.clip(RoundedCornerShape(22.dp)).background(rankBrush(rank)).padding(horizontal = 26.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("${rank.emoji}  ${rank.name}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text(
                if (reached) "Levels $from–${from + Progress.LEVELS_PER_RANK - 1} · banner unlocked" else "Levels $from–${from + Progress.LEVELS_PER_RANK - 1} · 🔒 banner & frame",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun LevelNode(node: Int, info: LevelInfo, emoji: String, photo: File?) {
    val colors = MaterialTheme.colorScheme
    val current = info.level
    val rank = Progress.rankOf(node)
    val done = node < current
    val here = node == current
    val frac = nodeX(node)
    val nextFrac = nodeX(node + 1)
    val track = colors.outline.copy(alpha = 0.55f)
    val gold = Color(rank.to)
    val pulse = rememberInfiniteTransition(label = "pulse")
    val glow by pulse.animateFloat(1f, 1.18f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "glow")

    BoxWithConstraints(
        Modifier.fillMaxWidth().height(ROW_HEIGHT).drawBehind {
            if (node < Progress.MAX_LEVEL) {
                val from = Offset(size.width * frac, size.height / 2)
                // The first level of the next rank sits above a rank banner, so the line has further to go.
                val gap = if (node % Progress.LEVELS_PER_RANK == 0) 86.dp.toPx() else 0f
                val to = Offset(size.width * nextFrac, -size.height / 2 - gap)
                drawLine(
                    color = if (done) gold else track,
                    start = from,
                    end = to,
                    strokeWidth = 7.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = if (done) null else PathEffect.dashPathEffect(floatArrayOf(2f, 18f)),
                )
            }
        },
    ) {
        val nodeSize = if (here) 72.dp else 54.dp
        val center = maxWidth * frac
        Box(
            Modifier.offset(x = center - nodeSize / 2, y = (ROW_HEIGHT - nodeSize) / 2).size(nodeSize),
            contentAlignment = Alignment.Center,
        ) {
            if (here) {
                Box(Modifier.size(nodeSize).graphicsLayer { scaleX = glow; scaleY = glow }.clip(CircleShape).background(gold.copy(alpha = 0.28f)))
                AvatarRing(emoji, photo, info, nodeSize)
            } else {
                Box(
                    Modifier.size(nodeSize).clip(CircleShape)
                        .then(if (done) Modifier.background(rankBrush(rank)) else Modifier.background(colors.surfaceVariant).border(BorderStroke(2.dp, colors.outline), CircleShape)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (done) "✓" else "$node",
                        color = if (done) Color.White else colors.onSurfaceVariant,
                        fontWeight = FontWeight.Black,
                        fontSize = if (done) 22.sp else 17.sp,
                    )
                }
            }
        }
        val labelWidth = 104.dp
        val onLeft = frac > 0.5f
        Column(
            Modifier.offset(
                x = if (onLeft) center - nodeSize / 2 - 10.dp - labelWidth else center + nodeSize / 2 + 10.dp,
                y = ROW_HEIGHT / 2 - 18.dp,
            ).width(labelWidth),
            horizontalAlignment = if (onLeft) Alignment.End else Alignment.Start,
        ) {
            Text(
                if (here) "You are here" else "Level $node",
                fontWeight = if (here) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 13.sp,
                color = if (here) gold else colors.onBackground,
                textAlign = if (onLeft) TextAlign.End else TextAlign.Start,
            )
            Text("${Progress.pointsFor(node)} XP", fontSize = 11.sp, color = colors.onSurfaceVariant)
        }
    }
}
