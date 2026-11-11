package com.amoldalwai.diary.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amoldalwai.diary.R

private const val REPO_URL = "https://github.com/amoldalwai/MyDailyDiary"

/** Who made the app and why, on a slowly moving gradient with cards that glide in one after another. */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    SystemBarsStyle(darkIcons = !appIsDark())
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty() }

    val drift = rememberInfiniteTransition(label = "aboutDrift")
    val phase by drift.animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "phase")
    val glow by drift.animateFloat(1f, 1.08f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    val c1 = colors.primaryContainer
    val c2 = colors.secondaryContainer
    val c3 = colors.tertiaryContainer
    val base = colors.background

    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                val w = size.width
                val h = size.height
                drawRect(
                    Brush.linearGradient(
                        listOf(c1, base, c2, c3),
                        start = Offset(w * phase * 0.5f, 0f),
                        end = Offset(w * (1f - phase * 0.5f), h),
                    ),
                )
                drawCircle(c3.copy(alpha = 0.85f), w * 0.55f, Offset(w * (0.1f + 0.6f * phase), h * (0.2f + 0.1f * phase)))
                drawCircle(c1.copy(alpha = 0.85f), w * 0.5f, Offset(w * (0.95f - 0.6f * phase), h * (0.78f - 0.12f * phase)))
                drawCircle(c2.copy(alpha = 0.8f), w * 0.4f, Offset(w * (0.5f + 0.3f * phase), h * (0.5f - 0.2f * phase)))
            },
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = colors.onBackground) }
                Text("About", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = colors.onBackground)
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(4.dp))

                AboutCard(phase, Modifier.staggeredEntrance(0)) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(104.dp)
                                .graphicsLayer {
                                    scaleX = glow
                                    scaleY = glow
                                }
                                .clip(RoundedCornerShape(28.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFFF0A67C), Color(0xFFD4694F), Color(0xFFA8412F)))),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(104.dp).graphicsLayer { scaleX = 1.2f; scaleY = 1.2f })
                        }
                        Spacer(Modifier.height(18.dp))
                        Text("Hi, I'm Amol Dalwai", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = colors.onSurface)
                        Text("The creator of LifeLoom", fontSize = 14.sp, color = colors.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Box(
                            Modifier
                                .size(52.dp)
                                .bouncyClick { uriHandler.openUri(REPO_URL) }
                                .clip(CircleShape)
                                .background(colors.onSurface),
                            contentAlignment = Alignment.Center,
                        ) { Icon(GitHubIcon, "Open the LifeLoom repository on GitHub", tint = colors.surface, modifier = Modifier.size(28.dp)) }
                        Spacer(Modifier.height(6.dp))
                        Text("See the code on GitHub", fontSize = 12.sp, color = colors.onSurfaceVariant)
                    }
                }

                AboutCard(phase, Modifier.staggeredEntrance(1)) {
                    Text("Why I built LifeLoom", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onSurface)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "I wanted one place to track my habits, my goals and my everyday activities, without paying for yet another subscription.",
                        fontSize = 15.sp,
                        lineHeight = 23.sp,
                        color = colors.onSurface,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "LifeLoom is for anyone who wants an easy way to keep track of everything they do, and to watch their progress add up, one day at a time.",
                        fontSize = 15.sp,
                        lineHeight = 23.sp,
                        color = colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "And a special thank you to my wife, Shilpa, for her love and support. 💛",
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.primary,
                    )
                }

                Row(Modifier.staggeredEntrance(2), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Promise("🎟️", "No subscriptions", phase, Modifier.weight(1f))
                    Promise("🔒", "Private on your phone", phase, Modifier.weight(1f))
                    Promise("🚫", "No ads", phase, Modifier.weight(1f))
                }

                AboutCard(phase, Modifier.staggeredEntrance(3)) {
                    Text("How LifeLoom works", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onSurface)
                    Spacer(Modifier.height(14.dp))
                    HowItWorks("🔒", "Everything stays on your phone", "Your pages, photos, habits and library live in a private database on this device. There is no account, no cloud copy and no tracking.")
                    Spacer(Modifier.height(14.dp))
                    HowItWorks("🌐", "The internet is only used to search", "When you add a movie, series, book or game, LifeLoom looks it up on Wikipedia, TVMaze, Steam and Open Library for the title, year and cover. Only your search words are sent, and the cover is saved on your phone.")
                    Spacer(Modifier.height(14.dp))
                    HowItWorks("🧱", "Under the hood", "Written in Kotlin with Jetpack Compose and Material 3. Data is stored in SQLite, images are loaded with Coil, and reminders are scheduled by Android itself.")
                }

                AboutCard(phase, Modifier.staggeredEntrance(4)) {
                    Text("Made with", fontSize = 12.sp, color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("Kotlin and Jetpack Compose", fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                    if (version.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text("Version", fontSize = 12.sp, color = colors.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(version, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                    }
                }
            }
        }
    }
}

/** Frosted-glass look: translucent tinted fill, bright edge and a soft sheen that drifts across. */
@Composable
private fun Modifier.glass(shape: RoundedCornerShape, phase: Float): Modifier {
    val colors = MaterialTheme.colorScheme
    val tint = colors.surface
    val edge = Color.White
    return this
        .clip(shape)
        .background(Brush.linearGradient(listOf(tint.copy(alpha = 0.6f), tint.copy(alpha = 0.32f))))
        .drawBehind {
            val w = size.width
            val x = -w * 0.4f + w * 1.8f * phase
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, edge.copy(alpha = 0.28f), Color.Transparent),
                    start = Offset(x, 0f),
                    end = Offset(x + w * 0.5f, size.height),
                ),
            )
        }
        .border(BorderStroke(1.2.dp, Brush.linearGradient(listOf(edge.copy(alpha = 0.95f), edge.copy(alpha = 0.15f), edge.copy(alpha = 0.6f)))), shape)
}

@Composable
private fun AboutCard(phase: Float, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(28.dp), phase)
            .padding(22.dp),
    ) { content() }
}

@Composable
private fun HowItWorks(emoji: String, title: String, body: String) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.Top) {
        Text(emoji, fontSize = 22.sp, modifier = Modifier.width(34.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(body, fontSize = 14.sp, lineHeight = 21.sp, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun Promise(emoji: String, label: String, phase: Float, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier
            .glass(RoundedCornerShape(22.dp), phase)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 26.sp)
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
