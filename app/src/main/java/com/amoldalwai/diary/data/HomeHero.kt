package com.amoldalwai.diary.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/** One random landscape photo per day for the top of Home, kept on the phone so it also shows offline. */
class HomeHero(context: Context) {
    private val appContext = context.applicationContext
    private val dir = File(appContext.filesDir, "hero").apply { mkdirs() }

    /** True when the last real download attempt failed; skipped attempts (already fetched today, offline) leave it alone. */
    @Volatile
    var lastFetchFailed = false
        private set

    /** The newest photo already stored, today's or an older one. */
    fun latest(): File? = dir.listFiles { f -> f.extension == "jpg" }?.maxByOrNull { it.lastModified() }

    /**
     * Fetches a photo when none has been stored today (or always, with [force]) and a network is available.
     * Returns the new file, or null if nothing new was stored.
     */
    suspend fun refresh(today: LocalDate, force: Boolean = false): File? = withContext(Dispatchers.IO) {
        if (!force && dir.listFiles().orEmpty().any { it.name.startsWith("hero_${today}_") }) return@withContext null
        if (!isOnline()) return@withContext null
        // The date seeds the daily photo, so reopening the app doesn't change it.
        val seed = if (force) System.nanoTime().toString() else today.toString()
        val target = File(dir, "hero_${today}_$seed.jpg")
        val fetched = runCatching {
            val conn = URL("https://picsum.photos/seed/$seed/800/520").openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 8_000
                conn.readTimeout = 12_000
                if (conn.responseCode !in 200..299) return@runCatching null
                val bytes = conn.inputStream.use { it.readBytes() }
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                if (bounds.outWidth <= 0) return@runCatching null
                target.writeBytes(bytes)
                dir.listFiles()?.filter { it != target }?.forEach { it.delete() }
                target
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
        lastFetchFailed = fetched == null
        fetched
    }

    fun isOnline(): Boolean {
        val manager = appContext.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
