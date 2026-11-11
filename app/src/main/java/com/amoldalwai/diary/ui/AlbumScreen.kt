package com.amoldalwai.diary.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amoldalwai.diary.data.PhotoRef
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Every photo from every entry, as stamps on album pages grouped by month. Tap a stamp to open its entry. */
@Composable
fun AlbumScreen(
    photos: List<PhotoRef>,
    total: Int,
    loading: Boolean,
    hasMore: Boolean,
    imageFile: (String) -> File,
    onLoadMore: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    SystemBarsStyle(darkIcons = !appIsDark())

    val byMonth = remember(photos) { photos.groupBy { YearMonth.from(LocalDate.ofEpochDay(it.dateEpochDay)) } }
    val gridState = rememberLazyGridState()
    val nearEnd by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 6
        }
    }
    LaunchedEffect(nearEnd, photos.size, hasMore, loading) {
        if (nearEnd && hasMore && !loading) onLoadMore()
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(colors.background, colors.surfaceVariant)))) {
        FloatingBlobs()
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = colors.onBackground) }
                Column {
                    Text("Stamp album", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = colors.onBackground)
                    Text(
                        "$total ${if (total == 1) "stamp" else "stamps"} collected",
                        fontSize = 13.sp,
                        color = colors.onSurfaceVariant,
                    )
                }
            }

            if (!loading && photos.isEmpty()) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("✉️", fontSize = 56.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("No stamps yet", style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
                    Spacer(Modifier.height(4.dp))
                    Text("Photos you add to your entries are collected here.", color = colors.onSurfaceVariant, fontSize = 14.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    byMonth.forEach { (month, list) ->
                        item(key = "month-$month", span = { GridItemSpan(maxLineSpan) }) {
                            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onBackground,
                                    modifier = Modifier.weight(1f),
                                )
                                Text("${list.size}", color = colors.onSurfaceVariant, fontSize = 13.sp)
                            }
                        }
                        itemsIndexed(list, key = { _, p -> p.name }) { index, photo ->
                            val tilt = remember(photo.name) { ((photo.name.hashCode() and 0xff) % 7 - 3) * 0.9f }
                            Stamp(
                                imageFile(photo.name),
                                Modifier.fillMaxWidth().staggeredEntrance(index),
                                rotation = tilt,
                                compact = true,
                                onClick = { onOpenEntry(photo.entryId) },
                            )
                        }
                    }
                    if (loading) {
                        item(key = "loading", span = { GridItemSpan(maxLineSpan) }) {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}
