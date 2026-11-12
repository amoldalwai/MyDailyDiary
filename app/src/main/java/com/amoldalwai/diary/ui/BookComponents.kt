package com.amoldalwai.diary.ui

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.amoldalwai.diary.data.Book
import com.amoldalwai.diary.data.BookResult
import com.amoldalwai.diary.data.BookStatus
import com.amoldalwai.diary.data.FinishInfo
import com.amoldalwai.diary.data.OpenLibrary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.io.File
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The stored cover, or a tinted placeholder showing the title when there is none. */
@Composable
fun BookCover(book: Book, cover: (String) -> File, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier
            .aspectRatio(0.68f)
            .shadow(4.dp, shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (book.cover != null) {
            AsyncImage(
                model = cover(book.cover),
                contentDescription = book.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                book.title,
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

@Composable
fun ProgressBar(progress: Float, track: Color, fill: Color, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "bookProgress")
    Box(modifier.height(6.dp).clip(CircleShape).background(track)) {
        Box(Modifier.fillMaxWidth(animated).height(6.dp).clip(CircleShape).background(fill))
    }
}

/** One book being read: cover, title and the page reached on the day being viewed. */
@Composable
fun BookProgressRow(
    book: Book,
    page: Int,
    ink: Color,
    cover: (String) -> File,
    onPage: (Int) -> Unit,
) {
    var text by remember(book.id, page) { mutableStateOf(page.toString()) }
    LaunchedEffect(text) {
        val value = text.toIntOrNull()
        if (value != null && value != page) {
            delay(700)
            onPage(value.coerceIn(0, book.totalPages ?: 99_999))
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BookCover(book, cover, Modifier.width(44.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(book.title, color = ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (book.authors.isNotBlank()) {
                Text(book.authors, color = ink.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Page", color = ink.copy(alpha = 0.6f), fontSize = 13.sp)
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = text,
                    onValueChange = { text = it.filter(Char::isDigit).take(5) },
                    singleLine = true,
                    textStyle = TextStyle(color = ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                    cursorBrush = SolidColor(ink),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .width(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ink.copy(alpha = 0.08f))
                        .padding(vertical = 6.dp),
                )
                book.totalPages?.let { Text("  / $it", color = ink.copy(alpha = 0.6f), fontSize = 13.sp) }
                Spacer(Modifier.weight(1f))
                Text(
                    "+10",
                    color = ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .bouncyClick { onPage((page + 10).coerceAtMost(book.totalPages ?: 99_999)) }
                        .clip(CircleShape)
                        .background(ink.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            if (book.totalPages != null) {
                Spacer(Modifier.height(6.dp))
                ProgressBar(page.toFloat() / book.totalPages, ink.copy(alpha = 0.12f), ink)
            }
        }
    }
}

private val statusLabels = mapOf(
    BookStatus.Reading to "Reading now",
    BookStatus.Want to "Want to read",
    BookStatus.Finished to "Already read",
)

@Composable
fun StatusChips(selected: BookStatus, onSelect: (BookStatus) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BookStatus.entries.forEach { status ->
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

/** Search Open Library by title; picking a result lets you choose the shelf and page details. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSearchSheet(onDismiss: () -> Unit, onAdd: (BookResult, BookStatus, Int?, Int, FinishInfo?) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<BookResult>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<BookResult?>(null) }
    var status by remember { mutableStateOf(BookStatus.Want) }
    var askFinish by remember { mutableStateOf(false) }
    var total by remember { mutableStateOf("") }
    var current by remember { mutableStateOf("") }

    LaunchedEffect(query) {
        error = null
        if (query.trim().length < 2) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(500)
        loading = true
        try {
            results = OpenLibrary.search(query.trim())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = "Couldn't reach Open Library. Check your connection."
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
                Text("Add a book", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Searches Open Library by title or author.", fontSize = 13.sp, color = colors.onSurfaceVariant)
                Text("💡 Tip: add the year to the title for a closer match, like \"Dune 1965\".", fontSize = 13.sp, color = colors.primary)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    placeholder = { Text("Search by title or author") },
                )
                Spacer(Modifier.height(10.dp))
                when {
                    loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.5.dp)
                    }

                    error != null -> Text(error.orEmpty(), color = colors.error)
                    results.isNotEmpty() -> LazyColumn(Modifier.heightIn(max = 460.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(results, key = { it.key.ifEmpty { it.title } + it.coverId }) { result ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        selected = result
                                        total = result.pages?.toString().orEmpty()
                                        current = ""
                                    }
                                    .padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ResultCover(result, Modifier.width(46.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(result.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    val line = listOfNotNull(result.authors.ifBlank { null }, result.year?.toString()).joinToString(" · ")
                                    if (line.isNotEmpty()) Text(line, fontSize = 13.sp, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    result.pages?.let { Text("$it pages", fontSize = 12.sp, color = colors.onSurfaceVariant) }
                                }
                            }
                        }
                    }

                    query.trim().length >= 2 -> Text("No matches yet. Try fewer words, or add it by hand below.", color = colors.onSurfaceVariant)
                }
                if (query.trim().length >= 2 && !loading) {
                    TextButton(onClick = {
                        selected = BookResult(key = "", title = query.trim(), authors = "", year = null, coverId = null, pages = null)
                        total = ""
                        current = ""
                    }) { Text("Add “${query.trim()}” by hand") }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ResultCover(pick, Modifier.width(72.dp))
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(pick.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        val line = listOfNotNull(pick.authors.ifBlank { null }, pick.year?.toString()).joinToString(" · ")
                        if (line.isNotEmpty()) Text(line, color = colors.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(16.dp))
                StatusChips(status) { status = it }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = total,
                        onValueChange = { total = it.filter(Char::isDigit).take(5) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Total pages") },
                        supportingText = { if (pick.pages == null) Text("Not listed, add it yourself") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    if (status == BookStatus.Reading) {
                        OutlinedTextField(
                            value = current,
                            onValueChange = { current = it.filter(Char::isDigit).take(5) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text("I'm on page") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = { selected = null }) { Text("Back") }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = {
                        val totalPages = total.toIntOrNull()?.takeIf { it > 0 }
                        val reachedEnd = status == BookStatus.Reading && totalPages != null && (current.toIntOrNull() ?: 0) >= totalPages
                        if (status == BookStatus.Finished || reachedEnd) askFinish = true else {
                            onAdd(pick, status, totalPages, current.toIntOrNull() ?: 0, null)
                            onDismiss()
                        }
                    }) { Text("Add to library") }
                }
                if (askFinish) {
                    FinishDialog(
                        title = pick.title,
                        noun = "book",
                        startRating = 3,
                        askYear = true,
                        onConfirm = { finish ->
                            askFinish = false
                            onAdd(pick, BookStatus.Finished, total.toIntOrNull()?.takeIf { it > 0 }, current.toIntOrNull() ?: 0, finish)
                            onDismiss()
                        },
                        onCancel = { askFinish = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultCover(result: BookResult, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier.aspectRatio(0.68f).clip(shape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (result.coverId != null) {
            AsyncImage(
                model = OpenLibrary.coverUrl(result.coverId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text("📖", fontSize = 22.sp)
        }
    }
}

/** The book's title page: status, reading progress, then rating and review. */
@Composable
fun BookDetailSheet(
    book: Book,
    cover: (String) -> File,
    onDismiss: () -> Unit,
    onSave: (Book) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var status by remember(book.id) { mutableStateOf(book.status) }
    var total by remember(book.id) { mutableStateOf(book.totalPages?.toString().orEmpty()) }
    var current by remember(book.id) { mutableStateOf(book.currentPage.toString()) }
    var rating by remember(book.id) { mutableStateOf(book.rating) }
    var review by remember(book.id) { mutableStateOf(book.review) }
    val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    fun date(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(dateFormat)
    val totalPages = total.toIntOrNull()?.takeIf { it > 0 }

    MediaDetailSheet(
        title = book.title,
        noun = "book",
        cover = book.cover?.let(cover),
        rating = rating,
        facts = listOfNotNull(book.authors.ifBlank { null }, book.year?.toString()),
        notes = listOfNotNull(
            book.startedAt?.let { "Started" to date(it) },
            book.finishedAt?.let { "Finished" to date(it) }
                ?: if (book.status == BookStatus.Finished) "Finished" to (book.finishedYear?.toString() ?: "Long time ago") else null,
        ),
        onDismiss = onDismiss,
        onDelete = onDelete,
        onSave = {
            val page = (current.toIntOrNull() ?: book.currentPage).let { if (totalPages != null) it.coerceAtMost(totalPages) else it }
            // Reaching the last page finishes the book, which then asks for a rating.
            val reachedEnd = status == BookStatus.Reading && totalPages != null && page >= totalPages
            onSave(
                book.copy(
                    status = if (reachedEnd) BookStatus.Finished else status,
                    rating = rating,
                    review = review.trim(),
                    totalPages = totalPages,
                    currentPage = page,
                ),
            )
        },
    ) {
        DetailSection("Shelf", card = false, index = 1) { StatusChips(status) { status = it } }
        DetailSection("Pages", index = 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = total,
                    onValueChange = { total = it.filter(Char::isDigit).take(5) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Total pages") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                if (status == BookStatus.Reading) {
                    OutlinedTextField(
                        value = current,
                        onValueChange = { current = it.filter(Char::isDigit).take(5) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Current page") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            }
            if (totalPages != null && status == BookStatus.Reading) {
                Spacer(Modifier.height(12.dp))
                ProgressBar((current.toIntOrNull() ?: 0).toFloat() / totalPages, colors.surface, colors.primary, Modifier.fillMaxWidth())
            }
        }
        DetailSection("Your take", index = 3) { ReviewSection(rating, review, { rating = it }, { review = it }) }
    }
}
