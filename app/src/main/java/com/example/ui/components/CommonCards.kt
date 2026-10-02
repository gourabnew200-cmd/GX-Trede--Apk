package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TimeFrame
import com.example.ui.theme.*

@Composable
fun LiveIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Row(
        modifier = modifier
            .background(DarkSurfaceElevated, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(BullGreen.copy(alpha = alpha))
        )
        Text("LIVE TICKER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BullGreen)
    }
}

@Composable
fun PriceChangeBadge(
    changePct: Double,
    modifier: Modifier = Modifier,
    prefix: String = ""
) {
    val isPositive = changePct >= 0
    val color = if (isPositive) BullGreen else BearRed
    val bgColor = if (isPositive) BullGreen.copy(alpha = 0.15f) else BearRed.copy(alpha = 0.15f)

    Row(
        modifier = modifier
            .background(bgColor, shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = "$prefix${String.format("%.2f", kotlin.math.abs(changePct))}%",
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SentimentBadge(
    sentiment: String,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (sentiment.uppercase()) {
        "STRONG BULLISH", "EXTREME BULLISH", "BULLISH" -> BullGreen to "Bullish"
        "BEARISH", "STRONG BEARISH" -> BearRed to "Bearish"
        else -> WarningAmber to "Neutral"
    }

    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
            .border(1.dp, color.copy(alpha = 0.35f), shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TimeframeSelector(
    selectedTimeFrame: TimeFrame,
    onSelect: (TimeFrame) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurfaceVariant, shape = RoundedCornerShape(8.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TimeFrame.values().forEach { tf ->
            val isSelected = tf == selectedTimeFrame
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) DarkSurfaceElevated else Color.Transparent)
                    .clickable { onSelect(tf) }
                    .padding(vertical = 6.dp)
                    .testTag("tf_${tf.label}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tf.label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) NeonCyan else TextSecondary
                )
            }
        }
    }
}
