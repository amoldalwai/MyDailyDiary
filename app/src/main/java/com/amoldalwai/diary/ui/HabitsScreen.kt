package com.amoldalwai.diary.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Edit
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

private val habitEmojis = listOf("💪", "👟", "📖", "🧘", "💧", "😴", "🥗", "🏃", "🎨", "🎵", "🧹", "💊", "🙏", "✍️", "🚿", "🌿")

private val habitSuggestions = listOf(
    "Gym" to "💪", "10k steps" to "👟", "Read" to "📖", "Meditate" to "🧘", "Drink water" to "💧", "Sleep by 11" to "😴",
)

/** Manage the habit list and see each habit's streaks and recent history. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitsScreen(
    stats: List<HabitStat>,
    onAdd: (name: String, emoji: String) -> Unit,
    onUpdate: (Habit, name: String, emoji: String) -> Unit,
    onDelete: (Habit) -> Unit,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    SystemBarsStyle(darkIcons = !appIsDark())
    var editing by remember { mutableStateOf<Habit?>(null) }
    var adding by remember { mutableStateOf(false) }

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
                    Text("🌱", fontSize = 56.sp)
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
                        habitSuggestions.forEach { (name, emoji) ->
                            Text(
                                "$emoji  $name",
                                modifier = Modifier
                                    .bouncyClick { onAdd(name, emoji) }
                                    .clip(CircleShape)
                                    .background(colors.primaryContainer)
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                                color = colors.onPrimaryContainer,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
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
            Box(Modifier.size(44.dp).clip(CircleShape).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
                Text(stat.habit.emoji, fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stat.habit.name, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = colors.onSurface)
                Text(
                    "🔥 ${stat.currentStreak} day streak · best ${stat.bestStreak}",
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant,
                )
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
    var emoji by remember { mutableStateOf(initial?.emoji ?: habitEmojis.first()) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New habit" else "Edit habit") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(28) },
                    label = { Text("Habit") },
                    placeholder = { Text("e.g. Gym, 10k steps") },
                    singleLine = true,
                )
                Spacer(Modifier.height(14.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    habitEmojis.forEach { option ->
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (option == emoji) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { emoji = option },
                            contentAlignment = Alignment.Center,
                        ) { Text(option, fontSize = 20.sp) }
                    }
                }
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
