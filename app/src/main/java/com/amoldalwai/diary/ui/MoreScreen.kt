package com.amoldalwai.diary.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CatchingPokemon
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amoldalwai.diary.data.LevelInfo

/** The rest of the app in one short list. */
@Composable
fun MoreScreen(
    entryCount: Int,
    onInsights: () -> Unit,
    onHabits: () -> Unit,
    onExercise: () -> Unit,
    pokemonEnabled: Boolean,
    onPokemon: () -> Unit,
    wallet: Int,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    profileName: String,
    profileEmoji: String,
    profilePhoto: java.io.File?,
    level: LevelInfo,
    onProfile: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    SystemBarsStyle(darkIcons = !appIsDark())
    Box(Modifier.fillMaxSize().background(colors.background)) {
        FloatingBlobs()
        Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 18.dp, bottom = 130.dp)) {
            Column(Modifier.staggeredEntrance(0)) {
                Text("More", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = colors.onBackground)
                Text("$entryCount ${if (entryCount == 1) "page" else "pages"} written so far", fontSize = 13.sp, color = colors.onSurfaceVariant)
            }
            Spacer(Modifier.height(18.dp))
            ProfileRow(profileName, profileEmoji, profilePhoto, level, Modifier.staggeredEntrance(1), onProfile)
            Spacer(Modifier.height(12.dp))
            MoreRow(Icons.Rounded.BarChart, "Insights", "Streaks, moods and your last 8 weeks", Modifier.staggeredEntrance(1), onInsights)
            Spacer(Modifier.height(12.dp))
            MoreRow(Icons.Rounded.TaskAlt, "Daily habits", "Add habits and see your streaks", Modifier.staggeredEntrance(2), onHabits)
            Spacer(Modifier.height(12.dp))
            MoreRow(Icons.Rounded.FitnessCenter, "Exercise", "Log the muscles you train and keep a weekly streak", Modifier.staggeredEntrance(2), onExercise)
            if (pokemonEnabled) {
                Spacer(Modifier.height(12.dp))
                MoreRow(Icons.Rounded.CatchingPokemon, "Poké Balls", "Spend your $wallet points on random Pokémon", Modifier.staggeredEntrance(2), onPokemon)
            }
            Spacer(Modifier.height(12.dp))
            MoreRow(Icons.Rounded.Settings, "Settings", "Colours, lock, reminders and sections", Modifier.staggeredEntrance(3), onSettings)
            Spacer(Modifier.height(12.dp))
            MoreRow(Icons.Rounded.Favorite, "About the creator", "Who made LifeLoom and why", Modifier.staggeredEntrance(4), onAbout)
        }
    }
}

@Composable
private fun ProfileRow(name: String, emoji: String, photo: java.io.File?, level: LevelInfo, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier.fillMaxWidth().bouncyClick(onClick).clip(shape).background(colors.surface).border(BorderStroke(1.dp, colors.outline.copy(alpha = 0.5f)), shape).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarRing(emoji, photo, level, 58.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(name.ifBlank { "Your profile" }, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = colors.onSurface)
            Text("Level ${level.level} · ${level.title} · ${level.points} XP", fontSize = 12.sp, color = colors.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = colors.onSurfaceVariant)
    }
}

@Composable
private fun MoreRow(icon: ImageVector, title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier.fillMaxWidth().bouncyClick(onClick).clip(shape).background(colors.surface).border(BorderStroke(1.dp, colors.outline.copy(alpha = 0.5f)), shape).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = colors.onPrimaryContainer)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = colors.onSurface)
            Text(subtitle, fontSize = 12.sp, color = colors.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = colors.onSurfaceVariant)
    }
}
