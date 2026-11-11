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

enum class ThemeMode { System, Light, Dark }

class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("diary_settings", Context.MODE_PRIVATE)

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
    var driveEmail by mutableStateOf(prefs.getString("driveEmail", null))
        private set
    var lastSyncAt by mutableLongStateOf(prefs.getLong("lastSyncAt", 0L))
        private set

    val driveConnected: Boolean get() = driveEmail != null

    var booksEnabled by mutableStateOf(prefs.getBoolean("booksEnabled", true))
        private set
    var habitSound by mutableStateOf(prefs.getBoolean("habitSound", true))
        private set

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

    fun updateDrive(email: String?) {
        driveEmail = email
        prefs.edit().apply { if (email == null) remove("driveEmail").remove("lastSyncAt") else putString("driveEmail", email) }.apply()
        if (email == null) lastSyncAt = 0L
    }

    fun updateLastSync(at: Long) {
        lastSyncAt = at
        prefs.edit().putLong("lastSyncAt", at).apply()
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
