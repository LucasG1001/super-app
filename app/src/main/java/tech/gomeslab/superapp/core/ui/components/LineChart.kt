package tech.gomeslab.superapp.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tech.gomeslab.superapp.core.ui.theme.ControlActive
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

data class ChartPoint(val x: Double, val y: Double, val label: String)

private val LINE_WIDTH = 2.dp
private val DOT_RADIUS = 4.dp
private val DOT_RING = 2.dp
private val GRID_WIDTH = 1.dp
private val AXIS_GUTTER = 40.dp
private val AXIS_LABEL_GAP = 8.dp
private val BOTTOM_GUTTER = 22.dp
private val TOP_GUTTER = 28.dp
private val TOOLTIP_PADDING = 8.dp
private const val AREA_ALPHA = 0.10f
private const val TARGET_TICKS = 4

/**
 * Gráfico de linha de uma série (sem legenda: o título da seção nomeia a série). Linha de 2dp,
 * área a 10%, ponto final com rótulo do valor e leitura por toque/arrasto (cruz + balão).
 */
@Composable
fun LineChart(
    points: List<ChartPoint>,
    formatValue: (Double) -> String,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    description: String = "",
) {
    val sorted = remember(points) { points.sortedBy { it.x } }
    val measurer = rememberTextMeasurer()
    val colors = MaterialTheme.colorScheme
    val axisStyle = MaterialTheme.typography.labelMedium.copy(color = colors.onSurfaceVariant)
    val valueStyle = MaterialTheme.typography.labelLarge.copy(color = colors.onSurface)
    var selected by remember(sorted) { mutableStateOf<Int?>(null) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = description }
            .pointerInput(sorted) {
                detectTapGestures { offset -> selected = nearestIndex(sorted, offset.x, size.width.toFloat(), AXIS_GUTTER.toPx()) }
            }
            .pointerInput(sorted) {
                detectDragGestures(
                    onDragEnd = { selected = null },
                    onDragCancel = { selected = null },
                ) { change, _ ->
                    selected = nearestIndex(sorted, change.position.x, size.width.toFloat(), AXIS_GUTTER.toPx())
                }
            },
    ) {
        if (sorted.isEmpty()) return@Canvas
        val ticks = niceTicks(sorted.minOf { it.y }, sorted.maxOf { it.y })
        val yMin = ticks.first()
        val yMax = ticks.last()
        val tickLabels = ticks.map { measurer.measure(formatValue(it), axisStyle) }
        val left = (tickLabels.maxOf { it.size.width } + AXIS_LABEL_GAP.toPx()).coerceAtLeast(AXIS_GUTTER.toPx())
        val top = TOP_GUTTER.toPx()
        val plotWidth = size.width - left - DOT_RADIUS.toPx() * 2
        val plotHeight = size.height - top - BOTTOM_GUTTER.toPx()
        val xMin = sorted.first().x
        val xSpan = (sorted.last().x - xMin).takeIf { it > 0 } ?: 1.0

        val byIndex = sorted.last().x == xMin
        fun px(index: Int, p: ChartPoint) = Offset(
            x = when {
                sorted.size == 1 -> left + plotWidth / 2
                byIndex -> left + index.toFloat() / sorted.lastIndex * plotWidth
                else -> left + ((p.x - xMin) / xSpan).toFloat() * plotWidth
            },
            y = top + (1 - ((p.y - yMin) / (yMax - yMin)).toFloat()) * plotHeight,
        )

        ticks.forEachIndexed { index, tick ->
            val y = top + (1 - ((tick - yMin) / (yMax - yMin)).toFloat()) * plotHeight
            drawLine(colors.outlineVariant, Offset(left, y), Offset(size.width, y), GRID_WIDTH.toPx())
            val text = tickLabels[index]
            drawText(text, topLeft = Offset(0f, y - text.size.height / 2f))
        }

        val first = measurer.measure(sorted.first().label, axisStyle)
        drawText(first, topLeft = Offset(left, size.height - first.size.height))
        if (sorted.size > 1) {
            val last = measurer.measure(sorted.last().label, axisStyle)
            drawText(last, topLeft = Offset(size.width - last.size.width, size.height - last.size.height))
        }

        val offsets = sorted.mapIndexed(::px)
        if (offsets.size > 1) {
            val line = Path().apply {
                moveTo(offsets.first().x, offsets.first().y)
                offsets.drop(1).forEach { lineTo(it.x, it.y) }
            }
            val area = Path().apply {
                addPath(line)
                lineTo(offsets.last().x, top + plotHeight)
                lineTo(offsets.first().x, top + plotHeight)
                close()
            }
            drawPath(area, ControlActive.copy(alpha = AREA_ALPHA))
            drawPath(
                line,
                ControlActive,
                style = Stroke(width = LINE_WIDTH.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }

        val endIndex = offsets.lastIndex
        drawDot(offsets[endIndex], colors.surface)
        if (selected == null) {
            val endLabel = measurer.measure(formatValue(sorted[endIndex].y), valueStyle)
            val x = (offsets[endIndex].x - endLabel.size.width / 2f).coerceIn(left, size.width - endLabel.size.width)
            drawText(endLabel, topLeft = Offset(x, offsets[endIndex].y - endLabel.size.height - DOT_RADIUS.toPx() * 2))
        }

        selected?.let { index ->
            val point = offsets[index]
            drawLine(colors.outline, Offset(point.x, top), Offset(point.x, top + plotHeight), GRID_WIDTH.toPx())
            drawDot(point, colors.surface)
            drawTooltip(measurer, "${sorted[index].label} · ${formatValue(sorted[index].y)}", point, valueStyle, colors.surfaceVariant)
        }
    }
}

private fun DrawScope.drawDot(center: Offset, surface: Color) {
    drawCircle(surface, radius = (DOT_RADIUS + DOT_RING).toPx(), center = center)
    drawCircle(ControlActive, radius = DOT_RADIUS.toPx(), center = center)
}

private fun DrawScope.drawTooltip(
    measurer: TextMeasurer,
    text: String,
    anchor: Offset,
    style: TextStyle,
    background: Color,
) {
    val layout = measurer.measure(text, style)
    val pad = TOOLTIP_PADDING.toPx()
    val width = layout.size.width + pad * 2
    val height = layout.size.height + pad
    val x = (anchor.x - width / 2).coerceIn(0f, size.width - width)
    val topLeft = Offset(x, 0f)
    drawRoundRect(background, topLeft, Size(width, height), CornerRadius(height / 2))
    drawText(layout, topLeft = Offset(x + pad, pad / 2))
}

private fun nearestIndex(points: List<ChartPoint>, touchX: Float, width: Float, left: Float): Int? {
    if (points.isEmpty()) return null
    if (points.size == 1) return 0
    val xMin = points.first().x
    val fraction = ((touchX - left) / (width - left)).coerceIn(0f, 1f)
    val span = (points.last().x - xMin).takeIf { it > 0 }
        ?: return (fraction * points.lastIndex).roundToInt()
    val target = xMin + span * fraction
    return points.indices.minByOrNull { abs(points[it].x - target) }
}

private fun niceTicks(min: Double, max: Double): List<Double> {
    val low = if (min == max) min - 1 else min
    val high = if (min == max) max + 1 else max
    val rough = (high - low) / (TARGET_TICKS - 1)
    val magnitude = 10.0.pow(floor(log10(rough)))
    val step = listOf(1.0, 2.0, 2.5, 5.0, 10.0).map { it * magnitude }.first { it >= rough }
    val start = floor(low / step) * step
    val end = ceil(high / step) * step
    return generateSequence(start) { it + step }.takeWhile { it <= end + step / 2 }.toList()
}
