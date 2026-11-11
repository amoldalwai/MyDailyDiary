package com.amoldalwai.diary.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amoldalwai.diary.R
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource

private class Slide(
    val emoji: List<String>,
    val title: String,
    val body: String,
    val points: List<String>,
    val from: Color,
    val to: Color,
)

private val slides = listOf(
    Slide(
        listOf("📓", "❤️", "🧵"),
        "Welcome to LifeLoom",
        "Your journal, habits and everything you read, play and watch, woven into one calm place.",
        listOf("Everything stays on your phone", "No account and no ads", "Made to feel like yours"),
        Color(0xFFF0A67C), Color(0xFFA8412F),
    ),
    Slide(
        listOf("✍️", "📷", "🎙️"),
        "Write your day",
        "Decorate a full page, or log the day in seconds with Quick Add.",
        listOf("Moods, tags, photos, drawing and voice", "A calendar and search to find any day", "New pages are for today onward, so your journal stays honest"),
        Color(0xFF86AE90), Color(0xFF2C5240),
    ),
    Slide(
        listOf("✅", "🔥", "⏰"),
        "Build habits that stick",
        "Tick habits off from Home or any page and watch the streaks grow.",
        listOf("A This week table of the last 7 days", "Friendly nudges, one habit at a time", "Choose the hours and the habits you want to be asked about"),
        Color(0xFF63CBC4), Color(0xFF175862),
    ),
    Slide(
        listOf("📚", "🎮", "🎬"),
        "Your personal library",
        "Keep books, games, movies and series with posters, ratings and reviews.",
        listOf("Search them all from one box", "Attach them to your journal pages", "Confetti when you finish one"),
        Color(0xFFC5ABEE), Color(0xFF5A3C98),
    ),
    Slide(
        listOf("🏆", "⭐", "🚀"),
        "Level up as you go",
        "Every entry, habit and finished title earns XP.",
        listOf("50 levels across 10 ranks", "Bronze, silver and gold awards", "A profile with your own photo and banner"),
        Color(0xFFF7C948), Color(0xFFB86A12),
    ),
)

/** First-run introduction: swipeable slides with Skip and Next; the last slide can lead into the interactive tour. */
@Composable
fun WelcomeScreen(onFinish: (startTour: Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    SystemBarsStyle(darkIcons = !appIsDark())
    val pager = rememberPagerState { slides.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == slides.lastIndex

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            if (!last) TextButton(onClick = { onFinish(false) }) { Text("Skip") } else Spacer(Modifier.height(48.dp))
        }

        HorizontalPager(pager, Modifier.weight(1f)) { page -> SlidePage(slides[page], page == 0) }

        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            slides.indices.forEach { i ->
                val on = i == pager.currentPage
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (on) 22.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(if (on) colors.primary else colors.outline.copy(alpha = 0.6f)),
                )
            }
        }

        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (last) {
                Button(onClick = { onFinish(true) }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Take a quick tour", fontWeight = FontWeight.SemiBold) }
                TextButton(onClick = { onFinish(false) }) { Text("Start writing") }
            } else {
                Button(
                    onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text("Next", fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun SlidePage(slide: Slide, showLogo: Boolean) {
    val colors = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "slideFloat")
    val bob by transition.animateFloat(-6f, 6f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    val shape = RoundedCornerShape(36.dp)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Short screens (landscape, split view) get no picture and a scrolling page.
        val showPicture = maxHeight >= 260.dp
        val pictureHeight = (maxHeight * 0.42f).coerceIn(96.dp, 230.dp)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SlideContent(slide, showLogo, bob, if (showPicture) pictureHeight else null, shape)
        }
    }
}

@Composable
private fun SlideContent(slide: Slide, showLogo: Boolean, bob: Float, pictureHeight: androidx.compose.ui.unit.Dp?, shape: RoundedCornerShape) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (pictureHeight != null) Box(
            Modifier.fillMaxWidth().height(pictureHeight).clip(shape).background(Brush.linearGradient(listOf(slide.from, slide.to))),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(260.dp).offset(x = 90.dp, y = (-70).dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)))
            Box(Modifier.size(170.dp).offset(x = (-110).dp, y = 80.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f)))
            if (showLogo) {
                Image(
                    painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(pictureHeight * 0.9f).graphicsLayer { translationY = bob * density },
                )
            } else {
                Text(slide.emoji[0], fontSize = 84.sp, modifier = Modifier.graphicsLayer { translationY = bob * density })
                Text(slide.emoji[1], fontSize = 44.sp, modifier = Modifier.offset(x = (-92).dp, y = 52.dp).graphicsLayer { translationY = -bob * density })
                Text(slide.emoji[2], fontSize = 44.sp, modifier = Modifier.offset(x = 92.dp, y = (-48).dp).graphicsLayer { translationY = bob * density })
            }
        }
        Spacer(Modifier.height(26.dp))
        Text(slide.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = colors.onBackground, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(slide.body, fontSize = 16.sp, lineHeight = 22.sp, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            slide.points.forEach { point ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(Modifier.padding(top = 3.dp).size(18.dp).clip(CircleShape).background(slide.from.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                        Text("✓", fontSize = 11.sp, color = slide.to, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(point, fontSize = 15.sp, lineHeight = 21.sp, color = colors.onSurface)
                }
            }
        }
    }
}
