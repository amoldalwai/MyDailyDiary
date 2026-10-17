package com.amoldalwai.diary.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amoldalwai.diary.data.DiaryEntry
import com.amoldalwai.diary.data.HomeStats
import kotlinx.coroutines.delay
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    entries: List<DiaryEntry>,
    loading: Boolean,
    loadingMore: Boolean,
    hasMore: Boolean,
    stats: HomeStats,
    weekDays: Set<Long>,
    onThisDay: List<DiaryEntry>,
    imageFile: (String) -> File,
    onNewEntry: () -> Unit,
    onOpenEntry: (DiaryEntry) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onLoadMore: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    SystemBarsStyle(darkIcons = !appIsDark())

    val today = remember { LocalDate.now() }
    val listState = rememberLazyListState()
    val animatedIds = remember { HashSet<String>() }

    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(nearEnd, entries.size, hasMore, loading) {
        if (nearEnd && hasMore && !loading) onLoadMore()
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(colors.background, colors.surfaceVariant)))) {
        FloatingBlobs()

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 20.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 120.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                Header(onOpenSearch, onOpenCalendar, onOpenInsights, onOpenSettings, Modifier.staggeredEntrance(0))
            }
            item(key = "stats") {
                Row(
                    Modifier.fillMaxWidth().staggeredEntrance(1),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatCard("📖", stats.total, "Entries", Modifier.weight(1f))
                    StatCard("🔥", stats.streak, "Day streak", Modifier.weight(1f))
                    StatCard("🌙", stats.thisMonth, "This month", Modifier.weight(1f))
                }
            }
            item(key = "week") { WeekStrip(today, weekDays, onDayClick, Modifier.staggeredEntrance(2)) }
            if (onThisDay.isNotEmpty()) {
                item(key = "onthisday") { OnThisDayCard(onThisDay, onOpenEntry, Modifier.staggeredEntrance(3)) }
            }
            item(key = "title") {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f).padding(top = 6.dp)) {
                        Text(
                            "Your memories",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onBackground,
                        )
                        Text(
                            "${stats.total} ${if (stats.total == 1) "page" else "pages"} so far",
                            fontSize = 13.sp,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    Ribbon(Modifier.padding(end = 18.dp))
                }
            }

            if (loading) {
                items(3) { ShimmerCard() }
            } else if (entries.isEmpty()) {
                item(key = "empty") { EmptyState() }
            }

            itemsIndexed(entries, key = { _, e -> e.id }) { index, entry ->
                val alreadyShown = remember { animatedIds.contains(entry.id) }
                animatedIds.add(entry.id)
                EntryCard(
                    entry = entry,
                    imageFile = imageFile,
                    onClick = { onOpenEntry(entry) },
                    modifier = Modifier.animateItem().paperDrop(index, skip = alreadyShown),
                )
            }

            if (loadingMore) {
                item(key = "more") { ShimmerCard() }
            } else if (!hasMore && entries.size > 3) {
                item(key = "end") {
                    Text(
                        "That's every page ✨",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        textAlign = TextAlign.Center,
                        color = colors.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        WriteFab(
            onClick = onNewEntry,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp),
        )
    }
}

@Composable
private fun Header(
    onOpenSearch: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hour = remember { LocalTime.now().hour }
    val greeting = when {
        hour < 5 -> "Late night thoughts"
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    val date = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())).uppercase()
    }
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LogoMark(Modifier.size(40.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "Diary",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.weight(1f))
            RoundIconButton(Icons.Rounded.Search, "Search", onOpenSearch)
            Spacer(Modifier.width(6.dp))
            RoundIconButton(Icons.Rounded.CalendarMonth, "Calendar", onOpenCalendar)
            Spacer(Modifier.width(6.dp))
            RoundIconButton(Icons.Rounded.BarChart, "Insights", onOpenInsights)
            Spacer(Modifier.width(6.dp))
            RoundIconButton(Icons.Rounded.Settings, "Settings", onOpenSettings)
        }
        Spacer(Modifier.height(22.dp))
        Text(
            date,
            style = MaterialTheme.typography.labelLarge,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            greeting,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "What's worth remembering today?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatCard(emoji: String, value: Int, label: String, modifier: Modifier = Modifier) {
    var target by remember { mutableIntStateOf(0) }
    LaunchedEffect(value) { target = value }
    val shown by animateIntAsState(target, tween(900, easing = FastOutSlowInEasing), label = "count")
    Column(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)), RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "$shown",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WeekStrip(
    today: LocalDate,
    daysWithEntries: Set<Long>,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(colors.surface.copy(alpha = 0.75f))
            .border(BorderStroke(1.dp, colors.outline.copy(alpha = 0.4f)), RoundedCornerShape(26.dp))
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        for (offset in 6 downTo 0) {
            val date = today.minusDays(offset.toLong())
            val hasEntry = date.toEpochDay() in daysWithEntries
            val isToday = offset == 0
            val pop = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                delay(300L + (6 - offset) * 60L)
                pop.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
            }
            Column(
                Modifier
                    .weight(1f)
                    .graphicsLayer {
                        scaleX = pop.value
                        scaleY = pop.value
                        alpha = pop.value.coerceIn(0f, 1f)
                    }
                    .bouncyClick { onDayClick(date) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (hasEntry) {
                                Brush.linearGradient(listOf(colors.primary, Color(0xFFE59A6B)))
                            } else {
                                Brush.linearGradient(listOf(colors.surfaceVariant, colors.surfaceVariant))
                            },
                        )
                        .then(if (isToday && !hasEntry) Modifier.border(2.dp, colors.primary, CircleShape) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${date.dayOfMonth}",
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                        color = if (hasEntry) colors.onPrimary else colors.onSurface,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    val transition = rememberInfiniteTransition(label = "float")
    val dy by transition.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dy",
    )
    Column(
        Modifier.fillMaxWidth().padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("📖", fontSize = 64.sp, modifier = Modifier.graphicsLayer { translationY = dy.dp.toPx() })
        Spacer(Modifier.height(16.dp))
        Text(
            "Your story starts here",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Tap “Write today” to capture your first memory.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OnThisDayCard(entries: List<DiaryEntry>, onOpen: (DiaryEntry) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(colors.tertiary.copy(alpha = 0.22f), colors.primary.copy(alpha = 0.18f))))
            .border(1.dp, colors.tertiary.copy(alpha = 0.3f), shape)
            .padding(18.dp),
    ) {
        Text(
            "✨ ON THIS DAY",
            fontSize = 11.sp,
            letterSpacing = 1.8.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurfaceVariant,
        )
        entries.take(3).forEach { entry ->
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().bouncyClick { onOpen(entry) }, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${entry.date.year}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        entry.title.ifBlank { entry.body.trim().ifBlank { "Untitled day" } },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurface,
                    )
                    val years = LocalDate.now().year - entry.date.year
                    Text(
                        "$years ${if (years == 1) "year" else "years"} ago",
                        fontSize = 12.sp,
                        color = colors.onSurfaceVariant,
                    )
                }
                entry.mood?.let { Text(it.emoji, fontSize = 20.sp) }
            }
        }
    }
}

@Composable
private fun WriteFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(500)
        shown = true
    }
    val scale by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "fabScale",
    )
    val transition = rememberInfiniteTransition(label = "fabFloat")
    val dy by transition.animateFloat(
        initialValue = 0f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "fabDy",
    )
    Row(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = dy.dp.toPx()
            }
            .bouncyClick(onClick)
            .shadow(14.dp, CircleShape, ambientColor = colors.primary, spotColor = colors.primary)
            .background(Brush.horizontalGradient(listOf(colors.primary, Color(0xFFE59A6B))), CircleShape)
            .padding(horizontal = 22.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Edit, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text("Write today", color = colors.onPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}
