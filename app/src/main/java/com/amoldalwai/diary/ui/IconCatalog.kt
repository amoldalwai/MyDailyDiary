package com.amoldalwai.diary.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BakeryDining
import androidx.compose.material.icons.rounded.Bathtub
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BeachAccess
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.DeliveryDining
import androidx.compose.material.icons.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Egg
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.Hiking
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Icecream
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocalLaundryService
import androidx.compose.material.icons.rounded.LocalPizza
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material.icons.rounded.LunchDining
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.NoDrinks
import androidx.compose.material.icons.rounded.NoFood
import androidx.compose.material.icons.rounded.NoMeals
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Piano
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Pool
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RamenDining
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SetMeal
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Shower
import androidx.compose.material.icons.rounded.SmokeFree
import androidx.compose.material.icons.rounded.SoupKitchen
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsBasketball
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.SportsTennis
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Theaters
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.rounded.Yard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class IconGroup(val name: String, val icons: List<Pair<String, ImageVector>>)

/** Every icon a habit or an activity can use, in picker order. Keys are what gets saved, so they never change. */
object IconCatalog {
    val groups: List<IconGroup> = listOf(
        IconGroup(
            "Health & fitness",
            listOf(
                "run" to Icons.Rounded.DirectionsRun, "gym" to Icons.Rounded.FitnessCenter, "walk" to Icons.Rounded.DirectionsWalk,
                "bike" to Icons.Rounded.DirectionsBike, "swim" to Icons.Rounded.Pool, "hike" to Icons.Rounded.Hiking,
                "football" to Icons.Rounded.SportsSoccer, "basketball" to Icons.Rounded.SportsBasketball, "tennis" to Icons.Rounded.SportsTennis,
                "meditate" to Icons.Rounded.SelfImprovement, "spa" to Icons.Rounded.Spa, "sleep" to Icons.Rounded.Bedtime,
                "water" to Icons.Rounded.WaterDrop, "medicine" to Icons.Rounded.Medication, "vitamins" to Icons.Rounded.Vaccines,
                "heart" to Icons.Rounded.MonitorHeart, "doctor" to Icons.Rounded.LocalHospital, "recover" to Icons.Rounded.Healing,
                "shower" to Icons.Rounded.Shower, "bath" to Icons.Rounded.Bathtub, "nosmoke" to Icons.Rounded.SmokeFree,
            ),
        ),
        IconGroup(
            "Food & drink",
            listOf(
                "healthy" to Icons.Rounded.Eco, "cook" to Icons.Rounded.Restaurant, "homemade" to Icons.Rounded.SoupKitchen,
                "fastfood" to Icons.Rounded.Fastfood, "delivery" to Icons.Rounded.DeliveryDining, "pizza" to Icons.Rounded.LocalPizza,
                "burger" to Icons.Rounded.LunchDining, "noodles" to Icons.Rounded.RamenDining, "fish" to Icons.Rounded.SetMeal,
                "breakfast" to Icons.Rounded.Egg, "bakery" to Icons.Rounded.BakeryDining, "sweets" to Icons.Rounded.Icecream,
                "coffee" to Icons.Rounded.Coffee, "drinks" to Icons.Rounded.LocalBar,
                "nomeat" to Icons.Rounded.NoMeals, "nosweets" to Icons.Rounded.NoFood, "nosoda" to Icons.Rounded.NoDrinks,
            ),
        ),
        IconGroup(
            "Hobbies & fun",
            listOf(
                "game" to Icons.Rounded.SportsEsports, "movie" to Icons.Rounded.Movie, "tv" to Icons.Rounded.Tv,
                "theatre" to Icons.Rounded.Theaters, "video" to Icons.Rounded.Videocam, "read" to Icons.Rounded.MenuBook,
                "stories" to Icons.Rounded.AutoStories, "music" to Icons.Rounded.MusicNote, "headphones" to Icons.Rounded.Headphones,
                "podcast" to Icons.Rounded.Podcasts, "instrument" to Icons.Rounded.Piano, "sing" to Icons.Rounded.Mic,
                "art" to Icons.Rounded.Brush, "paint" to Icons.Rounded.Palette, "draw" to Icons.Rounded.Draw,
                "photo" to Icons.Rounded.PhotoCamera, "puzzle" to Icons.Rounded.Extension, "board" to Icons.Rounded.Casino,
                "relax" to Icons.Rounded.BeachAccess, "nature" to Icons.Rounded.Park, "garden" to Icons.Rounded.Yard,
            ),
        ),
        IconGroup(
            "Work & study",
            listOf(
                "work" to Icons.Rounded.Work, "study" to Icons.Rounded.School, "computer" to Icons.Rounded.Computer,
                "code" to Icons.Rounded.Code, "meeting" to Icons.Rounded.Groups, "write" to Icons.Rounded.Edit,
                "journal" to Icons.Rounded.EditNote, "learn" to Icons.Rounded.Psychology, "idea" to Icons.Rounded.Lightbulb,
                "language" to Icons.Rounded.Language, "translate" to Icons.Rounded.Translate, "science" to Icons.Rounded.Science,
                "plan" to Icons.Rounded.Event, "email" to Icons.Rounded.Mail, "grow" to Icons.Rounded.TrendingUp,
                "save" to Icons.Rounded.Savings, "money" to Icons.Rounded.Payments, "income" to Icons.Rounded.AttachMoney,
            ),
        ),
        IconGroup(
            "People & home",
            listOf(
                "family" to Icons.Rounded.FamilyRestroom, "friends" to Icons.Rounded.People, "date" to Icons.Rounded.Favorite,
                "call" to Icons.Rounded.Call, "chat" to Icons.Rounded.Chat, "party" to Icons.Rounded.Celebration,
                "cake" to Icons.Rounded.Cake, "kindness" to Icons.Rounded.VolunteerActivism, "handshake" to Icons.Rounded.Handshake,
                "kids" to Icons.Rounded.ChildCare, "pet" to Icons.Rounded.Pets, "clean" to Icons.Rounded.CleaningServices,
                "laundry" to Icons.Rounded.LocalLaundryService, "fix" to Icons.Rounded.Handyman, "build" to Icons.Rounded.Build,
                "flowers" to Icons.Rounded.LocalFlorist, "shop" to Icons.Rounded.ShoppingCart, "home" to Icons.Rounded.Home,
            ),
        ),
        IconGroup(
            "Day & travel",
            listOf(
                "sunrise" to Icons.Rounded.WbSunny, "night" to Icons.Rounded.NightsStay, "alarm" to Icons.Rounded.Alarm,
                "timer" to Icons.Rounded.Timer, "phone" to Icons.Rounded.PhoneAndroid, "travel" to Icons.Rounded.Flight,
                "car" to Icons.Rounded.DirectionsCar, "train" to Icons.Rounded.Train, "trip" to Icons.Rounded.Luggage,
                "star" to Icons.Rounded.Star,
            ),
        ),
    )

    val all: Map<String, ImageVector> = linkedMapOf<String, ImageVector>().apply { groups.forEach { putAll(it.icons) } }

    /** Habits and activities saved with an emoji before icons existed keep working through this table. */
    private val legacyEmoji = mapOf(
        "💪" to "gym", "👟" to "walk", "📖" to "read", "🧘" to "meditate", "💧" to "water", "😴" to "sleep", "🥗" to "healthy",
        "🏃" to "run", "🎨" to "art", "🎵" to "music", "🧹" to "clean", "💊" to "medicine", "🙏" to "meditate", "✍️" to "write",
        "✍" to "write", "🚿" to "shower", "🌿" to "nature", "🎮" to "game", "🎬" to "movie", "🍳" to "cook", "☕" to "coffee",
        "🚗" to "car", "✈️" to "travel", "✈" to "travel", "🐶" to "pet", "🐱" to "pet", "🛒" to "shop", "🏠" to "home",
        "💻" to "computer", "📚" to "study", "💼" to "work", "🍕" to "pizza", "🍔" to "burger", "🍦" to "sweets", "🚴" to "bike",
        "🏊" to "swim", "⚽" to "football", "🎧" to "headphones", "📷" to "photo", "🎂" to "cake", "❤️" to "date", "❤" to "date",
        "📞" to "call", "👨‍👩‍👧" to "family", "👫" to "friends",
    )

    /** The catalog key for whatever was saved: a key itself, an old emoji, or the star when nothing matches. */
    fun keyFor(saved: String): String = if (saved in all) saved else legacyEmoji[saved] ?: "star"

    fun vectorFor(saved: String): ImageVector = all.getValue(keyFor(saved))
}

/** Just the glyph, in one colour. */
@Composable
fun Glyph(saved: String, tint: Color, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Icon(IconCatalog.vectorFor(saved), contentDescription = null, tint = tint, modifier = modifier.size(size))
}

/**
 * The shared icon look: a thin ring with a coloured glyph, and a filled circle with a white glyph once picked or done.
 */
@Composable
fun GlyphCircle(
    saved: String,
    selected: Boolean,
    size: Dp = 52.dp,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    onAccent: Color = MaterialTheme.colorScheme.onPrimary,
    idleRing: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
    idleGlyph: Color = accent,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .drawBehind { if (selected) drawCircle(accent) }
            .border(1.6.dp, if (selected) accent else idleRing, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            IconCatalog.vectorFor(saved),
            contentDescription = null,
            tint = if (selected) onAccent else idleGlyph,
            modifier = Modifier.size(size * 0.54f),
        )
    }
}

/** Every icon by group; the picked one shows filled. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconPicker(selected: String, onPick: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val current = IconCatalog.keyFor(selected)
    Column {
        IconCatalog.groups.forEach { group ->
            Text(group.name.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                group.icons.forEach { (key, _) ->
                    GlyphCircle(key, key == current, 44.dp, Modifier.bouncyClick { onPick(key) })
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

