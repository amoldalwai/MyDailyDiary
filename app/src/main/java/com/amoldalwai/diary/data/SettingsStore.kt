package com.amoldalwai.diary.data

import android.content.Context
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import org.json.JSONObject

/** Per-habit nudge choice: whether it is asked about, and its own time window ([from] and [to] are minutes after midnight, -1 = use the shared window). */
data class HabitNudge(val on: Boolean = true, val from: Int = -1, val to: Int = -1)

enum class ThemeMode { System, Light, Dark }

enum class Palette(val label: String) { Sage("Sage"), Teal("Teal"), Lavender("Lavender"), Terracotta("Terracotta") }

class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("diary_settings", Context.MODE_PRIVATE)

    var palette by mutableStateOf(
        runCatching { Palette.valueOf(prefs.getString("palette", "Sage")!!) }.getOrDefault(Palette.Sage),
    )
        private set

    fun updatePalette(value: Palette) {
        palette = value
        prefs.edit().putString("palette", value.name).apply()
    }

    // Quick-add chips; kept on this phone only (the tags they produce travel with each entry).
    var activityGroups by mutableStateOf(prefs.getString("activities", null)?.let(Activities::decode) ?: Activities.defaults)
        private set

    fun updateActivityGroups(groups: List<ActivityGroup>) {
        activityGroups = groups
        prefs.edit().putString("activities", Activities.encode(groups)).apply()
    }

    // Whether Quick Add shows the Work / Study / Health... chip groups.
    var quickActivities by mutableStateOf(prefs.getBoolean("quickActivities", true))
        private set

    fun updateQuickActivities(enabled: Boolean) {
        quickActivities = enabled
        prefs.edit().putBoolean("quickActivities", enabled).apply()
    }

    // Local profile and the highest level / awards already celebrated (0 = nothing recorded yet).
    var profileName by mutableStateOf(prefs.getString("profileName", "") ?: "")
        private set
    var profileEmoji by mutableStateOf(prefs.getString("profileEmoji", "🙂") ?: "🙂")
        private set
    var profilePhoto by mutableStateOf(prefs.getString("profilePhoto", null))
        private set
    var profileBio by mutableStateOf(prefs.getString("profileBio", "") ?: "")
        private set

    /** Index of the chosen banner, or -1 to follow the current rank. */
    var profileBanner by mutableIntStateOf(prefs.getInt("profileBanner", -1))
        private set
    var celebratedLevel by mutableIntStateOf(prefs.getInt("levelSeen2", 0))
        private set
    var celebratedAwards by mutableStateOf(prefs.getStringSet("awardsSeen2", null)?.toSet())
        private set
    var coversShrunk by mutableStateOf(prefs.getBoolean("coversShrunk", false))
        private set

    fun markCoversShrunk() {
        coversShrunk = true
        prefs.edit().putBoolean("coversShrunk", true).apply()
    }

    fun updateProfile(name: String, emoji: String, bio: String, banner: Int) {
        profileName = name.trim().take(24)
        profileEmoji = emoji
        profileBio = bio.trim().take(60)
        profileBanner = banner
        prefs.edit().putString("profileName", profileName).putString("profileEmoji", emoji)
            .putString("profileBio", profileBio).putInt("profileBanner", banner).apply()
    }

    fun updateProfilePhoto(name: String?) {
        profilePhoto = name
        prefs.edit().apply { if (name == null) remove("profilePhoto") else putString("profilePhoto", name) }.apply()
    }

    fun updateCelebrated(level: Int, awards: Set<String>) {
        celebratedLevel = level
        celebratedAwards = awards
        prefs.edit().putInt("levelSeen2", level).putStringSet("awardsSeen2", awards).apply()
    }

    var themeMode by mutableStateOf(
        runCatching { ThemeMode.valueOf(prefs.getString("theme", "System")!!) }.getOrDefault(ThemeMode.System),
    )
        private set
    var pinEnabled by mutableStateOf(prefs.contains("pinHash"))
        private set
    var biometricEnabled by mutableStateOf(prefs.getBoolean("biometric", false))
        private set
    var reminderEnabled by mutableStateOf(prefs.getBoolean("reminderOn", false))
        private set
    var reminderHour by mutableIntStateOf(prefs.getInt("reminderHour", 21))
        private set
    var reminderMinute by mutableIntStateOf(prefs.getInt("reminderMinute", 0))
        private set
    var booksEnabled by mutableStateOf(prefs.getBoolean("booksEnabled", true))
        private set
    var habitSound by mutableStateOf(prefs.getBoolean("habitSound", true))
        private set

    // Habit nudges: one question about one habit at a time, spaced out inside a daily window (minutes after midnight).
    var nudgesEnabled by mutableStateOf(prefs.getBoolean("nudgesOn", false))
        private set
    var nudgeFrom by mutableIntStateOf(prefs.getInt("nudgeFrom", 10 * 60))
        private set
    var nudgeTo by mutableIntStateOf(prefs.getInt("nudgeTo", 20 * 60))
        private set
    var nudgeGap by mutableIntStateOf(prefs.getInt("nudgeGap", 90))
        private set
    var nudgeOverrides by mutableStateOf(decodeNudges(prefs.getString("nudgeHabits", null)))
        private set

    fun updateNudges(enabled: Boolean, from: Int, to: Int, gap: Int) {
        nudgesEnabled = enabled
        nudgeFrom = from
        nudgeTo = to
        nudgeGap = gap
        prefs.edit().putBoolean("nudgesOn", enabled).putInt("nudgeFrom", from).putInt("nudgeTo", to).putInt("nudgeGap", gap).apply()
    }

    fun nudgeFor(habitId: String): HabitNudge = nudgeOverrides[habitId] ?: HabitNudge()

    fun updateHabitNudge(habitId: String, nudge: HabitNudge) {
        nudgeOverrides = nudgeOverrides + (habitId to nudge)
        val json = JSONObject()
        nudgeOverrides.forEach { (id, n) -> json.put(id, JSONObject().put("on", n.on).put("from", n.from).put("to", n.to)) }
        prefs.edit().putString("nudgeHabits", json.toString()).apply()
    }

    /** The habit and wording last used, so the next nudge rotates instead of repeating. */
    var nudgeLastHabit: String?
        get() = prefs.getString("nudgeLastHabit", null)
        set(value) = prefs.edit().putString("nudgeLastHabit", value).apply()
    var nudgeLastText: Int
        get() = prefs.getInt("nudgeLastText", -1)
        set(value) = prefs.edit().putInt("nudgeLastText", value).apply()

    private fun decodeNudges(json: String?): Map<String, HabitNudge> {
        if (json == null) return emptyMap()
        return runCatching {
            val o = JSONObject(json)
            o.keys().asSequence().associateWith { id ->
                val n = o.getJSONObject(id)
                HabitNudge(n.optBoolean("on", true), n.optInt("from", -1), n.optInt("to", -1))
            }
        }.getOrDefault(emptyMap())
    }

    fun updateBooksEnabled(enabled: Boolean) {
        booksEnabled = enabled
        prefs.edit().putBoolean("booksEnabled", enabled).apply()
    }

    var gamesEnabled by mutableStateOf(prefs.getBoolean("gamesEnabled", true))
        private set

    var watchEnabled by mutableStateOf(prefs.getBoolean("watchEnabled", true))
        private set

    fun updateWatchEnabled(enabled: Boolean) {
        watchEnabled = enabled
        prefs.edit().putBoolean("watchEnabled", enabled).apply()
    }

    fun updateGamesEnabled(enabled: Boolean) {
        gamesEnabled = enabled
        prefs.edit().putBoolean("gamesEnabled", enabled).apply()
    }

    fun updateHabitSound(enabled: Boolean) {
        habitSound = enabled
        prefs.edit().putBoolean("habitSound", enabled).apply()
    }

    fun updateTheme(mode: ThemeMode) {
        themeMode = mode
        prefs.edit().putString("theme", mode.name).apply()
    }

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString("pinSalt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("pinHash", Base64.encodeToString(hash(pin, salt), Base64.NO_WRAP))
            .apply()
        pinEnabled = true
    }

    fun clearPin() {
        prefs.edit().remove("pinSalt").remove("pinHash").putBoolean("biometric", false).apply()
        pinEnabled = false
        biometricEnabled = false
    }

    fun verifyPin(pin: String): Boolean {
        val salt = prefs.getString("pinSalt", null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        val expected = prefs.getString("pinHash", null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        return MessageDigest.isEqual(expected, hash(pin, salt))
    }

    fun updateBiometric(enabled: Boolean) {
        biometricEnabled = enabled
        prefs.edit().putBoolean("biometric", enabled).apply()
    }

    fun updateReminder(enabled: Boolean, hour: Int, minute: Int) {
        reminderEnabled = enabled
        reminderHour = hour
        reminderMinute = minute
        prefs.edit().putBoolean("reminderOn", enabled).putInt("reminderHour", hour).putInt("reminderMinute", minute).apply()
    }

    private fun hash(pin: String, salt: ByteArray): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(pin.toCharArray(), salt, 20_000, 256))
            .encoded
}
