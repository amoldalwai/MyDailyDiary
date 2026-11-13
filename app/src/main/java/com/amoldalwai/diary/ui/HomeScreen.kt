package com.amoldalwai.diary.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import java.io.File
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amoldalwai.diary.data.DiaryEntry
import com.amoldalwai.diary.data.Greetings
import com.amoldalwai.diary.data.LevelInfo
import com.amoldalwai.diary.data.Mood
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The calm landing page: a greeting, an optional mood tap, today's habits, one memory, and the latest pages. */
@Composable
fun HomeScreen(
    recent: List<DiaryEntry>,
    loading: Boolean,
    onThisDay: List<DiaryEntry>,
    onOpenEntry: (DiaryEntry) -> Unit,
    onSeeAll: () -> Unit,
    onMood: (Mood) -> Unit,
    habitUi: HabitUi,
    profileEmoji: String,
    profileName: String,
    profilePhoto: File?,
    heroImage: File?,
    heroGradient: Int,
    onRefreshHero: () -> Unit,
    imageFile: (String) -> File,
    level: LevelInfo,
    onProfile: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    SystemBarsStyle(darkIcons = !appIsDark())
    val today = remember { LocalDate.now() }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        FloatingBlobs()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 24.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 130.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "greeting") { Greeting(profileEmoji, profileName, profilePhoto, level, onProfile, heroImage, heroGradient, onRefreshHero, Modifier.tourTarget("home_hero").staggeredEntrance(0)) }
            item(key = "mood") { MoodCard(onMood, Modifier.tourTarget("mood_card").staggeredEntrance(1)) }
            if (habitUi.habits.isNotEmpty()) {
                item(key = "habits") { TodayHabitsCard(habitUi, today, Modifier.staggeredEntrance(2)) }
            }
            if (onThisDay.isNotEmpty()) {
                item(key = "onthisday") { OnThisDayCard(onThisDay, onOpenEntry, Modifier.staggeredEntrance(3)) }
            }
            item(key = "recent") {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Recent", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onBackground, modifier = Modifier.weight(1f))
                    if (recent.isNotEmpty()) {
                        Text(
                            "See all",
                            modifier = Modifier.bouncyClick(onSeeAll).padding(horizontal = 4.dp, vertical = 6.dp),
                            color = colors.primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
            when {
                loading -> items(2) { ShimmerCard() }
                recent.isEmpty() -> item(key = "empty") { EmptyState() }
                else -> recent.forEachIndexed { index, entry ->
                    item(key = entry.id) { RecentRow(entry, imageFile, { onOpenEntry(entry) }, Modifier.staggeredEntrance(index + 4)) }
                }
            }
        }
    }
}

@Composable
private fun Greeting(
    profileEmoji: String,
    profileName: String,
    profilePhoto: File?,
    level: LevelInfo,
    onProfile: () -> Unit,
    heroImage: File?,
    heroGradient: Int,
    onRefreshHero: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Re-read the clock every minute so the greeting follows the day while the app stays open.
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            delay(60_000)
            value = LocalDateTime.now()
        }
    }
    val greeting = remember(now.hour, now.toLocalDate(), profileName) { Greetings.greeting(now.hour, now.toLocalDate(), profileName) }
    val date = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())) }
    val shape = RoundedCornerShape(30.dp)
    Box(modifier.fillMaxWidth().height(224.dp).clip(shape)) {
        if (heroImage != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(heroImage).crossfade(600).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            HeroGradient(heroGradient, Modifier.fillMaxSize())
        }
        // A deep shade rises from the bottom, like a streaming banner, so the white words read on any photo; a light one at the top is for the avatar.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color(0x59000000),
                    0.28f to Color.Transparent,
                    0.48f to Color(0x26000000),
                    0.76f to Color(0xB8000000),
                    1f to Color(0xF2000000),
                ),
            ),
        )
        val lift = Shadow(Color(0x80000000), Offset(0f, 2f), 8f)
        Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 22.dp, vertical = 20.dp)) {
            Text(date, fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f), style = TextStyle(shadow = lift))
            Spacer(Modifier.height(4.dp))
            Text(greeting, style = MaterialTheme.typography.headlineLarge.copy(shadow = lift), fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier.align(Alignment.TopStart).padding(14.dp).size(34.dp).bouncyClick(onRefreshHero).clip(CircleShape).background(Color(0x33FFFFFF)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Refresh, "Change the banner", tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.align(Alignment.TopEnd).padding(14.dp).tourTarget("home_profile").bouncyClick(onProfile), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.clip(CircleShape).background(Color(0x33FFFFFF)).padding(3.dp)) { AvatarRing(profileEmoji, profilePhoto, level, 52.dp) }
            Text("Lv ${level.level}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

/** Four dark, colourful backdrops for the banner when there is no photo; white text reads on all of them. */
private val heroPalettes = listOf(
    listOf(0xFF3A2A78, 0xFF8A3F72, 0xFF2F4F9A),
    listOf(0xFF0F5360, 0xFF1F7F7A, 0xFF2D4F8F),
    listOf(0xFF8A2F2F, 0xFFB5503A, 0xFF6E2D63),
    listOf(0xFF1F5A3F, 0xFF3F7A4C, 0xFF24606F),
).map { palette -> palette.map { Color(it) } }

@Composable
private fun HeroGradient(choice: Int, modifier: Modifier = Modifier) {
    Crossfade(choice % heroPalettes.size, animationSpec = tween(600), modifier = modifier, label = "heroPalette") { index ->
        val palette = heroPalettes[index]
        val tones = remember(index) { (palette + palette.first()).map(::deepenForWhiteText) }
        val drift = rememberInfiniteTransition(label = "heroGradient")
        val phase by drift.animateFloat(0f, 1f, infiniteRepeatable(tween(10_000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "heroPhase")
        Box(
            Modifier.fillMaxSize().drawBehind {
                val w = size.width
                val h = size.height
                drawRect(
                    Brush.linearGradient(tones, start = Offset(w * phase, 0f), end = Offset(w * phase + w * 1.1f, h), tileMode = TileMode.Mirror),
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(w * (0.15f + 0.7f * phase), h * 0.3f),
                        radius = w * 0.55f,
                    ),
                )
            },
        )
    }
}

/** Darkens a colour until white text on it reads clearly (contrast of at least 4.5:1). */
private fun deepenForWhiteText(color: Color): Color {
    var c = color
    repeat(10) { if (c.luminance() > 0.17f) c = lerp(c, Color.Black, 0.15f) }
    return c
}

/** Tapping a mood opens Quick Add with it already chosen; ignoring the card is fine. */
@Composable
private fun MoodCard(onMood: (Mood) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(colors.surface).border(BorderStroke(1.dp, colors.outline.copy(alpha = 0.5f)), shape).padding(vertical = 16.dp),
    ) {
        Text("How are you feeling?", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = colors.onSurface, modifier = Modifier.padding(horizontal = 18.dp))
        Text("Optional. Or tap + to write about your day.", fontSize = 12.sp, color = colors.onSurfaceVariant, modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp))
        Spacer(Modifier.height(12.dp))
        // The tapped face pops with a ring first, then Quick add slides up with it chosen.
        var picked by remember { mutableStateOf<Mood?>(null) }
        val scope = rememberCoroutineScope()
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Mood.entries.forEach { mood ->
                MoodPick(
                    mood = mood,
                    selected = picked == mood,
                    othersSelected = picked != null,
                    faceSize = 52.dp,
                    accent = colors.primary,
                    labelColor = colors.onSurface,
                ) {
                    if (picked == null) {
                        picked = mood
                        scope.launch {
                            delay(450)
                            onMood(mood)
                            picked = null
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentRow(entry: DiaryEntry, imageFile: (String) -> File, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(22.dp)
    val title = entry.title.ifBlank { entry.body.trim().lineSequence().firstOrNull().orEmpty() }.ifBlank { "Untitled day" }
    val meta = buildList {
        add(entry.date.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())))
        entry.mood?.let { add(it.label) }
        if (entry.images.isNotEmpty()) add("${entry.images.size} ${if (entry.images.size == 1) "photo" else "photos"}")
        if (entry.tags.isNotEmpty()) add(entry.tags.take(2).joinToString(", "))
    }.joinToString(" · ")
    Row(
        modifier.fillMaxWidth().bouncyClick(onClick).clip(shape).background(colors.surface).border(BorderStroke(1.dp, colors.outline.copy(alpha = 0.5f)), shape).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (entry.images.isNotEmpty()) {
            PhotoStack(entry.images, imageFile)
        } else {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
                val mood = entry.mood
                if (mood != null) MoodFace(mood, 38.dp) else Text("📝", fontSize = 23.sp)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.onSurface)
            Text(meta, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.onSurfaceVariant)
        }
    }
}

/** Up to three of the page's photos, small and overlapping like a handful of prints. */
@Composable
private fun PhotoStack(names: List<String>, imageFile: (String) -> File) {
    val shown = names.take(3)
    val tilts = listOf(-4f, 6f, -9f)
    val shape = RoundedCornerShape(9.dp)
    Box(Modifier.width(44.dp + 10.dp * (shown.size - 1)).height(52.dp), contentAlignment = Alignment.CenterStart) {
        for (k in shown.indices.reversed()) {
            AsyncImage(
                model = imageFile(shown[k]),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset(x = 10.dp * k)
                    .size(44.dp)
                    .graphicsLayer { rotationZ = tilts[k] }
                    .shadow(3.dp, shape)
                    .clip(shape)
                    .border(2.dp, Color.White, shape),
            )
        }
    }
}

@Composable
private fun EmptyState() {
    val transition = rememberInfiniteTransition(label = "float")
    val dy by transition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dy",
    )
    Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("📖", fontSize = 56.sp, modifier = Modifier.graphicsLayer { translationY = dy.dp.toPx() })
        Spacer(Modifier.height(14.dp))
        Text("Your story starts here", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(6.dp))
        Text(
            "Tap + to capture your first memory.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TodayHabitsCard(habitUi: HabitUi, today: LocalDate, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    LaunchedEffect(today) { habitUi.load(today) }
    val done = habitUi.done(today)
    val doneCount = habitUi.habits.count { it.id in done }
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(colors.surface).border(BorderStroke(1.dp, colors.outline.copy(alpha = 0.5f)), shape).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Today's habits", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = colors.onSurface, modifier = Modifier.weight(1f))
            Text(
                if (doneCount == habitUi.habits.size) "All done 🎉" else "$doneCount/${habitUi.habits.size}",
                fontSize = 13.sp,
                color = colors.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        HabitChips(
            habits = habitUi.habits,
            doneIds = done,
            ink = colors.onSurface,
            accent = colors.primary,
            onAccent = colors.onPrimary,
            sound = habitUi.sound,
            onToggle = { habitUi.toggle(it, today) },
        )
    }
}

@Composable
private fun OnThisDayCard(entries: List<DiaryEntry>, onOpen: (DiaryEntry) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(colors.tertiary.copy(alpha = 0.2f), colors.primary.copy(alpha = 0.14f))))
            .border(1.dp, colors.tertiary.copy(alpha = 0.3f), shape)
            .padding(18.dp),
    ) {
        Text("ON THIS DAY", fontSize = 11.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurfaceVariant)
        entries.take(2).forEach { entry ->
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().bouncyClick { onOpen(entry) }, verticalAlignment = Alignment.CenterVertically) {
                Text("${entry.date.year}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = colors.onSurface)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        entry.title.ifBlank { entry.body.trim().ifBlank { "Untitled day" } },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurface,
                    )
                    val years = LocalDate.now().year - entry.date.year
                    Text("$years ${if (years == 1) "year" else "years"} ago", fontSize = 12.sp, color = colors.onSurfaceVariant)
                }
                entry.mood?.let { MoodFace(it, 32.dp) }
            }
        }
    }
}
