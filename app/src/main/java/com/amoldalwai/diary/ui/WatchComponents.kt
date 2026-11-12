package com.amoldalwai.diary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.amoldalwai.diary.data.ScreenSearch
import com.amoldalwai.diary.data.FinishInfo
import com.amoldalwai.diary.data.WatchItem
import com.amoldalwai.diary.data.WatchKind
import com.amoldalwai.diary.data.WatchResult
import com.amoldalwai.diary.data.WatchStatus
import com.amoldalwai.diary.data.WikipediaGames
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val watchStatusLabels = mapOf(
    WatchStatus.Watching to "Watching",
    WatchStatus.Watched to "Watched",
    WatchStatus.Watchlist to "Watchlist",
)

val watchKindLabels = mapOf(WatchKind.Movie to "Movie", WatchKind.Series to "Series")

@Composable
private fun WatchStatusChips(selected: WatchStatus, onSelect: (WatchStatus) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(WatchStatus.Watched, WatchStatus.Watching, WatchStatus.Watchlist).forEach { status ->
            val on = status == selected
            Text(
                watchStatusLabels.getValue(status),
                modifier = Modifier
                    .bouncyClick { onSelect(status) }
                    .clip(CircleShape)
                    .background(if (on) colors.primary else colors.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                color = if (on) colors.onPrimary else colors.onSurface,
                fontSize = 13.sp,
                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun KindChips(selected: WatchKind, onSelect: (WatchKind) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WatchKind.entries.forEach { kind ->
            val on = kind == selected
            Text(
                watchKindLabels.getValue(kind),
                modifier = Modifier
                    .bouncyClick { onSelect(kind) }
                    .clip(CircleShape)
                    .background(if (on) colors.secondary else colors.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                color = if (on) colors.onSecondary else colors.onSurface,
                fontSize = 13.sp,
                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

/** A labelled − value + control for a season or episode number; never goes below zero. */
@Composable
private fun Stepper(label: String, value: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    @Composable
    fun Button(symbol: String, delta: Int) {
        Box(
            Modifier
                .size(38.dp)
                .bouncyClick { onChange((value + delta).coerceIn(0, 999)) }
                .clip(CircleShape)
                .background(colors.surface),
            contentAlignment = Alignment.Center,
        ) { Text(symbol, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = colors.onSurface) }
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = colors.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button("−", -1)
            Text("$value", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.onSurface, modifier = Modifier.width(36.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Button("+", 1)
        }
    }
}

@Composable
private fun SeasonEpisode(season: Int, episode: Int, onSeason: (Int) -> Unit, onEpisode: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        Stepper("Season", season, onSeason)
        Stepper("Episode", episode, onEpisode)
    }
}

@Composable
private fun RemoteArt(url: String?, modifier: Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        if (url != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .addHeader("User-Agent", WikipediaGames.USER_AGENT)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Text("🎬", fontSize = 20.sp)
        }
    }
}

/** Search movies (Wikipedia) and series (Wikipedia + TVMaze) in one list; anything missing can be added by hand. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchSearchSheet(onDismiss: () -> Unit, onAdd: (WatchResult, WatchKind, WatchStatus, Int, Int, FinishInfo?) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<WatchResult>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<WatchResult?>(null) }
    var kind by remember { mutableStateOf(WatchKind.Movie) }
    var status by remember { mutableStateOf(WatchStatus.Watchlist) }
    var askFinish by remember { mutableStateOf(false) }
    var season by remember { mutableStateOf(1) }
    var episode by remember { mutableStateOf(0) }
    var year by remember { mutableStateOf("") }

    LaunchedEffect(query) {
        error = null
        if (query.trim().length < 2) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(500)
        loading = true
        val found = runCatching { ScreenSearch.search(query.trim()) }
        currentCoroutineContext().ensureActive()
        found.onSuccess { results = it }.onFailure {
            error = "Couldn't reach Wikipedia or TVMaze. Check your connection."
            results = emptyList()
        }
        loading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
            val pick = selected
            if (pick == null) {
                Text("Add a movie or series", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Searches Wikipedia for films and series, and TVMaze for series posters.", fontSize = 13.sp, color = colors.onSurfaceVariant)
                Text("💡 Tip: add the year to the title for a closer match, like \"Dune 2021\".", fontSize = 13.sp, color = colors.primary)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    placeholder = { Text("Search by title") },
                )
                Spacer(Modifier.height(10.dp))
                when {
                    loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.5.dp)
                    }

                    error != null -> Text(error.orEmpty(), color = colors.error)
                    results.isNotEmpty() -> LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(results, key = { it.source + it.kind + it.title + it.year }) { result ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        kind = result.kind
                                        year = result.year?.toString().orEmpty()
                                        selected = result
                                    }
                                    .padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RemoteArt(result.thumbUrl, Modifier.width(52.dp).aspectRatio(0.68f).clip(RoundedCornerShape(6.dp)))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(result.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    val line = listOfNotNull(watchKindLabels.getValue(result.kind), result.year?.toString(), result.source).joinToString(" · ")
                                    Text(line, fontSize = 12.sp, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (result.info.isNotBlank()) {
                                        Text(result.info, fontSize = 12.sp, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }

                    query.trim().length >= 2 -> Text("No matches. Add it by hand below.", color = colors.onSurfaceVariant)
                }
                if (query.trim().length >= 2 && !loading) {
                    TextButton(onClick = {
                        kind = WatchKind.Movie
                        year = ""
                        selected = WatchResult(WatchKind.Movie, query.trim(), null, "", null, null, "Manual")
                    }) { Text("Add “${query.trim()}” by hand") }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (pick.thumbUrl != null) {
                        RemoteArt(pick.thumbUrl, Modifier.width(84.dp).aspectRatio(0.68f).clip(RoundedCornerShape(8.dp)))
                        Spacer(Modifier.width(14.dp))
                    }
                    Column {
                        Text(pick.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (pick.info.isNotBlank()) Text(pick.info, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (pick.source == "Manual") {
                    Text("Type", fontSize = 12.sp, color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    KindChips(kind) { kind = it }
                    Spacer(Modifier.height(14.dp))
                }
                Text("Status", fontSize = 12.sp, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                WatchStatusChips(status) { status = it }
                if (kind == WatchKind.Series && status == WatchStatus.Watching) {
                    Spacer(Modifier.height(14.dp))
                    SeasonEpisode(season, episode, { season = it }, { episode = it })
                }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it.filter(Char::isDigit).take(4) },
                    modifier = Modifier.width(140.dp),
                    singleLine = true,
                    label = { Text("Year") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = { selected = null }) { Text("Back") }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = {
                        if (status == WatchStatus.Watched) askFinish = true else {
                            val watching = kind == WatchKind.Series && status == WatchStatus.Watching
                            onAdd(pick.copy(year = year.toIntOrNull()), kind, status, if (watching) season else 0, if (watching) episode else 0, null)
                            onDismiss()
                        }
                    }) { Text("Add to library") }
                }
                if (askFinish) {
                    FinishDialog(
                        title = pick.title,
                        noun = if (kind == WatchKind.Series) "series" else "movie",
                        startRating = null,
                        askYear = true,
                        onConfirm = { finish ->
                            askFinish = false
                            onAdd(pick.copy(year = year.toIntOrNull()), kind, status, 0, 0, finish)
                            onDismiss()
                        },
                        onCancel = { askFinish = false },
                    )
                }
            }
        }
    }
}

/** The title page for a movie or series: shelf, season/episode for series, then rating and review. */
@Composable
fun WatchDetailSheet(
    item: WatchItem,
    cover: (String) -> File,
    onDismiss: () -> Unit,
    onSave: (WatchItem) -> Unit,
    onDelete: () -> Unit,
) {
    var status by remember(item.id) { mutableStateOf(item.status) }
    var season by remember(item.id) { mutableStateOf(item.season) }
    var episode by remember(item.id) { mutableStateOf(item.episode) }
    var rating by remember(item.id) { mutableStateOf(item.rating) }
    var review by remember(item.id) { mutableStateOf(item.review) }
    val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    fun date(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(dateFormat)

    MediaDetailSheet(
        title = item.title,
        noun = if (item.kind == WatchKind.Series) "series" else "movie",
        cover = item.cover?.let(cover),
        rating = rating,
        facts = listOfNotNull(watchKindLabels.getValue(item.kind), item.year?.toString(), item.info.ifBlank { null }),
        notes = listOfNotNull(
            item.startedAt?.let { "Started" to date(it) },
            item.finishedAt?.let { "Finished" to date(it) }
                ?: if (item.status == WatchStatus.Watched) "Finished" to (item.finishedYear?.toString() ?: "Long time ago") else null,
        ),
        onDismiss = onDismiss,
        onDelete = onDelete,
        onSave = { onSave(item.copy(status = status, season = season, episode = episode, rating = rating, review = review.trim())) },
    ) {
        DetailSection("Shelf", card = false, index = 1) { WatchStatusChips(status) { status = it } }
        if (item.kind == WatchKind.Series) {
            DetailSection("Progress", index = 2) {
                SeasonEpisode(season, episode, { season = it }, { episode = it })
                Spacer(Modifier.height(12.dp))
                TextButton(
                    onClick = {
                        if (season == 0) season = 1
                        episode += 1
                        if (status == WatchStatus.Watchlist) status = WatchStatus.Watching
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text("Watched the next episode") }
            }
        }
        DetailSection("Your take", index = 3) { ReviewSection(rating, review, { rating = it }, { review = it }) }
    }
}
