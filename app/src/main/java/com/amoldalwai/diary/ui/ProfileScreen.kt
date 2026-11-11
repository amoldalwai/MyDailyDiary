package com.amoldalwai.diary.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.amoldalwai.diary.data.AwardStatus
import com.amoldalwai.diary.data.AwardTier
import com.amoldalwai.diary.data.LevelInfo
import com.amoldalwai.diary.data.Progress
import com.amoldalwai.diary.data.ProgressStats
import com.amoldalwai.diary.data.Rank
import java.io.File

private val avatars = listOf(
    "🙂", "😎", "🤓", "🥳", "😺", "🐶", "🦊", "🐼", "🦉", "🐢", "🦋", "🌻",
    "🌙", "⭐", "🔥", "🌈", "📚", "🎮", "🎬", "🎧", "☕", "🚀", "🧘", "🌊",
)

fun rankBrush(rank: Rank): Brush = Brush.linearGradient(listOf(Color(rank.from), Color(rank.to)))

/** The avatar (photo or emoji) with a ring in the current rank's colours that fills toward the next level. */
@Composable
fun AvatarRing(emoji: String, photo: File?, level: LevelInfo, size: Dp, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val rank = Progress.rankOf(level.level)
    val sweep = Brush.sweepGradient(listOf(Color(rank.to), Color(rank.from), Color(rank.to)))
    val track = colors.outline.copy(alpha = 0.3f)
    val progress = level.progress
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = this.size.minDimension * 0.06f
            val inset = stroke / 2
            val box = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(track, -90f, 360f, false, Offset(inset, inset), box, style = Stroke(stroke))
            drawArc(sweep, -90f, 360f * progress.coerceIn(0.02f, 1f), false, Offset(inset, inset), box, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Box(Modifier.size(size * 0.82f).clip(CircleShape).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
            if (photo != null) {
                AsyncImage(model = photo, contentDescription = "Profile photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text(emoji, fontSize = (size.value * 0.4f).sp)
            }
        }
    }
}

/** Your level, rank, points and awards; everything is worked out from what you've written, read and watched. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    name: String,
    emoji: String,
    photo: File?,
    bio: String,
    bannerIndex: Int,
    level: LevelInfo,
    stats: ProgressStats,
    celebratedAwards: Set<String>,
    onSave: (name: String, emoji: String, bio: String, banner: Int) -> Unit,
    onPickPhoto: (android.net.Uri) -> Unit,
    onClearPhoto: () -> Unit,
    onJourney: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    SystemBarsStyle(darkIcons = false)
    var editing by remember { mutableStateOf(false) }
    val statuses = remember(stats) { Progress.statuses(stats) }
    val rankIdx = Progress.rankIndex(level.level)
    val bannerRank = Progress.ranks[if (bannerIndex in 0..rankIdx) bannerIndex else rankIdx]
    val rank = Progress.ranks[rankIdx]
    val shape = RoundedCornerShape(26.dp)
    val tierOf = { s: AwardStatus -> earnedTier(s, celebratedAwards) }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 40.dp)) {
            Box(Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp)).background(rankBrush(bannerRank))) {
                val t = rememberInfiniteTransition(label = "banner")
                val drift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse), label = "drift")
                Canvas(Modifier.matchParentSize()) {
                    drawCircle(Color.White.copy(alpha = 0.13f), 110.dp.toPx(), Offset(size.width * (0.12f + 0.12f * drift), size.height * 0.25f))
                    drawCircle(Color.White.copy(alpha = 0.10f), 70.dp.toPx(), Offset(size.width * (0.88f - 0.1f * drift), size.height * 0.55f))
                    drawCircle(Color.White.copy(alpha = 0.08f), 45.dp.toPx(), Offset(size.width * (0.55f + 0.08f * drift), size.height * 0.1f))
                }
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).bouncyClick(onBack).clip(CircleShape).background(Color.Black.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Color.White)
                    }
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.size(42.dp).bouncyClick { editing = true }.clip(CircleShape).background(Color.Black.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Edit, "Edit profile", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Column(Modifier.fillMaxWidth().offset(y = (-72).dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box {
                    Box(Modifier.size(148.dp).clip(CircleShape).background(colors.background), contentAlignment = Alignment.Center) {
                        AvatarRing(emoji, photo, level, 140.dp)
                    }
                    Box(
                        Modifier.align(Alignment.BottomEnd).size(44.dp).clip(CircleShape).background(colors.background).padding(3.dp).clip(CircleShape).background(rankBrush(rank)),
                        contentAlignment = Alignment.Center,
                    ) { Text("${level.level}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp) }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    name.ifBlank { "Your name" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (name.isBlank()) colors.onSurfaceVariant else colors.onBackground,
                )
                if (bio.isNotBlank()) {
                    Text(bio, fontSize = 14.sp, color = colors.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp, vertical = 2.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "${rank.emoji} ${rank.name} · Level ${level.level}",
                    modifier = Modifier.clip(CircleShape).background(rankBrush(rank)).padding(horizontal = 16.dp, vertical = 7.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }

            Column(Modifier.offset(y = (-48).dp).padding(horizontal = 20.dp)) {
                Column(
                    Modifier.fillMaxWidth().clip(shape).background(colors.surface).border(BorderStroke(1.dp, colors.outline.copy(alpha = 0.5f)), shape).padding(18.dp),
                ) {
                    ProgressBar(level.progress, colors.outline.copy(alpha = 0.3f), Color(rank.to), Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (level.nextAt == null) "${level.points} XP · top level reached"
                        else "${level.points} XP · ${level.nextAt - level.points} to level ${level.level + 1}",
                        fontSize = 13.sp,
                        color = colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth().bouncyClick(onJourney).clip(RoundedCornerShape(16.dp)).background(colors.primaryContainer).padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("🗺️  Level journey", fontWeight = FontWeight.SemiBold, color = colors.onPrimaryContainer, modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = colors.onPrimaryContainer)
                    }
                }

                Spacer(Modifier.height(14.dp))
                val earned = statuses.sumOf { s -> tierOf(s)?.let { it.ordinal + 1 } ?: 0 }
                val completed = stats.booksFinished + stats.gamesCompleted + stats.moviesWatched + stats.seriesFinished
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("📝", "${stats.entries}", "Entries", Modifier.weight(1f))
                    StatTile("🔥", "${stats.bestStreak}", "Best streak", Modifier.weight(1f))
                    StatTile("🏅", "$earned", "Medals", Modifier.weight(1f))
                    StatTile("✅", "$completed", "Finished", Modifier.weight(1f))
                }

                Spacer(Modifier.height(22.dp))
                Text("Showcase", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onBackground)
                Spacer(Modifier.height(10.dp))
                val showcase = statuses.mapNotNull { s -> tierOf(s)?.let { s to it } }.sortedByDescending { it.second.ordinal }.take(3)
                if (showcase.isEmpty()) {
                    Text("Earn your first award and it will be shown here.", fontSize = 13.sp, color = colors.onSurfaceVariant)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        showcase.forEach { (s, tier) -> ShowcaseMedal(s, tier, Modifier.weight(1f)) }
                        repeat(3 - showcase.size) { Spacer(Modifier.weight(1f)) }
                    }
                }

                Spacer(Modifier.height(22.dp))
                Text("Awards", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onBackground)
                Text("Earn bronze, silver and gold in each one.", fontSize = 13.sp, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                statuses.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { AwardCard(it, tierOf(it), Modifier.weight(1f)) }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Spacer(Modifier.height(10.dp))
                Text("How XP works", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onBackground)
                Spacer(Modifier.height(10.dp))
                Column(
                    Modifier.fillMaxWidth().clip(shape).background(colors.surface).border(BorderStroke(1.dp, colors.outline.copy(alpha = 0.5f)), shape).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatLine("Entries written", stats.entries, "+10 each")
                    StatLine("Photos added", stats.photos, "+2 each, up to 5 per entry")
                    StatLine("Longest streak", stats.bestStreak, "+50 for every 7 days")
                    StatLine("Perfect habit days", stats.perfectHabitDays, "+15 each")
                    StatLine("Books finished", stats.booksFinished, "+50 each")
                    StatLine("Games completed", stats.gamesCompleted, "+50 each")
                    StatLine("Movies watched", stats.moviesWatched, "+20 each")
                    StatLine("Series finished", stats.seriesFinished, "+40 each")
                    StatLine("Ratings given", stats.ratings, "+5 each")
                    StatLine("Reviews written", stats.reviews, "+10 each")
                }
            }
        }
    }

    if (editing) {
        var draftName by remember { mutableStateOf(name) }
        var draftBio by remember { mutableStateOf(bio) }
        var draftEmoji by remember { mutableStateOf(emoji) }
        var draftBanner by remember { mutableIntStateOf(bannerIndex) }
        val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) onPickPhoto(uri) }
        ModalBottomSheet(
            onDismissRequest = { editing = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.surface,
        ) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Edit profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Button(onClick = {
                        onSave(draftName, draftEmoji, draftBio, draftBanner)
                        editing = false
                    }) { Text("Save") }
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarRing(draftEmoji, photo, level, 84.dp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Row(
                            Modifier.bouncyClick { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                                .clip(CircleShape).background(colors.primaryContainer).padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.PhotoCamera, null, tint = colors.onPrimaryContainer, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Choose photo", color = colors.onPrimaryContainer, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        if (photo != null) TextButton(onClick = onClearPhoto) { Text("Remove photo") }
                    }
                }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(draftName, { draftName = it.take(24) }, singleLine = true, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(draftBio, { draftBio = it.take(60) }, singleLine = true, label = { Text("Bio") }, placeholder = { Text("A line about you") }, modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(16.dp))
                Text("Banner", fontWeight = FontWeight.SemiBold)
                Text("Each rank you reach unlocks a new banner.", fontSize = 12.sp, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Auto",
                        modifier = Modifier.height(44.dp).clip(RoundedCornerShape(14.dp))
                            .border(BorderStroke(if (draftBanner == -1) 3.dp else 1.dp, if (draftBanner == -1) colors.primary else colors.outline), RoundedCornerShape(14.dp))
                            .bouncyClick { draftBanner = -1 }.padding(horizontal = 14.dp, vertical = 12.dp),
                        fontSize = 13.sp,
                    )
                    Progress.ranks.forEachIndexed { i, r ->
                        val unlocked = i <= rankIdx
                        Box(
                            Modifier.size(44.dp).alpha(if (unlocked) 1f else 0.4f).clip(RoundedCornerShape(14.dp)).background(rankBrush(r))
                                .border(BorderStroke(if (draftBanner == i) 3.dp else 0.dp, colors.primary), RoundedCornerShape(14.dp))
                                .bouncyClick { if (unlocked) draftBanner = i },
                            contentAlignment = Alignment.Center,
                        ) { Text(if (unlocked) r.emoji else "🔒", fontSize = 18.sp) }
                    }
                }

                if (photo == null) {
                    Spacer(Modifier.height(16.dp))
                    Text("Avatar", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        avatars.forEach { a ->
                            Box(
                                Modifier.size(42.dp).bouncyClick { draftEmoji = a }.clip(CircleShape)
                                    .background(if (a == draftEmoji) colors.primary.copy(alpha = 0.3f) else colors.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) { Text(a, fontSize = 22.sp) }
                        }
                    }
                }
            }
        }
    }
}

private fun earnedTier(status: AwardStatus, celebrated: Set<String>): AwardTier? {
    // Awards stay earned even if the data behind them later shrinks.
    val remembered = (0..2).lastOrNull { "${status.award.id}:$it" in celebrated }?.let { AwardTier.entries[it] }
    return listOfNotNull(status.tier, remembered).maxByOrNull { it.ordinal }
}

@Composable
private fun StatTile(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.clip(shape).background(colors.surface).border(BorderStroke(1.dp, colors.outline.copy(alpha = 0.5f)), shape).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 20.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.onSurface)
        Text(label, fontSize = 10.sp, color = colors.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun ShowcaseMedal(status: AwardStatus, tier: AwardTier, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val tint = when (tier) {
        AwardTier.Bronze -> Color(0xFFCD8B5A)
        AwardTier.Silver -> Color(0xFFB9C2CC)
        AwardTier.Gold -> Gold
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(78.dp).clip(CircleShape).background(Brush.verticalGradient(listOf(tint.copy(alpha = 0.55f), tint.copy(alpha = 0.18f))))
                .border(BorderStroke(3.dp, tint), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text(status.award.emoji, fontSize = 34.sp) }
        Spacer(Modifier.height(6.dp))
        Text(status.award.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${tier.emoji} ${tier.label}", fontSize = 11.sp, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun AwardCard(status: AwardStatus, tier: AwardTier?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(22.dp)
    val locked = tier == null
    Column(
        modifier.clip(shape).background(colors.surface).border(BorderStroke(1.dp, colors.outline.copy(alpha = 0.5f)), shape).padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(status.award.emoji, fontSize = 36.sp, modifier = Modifier.alpha(if (locked) 0.35f else 1f))
        Spacer(Modifier.height(4.dp))
        Text(status.award.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.onSurface, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            tier?.let { "${it.emoji} ${it.label}" } ?: "Locked",
            fontSize = 12.sp,
            color = if (locked) colors.onSurfaceVariant else colors.onSurface,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(10.dp))
        val next = status.next
        if (next != null) {
            ProgressBar(status.progressToNext, colors.outline.copy(alpha = 0.35f), Gold, Modifier.fillMaxWidth())
            Spacer(Modifier.height(5.dp))
            Text("${status.current} / $next ${status.award.goal}", fontSize = 11.sp, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        } else {
            Text("Max tier · ${status.current} ${status.award.goal}", fontSize = 11.sp, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun StatLine(label: String, value: Int, rule: String) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, color = colors.onSurface)
            Text(rule, fontSize = 11.sp, color = colors.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Text("$value", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = colors.onSurface)
    }
}
