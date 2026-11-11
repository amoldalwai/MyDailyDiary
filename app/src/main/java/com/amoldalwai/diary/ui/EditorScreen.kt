package com.amoldalwai.diary.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amoldalwai.diary.data.DiaryEntry
import com.amoldalwai.diary.data.Mood
import com.amoldalwai.diary.data.PageStyle
import com.amoldalwai.diary.data.SpanType
import com.amoldalwai.diary.util.RichText
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

private val polaroidTilts = listOf(-3f, 2.5f, -1.5f, 3f)
private const val MILLIS_PER_DAY = 86_400_000L

private val stickers = listOf(
    "😀", "😍", "🥳", "😎", "🤗", "😴", "😭", "😡", "🤔", "🙏",
    "👍", "❤️", "🔥", "✨", "🌈", "☀️", "🌙", "⭐", "🌸", "🌻",
    "🍀", "🌊", "🏔️", "🍕", "🍰", "☕", "🍷", "🎉", "🎂", "🎁",
    "🎵", "📚", "✈️", "🏖️", "🐶", "🐱", "🦋", "💪", "🧘", "🏠",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    original: DiaryEntry,
    knownTags: List<String>,
    imageFile: (String) -> File,
    onPickImages: (List<Uri>, (List<String>) -> Unit) -> Unit,
    onAddDrawing: (Bitmap, (String) -> Unit) -> Unit,
    habitUi: HabitUi,
    mediaUi: MediaUi,
    onSave: (DiaryEntry) -> Unit,
    onDiscard: () -> Unit,
) {
    val context = LocalContext.current
    var entry by remember { mutableStateOf(original) }
    var body by remember { mutableStateOf(TextFieldValue(original.body)) }
    var pendingBold by remember { mutableStateOf<Boolean?>(null) }
    var pendingItalic by remember { mutableStateOf<Boolean?>(null) }
    var tagInput by remember { mutableStateOf("") }
    var showStyleSheet by remember { mutableStateOf(false) }
    var showStickers by remember { mutableStateOf(false) }
    var showDrawing by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }

    val bg = PageStyles.background(entry.style.backgroundId)
    val font = PageStyles.font(entry.style.fontId)
    val scale = entry.style.fontScale
    val startPad by animateDpAsState(if (entry.style.margin) 50.dp else 22.dp, label = "pageStart")
    val wordCount = remember(entry.body) { entry.body.trim().split(Regex("\\s+")).count { it.isNotEmpty() } }
    val richTransformation = remember(entry.spans) { RichText.transformation(entry.spans) }

    val selStart = min(body.selection.start, body.selection.end)
    val selEnd = max(body.selection.start, body.selection.end)
    val collapsed = selStart == selEnd
    val boldOn = if (collapsed) pendingBold ?: RichText.coversBefore(entry.spans, SpanType.Bold, selStart)
    else RichText.coversAll(entry.spans, SpanType.Bold, selStart, selEnd)
    val italicOn = if (collapsed) pendingItalic ?: RichText.coversBefore(entry.spans, SpanType.Italic, selStart)
    else RichText.coversAll(entry.spans, SpanType.Italic, selStart, selEnd)

    // Text edits arrive as whole new values, so work out what changed to keep bold/italic ranges aligned.
    fun onBodyChange(value: TextFieldValue) {
        val oldText = body.text
        val newText = value.text
        if (oldText != newText) {
            val shortest = min(oldText.length, newText.length)
            var prefix = 0
            while (prefix < shortest && oldText[prefix] == newText[prefix]) prefix++
            var suffix = 0
            while (suffix < shortest - prefix && oldText[oldText.length - 1 - suffix] == newText[newText.length - 1 - suffix]) suffix++
            val boldActive = pendingBold ?: RichText.coversBefore(entry.spans, SpanType.Bold, prefix)
            val italicActive = pendingItalic ?: RichText.coversBefore(entry.spans, SpanType.Italic, prefix)
            entry = entry.copy(
                body = newText,
                spans = RichText.applyEdit(
                    entry.spans,
                    prefix,
                    oldText.length - suffix,
                    newText.length - suffix - prefix,
                    boldActive,
                    italicActive,
                ),
            )
            pendingBold = null
            pendingItalic = null
        } else if (value.selection != body.selection) {
            pendingBold = null
            pendingItalic = null
        }
        body = value
    }

    fun toggleFormat(type: SpanType) {
        if (collapsed) {
            val current = if (type == SpanType.Bold) boldOn else italicOn
            if (type == SpanType.Bold) pendingBold = !current else pendingItalic = !current
        } else {
            val on = !(if (type == SpanType.Bold) boldOn else italicOn)
            entry = entry.copy(spans = RichText.setRange(entry.spans, type, selStart, selEnd, on))
        }
    }

    fun insertAtCursor(text: String) {
        val newText = body.text.replaceRange(selStart, selEnd, text)
        onBodyChange(TextFieldValue(newText, TextRange(selStart + text.length)))
    }

    fun addTag(raw: String) {
        val tag = raw.trim().trimStart('#').lowercase().replace(Regex("\\s+"), "-").take(24)
        if (tag.isNotEmpty() && tag !in entry.tags) entry = entry.copy(tags = entry.tags + tag)
        tagInput = ""
    }

    fun close() {
        val current = entry
        if (current.isBlank || current == original) onDiscard() else onSave(current.copy(updatedAt = System.currentTimeMillis()))
    }

    BackHandler { close() }
    SystemBarsStyle(darkIcons = !bg.isDark)

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(12)) { uris ->
        if (uris.isNotEmpty()) {
            importing = true
            onPickImages(uris) { names ->
                entry = entry.copy(images = entry.images + names)
                importing = false
            }
        }
    }
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                val needsSpace = body.text.isNotEmpty() && !body.text.endsWith(" ") && !body.text.endsWith("\n")
                insertAtCursor(if (needsSpace) " $spoken" else spoken)
            }
        }
    }
    fun pickPhotos() = photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    fun startVoice() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your entry")
        }
        try {
            speech.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Voice input isn't available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    PageSurface(bg, Modifier.fillMaxSize(), paper = entry.style.margin) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { close() }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = bg.ink)
                }
                Surface(onClick = { showDatePicker = true }, shape = CircleShape, color = bg.ink.copy(alpha = 0.08f)) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CalendarMonth, null, tint = bg.ink, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            entry.date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())),
                            color = bg.ink,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Surface(onClick = { close() }, shape = CircleShape, color = bg.ink) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Check, null, tint = bg.onInk, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Done", color = bg.onInk, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.width(8.dp))
            }

            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = startPad, end = 22.dp),
            ) {
                Spacer(Modifier.height(8.dp))
                SectionLabel("How are you feeling?", bg.inkSoft)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Mood.entries.forEach { mood ->
                        MoodChip(mood, selected = entry.mood == mood, ink = bg.ink) {
                            entry = entry.copy(mood = if (entry.mood == mood) null else mood)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                if (habitUi.habits.isNotEmpty()) {
                    LaunchedEffect(entry.dateEpochDay) { habitUi.load(entry.date) }
                    SectionLabel("Habits", bg.inkSoft)
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
                    Spacer(Modifier.height(18.dp))
                }

                BasicTextField(
                    value = entry.title,
                    onValueChange = { entry = entry.copy(title = it) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        fontFamily = font.family,
                        fontWeight = FontWeight.Bold,
                        fontSize = (28 * scale).sp,
                        lineHeight = (36 * scale).sp,
                        color = bg.ink,
                    ),
                    cursorBrush = SolidColor(bg.ink),
                    decorationBox = { inner ->
                        Box {
                            if (entry.title.isEmpty()) {
                                Text(
                                    "Title your day",
                                    fontFamily = font.family,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (28 * scale).sp,
                                    color = bg.ink.copy(alpha = 0.35f),
                                )
                            }
                            inner()
                        }
                    },
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("Your story", bg.inkSoft)
                    Spacer(Modifier.weight(1f))
                    Text("$wordCount ${if (wordCount == 1) "word" else "words"}", color = bg.inkSoft, fontSize = 12.sp)
                }
                Spacer(Modifier.height(6.dp))
                BasicTextField(
                    value = body,
                    onValueChange = ::onBodyChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp)
                        .pageLines(entry.style.lines, 29f * scale, PageStyles.lineColor(entry.style.lineColorId, bg)),
                    textStyle = TextStyle(
                        fontFamily = font.family,
                        fontSize = (18 * scale).sp,
                        lineHeight = (29 * scale).sp,
                        color = bg.ink,
                    ),
                    cursorBrush = SolidColor(bg.ink),
                    visualTransformation = richTransformation,
                    decorationBox = { inner ->
                        Box {
                            if (body.text.isEmpty()) {
                                Text(
                                    "What happened today?",
                                    fontFamily = font.family,
                                    fontSize = (18 * scale).sp,
                                    color = bg.ink.copy(alpha = 0.35f),
                                )
                            }
                            inner()
                        }
                    },
                )

                Spacer(Modifier.height(18.dp))
                SectionLabel("Tags", bg.inkSoft)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    entry.tags.forEach { tag ->
                        Row(
                            Modifier
                                .clip(CircleShape)
                                .background(bg.ink.copy(alpha = 0.12f))
                                .clickable { entry = entry.copy(tags = entry.tags - tag) }
                                .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("#$tag", color = bg.ink, fontSize = 13.sp)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Rounded.Close, "Remove tag", tint = bg.inkSoft, modifier = Modifier.size(14.dp))
                        }
                    }
                    BasicTextField(
                        value = tagInput,
                        onValueChange = { v ->
                            if (v.endsWith(",") || v.endsWith(" ")) addTag(v.dropLast(1)) else tagInput = v
                        },
                        singleLine = true,
                        textStyle = TextStyle(color = bg.ink, fontSize = 14.sp),
                        cursorBrush = SolidColor(bg.ink),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { addTag(tagInput) }),
                        modifier = Modifier.width(120.dp),
                        decorationBox = { inner ->
                            Box(Modifier.padding(vertical = 6.dp)) {
                                if (tagInput.isEmpty()) Text("+ add tag", color = bg.ink.copy(alpha = 0.4f), fontSize = 14.sp)
                                inner()
                            }
                        },
                    )
                }
                val suggestions = remember(knownTags, entry.tags) { knownTags.filter { it !in entry.tags }.take(6) }
                if (suggestions.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.forEach { tag ->
                            Text(
                                "#$tag",
                                color = bg.inkSoft,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .border(1.dp, bg.ink.copy(alpha = 0.2f), CircleShape)
                                    .clickable { addTag(tag) }
                                    .padding(horizontal = 12.dp, vertical = 5.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                SectionLabel("Memories", bg.inkSoft)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 14.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    entry.images.forEachIndexed { index, name ->
                        key(name) {
                            Box(Modifier.popIn()) {
                                Stamp(
                                    imageFile(name),
                                    Modifier.width(128.dp),
                                    rotation = polaroidTilts[index % polaroidTilts.size],
                                )
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 6.dp, y = (-6).dp)
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(bg.ink)
                                        .clickable { entry = entry.copy(images = entry.images - name) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Rounded.Close, "Remove photo", tint = bg.onInk, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                    AddPhotoTile(importing = importing, ink = bg.ink) { pickPhotos() }
                }
                if (mediaUi.anyEnabled) {
                    Spacer(Modifier.height(10.dp))
                    SectionLabel("Books · Movies · Games", bg.inkSoft)
                    Spacer(Modifier.height(10.dp))
                    EntryMediaSection(
                        links = entry.links,
                        onLinks = { entry = entry.copy(links = it) },
                        date = entry.date,
                        ui = mediaUi,
                        ink = bg.ink,
                        onInk = bg.onInk,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            Surface(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth(),
                shape = CircleShape,
                color = bg.ink.copy(alpha = 0.1f),
            ) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ToolIcon(Icons.Rounded.PhotoLibrary, "Add photos", bg.ink) { pickPhotos() }
                    ToolIcon(Icons.Rounded.Brush, "Draw", bg.ink) { showDrawing = true }
                    ToolIcon(Icons.Rounded.Mic, "Voice to text", bg.ink) { startVoice() }
                    ToolIcon(Icons.Rounded.EmojiEmotions, "Stickers", bg.ink) { showStickers = true }
                    ToolIcon(Icons.Rounded.FormatBold, "Bold", bg.ink, active = boldOn) { toggleFormat(SpanType.Bold) }
                    ToolIcon(Icons.Rounded.FormatItalic, "Italic", bg.ink, active = italicOn) { toggleFormat(SpanType.Italic) }
                    ToolIcon(Icons.Rounded.Palette, "Page style", bg.ink) { showStyleSheet = true }
                }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = entry.dateEpochDay * MILLIS_PER_DAY,
            selectableDates = object : SelectableDates {
                // No back-dating: today and later, or the day this page already has.
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val day = Math.floorDiv(utcTimeMillis, MILLIS_PER_DAY)
                    return day >= java.time.LocalDate.now().toEpochDay() || day == original.dateEpochDay
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { entry = entry.copy(dateEpochDay = Math.floorDiv(it, MILLIS_PER_DAY)) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }

    if (showStyleSheet) {
        ModalBottomSheet(
            onDismissRequest = { showStyleSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            StyleSheetContent(entry.style) { entry = entry.copy(style = it) }
        }
    }

    if (showStickers) {
        ModalBottomSheet(
            onDismissRequest = { showStickers = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                Text("Stickers", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Tap to add at your cursor", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    stickers.forEach { sticker ->
                        Box(
                            Modifier
                                .size(48.dp)
                                .bouncyClick { insertAtCursor(sticker) },
                            contentAlignment = Alignment.Center,
                        ) { Text(sticker, fontSize = 28.sp) }
                    }
                }
            }
        }
    }

    if (showDrawing) {
        DrawingDialog(
            onDismiss = { showDrawing = false },
            onSave = { bitmap ->
                showDrawing = false
                importing = true
                onAddDrawing(bitmap) { name ->
                    entry = entry.copy(images = entry.images + name)
                    importing = false
                }
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String, color: Color) {
    Text(text.uppercase(), color = color, fontSize = 11.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun MoodChip(mood: Mood, selected: Boolean, ink: Color, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "moodScale",
    )
    val fill by animateFloatAsState(if (selected) 0.18f else 0.06f, label = "moodFill")
    Column(
        Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(18.dp))
            .background(ink.copy(alpha = fill))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(mood.emoji, fontSize = 26.sp)
        AnimatedVisibility(selected) { Text(mood.label, color = ink, fontSize = 11.sp) }
    }
}

@Composable
private fun ToolIcon(icon: ImageVector, description: String, ink: Color, active: Boolean = false, onClick: () -> Unit) {
    val fill by animateColorAsState(if (active) ink.copy(alpha = 0.22f) else Color.Transparent, label = "toolFill")
    Box(Modifier.padding(horizontal = 2.dp).clip(CircleShape).background(fill)) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = description, tint = ink, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun AddPhotoTile(importing: Boolean, ink: Color, onClick: () -> Unit) {
    val line = ink.copy(alpha = 0.35f)
    Box(
        Modifier
            .size(width = 110.dp, height = 140.dp)
            .clip(RoundedCornerShape(20.dp))
            .drawBehind {
                drawRoundRect(
                    color = line,
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f))),
                )
            }
            .clickable(enabled = !importing, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (importing) {
            CircularProgressIndicator(Modifier.size(28.dp), color = ink, strokeWidth = 2.5.dp)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Add, null, tint = ink.copy(alpha = 0.7f), modifier = Modifier.size(30.dp))
                Spacer(Modifier.height(4.dp))
                Text("Add photo", color = ink.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun LinePreview(kind: String, selected: Boolean, name: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val ring by animateColorAsState(if (selected) colors.primary else Color.Transparent, label = "lineRing")
    val ink = colors.onSurface.copy(alpha = 0.35f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            Modifier
                .size(width = 72.dp, height = 52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surfaceVariant)
                .border(BorderStroke(2.dp, ring), RoundedCornerShape(14.dp))
                .clickable(onClick = onClick),
        ) {
            val gap = 11.dp.toPx()
            var y = gap
            while (y < size.height) {
                when (kind) {
                    "ruled" -> drawLine(ink, Offset(8.dp.toPx(), y), Offset(size.width - 8.dp.toPx(), y), 1.dp.toPx())
                    "dotted" -> {
                        var x = 10.dp.toPx()
                        while (x < size.width - 6.dp.toPx()) {
                            drawCircle(ink, 1.2.dp.toPx(), Offset(x, y))
                            x += gap
                        }
                    }
                    "grid" -> drawLine(ink, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                }
                y += gap
            }
            if (kind == "grid") {
                var x = gap
                while (x < size.width) {
                    drawLine(ink, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                    x += gap
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(name, fontSize = 11.sp, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun StyleSheetContent(style: PageStyle, onChange: (PageStyle) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 22.dp)
            .padding(bottom = 24.dp),
    ) {
        Text("Customize page", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))

        SectionLabel("Background", colors.onSurfaceVariant)
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PageStyles.backgrounds.forEach { option ->
                val selected = option.id == style.backgroundId
                val scale by animateFloatAsState(
                    targetValue = if (selected) 1.12f else 1f,
                    animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                    label = "swatch",
                )
                val ring by animateColorAsState(if (selected) colors.primary else colors.outline, label = "ring")
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(option.swatch)
                            .border(BorderStroke(if (selected) 3.dp else 1.dp, ring), CircleShape)
                            .clickable { onChange(style.copy(backgroundId = option.id)) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) Icon(Icons.Rounded.Check, null, tint = option.ink, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(option.name, fontSize = 11.sp, color = colors.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        SectionLabel("Page lines", colors.onSurfaceVariant)
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PageStyles.lineKinds.forEach { kind ->
                LinePreview(kind.id, selected = style.lines == kind.id, name = kind.name) { onChange(style.copy(lines = kind.id)) }
            }
        }

        if (style.lines != "blank") {
            SectionLabel("Line colour", colors.onSurfaceVariant)
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PageStyles.lineColors.forEach { option ->
                    val selected = style.lineColorId == option.id
                    val ring by animateColorAsState(if (selected) colors.primary else colors.outline, label = "lineColorRing")
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(option.color ?: colors.surfaceVariant)
                                .border(BorderStroke(if (selected) 3.dp else 1.dp, ring), CircleShape)
                                .clickable { onChange(style.copy(lineColorId = option.id)) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (option.color == null) Text("A", fontSize = 14.sp, color = colors.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(option.name, fontSize = 11.sp, color = colors.onSurfaceVariant)
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Margin & binder holes", color = colors.onSurface, fontWeight = FontWeight.Medium)
                Text("Notebook-style page edge", fontSize = 12.sp, color = colors.onSurfaceVariant)
            }
            Switch(checked = style.margin, onCheckedChange = { onChange(style.copy(margin = it)) })
        }

        Spacer(Modifier.height(12.dp))
        SectionLabel("Font", colors.onSurfaceVariant)
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PageStyles.fonts.forEach { option ->
                val selected = option.id == style.fontId
                val fill by animateColorAsState(if (selected) colors.primaryContainer else colors.surfaceVariant, label = "fontFill")
                val ring by animateColorAsState(if (selected) colors.primary else Color.Transparent, label = "fontRing")
                Column(
                    Modifier
                        .width(88.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(fill)
                        .border(BorderStroke(2.dp, ring), RoundedCornerShape(18.dp))
                        .clickable { onChange(style.copy(fontId = option.id)) }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Aa", fontFamily = option.family, fontSize = 28.sp, color = colors.onSurface)
                    Spacer(Modifier.height(2.dp))
                    Text(option.name, fontSize = 11.sp, color = colors.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        SectionLabel("Text size", colors.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("A", fontSize = 14.sp, color = colors.onSurfaceVariant)
            Slider(
                value = style.fontScale,
                onValueChange = { onChange(style.copy(fontScale = it)) },
                valueRange = 0.85f..1.5f,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            )
            Text("A", fontSize = 26.sp, color = colors.onSurfaceVariant)
        }
    }
}
