package com.amoldalwai.diary.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import com.amoldalwai.diary.data.DiaryEntry
import com.amoldalwai.diary.ui.PageStyles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object PdfExporter {
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 48f

    suspend fun exportAndShare(
        context: Context,
        entries: List<DiaryEntry>,
        fileName: String,
        coverTitle: String?,
        imageFile: (String) -> File,
    ) {
        val file = withContext(Dispatchers.IO) { export(context, entries, fileName, coverTitle, imageFile) }
        share(context, file)
    }

    private fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share diary PDF"))
    }

    private fun export(
        context: Context,
        entries: List<DiaryEntry>,
        fileName: String,
        coverTitle: String?,
        imageFile: (String) -> File,
    ): File {
        val dir = File(context.cacheDir, "pdf").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val out = File(dir, "$safeName.pdf")

        val doc = PdfDocument()
        val writer = Writer(doc)
        if (coverTitle != null) writer.cover(coverTitle, entries.size)
        entries.sortedWith(compareBy({ it.dateEpochDay }, { it.createdAt })).forEach { writer.entry(it, imageFile) }
        writer.finish()
        out.outputStream().use { doc.writeTo(it) }
        doc.close()
        return out
    }

    private fun typefaceFor(fontId: String): Typeface = when (fontId) {
        "sans" -> Typeface.SANS_SERIF
        "hand" -> Typeface.create("cursive", Typeface.NORMAL)
        "casual" -> Typeface.create("casual", Typeface.NORMAL)
        "light" -> Typeface.create("sans-serif-light", Typeface.NORMAL)
        "mono" -> Typeface.MONOSPACE
        else -> Typeface.SERIF
    }

    private class Writer(val doc: PdfDocument) {
        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var pageCount = 0
        private var y = 0f
        private var current: DiaryEntry? = null
        private var ink = Color.BLACK

        private fun startPage(entry: DiaryEntry?) {
            finish()
            pageCount++
            val p = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageCount).create())
            page = p
            canvas = p.canvas
            val bg = PageStyles.background(entry?.style?.backgroundId ?: "paper")
            val paint = Paint().apply {
                shader = LinearGradient(0f, 0f, 0f, PAGE_H.toFloat(), bg.top.toArgb(), bg.bottom.toArgb(), Shader.TileMode.CLAMP)
            }
            p.canvas.drawRect(0f, 0f, PAGE_W.toFloat(), PAGE_H.toFloat(), paint)
            ink = bg.ink.toArgb()
            current = entry
            y = MARGIN
        }

        fun finish() {
            page?.let { doc.finishPage(it) }
            page = null
        }

        private fun ensure(height: Float) {
            if (page == null || y + height > PAGE_H - MARGIN) startPage(current)
        }

        private fun paint(size: Float, bold: Boolean = false, alpha: Int = 255, face: Typeface = Typeface.SERIF) =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size
                typeface = if (bold) Typeface.create(face, Typeface.BOLD) else face
                color = ink
                this.alpha = alpha
            }

        private fun lines(text: String, paint: Paint, maxWidth: Float): List<String> {
            val result = mutableListOf<String>()
            for (para in text.split("\n")) {
                if (para.isBlank()) {
                    result += ""
                    continue
                }
                var line = ""
                for (word in para.split(" ")) {
                    val candidate = if (line.isEmpty()) word else "$line $word"
                    if (line.isEmpty() || paint.measureText(candidate) <= maxWidth) {
                        line = candidate
                    } else {
                        result += line
                        line = word
                    }
                }
                result += line
            }
            return result
        }

        private fun text(text: String, paint: Paint, gapAfter: Float = 0f) {
            val lineHeight = paint.textSize * 1.5f
            for (line in lines(text, paint, PAGE_W - 2 * MARGIN)) {
                ensure(lineHeight)
                canvas!!.drawText(line, MARGIN, y + paint.textSize, paint)
                y += lineHeight
            }
            y += gapAfter
        }

        fun cover(title: String, count: Int) {
            startPage(null)
            val c = canvas!!
            val big = paint(34f, bold = true)
            val small = paint(14f, alpha = 160)
            c.drawText(title, (PAGE_W - big.measureText(title)) / 2f, PAGE_H / 2f - 10f, big)
            val sub = "$count ${if (count == 1) "entry" else "entries"}"
            c.drawText(sub, (PAGE_W - small.measureText(sub)) / 2f, PAGE_H / 2f + 24f, small)
        }

        fun entry(entry: DiaryEntry, imageFile: (String) -> File) {
            startPage(entry)
            val face = typefaceFor(entry.style.fontId)
            val scale = entry.style.fontScale
            val date = entry.date

            text(
                date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase(),
                paint(10f, alpha = 150, face = Typeface.SANS_SERIF),
            )
            text(date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())), paint(28f, bold = true), 6f)

            val meta = buildList {
                entry.mood?.let { add(it.label) }
                if (entry.tags.isNotEmpty()) add(entry.tags.joinToString(" ") { "#$it" })
            }.joinToString("   ·   ")
            if (meta.isNotEmpty()) text(meta, paint(11f, alpha = 170, face = Typeface.SANS_SERIF), 14f) else y += 10f

            if (entry.title.isNotBlank()) text(entry.title, paint(20f * scale, bold = true, face = face), 8f)
            if (entry.body.isNotBlank()) text(entry.body, paint(12f * scale, face = face), 14f)

            for (name in entry.images) {
                val bmp = decode(imageFile(name)) ?: continue
                val width = min(300f, PAGE_W - 2 * MARGIN)
                val height = min(width * bmp.height / bmp.width, 320f)
                val drawW = if (height < width * bmp.height / bmp.width) height * bmp.width / bmp.height else width
                ensure(height + 40f)
                val c = canvas!!
                val left = (PAGE_W - drawW) / 2f
                c.drawRect(left - 8f, y, left + drawW + 8f, y + height + 24f, Paint().apply { color = Color.rgb(255, 254, 251) })
                c.drawBitmap(bmp, null, android.graphics.RectF(left, y + 8f, left + drawW, y + 8f + height), Paint(Paint.FILTER_BITMAP_FLAG))
                y += height + 44f
                bmp.recycle()
            }
        }

        private fun decode(file: File): Bitmap? {
            if (!file.exists()) return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            if (bounds.outWidth <= 0) return null
            val opts = BitmapFactory.Options().apply { inSampleSize = max(1, bounds.outWidth / 900) }
            return BitmapFactory.decodeFile(file.path, opts)
        }
    }
}
