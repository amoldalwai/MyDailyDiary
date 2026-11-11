package com.amoldalwai.diary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.amoldalwai.diary.data.Book
import com.amoldalwai.diary.data.BookResult
import com.amoldalwai.diary.data.BookStatus
import com.amoldalwai.diary.data.Game
import com.amoldalwai.diary.data.GameResult
import com.amoldalwai.diary.data.GameStatus
import com.amoldalwai.diary.data.WatchItem
import com.amoldalwai.diary.data.WatchKind
import com.amoldalwai.diary.data.WatchResult
import com.amoldalwai.diary.data.WatchStatus
import java.io.File
import java.time.LocalDate

/** What journal pages need from the Books, Games and Movies & Series libraries. */
class MediaUi(
    val booksEnabled: Boolean,
    val gamesEnabled: Boolean,
    val watchEnabled: Boolean,
    val books: List<Book>,
    val games: List<Game>,
    val watch: List<WatchItem>,
    val bookCover: (String) -> File,
    val gameCover: (String) -> File,
    val watchCover: (String) -> File,
    val pageOn: (Book, LocalDate) -> Int,
    val loadLogs: (LocalDate) -> Unit,
    val setPage: (Book, LocalDate, Int) -> Unit,
    val addBook: (BookResult, BookStatus, Int?, Int, (String) -> Unit) -> Unit,
    val addGame: (GameResult, String, GameStatus, (String) -> Unit) -> Unit,
    val addWatch: (WatchResult, WatchKind, WatchStatus, Int, Int, (String) -> Unit) -> Unit,
    val setBookStatus: (String, BookStatus) -> Unit,
    val setGameStatus: (String, GameStatus) -> Unit,
    val setWatchStatus: (String, WatchStatus) -> Unit,
) {
    val anyEnabled: Boolean get() = booksEnabled || gamesEnabled || watchEnabled
}

private const val BOOK = "B"
private const val GAME = "G"
private const val WATCH = "W"

private fun link(kind: String, id: String) = "$kind:$id"

private val bookLabels = listOf("Want to read", "Reading", "Finished")
private val gameLabels = listOf("Backlog", "Playing", "Completed")
private val watchLabels = listOf("Watchlist", "Watching", "Watched")
private val bookOrder = listOf(BookStatus.Want, BookStatus.Reading, BookStatus.Finished)
private val gameOrder = listOf(GameStatus.Backlog, GameStatus.Playing, GameStatus.Completed)
private val watchOrder = listOf(WatchStatus.Watchlist, WatchStatus.Watching, WatchStatus.Watched)

/**
 * The bottom block of a page: library items attached to it (with status chips that update the library),
 * and one button each to attach a book, a movie or series, or a game.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryMediaSection(
    links: List<String>,
    onLinks: (List<String>) -> Unit,
    date: LocalDate,
    ui: MediaUi,
    ink: Color,
    onInk: Color,
    modifier: Modifier = Modifier,
) {
    if (!ui.anyEnabled) return
    val current by rememberUpdatedState(links)
    val setLinks by rememberUpdatedState(onLinks)
    var picker by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(date) { ui.loadLogs(date) }

    fun attach(id: String, kind: String) {
        val key = link(kind, id)
        if (key !in current) setLinks(current + key)
    }

    fun openAdd(kind: String) {
        val hasLibrary = when (kind) {
            BOOK -> ui.books.any { link(BOOK, it.id) !in current }
            GAME -> ui.games.any { link(GAME, it.id) !in current }
            else -> ui.watch.any { link(WATCH, it.id) !in current }
        }
        if (hasLibrary) picker = kind else search = kind
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        links.forEach { key ->
            val id = key.substringAfter(':')
            when (key.substringBefore(':')) {
                BOOK -> ui.books.firstOrNull { it.id == id }?.let { book ->
                    LinkedCard(ink, onRemove = { setLinks(current - key) }) {
                        if (book.status == BookStatus.Reading) {
                            BookProgressRow(book, ui.pageOn(book, date), ink, ui.bookCover) { ui.setPage(book, date, it) }
                        } else {
                            LinkedHeader(book.title, book.authors, book.cover?.let(ui.bookCover), ink)
                        }
                        StatusPills(bookLabels, bookOrder.indexOf(book.status), ink, onInk) { ui.setBookStatus(book.id, bookOrder[it]) }
                    }
                }

                GAME -> ui.games.firstOrNull { it.id == id }?.let { game ->
                    LinkedCard(ink, onRemove = { setLinks(current - key) }) {
                        LinkedHeader(game.title, game.platform, game.cover?.let(ui.gameCover), ink)
                        StatusPills(gameLabels, gameOrder.indexOf(game.status), ink, onInk) { ui.setGameStatus(game.id, gameOrder[it]) }
                    }
                }

                WATCH -> ui.watch.firstOrNull { it.id == id }?.let { item ->
                    LinkedCard(ink, onRemove = { setLinks(current - key) }) {
                        val sub = listOfNotNull(watchKindLabels[item.kind], item.year?.toString()).joinToString(" · ")
                        LinkedHeader(item.title, sub, item.cover?.let(ui.watchCover), ink)
                        StatusPills(watchLabels, watchOrder.indexOf(item.status), ink, onInk) { ui.setWatchStatus(item.id, watchOrder[it]) }
                    }
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (ui.booksEnabled) AddPill("Book", ink) { openAdd(BOOK) }
            if (ui.watchEnabled) AddPill("Movie / series", ink) { openAdd(WATCH) }
            if (ui.gamesEnabled) AddPill("Game", ink) { openAdd(GAME) }
        }
    }

    when (picker) {
        BOOK -> LibraryPickerSheet(
            title = "Attach a book",
            entries = ui.books.filter { link(BOOK, it.id) !in links }.map { PickEntry(it.id, it.title, it.authors, it.cover?.let(ui.bookCover)) },
            onPick = { attach(it, BOOK) },
            onSearch = { search = BOOK },
            onDismiss = { picker = null },
        )

        GAME -> LibraryPickerSheet(
            title = "Attach a game",
            entries = ui.games.filter { link(GAME, it.id) !in links }.map { PickEntry(it.id, it.title, it.platform, it.cover?.let(ui.gameCover)) },
            onPick = { attach(it, GAME) },
            onSearch = { search = GAME },
            onDismiss = { picker = null },
        )

        WATCH -> LibraryPickerSheet(
            title = "Attach a movie or series",
            entries = ui.watch.filter { link(WATCH, it.id) !in links }.map {
                PickEntry(it.id, it.title, listOfNotNull(watchKindLabels[it.kind], it.year?.toString()).joinToString(" · "), it.cover?.let(ui.watchCover))
            },
            onPick = { attach(it, WATCH) },
            onSearch = { search = WATCH },
            onDismiss = { picker = null },
        )
    }

    when (search) {
        BOOK -> BookSearchSheet(
            onDismiss = { search = null },
            onAdd = { result, status, total, now -> ui.addBook(result, status, total, now) { attach(it, BOOK) } },
        )

        GAME -> GameSearchSheet(
            onDismiss = { search = null },
            onAdd = { result, platform, status -> ui.addGame(result, platform, status) { attach(it, GAME) } },
        )

        WATCH -> WatchSearchSheet(
            onDismiss = { search = null },
            onAdd = { result, kind, status, season, episode -> ui.addWatch(result, kind, status, season, episode) { attach(it, WATCH) } },
        )
    }
}

@Composable
private fun LinkedCard(ink: Color, onRemove: () -> Unit, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(Modifier.fillMaxWidth().clip(shape).background(ink.copy(alpha = 0.07f))) {
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            content()
        }
        Box(
            Modifier.align(Alignment.TopEnd).size(34.dp).clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Close, "Remove from this page", tint = ink.copy(alpha = 0.5f), modifier = Modifier.size(16.dp)) }
    }
}

@Composable
private fun LinkedHeader(title: String, subtitle: String, cover: File?, ink: Color) {
    Row(Modifier.fillMaxWidth().padding(end = 28.dp), verticalAlignment = Alignment.CenterVertically) {
        CoverThumb(cover, title, Modifier.width(44.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = ink.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun CoverThumb(file: File?, title: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier.aspectRatio(0.68f).clip(shape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (file != null) {
            AsyncImage(model = file, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text(
                title,
                modifier = Modifier.padding(4.dp),
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusPills(labels: List<String>, selected: Int, ink: Color, onInk: Color, onSelect: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEachIndexed { index, label ->
            val on = index == selected
            Text(
                label,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (on) ink else ink.copy(alpha = 0.1f))
                    .clickable { if (!on) onSelect(index) }
                    .padding(horizontal = 13.dp, vertical = 7.dp),
                color = if (on) onInk else ink,
                fontSize = 13.sp,
                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun AddPill(label: String, ink: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .bouncyClick(onClick)
            .clip(CircleShape)
            .background(ink.copy(alpha = 0.1f))
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Add, null, tint = ink, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

private class PickEntry(val id: String, val title: String, val subtitle: String, val cover: File?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryPickerSheet(
    title: String,
    entries: List<PickEntry>,
    onPick: (String) -> Unit,
    onSearch: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.primaryContainer)
                    .clickable {
                        onDismiss()
                        onSearch()
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = colors.onPrimaryContainer)
                Spacer(Modifier.width(10.dp))
                Text("Search and add a new one", color = colors.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            Text("From your library", fontSize = 12.sp, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(entries, key = { it.id }) { entry ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                onPick(entry.id)
                                onDismiss()
                            }
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CoverThumb(entry.cover, entry.title, Modifier.width(40.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(entry.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (entry.subtitle.isNotBlank()) {
                                Text(entry.subtitle, fontSize = 12.sp, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}
