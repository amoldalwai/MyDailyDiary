package com.amoldalwai.diary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.amoldalwai.diary.data.Book
import com.amoldalwai.diary.data.BookStatus
import com.amoldalwai.diary.data.Game
import com.amoldalwai.diary.data.GameStatus
import com.amoldalwai.diary.data.WatchItem
import com.amoldalwai.diary.data.WatchKind
import com.amoldalwai.diary.data.WatchStatus
import java.io.File

private data class LibraryTile(
    val title: String,
    val count: String,
    val emoji: String,
    val gradient: List<Color>,
    val covers: List<File>,
    val onClick: () -> Unit,
)

/** One hub for everything collected: image cards for each section, and what is in progress right now. */
@Composable
fun LibraryScreen(
    books: List<Book>,
    games: List<Game>,
    watch: List<WatchItem>,
    albumTotal: Int,
    booksEnabled: Boolean,
    gamesEnabled: Boolean,
    watchEnabled: Boolean,
    bookCover: (String) -> File,
    gameCover: (String) -> File,
    watchCover: (String) -> File,
    onOpenBooks: () -> Unit,
    onOpenGames: () -> Unit,
    onOpenWatch: () -> Unit,
    onOpenAlbum: () -> Unit,
    onUpdateBook: (Book) -> Unit,
    onDeleteBook: (Book) -> Unit,
    onUpdateGame: (Game) -> Unit,
    onDeleteGame: (Game) -> Unit,
    onUpdateWatch: (WatchItem) -> Unit,
    onDeleteWatch: (WatchItem) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    SystemBarsStyle(darkIcons = !appIsDark())
    var query by rememberSaveable { mutableStateOf("") }
    var openBook by remember { mutableStateOf<String?>(null) }
    var openGame by remember { mutableStateOf<String?>(null) }
    var openWatch by remember { mutableStateOf<String?>(null) }
    val hits = remember(query, books, games, watch, booksEnabled, gamesEnabled, watchEnabled) {
        searchLibrary(query, if (booksEnabled) books else emptyList(), if (gamesEnabled) games else emptyList(), if (watchEnabled) watch else emptyList(), bookCover, gameCover, watchCover)
    }
    val tiles = remember(books, games, watch, albumTotal, booksEnabled, gamesEnabled, watchEnabled) {
        buildList {
            if (booksEnabled) {
                add(
                    LibraryTile(
                        "Books", "${books.size} ${if (books.size == 1) "book" else "books"} · ${books.count { it.status == BookStatus.Reading }} reading", "📚",
                        listOf(Color(0xFF6B8F71), Color(0xFF3F6149)), books.mapNotNull { it.cover }.take(3).map(bookCover), onOpenBooks,
                    ),
                )
            }
            if (gamesEnabled) {
                add(
                    LibraryTile(
                        "Games", "${games.size} ${if (games.size == 1) "game" else "games"} · ${games.count { it.status == GameStatus.Playing }} playing", "🎮",
                        listOf(Color(0xFF6A6FB0), Color(0xFF3B3F7C)), games.mapNotNull { it.cover }.take(3).map(gameCover), onOpenGames,
                    ),
                )
            }
            if (watchEnabled) {
                add(
                    LibraryTile(
                        "Movies & Series", "${watch.size} ${if (watch.size == 1) "title" else "titles"} · ${watch.count { it.status == WatchStatus.Watching }} watching", "🎬",
                        listOf(Color(0xFFC0694F), Color(0xFF8A3B2A)), watch.mapNotNull { it.cover }.take(3).map(watchCover), onOpenWatch,
                    ),
                )
            }
            add(
                LibraryTile(
                    "Stamp album", "$albumTotal ${if (albumTotal == 1) "photo" else "photos"}", "📮",
                    listOf(Color(0xFFB08968), Color(0xFF7A5A3F)), emptyList(), onOpenAlbum,
                ),
            )
        }
    }

    // What is in progress: cards that open the matching library.
    val continuing = remember(books, games, watch, booksEnabled, gamesEnabled, watchEnabled) {
        buildList {
            if (booksEnabled) books.filter { it.status == BookStatus.Reading }.forEach {
                add(MediaItem(it.id, "", it.title, "", it.cover?.let(bookCover), it.rating, progress = it.progress.takeIf { _ -> it.totalPages != null }, badge = "Book", kind = "book"))
            }
            if (watchEnabled) watch.filter { it.status == WatchStatus.Watching }.forEach {
                add(
                    MediaItem(
                        it.id, "", it.title, "", it.cover?.let(watchCover), it.rating, badge = if (it.kind == WatchKind.Series) "Series" else "Movie",
                        kind = "watch", note = if (it.kind == WatchKind.Series && (it.season > 0 || it.episode > 0)) "S${it.season} · E${it.episode}" else null,
                    ),
                )
            }
            if (gamesEnabled) games.filter { it.status == GameStatus.Playing }.forEach {
                add(MediaItem(it.id, "", it.title, "", it.cover?.let(gameCover), it.rating, badge = it.platform, kind = "game"))
            }
        }
    }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        FloatingBlobs()
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "title") {
                Column(Modifier.staggeredEntrance(0)) {
                    Text("Library", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = colors.onBackground)
                    Text("Everything you read, play and watch", fontSize = 13.sp, color = colors.onSurfaceVariant)
                }
            }
            item(key = "search") {
                LibrarySearchField(query, { query = it }, Modifier.staggeredEntrance(1))
            }
            if (query.isNotBlank()) {
                if (hits.isEmpty()) {
                    item(key = "nohits") {
                        Text("Nothing in your library matches “${query.trim()}”.", color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                    }
                } else {
                    items(hits.size, key = { hits[it].kind + hits[it].id }) { i ->
                        val hit = hits[i]
                        SearchHitRow(hit) {
                            when (hit.kind) {
                                "book" -> openBook = hit.id
                                "game" -> openGame = hit.id
                                else -> openWatch = hit.id
                            }
                        }
                    }
                }
            }
            if (query.isBlank()) tiles.chunked(2).forEachIndexed { row, pair ->
                item(key = "row$row") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEachIndexed { i, tile -> LibraryCard(tile, Modifier.weight(1f).staggeredEntrance(1 + row * 2 + i)) }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            if (query.isBlank() && continuing.isNotEmpty()) {
                item(key = "continue") {
                    Column(Modifier.padding(top = 8.dp)) {
                        Text("Continue", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onBackground)
                        Spacer(Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 20.dp)) {
                            itemsIndexed(continuing, key = { _, m -> m.kind + m.id }) { _, item ->
                                MediaCard(item, Modifier.width(112.dp)) {
                                    when (item.kind) {
                                        "book" -> onOpenBooks()
                                        "game" -> onOpenGames()
                                        else -> onOpenWatch()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    openBook?.let { id ->
        books.firstOrNull { it.id == id }?.let { book ->
            BookDetailSheet(book = book, cover = bookCover, onDismiss = { openBook = null }, onSave = onUpdateBook, onDelete = { onDeleteBook(book) })
        }
    }
    openGame?.let { id ->
        games.firstOrNull { it.id == id }?.let { game ->
            GameDetailSheet(game = game, cover = gameCover, onDismiss = { openGame = null }, onSave = onUpdateGame, onDelete = { onDeleteGame(game) })
        }
    }
    openWatch?.let { id ->
        watch.firstOrNull { it.id == id }?.let { item ->
            WatchDetailSheet(item = item, cover = watchCover, onDismiss = { openWatch = null }, onSave = onUpdateWatch, onDelete = { onDeleteWatch(item) })
        }
    }
}

private class SearchHit(val id: String, val kind: String, val title: String, val subtitle: String, val label: String, val cover: File?, val rating: Int?)

private fun searchLibrary(
    query: String,
    books: List<Book>,
    games: List<Game>,
    watch: List<WatchItem>,
    bookCover: (String) -> File,
    gameCover: (String) -> File,
    watchCover: (String) -> File,
): List<SearchHit> {
    val tokens = query.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return emptyList()
    fun matches(vararg fields: String) = fields.joinToString(" ").lowercase().let { hay -> tokens.all { it in hay } }
    return buildList {
        books.filter { matches(it.title, it.authors, it.year?.toString().orEmpty(), it.status.name, "book") }.forEach {
            add(SearchHit(it.id, "book", it.title, it.authors, "Book · ${it.status.name.replace("Want", "Want to read")}", it.cover?.let(bookCover), it.rating))
        }
        games.filter { matches(it.title, it.developer, it.platform, it.year?.toString().orEmpty(), it.status.name, "game") }.forEach {
            add(SearchHit(it.id, "game", it.title, listOf(it.platform, it.developer).filter(String::isNotBlank).joinToString(" · "), "Game · ${it.status.name}", it.cover?.let(gameCover), it.rating))
        }
        watch.filter { matches(it.title, it.info, it.year?.toString().orEmpty(), it.status.name, it.kind.name) }.forEach {
            add(SearchHit(it.id, "watch", it.title, listOfNotNull(it.year?.toString(), it.info.ifBlank { null }).joinToString(" · "), "${watchKindLabels.getValue(it.kind)} · ${it.status.name}", it.cover?.let(watchCover), it.rating))
        }
    }.sortedBy { it.title.lowercase() }
}

@Composable
private fun LibrarySearchField(query: String, onQuery: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().clip(CircleShape).background(colors.surface).border(1.dp, colors.outline.copy(alpha = 0.5f), CircleShape).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = colors.onSurfaceVariant)
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = TextStyle(color = colors.onSurface, fontSize = 16.sp),
            cursorBrush = SolidColor(colors.primary),
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) Text("Search books, games, movies and series", color = colors.onSurfaceVariant, fontSize = 15.sp)
                    inner()
                }
            },
        )
        if (query.isNotEmpty()) {
            Icon(Icons.Rounded.Close, "Clear", tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp).clickable { onQuery("") })
        }
    }
}

@Composable
private fun SearchHitRow(hit: SearchHit, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().bouncyClick(onClick).clip(shape).background(colors.surface).border(1.dp, colors.outline.copy(alpha = 0.5f), shape).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(48.dp).aspectRatio(0.68f).clip(RoundedCornerShape(8.dp)).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
            if (hit.cover != null) {
                AsyncImage(model = hit.cover, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text(hit.title.take(1).uppercase(), fontWeight = FontWeight.Bold, color = colors.onPrimaryContainer)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(hit.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.onSurface)
            if (hit.subtitle.isNotBlank()) Text(hit.subtitle, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.onSurfaceVariant)
            Text(hit.label, fontSize = 11.sp, color = colors.primary, fontWeight = FontWeight.Medium)
        }
        hit.rating?.let { Text("★ $it", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ratingColor(it)) }
    }
}

@Composable
private fun LibraryCard(tile: LibraryTile, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier
            .height(176.dp)
            .bouncyClick(tile.onClick)
            .shadow(8.dp, shape)
            .clip(shape)
            .background(Brush.linearGradient(tile.gradient))
            .border(1.dp, Color(0x26FFFFFF), shape),
    ) {
        Box(Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 12.dp).size(width = 84.dp, height = 96.dp)) {
            if (tile.covers.isEmpty()) {
                Text(tile.emoji, fontSize = 46.sp, modifier = Modifier.align(Alignment.Center))
            } else {
                tile.covers.forEachIndexed { i, file ->
                    AsyncImage(
                        model = file,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = (i * 14).dp, top = (i * 5).dp)
                            .size(width = 52.dp, height = 76.dp)
                            .graphicsLayer { rotationZ = (i - 1) * 6f }
                            .shadow(4.dp, RoundedCornerShape(7.dp))
                            .clip(RoundedCornerShape(7.dp)),
                    )
                }
            }
        }
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(tile.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(tile.count, color = Color(0xE6FFFFFF), fontSize = 12.sp)
        }
    }
}
