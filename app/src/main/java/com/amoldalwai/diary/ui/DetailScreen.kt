package com.amoldalwai.diary.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.amoldalwai.diary.data.DiaryEntry
import com.amoldalwai.diary.util.PdfExporter
import com.amoldalwai.diary.util.RichText
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val detailTilts = listOf(-2.5f, 2f, -1.5f, 3f)
private const val SWIPE_THRESHOLD_PX = 140f

/** Swipe left for the next remembered day, right for the previous one. */
@Composable
fun DetailScreen(
    entry: DiaryEntry,
    imageFile: (String) -> File,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onFlip: (newer: Boolean, onNone: () -> Unit) -> Unit,
    habitUi: HabitUi,
    mediaUi: MediaUi,
    onLinks: (List<String>) -> Unit,
) {
    val context = LocalContext.current
    AnimatedContent(
        targetState = entry,
        contentKey = { it.id },
        transitionSpec = {
            val newer = targetState.dateEpochDay > initialState.dateEpochDay ||
                (targetState.dateEpochDay == initialState.dateEpochDay && targetState.createdAt > initialState.createdAt)
            val dir = if (newer) 1 else -1
            val spec = tween<androidx.compose.ui.unit.IntOffset>(420, easing = FastOutSlowInEasing)
            (slideInHorizontally(spec) { it * dir } + fadeIn(tween(300))) togetherWith
                (slideOutHorizontally(spec) { -it * dir / 3 } + fadeOut(tween(300)))
        },
        label = "entryFlip",
    ) { page ->
        DetailPage(
            entry = page,
            imageFile = imageFile,
            onBack = onBack,
            onEdit = onEdit,
            onDelete = onDelete,
            habitUi = habitUi,
            mediaUi = mediaUi,
            onLinks = onLinks,
            onSwipe = { newer ->
                onFlip(newer) {
                    Toast.makeText(context, if (newer) "That's your latest entry" else "That's your first entry", Toast.LENGTH_SHORT).show()
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailPage(
    entry: DiaryEntry,
    imageFile: (String) -> File,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    habitUi: HabitUi,
    mediaUi: MediaUi,
    onLinks: (List<String>) -> Unit,
    onSwipe: (newer: Boolean) -> Unit,
) {
    val bg = PageStyles.background(entry.style.backgroundId)
    val font = PageStyles.font(entry.style.fontId)
    val scale = entry.style.fontScale
    var confirmDelete by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<String?>(null) }
    val isPast = entry.date.isBefore(java.time.LocalDate.now())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val startPad by animateDpAsState(if (entry.style.margin) 50.dp else 24.dp, label = "pageStart")
    val body = remember(entry.body, entry.spans) { RichText.toAnnotated(entry.body, entry.spans) }

    SystemBarsStyle(darkIcons = !bg.isDark)

    val weekday = entry.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val dateText = entry.date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()))
    val timeText = remember(entry.createdAt) {
        Instant.ofEpochMilli(entry.createdAt).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
    }

    PageSurface(
        bg,
        Modifier.fillMaxSize().pointerInput(entry.id) {
            var total = 0f
            detectHorizontalDragGestures(
                onDragStart = { total = 0f },
                onDragCancel = { total = 0f },
                onDragEnd = {
                    if (total < -SWIPE_THRESHOLD_PX) onSwipe(true) else if (total > SWIPE_THRESHOLD_PX) onSwipe(false)
                },
            ) { _, delta -> total += delta }
        },
        paper = entry.style.margin,
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = bg.ink)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {
                    scope.launch {
                        PdfExporter.exportAndShare(context, listOf(entry), "Diary_${entry.date}", null, imageFile)
                    }
                }) {
                    Icon(Icons.Rounded.Share, "Export as PDF", tint = bg.ink)
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Rounded.DeleteOutline, "Delete", tint = bg.ink)
                }
                if (isPast) {
                    Row(
                        Modifier.clip(CircleShape).background(bg.ink.copy(alpha = 0.08f)).padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Lock, null, tint = bg.inkSoft, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Past page", color = bg.inkSoft, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    }
                } else Surface(onClick = onEdit, shape = CircleShape, color = bg.ink) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Edit, null, tint = bg.onInk, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit", color = bg.onInk, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.width(8.dp))
            }

            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = startPad, end = 24.dp)) {
                Spacer(Modifier.height(12.dp))
                Column(Modifier.staggeredEntrance(0)) {
                    Text(weekday.uppercase(), color = bg.inkSoft, fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(dateText, color = bg.ink, fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        entry.mood?.let {
                            Text("${it.emoji}  ${it.label}", color = bg.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("  ·  ", color = bg.inkSoft)
                        }
                        Text("Written at $timeText", color = bg.inkSoft, fontSize = 13.sp)
                    }
                }

                if (entry.title.isNotBlank()) {
                    Spacer(Modifier.height(28.dp))
                    Text(
                        entry.title,
                        modifier = Modifier.staggeredEntrance(1),
                        color = bg.ink,
                        fontFamily = font.family,
                        fontWeight = FontWeight.Bold,
                        fontSize = (26 * scale).sp,
                        lineHeight = (34 * scale).sp,
                    )
                }
                if (entry.body.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    SelectionContainer(Modifier.staggeredEntrance(2)) {
                        Text(
                            body,
                            modifier = Modifier
                                .fillMaxWidth()
                                .pageLines(entry.style.lines, 30f * scale, PageStyles.lineColor(entry.style.lineColorId, bg)),
                            color = bg.ink,
                            fontFamily = font.family,
                            fontSize = (18 * scale).sp,
                            lineHeight = (30 * scale).sp,
                        )
                    }
                }

                if (entry.tags.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    FlowRow(
                        Modifier.staggeredEntrance(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        entry.tags.forEach { tag ->
                            Text(
                                "#$tag",
                                color = bg.ink,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(bg.ink.copy(alpha = 0.1f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }

                if (habitUi.habits.isNotEmpty()) {
                    LaunchedEffect(entry.dateEpochDay) { habitUi.load(entry.date) }
                    Spacer(Modifier.height(28.dp))
                    Text("HABITS", color = bg.inkSoft, fontSize = 11.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    HabitChips(
                        habits = habitUi.habits,
                        doneIds = habitUi.done(entry.date),
                        ink = bg.ink,
                        accent = bg.ink,
                        onAccent = bg.onInk,
                        sound = habitUi.sound,
                        onToggle = { habitUi.toggle(it, entry.date) },
                    )
                }

                if (entry.images.isNotEmpty()) {
                    Spacer(Modifier.height(32.dp))
                    Text("MEMORIES", color = bg.inkSoft, fontSize = 11.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(18.dp))
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
                        entry.images.forEachIndexed { index, name ->
                            Stamp(
                                imageFile(name),
                                Modifier.fillMaxWidth(0.8f).staggeredEntrance(index + 3),
                                rotation = detailTilts[index % detailTilts.size],
                                onClick = { viewing = name },
                            )
                        }
                    }
                }
                if (mediaUi.anyEnabled && (!isPast || entry.links.isNotEmpty())) {
                    Spacer(Modifier.height(32.dp))
                    Text("BOOKS · MOVIES · GAMES", color = bg.inkSoft, fontSize = 11.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    EntryMediaSection(
                        links = entry.links,
                        onLinks = onLinks,
                        readOnly = isPast,
                        date = entry.date,
                        ui = mediaUi,
                        ink = bg.ink,
                        onInk = bg.onInk,
                    )
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this entry?") },
            text = { Text("The entry and its photos will be removed from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }

    viewing?.let { name ->
        Dialog(onDismissRequest = { viewing = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f)).clickable { viewing = null }) {
                AsyncImage(
                    model = imageFile(name),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                )
            }
        }
    }
}
