package com.example.data.model

data class CandleStick(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double
)

data class OrderBookEntry(
    val price: Double,
    val amount: Double,
    val total: Double
)

data class OrderBook(
    val bids: List<OrderBookEntry>,
    val asks: List<OrderBookEntry>,
    val spread: Double,
    val spreadPct: Double
)

enum class TimeFrame(val label: String) {
    ONE_HOUR("1H"),
    ONE_DAY("24H"),
    ONE_WEEK("1W"),
    ONE_MONTH("1M"),
    ONE_YEAR("1Y"),
    ALL("ALL")
}

data class CryptoMarketItem(
    val symbol: String,
    val name: String,
    val currentPrice: Double,
    val change24h: Double,
    val change24hAmount: Double,
    val high24h: Double,
    val low24h: Double,
    val volume24hUsd: Double,
    val marketCapUsd: Double,
    val circulatingSupply: Double,
    val allTimeHigh: Double,
    val sparkline7d: List<Double>,
    val candlesticks: List<CandleStick>,
    val sentimentScore: Int, // 0 - 100
    val sentimentLabel: String,
    val socialMentions24h: Int,
    val redditSentimentPct: Int,
    val twitterSentimentPct: Int
)

enum class OrderType {
    MARKET,
    LIMIT
}

enum class TradeAction {
    BUY,
    SELL
}
