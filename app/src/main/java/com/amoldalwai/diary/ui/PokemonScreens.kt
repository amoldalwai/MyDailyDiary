package com.amoldalwai.diary.ui

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.amoldalwai.diary.BallReveal
import com.amoldalwai.diary.R
import com.amoldalwai.diary.data.BallDef
import com.amoldalwai.diary.data.CaughtPokemon
import com.amoldalwai.diary.data.PokemonMath
import com.amoldalwai.diary.data.Rarity
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

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
private fun rarityColor(r: Rarity) = Color(r.argb)
private fun ballColor(b: BallDef) = Color(b.argb)
private fun grouped(n: Int) = String.format(Locale.US, "%,d", n)

private fun BallDef.image(): Int = when (id) {
    "great" -> R.drawable.ball_great
    "ultra" -> R.drawable.ball_ultra
    "master" -> R.drawable.ball_master
    else -> R.drawable.ball_poke
}

private val BallDark = Color(0xFF23232B)
private val RayGold = Color(0xFFFFD45A)

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

/** The points icon: a gold coin with a star, so points look the same everywhere they are shown. */
@Composable
fun PointsCoin(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFFFE27A), Color(0xFFF0A81C))))
            .border(size / 16, Color(0xFFB87A0C), CircleShape),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Rounded.Star, null, tint = Color(0xFF8A5A00), modifier = Modifier.size(size * 0.62f)) }
}

/** Points you can spend, with the coin icon. */
@Composable
fun PointsChip(points: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.clip(CircleShape).background(colors.surface).border(1.dp, colors.outline.copy(alpha = 0.5f), CircleShape).padding(start = 6.dp, end = 14.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        PointsCoin(24.dp)
        Text(grouped(points), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = colors.onSurface)
    }
}

// ---------------------------------------------------------------- the shop

/** Four Poké Balls to buy with points; the "?" on each shows what is inside. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokeballShopScreen(
    balls: List<BallDef>,
    wallet: Int,
    busyId: String?,
    caughtCount: Int,
    total: Int,
    refunds: List<Int>,
    onOpen: (String) -> Unit,
    onCollection: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    SystemBarsStyle(darkIcons = !appIsDark())
    var info by rememberSaveable { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(colors.background, colors.surfaceVariant)))) {
        FloatingBlobs()
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = colors.onBackground) }
                Column(Modifier.weight(1f)) {
                    Text("Poké Balls", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = colors.onBackground)
                    Text("$caughtCount of ${grouped(total)} found", fontSize = 13.sp, color = colors.onSurfaceVariant)
                }
                PointsChip(wallet)
                Spacer(Modifier.width(8.dp))
            }
            LazyVerticalGrid(
                GridCells.Fixed(2),
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(balls, key = { it.id }) { ball ->
                    BallCard(ball, wallet, busy = busyId == ball.id, anyBusy = busyId != null, onOpen = { onOpen(ball.id) }, onInfo = { info = ball.id })
                }
                item(span = { GridItemSpan(2) }) {
                    Column(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Earn points by writing, tracking habits and finishing books and films.",
                            textAlign = TextAlign.Center,
                            fontSize = 12.5.sp,
                            color = colors.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            Modifier.clip(CircleShape).background(colors.primaryContainer).bouncyClick(onCollection).padding(horizontal = 20.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            PokeballIcon(22.dp)
                            Text("My Pokéball", fontWeight = FontWeight.Bold, color = colors.onPrimaryContainer)
                        }
                    }
                }
            }
        }
    }

    val shown = balls.firstOrNull { it.id == info }
    if (shown != null) {
        ModalBottomSheet(
            onDismissRequest = { info = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.surface,
        ) { BallInfo(shown, total, refunds) }
    }
}

@Composable
private fun BallCard(ball: BallDef, wallet: Int, busy: Boolean, anyBusy: Boolean, onOpen: () -> Unit, onInfo: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tint = ballColor(ball)
    val shape = RoundedCornerShape(24.dp)
    val canBuy = wallet >= ball.cost && !anyBusy
    Box(
        Modifier.fillMaxWidth().clip(shape).background(Brush.radialGradient(listOf(tint.copy(alpha = 0.22f), colors.surface), Offset(200f, 160f), 520f))
            .border(1.dp, colors.outline.copy(alpha = 0.4f), shape),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp).padding(top = 16.dp, bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(ball.image()), ball.name, Modifier.size(104.dp), contentScale = ContentScale.Fit)
            Spacer(Modifier.height(6.dp))
            Text(ball.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.onSurface)
            Row(
                Modifier.padding(top = 5.dp).clip(CircleShape).background(colors.surfaceVariant).padding(start = 5.dp, end = 11.dp, top = 3.dp, bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                PointsCoin(16.dp)
                Text(grouped(ball.cost), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = colors.onSurface)
            }
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier.fillMaxWidth().clip(CircleShape).background(if (canBuy) tint else colors.outline.copy(alpha = 0.35f)).then(if (canBuy) Modifier.bouncyClick(onOpen) else Modifier).padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    when {
                        busy -> "Opening…"
                        wallet < ball.cost -> "${grouped(ball.cost - wallet)} more"
                        else -> "Open"
                    },
                    color = if (canBuy) Color.White else colors.onSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.5.sp,
                )
            }
        }
        Box(
            Modifier.align(Alignment.TopEnd).padding(9.dp).size(26.dp).clip(CircleShape).background(colors.surfaceVariant).bouncyClick(onInfo),
            contentAlignment = Alignment.Center,
        ) { Text("?", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = colors.onSurfaceVariant) }
    }
}

@Composable
private fun BallInfo(ball: BallDef, pokedexSize: Int, refunds: List<Int>) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp).padding(bottom = 22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Image(painterResource(ball.image()), ball.name, Modifier.size(68.dp))
            Column {
                Text(ball.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onSurface)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PointsCoin(16.dp)
                    Text("${grouped(ball.cost)} points · one random Pokémon", fontSize = 13.sp, color = colors.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Rarity.entries.forEachIndexed { i, rarity ->
            val pct = ball.percent[i]
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(rarity.label, Modifier.width(84.dp), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = rarityColor(rarity))
                Box(Modifier.weight(1f).height(12.dp).clip(CircleShape).background(colors.surfaceVariant)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth((pct / 100.0).toFloat().coerceIn(0.02f, 1f)).clip(CircleShape).background(rarityColor(rarity)))
                }
                Text(formatPercent(pct), Modifier.width(60.dp), textAlign = TextAlign.End, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = colors.onSurface)
            }
        }
        Spacer(Modifier.height(10.dp))
        val notes = buildList {
            if (ball.guaranteeEpic > 0) add("An Epic or better is guaranteed within ${ball.guaranteeEpic} balls.")
            if (ball.guaranteeLegendary > 0) add("A Legendary is guaranteed within ${ball.guaranteeLegendary} balls.")
            add("Any of the ${grouped(pokedexSize)} Pokémon can appear, and the first generation turns up most.")
            add("A duplicate gives some points back (${refunds.joinToString(" / ")}).")
        }
        notes.forEach { Text(it, Modifier.padding(top = 4.dp), fontSize = 12.5.sp, lineHeight = 17.sp, color = colors.onSurfaceVariant) }
    }
}

private fun formatPercent(p: Double) = if (p >= 10) "${p.roundToInt()}%" else String.format(Locale.US, "%.1f%%", p).replace(".0%", "%")

// ---------------------------------------------------------------- the collection

@Composable
fun PokeballScreen(
    caught: List<CaughtPokemon>,
    total: Int,
    rarityOf: (CaughtPokemon) -> Rarity,
    wallet: Int,
    imageFile: (Int) -> File,
    onShop: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    SystemBarsStyle(darkIcons = !appIsDark())
    var openId by rememberSaveable { mutableStateOf<Int?>(null) }
    val viewing = openId?.let { id -> caught.indexOfFirst { it.id == id } }?.takeIf { it >= 0 }
    BackHandler(viewing != null) { openId = null }

    if (viewing != null) {
        CardViewer(caught, viewing, rarityOf, imageFile, onSelect = { openId = caught[it].id }, onClose = { openId = null })
        return
    }
    val counts = remember(caught) { Rarity.entries.map { r -> caught.count { rarityOf(it) == r } } }
    val firstGen = remember(caught) { caught.count { it.id <= 151 } }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(colors.background, colors.surfaceVariant)))) {
        FloatingBlobs()
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = colors.onBackground) }
                Column(Modifier.weight(1f)) {
                    Text("Pokéball", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = colors.onBackground)
                    Text("${caught.size} of ${grouped(total)} found", fontSize = 13.sp, color = colors.onSurfaceVariant)
                }
                Row(
                    Modifier.clip(CircleShape).background(colors.primaryContainer).bouncyClick(onShop).padding(start = 8.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    PointsCoin(22.dp)
                    Text(grouped(wallet), fontWeight = FontWeight.ExtraBold, color = colors.onPrimaryContainer)
                }
                Spacer(Modifier.width(8.dp))
            }
            LazyVerticalGrid(
                GridCells.Fixed(3),
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(span = { GridItemSpan(3) }) {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(colors.surface.copy(alpha = 0.92f)).padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Rarity.entries.forEachIndexed { i, r ->
                                Column(
                                    Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(rarityColor(r).copy(alpha = 0.14f)).padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text("${counts[i]}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = rarityColor(r))
                                    Text(r.label, fontSize = 10.5.sp, color = colors.onSurfaceVariant, maxLines = 1)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Generation I", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.onSurface)
                            Text("$firstGen of 151", fontSize = 12.sp, color = colors.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(6.dp))
                        ProgressBar(firstGen / 151f, colors.outline.copy(alpha = 0.3f), colors.primary, Modifier.fillMaxWidth())
                    }
                }
                if (caught.isEmpty()) {
                    item(span = { GridItemSpan(3) }) {
                        Text(
                            "Nothing here yet. Open a Poké Ball with your points to find your first Pokémon.",
                            Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 8.dp),
                            textAlign = TextAlign.Center,
                            color = colors.onSurfaceVariant,
                            fontSize = 13.sp,
                        )
                    }
                }
                items(caught, key = { it.id }) { p ->
                    Box(Modifier.fillMaxWidth().bouncyClick { openId = p.id }) { PokemonTile(p, rarityOf(p), imageFile, Modifier.fillMaxWidth()) }
                }
            }
        }
    }
}

/** One Pokémon in the collection, with its rarity colour behind it. */
@Composable
private fun PokemonTile(p: CaughtPokemon, rarity: Rarity, imageFile: (Int) -> File, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val tint = rarityColor(rarity)
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.clip(shape).background(Brush.verticalGradient(listOf(colors.surfaceVariant, tint.copy(alpha = if (rarity == Rarity.Common) 0.12f else 0.3f)))).padding(top = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(imageFile(p.id)).crossfade(true).build(),
                contentDescription = PokemonMath.pretty(p.name),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
        Text(PokemonMath.pretty(p.name), Modifier.padding(horizontal = 4.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.onSurface)
        Text(PokemonMath.number(p.id), Modifier.padding(bottom = 5.dp), fontSize = 10.5.sp, color = colors.onSurfaceVariant)
        Box(Modifier.fillMaxWidth().height(4.dp).background(tint))
    }
}

// ---------------------------------------------------------------- the card

@Composable
private fun CardViewer(caught: List<CaughtPokemon>, index: Int, rarityOf: (CaughtPokemon) -> Rarity, imageFile: (Int) -> File, onSelect: (Int) -> Unit, onClose: () -> Unit) {
    val pokemon = caught[index]
    val rarity = rarityOf(pokemon)
    val top = if (rarity == Rarity.Common) shade(typeColor(pokemon.types.firstOrNull() ?: "normal"), 0.35f) else shade(rarityColor(rarity), 0.55f)

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(top, Color(0xFF14121F))))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Close", tint = Color.White) }
                Column(Modifier.weight(1f)) {
                    Text("My card", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                    Text("Drag the card to tilt it", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                }
            }
            Spacer(Modifier.weight(1f))
            Crossfade(pokemon.id, label = "card") { id ->
                val shown = caught.first { it.id == id }
                PokemonCard(shown, rarityOf(shown), imageFile)
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

private val RAINBOW = listOf(
    Color(0x00FFFFFF), Color(0xFFFF7773), Color(0xFFFFED5F), Color(0xFFA8FF5F), Color(0xFF83FFF7), Color(0xFF7894FF), Color(0xFFD875FF), Color(0x00FFFFFF),
)

private fun rainbow(alpha: Float) = RAINBOW.map { it.copy(alpha = it.alpha * alpha) }

/**
 * A collectible card that tilts in 3D under your finger and sways gently when left alone.
 * Rarity shows in the frame, the foil and the reflection: Common is plain, Rare gets a silver sheen, Epic gets a rainbow holo foil
 * across the whole card, Legendary adds a golden frame, the brightest reflection and twinkling sparkles.
 */
@Composable
fun PokemonCard(p: CaughtPokemon, rarity: Rarity, imageFile: (Int) -> File, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val tiltX = remember { Animatable(0f) }
    val tiltY = remember { Animatable(0f) }
    var dragging by remember { mutableStateOf(false) }
    val idle = rememberInfiniteTransition(label = "idle")
    val swayX by idle.animateFloat(-1f, 1f, infiniteRepeatable(tween(3600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "swayX")
    val swayY by idle.animateFloat(-1f, 1f, infiniteRepeatable(tween(4800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "swayY")
    val bob by idle.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    val twinkle by idle.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "twinkle")
    val rx = tiltX.value + if (dragging) 0f else swayX * 4f
    val ry = tiltY.value + if (dragging) 0f else swayY * 11f

    val tier = rarity.ordinal
    val tint = rarityColor(rarity)
    val main = typeColor(p.types.firstOrNull() ?: "normal")
    val c1 = shade(main, 1.12f)
    val c2 = shade(main, 0.76f)
    val cream = Color(0xFFFFFDF4)
    val ink = Color(0xFF1C1C24)
    val soft = Color(0xFF5B5A4C)
    // Where the light sits follows the tilt, so the shine slides across as the card turns.
    val shineX = (0.5f + ry / 60f).coerceIn(0f, 1f)
    val shineY = (0.4f - rx / 50f).coerceIn(0f, 1f)
    val frame = when (rarity) {
        Rarity.Common -> listOf(Color(0xFFF2C94C), Color(0xFFD9A92B), Color(0xFFF2C94C))
        Rarity.Rare -> listOf(Color(0xFFE9F1FF), Color(0xFF7FA6D9), Color(0xFFF6FAFF), Color(0xFF7FA6D9))
        Rarity.Epic -> listOf(Color(0xFFD9A6FF), Color(0xFF8A55C7), Color(0xFFFF9BD9), Color(0xFF8A55C7))
        Rarity.Legendary -> listOf(Color(0xFFFFF1A8), Color(0xFFE0A020), Color(0xFFFFFFFF), Color(0xFFE0A020), Color(0xFFFFF1A8))
    }
    val frameBrush = Brush.linearGradient(frame, Offset(310f * density.density * (shineX - 0.5f), 0f), Offset(310f * density.density * (shineX + 0.9f), 470f * density.density))
    val sparks = remember(p.id) { List(if (tier == 3) 20 else if (tier == 2) 9 else 0) { Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat()) } }
    val cardShape = RoundedCornerShape(22.dp)

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
            .then(if (tier >= 2) Modifier.shadow(if (tier == 3) 26.dp else 16.dp, cardShape, ambientColor = tint, spotColor = tint) else Modifier)
            .clip(cardShape)
            .background(Brush.linearGradient(listOf(c1, c2)))
            .border(3.dp, frameBrush, cardShape)
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
                        val foil = listOf(0.17f, 0.3f, 0.5f, 0.7f)[tier]
                        val shift = size.width * (shineX * 1.6f - 0.3f)
                        drawRect(Brush.linearGradient(rainbow(foil), Offset(shift - size.width, 0f), Offset(shift + size.width * 0.6f, size.height)))
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
                Text(
                    when (rarity) {
                        Rarity.Common -> "LifeLoom"
                        Rarity.Rare -> "◆ Rare"
                        Rarity.Epic -> "✦ Epic"
                        Rarity.Legendary -> "★ Legendary"
                    },
                    fontSize = 9.5.sp,
                    fontWeight = if (tier == 0) FontWeight.Normal else FontWeight.ExtraBold,
                    color = if (tier == 0) Color(0xFF8C8A78) else shade(tint, 0.85f),
                )
            }
        }
        // The reflection: a soft light that follows the tilt, a diagonal glare band and, from Epic up, a foil across the whole card.
        Box(
            Modifier.fillMaxSize().drawBehind {
                drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent), Offset(size.width * shineX, size.height * shineY), size.width * 0.7f))
                val glare = listOf(0.1f, 0.17f, 0.26f, 0.36f)[tier]
                val pos = (shineX * 1.7f - 0.35f) * size.width
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = glare), Color.White.copy(alpha = glare * 0.3f), Color.Transparent),
                        Offset(pos - size.width * 0.35f, pos * 0.2f),
                        Offset(pos + size.width * 0.25f, size.height * 0.75f + pos * 0.2f),
                    ),
                )
                if (tier >= 2) {
                    val s2 = size.width * (shineX * 2f - 0.5f)
                    drawRect(Brush.linearGradient(rainbow(if (tier == 3) 0.2f else 0.12f), Offset(s2 - size.width, 0f), Offset(s2 + size.width, size.height)))
                }
                sparks.forEach { (sx, sy, phase) ->
                    val a = sin(((twinkle + phase) % 1f) * 2f * PI.toFloat()) * 0.5f + 0.5f
                    val r = (2.5f + 3f * sx) * density.density * (0.4f + 0.6f * a)
                    val c = Offset(size.width * (0.06f + 0.88f * sx), size.height * (0.04f + 0.92f * sy))
                    val col = if (tier == 3) Color(0xFFFFF3B0) else Color.White
                    drawCircle(col.copy(alpha = a * 0.85f), r, c)
                    drawLine(col.copy(alpha = a * 0.7f), Offset(c.x - r * 2.2f, c.y), Offset(c.x + r * 2.2f, c.y), strokeWidth = 1.2f * density.density)
                    drawLine(col.copy(alpha = a * 0.7f), Offset(c.x, c.y - r * 2.2f), Offset(c.x, c.y + r * 2.2f), strokeWidth = 1.2f * density.density)
                }
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

// ---------------------------------------------------------------- opening a Poké Ball

private class Spark(val angle: Float, val distance: Float, val size: Float, val delay: Float)

private val revealBackground = listOf(Color(0xFF4A4A63), Color(0xFF2C5AA8), Color(0xFF6B2FB0), Color(0xFFB9830F))

/**
 * The Poké Ball floats and waits. It takes a few taps (four to seven, picked at random) before it shakes hard and opens; golden
 * rays burst out and the Pokémon rises from the ball. The better the rarity the bigger the show. Silent, with a small buzz per tap.
 */
@Composable
fun PokeballOpenOverlay(
    reveal: BallReveal,
    canOpenAgain: Boolean,
    imageFile: (Int) -> File,
    onDone: (again: Boolean, collection: Boolean) -> Unit,
) {
    BackHandler { onDone(false, false) }
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val tier = reveal.rarity.ordinal
    val ball = ballColor(reveal.ball)
    val ballImage = ImageBitmap.imageResource(reveal.ball.image())
    val t = remember { Animatable(0f) }
    val needed = remember { Random.nextInt(4, 8) }
    var taps by remember { mutableIntStateOf(0) }
    var opening by remember { mutableStateOf(false) }
    val kick = remember { Animatable(1f) }
    val idle = rememberInfiniteTransition(label = "ballIdle")
    val float by idle.animateFloat(-1f, 1f, infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float")
    val hint by idle.animateFloat(0.55f, 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "hint")
    val spin = rememberInfiniteTransition(label = "rays")
    val rays by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(16000, easing = LinearEasing)), label = "raysAngle")
    val sparks = remember {
        List(listOf(8, 12, 18, 34)[tier]) {
            Spark(Random.nextFloat() * 6.2832f, 120f + Random.nextFloat() * (if (tier == 3) 230f else 150f), 3f + Random.nextFloat() * 4f, Random.nextFloat() * 0.18f)
        }
    }
    val density = LocalDensity.current
    LaunchedEffect(Unit) { t.animateTo(0.1f, tween(450, easing = LinearEasing)) }

    fun tapBall() {
        if (opening || t.value < 0.1f) return
        taps++
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        scope.launch {
            kick.snapTo(0f)
            kick.animateTo(1f, tween(420, easing = LinearEasing))
        }
        if (taps >= needed) {
            opening = true
            view.performHapticFeedback(if (tier >= 2) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.CONFIRM)
            // The last tap is the shake: jump straight to the moment before the lid lifts.
            scope.launch {
                t.snapTo(0.385f)
                t.animateTo(1f, tween(3000, easing = LinearEasing))
            }
        }
    }

    val bg = revealBackground[tier]
    Box(
        Modifier.fillMaxSize().background(Brush.radialGradient(listOf(if (opening) bg else Color(0xFF3A3563), Color(0xFF120F22)), Offset.Unspecified, 1500f)).pointerInput(Unit) { detectDragGestures { _, _ -> } },
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            val focusY = maxHeight * 0.34f
            val ballSize = 168.dp
            val open = FastOutSlowInEasing.transform(seg(t.value, 0.42f, 0.52f))
            val burst = seg(t.value, 0.46f, 0.64f)
            val shake = if (tier == 3 && t.value in 0.44f..0.7f) sin(t.value * 160f) * 7f * (1f - seg(t.value, 0.44f, 0.7f)) else 0f

            Box(Modifier.fillMaxSize().graphicsLayer { translationX = shake * density.density }) {
                // Golden rays: a faint leak while tapping, then the burst.
                Canvas(Modifier.fillMaxSize()) {
                    val cx = size.width / 2
                    val cy = focusY.toPx()
                    val leak = if (opening) 0f else taps / needed.toFloat() * 0.35f
                    val power = (leak + burst * listOf(0.55f, 0.7f, 0.85f, 1f)[tier]).coerceAtMost(1f)
                    if (power > 0f) {
                        val radius = 640.dp.toPx() * (0.5f + 0.5f * (if (opening) burst else 0.6f))
                        rotate(rays, Offset(cx, cy)) {
                            for (i in 0 until 15) drawArc(RayGold.copy(alpha = 0.34f * power), i * 24f, 11f, true, Offset(cx - radius, cy - radius), Size(radius * 2, radius * 2))
                        }
                        if (tier == 3) {
                            rotate(-rays * 1.4f, Offset(cx, cy)) {
                                for (i in 0 until 24) drawArc(Color(0xFFFFF6C8).copy(alpha = 0.3f * power), i * 15f, 4f, true, Offset(cx - radius, cy - radius), Size(radius * 2, radius * 2))
                            }
                        }
                        val halo = 230.dp.toPx()
                        drawCircle(Brush.radialGradient(listOf(RayGold.copy(alpha = 0.9f * power), RayGold.copy(alpha = 0.28f * power), Color.Transparent), Offset(cx, cy), halo), halo, Offset(cx, cy))
                    }
                    // Sparkles flying out of the ball.
                    sparks.forEach { s ->
                        val p = seg(t.value, 0.5f + s.delay, 0.92f)
                        if (p > 0f && p < 1f) {
                            val d = FastOutSlowInEasing.transform(p) * s.distance.dp.toPx() * 1.5f
                            drawCircle(Color(0xFFFFF3B0).copy(alpha = 1f - p), s.size.dp.toPx() * (1f - 0.6f * p), Offset(cx + cos(s.angle) * d, cy + sin(s.angle) * d))
                        }
                    }
                }

                // The ball: floats, wobbles harder with each tap, then the lid swings open on its hinge.
                val drop = (1f - FastOutSlowInEasing.transform(seg(t.value, 0f, 0.1f))) * -260f
                val finalShake = sin(seg(t.value, 0.12f, 0.4f) * 4f * PI.toFloat()) * 14f * (1f - seg(t.value, 0.12f, 0.4f))
                val tapShake = sin(kick.value * 3f * PI.toFloat()) * 16f * (1f - kick.value) * (if (taps % 2 == 0) 1f else -1f)
                val rotation = if (opening) finalShake else tapShake
                val squish = if (opening) 1f else 1f + 0.07f * (1f - kick.value)
                val vanish = 1f - seg(t.value, 0.52f, 0.58f)
                Canvas(
                    Modifier.align(Alignment.TopCenter).offset(y = focusY - ballSize / 2).size(ballSize)
                        .graphicsLayer {
                            translationY = (drop + (if (opening) 0f else float * 5f)) * density.density
                            rotationZ = rotation
                            scaleX = squish
                            scaleY = squish
                            alpha = vanish
                        },
                ) { drawBallOpening(ballImage, open, if (opening) 0f else taps / needed.toFloat()) }
                if (!opening) {
                    Box(
                        Modifier.align(Alignment.TopCenter).offset(y = focusY - 110.dp).size(220.dp)
                            .pointerInput(Unit) { detectTapGestures { tapBall() } },
                    )
                    Column(Modifier.align(Alignment.TopCenter).offset(y = focusY + 110.dp).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(reveal.ball.name.uppercase(), color = Color(0xFFC9C4FF), fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (taps == 0) "Tap the ball" else if (taps >= needed - 1) "Almost there…" else "Keep tapping",
                            Modifier.padding(top = 6.dp).graphicsLayer { alpha = hint },
                            color = Color.White,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                        )
                        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            repeat(needed) { i -> Box(Modifier.size(10.dp).clip(CircleShape).background(if (i < taps) RayGold else Color.White.copy(alpha = 0.25f))) }
                        }
                    }
                }

                // The white flash as it opens.
                val flash = seg(t.value, 0.42f, 0.46f) * (1f - seg(t.value, 0.46f, 0.6f))
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = flash * 0.9f }.background(Color.White))

                // The Pokémon rises from the ball and settles at its place.
                val rise = FastOutSlowInEasing.transform(seg(t.value, 0.5f, 0.64f))
                val pop = 1f + 0.1f * sin(seg(t.value, 0.56f, 0.72f) * PI.toFloat())
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(imageFile(reveal.pokemon.id)).build(),
                    contentDescription = PokemonMath.pretty(reveal.pokemon.name),
                    modifier = Modifier.align(Alignment.TopCenter).offset(y = focusY - 125.dp).size(250.dp)
                        .graphicsLayer { scaleX = (0.1f + 0.9f * rise) * pop; scaleY = (0.1f + 0.9f * rise) * pop; alpha = seg(t.value, 0.5f, 0.55f) },
                    contentScale = ContentScale.Fit,
                )
                Column(
                    Modifier.align(Alignment.TopCenter).offset(y = focusY - 190.dp).graphicsLayer { alpha = seg(t.value, 0.6f, 0.72f) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        reveal.rarity.label.uppercase(),
                        Modifier.clip(CircleShape).background(rarityColor(reveal.rarity)).padding(horizontal = 18.dp, vertical = 6.dp),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        letterSpacing = 2.sp,
                    )
                }
                Column(
                    Modifier.align(Alignment.TopCenter).offset(y = focusY + 140.dp).padding(horizontal = 24.dp).graphicsLayer { alpha = seg(t.value, 0.64f, 0.78f) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(PokemonMath.pretty(reveal.pokemon.name), color = Color.White, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 30.sp)
                    Text("${PokemonMath.number(reveal.pokemon.id)} · ${reveal.pokemon.genus}", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(12.dp))
                    if (reveal.isNew) {
                        Text("NEW", Modifier.clip(CircleShape).background(RayGold).padding(horizontal = 16.dp, vertical = 6.dp), color = Color(0xFF3D2C00), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    } else {
                        Row(
                            Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.18f)).padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("Already in your Pokéball · +${reveal.refund}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            PointsCoin(16.dp)
                        }
                    }
                }

                Column(
                    Modifier.align(Alignment.BottomCenter).padding(horizontal = 22.dp).padding(bottom = 24.dp).graphicsLayer { alpha = seg(t.value, 0.8f, 0.92f) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val ready = t.value > 0.8f
                    if (canOpenAgain) {
                        Row(
                            Modifier.fillMaxWidth().clip(CircleShape).background(ball).bouncyClick { if (ready) onDone(true, false) }.padding(vertical = 13.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) { Text("Open another ${reveal.ball.name}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp) }
                        Spacer(Modifier.height(10.dp))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier.weight(1f).clip(CircleShape).border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape).bouncyClick { if (ready) onDone(false, true) }.padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("My Pokéball", color = Color(0xFFD9D6F5), fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
                        Box(
                            Modifier.weight(1f).clip(CircleShape).background(RayGold).bouncyClick { if (ready) onDone(false, false) }.padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("Done", color = Color(0xFF3D2C00), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp) }
                    }
                }
            }
        }
    }
}

/** The ball's picture in two halves: the lower half stays, the upper half is the lid and swings up on a hinge at its left edge. */
private fun DrawScope.drawBallOpening(image: ImageBitmap, open: Float, crack: Float) {
    val s = size.minDimension
    val c = center
    val dst = IntSize(s.roundToInt(), s.roundToInt())
    val half = s * 0.45f

    clipRect(top = c.y) { drawImage(image, dstSize = dst, filterQuality = FilterQuality.High) }
    if (open > 0f) {
        val mouth = Offset(c.x - half * 0.92f, c.y - half * 0.16f)
        drawOval(Brush.radialGradient(listOf(Color(0xFFFFFDF0), Color(0xFFFFE9A0), Color(0xFFE9B449)), c, half), mouth, Size(half * 1.84f, half * 0.32f), alpha = open)
        val beam = Path().apply {
            moveTo(c.x - half * 0.8f, c.y)
            lineTo(c.x - half * 1.4f, c.y - half * 2.8f)
            lineTo(c.x + half * 1.4f, c.y - half * 2.8f)
            lineTo(c.x + half * 0.8f, c.y)
            close()
        }
        drawPath(beam, Brush.verticalGradient(listOf(Color(0x00FFF7C8), Color(0xE6FFF7C8)), c.y - half * 2.8f, c.y), alpha = open)
    }
    rotate(-52f * open, Offset(c.x - half * 0.92f, c.y)) {
        clipRect(bottom = c.y) { drawImage(image, dstSize = dst, filterQuality = FilterQuality.High) }
    }
    if (crack > 0f) {
        drawLine(
            Color(0xFFFFF3B0).copy(alpha = 0.35f + 0.6f * crack),
            Offset(c.x - half * 0.85f, c.y),
            Offset(c.x + half * 0.85f, c.y),
            strokeWidth = s * 0.03f * (0.5f + crack),
            cap = StrokeCap.Round,
        )
    }
}
