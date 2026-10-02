package com.example.data.model

data class MutualFundItem(
    val code: String,
    val name: String,
    val provider: String,
    val nav: Double,
    val changePct: Double,
    val expenseRatioPct: Double,
    val return1YPct: Double,
    val return3YPct: Double,
    val return5YPct: Double,
    val riskLevel: String, // "Conservative", "Moderate", "Aggressive"
    val category: String, // "Index ETF", "Large-Cap Growth", "Digital Assets & Crypto", "Balanced Wealth"
    val dividendYieldPct: Double,
    val description: String,
    val historyNavPoints: List<Double>
)
