package com.amoldalwai.diary.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amoldalwai.diary.data.DayMarker
import com.amoldalwai.diary.data.DiaryEntry
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

/** Every page you've written, as a list or a calendar; the search icon opens the full search screen. */
@Composable
fun JournalScreen(
    entries: List<DiaryEntry>,
    loading: Boolean,
    loadingMore: Boolean,
    hasMore: Boolean,
    imageFile: (String) -> File,
    onOpenEntry: (DiaryEntry) -> Unit,
    onLoadMore: () -> Unit,
    onSearch: () -> Unit,
    month: YearMonth,
    markers: Map<Long, DayMarker>,
    dayEntries: List<DiaryEntry>,
    selected: LocalDate,
    onMonthChange: (YearMonth) -> Unit,
    onSelect: (LocalDate) -> Unit,
    onNewEntry: (LocalDate) -> Unit,
    loadForExport: suspend (from: Long?, to: Long?) -> List<DiaryEntry>,
    completions: Map<Long, List<File>>,
    onDeleteEntries: (List<DiaryEntry>) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    SystemBarsStyle(darkIcons = !appIsDark())
    var calendarMode by rememberSaveable { mutableStateOf(false) }
    var picked by remember { mutableStateOf<Map<String, DiaryEntry>>(emptyMap()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val selecting = picked.isNotEmpty()
    BackHandler(enabled = selecting) { picked = emptyMap() }

    fun toggle(entry: DiaryEntry) {
        picked = if (entry.id in picked) picked - entry.id else picked + (entry.id to entry)
    }
    // A tap opens the page normally, but once something is picked it adds or removes that page.
    val openOrToggle: (DiaryEntry) -> Unit = { entry -> if (selecting) toggle(entry) else onOpenEntry(entry) }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        FloatingBlobs()
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            if (selecting) {
                Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundIconButton(Icons.Rounded.Close, "Cancel selection", { picked = emptyMap() })
                    Text(
                        "${picked.size} selected",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground,
                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                    )
                    if (!calendarMode) {
                        RoundIconButton(Icons.Rounded.SelectAll, "Select all", { picked = entries.associateBy { it.id } })
                        Spacer(Modifier.width(8.dp))
                    }
                    RoundIconButton(Icons.Rounded.DeleteOutline, "Delete selected", { confirmDelete = true })
                }
            } else {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Journal", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = colors.onBackground, modifier = Modifier.weight(1f))
                    RoundIconButton(Icons.Rounded.Search, "Search", onSearch)
                }
            }
            Segments(calendarMode) { calendarMode = it }
            if (calendarMode) {
                CalendarScreen(
                    month = month,
                    markers = markers,
                    dayEntries = dayEntries,
                    selected = selected,
                    imageFile = imageFile,
                    onMonthChange = onMonthChange,
                    onSelect = onSelect,
                    onBack = {},
                    onOpenEntry = openOrToggle,
                    onNewEntry = onNewEntry,
                    loadForExport = loadForExport,
                    completions = completions,
                    embedded = true,
                    pickedIds = picked.keys,
                    onLongPress = ::toggle,
                )
            } else {
                EntryList(entries, loading, loadingMore, hasMore, imageFile, openOrToggle, onLoadMore, picked.keys, ::toggle)
            }
        }
    }

    if (confirmDelete) {
        val count = picked.size
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(if (count == 1) "Delete this page forever?" else "Delete $count pages forever?") },
            text = { Text("Once deleted, ${if (count == 1) "this page and its photos are" else "these pages and their photos are"} gone for good and can't be brought back. Are you sure?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDeleteEntries(picked.values.toList())
                    picked = emptyMap()
                }) { Text("Delete", color = colors.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Segments(calendarMode: Boolean, onChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .fillMaxWidth()
            .clip(CircleShape)
            .background(colors.surfaceVariant)
            .padding(4.dp),
    ) {
        listOf("Entries" to false, "Calendar" to true).forEach { (label, mode) ->
            val on = mode == calendarMode
            val fill by animateColorAsState(if (on) colors.surface else colors.surface.copy(alpha = 0f), label = "segFill")
            Text(
                label,
                modifier = Modifier
                    .weight(1f)
                    .bouncyClick { onChange(mode) }
                    .clip(RoundedCornerShape(50))
                    .background(fill)
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
private fun EntryList(
    entries: List<DiaryEntry>,
    loading: Boolean,
    loadingMore: Boolean,
    hasMore: Boolean,
    imageFile: (String) -> File,
    onOpenEntry: (DiaryEntry) -> Unit,
    onLoadMore: () -> Unit,
    pickedIds: Set<String>,
    onLongPress: (DiaryEntry) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
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
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (loading) {
            item(key = "loading") { ShimmerCard() }
        } else if (entries.isEmpty()) {
            item(key = "empty") {
                Text(
                    "No pages yet. Tap + to write the first one.",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    textAlign = TextAlign.Center,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        itemsIndexed(entries, key = { _, e -> e.id }) { index, entry ->
            val alreadyShown = remember { animatedIds.contains(entry.id) }
            animatedIds.add(entry.id)
            EntryCard(
                entry = entry,
                imageFile = imageFile,
                onClick = { onOpenEntry(entry) },
                modifier = Modifier.animateItem().paperDrop(index, skip = alreadyShown),
                selecting = pickedIds.isNotEmpty(),
                selected = entry.id in pickedIds,
                onLongClick = { onLongPress(entry) },
            )
        }
        if (loadingMore) {
            item(key = "more") { ShimmerCard() }
        } else if (!hasMore && entries.size > 3) {
            item(key = "end") {
                Text(
                    "That's every page",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    textAlign = TextAlign.Center,
                    color = colors.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }
        }
    }
}
