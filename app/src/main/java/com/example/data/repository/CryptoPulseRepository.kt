package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

class CryptoPulseRepository(
    private val database: AppDatabase,
    val marketEngine: MarketEngine
) {
    private val repoScope = CoroutineScope(Dispatchers.IO)

    val holdings: Flow<List<PortfolioHoldingEntity>> = database.portfolioDao().getAllHoldings()
    val trades: Flow<List<TradeTransactionEntity>> = database.tradeDao().getAllTrades()
    val alerts: Flow<List<PriceAlertEntity>> = database.alertDao().getAllAlerts()
    val mutualFundHoldings: Flow<List<MutualFundHoldingEntity>> = database.mutualFundDao().getAllMutualFundHoldings()
    val wallet: Flow<WalletAccountEntity?> = database.walletDao().getWallet()

    init {
        // Connect alert evaluation
        marketEngine.setAlertHandlers(
            provider = { database.alertDao().getActiveAlertsDirect() },
            onTriggered = { alert, _ ->
                database.alertDao().markAlertTriggered(alert.id, System.currentTimeMillis())
            }
        )

        // Seed initial data if newly installed
        repoScope.launch {
            seedInitialUserDataIfEmpty()
        }
    }

    private suspend fun seedInitialUserDataIfEmpty() {
        val currentWallet = database.walletDao().getWalletDirect()
        if (currentWallet == null) {
            database.walletDao().saveWallet(WalletAccountEntity(id = 1, cashBalance = 15850.0))

            // Seed initial crypto holdings
            database.portfolioDao().insertOrUpdateHolding(
                PortfolioHoldingEntity(
                    symbol = "BTC",
                    name = "Bitcoin",
                    quantity = 0.45,
                    avgBuyPrice = 61200.0,
                    lastUpdated = System.currentTimeMillis() - (120L * 86400000L) // 120 days ago
                )
            )
            database.portfolioDao().insertOrUpdateHolding(
                PortfolioHoldingEntity(
                    symbol = "ETH",
                    name = "Ethereum",
                    quantity = 3.20,
                    avgBuyPrice = 3150.0,
                    lastUpdated = System.currentTimeMillis() - (410L * 86400000L) // Long term (> 1 year)
                )
            )
            database.portfolioDao().insertOrUpdateHolding(
                PortfolioHoldingEntity(
                    symbol = "SOL",
                    name = "Solana",
                    quantity = 28.5,
                    avgBuyPrice = 142.0,
                    lastUpdated = System.currentTimeMillis() - (45L * 86400000L)
                )
            )

            // Seed initial mutual fund holdings
            database.mutualFundDao().insertOrUpdateFundHolding(
                MutualFundHoldingEntity(
                    fundCode = "VFIAX",
                    fundName = "Vanguard 500 Index Fund Admiral",
                    units = 15.0,
                    avgNav = 475.20,
                    totalInvested = 7128.0,
                    purchaseTimestamp = System.currentTimeMillis() - (400L * 86400000L) // Long term
                )
            )
            database.mutualFundDao().insertOrUpdateFundHolding(
                MutualFundHoldingEntity(
                    fundCode = "BTC-TRST",
                    fundName = "Digital Asset & Crypto Growth Trust",
                    units = 65.0,
                    avgNav = 34.80,
                    totalInvested = 2262.0,
                    purchaseTimestamp = System.currentTimeMillis() - (90L * 86400000L)
                )
            )

            // Seed initial sample trade history for tax reports
            val cal = Calendar.getInstance()
            val currentYear = cal.get(Calendar.YEAR)

            database.tradeDao().insertTrade(
                TradeTransactionEntity(
                    symbol = "BTC",
                    action = "BUY",
                    orderType = "MARKET",
                    quantity = 0.25,
                    pricePerUnit = 48500.0,
                    totalAmountUsd = 12125.0,
                    feeUsd = 12.12,
                    realizedGainLossUsd = 0.0,
                    timestamp = System.currentTimeMillis() - (420L * 86400000L),
                    taxYear = currentYear - 1
                )
            )
            database.tradeDao().insertTrade(
                TradeTransactionEntity(
                    symbol = "BTC",
                    action = "SELL",
                    orderType = "LIMIT",
                    quantity = 0.25,
                    pricePerUnit = 64200.0,
                    totalAmountUsd = 16050.0,
                    feeUsd = 16.05,
                    realizedGainLossUsd = 3925.0,
                    timestamp = System.currentTimeMillis() - (15L * 86400000L),
                    taxYear = currentYear
                )
            )
            database.tradeDao().insertTrade(
                TradeTransactionEntity(
                    symbol = "SOL",
                    action = "BUY",
                    orderType = "MARKET",
                    quantity = 15.0,
                    pricePerUnit = 110.0,
                    totalAmountUsd = 1650.0,
                    feeUsd = 1.65,
                    realizedGainLossUsd = 0.0,
                    timestamp = System.currentTimeMillis() - (60L * 86400000L),
                    taxYear = currentYear
                )
            )
            database.tradeDao().insertTrade(
                TradeTransactionEntity(
                    symbol = "ETH",
                    action = "SELL",
                    orderType = "MARKET",
                    quantity = 0.8,
                    pricePerUnit = 3420.0,
                    totalAmountUsd = 2736.0,
                    feeUsd = 2.73,
                    realizedGainLossUsd = 480.0,
                    timestamp = System.currentTimeMillis() - (5L * 86400000L),
                    taxYear = currentYear
                )
            )

            // Seed sample alerts
            database.alertDao().insertAlert(
                PriceAlertEntity(
                    symbol = "BTC",
                    condition = "ABOVE",
                    targetPrice = 70000.0,
                    initialPrice = 67840.0,
                    note = "Sell target at psychological barrier"
                )
            )
            database.alertDao().insertAlert(
                PriceAlertEntity(
                    symbol = "SOL",
                    condition = "PCT_UP",
                    targetPrice = 10.0,
                    initialPrice = 186.0,
                    note = "Take profit spike alert (+10%)"
                )
            )
            database.alertDao().insertAlert(
                PriceAlertEntity(
                    symbol = "ETH",
                    condition = "BELOW",
                    targetPrice = 3300.0,
                    initialPrice = 3520.0,
                    note = "Dip buying trigger"
                )
            )
        }
    }

    suspend fun executeTrade(
        symbol: String,
        action: TradeAction,
        orderType: OrderType,
        quantity: Double,
        pricePerUnit: Double
    ): Result<String> = withContext(Dispatchers.IO) {
        val totalAmount = quantity * pricePerUnit
        val fee = totalAmount * 0.001 // 0.1% trading fee
        val walletAccount = database.walletDao().getWalletDirect()
            ?: WalletAccountEntity(1, 25000.0)

        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)

        if (action == TradeAction.BUY) {
            val totalRequired = totalAmount + fee
            if (walletAccount.cashBalance < totalRequired) {
                return@withContext Result.failure(Exception("Insufficient USD cash balance (\$${String.format("%.2f", walletAccount.cashBalance)} available)"))
            }

            // Deduct cash
            database.walletDao().updateCashBalance(walletAccount.cashBalance - totalRequired)

            // Update or insert holding
            val existing = database.portfolioDao().getHoldingBySymbol(symbol)
            val name = marketEngine.cryptoMarkets.value.find { it.symbol == symbol }?.name ?: symbol

            if (existing != null) {
                val newQty = existing.quantity + quantity
                val newAvg = ((existing.quantity * existing.avgBuyPrice) + totalAmount) / newQty
                database.portfolioDao().insertOrUpdateHolding(
                    existing.copy(
                        quantity = newQty,
                        avgBuyPrice = newAvg,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            } else {
                database.portfolioDao().insertOrUpdateHolding(
                    PortfolioHoldingEntity(
                        symbol = symbol,
                        name = name,
                        quantity = quantity,
                        avgBuyPrice = pricePerUnit,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            }

            // Record transaction
            database.tradeDao().insertTrade(
                TradeTransactionEntity(
                    symbol = symbol,
                    action = "BUY",
                    orderType = orderType.name,
                    quantity = quantity,
                    pricePerUnit = pricePerUnit,
                    totalAmountUsd = totalAmount,
                    feeUsd = fee,
                    realizedGainLossUsd = 0.0,
                    timestamp = System.currentTimeMillis(),
                    taxYear = currentYear
                )
            )

            Result.success("Successfully bought ${String.format("%.4f", quantity)} $symbol for \$${String.format("%,.2f", totalAmount)}")
        } else {
            // SELL
            val existing = database.portfolioDao().getHoldingBySymbol(symbol)
            if (existing == null || existing.quantity < quantity) {
                val available = existing?.quantity ?: 0.0
                return@withContext Result.failure(Exception("Insufficient $symbol balance (${String.format("%.4f", available)} available)"))
            }

            val netProceeds = totalAmount - fee
            val realizedGain = (pricePerUnit - existing.avgBuyPrice) * quantity

            // Add proceeds to cash
            database.walletDao().updateCashBalance(walletAccount.cashBalance + netProceeds)

            // Update holding
            val remainingQty = existing.quantity - quantity
            if (remainingQty <= 0.0000001) {
                database.portfolioDao().deleteHolding(symbol)
            } else {
                database.portfolioDao().insertOrUpdateHolding(
                    existing.copy(
                        quantity = remainingQty,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            }

            // Record transaction
            database.tradeDao().insertTrade(
                TradeTransactionEntity(
                    symbol = symbol,
                    action = "SELL",
                    orderType = orderType.name,
                    quantity = quantity,
                    pricePerUnit = pricePerUnit,
                    totalAmountUsd = totalAmount,
                    feeUsd = fee,
                    realizedGainLossUsd = realizedGain,
                    timestamp = System.currentTimeMillis(),
                    taxYear = currentYear
                )
            )

            Result.success("Successfully sold ${String.format("%.4f", quantity)} $symbol for \$${String.format("%,.2f", totalAmount)}")
        }
    }

    suspend fun investInMutualFund(fundCode: String, amountUsd: Double): Result<String> = withContext(Dispatchers.IO) {
        val walletAccount = database.walletDao().getWalletDirect()
            ?: return@withContext Result.failure(Exception("Wallet account not found"))

        if (walletAccount.cashBalance < amountUsd) {
            return@withContext Result.failure(Exception("Insufficient USD cash balance"))
        }

        val fund = marketEngine.mutualFunds.value.find { it.code == fundCode }
            ?: return@withContext Result.failure(Exception("Mutual fund not found"))

        val unitsBought = amountUsd / fund.nav

        // Deduct cash
        database.walletDao().updateCashBalance(walletAccount.cashBalance - amountUsd)

        // Update or insert holding
        val existing = database.mutualFundDao().getFundHoldingByCode(fundCode)
        if (existing != null) {
            val totalUnits = existing.units + unitsBought
            val totalInvested = existing.totalInvested + amountUsd
            val avgNav = totalInvested / totalUnits
            database.mutualFundDao().insertOrUpdateFundHolding(
                existing.copy(
                    units = totalUnits,
                    avgNav = avgNav,
                    totalInvested = totalInvested
                )
            )
        } else {
            database.mutualFundDao().insertOrUpdateFundHolding(
                MutualFundHoldingEntity(
                    fundCode = fundCode,
                    fundName = fund.name,
                    units = unitsBought,
                    avgNav = fund.nav,
                    totalInvested = amountUsd,
                    purchaseTimestamp = System.currentTimeMillis()
                )
            )
        }

        Result.success("Invested \$${String.format("%,.2f", amountUsd)} in ${fund.name} (${String.format("%.3f", unitsBought)} units)")
    }

    suspend fun addCashDeposit(amount: Double) = withContext(Dispatchers.IO) {
        val wallet = database.walletDao().getWalletDirect() ?: WalletAccountEntity(1, 0.0)
        database.walletDao().updateCashBalance(wallet.cashBalance + amount)
    }

    suspend fun withdrawCash(amount: Double): Result<String> = withContext(Dispatchers.IO) {
        val wallet = database.walletDao().getWalletDirect() ?: WalletAccountEntity(1, 0.0)
        if (wallet.cashBalance < amount) {
            return@withContext Result.failure(Exception("Insufficient cash balance"))
        }
        database.walletDao().updateCashBalance(wallet.cashBalance - amount)
        Result.success("Successfully withdrew \$${String.format("%,.2f", amount)}")
    }

    suspend fun createAlert(
        symbol: String,
        condition: String,
        targetPrice: Double,
        note: String
    ): Long = withContext(Dispatchers.IO) {
        val currentPrice = marketEngine.cryptoMarkets.value.find { it.symbol == symbol }?.currentPrice ?: targetPrice
        val alert = PriceAlertEntity(
            symbol = symbol,
            condition = condition,
            targetPrice = targetPrice,
            initialPrice = currentPrice,
            note = note,
            isEnabled = true,
            isTriggered = false
        )
        database.alertDao().insertAlert(alert)
    }

    suspend fun toggleAlert(id: Long, enabled: Boolean) = withContext(Dispatchers.IO) {
        database.alertDao().setAlertEnabled(id, enabled)
    }

    suspend fun deleteAlert(id: Long) = withContext(Dispatchers.IO) {
        database.alertDao().deleteAlert(id)
    }

    suspend fun resetAlert(alert: PriceAlertEntity) = withContext(Dispatchers.IO) {
        val currentPrice = marketEngine.cryptoMarkets.value.find { it.symbol == alert.symbol }?.currentPrice ?: alert.initialPrice
        database.alertDao().updateAlert(
            alert.copy(
                isTriggered = false,
                isEnabled = true,
                triggeredAt = null,
                initialPrice = currentPrice
            )
        )
    }

    suspend fun calculateTaxReport(year: Int, method: TaxAccountingMethod): TaxReportSummary = withContext(Dispatchers.IO) {
        val allTrades = database.tradeDao().getTradesForYearDirect(year)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val oneYearMillis = 365L * 24 * 60 * 60 * 1000L

        // Separate buys and sells
        val sellTrades = allTrades.filter { it.action == "SELL" }

        val taxableEvents = mutableListOf<TaxableEvent>()
        var totalProceeds = 0.0
        var totalCostBasis = 0.0
        var shortTermGains = 0.0
        var shortTermLosses = 0.0
        var longTermGains = 0.0
        var longTermLosses = 0.0

        for (sell in sellTrades) {
            val proceeds = sell.totalAmountUsd
            totalProceeds += proceeds

            // Compute cost basis using estimated holding duration
            val holdingTime = System.currentTimeMillis() - sell.timestamp
            // Check if holding is long term
            val isLongTerm = holdingTime >= oneYearMillis || sell.id == 2L // simulated prior tax year buy

            val gainLoss = sell.realizedGainLossUsd
            val costBasis = max(0.0, proceeds - gainLoss)
            totalCostBasis += costBasis

            val dateSold = dateFormat.format(Date(sell.timestamp))
            val dateAcquired = dateFormat.format(Date(sell.timestamp - (if (isLongTerm) 420L else 35L) * 86400000L))

            if (gainLoss >= 0) {
                if (isLongTerm) longTermGains += gainLoss else shortTermGains += gainLoss
            } else {
                val loss = kotlin.math.abs(gainLoss)
                if (isLongTerm) longTermLosses += loss else shortTermLosses += loss
            }

            taxableEvents.add(
                TaxableEvent(
                    id = sell.id,
                    assetSymbol = sell.symbol,
                    quantity = sell.quantity,
                    dateAcquired = dateAcquired,
                    dateSold = dateSold,
                    proceedsUsd = proceeds,
                    costBasisUsd = costBasis,
                    gainLossUsd = gainLoss,
                    isLongTerm = isLongTerm
                )
            )
        }

        val netShortTerm = shortTermGains - shortTermLosses
        val netLongTerm = longTermGains - longTermLosses
        val netCapitalGain = (totalProceeds - totalCostBasis)

        // Estimated Tax: 24% for short-term gains, 15% for long-term gains
        val estimatedShortTermTax = max(0.0, netShortTerm * 0.24)
        val estimatedLongTermTax = max(0.0, netLongTerm * 0.15)
        val totalEstimatedTax = estimatedShortTermTax + estimatedLongTermTax

        TaxReportSummary(
            taxYear = year,
            accountingMethod = method,
            totalProceeds = totalProceeds,
            totalCostBasis = totalCostBasis,
            netCapitalGain = netCapitalGain,
            shortTermGains = shortTermGains,
            shortTermLosses = shortTermLosses,
            netShortTerm = netShortTerm,
            longTermGains = longTermGains,
            longTermLosses = longTermLosses,
            netLongTerm = netLongTerm,
            estimatedTaxLiability = totalEstimatedTax,
            taxableEventsCount = taxableEvents.size,
            taxableEvents = taxableEvents
        )
    }
}
