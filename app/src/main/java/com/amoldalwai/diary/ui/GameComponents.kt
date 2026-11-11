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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import com.amoldalwai.diary.data.WikipediaGames
import com.amoldalwai.diary.data.Game
import com.amoldalwai.diary.data.GameResult
import com.amoldalwai.diary.data.GameStatus
import com.amoldalwai.diary.data.NotAGameException
import com.amoldalwai.diary.data.SteamStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val gamePlatforms = listOf("PC", "Xbox", "PlayStation", "Switch", "Mobile", "Other")

private val statusLabels = mapOf(
    GameStatus.Playing to "Playing now",
    GameStatus.Completed to "Completed",
    GameStatus.Backlog to "Backlog",
)

/** The stored cover, or a tinted placeholder showing the title when there is none. */
@Composable
fun GameCover(game: Game, cover: (String) -> File, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier
            .aspectRatio(0.68f)
            .shadow(4.dp, shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (game.cover != null) {
            AsyncImage(
                model = cover(game.cover),
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                game.title,
                modifier = Modifier.padding(6.dp),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

/** Steam's portrait art, or the wide header cropped to a portrait when a game has none. */
@Composable
private fun SteamArt(appId: Long, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = SteamStore.portraitUrl(appId),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    ) {
        if (painter.state is AsyncImagePainter.State.Error) {
            AsyncImage(
                model = SteamStore.headerUrl(appId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            SubcomposeAsyncImageContent()
        }
    }
}

/** Art for a search result: Steam's portrait, a Wikipedia infobox image, or a placeholder. */
@Composable
private fun GameArt(result: GameResult, modifier: Modifier = Modifier) {
    when {
        result.appId > 0 -> SteamArt(result.appId, modifier)
        result.coverUrl != null -> AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(result.coverUrl)
                .addHeader("User-Agent", WikipediaGames.USER_AGENT)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
        else -> Box(modifier, contentAlignment = Alignment.Center) { Text("🎮", fontSize = 20.sp) }
    }
}

@Composable
private fun StatusChips(selected: GameStatus, onSelect: (GameStatus) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GameStatus.entries.forEach { status ->
            val on = status == selected
            Text(
                statusLabels.getValue(status),
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
private fun PlatformChips(selected: String, onSelect: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        gamePlatforms.forEach { platform ->
            val on = platform == selected
            Text(
                platform,
                modifier = Modifier
                    .bouncyClick { onSelect(platform) }
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

/** Search Steam (PC) and Wikipedia (every platform) together; anything missing can be added by hand. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSearchSheet(onDismiss: () -> Unit, onAdd: (GameResult, String, GameStatus) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<GameResult>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<GameResult?>(null) }
    var platform by remember { mutableStateOf("PC") }
    var status by remember { mutableStateOf(GameStatus.Completed) }
    var year by remember { mutableStateOf("") }

    LaunchedEffect(query) {
        error = null
        if (query.trim().length < 2) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(500)
        loading = true
        val q = query.trim()
        val (steam, wiki) = coroutineScope {
            val s = async { runCatching { SteamStore.search(q) } }
            val w = async { runCatching { WikipediaGames.search(q) } }
            s.await() to w.await()
        }
        currentCoroutineContext().ensureActive()
        if (steam.isFailure && wiki.isFailure) {
            error = "Couldn't reach Steam or Wikipedia. Check your connection."
            results = emptyList()
        } else {
            results = steam.getOrDefault(emptyList()) + wiki.getOrDefault(emptyList())
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
                Text("Add a game", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Searches Steam (PC) and Wikipedia (all platforms).", fontSize = 13.sp, color = colors.onSurfaceVariant)
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
                        items(results, key = { it.source + it.appId + it.title }) { result ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        platform = if (result.source == "Steam") "PC" else "Other"
                                        year = result.year?.toString().orEmpty()
                                        selected = result
                                    }
                                    .padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                GameArt(
                                    result,
                                    Modifier.width(52.dp).aspectRatio(0.68f).clip(RoundedCornerShape(6.dp)).background(colors.primaryContainer),
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(result.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    val line = listOfNotNull(result.source, result.developer.ifBlank { null }, result.year?.toString()).joinToString(" · ")
                                    Text(line, fontSize = 12.sp, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }

                    query.trim().length >= 2 -> Text("No matches on Steam or Wikipedia. Add it by hand below.", color = colors.onSurfaceVariant)
                }
                if (query.trim().length >= 2 && !loading) {
                    TextButton(onClick = {
                        platform = "Xbox"
                        year = ""
                        selected = GameResult(appId = 0, title = query.trim())
                    }) { Text("Add “${query.trim()}” by hand") }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (pick.appId > 0 || pick.coverUrl != null) {
                        GameArt(
                            pick,
                            Modifier.width(84.dp).aspectRatio(0.68f).clip(RoundedCornerShape(8.dp)).background(colors.primaryContainer),
                        )
                        Spacer(Modifier.width(14.dp))
                    }
                    Column {
                        Text(pick.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (pick.developer.isNotBlank()) Text(pick.developer, color = colors.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Status", fontSize = 12.sp, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                StatusChips(status) { status = it }
                Spacer(Modifier.height(14.dp))
                Text("Platform", fontSize = 12.sp, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                PlatformChips(platform) { platform = it }
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
                        onAdd(pick.copy(year = year.toIntOrNull()), platform, status)
                        onDismiss()
                    }) { Text("Add to library") }
                }
            }
        }
    }
}

/** The game's title page: status, platform, then rating and review. */
@Composable
fun GameDetailSheet(
    game: Game,
    cover: (String) -> File,
    onDismiss: () -> Unit,
    onSave: (Game) -> Unit,
    onDelete: () -> Unit,
) {
    var status by remember(game.id) { mutableStateOf(game.status) }
    var platform by remember(game.id) { mutableStateOf(game.platform) }
    var rating by remember(game.id) { mutableStateOf(game.rating) }
    var review by remember(game.id) { mutableStateOf(game.review) }
    val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    fun date(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(dateFormat)

    MediaDetailSheet(
        title = game.title,
        noun = "game",
        cover = game.cover?.let(cover),
        rating = rating,
        facts = listOfNotNull(game.year?.toString(), platform, game.developer.ifBlank { null }),
        notes = listOfNotNull(
            game.startedAt?.let { "Started" to date(it) },
            game.completedAt?.let { "Completed" to date(it) },
        ),
        onDismiss = onDismiss,
        onDelete = onDelete,
        onSave = { onSave(game.copy(status = status, platform = platform, rating = rating, review = review.trim())) },
    ) {
        DetailSection("Shelf", card = false, index = 1) { StatusChips(status) { status = it } }
        DetailSection("Platform", card = false, index = 2) { PlatformChips(platform) { platform = it } }
        DetailSection("Your take", index = 3) { ReviewSection(rating, review, { rating = it }, { review = it }) }
    }
}
