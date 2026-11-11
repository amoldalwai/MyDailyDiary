package com.amoldalwai.diary.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amoldalwai.diary.data.Palette
import com.amoldalwai.diary.ui.theme.diaryColors

/** One round swatch per palette (its main colour over its soft tint); the chosen one gets a ring and a tick. */
@Composable
fun PalettePicker(selected: Palette, darkMode: Boolean, onSelect: (Palette) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Palette.entries.forEach { palette ->
            val scheme = diaryColors(palette, darkMode)
            val on = palette == selected
            val ring by animateColorAsState(if (on) scheme.primary else Color.Transparent, label = "ring")
            Column(Modifier.bouncyClick { onSelect(palette) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(54.dp).border(3.dp, ring, CircleShape).padding(5.dp).clip(CircleShape).background(scheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(28.dp).clip(CircleShape).background(scheme.primary), contentAlignment = Alignment.Center) {
                        if (on) Icon(Icons.Rounded.Check, null, tint = scheme.onPrimary, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    palette.label,
                    fontSize = 11.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
