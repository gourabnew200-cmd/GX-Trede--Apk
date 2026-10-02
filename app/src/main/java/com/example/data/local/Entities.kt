package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "portfolio_holdings")
data class PortfolioHoldingEntity(
    @PrimaryKey val symbol: String,
    val name: String,
    val quantity: Double,
    val avgBuyPrice: Double,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "trade_transactions")
data class TradeTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val action: String, // "BUY" or "SELL"
    val orderType: String, // "MARKET" or "LIMIT"
    val quantity: Double,
    val pricePerUnit: Double,
    val totalAmountUsd: Double,
    val feeUsd: Double,
    val realizedGainLossUsd: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val taxYear: Int
)

@Entity(tableName = "price_alerts")
data class PriceAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val condition: String, // "ABOVE", "BELOW", "PCT_UP", "PCT_DOWN"
    val targetPrice: Double,
    val initialPrice: Double,
    val isEnabled: Boolean = true,
    val isTriggered: Boolean = false,
    val triggeredAt: Long? = null,
    val note: String = ""
)

@Entity(tableName = "mutual_fund_holdings")
data class MutualFundHoldingEntity(
    @PrimaryKey val fundCode: String,
    val fundName: String,
    val units: Double,
    val avgNav: Double,
    val totalInvested: Double,
    val purchaseTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "wallet_account")
data class WalletAccountEntity(
    @PrimaryKey val id: Int = 1,
    val cashBalance: Double = 25000.0 // Starting simulated fiat capital
)
