package com.amoldalwai.diary.ui

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private class StrokeData(val points: List<Offset>, val color: Color, val widthDp: Float)

private val paper = Color(0xFFFFFBF3)
private val inkColors = listOf(
    Color(0xFF2E2622), Color(0xFFD9575A), Color(0xFFE8896B), Color(0xFFE5B84A),
    Color(0xFF7C9A82), Color(0xFF4F8FC0), Color(0xFF8B7BB8), Color(0xFFFFFFFF),
)

@Composable
fun DrawingDialog(onDismiss: () -> Unit, onSave: (Bitmap) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current.density
    val strokes = remember { mutableStateListOf<StrokeData>() }
    val current = remember { mutableStateListOf<Offset>() }
    var color by remember { mutableStateOf(inkColors.first()) }
    var widthDp by remember { mutableFloatStateOf(5f) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    fun commit() {
        if (current.isNotEmpty()) {
            strokes.add(StrokeData(current.toList(), color, widthDp))
            current.clear()
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = colors.surface) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Cancel", tint = colors.onSurface) }
                    Text("Draw a memory", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onSurface)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) }) {
                        Icon(Icons.AutoMirrored.Rounded.Undo, "Undo", tint = colors.onSurface)
                    }
                    IconButton(onClick = { strokes.clear() }) { Icon(Icons.Rounded.DeleteOutline, "Clear", tint = colors.onSurface) }
                    Surface(
                        onClick = {
                            if (strokes.isNotEmpty() && canvasSize.width > 0) {
                                onSave(render(strokes, canvasSize, density))
                            } else {
                                onDismiss()
                            }
                        },
                        shape = CircleShape,
                        color = colors.primary,
                    ) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Check, null, tint = colors.onPrimary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add", color = colors.onPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }

                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(paper)
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { current.add(it) },
                                onDragEnd = { commit() },
                                onDragCancel = { commit() },
                            ) { change, _ ->
                                change.consume()
                                current.add(change.position)
                            }
                        },
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        strokes.forEach { drawStroke(it.points, it.color, it.widthDp.dp.toPx()) }
                        if (current.isNotEmpty()) drawStroke(current.toList(), color, widthDp.dp.toPx())
                    }
                    if (strokes.isEmpty() && current.isEmpty()) {
                        Text(
                            "Sketch, doodle, or write something by hand",
                            modifier = Modifier.align(Alignment.Center),
                            color = Color(0xFF2E2622).copy(alpha = 0.35f),
                        )
                    }
                }

                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    inkColors.forEach { c ->
                        Box(
                            Modifier
                                .size(if (c == color) 34.dp else 28.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(if (c == color) 3.dp else 1.dp, if (c == color) colors.primary else colors.outline, CircleShape)
                                .clickable { color = c },
                        )
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(colors.onSurfaceVariant))
                    Slider(
                        value = widthDp,
                        onValueChange = { widthDp = it },
                        valueRange = 2f..24f,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    )
                    Box(Modifier.size(20.dp).clip(CircleShape).background(colors.onSurfaceVariant))
                }
            }
        }
    }
}

private fun DrawScope.drawStroke(points: List<Offset>, color: Color, width: Float) {
    if (points.size == 1) {
        drawCircle(color, radius = width / 2f, center = points[0])
        return
    }
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val cur = points[i]
            quadraticBezierTo(prev.x, prev.y, (prev.x + cur.x) / 2f, (prev.y + cur.y) / 2f)
        }
        lineTo(points.last().x, points.last().y)
    }
    drawPath(path, color, style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun render(strokes: List<StrokeData>, size: IntSize, density: Float): Bitmap {
    val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(paper.toArgb())
    for (stroke in strokes) {
        val width = stroke.widthDp * density
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stroke.color.toArgb()
            strokeWidth = width
            style = if (stroke.points.size == 1) Paint.Style.FILL else Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val points = stroke.points
        if (points.size == 1) {
            canvas.drawCircle(points[0].x, points[0].y, width / 2f, paint)
            continue
        }
        val path = AndroidPath().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val cur = points[i]
                quadTo(prev.x, prev.y, (prev.x + cur.x) / 2f, (prev.y + cur.y) / 2f)
            }
            lineTo(points.last().x, points.last().y)
        }
        canvas.drawPath(path, paint)
    }
    return bitmap
}
