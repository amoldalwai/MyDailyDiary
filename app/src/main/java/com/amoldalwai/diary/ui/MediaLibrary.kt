package com.amoldalwai.diary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed

/**
 * The library screen shared by every collection (books, games, later movies): an "All" tab plus one tab per shelf,
 * shown either as Netflix-style rows or as a wooden bookshelf. A section only supplies items, shelves and its own sheets.
 */
@Composable
fun MediaLibraryScreen(
    title: String,
    emoji: String,
    summary: String,
    shelves: List<MediaShelf>,
    items: List<MediaItem>,
    emptyTitle: (MediaShelf?) -> String,
    emptyHint: String,
    onBack: () -> Unit,
    addSheet: @Composable (dismiss: () -> Unit) -> Unit,
    detailSheet: @Composable (id: String, dismiss: () -> Unit) -> Unit,
    kinds: List<MediaShelf> = emptyList(),
) {
    val colors = MaterialTheme.colorScheme
    SystemBarsStyle(darkIcons = !appIsDark())

    var tab by rememberSaveable { mutableStateOf<String?>(null) }
    var shelfMode by rememberSaveable { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    var kind by rememberSaveable { mutableStateOf<String?>(null) }

    // Optional second filter (Movies / Series); everything below works on the filtered list.
    val scoped = remember(items, kind) { items.filter { kind == null || it.kind == kind } }
    val shown = remember(scoped, tab) { scoped.filter { tab == null || it.shelf == tab } }
    val activeShelf = shelves.firstOrNull { it.key == tab }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(colors.background, colors.surfaceVariant)))) {
        FloatingBlobs()
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = colors.onBackground) }
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = colors.onBackground)
                    Text(summary, fontSize = 13.sp, color = colors.onSurfaceVariant)
                }
                Text(
                    if (shelfMode) "🎞 Rows" else "📚 Shelf",
                    modifier = Modifier
                        .bouncyClick { shelfMode = !shelfMode }
                        .clip(CircleShape)
                        .background(colors.primaryContainer)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    color = colors.onPrimaryContainer,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
                IconButton(onClick = { adding = true }) { Icon(Icons.Rounded.Add, "Add", tint = colors.primary) }
            }

            if (kinds.isNotEmpty()) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf<MediaShelf?>(null).plus(kinds).forEach { k ->
                        val on = k?.key == kind
                        Text(
                            k?.label ?: "Everything",
                            modifier = Modifier
                                .bouncyClick { kind = k?.key }
                                .clip(CircleShape)
                                .background(if (on) colors.secondaryContainer else colors.surface.copy(alpha = 0.5f))
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            color = if (on) colors.onSecondaryContainer else colors.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }

            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf<MediaShelf?>(null) .plus(shelves).forEach { shelf ->
                    val on = shelf?.key == tab
                    val count = if (shelf == null) scoped.size else scoped.count { it.shelf == shelf.key }
                    Text(
                        "${shelf?.label ?: "All"} · $count",
                        modifier = Modifier
                            .bouncyClick { tab = shelf?.key }
                            .clip(CircleShape)
                            .background(if (on) colors.primary else colors.surface.copy(alpha = 0.85f))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        color = if (on) colors.onPrimary else colors.onSurface,
                        fontSize = 13.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }

            when {
                shown.isEmpty() -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(emoji, fontSize = 56.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(emptyTitle(activeShelf), style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
                    Spacer(Modifier.height(6.dp))
                    Text(emptyHint, color = colors.onSurfaceVariant)
                }

                shelfMode -> LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    val groups = if (tab == null) {
                        shelves.map { it to scoped.filter { item -> item.shelf == it.key } }.filter { it.second.isNotEmpty() }
                    } else {
                        listOf((activeShelf ?: MediaShelf("", "")) to shown)
                    }
                    items(groups, key = { it.first.key }) { (shelf, group) ->
                        Bookshelf(
                            label = shelf.label.takeIf { it.isNotEmpty() },
                            items = group,
                            firstIndex = 0,
                            onClick = { selectedId = it.id },
                        )
                    }
                }

                tab == null -> LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                    shelves.forEach { shelf ->
                        val group = scoped.filter { it.shelf == shelf.key }
                        if (group.isNotEmpty()) {
                            item(key = shelf.key) {
                                Column {
                                    Text(
                                        shelf.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.onBackground,
                                        modifier = Modifier.padding(horizontal = 22.dp),
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    LazyRow(contentPadding = PaddingValues(horizontal = 22.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        itemsIndexed(group, key = { _, it -> it.id }) { index, item ->
                                            MediaCard(item, Modifier.width(118.dp).staggeredEntrance(index)) { selectedId = item.id }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    gridItemsIndexed(shown, key = { _, it -> it.id }) { index, item ->
                        MediaCard(item, Modifier.staggeredEntrance(index)) { selectedId = item.id }
                    }
                }
            }
        }
    }

    selectedId?.let { id -> detailSheet(id) { selectedId = null } }
    if (adding) addSheet { adding = false }
}
