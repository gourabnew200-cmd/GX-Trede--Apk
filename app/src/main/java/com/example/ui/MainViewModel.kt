package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.repository.CryptoPulseRepository
import com.example.data.repository.MarketEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.max

data class PortfolioMetrics(
    val netWorthUsd: Double = 0.0,
    val cryptoValueUsd: Double = 0.0,
    val mutualFundsValueUsd: Double = 0.0,
    val cashBalanceUsd: Double = 0.0,
    val totalProfitLossUsd: Double = 0.0,
    val totalProfitLossPct: Double = 0.0,
    val dailyProfitLossUsd: Double = 0.0,
    val dailyProfitLossPct: Double = 0.0
)

data class HoldingDisplayItem(
    val symbol: String,
    val name: String,
    val quantity: Double,
    val avgBuyPrice: Double,
    val currentPrice: Double,
    val currentValueUsd: Double,
    val totalCostBasisUsd: Double,
    val profitLossUsd: Double,
    val profitLossPct: Double,
    val portfolioSharePct: Double
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val marketEngine = MarketEngine()
    val repository = CryptoPulseRepository(database, marketEngine)

    // Current Navigation Tab (0: Markets, 1: Portfolio, 2: Alerts, 3: Sentiment, 4: Mutual Funds & Tax)
    private val _currentTab = MutableStateFlow(0)
    val currentTab = _currentTab.asStateFlow()

    // Markets State
    val cryptoMarkets = marketEngine.cryptoMarkets
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _marketFilter = MutableStateFlow("ALL") // "ALL", "GAINERS", "LOSERS", "VOLATILE"
    val marketFilter = _marketFilter.asStateFlow()

    private val _selectedCoin = MutableStateFlow<CryptoMarketItem?>(null)
    val selectedCoin = _selectedCoin.asStateFlow()

    private val _tradingCoin = MutableStateFlow<CryptoMarketItem?>(null)
    val tradingCoin = _tradingCoin.asStateFlow()

    // Database Flows
    val holdings = repository.holdings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val trades = repository.trades.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val alerts = repository.alerts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val mutualFundHoldings = repository.mutualFundHoldings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val wallet = repository.wallet.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val mutualFunds = marketEngine.mutualFunds
    val sentimentData = marketEngine.sentimentData

    // UI Sheets State
    private val _showCreateAlertSheet = MutableStateFlow(false)
    val showCreateAlertSheet = _showCreateAlertSheet.asStateFlow()

    private val _preselectedAlertSymbol = MutableStateFlow<String?>(null)
    val preselectedAlertSymbol = _preselectedAlertSymbol.asStateFlow()

    private val _showDepositSheet = MutableStateFlow(false)
    val showDepositSheet = _showDepositSheet.asStateFlow()

    private val _showMutualFundInvestSheet = MutableStateFlow<MutualFundItem?>(null)
    val showMutualFundInvestSheet = _showMutualFundInvestSheet.asStateFlow()

    // Tax Engine State
    private val _taxYear = MutableStateFlow(2026)
    val taxYear = _taxYear.asStateFlow()

    private val _taxMethod = MutableStateFlow(TaxAccountingMethod.FIFO)
    val taxMethod = _taxMethod.asStateFlow()

    private val _taxReport = MutableStateFlow<TaxReportSummary?>(null)
    val taxReport = _taxReport.asStateFlow()

    // Snackbars / Feedback message
    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    init {
        // Collect real-time triggered alert events
        viewModelScope.launch {
            marketEngine.alertTriggerEvents.collect { event ->
                triggerVibration()
                _snackbarMessage.emit("🚨 Alert Triggered: ${event.message}")
            }
        }

        // Recalculate tax report when tax year or method changes
        viewModelScope.launch {
            combine(_taxYear, _taxMethod, trades) { year, method, _ ->
                Pair(year, method)
            }.collect { (year, method) ->
                refreshTaxReport(year, method)
            }
        }
    }

    // Derived Portfolio Metrics
    val portfolioMetrics: StateFlow<PortfolioMetrics> = combine(
        holdings,
        cryptoMarkets,
        mutualFundHoldings,
        mutualFunds,
        wallet
    ) { hList, cList, mfHoldings, mFunds, w ->
        val priceMap = cList.associateBy({ it.symbol }, { it.currentPrice })
        val changeMap = cList.associateBy({ it.symbol }, { it.change24h })
        val navMap = mFunds.associateBy({ it.code }, { it.nav })
        val mfChangeMap = mFunds.associateBy({ it.code }, { it.changePct })

        var cryptoVal = 0.0
        var cryptoCost = 0.0
        var crypto24hDelta = 0.0

        hList.forEach { h ->
            val p = priceMap[h.symbol] ?: h.avgBuyPrice
            val value = h.quantity * p
            val cost = h.quantity * h.avgBuyPrice
            val chg = changeMap[h.symbol] ?: 0.0
            val dayDelta = value * (chg / 100.0)

            cryptoVal += value
            cryptoCost += cost
            crypto24hDelta += dayDelta
        }

        var mfVal = 0.0
        var mfCost = 0.0
        var mf24hDelta = 0.0

        mfHoldings.forEach { mfh ->
            val nav = navMap[mfh.fundCode] ?: mfh.avgNav
            val value = mfh.units * nav
            val chg = mfChangeMap[mfh.fundCode] ?: 0.0
            val dayDelta = value * (chg / 100.0)

            mfVal += value
            mfCost += mfh.totalInvested
            mf24hDelta += dayDelta
        }

        val cash = w?.cashBalance ?: 0.0
        val netWorth = cryptoVal + mfVal + cash
        val totalCost = cryptoCost + mfCost + cash

        val totalPl = (netWorth - totalCost)
        val totalPlPct = if (totalCost > 0) (totalPl / totalCost) * 100.0 else 0.0

        val dayDeltaTotal = crypto24hDelta + mf24hDelta
        val dayDeltaPct = if (netWorth > 0) (dayDeltaTotal / netWorth) * 100.0 else 0.0

        PortfolioMetrics(
            netWorthUsd = netWorth,
            cryptoValueUsd = cryptoVal,
            mutualFundsValueUsd = mfVal,
            cashBalanceUsd = cash,
            totalProfitLossUsd = totalPl,
            totalProfitLossPct = totalPlPct,
            dailyProfitLossUsd = dayDeltaTotal,
            dailyProfitLossPct = dayDeltaPct
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PortfolioMetrics())

    // Derived Detailed Holdings List
    val holdingDisplayItems: StateFlow<List<HoldingDisplayItem>> = combine(
        holdings,
        cryptoMarkets,
        portfolioMetrics
    ) { hList, cList, metrics ->
        val priceMap = cList.associateBy({ it.symbol }, { it.currentPrice })
        val totalCrypto = max(1.0, metrics.cryptoValueUsd)

        hList.map { h ->
            val currP = priceMap[h.symbol] ?: h.avgBuyPrice
            val currVal = h.quantity * currP
            val cost = h.quantity * h.avgBuyPrice
            val pl = currVal - cost
            val plPct = if (cost > 0) (pl / cost) * 100.0 else 0.0
            val share = (currVal / totalCrypto) * 100.0

            HoldingDisplayItem(
                symbol = h.symbol,
                name = h.name,
                quantity = h.quantity,
                avgBuyPrice = h.avgBuyPrice,
                currentPrice = currP,
                currentValueUsd = currVal,
                totalCostBasisUsd = cost,
                profitLossUsd = pl,
                profitLossPct = plPct,
                portfolioSharePct = share
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(tab: Int) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setMarketFilter(filter: String) {
        _marketFilter.value = filter
    }

    fun openCoinDetail(coin: CryptoMarketItem) {
        _selectedCoin.value = coin
    }

    fun closeCoinDetail() {
        _selectedCoin.value = null
    }

    fun openTradeSheet(coin: CryptoMarketItem) {
        _tradingCoin.value = coin
    }

    fun closeTradeSheet() {
        _tradingCoin.value = null
    }

    fun openCreateAlertSheet(symbol: String? = null) {
        _preselectedAlertSymbol.value = symbol
        _showCreateAlertSheet.value = true
    }

    fun closeCreateAlertSheet() {
        _showCreateAlertSheet.value = false
        _preselectedAlertSymbol.value = null
    }

    fun openDepositSheet() {
        _showDepositSheet.value = true
    }

    fun closeDepositSheet() {
        _showDepositSheet.value = false
    }

    fun openInvestMutualFundSheet(fund: MutualFundItem) {
        _showMutualFundInvestSheet.value = fund
    }

    fun closeInvestMutualFundSheet() {
        _showMutualFundInvestSheet.value = null
    }

    fun executeTrade(action: TradeAction, orderType: OrderType, quantity: Double, price: Double) {
        val coin = _tradingCoin.value ?: return
        viewModelScope.launch {
            val result = repository.executeTrade(coin.symbol, action, orderType, quantity, price)
            result.onSuccess { msg ->
                _snackbarMessage.emit(msg)
                closeTradeSheet()
            }.onFailure { err ->
                _snackbarMessage.emit("⚠️ ${err.message}")
            }
        }
    }

    fun createPriceAlert(symbol: String, condition: String, targetPrice: Double, note: String) {
        viewModelScope.launch {
            repository.createAlert(symbol, condition, targetPrice, note)
            _snackbarMessage.emit("🔔 Alert configured for $symbol at \$${String.format("%,.2f", targetPrice)}")
            closeCreateAlertSheet()
        }
    }

    fun toggleAlert(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleAlert(id, enabled)
        }
    }

    fun deleteAlert(id: Long) {
        viewModelScope.launch {
            repository.deleteAlert(id)
            _snackbarMessage.emit("Alert deleted")
        }
    }

    fun resetAlert(alert: PriceAlertEntity) {
        viewModelScope.launch {
            repository.resetAlert(alert)
            _snackbarMessage.emit("Alert reset and re-activated for ${alert.symbol}")
        }
    }

    fun investInMutualFund(fundCode: String, amountUsd: Double) {
        viewModelScope.launch {
            val result = repository.investInMutualFund(fundCode, amountUsd)
            result.onSuccess { msg ->
                _snackbarMessage.emit("📈 $msg")
                closeInvestMutualFundSheet()
            }.onFailure { err ->
                _snackbarMessage.emit("⚠️ ${err.message}")
            }
        }
    }

    fun addDeposit(amount: Double) {
        viewModelScope.launch {
            repository.addCashDeposit(amount)
            _snackbarMessage.emit("Deposited \$${String.format("%,.2f", amount)} USD to wallet")
            closeDepositSheet()
        }
    }

    fun setTaxYear(year: Int) {
        _taxYear.value = year
    }

    fun setTaxMethod(method: TaxAccountingMethod) {
        _taxMethod.value = method
    }

    private fun refreshTaxReport(year: Int, method: TaxAccountingMethod) {
        viewModelScope.launch {
            val report = repository.calculateTaxReport(year, method)
            _taxReport.value = report
        }
    }

    fun exportTaxSummary(context: Context) {
        val report = _taxReport.value ?: return
        val textBuilder = StringBuilder()
        textBuilder.append("=== CryptoPulse End-of-Year Tax Report ===\n")
        textBuilder.append("Tax Year: ${report.taxYear}\n")
        textBuilder.append("Accounting Method: ${report.accountingMethod.title} (${report.accountingMethod.description})\n")
        textBuilder.append("------------------------------------------\n")
        textBuilder.append("Total Proceeds: \$${String.format("%,.2f", report.totalProceeds)}\n")
        textBuilder.append("Total Cost Basis: \$${String.format("%,.2f", report.totalCostBasis)}\n")
        textBuilder.append("Net Realized Gain/Loss: \$${String.format("%,.2f", report.netCapitalGain)}\n\n")

        textBuilder.append("Short-Term Capital Gains: \$${String.format("%,.2f", report.shortTermGains)}\n")
        textBuilder.append("Short-Term Capital Losses: \$${String.format("%,.2f", report.shortTermLosses)}\n")
        textBuilder.append("Net Short-Term: \$${String.format("%,.2f", report.netShortTerm)}\n")
        textBuilder.append("Est. Short-Term Tax (24%): \$${String.format("%,.2f", max(0.0, report.netShortTerm * 0.24))}\n\n")

        textBuilder.append("Long-Term Capital Gains: \$${String.format("%,.2f", report.longTermGains)}\n")
        textBuilder.append("Long-Term Capital Losses: \$${String.format("%,.2f", report.longTermLosses)}\n")
        textBuilder.append("Net Long-Term: \$${String.format("%,.2f", report.netLongTerm)}\n")
        textBuilder.append("Est. Long-Term Tax (15%): \$${String.format("%,.2f", max(0.0, report.netLongTerm * 0.15))}\n\n")

        textBuilder.append("TOTAL ESTIMATED TAX LIABILITY: \$${String.format("%,.2f", report.estimatedTaxLiability)}\n")
        textBuilder.append("==========================================\n")
        textBuilder.append("IRS Form 8949 Sales Ledger Preview (${report.taxableEventsCount} events):\n")
        textBuilder.append("Asset | Qty | Acquired | Sold | Proceeds | Cost Basis | Net Gain\n")

        report.taxableEvents.forEach { ev ->
            textBuilder.append(
                "${ev.assetSymbol} | ${String.format("%.4f", ev.quantity)} | ${ev.dateAcquired} | ${ev.dateSold} | " +
                        "\$${String.format("%,.2f", ev.proceedsUsd)} | \$${String.format("%,.2f", ev.costBasisUsd)} | " +
                        "\$${String.format("%,.2f", ev.gainLossUsd)} [${if (ev.isLongTerm) "LONG" else "SHORT"}]\n"
            )
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, textBuilder.toString())
            putExtra(Intent.EXTRA_SUBJECT, "CryptoPulse ${report.taxYear} Tax Summary Report")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Export Tax Summary Report")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    private fun triggerVibration() {
        try {
            val context = getApplication<Application>()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(180, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(180)
            }
        } catch (_: Exception) {}
    }
}
