package com.amoldalwai.diary.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amoldalwai.diary.data.DiaryEntry
import com.amoldalwai.diary.data.Mood
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun SearchScreen(
    results: List<DiaryEntry>,
    totalCount: Int,
    loading: Boolean,
    hasMore: Boolean,
    knownTags: List<String>,
    imageFile: (String) -> File,
    query: String,
    onQuery: (String) -> Unit,
    mood: Mood?,
    onMood: (Mood?) -> Unit,
    tag: String?,
    onTag: (String?) -> Unit,
    onSearch: () -> Unit,
    onLoadMore: () -> Unit,
    onBack: () -> Unit,
    onOpenEntry: (DiaryEntry) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    SystemBarsStyle(darkIcons = !appIsDark())

    val focus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 3
        }
    }

    // Typing restarts this effect, so the query only runs once the user pauses.
    LaunchedEffect(query, mood, tag) {
        delay(250)
        onSearch()
    }
    LaunchedEffect(nearEnd, results.size, hasMore, loading) {
        if (nearEnd && hasMore && !loading) onLoadMore()
    }
    LaunchedEffect(Unit) {
        if (query.isEmpty()) {
            delay(350)
            runCatching { focus.requestFocus() }
        }
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(colors.background, colors.surfaceVariant)))) {
        FloatingBlobs()
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = colors.onBackground) }
                Row(
                    Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(colors.surface.copy(alpha = 0.9f))
                        .border(1.dp, colors.outline.copy(alpha = 0.4f), CircleShape)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Search, null, tint = colors.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) Text("Search your memories", color = colors.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 16.sp)
                        BasicTextField(
                            value = query,
                            onValueChange = onQuery,
                            singleLine = true,
                            textStyle = TextStyle(color = colors.onSurface, fontSize = 16.sp),
                            cursorBrush = SolidColor(colors.primary),
                            modifier = Modifier.fillMaxWidth().focusRequester(focus),
                        )
                    }
                    if (query.isNotEmpty()) {
                        Icon(
                            Icons.Rounded.Close,
                            "Clear",
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(20.dp).clickable { onQuery("") },
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
            }

            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Mood.entries.forEach { m ->
                    FilterPill("${m.emoji} ${m.label}", selected = mood == m) { onMood(if (mood == m) null else m) }
                }
            }
            if (knownTags.isNotEmpty()) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    knownTags.forEach { t -> FilterPill("#$t", selected = tag == t) { onTag(if (tag == t) null else t) } }
                }
            }

            Text(
                "$totalCount ${if (totalCount == 1) "result" else "results"}",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                color = colors.onSurfaceVariant,
                fontSize = 13.sp,
            )

            if (results.isEmpty()) {
                if (loading) {
                    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(2) { ShimmerCard() }
                    }
                } else {
                    Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("No matching entries", style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(results, key = { _, e -> e.id }) { index, entry ->
                        EntryCard(
                            entry = entry,
                            imageFile = imageFile,
                            onClick = { onOpenEntry(entry) },
                            modifier = Modifier.animateItem().staggeredEntrance(index),
                        )
                    }
                    if (loading) item(key = "more") { ShimmerCard() }
                }
            }
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val fill = animateColorAsState(if (selected) colors.primary else colors.surface.copy(alpha = 0.85f), label = "pill")
    val text = animateColorAsState(if (selected) colors.onPrimary else colors.onSurface, label = "pillText")
    Text(
        label,
        color = text.value,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .bouncyClick(onClick)
            .clip(RoundedCornerShape(50))
            .background(fill.value)
            .border(1.dp, colors.outline.copy(alpha = 0.4f), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}
