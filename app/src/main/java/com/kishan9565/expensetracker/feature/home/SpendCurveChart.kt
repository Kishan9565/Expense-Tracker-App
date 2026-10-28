package com.kishan9565.expensetracker.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kishan9565.expensetracker.core.money.Money
import com.kishan9565.expensetracker.core.time.epochDay
import com.kishan9565.expensetracker.data.db.DayTotal
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** Cumulative spend per day within a specific date range. No-spend days repeat previous totals. */
fun cumulativeSpend(daily: List<DayTotal>, startDate: LocalDate, endDate: LocalDate, today: LocalDate): List<Long> {
    if (startDate.isAfter(today)) return emptyList()
    val first = startDate.epochDay()
    val last = minOf(endDate, today).epochDay()
    val daysCount = (last - first + 1).toInt()
    if (daysCount <= 0) return emptyList()
    
    val byDay = daily.associate { it.day to it.totalPaise }
    var running = 0L
    return List(daysCount) { i ->
        running += byDay[first + i] ?: 0L
        running
    }
}

private fun smoothPath(p: List<Offset>): Path = Path().apply {
    if (p.isEmpty()) return@apply
    moveTo(p[0].x, p[0].y)
    for (i in 1 until p.size) {
        val midX = (p[i - 1].x + p[i].x) / 2f
        cubicTo(midX, p[i - 1].y, midX, p[i].y, p[i].x, p[i].y)   // horizontal tangents: smooth, no overshoot
    }
}

@Composable
fun MiniSparkline(
    data: List<Long>,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (data.size < 2) {
        // Fallback flat line or empty space
        Canvas(modifier.height(32.dp).width(60.dp)) {
            val y = size.height / 2
            drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }
        return
    }
    
    val maxValue = maxOf(data.max(), 1L)
    Canvas(modifier.height(32.dp).width(60.dp)) {
        val w = size.width
        val h = size.height
        val pts = data.mapIndexed { i, v -> 
            Offset(i * w / (data.size - 1), h - (v.toFloat() / maxValue * h))
        }
        val line = smoothPath(pts)
        drawPath(line, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun SpendCurveChart(
    current: List<Long>,                 // cumulative paise per day so far; caller guarantees size >= 2
    previous: List<Long>?,               // last period, same units; null = no comparison line
    daysInPeriod: Int,
    modifier: Modifier = Modifier,
    onScrub: (dayIndex: Int?) -> Unit = {},
) {
    require(current.size >= 2)
    val lineColor = MaterialTheme.colorScheme.primary
    val faintColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val gridColor = MaterialTheme.colorScheme.outline
    var scrub by remember { mutableStateOf<Int?>(null) }
    val maxValue = maxOf(current.max(), previous?.maxOrNull() ?: 0L, 1L)
    val summary = "Spent ${Money.format(current.last())} so far this period"

    Canvas(
        modifier
            .fillMaxWidth()
            .height(160.dp)
            .semantics { contentDescription = summary }
            .pointerInput(current, daysInPeriod) {
                fun indexAt(x: Float) =
                    ((x / size.width) * (daysInPeriod - 1)).roundToInt().coerceIn(0, current.lastIndex)
                detectDragGestures(
                    onDragStart = { scrub = indexAt(it.x); onScrub(scrub) },
                    onDrag = { change, _ -> scrub = indexAt(change.position.x); onScrub(scrub) },
                    onDragEnd = { scrub = null; onScrub(null) },
                    onDragCancel = { scrub = null; onScrub(null) },
                )
            }
    ) {
        val top = 8.dp.toPx()
        val h = size.height - top
        // Float is fine for DRAWING only; money stays Long everywhere else
        fun pt(i: Int, v: Long) = Offset(i * size.width / (daysInPeriod - 1), top + h * (1f - v.toFloat() / maxValue))

        repeat(3) { g ->
            val y = top + h * g / 2f
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
        previous?.let { prev ->
            drawPath(
                smoothPath(prev.mapIndexed { i, v -> pt(i, v) }), faintColor,
                style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))),
            )
        }

        val pts = current.mapIndexed { i, v -> pt(i, v) }
        val line = smoothPath(pts)
        val fill = Path().apply {
            addPath(line); lineTo(pts.last().x, size.height); lineTo(pts.first().x, size.height); close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.25f), Color.Transparent), startY = top, endY = size.height))
        drawPath(line, lineColor, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        val focus = pts[scrub ?: current.lastIndex]       // end dot = today; dragging moves it
        if (scrub != null) drawLine(faintColor, Offset(focus.x, top), Offset(focus.x, size.height), strokeWidth = 1.dp.toPx())
        drawCircle(lineColor.copy(alpha = 0.25f), 8.dp.toPx(), focus)
        drawCircle(lineColor, 4.dp.toPx(), focus)
    }
}
