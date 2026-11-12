package com.amoldalwai.diary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import android.view.HapticFeedbackConstants
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amoldalwai.diary.data.Habit
import com.amoldalwai.diary.data.HabitStat

private val habitSuggestions = listOf(
    "Gym" to "gym", "10k steps" to "walk", "Read" to "read", "Meditate" to "meditate", "Drink water" to "water", "Sleep by 11" to "sleep",
)

/** Manage the habit list and see each habit's streaks and recent history. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitsScreen(
    stats: List<HabitStat>,
    onAdd: (name: String, emoji: String) -> Unit,
    onUpdate: (Habit, name: String, emoji: String) -> Unit,
    onDelete: (Habit) -> Unit,
    onToggleToday: (habitId: String, done: Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    SystemBarsStyle(darkIcons = !appIsDark())
    var editing by remember { mutableStateOf<Habit?>(null) }
    var adding by remember { mutableStateOf(false) }
    var weekTab by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(colors.background, colors.surfaceVariant)))) {
        FloatingBlobs()
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = colors.onBackground) }
                Column(Modifier.weight(1f)) {
                    Text("Daily habits", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = colors.onBackground)
                    Text("Tick them off on any journal page", fontSize = 13.sp, color = colors.onSurfaceVariant)
                }
                IconButton(onClick = { adding = true }) { Icon(Icons.Rounded.Add, "Add habit", tint = colors.primary) }
            }

            if (stats.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box(Modifier.size(72.dp).clip(CircleShape).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Eco, null, tint = colors.primary, modifier = Modifier.size(40.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Build a habit", style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Pick a few things you want to do every day. They show up on each page of your journal.",
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        habitSuggestions.forEach { (name, icon) ->
                            Row(
                                Modifier
                                    .bouncyClick { onAdd(name, icon) }
                                    .clip(CircleShape)
                                    .background(colors.primaryContainer)
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Glyph(icon, colors.primary, 18.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(name, color = colors.onPrimaryContainer, fontSize = 14.sp)
                            }
                        }
                    }
                }
            } else {
                HabitTabs(weekTab) { weekTab = it }
                if (weekTab) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
                        WeekTable(stats, onToggleToday)
                    }
                } else LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(stats, key = { _, s -> s.habit.id }) { index, stat ->
                        HabitStatCard(stat, onEdit = { editing = stat.habit }, modifier = Modifier.animateItem().staggeredEntrance(index))
                    }
                }
            }
        }
    }

    if (adding) {
        HabitDialog(
            initial = null,
            onDismiss = { adding = false },
            onSave = { name, emoji ->
                onAdd(name, emoji)
                adding = false
            },
            onDelete = null,
        )
    }
    editing?.let { habit ->
        HabitDialog(
            initial = habit,
            onDismiss = { editing = null },
            onSave = { name, emoji ->
                onUpdate(habit, name, emoji)
                editing = null
            },
            onDelete = {
                onDelete(habit)
                editing = null
            },
        )
    }
}

@Composable
private fun HabitTabs(weekTab: Boolean, onChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.padding(horizontal = 20.dp, vertical = 8.dp).fillMaxWidth().clip(CircleShape).background(colors.surfaceVariant).padding(4.dp),
    ) {
        listOf("Streaks" to false, "This week" to true).forEach { (label, week) ->
            val on = week == weekTab
            Text(
                label,
                modifier = Modifier
                    .weight(1f)
                    .bouncyClick { onChange(week) }
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

/** Habits as rows and the last seven days as columns; only today can be ticked here, past days are a record. */
@Composable
private fun WeekTable(stats: List<HabitStat>, onToggleToday: (String, Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val view = LocalView.current
    val today = remember { LocalDate.now() }
    val days = remember { (6 downTo 0).map { today.minusDays(it.toLong()) } }
    val weeks = remember(stats) { stats.map { it.last28.takeLast(7) } }
    val ticks = weeks.sumOf { w -> w.count { it } }
    val possible = stats.size * 7
    val shape = RoundedCornerShape(24.dp)

    Column(
        Modifier.fillMaxWidth().clip(shape).background(colors.surface.copy(alpha = 0.85f)).border(1.dp, colors.outline.copy(alpha = 0.4f), shape).padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Last 7 days", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = colors.onSurface, modifier = Modifier.weight(1f))
            Text("$ticks of $possible", fontSize = 13.sp, color = colors.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        ProgressBar(if (possible == 0) 0f else ticks.toFloat() / possible, colors.outline.copy(alpha = 0.3f), colors.primary, Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            days.forEach { day ->
                val isToday = day == today
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), fontSize = 10.sp, color = colors.onSurfaceVariant)
                    Text(
                        "${day.dayOfMonth}",
                        modifier = Modifier.clip(CircleShape).background(if (isToday) colors.primary else Color.Transparent).padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isToday) colors.onPrimary else colors.onSurface,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        stats.forEachIndexed { row, stat ->
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outline.copy(alpha = 0.25f)))
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                // Name on its own line with the streak on the right, then the seven days underneath.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlyphCircle(stat.habit.emoji, false, 34.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stat.habit.name,
                        modifier = Modifier.weight(1f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (stat.currentStreak > 0) StreakChip(stat.currentStreak)
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    weeks[row].forEachIndexed { i, done ->
                        val isToday = i == 6
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            DayDot(done, isToday) {
                                if (!done) view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                onToggleToday(stat.habit.id, !done)
                            }
                        }
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outline.copy(alpha = 0.25f)))
        Text("Habits done each day", modifier = Modifier.padding(top = 10.dp), fontSize = 11.sp, color = colors.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            for (i in 0 until 7) {
                val count = weeks.count { it[i] }
                val all = stats.isNotEmpty() && count == stats.size
                Text(
                    "$count/${stats.size}",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    fontWeight = if (all) FontWeight.Bold else FontWeight.Normal,
                    color = if (all) colors.primary else colors.onSurfaceVariant,
                )
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    Text("Tap today's circle to tick a habit. Earlier days are a record and can't be changed.", fontSize = 12.sp, color = colors.onSurfaceVariant)
}

/** The streak flame is always orange, whatever the theme colour. */
internal val FlameOrange = Color(0xFFFF7A1A)

/** Flame and count in a soft orange pill; [suffix] is a unit such as "w" for weeks. */
@Composable
internal fun StreakChip(days: Int, suffix: String = "") {
    Row(
        Modifier.clip(CircleShape).background(FlameOrange.copy(alpha = 0.14f)).padding(start = 7.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.LocalFireDepartment, "Streak", tint = FlameOrange, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(3.dp))
        Text("$days$suffix", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FlameOrange)
    }
}

@Composable
private fun DayDot(done: Boolean, isToday: Boolean, onToggleToday: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val base = Modifier.size(30.dp).clip(CircleShape)
    val styled = when {
        done -> base.background(colors.primary)
        isToday -> base.border(2.dp, colors.primary, CircleShape)
        else -> base.border(1.5.dp, colors.outline.copy(alpha = 0.55f), CircleShape)
    }
    Box(if (isToday) styled.clickable(onClick = onToggleToday) else styled, contentAlignment = Alignment.Center) {
        if (done) Icon(Icons.Rounded.Check, null, tint = colors.onPrimary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun HabitStatCard(stat: HabitStat, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface.copy(alpha = 0.85f))
            .border(1.dp, colors.outline.copy(alpha = 0.4f), shape)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlyphCircle(stat.habit.emoji, false, 46.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stat.habit.name, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = colors.onSurface)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LocalFireDepartment, null, tint = FlameOrange, modifier = Modifier.size(17.dp))
                    Text(
                        " ${stat.currentStreak} day streak · best ${stat.bestStreak}",
                        fontSize = 13.sp,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, "Edit habit", tint = colors.onSurfaceVariant) }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                stat.last28.chunked(7).forEach { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        week.forEach { done ->
                            Box(
                                Modifier
                                    .size(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (done) colors.primary else colors.surfaceVariant),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("${stat.doneLast30}/30", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = colors.onSurface)
                Text("last 30 days", fontSize = 12.sp, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text("${stat.totalDone} ticks in total", fontSize = 12.sp, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HabitDialog(
    initial: Habit?,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var emoji by remember { mutableStateOf(IconCatalog.keyFor(initial?.emoji ?: "gym")) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New habit" else "Edit habit") },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(28) },
                    label = { Text("Habit") },
                    placeholder = { Text("e.g. Gym, 10k steps") },
                    singleLine = true,
                )
                Spacer(Modifier.height(14.dp))
                IconPicker(emoji) { emoji = it }
                if (onDelete != null) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { confirmDelete = true }) { Text("Delete habit", color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, emoji) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this habit?") },
            text = { Text("Its history is removed too. Your journal entries are not affected.") },
            confirmButton = { TextButton(onClick = onDelete) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
