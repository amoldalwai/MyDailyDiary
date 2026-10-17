package com.amoldalwai.diary.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.amoldalwai.diary.data.SpanType
import com.amoldalwai.diary.data.TextSpan
import org.json.JSONArray

/** Bold/italic are stored as ranges next to the plain-text body, so the text never contains markup. */
object RichText {
    private val legacyPattern = Regex("""\*\*(.+?)\*\*|\*(.+?)\*""")

    /** Converts the earlier `**bold**` / `*italic*` marker format into plain text plus spans. */
    fun parseLegacy(text: String): Pair<String, List<TextSpan>> {
        val out = StringBuilder()
        val spans = mutableListOf<TextSpan>()
        var last = 0
        for (m in legacyPattern.findAll(text)) {
            out.append(text, last, m.range.first)
            val bold = m.groups[1] != null
            val inner = (m.groups[1] ?: m.groups[2])!!.value
            spans += TextSpan(out.length, out.length + inner.length, if (bold) SpanType.Bold else SpanType.Italic)
            out.append(inner)
            last = m.range.last + 1
        }
        out.append(text, last, text.length)
        return out.toString() to spans
    }

    fun spansToJson(spans: List<TextSpan>): String {
        val array = JSONArray()
        spans.forEach { array.put(JSONArray().put(it.start).put(it.end).put(it.type.ordinal)) }
        return array.toString()
    }

    fun spansFromJson(json: String?): List<TextSpan> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            List(array.length()) { i ->
                val item = array.getJSONArray(i)
                TextSpan(item.getInt(0), item.getInt(1), SpanType.entries[item.getInt(2)])
            }
        }.getOrDefault(emptyList())
    }

    fun normalize(spans: List<TextSpan>): List<TextSpan> =
        SpanType.entries.flatMap { type -> merge(spans.filter { it.type == type && it.end > it.start }) }
            .sortedBy { it.start }

    private fun merge(list: List<TextSpan>): List<TextSpan> {
        val result = mutableListOf<TextSpan>()
        for (s in list.sortedBy { it.start }) {
            val last = result.lastOrNull()
            if (last != null && s.start <= last.end) {
                result[result.lastIndex] = last.copy(end = maxOf(last.end, s.end))
            } else {
                result += s
            }
        }
        return result
    }

    /** True when every character in [start, end) already has [type]. */
    fun coversAll(spans: List<TextSpan>, type: SpanType, start: Int, end: Int): Boolean =
        end > start && normalize(spans).any { it.type == type && it.start <= start && it.end >= end }

    /** True when the character just before [pos] has [type]; this decides whether typing continues the style. */
    fun coversBefore(spans: List<TextSpan>, type: SpanType, pos: Int): Boolean =
        spans.any { it.type == type && it.start < pos && it.end >= pos }

    fun setRange(spans: List<TextSpan>, type: SpanType, start: Int, end: Int, on: Boolean): List<TextSpan> {
        if (end <= start) return spans
        val out = mutableListOf<TextSpan>()
        for (s in spans) {
            if (s.type != type || s.end <= start || s.start >= end) {
                out += s
            } else {
                if (s.start < start) out += TextSpan(s.start, start, type)
                if (s.end > end) out += TextSpan(end, s.end, type)
            }
        }
        if (on) out += TextSpan(start, end, type)
        return normalize(out)
    }

    /**
     * Keeps spans aligned after the text in [prefix, removedEnd) was replaced by [insertedLen] new characters.
     * The inserted text gets bold/italic only when the matching "active" flag is set.
     */
    fun applyEdit(
        spans: List<TextSpan>,
        prefix: Int,
        removedEnd: Int,
        insertedLen: Int,
        boldActive: Boolean,
        italicActive: Boolean,
    ): List<TextSpan> {
        val delta = insertedLen - (removedEnd - prefix)
        val pureInsert = removedEnd == prefix
        val moved = spans.mapNotNull { s ->
            val start: Int
            val end: Int
            if (pureInsert) {
                start = if (s.start >= prefix) s.start + delta else s.start
                end = if (s.end > prefix) s.end + delta else s.end
            } else {
                start = when {
                    s.start >= removedEnd -> s.start + delta
                    s.start > prefix -> prefix + insertedLen
                    else -> s.start
                }
                end = when {
                    s.end >= removedEnd -> s.end + delta
                    s.end > prefix -> prefix
                    else -> s.end
                }
            }
            if (end > start) TextSpan(start, end, s.type) else null
        }
        var result = moved
        if (insertedLen > 0) {
            result = setRange(result, SpanType.Bold, prefix, prefix + insertedLen, boldActive)
            result = setRange(result, SpanType.Italic, prefix, prefix + insertedLen, italicActive)
        }
        return normalize(result)
    }

    fun toAnnotated(text: String, spans: List<TextSpan>): AnnotatedString = buildAnnotatedString {
        append(text)
        for (s in spans) {
            val start = s.start.coerceIn(0, text.length)
            val end = s.end.coerceIn(0, text.length)
            if (end > start) {
                addStyle(
                    if (s.type == SpanType.Bold) SpanStyle(fontWeight = FontWeight.Bold) else SpanStyle(fontStyle = FontStyle.Italic),
                    start,
                    end,
                )
            }
        }
    }

    fun transformation(spans: List<TextSpan>) = VisualTransformation { text ->
        TransformedText(toAnnotated(text.text, spans), OffsetMapping.Identity)
    }
}
