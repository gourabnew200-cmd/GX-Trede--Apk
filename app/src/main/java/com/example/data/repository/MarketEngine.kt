package com.example.data.repository

import com.example.data.local.PriceAlertEntity
import com.example.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

data class TriggeredAlertEvent(
    val alert: PriceAlertEntity,
    val currentPrice: Double,
    val message: String
)

class MarketEngine {

    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _cryptoMarkets = MutableStateFlow<List<CryptoMarketItem>>(emptyList())
    val cryptoMarkets: StateFlow<List<CryptoMarketItem>> = _cryptoMarkets.asStateFlow()

    private val _mutualFunds = MutableStateFlow<List<MutualFundItem>>(emptyList())
    val mutualFunds: StateFlow<List<MutualFundItem>> = _mutualFunds.asStateFlow()

    private val _sentimentData = MutableStateFlow<SocialSentimentData?>(null)
    val sentimentData: StateFlow<SocialSentimentData?> = _sentimentData.asStateFlow()

    private val _alertTriggerEvents = MutableSharedFlow<TriggeredAlertEvent>(extraBufferCapacity = 64)
    val alertTriggerEvents = _alertTriggerEvents.asSharedFlow()

    private var activeAlertsProvider: (suspend () -> List<PriceAlertEntity>)? = null
    private var onAlertTriggeredCallback: (suspend (PriceAlertEntity, Double) -> Unit)? = null

    init {
        initializeInitialData()
        startLiveMarketTick()
    }

    fun setAlertHandlers(
        provider: suspend () -> List<PriceAlertEntity>,
        onTriggered: suspend (PriceAlertEntity, Double) -> Unit
    ) {
        this.activeAlertsProvider = provider
        this.onAlertTriggeredCallback = onTriggered
    }

    private fun initializeInitialData() {
        val now = System.currentTimeMillis()
        val initialCryptos = listOf(
            createCryptoItem(
                symbol = "BTC",
                name = "Bitcoin",
                basePrice = 67840.50,
                change24h = 3.42,
                volume = 38450120400.0,
                marketCap = 1335800000000.0,
                circulating = 19760000.0,
                ath = 73750.07,
                sentimentScore = 84,
                sentimentLabel = "Strong Bullish",
                mentions = 148200,
                redditPct = 86,
                twitterPct = 82,
                volatility = 0.003
            ),
            createCryptoItem(
                symbol = "ETH",
                name = "Ethereum",
                basePrice = 3520.80,
                change24h = 2.15,
                volume = 19800450000.0,
                marketCap = 423500000000.0,
                circulating = 120250000.0,
                ath = 4891.70,
                sentimentScore = 78,
                sentimentLabel = "Bullish",
                mentions = 98400,
                redditPct = 79,
                twitterPct = 76,
                volatility = 0.004
            ),
            createCryptoItem(
                symbol = "SOL",
                name = "Solana",
                basePrice = 186.45,
                change24h = 6.84,
                volume = 7840250000.0,
                marketCap = 87400000000.0,
                circulating = 468900000.0,
                ath = 260.06,
                sentimentScore = 91,
                sentimentLabel = "Extreme Bullish",
                mentions = 112000,
                redditPct = 88,
                twitterPct = 92,
                volatility = 0.007
            ),
            createCryptoItem(
                symbol = "BNB",
                name = "BNB",
                basePrice = 584.20,
                change24h = 1.08,
                volume = 1420000000.0,
                marketCap = 85200000000.0,
                circulating = 145880000.0,
                ath = 720.67,
                sentimentScore = 69,
                sentimentLabel = "Neutral",
                mentions = 32000,
                redditPct = 68,
                twitterPct = 71,
                volatility = 0.003
            ),
            createCryptoItem(
                symbol = "XRP",
                name = "XRP",
                basePrice = 0.584,
                change24h = -0.75,
                volume = 2100000000.0,
                marketCap = 32800000000.0,
                circulating = 56200000000.0,
                ath = 3.84,
                sentimentScore = 62,
                sentimentLabel = "Neutral",
                mentions = 45000,
                redditPct = 60,
                twitterPct = 65,
                volatility = 0.005
            ),
            createCryptoItem(
                symbol = "AVAX",
                name = "Avalanche",
                basePrice = 32.18,
                change24h = 4.30,
                volume = 890000000.0,
                marketCap = 13100000000.0,
                circulating = 407000000.0,
                ath = 146.22,
                sentimentScore = 75,
                sentimentLabel = "Bullish",
                mentions = 28400,
                redditPct = 74,
                twitterPct = 77,
                volatility = 0.006
            ),
            createCryptoItem(
                symbol = "DOGE",
                name = "Dogecoin",
                basePrice = 0.142,
                change24h = 5.12,
                volume = 1680000000.0,
                marketCap = 20700000000.0,
                circulating = 146000000000.0,
                ath = 0.737,
                sentimentScore = 80,
                sentimentLabel = "Bullish",
                mentions = 84000,
                redditPct = 81,
                twitterPct = 84,
                volatility = 0.008
            ),
            createCryptoItem(
                symbol = "LINK",
                name = "Chainlink",
                basePrice = 14.85,
                change24h = 3.10,
                volume = 540000000.0,
                marketCap = 8900000000.0,
                circulating = 608000000.0,
                ath = 52.88,
                sentimentScore = 82,
                sentimentLabel = "Bullish",
                mentions = 31000,
                redditPct = 85,
                twitterPct = 80,
                volatility = 0.005
            ),
            createCryptoItem(
                symbol = "SUI",
                name = "Sui Network",
                basePrice = 2.18,
                change24h = 9.40,
                volume = 980000000.0,
                marketCap = 6100000000.0,
                circulating = 2800000000.0,
                ath = 2.36,
                sentimentScore = 89,
                sentimentLabel = "Strong Bullish",
                mentions = 52000,
                redditPct = 87,
                twitterPct = 90,
                volatility = 0.009
            ),
            createCryptoItem(
                symbol = "NEAR",
                name = "NEAR Protocol",
                basePrice = 5.42,
                change24h = 1.95,
                volume = 430000000.0,
                marketCap = 6500000000.0,
                circulating = 1200000000.0,
                ath = 20.42,
                sentimentScore = 71,
                sentimentLabel = "Bullish",
                mentions = 21000,
                redditPct = 73,
                twitterPct = 70,
                volatility = 0.006
            )
        )
        _cryptoMarkets.value = initialCryptos

        // Initial Mutual Funds
        val initialFunds = listOf(
            MutualFundItem(
                code = "VFIAX",
                name = "Vanguard 500 Index Fund Admiral",
                provider = "Vanguard Group",
                nav = 514.82,
                changePct = 0.42,
                expenseRatioPct = 0.04,
                return1YPct = 26.8,
                return3YPct = 34.2,
                return5YPct = 88.4,
                riskLevel = "Moderate",
                category = "Large Cap Blend Index",
                dividendYieldPct = 1.32,
                description = "Tracks the S&P 500 index representing 500 of the largest U.S. companies. Extremely low fee long-term pillar.",
                historyNavPoints = listOf(480.0, 485.2, 492.0, 489.5, 498.4, 506.1, 514.82)
            ),
            MutualFundItem(
                code = "FSKAX",
                name = "Fidelity Total Market Index Fund",
                provider = "Fidelity Investments",
                nav = 149.65,
                changePct = 0.38,
                expenseRatioPct = 0.015,
                return1YPct = 25.4,
                return3YPct = 31.8,
                return5YPct = 82.5,
                riskLevel = "Moderate",
                category = "Total Stock Market",
                dividendYieldPct = 1.38,
                description = "Seeks to provide investment results corresponding to the total market return of all investable US equities.",
                historyNavPoints = listOf(138.0, 140.5, 142.1, 144.8, 147.2, 148.9, 149.65)
            ),
            MutualFundItem(
                code = "MALOX",
                name = "BlackRock Global Allocation Fund",
                provider = "BlackRock",
                nav = 68.74,
                changePct = -0.15,
                expenseRatioPct = 0.78,
                return1YPct = 14.8,
                return3YPct = 19.5,
                return5YPct = 48.2,
                riskLevel = "Moderate",
                category = "Global Multi-Asset Allocation",
                dividendYieldPct = 2.10,
                description = "Invests globally in stocks, bonds, and cash equivalents to preserve capital while maximizing total return.",
                historyNavPoints = listOf(64.0, 65.2, 66.8, 66.1, 67.5, 68.9, 68.74)
            ),
            MutualFundItem(
                code = "VGT",
                name = "Vanguard Information Tech ETF",
                provider = "Vanguard Group",
                nav = 598.30,
                changePct = 1.12,
                expenseRatioPct = 0.10,
                return1YPct = 35.6,
                return3YPct = 48.2,
                return5YPct = 142.6,
                riskLevel = "Aggressive",
                category = "Technology Sector",
                dividendYieldPct = 0.68,
                description = "Focuses on leading technology, cloud, AI, and semiconductor giants providing high growth opportunities.",
                historyNavPoints = listOf(540.0, 552.0, 568.4, 574.0, 582.5, 591.0, 598.30)
            ),
            MutualFundItem(
                code = "SWBGX",
                name = "Schwab Balanced Growth Fund",
                provider = "Charles Schwab",
                nav = 39.12,
                changePct = 0.22,
                expenseRatioPct = 0.52,
                return1YPct = 16.4,
                return3YPct = 22.0,
                return5YPct = 56.4,
                riskLevel = "Conservative-Moderate",
                category = "Balanced Wealth & Income",
                dividendYieldPct = 2.45,
                description = "A diversified 60/40 blend of quality equities and investment-grade fixed income bonds for low volatility.",
                historyNavPoints = listOf(36.5, 37.1, 37.8, 38.0, 38.6, 38.9, 39.12)
            ),
            MutualFundItem(
                code = "BTC-TRST",
                name = "Digital Asset & Crypto Growth Trust",
                provider = "Apex Digital Wealth",
                nav = 44.50,
                changePct = 3.85,
                expenseRatioPct = 0.65,
                return1YPct = 68.2,
                return3YPct = 112.4,
                return5YPct = 210.5,
                riskLevel = "Aggressive",
                category = "Digital Assets & Web3",
                dividendYieldPct = 0.0,
                description = "Regulated diversified fund holding spot crypto (BTC, ETH, SOL) and top institutional digital infrastructure equities.",
                historyNavPoints = listOf(32.0, 35.5, 38.2, 36.9, 41.2, 42.8, 44.50)
            )
        )
        _mutualFunds.value = initialFunds

        // Initial Social Sentiment Data
        _sentimentData.value = SocialSentimentData(
            fearAndGreedIndex = 76,
            fearAndGreedStatus = "Extreme Greed",
            yesterdayIndex = 72,
            lastWeekIndex = 65,
            lastMonthIndex = 54,
            overallBullishPct = 74,
            overallBearishPct = 16,
            overallNeutralPct = 10,
            redditStats = SocialChannelStats(
                channelName = "Reddit r/CryptoCurrency",
                totalVolume = "142.8K posts & comments",
                sentimentBullishPct = 78,
                highlightMetric = "#1 Topic",
                highlightLabel = "Bitcoin Institutional Inflows"
            ),
            twitterStats = SocialChannelStats(
                channelName = "X / Crypto Twitter",
                totalVolume = "894.2K tweets / 24h",
                sentimentBullishPct = 81,
                highlightMetric = "Viral Trend",
                highlightLabel = "#SolanaBreakout & #BTC100k"
            ),
            telegramStats = SocialChannelStats(
                channelName = "Telegram Alpha Groups",
                totalVolume = "4.9K active channels",
                sentimentBullishPct = 75,
                highlightMetric = "Message Velocity",
                highlightLabel = "+34% surge in trading volume"
            ),
            trendingCoins = listOf(
                TrendingSocialCoin("SOL", "Solana", 91, "112K mentions", "+14.2% Bullish", true),
                TrendingSocialCoin("BTC", "Bitcoin", 84, "148K mentions", "+8.5% Bullish", true),
                TrendingSocialCoin("SUI", "Sui Network", 89, "52K mentions", "+22.4% Bullish", true),
                TrendingSocialCoin("DOGE", "Dogecoin", 80, "84K mentions", "+11.0% Bullish", true),
                TrendingSocialCoin("ETH", "Ethereum", 78, "98K mentions", "+4.2% Bullish", true)
            ),
            newsItems = listOf(
                CryptoNewsItem(
                    id = "news_1",
                    title = "Spot Bitcoin ETF net weekly inflows surpass record \$1.8 Billion amid institutional rally",
                    source = "Bloomberg Terminal",
                    timeAgo = "18m ago",
                    sentiment = "BULLISH",
                    sentimentScore = 92,
                    affectedCoins = listOf("BTC", "ETH")
                ),
                CryptoNewsItem(
                    id = "news_2",
                    title = "Solana ecosystem DEX daily volume flips main competitor on low fee liquidity migration",
                    source = "CoinDesk Insights",
                    timeAgo = "45m ago",
                    sentiment = "BULLISH",
                    sentimentScore = 88,
                    affectedCoins = listOf("SOL")
                ),
                CryptoNewsItem(
                    id = "news_3",
                    title = "Federal Reserve signals potential rate cut cycle favorable for risk-on investment assets",
                    source = "Reuters Finance",
                    timeAgo = "2h ago",
                    sentiment = "BULLISH",
                    sentimentScore = 82,
                    affectedCoins = listOf("BTC", "ETH", "SOL", "VFIAX")
                ),
                CryptoNewsItem(
                    id = "news_4",
                    title = "Regulatory clarity bill moves forward in senate committee with bipartisan backing",
                    source = "The Block",
                    timeAgo = "4h ago",
                    sentiment = "BULLISH",
                    sentimentScore = 79,
                    affectedCoins = listOf("BTC", "XRP", "ETH")
                ),
                CryptoNewsItem(
                    id = "news_5",
                    title = "Short-term profit taking observed in derivative funding rates near key resistance zone",
                    source = "Glassnode Analytics",
                    timeAgo = "5h ago",
                    sentiment = "NEUTRAL",
                    sentimentScore = 52,
                    affectedCoins = listOf("BTC", "ETH")
                )
            )
        )
    }

    private fun createCryptoItem(
        symbol: String,
        name: String,
        basePrice: Double,
        change24h: Double,
        volume: Double,
        marketCap: Double,
        circulating: Double,
        ath: Double,
        sentimentScore: Int,
        sentimentLabel: String,
        mentions: Int,
        redditPct: Int,
        twitterPct: Int,
        volatility: Double
    ): CryptoMarketItem {
        val sparkline = mutableListOf<Double>()
        var curr = basePrice * (1.0 - (change24h / 100.0))
        for (i in 0..20) {
            val delta = curr * (Random.nextDouble(-volatility, volatility * 1.2))
            curr = max(curr + delta, basePrice * 0.7)
            sparkline.add(curr)
        }
        sparkline.add(basePrice)

        val candlesticks = generateCandlesticks(basePrice, volatility)

        return CryptoMarketItem(
            symbol = symbol,
            name = name,
            currentPrice = basePrice,
            change24h = change24h,
            change24hAmount = basePrice * (change24h / 100.0),
            high24h = basePrice * (1.0 + abs(change24h / 100.0) * 0.6),
            low24h = basePrice * (1.0 - abs(change24h / 100.0) * 0.6),
            volume24hUsd = volume,
            marketCapUsd = marketCap,
            circulatingSupply = circulating,
            allTimeHigh = ath,
            sparkline7d = sparkline,
            candlesticks = candlesticks,
            sentimentScore = sentimentScore,
            sentimentLabel = sentimentLabel,
            socialMentions24h = mentions,
            redditSentimentPct = redditPct,
            twitterSentimentPct = twitterPct
        )
    }

    private fun generateCandlesticks(currentPrice: Double, volatility: Double): List<CandleStick> {
        val list = mutableListOf<CandleStick>()
        var lastClose = currentPrice * 0.94
        val now = System.currentTimeMillis()
        val interval = 3600000L // 1 hour per candle for 24 candles

        for (i in 24 downTo 1) {
            val time = now - (i * interval)
            val open = lastClose
            val move = open * Random.nextDouble(-volatility * 1.5, volatility * 1.7)
            val close = open + move
            val high = max(open, close) + (open * Random.nextDouble(0.001, volatility))
            val low = min(open, close) - (open * Random.nextDouble(0.001, volatility))
            list.add(CandleStick(time, open, high, low, close))
            lastClose = close
        }
        return list
    }

    fun getOrderBook(price: Double): OrderBook {
        val bids = mutableListOf<OrderBookEntry>()
        val asks = mutableListOf<OrderBookEntry>()

        var bidTotal = 0.0
        for (i in 1..7) {
            val p = price * (1.0 - (0.0008 * i) - (Random.nextDouble(0.0001, 0.0004)))
            val amt = Random.nextDouble(0.15, 2.5) * (1000.0 / max(price, 1.0))
            bidTotal += amt
            bids.add(OrderBookEntry(p, amt, bidTotal))
        }

        var askTotal = 0.0
        for (i in 1..7) {
            val p = price * (1.0 + (0.0008 * i) + (Random.nextDouble(0.0001, 0.0004)))
            val amt = Random.nextDouble(0.15, 2.5) * (1000.0 / max(price, 1.0))
            askTotal += amt
            asks.add(OrderBookEntry(p, amt, askTotal))
        }

        val spread = asks.first().price - bids.first().price
        val spreadPct = (spread / price) * 100.0
        return OrderBook(bids, asks, spread, spreadPct)
    }

    private fun startLiveMarketTick() {
        engineScope.launch {
            while (isActive) {
                delay(3000) // Live price tick every 3 seconds
                tickMarketPrices()
            }
        }
    }

    private suspend fun tickMarketPrices() {
        val updated = _cryptoMarkets.value.map { item ->
            // Subtle micro fluctuation
            val pctDelta = Random.nextDouble(-0.0035, 0.0042)
            val newPrice = max(0.000001, item.currentPrice * (1.0 + pctDelta))
            val newChange = item.change24h + (pctDelta * 50)
            val newHigh = max(item.high24h, newPrice)
            val newLow = min(item.low24h, newPrice)

            // Update sparkline
            val newSpark = item.sparkline7d.toMutableList()
            if (newSpark.size > 25) {
                newSpark.removeAt(0)
            }
            newSpark.add(newPrice)

            item.copy(
                currentPrice = newPrice,
                change24h = ((newChange * 100).roundToInt()) / 100.0,
                change24hAmount = newPrice - (newPrice / (1.0 + (newChange / 100.0))),
                high24h = newHigh,
                low24h = newLow,
                sparkline7d = newSpark
            )
        }
        _cryptoMarkets.value = updated

        // Check active alerts against new prices
        checkAlerts(updated)
    }

    private suspend fun checkAlerts(currentMarkets: List<CryptoMarketItem>) {
        val provider = activeAlertsProvider ?: return
        val alerts = provider()
        if (alerts.isEmpty()) return

        val priceMap = currentMarkets.associateBy({ it.symbol }, { it.currentPrice })

        for (alert in alerts) {
            val currPrice = priceMap[alert.symbol] ?: continue
            var triggered = false
            var message = ""

            when (alert.condition) {
                "ABOVE" -> {
                    if (currPrice >= alert.targetPrice) {
                        triggered = true
                        message = "${alert.symbol} surged above target \$${formatPrice(alert.targetPrice)}! Current: \$${formatPrice(currPrice)}"
                    }
                }
                "BELOW" -> {
                    if (currPrice <= alert.targetPrice) {
                        triggered = true
                        message = "${alert.symbol} dipped below target \$${formatPrice(alert.targetPrice)}! Current: \$${formatPrice(currPrice)}"
                    }
                }
                "PCT_UP" -> {
                    val gainPct = ((currPrice - alert.initialPrice) / alert.initialPrice) * 100.0
                    if (gainPct >= alert.targetPrice) {
                        triggered = true
                        message = "${alert.symbol} climbed +${String.format("%.1f", gainPct)}%! Current: \$${formatPrice(currPrice)}"
                    }
                }
                "PCT_DOWN" -> {
                    val dropPct = ((alert.initialPrice - currPrice) / alert.initialPrice) * 100.0
                    if (dropPct >= alert.targetPrice) {
                        triggered = true
                        message = "${alert.symbol} fell -${String.format("%.1f", dropPct)}%! Current: \$${formatPrice(currPrice)}"
                    }
                }
            }

            if (triggered) {
                onAlertTriggeredCallback?.invoke(alert, currPrice)
                _alertTriggerEvents.emit(
                    TriggeredAlertEvent(
                        alert = alert,
                        currentPrice = currPrice,
                        message = message
                    )
                )
            }
        }
    }

    private fun formatPrice(p: Double): String {
        return if (p >= 1000) String.format("%,.2f", p)
        else if (p >= 1) String.format("%.2f", p)
        else String.format("%.4f", p)
    }
}
