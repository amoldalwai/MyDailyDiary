package com.amoldalwai.diary.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amoldalwai.diary.Screen

private data class TabSpec(val screen: Screen, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabSpec(Screen.Home, "Home", Icons.Rounded.Home),
    TabSpec(Screen.Journal, "Journal", Icons.Rounded.MenuBook),
    TabSpec(Screen.Library, "Library", Icons.Rounded.CollectionsBookmark),
    TabSpec(Screen.More, "More", Icons.Rounded.MoreHoriz),
)

/** A floating, rounded bar: Home and Journal, a raised round + for a new entry, then Library and More. */
@Composable
fun FloatingNavBar(current: Screen, onTab: (Screen) -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(34.dp)
    Box(
        modifier
            .fillMaxWidth()
            // A soft fade so list content slides under the bar instead of showing through it.
            .background(Brush.verticalGradient(listOf(Color.Transparent, colors.background.copy(alpha = 0.92f), colors.background)))
            .navigationBarsPadding()
            .padding(horizontal = 18.dp)
            .padding(top = 26.dp, bottom = 12.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(16.dp, shape, ambientColor = colors.primary.copy(alpha = 0.25f), spotColor = colors.primary.copy(alpha = 0.25f))
                .clip(shape)
                .background(colors.surface)
                .border(1.dp, colors.outline.copy(alpha = 0.6f), shape),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.take(2).forEach { NavItem(it, current == it.screen, Modifier.weight(1f).tourTarget("nav_${it.label.lowercase()}")) { onTab(it.screen) } }
            Spacer(Modifier.width(72.dp))
            tabs.drop(2).forEach { NavItem(it, current == it.screen, Modifier.weight(1f).tourTarget("nav_${it.label.lowercase()}")) { onTab(it.screen) } }
        }
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-14).dp)
                .size(60.dp)
                .tourTarget("nav_add")
                .bouncyClick(onAdd)
                .shadow(12.dp, CircleShape, ambientColor = colors.primary, spotColor = colors.primary)
                .clip(CircleShape)
                .background(colors.primary)
                .border(4.dp, colors.background, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Add, "New entry", tint = colors.onPrimary, modifier = Modifier.size(30.dp)) }
    }
}

@Composable
private fun NavItem(spec: TabSpec, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tint by animateColorAsState(if (selected) colors.primary else colors.onSurfaceVariant, label = "navTint")
    val indicator by animateColorAsState(if (selected) colors.primaryContainer else colors.primaryContainer.copy(alpha = 0f), label = "navPill")
    val lift by animateFloatAsState(if (selected) 1f else 0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "navLift")
    Column(modifier.bouncyClick(onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .graphicsLayer { translationY = -2.dp.toPx() * lift }
                .clip(RoundedCornerShape(16.dp))
                .background(indicator)
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) { Icon(spec.icon, spec.label, tint = tint, modifier = Modifier.size(24.dp)) }
        Text(spec.label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = tint)
    }
}
