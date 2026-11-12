package com.amoldalwai.diary.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amoldalwai.diary.data.DayMarker
import com.amoldalwai.diary.data.DiaryEntry
import com.amoldalwai.diary.util.PdfExporter
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun CalendarScreen(
    month: YearMonth,
    markers: Map<Long, DayMarker>,
    dayEntries: List<DiaryEntry>,
    selected: LocalDate,
    imageFile: (String) -> File,
    onMonthChange: (YearMonth) -> Unit,
    onSelect: (LocalDate) -> Unit,
    onBack: () -> Unit,
    onOpenEntry: (DiaryEntry) -> Unit,
    onNewEntry: (LocalDate) -> Unit,
    loadForExport: suspend (from: Long?, to: Long?) -> List<DiaryEntry>,
    completions: Map<Long, List<File>> = emptyMap(),
    embedded: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    SystemBarsStyle(darkIcons = !appIsDark())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val today = remember { LocalDate.now() }
    val monthCount = markers.values.sumOf { it.count }

    // Embedded in the Journal tab: no own background, insets or back arrow.
    Box(Modifier.fillMaxSize().then(if (embedded) Modifier else Modifier.background(Brush.verticalGradient(listOf(colors.background, colors.surfaceVariant))))) {
        if (!embedded) FloatingBlobs()
        Column(Modifier.fillMaxSize().then(if (embedded) Modifier else Modifier.statusBarsPadding().navigationBarsPadding())) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!embedded) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = colors.onBackground)
                    }
                    Text(
                        "Calendar",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground,
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {
                    scope.launch {
                        val entries = loadForExport(month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay())
                        if (entries.isEmpty()) {
                            Toast.makeText(context, "No entries in this month", Toast.LENGTH_SHORT).show()
                        } else {
                            val label = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
                            PdfExporter.exportAndShare(context, entries, "Diary_$month", "Diary \u2014 $label", imageFile)
                        }
                    }
                }) {
                    Icon(Icons.Rounded.Share, "Export month as PDF", tint = colors.onBackground)
                }
                Surface(
                    onClick = {
                        onMonthChange(YearMonth.from(today))
                        onSelect(today)
                    },
                    shape = CircleShape,
                    color = colors.primaryContainer,
                ) {
                    Text(
                        "Today",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = colors.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                }
                Spacer(Modifier.width(8.dp))
            }

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = if (embedded) 130.dp else 0.dp)) {
                Spacer(Modifier.height(8.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(0)
                        .clip(RoundedCornerShape(28.dp))
                        .background(colors.surface.copy(alpha = 0.8f))
                        .border(1.dp, colors.outline.copy(alpha = 0.4f), RoundedCornerShape(28.dp))
                        .padding(horizontal = 10.dp, vertical = 14.dp)
                        .pointerInput(month) {
                            var total = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { total = 0f },
                                onDragCancel = { total = 0f },
                                onDragEnd = {
                                    if (total > 120f) onMonthChange(month.minusMonths(1))
                                    else if (total < -120f) onMonthChange(month.plusMonths(1))
                                },
                            ) { _, delta -> total += delta }
                        },
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onMonthChange(month.minusMonths(1)) }) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Previous month", tint = colors.onSurface)
                        }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface,
                            )
                            Text(
                                "$monthCount ${if (monthCount == 1) "entry" else "entries"}",
                                fontSize = 12.sp,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onMonthChange(month.plusMonths(1)) }) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Next month", tint = colors.onSurface)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    WeekdayHeader()
                    AnimatedContent(
                        targetState = month,
                        transitionSpec = { monthTransition(initialState, targetState) },
                        modifier = Modifier.animateContentSize(),
                        label = "month",
                    ) { m ->
                        MonthGrid(m, today, selected, markers, completions, onSelect)
                    }
                }

                Spacer(Modifier.height(22.dp))
                Column(Modifier.staggeredEntrance(1)) {
                    Text(
                        selected.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onBackground,
                    )
                    Spacer(Modifier.height(12.dp))
                    if (dayEntries.isEmpty()) {
                        Text(
                            if (selected.isBefore(today)) "No entry for this day. Past days can't be added to." else "Nothing planned for this day yet.",
                            color = colors.onSurfaceVariant,
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            dayEntries.forEach { entry ->
                                EntryCard(entry, imageFile, onClick = { onOpenEntry(entry) })
                            }
                        }
                    }
                    val finished = completions[selected.toEpochDay()].orEmpty()
                    if (finished.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text("Finished this day", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            finished.forEach { file ->
                                AsyncImage(
                                    model = file,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.width(66.dp).aspectRatio(0.68f).clip(RoundedCornerShape(10.dp)),
                                )
                            }
                        }
                    }
                    if (!selected.isBefore(today)) {
                        Spacer(Modifier.height(16.dp))
                        Row(
                            Modifier
                                .bouncyClick { onNewEntry(selected) }
                                .clip(CircleShape)
                                .background(Brush.horizontalGradient(listOf(colors.primary, Color(0xFFE59A6B))))
                                .padding(horizontal = 22.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Edit, null, tint = colors.onPrimary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (dayEntries.isEmpty()) "Write for this day" else "Add another entry",
                                color = colors.onPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

private fun monthTransition(from: YearMonth, to: YearMonth): ContentTransform {
    val forward = to.isAfter(from)
    val spec = tween<IntOffset>(380, easing = FastOutSlowInEasing)
    return if (forward) {
        (slideInHorizontally(spec) { it / 3 } + fadeIn(tween(300))) togetherWith
            (slideOutHorizontally(spec) { -it / 3 } + fadeOut(tween(200)))
    } else {
        (slideInHorizontally(spec) { -it / 3 } + fadeIn(tween(300))) togetherWith
            (slideOutHorizontally(spec) { it / 3 } + fadeOut(tween(200)))
    }
}

@Composable
private fun WeekdayHeader() {
    val first = remember { WeekFields.of(Locale.getDefault()).firstDayOfWeek }
    Row(Modifier.fillMaxWidth()) {
        for (i in 0 until 7) {
            Text(
                first.plus(i.toLong()).getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    selected: LocalDate,
    markers: Map<Long, DayMarker>,
    completions: Map<Long, List<File>>,
    onSelect: (LocalDate) -> Unit,
) {
    val first = remember { WeekFields.of(Locale.getDefault()).firstDayOfWeek }
    val lead = (month.atDay(1).dayOfWeek.value - first.value + 7) % 7
    val rows = (lead + month.lengthOfMonth() + 6) / 7
    Column {
        for (row in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val day = row * 7 + col - lead + 1
                    if (day < 1 || day > month.lengthOfMonth()) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        val date = month.atDay(day)
                        DayCell(
                            date = date,
                            isSelected = date == selected,
                            isToday = date == today,
                            isFuture = date.isAfter(today),
                            marker = markers[date.toEpochDay()],
                            poster = completions[date.toEpochDay()]?.firstOrNull(),
                            onClick = { onSelect(date) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    marker: DayMarker?,
    poster: File?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    val fill by animateColorAsState(
        when {
            isSelected -> colors.primary
            marker != null -> colors.primaryContainer
            else -> Color.Transparent
        },
        tween(250),
        label = "dayFill",
    )
    val scale by animateFloatAsState(
        if (isSelected) 1.08f else 1f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "dayScale",
    )
    Box(
        modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(fill)
            .then(if (isToday && !isSelected) Modifier.border(1.5.dp, colors.primary, shape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (poster != null) {
            AsyncImage(
                model = poster,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 3.dp).size(width = 13.dp, height = 19.dp).clip(RoundedCornerShape(3.dp)),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${date.dayOfMonth}",
                fontSize = 15.sp,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    isSelected -> colors.onPrimary
                    isFuture -> colors.onSurface.copy(alpha = 0.7f)
                    else -> colors.onSurface
                },
            )
            if (marker != null) {
                if (marker.mood != null) {
                    MoodFace(marker.mood, 14.dp)
                } else {
                    Box(
                        Modifier
                            .padding(top = 3.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) colors.onPrimary else colors.primary),
                    )
                }
            }
        }
    }
}
