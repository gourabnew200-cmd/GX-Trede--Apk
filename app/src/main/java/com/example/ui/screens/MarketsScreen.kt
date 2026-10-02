package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CryptoMarketItem
import com.example.data.model.TimeFrame
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun MarketsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val markets by viewModel.cryptoMarkets.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filter by viewModel.marketFilter.collectAsState()
    val selectedCoin by viewModel.selectedCoin.collectAsState()

    val filteredList = remember(markets, searchQuery, filter) {
        var list = markets.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.symbol.contains(searchQuery, ignoreCase = true)
        }
        when (filter) {
            "GAINERS" -> list.filter { it.change24h > 0 }.sortedByDescending { it.change24h }
            "LOSERS" -> list.filter { it.change24h < 0 }.sortedBy { it.change24h }
            "VOLUME" -> list.sortedByDescending { it.volume24hUsd }
            else -> list
        }
    }

    Box(modifier = modifier.fillMaxSize().background(DarkBackground)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar & Filter Row
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search crypto (e.g. BTC, Solana)", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_crypto_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "ALL" to "All Assets",
                    "GAINERS" to "🔥 Top Gainers",
                    "LOSERS" to "📉 Top Dips",
                    "VOLUME" to "📊 24h Volume"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = filter == key,
                        onClick = { viewModel.setMarketFilter(key) },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                            selectedLabelColor = NeonCyan,
                            containerColor = DarkSurface
                        ),
                        modifier = Modifier.testTag("filter_$key")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Market List Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Asset", fontSize = 11.sp, color = TextMuted)
                Text("Price / 24h Change", fontSize = 11.sp, color = TextMuted)
            }

            // Markets List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("crypto_market_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredList, key = { it.symbol }) { coin ->
                    CryptoMarketRowItem(
                        coin = coin,
                        onClick = { viewModel.openCoinDetail(coin) },
                        onTradeClick = { viewModel.openTradeSheet(coin) },
                        onAlertClick = { viewModel.openCreateAlertSheet(coin.symbol) }
                    )
                }
            }
        }

        // Coin Detail Modal Sheet
        selectedCoin?.let { coin ->
            CoinDetailModalSheet(
                coin = coin,
                viewModel = viewModel,
                onDismiss = { viewModel.closeCoinDetail() },
                onTrade = {
                    viewModel.closeCoinDetail()
                    viewModel.openTradeSheet(coin)
                },
                onSetAlert = {
                    viewModel.closeCoinDetail()
                    viewModel.openCreateAlertSheet(coin.symbol)
                }
            )
        }
    }
}

@Composable
fun CryptoMarketRowItem(
    coin: CryptoMarketItem,
    onClick: () -> Unit,
    onTradeClick: () -> Unit,
    onAlertClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("market_item_${coin.symbol}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Coin Icon & Symbol
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        coin.symbol.take(3),
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = NeonCyan
                    )
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(coin.symbol, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                        SentimentBadge(coin.sentimentLabel)
                    }
                    Text(coin.name, color = TextSecondary, fontSize = 12.sp)
                }
            }

            // Sparkline
            SparklineAreaChart(
                dataPoints = coin.sparkline7d,
                lineColor = if (coin.change24h >= 0) BullGreen else BearRed,
                showGradient = false,
                strokeWidth = 2.5f,
                modifier = Modifier
                    .width(60.dp)
                    .height(28.dp)
            )

            // Price & 24h Change
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "\$${formatCryptoPrice(coin.currentPrice)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                PriceChangeBadge(changePct = coin.change24h)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinDetailModalSheet(
    coin: CryptoMarketItem,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onTrade: () -> Unit,
    onSetAlert: () -> Unit
) {
    var chartType by remember { mutableStateOf("CANDLE") } // "CANDLE" or "LINE"
    var selectedTimeFrame by remember { mutableStateOf(TimeFrame.ONE_DAY) }
    val orderBook = remember(coin.currentPrice) { viewModel.marketEngine.getOrderBook(coin.currentPrice) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = DarkBorder) },
        modifier = Modifier.testTag("coin_detail_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(coin.symbol.take(3), fontWeight = FontWeight.Black, color = NeonCyan, fontSize = 14.sp)
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(coin.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(coin.symbol, fontSize = 12.sp, color = TextSecondary)
                        }
                        Text(
                            "\$${formatCryptoPrice(coin.currentPrice)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                PriceChangeBadge(changePct = coin.change24h)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Chart View Mode Selector (Candlestick vs Line) & Timeframes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = chartType == "CANDLE",
                        onClick = { chartType = "CANDLE" },
                        label = { Text("Candles", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeonCyan.copy(alpha = 0.2f), selectedLabelColor = NeonCyan)
                    )
                    FilterChip(
                        selected = chartType == "LINE",
                        onClick = { chartType = "LINE" },
                        label = { Text("Line", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.ShowChart, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeonCyan.copy(alpha = 0.2f), selectedLabelColor = NeonCyan)
                    )
                }

                TimeframeSelector(
                    selectedTimeFrame = selectedTimeFrame,
                    onSelect = { selectedTimeFrame = it },
                    modifier = Modifier.width(190.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chart Canvas Container
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    if (chartType == "CANDLE") {
                        CandlestickChart(
                            candlesticks = coin.candlesticks,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        SparklineAreaChart(
                            dataPoints = coin.sparkline7d,
                            lineColor = if (coin.change24h >= 0) BullGreen else BearRed,
                            showGradient = true,
                            strokeWidth = 3f,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Key Market Statistics Grid
            Text("Market Overview & Stats", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox(label = "24h High", value = "\$${formatCryptoPrice(coin.high24h)}", modifier = Modifier.weight(1f))
                    StatBox(label = "24h Low", value = "\$${formatCryptoPrice(coin.low24h)}", modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox(label = "24h Volume", value = "\$${formatCompactUsd(coin.volume24hUsd)}", modifier = Modifier.weight(1f))
                    StatBox(label = "Market Cap", value = "\$${formatCompactUsd(coin.marketCapUsd)}", modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox(label = "Circulating Supply", value = "${formatCompactUsd(coin.circulatingSupply)} ${coin.symbol}", modifier = Modifier.weight(1f))
                    StatBox(label = "All-Time High", value = "\$${formatCryptoPrice(coin.allTimeHigh)}", modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Social Sentiment Highlight
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Social Sentiment Score", fontSize = 12.sp, color = TextSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${coin.sentimentScore}/100", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                            SentimentBadge(coin.sentimentLabel)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Reddit: ${coin.redditSentimentPct}% Bullish", fontSize = 11.sp, color = TextSecondary)
                        Text("Twitter: ${coin.twitterSentimentPct}% Bullish", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Real-Time Order Depth
            OrderBookView(orderBook = orderBook)

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons: Set Alert & Trade Now
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onSetAlert,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("detail_set_alert_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningAmber)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Set Alert", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onTrade,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("detail_trade_now_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBackground)
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Trade ${coin.symbol}", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(label, fontSize = 11.sp, color = TextMuted)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
    }
}

fun formatCryptoPrice(p: Double): String {
    return if (p >= 1000) String.format("%,.2f", p)
    else if (p >= 1) String.format("%.2f", p)
    else String.format("%.4f", p)
}

fun formatCompactUsd(n: Double): String {
    return when {
        n >= 1_000_000_000_000.0 -> String.format("%.2fT", n / 1_000_000_000_000.0)
        n >= 1_000_000_000.0 -> String.format("%.2fB", n / 1_000_000_000.0)
        n >= 1_000_000.0 -> String.format("%.2fM", n / 1_000_000.0)
        n >= 1_000.0 -> String.format("%.2fK", n / 1_000.0)
        else -> String.format("%.2f", n)
    }
}
