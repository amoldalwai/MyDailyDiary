package com.amoldalwai.diary.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import java.io.File
import java.util.UUID
import kotlin.math.max

/** Saves downloaded cover art as an about-500 px portrait JPEG; shared by every library section. */
object CoverStore {
    private const val POSTER_W = 500
    private const val POSTER_H = 735

    /** Returns the stored file name, or null when the bytes are not a decodable image. */
    fun save(dir: File, prefix: String, bytes: ByteArray): String? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0) return@runCatching null
        val options = BitmapFactory.Options().apply { inSampleSize = max(1, bounds.outWidth / 500) }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return@runCatching null
        // Wide art (game headers, logos, banners) becomes a poster so every tile has the same shape.
        val poster = if (bitmap.width.toFloat() / bitmap.height > 0.8f) toPoster(bitmap) else bitmap
        val name = "${prefix}${UUID.randomUUID()}.jpg"
        File(dir, name).outputStream().use { poster.compress(Bitmap.CompressFormat.JPEG, 80, it) }
        if (poster !== bitmap) poster.recycle()
        bitmap.recycle()
        name
    }.getOrNull()

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
