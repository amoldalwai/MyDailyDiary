package com.amoldalwai.diary.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.amoldalwai.diary.data.WatchItem
import com.amoldalwai.diary.data.WatchKind
import com.amoldalwai.diary.data.WatchResult
import com.amoldalwai.diary.data.WatchStatus
import java.io.File

/** The movies and series library: the shared media library fed with watch items, with a Movies / Series filter. */
@Composable
fun WatchScreen(
    items: List<WatchItem>,
    cover: (String) -> File,
    onAdd: (WatchResult, WatchKind, WatchStatus, Int, Int) -> Unit,
    onUpdate: (WatchItem) -> Unit,
    onDelete: (WatchItem) -> Unit,
    onBack: () -> Unit,
) {
    val shelves = remember {
        listOf(WatchStatus.Watching, WatchStatus.Watched, WatchStatus.Watchlist).map { MediaShelf(it.name, watchStatusLabels.getValue(it)) }
    }
    val kinds = remember { listOf(MediaShelf(WatchKind.Movie.name, "Movies"), MediaShelf(WatchKind.Series.name, "Series")) }
    val cards = remember(items) {
        items.map {
            MediaItem(
                id = it.id,
                shelf = it.status.name,
                title = it.title,
                subtitle = it.year?.toString().orEmpty(),
                cover = it.cover?.let(cover),
                rating = it.rating,
                badge = watchKindLabels.getValue(it.kind),
                kind = it.kind.name,
                note = if (it.kind == WatchKind.Series && (it.season > 0 || it.episode > 0)) "S${it.season} · E${it.episode}" else null,
            )
        }
    }
    val watched = items.count { it.status == WatchStatus.Watched }

    MediaLibraryScreen(
        title = "Movies & Series",
        emoji = "🎬",
        summary = "${items.size} ${if (items.size == 1) "title" else "titles"} · $watched watched",
        shelves = shelves,
        items = cards,
        kinds = kinds,
        emptyTitle = { shelf ->
            when (shelf?.key) {
                WatchStatus.Watching.name -> "Nothing on right now"
                WatchStatus.Watched.name -> "Nothing watched yet"
                WatchStatus.Watchlist.name -> "Your watchlist is empty"
                else -> "Your library is empty"
            }
        },
        emptyHint = "Tap + to add a movie or series.",
        onBack = onBack,
        addSheet = { dismiss -> WatchSearchSheet(onDismiss = dismiss, onAdd = onAdd) },
        detailSheet = { id, dismiss ->
            items.firstOrNull { it.id == id }?.let { item ->
                WatchDetailSheet(
                    item = item,
                    cover = cover,
                    onDismiss = dismiss,
                    onSave = onUpdate,
                    onDelete = { onDelete(item) },
                )
            }
        },
    )
}
