package com.amoldalwai.diary.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amoldalwai.diary.data.Activity
import com.amoldalwai.diary.data.ActivityGroup
import com.amoldalwai.diary.data.Mood
import java.time.LocalDate

/** Material icons an activity can use; anything not in here is shown as the emoji it was saved with. */
val activityIcons: Map<String, ImageVector> = linkedMapOf(
    "run" to Icons.Rounded.DirectionsRun, "walk" to Icons.Rounded.DirectionsWalk, "bike" to Icons.Rounded.DirectionsBike,
    "gym" to Icons.Rounded.FitnessCenter, "sleep" to Icons.Rounded.Bedtime, "water" to Icons.Rounded.WaterDrop,
    "meditate" to Icons.Rounded.SelfImprovement, "spa" to Icons.Rounded.Spa, "work" to Icons.Rounded.Work,
    "study" to Icons.Rounded.School, "meeting" to Icons.Rounded.Groups, "read" to Icons.Rounded.MenuBook,
    "computer" to Icons.Rounded.Computer, "family" to Icons.Rounded.FamilyRestroom, "friends" to Icons.Rounded.People,
    "date" to Icons.Rounded.Favorite, "call" to Icons.Rounded.Call, "party" to Icons.Rounded.Cake,
    "game" to Icons.Rounded.SportsEsports, "movie" to Icons.Rounded.Movie, "music" to Icons.Rounded.MusicNote,
    "headphones" to Icons.Rounded.Headphones, "art" to Icons.Rounded.Brush, "cook" to Icons.Rounded.Restaurant,
    "coffee" to Icons.Rounded.Coffee, "travel" to Icons.Rounded.Flight, "nature" to Icons.Rounded.Park,
    "pet" to Icons.Rounded.Pets, "shop" to Icons.Rounded.ShoppingCart, "clean" to Icons.Rounded.CleaningServices,
    "home" to Icons.Rounded.Home, "star" to Icons.Rounded.Star,
)

private data class NoteTemplate(val label: String, val text: String)

private val templates = listOf(
    NoteTemplate("Gratitude", "Three things I'm grateful for today:\n1. \n2. \n3. "),
    NoteTemplate("Morning intentions", "The one thing that matters today:\n\nHow I want to feel:\n"),
    NoteTemplate("Evening reflection", "Three wins today:\n1. \n2. \n3. \n\nOne lesson:\n\nOne thing to let go of:\n"),
    NoteTemplate("Brain dump", "Everything on my mind right now:\n\n"),
    NoteTemplate("Daily review", "Highlight of the day:\n\nA challenge I faced:\n\nTomorrow I will:\n"),
)

@Composable
private fun ActivityIcon(icon: String, tint: Color) {
    val vector = activityIcons[icon]
    if (vector != null) Icon(vector, null, tint = tint, modifier = Modifier.size(24.dp)) else Text(icon, fontSize = 22.sp)
}

/**
 * The fast way to add an entry: optional mood, tap what you did (saved as the entry's tags), a short note with
 * templates, then Save. "Open full note" carries everything into the page editor.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickAddSheet(
    groups: List<ActivityGroup>,
    activitiesEnabled: Boolean,
    habitUi: HabitUi,
    mediaUi: MediaUi,
    initialMood: Mood?,
    onDismiss: () -> Unit,
    onSave: (Mood?, List<String>, String, List<String>) -> Unit,
    onFullNote: (Mood?, List<String>, String, List<String>) -> Unit,
    onGroupsChange: (List<ActivityGroup>) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val today = remember { LocalDate.now() }
    var mood by remember { mutableStateOf(initialMood) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var note by remember { mutableStateOf("") }
    var links by remember { mutableStateOf(listOf<String>()) }
    var editing by remember { mutableStateOf(false) }
    var addingTo by remember { mutableStateOf<String?>(null) }
    var addingGroup by remember { mutableStateOf(false) }
    val collapsed = remember { mutableStateMapOf<String, Boolean>() }
    LaunchedEffect(Unit) { habitUi.load(today) }
    val canSave = note.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.background,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(horizontal = 18.dp).padding(bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("New entry", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (activitiesEnabled) {
                    TextButton(onClick = { editing = !editing }) { Text(if (editing) "Done" else "Edit chips") }
                }
                Button(onClick = { onSave(mood, selected.toList(), note, links) }, enabled = canSave) { Text("Save") }
            }

            Spacer(Modifier.height(10.dp))
            Text("Mood (optional)", fontSize = 12.sp, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Mood.entries.forEach { m ->
                    val on = m == mood
                    Box(
                        Modifier
                            .size(44.dp)
                            .bouncyClick { mood = if (on) null else m }
                            .clip(CircleShape)
                            .background(if (on) colors.primary else colors.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) { Text(m.emoji, fontSize = 23.sp) }
                }
            }

            Spacer(Modifier.height(18.dp))
            if (activitiesEnabled || habitUi.habits.isNotEmpty()) {
                Text(if (activitiesEnabled) "What have you been up to?" else "Today's habits", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }

            if (habitUi.habits.isNotEmpty()) {
                val done = habitUi.done(today)
                GroupCard("Habits", editing = false, collapsed = collapsed["habits"] == true, onToggle = { collapsed["habits"] = collapsed["habits"] != true }, onAdd = null, onDelete = null) {
                    habitUi.habits.forEach { habit ->
                        ActivityChip(habit.emoji, habit.name, habit.id in done, false) { habitUi.toggle(habit.id, today) }
                    }
                }
            }
            if (activitiesEnabled) groups.forEach { group ->
                GroupCard(
                    group.name,
                    editing = editing,
                    collapsed = collapsed[group.id] == true,
                    onToggle = { collapsed[group.id] = collapsed[group.id] != true },
                    onAdd = { addingTo = group.id },
                    onDelete = { onGroupsChange(groups.filterNot { it.id == group.id }) },
                ) {
                    group.items.forEach { activity ->
                        ActivityChip(
                            activity.icon, activity.name, activity.name in selected, editing,
                            onRemove = { onGroupsChange(groups.map { if (it.id == group.id) it.copy(items = it.items - activity) else it }) },
                        ) { selected = if (activity.name in selected) selected - activity.name else selected + activity.name }
                    }
                }
            }
            if (activitiesEnabled && editing) {
                TextButton(onClick = { addingGroup = true }) { Text("＋ Add a group") }
            }

            Spacer(Modifier.height(14.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surface).border(BorderStroke(1.dp, colors.outline.copy(alpha = 0.5f)), RoundedCornerShape(20.dp)).padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Quick note", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onFullNote(mood, selected.toList(), note, links) }) { Text("Open full note ›") }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 8,
                    placeholder = { Text("Write a few words to save this entry") },
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    templates.forEach { t ->
                        Text(
                            t.label,
                            modifier = Modifier
                                .bouncyClick { note = if (note.isBlank()) t.text else note.trimEnd() + "\n\n" + t.text }
                                .clip(CircleShape)
                                .border(1.dp, colors.outline, CircleShape)
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            fontSize = 12.sp,
                            color = colors.onSurface,
                        )
                    }
                }
            }
            if (mediaUi.anyEnabled) {
                Spacer(Modifier.height(18.dp))
                Text("Books · Movies · Games", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                EntryMediaSection(
                    links = links,
                    onLinks = { links = it },
                    date = today,
                    ui = mediaUi,
                    ink = colors.onBackground,
                    onInk = colors.background,
                )
            }
        }
    }

    addingTo?.let { groupId ->
        AddActivityDialog(
            groupName = groups.firstOrNull { it.id == groupId }?.name.orEmpty(),
            onDismiss = { addingTo = null },
            onAdd = { activity ->
                onGroupsChange(groups.map { if (it.id == groupId) it.copy(items = it.items + activity) else it })
                addingTo = null
            },
        )
    }
    if (addingGroup) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addingGroup = false },
            title = { Text("New group") },
            text = { OutlinedTextField(name, { name = it.take(24) }, singleLine = true, label = { Text("Group name") }) },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    onGroupsChange(groups + ActivityGroup(name = name.trim(), items = emptyList()))
                    addingGroup = false
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { addingGroup = false }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupCard(
    title: String,
    editing: Boolean,
    collapsed: Boolean,
    onToggle: () -> Unit,
    onAdd: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    chips: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.fillMaxWidth().padding(top = 12.dp).clip(shape).background(colors.surface).border(BorderStroke(1.dp, colors.outline.copy(alpha = 0.5f)), shape).padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            if (editing && onDelete != null) {
                Box(Modifier.size(36.dp).bouncyClick(onDelete), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Delete, "Delete group", tint = colors.error, modifier = Modifier.size(20.dp)) }
            }
            if (onAdd != null) {
                Box(Modifier.size(36.dp).bouncyClick(onAdd), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Add, "Add activity", tint = colors.primary, modifier = Modifier.size(22.dp)) }
            }
            Box(Modifier.size(36.dp).bouncyClick(onToggle), contentAlignment = Alignment.Center) {
                Icon(if (collapsed) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess, if (collapsed) "Expand" else "Collapse", tint = colors.onSurfaceVariant)
            }
        }
        if (!collapsed) {
            FlowRow(Modifier.padding(end = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { chips() }
        }
    }
}

/** Round icon over a short label: solid when picked, soft when not; a small ✕ appears while editing. */
@Composable
private fun ActivityChip(icon: String, label: String, selected: Boolean, editing: Boolean, onRemove: (() -> Unit)? = null, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.width(64.dp).bouncyClick(onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(if (selected) colors.primary else colors.primaryContainer),
                contentAlignment = Alignment.Center,
            ) { ActivityIcon(icon, if (selected) colors.onPrimary else colors.onPrimaryContainer) }
            if (editing && onRemove != null) {
                Box(
                    Modifier.align(Alignment.TopEnd).size(20.dp).bouncyClick(onRemove).clip(CircleShape).background(colors.error),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Close, "Remove", tint = Color.White, modifier = Modifier.size(13.dp)) }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) colors.onSurface else colors.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddActivityDialog(groupName: String, onDismiss: () -> Unit, onAdd: (Activity) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var name by remember { mutableStateOf("") }
    var iconKey by remember { mutableStateOf("star") }
    var emoji by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to $groupName") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(name, { name = it.take(16) }, singleLine = true, label = { Text("Name") })
                Spacer(Modifier.height(14.dp))
                Text("Icon", fontSize = 12.sp, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    activityIcons.forEach { (key, vector) ->
                        val on = emoji.isBlank() && key == iconKey
                        Box(
                            Modifier.size(42.dp).bouncyClick { iconKey = key; emoji = "" }.clip(CircleShape).background(if (on) colors.primary else colors.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) { Icon(vector, key, tint = if (on) colors.onPrimary else colors.onPrimaryContainer, modifier = Modifier.size(22.dp)) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(emoji, { emoji = it.take(4) }, singleLine = true, label = { Text("Or type an emoji") })
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onAdd(Activity(name.trim(), emoji.trim().ifEmpty { iconKey })) }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
