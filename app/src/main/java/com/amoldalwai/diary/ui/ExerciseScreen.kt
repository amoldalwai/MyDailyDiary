package com.amoldalwai.diary.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amoldalwai.diary.data.ExerciseData
import com.amoldalwai.diary.data.ExerciseMath
import com.amoldalwai.diary.data.ExerciseStats
import com.amoldalwai.diary.data.Muscle
import com.amoldalwai.diary.data.MuscleStat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

private val WeekLetters = listOf("M", "T", "W", "T", "F", "S", "S")
private val AmberText = Color(0xFF9A6A0B)
private val AmberFill = Color(0xFFFDF0D6)
private val RedText = Color(0xFFA4432F)
private val RedFill = Color(0xFFF6E1DC)
private val Amber = Color(0xFFC98A1B)

private enum class DotState { Done, Rest, Today, Missed, Future }


/** Log the muscles trained today, follow a weekly streak, and see progress; the same shape as Daily habits. */
@Composable
fun ExerciseScreen(
    data: ExerciseData,
    weeklyGoal: Int,
    onToggleMuscle: (String) -> Unit,
    onToggleRest: () -> Unit,
    onAddMuscle: (name: String, icon: String, target: Int) -> Unit,
    onUpdateMuscle: (Muscle) -> Unit,
    onRemoveMuscle: (String) -> Unit,
    onGoal: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    SystemBarsStyle(darkIcons = !appIsDark())
    val today = remember { LocalDate.now() }
    val stats = remember(data, weeklyGoal) { ExerciseMath.stats(data, weeklyGoal, today) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf(false) }
    val subtitle = when (tab) {
        0 -> "Tap the muscles you trained today"
        1 -> "Your progress"
        else -> "Tap today's circle to log a muscle"
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(colors.background, colors.surfaceVariant)))) {
        FloatingBlobs()
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = colors.onBackground) }
                Column(Modifier.weight(1f)) {
                    Text("Exercise", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = colors.onBackground)
                    Text(subtitle, fontSize = 13.sp, color = colors.onSurfaceVariant)
                }
                IconButton(onClick = { editing = true }) { Icon(Icons.Rounded.Edit, "Edit muscles", tint = colors.primary) }
            }
            ExerciseTabs(tab) { tab = it }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (tab) {
                    0 -> TodayTab(data, stats, today, onToggleMuscle, onToggleRest, onAdd = { editing = true })
                    1 -> ProgressTab(stats)
                    else -> WeekTab(stats, today, onToggleMuscle)
                }
            }
        }
    }

    if (editing) {
        EditSheet(
            muscles = data.muscles,
            weeklyGoal = weeklyGoal,
            onDismiss = { editing = false },
            onAdd = onAddMuscle,
            onUpdate = onUpdateMuscle,
            onRemove = onRemoveMuscle,
            onGoal = onGoal,
        )
    }
}

@Composable
private fun ExerciseTabs(selected: Int, onSelect: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp).fillMaxWidth().clip(CircleShape).background(colors.surfaceVariant).padding(4.dp)) {
        listOf("Today", "Progress", "This week").forEachIndexed { index, label ->
            val on = index == selected
            Text(
                label,
                modifier = Modifier
                    .weight(1f)
                    .bouncyClick { onSelect(index) }
                    .clip(RoundedCornerShape(50))
                    .background(if (on) colors.surface else Color.Transparent)
                    .padding(vertical = 10.dp),
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                color = if (on) colors.primary else colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(colors.surface.copy(alpha = 0.85f)).border(1.dp, colors.outline.copy(alpha = 0.4f), shape).padding(16.dp),
    ) { content() }
}

private fun sinceText(days: Int?) = when (days) {
    null -> "Not yet"
    0 -> "Today"
    1 -> "Yesterday"
    else -> "$days days ago"
}

private fun dayState(index: Int, todayIndex: Int, done: Boolean, rest: Boolean = false): DotState = when {
    done -> DotState.Done
    rest -> DotState.Rest
    index == todayIndex -> DotState.Today
    index > todayIndex -> DotState.Future
    else -> DotState.Missed
}

@Composable
private fun ExerciseDot(state: DotState, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    val base = modifier.size(30.dp).clip(CircleShape)
    val styled = when (state) {
        DotState.Done -> base.background(colors.primary)
        DotState.Rest -> base.background(colors.surfaceVariant).border(1.dp, colors.outline.copy(alpha = 0.5f), CircleShape)
        DotState.Today -> base.border(2.dp, colors.primary, CircleShape)
        DotState.Missed -> base.border(1.5.dp, colors.outline.copy(alpha = 0.55f), CircleShape)
        DotState.Future -> base.border(1.dp, colors.outline.copy(alpha = 0.25f), CircleShape)
    }
    Box(if (onClick != null) styled.bouncyClick(onClick) else styled, contentAlignment = Alignment.Center) {
        when (state) {
            DotState.Done -> Icon(Icons.Rounded.Check, null, tint = colors.onPrimary, modifier = Modifier.size(18.dp))
            DotState.Rest -> Icon(Icons.Rounded.DarkMode, "Rest day", tint = colors.onSurfaceVariant, modifier = Modifier.size(15.dp))
            else -> Unit
        }
    }
}

@Composable
private fun WeekDots(states: List<DotState>, todayIndex: Int, onToday: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth()) {
        states.forEachIndexed { index, state ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                ExerciseDot(state, onClick = if (index == todayIndex && onToday != null) onToday else null)
            }
        }
    }
}

// ---------------------------------------------------------------- Today

@Composable
private fun TodayTab(
    data: ExerciseData,
    stats: ExerciseStats,
    today: LocalDate,
    onToggle: (String) -> Unit,
    onRest: () -> Unit,
    onAdd: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val epoch = today.toEpochDay()
    val trainedNames = stats.muscles.filter { (it.muscle.id to epoch) in data.logs }.map { it.muscle.name }
    val resting = epoch in data.restDays

    Card {
        Text(
            today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())).uppercase(),
            fontSize = 11.sp,
            letterSpacing = 1.3.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text("What did you train today?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onSurface)
        Spacer(Modifier.height(14.dp))
        val cells: List<MuscleStat?> = stats.muscles + listOf(null)
        cells.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                row.forEach { cell ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                        if (cell == null) AddTile(onAdd) else MuscleCell(cell, trained = (cell.muscle.id to epoch) in data.logs) { onToggle(cell.muscle.id) }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Trained today", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurfaceVariant)
                Text(
                    if (trainedNames.isEmpty()) (if (resting) "Resting today" else "Nothing yet") else trainedNames.joinToString(" · "),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            RestButton(resting, enabled = trainedNames.isEmpty(), onClick = onRest)
        }
    }
    StreakCard(stats)
}

@Composable
private fun MuscleCell(stat: MuscleStat, trained: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.bouncyClick(onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        GlyphCircle(stat.muscle.icon, trained, 74.dp)
        Spacer(Modifier.height(6.dp))
        Text(
            stat.muscle.name,
            fontSize = 12.5.sp,
            fontWeight = if (trained) FontWeight.Bold else FontWeight.Normal,
            color = if (trained) colors.primary else colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val old = stat.daysSince == null || stat.daysSince >= 7
        Text(
            if (trained) "Trained" else sinceText(stat.daysSince),
            fontSize = 11.sp,
            fontWeight = if (trained || old) FontWeight.SemiBold else FontWeight.Normal,
            color = if (trained) colors.primary else if (old) Amber else colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun AddTile(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.bouncyClick(onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(74.dp)
                .clip(CircleShape)
                .drawDashedRing(colors.outline.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Add, "Add a muscle", tint = colors.primary, modifier = Modifier.size(30.dp)) }
        Spacer(Modifier.height(6.dp))
        Text("Add", fontSize = 12.5.sp, color = colors.onSurfaceVariant)
    }
}

private fun Modifier.drawDashedRing(color: Color): Modifier = this.drawBehind {
    val stroke = 2.dp.toPx()
    drawCircle(color, radius = size.minDimension / 2 - stroke / 2, style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 9f))))
}

@Composable
private fun RestButton(resting: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = CircleShape
    Row(
        Modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .then(if (enabled) Modifier.bouncyClick(onClick) else Modifier)
            .clip(shape)
            .background(if (resting) colors.primaryContainer else Color.Transparent)
            .border(1.5.dp, if (resting) colors.primary else colors.outline.copy(alpha = 0.7f), shape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.DarkMode, null, tint = colors.primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(if (resting) "Resting" else "Rest day", fontSize = 13.5.sp, color = colors.onSurface)
    }
}

@Composable
private fun StreakCard(stats: ExerciseStats) {
    val colors = MaterialTheme.colorScheme
    val today = remember { LocalDate.now() }
    val todayIndex = today.dayOfWeek.value - 1
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.LocalFireDepartment, "Streak", tint = FlameOrange, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    if (stats.streakWeeks == 0) "Start your streak" else "${stats.streakWeeks} week streak",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.onSurface,
                )
                Text(sinceWorkoutText(stats) + if (stats.bestStreakWeeks > 0) " · best ${stats.bestStreakWeeks}w" else "", fontSize = 12.sp, color = colors.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("${stats.workoutsThisWeek} of ${stats.weeklyGoal}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                Text("workouts this week", fontSize = 11.5.sp, color = colors.onSurfaceVariant)
            }
            Spacer(Modifier.width(14.dp))
            ProgressBar(
                (stats.workoutsThisWeek.toFloat() / stats.weeklyGoal).coerceAtMost(1f),
                colors.outline.copy(alpha = 0.3f),
                colors.primary,
                Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        WeekDots(overallStates(stats, todayIndex), todayIndex)
        Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
            WeekLetters.forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 11.sp, color = colors.onSurfaceVariant) }
        }
        Text(
            "A week counts when you reach your weekly goal. Rest days never break a streak.",
            modifier = Modifier.padding(top = 10.dp),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = colors.onSurfaceVariant,
        )
    }
}

private fun sinceWorkoutText(stats: ExerciseStats) = when (val d = stats.daysSinceWorkout) {
    null -> "No workouts yet"
    0 -> "Worked out today"
    1 -> "1 day since your last workout"
    else -> "$d days since your last workout"
}

private fun overallStates(stats: ExerciseStats, todayIndex: Int) =
    (0..6).map { dayState(it, todayIndex, stats.weekWorkout[it], stats.weekRest[it]) }

// ---------------------------------------------------------------- Progress

@Composable
private fun ProgressTab(stats: ExerciseStats) {
    val colors = MaterialTheme.colorScheme
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.LocalFireDepartment, "Streak", tint = FlameOrange, modifier = Modifier.size(52.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("${stats.streakWeeks}", fontSize = 44.sp, lineHeight = 44.sp, fontWeight = FontWeight.ExtraBold, color = colors.onSurface)
                Text("week streak", fontSize = 13.sp, color = colors.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${stats.bestStreakWeeks}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                Text("best", fontSize = 12.sp, color = colors.onSurfaceVariant)
            }
        }
        Text(
            sinceWorkoutText(stats) + " · ${stats.workoutsThisMonth} workouts this month",
            modifier = Modifier.padding(top = 8.dp),
            fontSize = 12.sp,
            color = colors.onSurfaceVariant,
        )
    }
    Card {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Workouts per week", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = colors.onSurface, modifier = Modifier.weight(1f))
            Text("last 12 weeks", fontSize = 13.sp, color = colors.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        WeeklyBars(stats.weeklyWorkouts, stats.weeklyGoal)
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("12 weeks ago", fontSize = 10.sp, color = colors.onSurfaceVariant)
            Text("this week", fontSize = 10.sp, color = colors.onSurfaceVariant)
        }
        Text("Green bars reached your weekly goal of ${stats.weeklyGoal}.", modifier = Modifier.padding(top = 8.dp), fontSize = 12.sp, color = colors.onSurfaceVariant)
    }
    Card {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Each muscle", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = colors.onSurface, modifier = Modifier.weight(1f))
            Text("last 8 weeks", fontSize = 13.sp, color = colors.onSurfaceVariant)
        }
        Text(
            "Bars show how many days you trained it each week. Its streak counts weeks in a row you reached its target.",
            modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = colors.onSurfaceVariant,
        )
        if (stats.muscles.isEmpty()) {
            Text("Add a muscle with the pencil to start.", fontSize = 13.sp, color = colors.onSurfaceVariant)
        }
        stats.muscles.forEach { stat ->
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outline.copy(alpha = 0.2f)))
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                GlyphCircle(stat.muscle.icon, false, 44.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(stat.muscle.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "Target ${stat.muscle.target}×/week · ${sinceText(stat.daysSince).lowercase()}",
                        fontSize = 11.5.sp,
                        color = colors.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Spark(stat)
                Spacer(Modifier.width(8.dp))
                if (stat.streakWeeks > 0) StreakChip(stat.streakWeeks, "w")
            }
        }
    }
}

@Composable
private fun WeeklyBars(values: List<Int>, goal: Int) {
    val colors = MaterialTheme.colorScheme
    val hit = colors.primary
    val miss = colors.outline.copy(alpha = 0.45f)
    val goalColor = Amber
    Canvas(Modifier.fillMaxWidth().height(96.dp)) {
        val n = values.size
        val gap = 7.dp.toPx()
        val barWidth = (size.width - gap * (n - 1)) / n
        val top = max(values.maxOrNull() ?: 0, goal).coerceAtLeast(1).toFloat()
        values.forEachIndexed { i, v ->
            val h = (v / top * size.height).coerceAtLeast(4.dp.toPx())
            drawRoundRect(
                color = if (v >= goal) hit else miss,
                topLeft = Offset(i * (barWidth + gap), size.height - h),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(5.dp.toPx()),
            )
        }
        val y = size.height - goal / top * size.height
        drawLine(goalColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
    }
}

@Composable
private fun Spark(stat: MuscleStat) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.height(34.dp).width(58.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
        stat.history.forEach { v ->
            Box(
                Modifier
                    .weight(1f)
                    .height((4 + v * 7).coerceAtMost(34).dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        when {
                            v >= stat.muscle.target -> colors.primary
                            v > 0 -> colors.primary.copy(alpha = 0.4f)
                            else -> colors.outline.copy(alpha = 0.3f)
                        },
                    ),
            )
        }
    }
}

// ---------------------------------------------------------------- This week

@Composable
private fun WeekTab(stats: ExerciseStats, today: LocalDate, onToggle: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val todayIndex = today.dayOfWeek.value - 1
    val monday = today.minusDays(todayIndex.toLong())
    val sunday = monday.plusDays(6)
    val range = "${monday.dayOfMonth}–${sunday.dayOfMonth} ${sunday.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())}"

    Card {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("This week", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = colors.onSurface, modifier = Modifier.weight(1f))
            Text("$range · ${stats.workoutsThisWeek} workouts so far", fontSize = 12.sp, color = colors.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        ProgressBar((stats.workoutsThisWeek.toFloat() / stats.weeklyGoal).coerceAtMost(1f), colors.outline.copy(alpha = 0.3f), colors.primary, Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth()) {
            (0..6).forEach { i ->
                val day = monday.plusDays(i.toLong())
                val isToday = i == todayIndex
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(WeekLetters[i], fontSize = 10.5.sp, color = colors.onSurfaceVariant.copy(alpha = if (i > todayIndex) 0.5f else 1f))
                    Text(
                        "${day.dayOfMonth}",
                        modifier = Modifier.clip(CircleShape).background(if (isToday) colors.primary else Color.Transparent).padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isToday) colors.onPrimary else colors.onSurface.copy(alpha = if (i > todayIndex) 0.5f else 1f),
                    )
                }
            }
        }

        // Any workout
        Box(Modifier.fillMaxWidth().padding(top = 8.dp).height(1.dp).background(colors.outline.copy(alpha = 0.25f)))
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(colors.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Star, null, tint = colors.onPrimary, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text("Any workout", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = colors.onSurface, modifier = Modifier.weight(1f))
            CountPill(stats.workoutsThisWeek, stats.weeklyGoal, "goal")
            Spacer(Modifier.width(8.dp))
            if (stats.streakWeeks > 0) StreakChip(stats.streakWeeks, "w")
        }
        WeekDots(overallStates(stats, todayIndex), todayIndex)
        Spacer(Modifier.height(10.dp))

        stats.muscles.forEach { stat ->
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outline.copy(alpha = 0.25f)))
            Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                GlyphCircle(stat.muscle.icon, false, 34.dp)
                Spacer(Modifier.width(10.dp))
                Text(stat.muscle.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = colors.onSurface, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                CountPill(stat.thisWeek, stat.muscle.target, "this week")
                Spacer(Modifier.width(8.dp))
                if (stat.streakWeeks > 0) StreakChip(stat.streakWeeks, "w")
            }
            WeekDots(
                (0..6).map { dayState(it, todayIndex, stat.weekDays[it]) },
                todayIndex,
                onToday = { onToggle(stat.muscle.id) },
            )
            Spacer(Modifier.height(10.dp))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outline.copy(alpha = 0.25f)))
        Text("Muscles trained each day", modifier = Modifier.padding(top = 10.dp), fontSize = 11.5.sp, color = colors.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            (0..6).forEach { i ->
                Text(
                    if (i <= todayIndex) "${stats.weekMuscleCounts[i]}/${stats.muscles.size}" else "–",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    fontWeight = if (i <= todayIndex && stats.muscles.isNotEmpty() && stats.weekMuscleCounts[i] == stats.muscles.size) FontWeight.Bold else FontWeight.Normal,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
    Text(
        "Tap today's circle to mark a muscle. Earlier days are a record and can't be changed. The pills compare this week's days with each muscle's target.",
        fontSize = 12.sp,
        lineHeight = 17.sp,
        color = colors.onSurfaceVariant,
    )
}

@Composable
private fun CountPill(count: Int, target: Int, word: String) {
    val colors = MaterialTheme.colorScheme
    val (fill, text) = when {
        count >= target -> colors.primaryContainer to colors.onPrimaryContainer
        count > 0 -> AmberFill to AmberText
        else -> RedFill to RedText
    }
    Text(
        "$count/$target $word",
        modifier = Modifier.clip(CircleShape).background(fill).padding(horizontal = 8.dp, vertical = 3.dp),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = text,
        maxLines = 1,
    )
}

// ---------------------------------------------------------------- Edit sheet

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EditSheet(
    muscles: List<Muscle>,
    weeklyGoal: Int,
    onDismiss: () -> Unit,
    onAdd: (String, String, Int) -> Unit,
    onUpdate: (Muscle) -> Unit,
    onRemove: (String) -> Unit,
    onGoal: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var removing by remember { mutableStateOf<Muscle?>(null) }
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("muscle_chest") }
    var moreIcons by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 760.dp).verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 16.dp),
        ) {
            Text("Your muscles", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onSurface)
            Text("Set how often you want to train each one. Remove the ones you don't use; your history is kept.", fontSize = 13.sp, lineHeight = 18.sp, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))

            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFFFF3E8).copy(alpha = if (appIsDark()) 0.12f else 1f)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Weekly workout goal", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.onSurface)
                    Text("Your streak counts weeks you reach it", fontSize = 12.sp, color = colors.onSurfaceVariant)
                }
                Stepper("$weeklyGoal ${if (weeklyGoal == 1) "day" else "days"}", { onGoal(weeklyGoal - 1) }, { onGoal(weeklyGoal + 1) })
            }
            Spacer(Modifier.height(12.dp))

            muscles.forEach { muscle ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlyphCircle(muscle.icon, false, 42.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(muscle.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("days per week", fontSize = 11.sp, color = colors.onSurfaceVariant)
                    }
                    Stepper(
                        "${muscle.target}×",
                        { onUpdate(muscle.copy(target = (muscle.target - 1).coerceAtLeast(1))) },
                        { onUpdate(muscle.copy(target = (muscle.target + 1).coerceAtMost(7))) },
                    )
                    IconButton(onClick = { removing = muscle }) { Icon(Icons.Rounded.Close, "Remove ${muscle.name}", tint = colors.onSurfaceVariant) }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("ADD MORE", fontSize = 11.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            val missing = ExerciseMath.suggestions.filter { (n, _) -> muscles.none { it.name.equals(n, ignoreCase = true) } }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                missing.forEach { (n, key) ->
                    Row(
                        Modifier
                            .bouncyClick { onAdd(n, key, 1) }
                            .clip(CircleShape)
                            .border(1.4.dp, colors.outline, CircleShape)
                            .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Add, null, tint = colors.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(n, fontSize = 13.sp, color = colors.onSurface)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(20) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("New muscle name") },
            )
            Spacer(Modifier.height(10.dp))
            Text("Picture", fontSize = 12.sp, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MuscleArt.ids.keys.forEach { key -> GlyphCircle(key, key == icon, 46.dp, Modifier.bouncyClick { icon = key }) }
            }
            TextButton(onClick = { moreIcons = !moreIcons }) { Text(if (moreIcons) "Hide more icons" else "More icons") }
            if (moreIcons) IconPicker(icon) { icon = it }
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = {
                    onAdd(name, icon, 1)
                    name = ""
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Add muscle") }
        }
    }

    removing?.let { muscle ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("Remove ${muscle.name}?") },
            text = { Text("It disappears from your lists. Your history is kept, and adding ${muscle.name} again brings it back.") },
            confirmButton = {
                TextButton(onClick = {
                    onRemove(muscle.id)
                    removing = null
                }) { Text("Remove", color = colors.error) }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Stepper(label: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepButton(Icons.Rounded.Remove, "Less", onMinus)
        Text(label, modifier = Modifier.width(62.dp), textAlign = TextAlign.Center, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
        StepButton(Icons.Rounded.Add, "More", onPlus)
    }
}

@Composable
private fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.size(30.dp).bouncyClick(onClick).clip(CircleShape).border(1.dp, colors.outline.copy(alpha = 0.7f), CircleShape),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = colors.onSurface, modifier = Modifier.size(16.dp)) }
}
