package com.amoldalwai.diary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amoldalwai.diary.data.FinishInfo
import java.time.LocalDate

private const val LONG_AGO = 0

/**
 * Asked when something is marked finished: a rating (required) and, for things added after the fact,
 * the year ([askYear]) or "Long time ago". [onCancel] null means the question can't be skipped.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FinishDialog(
    title: String,
    noun: String,
    startRating: Int?,
    askYear: Boolean,
    onConfirm: (FinishInfo) -> Unit,
    onCancel: (() -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme
    val thisYear = remember { LocalDate.now().year }
    var rating by remember { mutableStateOf(startRating) }
    var year by remember { mutableStateOf(thisYear) }
    val watched = noun == "movie" || noun == "series"

    AlertDialog(
        onDismissRequest = { onCancel?.invoke() },
        title = { Text(if (askYear) "Add “$title”" else "How was “$title”?", maxLines = 2) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                RatingSlider(rating, { rating = it })
                if (askYear) {
                    Spacer(Modifier.height(16.dp))
                    Text(if (watched) "When did you watch it?" else "When did you finish it?", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Pick the year. If you don't remember, choose Long time ago; it won't count in your yearly summaries.",
                        fontSize = 12.sp,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        (thisYear downTo thisYear - 5).forEach { y -> YearChip(y.toString(), year == y) { year = y } }
                        YearChip("Long time ago", year == LONG_AGO) { year = LONG_AGO }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { rating?.let { onConfirm(FinishInfo(it, year.takeIf { y -> askYear && y != LONG_AGO })) } },
                enabled = rating != null,
            ) { Text(if (askYear) "Add to library" else "Save") }
        },
        dismissButton = onCancel?.let { cancel -> { TextButton(onClick = cancel) { Text("Back") } } },
    )
}

@Composable
private fun YearChip(label: String, on: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Text(
        label,
        modifier = Modifier
            .bouncyClick(onClick)
            .clip(CircleShape)
            .background(if (on) colors.primary else colors.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        color = if (on) colors.onPrimary else colors.onSurface,
        fontSize = 13.sp,
        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
    )
}
