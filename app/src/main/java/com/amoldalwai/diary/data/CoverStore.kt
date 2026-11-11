package com.amoldalwai.diary.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import java.io.File
import java.util.UUID
import kotlin.math.max

/** Saves downloaded cover art as a small portrait JPEG (covers are never zoomed); shared by every library section. */
object CoverStore {
    private const val POSTER_W = 360
    private const val POSTER_H = 530
    private const val QUALITY = 72

    /** Returns the stored file name, or null when the bytes are not a decodable image. */
    fun save(dir: File, prefix: String, bytes: ByteArray): String? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0) return@runCatching null
        val options = BitmapFactory.Options().apply { inSampleSize = max(1, bounds.outWidth / POSTER_W) }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return@runCatching null
        val bitmap = fitWidth(decoded)
        // Wide art (game headers, logos, banners) becomes a poster so every tile has the same shape.
        val poster = if (bitmap.width.toFloat() / bitmap.height > 0.8f) toPoster(bitmap) else bitmap
        val name = "${prefix}${UUID.randomUUID()}.jpg"
        File(dir, name).outputStream().use { poster.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
        if (poster !== bitmap) poster.recycle()
        bitmap.recycle()
        name
    }.getOrNull()

    private fun fitWidth(src: Bitmap): Bitmap {
        if (src.width <= POSTER_W) return src
        val scaled = Bitmap.createScaledBitmap(src, POSTER_W, src.height * POSTER_W / src.width, true)
        if (scaled !== src) src.recycle()
        return scaled
    }

    /** Re-encodes covers saved by older versions at the current size; returns the bytes freed. */
    fun shrinkExisting(dir: File): Long {
        var freed = 0L
        dir.listFiles { f -> COVER_PREFIXES.any { f.name.startsWith(it) } }.orEmpty().forEach { file ->
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, bounds)
                if (bounds.outWidth <= POSTER_W && file.length() < 40_000) return@runCatching
                val options = BitmapFactory.Options().apply { inSampleSize = max(1, bounds.outWidth / POSTER_W) }
                val decoded = BitmapFactory.decodeFile(file.path, options) ?: return@runCatching
                val bitmap = fitWidth(decoded)
                val tmp = File(dir, file.name + ".tmp")
                tmp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
                bitmap.recycle()
                if (tmp.length() < file.length()) {
                    freed += file.length() - tmp.length()
                    tmp.renameTo(file)
                } else {
                    tmp.delete()
                }
            }
        }
        return freed
    }

    private val COVER_PREFIXES = listOf("book_", "game_", "watch_")

    private fun toPoster(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(POSTER_W, POSTER_H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val cropW = (src.height * POSTER_W / POSTER_H).coerceAtMost(src.width)
        val crop = Bitmap.createBitmap(src, (src.width - cropW) / 2, 0, cropW, src.height)
        val tiny = Bitmap.createScaledBitmap(crop, 10, 15, true)
        val blurred = Bitmap.createScaledBitmap(tiny, POSTER_W, POSTER_H, true)
        canvas.drawBitmap(blurred, 0f, 0f, null)
        canvas.drawColor(0x66000000)
        val fitH = src.height * POSTER_W / src.width
        val top = (POSTER_H - fitH) / 2
        canvas.drawBitmap(src, Rect(0, 0, src.width, src.height), Rect(0, top, POSTER_W, top + fitH), Paint(Paint.FILTER_BITMAP_FLAG))
        crop.recycle()
        tiny.recycle()
        blurred.recycle()
        return out
    }
}
