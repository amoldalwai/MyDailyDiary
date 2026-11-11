package com.amoldalwai.diary.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amoldalwai.diary.data.DayMarker
import com.amoldalwai.diary.data.HabitStat
import com.amoldalwai.diary.data.InsightStats
import com.amoldalwai.diary.data.LevelInfo
import com.amoldalwai.diary.data.Mood
import com.amoldalwai.diary.data.Progress
import com.amoldalwai.diary.data.ProgressStats
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

private fun moodColor(m: Mood): Color = when (m) {
    Mood.Happy -> Color(0xFFF7C948)
    Mood.Loved -> Color(0xFFF28DA6)
    Mood.Excited -> Color(0xFFF7A35C)
    Mood.Calm -> Color(0xFF7BC8A4)
    Mood.Tired -> Color(0xFF9DA8B7)
    Mood.Sad -> Color(0xFF6EA8E5)
    Mood.Anxious -> Color(0xFFC09AE8)
    Mood.Angry -> Color(0xFFE8806A)
}

private fun moodScore(m: Mood): Int = when (m) {
    Mood.Happy, Mood.Loved, Mood.Excited -> 5
    Mood.Calm -> 4
    Mood.Tired -> 3
    Mood.Sad, Mood.Anxious -> 2
    Mood.Angry -> 1
}

private const val HEAT_WEEKS = 26

/** A year of your journal at a glance: streak and level, mood over time, a day-by-day map, your rhythm, habits and library. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InsightsScreen(
    stats: InsightStats?,
    yearMarkers: Map<Long, DayMarker>,
    habitStats: List<HabitStat>,
    library: ProgressStats,
    level: LevelInfo,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    SystemBarsStyle(darkIcons = !appIsDark())
    val progress = remember { Animatable(0f) }
    LaunchedEffect(stats != null) {
        if (stats != null) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
        }
    }
    val today = remember { LocalDate.now() }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(colors.background, colors.surfaceVariant)))) {
        FloatingBlobs()
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = colors.onBackground) }
                Column {
                    Text("Insights", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = colors.onBackground)
                    Text("Your days, at a glance", fontSize = 12.sp, color = colors.onSurfaceVariant)
                }
            }

            if (stats == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = colors.primary) }
            } else if (stats.total == 0) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("📊", fontSize = 56.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Write a few entries to see insights", color = colors.onSurfaceVariant)
                }
            } else {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Spacer(Modifier.height(2.dp))
                    HeroCard(stats, level, progress.value, Modifier.staggeredEntrance(0))
                    MonthCard(stats, yearMarkers, today, Modifier.staggeredEntrance(1))
                    MoodCard(stats, yearMarkers, today, progress.value, Modifier.staggeredEntrance(2))
                    HeatmapCard(yearMarkers, today, progress.value, Modifier.staggeredEntrance(3))
                    RhythmCard(yearMarkers, progress.value, Modifier.staggeredEntrance(4))
                    if (habitStats.isNotEmpty()) HabitsCard(habitStats, progress.value, Modifier.staggeredEntrance(5))
                    LibraryCard(library, level, Modifier.staggeredEntrance(6))
                    if (stats.tags.isNotEmpty()) {
                        InsightCard("Top tags", Modifier.staggeredEntrance(6)) {
                            val max = stats.tags.maxOf { it.second }.coerceAtLeast(1)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                stats.tags.forEach { (tag, count) ->
                                    Text(
                                        "#$tag",
                                        fontSize = (12 + 6f * count / max).sp,
                                        fontWeight = if (count * 2 >= max) FontWeight.SemiBold else FontWeight.Normal,
                                        color = colors.onPrimaryContainer,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(colors.primaryContainer.copy(alpha = 0.55f + 0.45f * count / max))
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun HeroCard(stats: InsightStats, level: LevelInfo, progress: Float, modifier: Modifier = Modifier) {
    val rank = Progress.rankOf(level.level)
    val from = Color(rank.from)
    val to = Color(rank.to)
    val drift = rememberInfiniteTransition(label = "heroDrift")
    val phase by drift.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse), label = "phase")
    val shape = RoundedCornerShape(30.dp)
    val line = when {
        stats.current >= 7 -> "A whole week and more. Keep the thread going."
        stats.current >= 3 -> "You're on a roll."
        stats.current >= 1 -> "A streak has started. Write tomorrow to grow it."
        else -> "Write a page today to start a streak."
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .drawBehind {
                drawRect(Brush.linearGradient(listOf(from, to, from), Offset(size.width * phase * 0.6f, 0f), Offset(size.width * (1f - phase * 0.4f), size.height)))
                drawCircle(Color.White.copy(alpha = 0.14f), size.width * 0.45f, Offset(size.width * (0.95f - 0.3f * phase), size.height * 0.1f))
                drawCircle(Color.White.copy(alpha = 0.10f), size.width * 0.3f, Offset(size.width * (0.1f + 0.2f * phase), size.height * 1.0f))
            }
            .padding(22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("🔥 Current streak", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${(stats.current * progress).roundToInt()}", fontSize = 64.sp, fontWeight = FontWeight.Black, color = Color.White, lineHeight = 66.sp)
                    Text(" ${if (stats.current == 1) "day" else "days"}", fontSize = 18.sp, color = Color.White.copy(alpha = 0.9f), modifier = Modifier.padding(bottom = 10.dp))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${rank.emoji} Level ${level.level}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(rank.name, fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                Spacer(Modifier.height(10.dp))
                Text("🏆 Best ${stats.longest}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(line, fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f))
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.16f)).padding(vertical = 12.dp),
        ) {
            HeroStat("${(stats.total * progress).roundToInt()}", "pages", Modifier.weight(1f))
            HeroStat("${(stats.words * progress).roundToInt()}", "words", Modifier.weight(1f))
            HeroStat("${(stats.photos * progress).roundToInt()}", "photos", Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeroStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
    }
}

@Composable
private fun MonthCard(stats: InsightStats, markers: Map<Long, DayMarker>, today: LocalDate, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val thisMonth = markers.entries.sumOf { (day, m) -> if (LocalDate.ofEpochDay(day).let { it.year == today.year && it.month == today.month }) m.count else 0 }
    val previous = today.minusMonths(1)
    val lastMonth = markers.entries.sumOf { (day, m) -> if (LocalDate.ofEpochDay(day).let { it.year == previous.year && it.month == previous.month }) m.count else 0 }
    val delta = thisMonth - lastMonth
    val avgWords = if (stats.total == 0) 0 else stats.words / stats.total
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SmallCard(Modifier.weight(1f).fillMaxHeight()) {
            Text("This month", fontSize = 12.sp, color = colors.onSurfaceVariant)
            Text("$thisMonth ${if (thisMonth == 1) "page" else "pages"}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onSurface)
            Spacer(Modifier.height(6.dp))
            val (label, tint) = when {
                delta > 0 -> "▲ $delta vs last month" to Color(0xFF2F8F6B)
                delta < 0 -> "▼ ${-delta} vs last month" to Color(0xFFD4694F)
                else -> "Same as last month" to colors.onSurfaceVariant
            }
            Text(label, fontSize = 12.sp, color = tint, fontWeight = FontWeight.SemiBold)
        }
        SmallCard(Modifier.weight(1f).fillMaxHeight()) {
            Text("Average page", fontSize = 12.sp, color = colors.onSurfaceVariant)
            Text("$avgWords words", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onSurface)
            Spacer(Modifier.height(6.dp))
            Text("${stats.words} written in total", fontSize = 12.sp, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun MoodCard(stats: InsightStats, markers: Map<Long, DayMarker>, today: LocalDate, progress: Float, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    InsightCard("Moods", modifier) {
        if (stats.moods.isEmpty()) {
            Text("Pick a mood on your entries to see them here.", color = colors.onSurfaceVariant, fontSize = 14.sp)
            return@InsightCard
        }
        val top = stats.moods.first()
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(moodColor(top.first).copy(alpha = 0.2f)).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(top.first.emoji, fontSize = 34.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Mostly ${top.first.label.lowercase()}", fontWeight = FontWeight.Bold, color = colors.onSurface, fontSize = 16.sp)
                val total = stats.moods.sumOf { it.second }.coerceAtLeast(1)
                Text("${top.second} of $total entries, ${(100f * top.second / total).roundToInt()}%", fontSize = 12.sp, color = colors.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(14.dp))
        val max = top.second
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            stats.moods.forEach { (mood, count) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(mood.emoji, fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(mood.label, modifier = Modifier.width(66.dp), fontSize = 13.sp, color = colors.onSurface)
                    Box(Modifier.weight(1f).height(12.dp).clip(CircleShape).background(colors.surfaceVariant)) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(count.toFloat() / max * progress)
                                .clip(CircleShape)
                                .background(moodColor(mood)),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("$count", fontWeight = FontWeight.SemiBold, color = colors.onSurface, fontSize = 13.sp)
                }
            }
        }

        // Average mood per week over the last eight weeks.
        val buckets = Array(8) { mutableListOf<Int>() }
        markers.forEach { (day, m) ->
            val mood = m.mood ?: return@forEach
            val weeksAgo = ((today.toEpochDay() - day) / 7).toInt()
            if (weeksAgo in 0..7) buckets[7 - weeksAgo] += moodScore(mood)
        }
        val averages = buckets.map { if (it.isEmpty()) null else it.average().toFloat() }
        if (averages.count { it != null } >= 2) {
            Spacer(Modifier.height(18.dp))
            Text("Mood over 8 weeks", fontWeight = FontWeight.SemiBold, color = colors.onSurface, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().height(110.dp)) {
                Column(Modifier.fillMaxHeight().width(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Text("😊", fontSize = 13.sp)
                    Text("😐", fontSize = 13.sp)
                    Text("😢", fontSize = 13.sp)
                }
                MoodLine(averages, progress, Modifier.weight(1f).fillMaxHeight())
            }
            Row(Modifier.fillMaxWidth().padding(start = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("7w ago", fontSize = 10.sp, color = colors.onSurfaceVariant)
                Text("this week", fontSize = 10.sp, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MoodLine(values: List<Float?>, progress: Float, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val line = colors.primary
    val grid = colors.outline.copy(alpha = 0.3f)
    Canvas(modifier) {
        val padY = 8.dp.toPx()
        val h = size.height - padY * 2
        for (i in 0..4) drawLine(grid, Offset(0f, padY + h * i / 4f), Offset(size.width, padY + h * i / 4f), 1.dp.toPx())
        fun point(i: Int, v: Float) = Offset(size.width * (0.03f + 0.94f * i / 7f), padY + h * (1f - (v - 1f) / 4f))
        val points = values.mapIndexedNotNull { i, v -> v?.let { i to point(i, it) } }
        if (points.size < 2) return@Canvas
        clipRect(right = size.width * progress) {
            val path = Path().apply {
                moveTo(points.first().second.x, points.first().second.y)
                points.drop(1).forEach { lineTo(it.second.x, it.second.y) }
            }
            val area = Path().apply {
                addPath(path)
                lineTo(points.last().second.x, size.height)
                lineTo(points.first().second.x, size.height)
                close()
            }
            drawPath(area, Brush.verticalGradient(listOf(line.copy(alpha = 0.28f), Color.Transparent)))
            drawPath(path, line, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            points.forEach { (_, p) ->
                drawCircle(Color.White, 5.dp.toPx(), p)
                drawCircle(line, 3.dp.toPx(), p)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeatmapCard(markers: Map<Long, DayMarker>, today: LocalDate, progress: Float, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val start = remember(today) { today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks((HEAT_WEEKS - 1).toLong()) }
    val empty = colors.outline.copy(alpha = 0.22f)
    val plain = colors.primary.copy(alpha = 0.75f)
    val filled = markers.count { LocalDate.ofEpochDay(it.key) >= start }
    InsightCard("Your days in pixels", modifier) {
        Text("$filled days written in 6 months, by mood", fontSize = 12.sp, color = colors.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val gap = 3.dp
            val cell = (maxWidth - gap * (HEAT_WEEKS - 1)) / HEAT_WEEKS
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (c in 0 until HEAT_WEEKS) {
                        val first = start.plusWeeks(c.toLong())
                        val label = if (first.dayOfMonth <= 7) first.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3) else ""
                        Box(Modifier.width(cell)) {
                            Text(label, fontSize = 9.sp, color = colors.onSurfaceVariant, maxLines = 1, softWrap = false, modifier = Modifier.wrapContentWidth(Alignment.Start, unbounded = true))
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Canvas(Modifier.fillMaxWidth().height(cell * 7 + gap * 6)) {
                    val cellPx = cell.toPx()
                    val gapPx = gap.toPx()
                    for (c in 0 until HEAT_WEEKS) {
                        for (r in 0 until 7) {
                            val date = start.plusDays((c * 7 + r).toLong())
                            if (date.isAfter(today)) continue
                            val marker = markers[date.toEpochDay()]
                            val color = when {
                                marker == null -> empty
                                marker.mood != null -> moodColor(marker.mood)
                                else -> plain
                            }
                            // Columns appear left to right as the card animates in.
                            val reveal = ((progress * (HEAT_WEEKS + 2) - c) / 2f).coerceIn(0f, 1f)
                            val inset = cellPx * (1f - reveal) / 2f
                            drawRoundRect(
                                color,
                                Offset(c * (cellPx + gapPx) + inset, r * (cellPx + gapPx) + inset),
                                Size(cellPx - inset * 2, cellPx - inset * 2),
                                CornerRadius(cellPx * 0.28f),
                            )
                            if (date == today) {
                                drawRoundRect(
                                    colors.onSurface.copy(alpha = 0.7f),
                                    Offset(c * (cellPx + gapPx), r * (cellPx + gapPx)),
                                    Size(cellPx, cellPx),
                                    CornerRadius(cellPx * 0.28f),
                                    style = Stroke(1.5.dp.toPx()),
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Mood.entries.forEach { m ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(9.dp).clip(RoundedCornerShape(3.dp)).background(moodColor(m)))
                    Spacer(Modifier.width(4.dp))
                    Text(m.label, fontSize = 10.sp, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun RhythmCard(markers: Map<Long, DayMarker>, progress: Float, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val counts = IntArray(7)
    markers.forEach { (day, m) -> counts[LocalDate.ofEpochDay(day).dayOfWeek.value - 1] += m.count }
    val best = counts.indices.maxByOrNull { counts[it] } ?: 0
    val max = counts.max().coerceAtLeast(1)
    InsightCard("Your rhythm", modifier) {
        if (counts.all { it == 0 }) {
            Text("Write for a few days to see when you write most.", color = colors.onSurfaceVariant, fontSize = 14.sp)
            return@InsightCard
        }
        Text(
            "You write most on ${DayOfWeek.of(best + 1).getDisplayName(TextStyle.FULL, Locale.getDefault())}s",
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface,
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            counts.forEachIndexed { i, n ->
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight((n.toFloat() / max * progress).coerceAtLeast(0.04f))
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(if (i == best) Brush.verticalGradient(listOf(Color(0xFFF7C948), Color(0xFFE59A2B))) else Brush.verticalGradient(listOf(colors.primary.copy(alpha = 0.55f), colors.primary.copy(alpha = 0.4f)))),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        DayOfWeek.of(i + 1).getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        fontSize = 11.sp,
                        fontWeight = if (i == best) FontWeight.Bold else FontWeight.Normal,
                        color = if (i == best) colors.onSurface else colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitsCard(habitStats: List<HabitStat>, progress: Float, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    InsightCard("Habits", modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            habitStats.forEach { stat ->
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stat.habit.emoji, fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(stat.habit.name, color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Text("${(stat.doneLast30 / 30f * 100 * progress).roundToInt()}%", fontWeight = FontWeight.Bold, color = colors.primary, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        stat.last28.forEach { done ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (done) colors.primary else colors.outline.copy(alpha = 0.22f)),
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "🔥 ${stat.currentStreak} day streak  ·  best ${stat.bestStreak}  ·  ${stat.doneLast30}/30 days",
                        fontSize = 12.sp,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryCard(library: ProgressStats, level: LevelInfo, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    InsightCard("Library and progress", modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LibraryTile("📚", library.booksFinished, "books read", Modifier.weight(1f))
            LibraryTile("🎮", library.gamesCompleted, "games done", Modifier.weight(1f))
            LibraryTile("🎬", library.moviesWatched, "movies", Modifier.weight(1f))
            LibraryTile("📺", library.seriesFinished, "series", Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        val next = level.nextAt
        Text(
            if (next == null) "${level.points} XP · top level reached" else "${level.points} XP · ${next - level.points} to level ${level.level + 1}",
            fontSize = 13.sp,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        ProgressBar(level.progress, colors.outline.copy(alpha = 0.3f), Color(Progress.rankOf(level.level).to), Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Text(
            "✅ ${library.perfectHabitDays} perfect days  ·  ⭐ ${library.ratings} ratings  ·  ✍️ ${library.reviews} reviews",
            fontSize = 12.sp,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun LibraryTile(emoji: String, value: Int, label: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(colors.primaryContainer.copy(alpha = 0.55f)).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 20.sp)
        Text("$value", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.onSurface)
        Text(label, fontSize = 10.sp, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SmallCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .clip(shape)
            .background(colors.surface.copy(alpha = 0.85f))
            .border(BorderStroke(1.dp, colors.outline.copy(alpha = 0.4f)), shape)
            .padding(16.dp),
    ) { content() }
}

@Composable
private fun InsightCard(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface.copy(alpha = 0.85f))
            .border(BorderStroke(1.dp, colors.outline.copy(alpha = 0.4f)), shape)
            .padding(18.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
        Spacer(Modifier.height(14.dp))
        content()
    }
}
