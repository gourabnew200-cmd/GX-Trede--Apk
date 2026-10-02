package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.data.model.CryptoNewsItem
import com.example.data.model.SocialChannelStats
import com.example.data.model.TrendingSocialCoin
import com.example.ui.MainViewModel
import com.example.ui.components.SentimentBadge
import com.example.ui.components.SentimentGauge
import com.example.ui.theme.*

@Composable
fun SentimentScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val sentimentData by viewModel.sentimentData.collectAsState()

    Box(modifier = modifier.fillMaxSize().background(DarkBackground)) {
        if (sentimentData == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonCyan)
            }
            return
        }

        val data = sentimentData!!

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("sentiment_lazy_column"),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Fear & Greed Index Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("fear_and_greed_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Market Fear & Greed Index", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Multi-factor sentiment & momentum metric", fontSize = 12.sp, color = TextSecondary)
                            }
                            Box(
                                modifier = Modifier
                                    .background(NeonCyan.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("DAILY ALPHA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Custom Arc Gauge
                        SentimentGauge(
                            score = data.fearAndGreedIndex,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Historical benchmark comparison
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceVariant, RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            BenchmarkColumn("Yesterday", data.yesterdayIndex)
                            BenchmarkColumn("Last Week", data.lastWeekIndex)
                            BenchmarkColumn("Last Month", data.lastMonthIndex)
                        }
                    }
                }
            }

            // Overall Social Sentiment Bar
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Overall Social Mood", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                            Text("${data.overallBullishPct}% Bullish", fontWeight = FontWeight.Bold, color = BullGreen, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Tri-color segmented bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(data.overallBullishPct.toFloat())
                                    .fillMaxHeight()
                                    .background(BullGreen)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(data.overallNeutralPct.toFloat())
                                    .fillMaxHeight()
                                    .background(WarningAmber)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(data.overallBearishPct.toFloat())
                                    .fillMaxHeight()
                                    .background(BearRed)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Bullish: ${data.overallBullishPct}%", fontSize = 11.sp, color = BullGreen)
                            Text("Neutral: ${data.overallNeutralPct}%", fontSize = 11.sp, color = WarningAmber)
                            Text("Bearish: ${data.overallBearishPct}%", fontSize = 11.sp, color = BearRed)
                        }
                    }
                }
            }

            // Social Channels Breakdown Cards (Reddit, X, Telegram)
            item {
                Text(
                    "Social Volume & Community Pulse",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SocialChannelCard(
                        stats = data.redditStats,
                        accentColor = Color(0xFFFF4500),
                        icon = Icons.Default.Forum
                    )
                    SocialChannelCard(
                        stats = data.twitterStats,
                        accentColor = Color(0xFF1DA1F2),
                        icon = Icons.Default.Tag
                    )
                    SocialChannelCard(
                        stats = data.telegramStats,
                        accentColor = Color(0xFF0088CC),
                        icon = Icons.Default.Send
                    )
                }
            }

            // Trending Social Coins Heatmap
            item {
                Text(
                    "Trending Sentiment Leaders",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            items(data.trendingCoins, key = { it.symbol }) { coin ->
                TrendingCoinRow(coin = coin, onClick = {
                    val m = viewModel.cryptoMarkets.value.find { it.symbol == coin.symbol }
                    if (m != null) viewModel.openCoinDetail(m)
                })
            }

            // Curated Real-Time News & Sentiment Feed
            item {
                Text(
                    "Real-Time Sentiment Newsfeed",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            items(data.newsItems, key = { it.id }) { news ->
                NewsSentimentCard(news = news)
            }
        }
    }
}

@Composable
fun BenchmarkColumn(label: String, score: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = TextMuted)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            "$score",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = when {
                score >= 70 -> NeonCyan
                score >= 50 -> BullGreen
                score >= 40 -> WarningAmber
                else -> BearRed
            }
        )
    }
}

@Composable
fun SocialChannelCard(
    stats: SocialChannelStats,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                }

                Column {
                    Text(stats.channelName, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                    Text(stats.totalVolume, fontSize = 11.sp, color = TextSecondary)
                    Text(
                        "${stats.highlightMetric}: ${stats.highlightLabel}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = NeonCyan
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("${stats.sentimentBullishPct}%", fontWeight = FontWeight.Black, fontSize = 18.sp, color = BullGreen)
                Text("Bullish", fontSize = 11.sp, color = TextMuted)
            }
        }
    }
}

@Composable
fun TrendingCoinRow(
    coin: TrendingSocialCoin,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("trending_coin_${coin.symbol}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Text(coin.symbol.take(3), fontWeight = FontWeight.Bold, color = NeonCyan, fontSize = 11.sp)
                }

                Column {
                    Text(coin.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                    Text(coin.mentions24h, fontSize = 11.sp, color = TextSecondary)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${coin.sentimentScore}/100",
                        fontWeight = FontWeight.Bold,
                        color = if (coin.sentimentScore >= 80) NeonCyan else BullGreen,
                        fontSize = 14.sp
                    )
                    Text(coin.changeSentiment24h, fontSize = 11.sp, color = BullGreen, fontWeight = FontWeight.SemiBold)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
            }
        }
    }
}

@Composable
fun NewsSentimentCard(news: CryptoNewsItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(news.source, fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.SemiBold)
                    Text("•", fontSize = 11.sp, color = TextMuted)
                    Text(news.timeAgo, fontSize = 11.sp, color = TextMuted)
                }
                SentimentBadge(news.sentiment)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = news.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                news.affectedCoins.forEach { sym ->
                    Box(
                        modifier = Modifier
                            .background(DarkSurfaceVariant, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(sym, fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
