package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CandleStick
import com.example.ui.theme.*
import kotlin.math.max
import kotlin.math.min

@Composable
fun SparklineAreaChart(
    dataPoints: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color = BullGreen,
    showGradient: Boolean = true,
    strokeWidth: Float = 4f
) {
    if (dataPoints.size < 2) return

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(dataPoints.firstOrNull()) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(600))
    }

    Canvas(modifier = modifier) {
        val minVal = dataPoints.minOrNull() ?: 0.0
        val maxVal = dataPoints.maxOrNull() ?: 1.0
        val range = max(0.00001, maxVal - minVal)

        val width = size.width
        val height = size.height
        val stepX = width / (dataPoints.size - 1)

        val path = Path()
        val fillPath = Path()

        val points = dataPoints.mapIndexed { index, value ->
            val normY = 1f - ((value - minVal) / range).toFloat()
            val x = index * stepX
            val y = normY * (height * 0.85f) + (height * 0.08f)
            Offset(x, y)
        }

        points.forEachIndexed { i, pt ->
            if (i == 0) {
                path.moveTo(pt.x, pt.y)
                fillPath.moveTo(pt.x, height)
                fillPath.lineTo(pt.x, pt.y)
            } else {
                val prev = points[i - 1]
                val midX = (prev.x + pt.x) / 2
                path.cubicTo(midX, prev.y, midX, pt.y, pt.x, pt.y)
                fillPath.cubicTo(midX, prev.y, midX, pt.y, pt.x, pt.y)
            }
        }

        fillPath.lineTo(width, height)
        fillPath.close()

        if (showGradient) {
            val gradientBrush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.35f),
                    lineColor.copy(alpha = 0.0f)
                ),
                startY = 0f,
                endY = height
            )
            drawPath(fillPath, brush = gradientBrush)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

@Composable
fun CandlestickChart(
    candlesticks: List<CandleStick>,
    modifier: Modifier = Modifier,
    bullColor: Color = BullGreen,
    bearColor: Color = BearRed
) {
    if (candlesticks.isEmpty()) return

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("candlestick_canvas")
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val itemWidth = size.width / candlesticks.size
                        val index = (offset.x / itemWidth).toInt().coerceIn(0, candlesticks.size - 1)
                        selectedIndex = index
                    }
                }
        ) {
            val minLow = candlesticks.minOf { it.low }
            val maxHigh = candlesticks.maxOf { it.high }
            val range = max(0.0001, maxHigh - minLow)

            val width = size.width
            val height = size.height
            val candleCount = candlesticks.size
            val candleSlotWidth = width / candleCount
            val candleBodyWidth = max(4f, candleSlotWidth * 0.65f)

            // Draw horizontal price grid lines
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = height * (i.toFloat() / gridLines)
                drawLine(
                    color = DarkBorder.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }

            candlesticks.forEachIndexed { i, candle ->
                val isBull = candle.close >= candle.open
                val color = if (isBull) bullColor else bearColor

                val centerX = (i * candleSlotWidth) + (candleSlotWidth / 2f)

                val highY = height - (((candle.high - minLow) / range).toFloat() * height)
                val lowY = height - (((candle.low - minLow) / range).toFloat() * height)
                val openY = height - (((candle.open - minLow) / range).toFloat() * height)
                val closeY = height - (((candle.close - minLow) / range).toFloat() * height)

                // Draw Wick
                drawLine(
                    color = color,
                    start = Offset(centerX, highY),
                    end = Offset(centerX, lowY),
                    strokeWidth = 1.5f
                )

                // Draw Candle body
                val bodyTop = min(openY, closeY)
                val bodyHeight = max(3f, kotlin.math.abs(openY - closeY))
                drawRect(
                    color = color,
                    topLeft = Offset(centerX - (candleBodyWidth / 2f), bodyTop),
                    size = Size(candleBodyWidth, bodyHeight)
                )

                // Highlight tapped candle
                if (selectedIndex == i) {
                    drawLine(
                        color = NeonCyan.copy(alpha = 0.7f),
                        start = Offset(centerX, 0f),
                        end = Offset(centerX, height),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                }
            }
        }

        // Tooltip when tapping
        selectedIndex?.let { idx ->
            if (idx in candlesticks.indices) {
                val c = candlesticks[idx]
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("O: \$${String.format("%,.1f", c.open)}", fontSize = 11.sp, color = TextSecondary)
                    Text("H: \$${String.format("%,.1f", c.high)}", fontSize = 11.sp, color = BullGreen)
                    Text("L: \$${String.format("%,.1f", c.low)}", fontSize = 11.sp, color = BearRed)
                    Text("C: \$${String.format("%,.1f", c.close)}", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

data class AllocationSlice(
    val label: String,
    val value: Double,
    val color: Color
)

@Composable
fun DonutAllocationChart(
    slices: List<AllocationSlice>,
    modifier: Modifier = Modifier,
    centerTitle: String = "Total Assets",
    centerSubtitle: String = "\$0.00"
) {
    val total = remember(slices) { slices.sumOf { it.value } }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 32.dp.toPx()
            val diameter = min(size.width, size.height) - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            if (total <= 0.0) {
                drawArc(
                    color = DarkBorder,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )
                return@Canvas
            }

            var currentAngle = -90f
            slices.forEach { slice ->
                val sweep = ((slice.value / total) * 360f).toFloat()
                if (sweep > 0.5f) {
                    drawArc(
                        color = slice.color,
                        startAngle = currentAngle,
                        sweepAngle = sweep - 2f, // spacing gap
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                currentAngle += sweep
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerTitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Text(
                text = centerSubtitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun SentimentGauge(
    score: Int, // 0 - 100
    modifier: Modifier = Modifier
) {
    val animScore = remember { Animatable(0f) }
    LaunchedEffect(score) {
        animScore.animateTo(score.toFloat(), animationSpec = tween(1000))
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 18.dp.toPx()
            val diameter = min(size.width, size.height * 1.8f) - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - (diameter / 2f)))
            val arcSize = Size(diameter, diameter)

            // Background arc (180 degrees from -180 to 0)
            val gradientBrush = Brush.sweepGradient(
                colors = listOf(
                    BearRed,
                    WarningAmber,
                    BullGreen,
                    NeonCyan
                )
            )

            drawArc(
                brush = gradientBrush,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Needle indicator calculation
            val clampedScore = animScore.value.coerceIn(0f, 100f)
            val needleAngle = 180f + (clampedScore / 100f * 180f)
            val rad = Math.toRadians(needleAngle.toDouble())

            val centerX = size.width / 2f
            val centerY = size.height - (strokeWidth / 2f)
            val needleLength = (diameter / 2f) - 10.dp.toPx()

            val endX = (centerX + needleLength * Math.cos(rad)).toFloat()
            val endY = (centerY + needleLength * Math.sin(rad)).toFloat()

            // Draw center pin
            drawCircle(color = TextPrimary, radius = 7.dp.toPx(), center = Offset(centerX, centerY))
            // Draw Needle
            drawLine(
                color = TextPrimary,
                start = Offset(centerX, centerY),
                end = Offset(endX, endY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${animScore.value.toInt()}",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = when {
                    score >= 75 -> NeonCyan
                    score >= 55 -> BullGreen
                    score >= 45 -> WarningAmber
                    else -> BearRed
                }
            )
            Text(
                text = when {
                    score >= 75 -> "Extreme Greed"
                    score >= 55 -> "Greed"
                    score >= 45 -> "Neutral"
                    score >= 25 -> "Fear"
                    else -> "Extreme Fear"
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
        }
    }
}
