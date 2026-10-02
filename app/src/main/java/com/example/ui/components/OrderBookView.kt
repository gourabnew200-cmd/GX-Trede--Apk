package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OrderBook
import com.example.ui.theme.*
import kotlin.math.max

@Composable
fun OrderBookView(
    orderBook: OrderBook,
    modifier: Modifier = Modifier
) {
    val maxAskTotal = orderBook.asks.maxOfOrNull { it.total } ?: 1.0
    val maxBidTotal = orderBook.bids.maxOfOrNull { it.total } ?: 1.0
    val maxOverall = max(maxAskTotal, maxBidTotal)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurfaceVariant, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("order_book_view")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Order Depth", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(
                "Spread: ${String.format("%.2f", orderBook.spread)} (${String.format("%.3f", orderBook.spreadPct)}%)",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Price (USD)", fontSize = 11.sp, color = TextMuted)
            Text("Size", fontSize = 11.sp, color = TextMuted)
            Text("Total Sum", fontSize = 11.sp, color = TextMuted)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Asks (Sellers) - Red
        orderBook.asks.take(4).reversed().forEach { ask ->
            val fillRatio = (ask.total / maxOverall).toFloat().coerceIn(0.05f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
            ) {
                // Depth bar
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fillRatio)
                        .align(Alignment.CenterEnd)
                        .background(BearRed.copy(alpha = 0.18f))
                )
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        String.format("%,.2f", ask.price),
                        color = BearRed,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        String.format("%.4f", ask.amount),
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        String.format("%.3f", ask.total),
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Mid Market Spread Line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(DarkBorder)
                .height(1.dp)
        )

        // Bids (Buyers) - Green
        orderBook.bids.take(4).forEach { bid ->
            val fillRatio = (bid.total / maxOverall).toFloat().coerceIn(0.05f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
            ) {
                // Depth bar
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fillRatio)
                        .align(Alignment.CenterEnd)
                        .background(BullGreen.copy(alpha = 0.18f))
                )
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        String.format("%,.2f", bid.price),
                        color = BullGreen,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        String.format("%.4f", bid.amount),
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        String.format("%.3f", bid.total),
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
