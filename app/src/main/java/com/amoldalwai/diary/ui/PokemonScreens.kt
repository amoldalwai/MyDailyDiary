package com.amoldalwai.diary.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.amoldalwai.diary.data.CaughtPokemon
import com.amoldalwai.diary.data.LevelInfo
import com.amoldalwai.diary.data.PokeApi
import com.amoldalwai.diary.data.PokemonMath
import com.amoldalwai.diary.data.Progress
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private val typeColors = mapOf(
    "normal" to Color(0xFFA8A77A), "fire" to Color(0xFFEE8130), "water" to Color(0xFF6390F0), "electric" to Color(0xFFF7D02C),
    "grass" to Color(0xFF7AC74C), "ice" to Color(0xFF96D9D6), "fighting" to Color(0xFFC22E28), "poison" to Color(0xFFA33EA1),
    "ground" to Color(0xFFE2BF65), "flying" to Color(0xFFA98FF3), "psychic" to Color(0xFFF95587), "bug" to Color(0xFFA6B91A),
    "rock" to Color(0xFFB6A136), "ghost" to Color(0xFF735797), "dragon" to Color(0xFF6F35FC), "dark" to Color(0xFF705746),
    "steel" to Color(0xFFB7B7CE), "fairy" to Color(0xFFD685AD),
)

private fun typeColor(type: String) = typeColors[type] ?: Color(0xFF9AA39C)
private fun shade(c: Color, k: Float) = Color((c.red * k).coerceIn(0f, 1f), (c.green * k).coerceIn(0f, 1f), (c.blue * k).coerceIn(0f, 1f))
private fun seg(t: Float, from: Float, to: Float) = ((t - from) / (to - from)).coerceIn(0f, 1f)

private val BallDark = Color(0xFF23232B)

/** A small Poké Ball, drawn so it needs no picture. */
@Composable
fun PokeballIcon(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2
        val stroke = r * 0.14f
        drawCircle(Color.White, r)
        drawArc(Color(0xFFE53935), 180f, 180f, true)
        drawRect(BallDark, Offset(0f, center.y - stroke * 0.6f), Size(this.size.width, stroke * 1.2f))
        drawCircle(BallDark, r - stroke / 2, style = Stroke(stroke))
        drawCircle(BallDark, r * 0.3f)
        drawCircle(Color.White, r * 0.18f)
    }
}

// ---------------------------------------------------------------- level tree

/** The level journey with Pokémon: every level holds three, shown five levels at a time. */
@Composable
fun PokemonTreeScreen(
    level: LevelInfo,
    caught: List<CaughtPokemon>,
    names: Map<Int, String>,
    minPage: Int,
    maxPage: Int,
    loading: Boolean,
    offline: Boolean,
    syncing: Boolean,
    imageFile: (Int) -> File,
    onMore: (Int) -> Unit,
    onRetry: () -> Unit,
    onPokeball: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    SystemBarsStyle(darkIcons = !appIsDark())
    val byId = remember(caught) { caught.associateBy { it.id } }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(colors.background, colors.surfaceVariant)))) {
        FloatingBlobs()
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = colors.onBackground) }
                Column(Modifier.weight(1f)) {
                    Text("Pokémon journey", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = colors.onBackground)
                    Text("${caught.size} of ${PokemonMath.total} caught", fontSize = 13.sp, color = colors.onSurfaceVariant)
                }
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(colors.surfaceVariant).bouncyClick(onPokeball),
                    contentAlignment = Alignment.Center,
                ) { PokeballIcon(26.dp) }
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item("hero") {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.surface.copy(alpha = 0.9f))
                            .border(1.dp, colors.outline.copy(alpha = 0.4f), RoundedCornerShape(24.dp)).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        PokeballIcon(40.dp)
                        Column(Modifier.weight(1f)) {
                            Text("${caught.size} caught", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = colors.onSurface)
                            Text(
                                if (syncing) "Catching your Pokémon…" else "${(PokemonMath.total - caught.size).coerceAtLeast(0)} still to find · Level ${level.level} of ${Progress.MAX_LEVEL}",
                                fontSize = 12.sp,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (offline) {
                    item("offline") {
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFFFFF1E6)).border(1.dp, Color(0xFFF4C9A3), RoundedCornerShape(20.dp)).padding(14.dp),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Rounded.WifiOff, null, tint = Color(0xFFC2661A))
                                Column {
                                    Text("You're offline", fontWeight = FontWeight.Bold, color = Color(0xFF5A3410))
                                    Text(
                                        "The Pokémon journey needs internet to load new Pokémon and silhouettes. What you've already caught is saved on your phone.",
                                        fontSize = 12.5.sp,
                                        lineHeight = 17.sp,
                                        color = Color(0xFF7A4B1D),
                                    )
                                }
                            }
                            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = onRetry) { Text("Try again") }
                                OutlinedButton(onClick = onPokeball) { Text("Open my Pokéball") }
                            }
                        }
                    }
                }
                if (minPage > 0) item("earlier") { PagerButton("↑ Earlier levels (${(minPage - 1) * PokemonMath.PAGE_LEVELS + 1}–${minPage * PokemonMath.PAGE_LEVELS})", loading) { onMore(-1) } }
                for (page in minPage..maxPage) {
                    val levels = PokemonMath.levelsOnPage(page).toList()
                    items(levels.size, key = { "lv${levels[it]}" }) { i ->
                        LevelCard(levels[i], level, byId, names, imageFile)
                    }
                }
                if (maxPage < PokemonMath.pages - 1) {
                    item("later") { PagerButton("↓ Load next ${PokemonMath.PAGE_LEVELS} levels (${(maxPage + 1) * PokemonMath.PAGE_LEVELS + 1}–${(maxPage + 2) * PokemonMath.PAGE_LEVELS})", loading) { onMore(1) } }
                } else {
                    item("end") {
                        Text("That's the end of the journey.", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 12.sp, color = colors.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun PagerButton(label: String, loading: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier.fillMaxWidth().clip(shape).border(BorderStroke(1.6.dp, colors.outline), shape).bouncyClick(onClick).padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(if (loading) "Loading…" else label, color = colors.primary, fontWeight = FontWeight.Bold, fontSize = 13.5.sp) }
}

@Composable
private fun LevelCard(l: Int, level: LevelInfo, byId: Map<Int, CaughtPokemon>, names: Map<Int, String>, imageFile: (Int) -> File) {
    val colors = MaterialTheme.colorScheme
    val rank = Progress.rankOf(l)
    val unlocked = l <= level.level
    val here = l == level.level
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(colors.surface.copy(alpha = if (unlocked || l == level.level + 1) 0.92f else 0.65f))
            .border(if (here) BorderStroke(2.dp, colors.primary) else BorderStroke(1.dp, colors.outline.copy(alpha = 0.4f)), shape)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(rankBrush(rank)), contentAlignment = Alignment.Center) {
                Text("$l", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            }
            Text("${rank.emoji} ${rank.name}", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.onSurface)
            val (label, fill, ink) = when {
                l < level.level -> Triple("Caught", colors.primaryContainer, colors.primary)
                here -> Triple("You are here", colors.primary, colors.onPrimary)
                l == level.level + 1 -> Triple("Next", Color(0xFFFDF0D6), Color(0xFF9A6A0B))
                else -> Triple("Locked", colors.surfaceVariant, colors.onSurfaceVariant)
            }
            Text(label, Modifier.clip(CircleShape).background(fill).padding(horizontal = 9.dp, vertical = 3.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ink)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PokemonMath.idsFor(l).forEach { id ->
                PokemonTile(id, if (unlocked) byId[id] else null, names[id], imageFile, fresh = here && byId[id] != null, modifier = Modifier.weight(1f))
            }
        }
        if (l == level.level + 1) {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Progress to Level $l", fontSize = 11.5.sp, color = colors.onSurfaceVariant)
                level.nextAt?.let { Text("${level.points - level.levelStart} / ${it - level.levelStart} XP", fontSize = 11.5.sp, color = colors.onSurfaceVariant) }
            }
            Spacer(Modifier.height(5.dp))
            ProgressBar(level.progress, colors.outline.copy(alpha = 0.3f), colors.primary, Modifier.fillMaxWidth())
        }
    }
}

/** One Pokémon slot: the saved picture when caught, otherwise a black silhouette of its small sprite. */
@Composable
private fun PokemonTile(id: Int, caught: CaughtPokemon?, name: String?, imageFile: (Int) -> File, fresh: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.clip(shape).background(if (fresh) Color(0xFFFFF7E0) else colors.surfaceVariant).then(if (fresh) Modifier.border(2.dp, Color(0xFFF2B84B), shape) else Modifier).padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
            if (caught != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(imageFile(id)).crossfade(true).build(),
                    contentDescription = PokemonMath.pretty(caught.name),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            } else {
                SubcomposeAsyncImage(
                    model = PokeApi.spriteUrl(id),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.None,
                    colorFilter = ColorFilter.tint(Color.Black.copy(alpha = 0.28f), BlendMode.SrcIn),
                    loading = { Text("?", Modifier.align(Alignment.Center), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = colors.outline) },
                    error = { Text("?", Modifier.align(Alignment.Center), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = colors.outline) },
                )
            }
        }
        Text(
            if (caught != null) PokemonMath.pretty(caught.name) else "???",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (caught != null) colors.onSurface else colors.onSurfaceVariant,
        )
        Text(PokemonMath.number(id), fontSize = 10.5.sp, color = colors.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------- the collection

@Composable
fun PokeballScreen(
    caught: List<CaughtPokemon>,
    level: LevelInfo,
    syncing: Boolean,
    imageFile: (Int) -> File,
    onTree: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    SystemBarsStyle(darkIcons = !appIsDark())
    var openId by rememberSaveable { mutableStateOf<Int?>(null) }
    val viewing = openId?.let { id -> caught.indexOfFirst { it.id == id } }?.takeIf { it >= 0 }
    BackHandler(viewing != null) { openId = null }

    if (viewing != null) {
        CardViewer(caught, viewing, imageFile, onSelect = { openId = caught[it].id }, onClose = { openId = null })
        return
    }
    val lockedFrom = max(caught.maxOfOrNull { it.id } ?: 0, level.level * PokemonMath.PER_LEVEL) + 1
    val locked = (lockedFrom..min(PokemonMath.total, lockedFrom + 5)).toList()

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(colors.background, colors.surfaceVariant)))) {
        FloatingBlobs()
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = colors.onBackground) }
                Column(Modifier.weight(1f)) {
                    Text("Pokéball", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = colors.onBackground)
                    Text("${caught.size} of ${PokemonMath.total} caught", fontSize = 13.sp, color = colors.onSurfaceVariant)
                }
                FilledTonalButton(onClick = onTree) { Text("Journey") }
                Spacer(Modifier.width(8.dp))
            }
            LazyVerticalGrid(
                GridCells.Fixed(3),
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(span = { GridItemSpan(3) }) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.surface.copy(alpha = 0.9f)).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Column(Modifier.weight(0.6f)) {
                            Text("${caught.size}", fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = colors.onSurface)
                            Text("caught of ${PokemonMath.total}", fontSize = 12.sp, color = colors.onSurfaceVariant)
                        }
                        Column(Modifier.weight(1f)) {
                            ProgressBar(caught.size / PokemonMath.total.toFloat(), colors.outline.copy(alpha = 0.3f), colors.primary, Modifier.fillMaxWidth())
                            Text(
                                if (syncing) "Catching your Pokémon…" else if (level.level >= Progress.MAX_LEVEL) "Journey complete" else "Next 3 at Level ${level.level + 1}",
                                Modifier.padding(top = 6.dp),
                                fontSize = 11.5.sp,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (caught.isEmpty()) {
                    item(span = { GridItemSpan(3) }) {
                        Text(
                            if (syncing) "Catching your first Pokémon…" else "Your Pokémon will appear here. They need internet to be fetched the first time, then they stay on your phone.",
                            Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 8.dp),
                            textAlign = TextAlign.Center,
                            color = colors.onSurfaceVariant,
                            fontSize = 13.sp,
                        )
                    }
                }
                items(caught, key = { it.id }) { p ->
                    Box(Modifier.fillMaxWidth().bouncyClick { openId = p.id }) { PokemonTile(p.id, p, p.name, imageFile, fresh = false, modifier = Modifier.fillMaxWidth()) }
                }
                items(locked, key = { "lock$it" }) { id -> PokemonTile(id, null, null, imageFile, fresh = false, modifier = Modifier.fillMaxWidth()) }
            }
        }
    }
}

// ---------------------------------------------------------------- the card

@Composable
private fun CardViewer(caught: List<CaughtPokemon>, index: Int, imageFile: (Int) -> File, onSelect: (Int) -> Unit, onClose: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val pokemon = caught[index]
    val context = LocalContext.current
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(Unit) { onDispose { player?.release() } }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(shade(typeColor(pokemon.types.firstOrNull() ?: "normal"), 0.35f), Color(0xFF14121F))))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Close", tint = Color.White) }
                Column(Modifier.weight(1f)) {
                    Text("My card", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                    Text("Drag the card to tilt it", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                }
                if (pokemon.cryUrl != null) {
                    IconButton(onClick = {
                        player?.release()
                        player = runCatching {
                            MediaPlayer().apply {
                                setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).setUsage(AudioAttributes.USAGE_GAME).build())
                                setDataSource(pokemon.cryUrl)
                                setVolume(0.6f, 0.6f)
                                setOnPreparedListener { it.start() }
                                setOnCompletionListener { it.release() }
                                setOnErrorListener { mp, _, _ -> mp.release(); true }
                                prepareAsync()
                            }
                        }.getOrNull()
                    }) { Icon(Icons.Rounded.VolumeUp, "Play its cry", tint = Color.White) }
                }
            }
            Spacer(Modifier.weight(1f))
            androidx.compose.animation.Crossfade(pokemon.id, label = "card") { id ->
                PokemonCard(caught.first { it.id == id }, imageFile)
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                NavCircle(Icons.Rounded.ChevronLeft, "Previous", enabled = index < caught.lastIndex) { onSelect(index + 1) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${index + 1} / ${caught.size}", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("newest first", color = Color.White.copy(alpha = 0.65f), fontSize = 11.5.sp)
                }
                NavCircle(Icons.Rounded.ChevronRight, "Next", enabled = index > 0) { onSelect(index - 1) }
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun NavCircle(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = if (enabled) 0.18f else 0.07f)).then(if (enabled) Modifier.bouncyClick(onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = Color.White.copy(alpha = if (enabled) 1f else 0.35f)) }
}

/** A collectible card that tilts in 3D under your finger and sways gently when left alone. */
@Composable
fun PokemonCard(p: CaughtPokemon, imageFile: (Int) -> File, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val tiltX = remember { Animatable(0f) }
    val tiltY = remember { Animatable(0f) }
    var dragging by remember { mutableStateOf(false) }
    val idle = rememberInfiniteTransition(label = "idle")
    val swayX by idle.animateFloat(-1f, 1f, infiniteRepeatable(tween(3600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "swayX")
    val swayY by idle.animateFloat(-1f, 1f, infiniteRepeatable(tween(4800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "swayY")
    val bob by idle.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    val rx = tiltX.value + if (dragging) 0f else swayX * 4f
    val ry = tiltY.value + if (dragging) 0f else swayY * 11f

    val main = typeColor(p.types.firstOrNull() ?: "normal")
    val c1 = shade(main, 1.12f)
    val c2 = shade(main, 0.76f)
    val rim = if (p.legendary) Color(0xFFFFD24D) else Color(0xFFF2C94C)
    val cream = Color(0xFFFFFDF4)
    val ink = Color(0xFF1C1C24)
    val soft = Color(0xFF5B5A4C)
    // Where the light sits follows the tilt, so the shine slides across as the card turns.
    val shineX = (0.5f + ry / 60f).coerceIn(0f, 1f)
    val shineY = (0.4f - rx / 50f).coerceIn(0f, 1f)

    Box(
        modifier
            .size(310.dp, 468.dp)
            .graphicsLayer {
                rotationX = rx
                rotationY = ry
                cameraDistance = 16f * density.density
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = {
                        dragging = false
                        scope.launch { tiltX.animateTo(0f, spring(0.55f, 80f)) }
                        scope.launch { tiltY.animateTo(0f, spring(0.55f, 80f)) }
                    },
                    onDragCancel = {
                        dragging = false
                        scope.launch { tiltX.animateTo(0f) }
                        scope.launch { tiltY.animateTo(0f) }
                    },
                ) { change, drag ->
                    change.consume()
                    scope.launch {
                        tiltY.snapTo((tiltY.value + drag.x * 0.2f).coerceIn(-32f, 32f))
                        tiltX.snapTo((tiltX.value - drag.y * 0.16f).coerceIn(-24f, 24f))
                    }
                }
            }
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(c1, c2)))
            .border(3.dp, rim, RoundedCornerShape(22.dp))
            .padding(9.dp),
    ) {
        Column(
            Modifier.fillMaxSize().clip(RoundedCornerShape(15.dp)).background(cream).padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    PokemonMath.pretty(p.name),
                    Modifier.weight(1f),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("${p.stats["hp"] ?: 0}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFFB42F2F))
                Text("HP", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFB42F2F))
                Box(Modifier.size(26.dp).clip(CircleShape).background(main).border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape), contentAlignment = Alignment.Center) {
                    Text((p.types.firstOrNull() ?: "").take(2).uppercase(), color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(p.genus, fontSize = 10.5.sp, color = soft, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text("No. ${p.id.toString().padStart(3, '0')}", fontSize = 10.5.sp, color = soft)
            }
            Box(
                Modifier.fillMaxWidth().height(156.dp).clip(RoundedCornerShape(12.dp)).border(3.dp, Color(0xFFD6C785), RoundedCornerShape(12.dp))
                    .background(Brush.radialGradient(listOf(c1, c2))),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(imageFile(p.id)).crossfade(true).build(),
                    contentDescription = PokemonMath.pretty(p.name),
                    modifier = Modifier.size(140.dp).offset(y = (-6f * bob).dp).graphicsLayer { translationX = ry * -0.6f * density.density },
                    contentScale = ContentScale.Fit,
                )
                Box(
                    Modifier.fillMaxSize().drawBehind {
                        val rainbow = listOf(
                            Color(0x00FFFFFF), Color(0x2BFF7773), Color(0x2BFFED5F), Color(0x2BA8FF5F), Color(0x2B83FFF7), Color(0x2B7894FF), Color(0x2BD875FF), Color(0x00FFFFFF),
                        )
                        val shift = size.width * (shineX * 1.6f - 0.3f)
                        drawRect(Brush.linearGradient(rainbow, Offset(shift - size.width, 0f), Offset(shift + size.width * 0.6f, size.height)))
                    },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                p.types.forEach {
                    Text(
                        it.uppercase(),
                        Modifier.clip(CircleShape).background(typeColor(it)).padding(horizontal = 10.dp, vertical = 3.dp),
                        color = Color.White,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
            val shown = listOf("attack" to "Attack", "defense" to "Defense", "special-attack" to "Sp. Atk", "special-defense" to "Sp. Def", "speed" to "Speed")
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                shown.forEach { (key, label) ->
                    val v = p.stats[key] ?: 0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label, Modifier.width(52.dp), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = soft)
                        Box(Modifier.weight(1f).height(7.dp).clip(CircleShape).background(Color(0xFFE8E3CF))) {
                            Box(Modifier.fillMaxSize(min(1f, v / 150f)).clip(CircleShape).background(c2))
                        }
                        Text("$v", Modifier.width(30.dp), textAlign = TextAlign.End, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = ink)
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFF2EED8)).padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Fact("%.1f m".format(p.heightM), "Height", ink, soft)
                Fact("%.1f kg".format(p.weightKg), "Weight", ink, soft)
                Fact(PokemonMath.pretty(p.ability), "Ability", ink, soft)
            }
            Text(p.flavor, fontSize = 10.5.sp, lineHeight = 14.sp, fontStyle = FontStyle.Italic, color = Color(0xFF4B4A3D), maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Caught at Level ${p.caughtLevel}", fontSize = 9.5.sp, color = Color(0xFF8C8A78))
                Text(if (p.legendary) "★ Legendary" else "LifeLoom", fontSize = 9.5.sp, color = Color(0xFF8C8A78))
            }
        }
        Box(
            Modifier.fillMaxSize().drawBehind {
                drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent), Offset(size.width * shineX, size.height * shineY), size.width * 0.7f))
            },
        )
    }
}

@Composable
private fun Fact(value: String, label: String, ink: Color, soft: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, fontSize = 10.sp, color = soft)
    }
}

// ---------------------------------------------------------------- the level-up reveal

/** A Poké Ball drops, shakes and opens; the three Pokémon of the new level burst out and settle side by side. */
@Composable
fun PokemonRevealOverlay(level: Int, pokemon: List<CaughtPokemon>, sound: Boolean, imageFile: (Int) -> File, onDone: (openPokeball: Boolean) -> Unit) {
    BackHandler { onDone(false) }
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (sound) Chime.celebrate()
        t.animateTo(1f, tween(4600, easing = LinearEasing))
    }
    val spin = rememberInfiniteTransition(label = "rays")
    val rays by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(18000, easing = LinearEasing)), label = "raysAngle")
    val density = LocalDensity.current

    Box(
        Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF3A3563), Color(0xFF17152B)), Offset.Unspecified, 1400f)).pointerInput(Unit) { detectDragGestures { _, _ -> } },
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            val ballSize = 130.dp
            val ballCenterY = 120.dp
            val spread = min(124f, (maxWidth.value - 112f) / 2f - 8f).coerceAtLeast(70f)

            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2
                val cy = ballCenterY.toPx()
                rotate(rays, Offset(cx, cy)) {
                    for (i in 0 until 9) drawArc(Color.White.copy(alpha = 0.07f), i * 40f, 20f, true, Offset(cx - 600.dp.toPx(), cy - 600.dp.toPx()), Size(1200.dp.toPx(), 1200.dp.toPx()))
                }
            }

            // The ball: drops in, shakes, then the lid swings open on its hinge.
            val drop = (1f - FastOutSlowInEasing.transform(seg(t.value, 0f, 0.1f))) * -260f
            val shake = sin(seg(t.value, 0.12f, 0.4f) * 4f * PI.toFloat()) * 14f * (1f - seg(t.value, 0.12f, 0.4f))
            val open = FastOutSlowInEasing.transform(seg(t.value, 0.42f, 0.52f))
            Canvas(
                Modifier.align(Alignment.TopCenter).offset(y = ballCenterY - ballSize / 2).size(ballSize)
                    .graphicsLayer { translationY = drop * density.density; rotationZ = shake },
            ) { drawOpeningBall(open) }

            // The flash as it opens.
            val flash = seg(t.value, 0.42f, 0.46f) * (1f - seg(t.value, 0.46f, 0.58f))
            Box(
                Modifier.align(Alignment.TopCenter).offset(y = ballCenterY - 110.dp).size(220.dp).graphicsLayer { alpha = flash }
                    .background(Brush.radialGradient(listOf(Color.White, Color.White.copy(alpha = 0.5f), Color.Transparent))),
            )

            pokemon.forEachIndexed { i, p ->
                val start = 0.44f + i * 0.06f
                val burst = FastOutSlowInEasing.transform(seg(t.value, start, start + 0.09f))
                val settle = FastOutSlowInEasing.transform(seg(t.value, start + 0.09f, start + 0.17f))
                val tx = (i - 1) * spread
                val ty = 190f
                // Out of the ball in an arc: half way across and above it, then down into its own spot.
                val x = if (settle == 0f) tx * 0.5f * burst else tx * (0.5f + 0.5f * settle)
                val y = if (settle == 0f) -70f * burst else -70f + (ty + 70f) * settle
                val scale = if (settle == 0f) 0.1f + 1.1f * burst else 1.2f - 0.2f * settle
                Column(
                    Modifier.align(Alignment.TopCenter).offset(y = ballCenterY - 56.dp).width(112.dp)
                        .offset { IntOffset((x * density.density).roundToInt(), (y * density.density).roundToInt()) }
                        .graphicsLayer { scaleX = scale; scaleY = scale; alpha = seg(t.value, start, start + 0.03f) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(imageFile(p.id)).build(),
                        contentDescription = PokemonMath.pretty(p.name),
                        modifier = Modifier.size(112.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Text(PokemonMath.pretty(p.name), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(PokemonMath.number(p.id), color = Color(0xFFCFCAF5), fontSize = 11.sp)
                }
            }

            Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Column(Modifier.graphicsLayer { alpha = seg(t.value, 0.55f, 0.7f) }.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("NEW POKÉMON", color = Color(0xFFC9C4FF), fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    Text("Level $level reached!", color = Color.White, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 27.sp)
                    Text(
                        "You caught ${pokemon.size} new Pokémon. They are saved in your Pokéball.",
                        Modifier.padding(top = 4.dp),
                        color = Color(0xFFD9D6F5),
                        fontSize = 13.5.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(18.dp))
                Column(Modifier.graphicsLayer { alpha = seg(t.value, 0.8f, 0.92f) }.padding(horizontal = 22.dp)) {
                    Box(
                        Modifier.fillMaxWidth().clip(CircleShape).background(Color(0xFFFFD45A)).bouncyClick { if (t.value > 0.8f) onDone(true) }.padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Open my Pokéball", color = Color(0xFF3D2C00), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp) }
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier.fillMaxWidth().clip(CircleShape).border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape).bouncyClick { if (t.value > 0.8f) onDone(false) }.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Keep writing", color = Color(0xFFD9D6F5), fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOpeningBall(open: Float) {
    val r = size.minDimension / 2 * 0.92f
    val c = center
    val stroke = r * 0.09f
    val ballRect = Size(r * 2, r * 2)
    val topLeft = Offset(c.x - r, c.y - r)
    val circle = Path().apply { addOval(androidx.compose.ui.geometry.Rect(topLeft, ballRect)) }
    val red = Brush.radialGradient(listOf(Color(0xFFFF8F86), Color(0xFFEA3B32), Color(0xFF8F0F12)), Offset(c.x - r * 0.3f, c.y - r * 0.5f), r * 1.5f)
    val white = Brush.radialGradient(listOf(Color.White, Color(0xFFE9EDF0), Color(0xFFA9B3BA)), Offset(c.x - r * 0.25f, c.y + r * 0.45f), r * 1.5f)
    val band = r * 0.14f

    // Bottom half, with the glowing opening and a beam of light once the lid is up.
    clipPath(circle) {
        clipRect(top = c.y) {
            drawRect(white)
            drawRect(BallDark, Offset(0f, c.y), Size(size.width, band))
        }
    }
    drawArc(BallDark, 0f, 180f, false, topLeft, ballRect, style = Stroke(stroke))
    clipRect(top = c.y) { drawButton(c, r) }
    if (open > 0f) {
        val mouthTop = Offset(c.x - r * 0.92f, c.y - r * 0.17f)
        drawOval(Brush.radialGradient(listOf(Color(0xFFFFFDF0), Color(0xFFFFE9A0), Color(0xFFE9B449)), c, r), mouthTop, Size(r * 1.84f, r * 0.34f), alpha = open)
        drawOval(BallDark, mouthTop, Size(r * 1.84f, r * 0.34f), alpha = open, style = Stroke(stroke * 0.6f))
        val beam = Path().apply {
            moveTo(c.x - r * 0.8f, c.y - r * 0.05f)
            lineTo(c.x - r * 1.3f, c.y - r * 2.6f)
            lineTo(c.x + r * 1.3f, c.y - r * 2.6f)
            lineTo(c.x + r * 0.8f, c.y - r * 0.05f)
            close()
        }
        drawPath(beam, Brush.verticalGradient(listOf(Color(0x00FFF7C8), Color(0xF2FFF7C8)), c.y - r * 2.6f, c.y), alpha = open)
    }

    // The lid, hinged at the left edge.
    rotate(-52f * open, Offset(c.x - r * 0.92f, c.y)) {
        clipPath(circle) {
            clipRect(bottom = c.y) {
                drawRect(red)
                drawRect(BallDark, Offset(0f, c.y - band), Size(size.width, band))
            }
        }
        drawArc(BallDark, 180f, 180f, false, topLeft, ballRect, style = Stroke(stroke))
        clipRect(bottom = c.y) { drawButton(c, r) }
        rotate(-32f, Offset(c.x - r * 0.35f, c.y - r * 0.55f)) {
            drawOval(Color.White.copy(alpha = 0.36f), Offset(c.x - r * 0.7f, c.y - r * 0.72f), Size(r * 0.7f, r * 0.34f))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawButton(c: Offset, r: Float) {
    drawCircle(BallDark, r * 0.3f, c)
    drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFC5CED4)), Offset(c.x - r * 0.06f, c.y - r * 0.08f), r * 0.3f), r * 0.21f, c)
    drawCircle(Color(0xFFE4EAEE), r * 0.11f, c)
}
