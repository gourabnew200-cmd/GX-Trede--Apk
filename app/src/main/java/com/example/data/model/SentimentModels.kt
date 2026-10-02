package com.example.data.model

data class SocialChannelStats(
    val channelName: String,
    val totalVolume: String,
    val sentimentBullishPct: Int,
    val highlightMetric: String,
    val highlightLabel: String
)

data class TrendingSocialCoin(
    val symbol: String,
    val name: String,
    val sentimentScore: Int,
    val mentions24h: String,
    val changeSentiment24h: String,
    val isPositive: Boolean
)

data class CryptoNewsItem(
    val id: String,
    val title: String,
    val source: String,
    val timeAgo: String,
    val sentiment: String, // "BULLISH", "BEARISH", "NEUTRAL"
    val sentimentScore: Int,
    val affectedCoins: List<String>
)

data class SocialSentimentData(
    val fearAndGreedIndex: Int, // 0 - 100
    val fearAndGreedStatus: String, // "Extreme Greed", "Greed", "Neutral", "Fear", "Extreme Fear"
    val yesterdayIndex: Int,
    val lastWeekIndex: Int,
    val lastMonthIndex: Int,
    val overallBullishPct: Int,
    val overallBearishPct: Int,
    val overallNeutralPct: Int,
    val redditStats: SocialChannelStats,
    val twitterStats: SocialChannelStats,
    val telegramStats: SocialChannelStats,
    val trendingCoins: List<TrendingSocialCoin>,
    val newsItems: List<CryptoNewsItem>
)
