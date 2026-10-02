package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TradeTransactionEntity
import com.example.ui.HoldingDisplayItem
import com.example.ui.MainViewModel
import com.example.ui.components.AllocationSlice
import com.example.ui.components.DonutAllocationChart
import com.example.ui.components.PriceChangeBadge
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PortfolioScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val metrics by viewModel.portfolioMetrics.collectAsState()
    val holdings by viewModel.holdingDisplayItems.collectAsState()
    val mfHoldings by viewModel.mutualFundHoldings.collectAsState()
    val mutualFunds by viewModel.mutualFunds.collectAsState()
    val trades by viewModel.trades.collectAsState()
    val showDepositSheet by viewModel.showDepositSheet.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Crypto Holdings, 1: Mutual Funds, 2: Trade History

    val allocationSlices = remember(metrics) {
        listOf(
            AllocationSlice("Crypto", metrics.cryptoValueUsd, NeonCyan),
            AllocationSlice("Mutual Funds", metrics.mutualFundsValueUsd, PurpleAccent),
            AllocationSlice("USD Cash", metrics.cashBalanceUsd, BullGreen)
        )
    }

    Box(modifier = modifier.fillMaxSize().background(DarkBackground)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("portfolio_lazy_column"),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Net Worth Hero Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("net_worth_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(NeonCyan.copy(alpha = 0.12f), Color.Transparent),
                                    radius = 500f
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total Net Worth", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                PriceChangeBadge(changePct = metrics.dailyProfitLossPct, prefix = "24h ")
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "\$${String.format("%,.2f", metrics.netWorthUsd)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "All-Time: ${if (metrics.totalProfitLossUsd >= 0) "+" else ""}\$${String.format("%,.2f", metrics.totalProfitLossUsd)} (${String.format("%.2f", metrics.totalProfitLossPct)}%)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (metrics.totalProfitLossUsd >= 0) BullGreen else BearRed
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Action buttons: Deposit Cash, Trade
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.openDepositSheet() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .testTag("deposit_cash_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBackground)
                                ) {
                                    Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Deposit Cash", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.selectTab(0) }, // Jump to market
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .testTag("portfolio_trade_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                                ) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("New Trade", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Asset Allocation Donut Visualizer Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Asset Allocation Breakdown", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DonutAllocationChart(
                                slices = allocationSlices,
                                centerTitle = "Assets",
                                centerSubtitle = "\$${String.format("%,.0f", metrics.netWorthUsd)}",
                                modifier = Modifier.size(140.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AllocationLegendRow(
                                    label = "Crypto",
                                    valueUsd = metrics.cryptoValueUsd,
                                    pct = if (metrics.netWorthUsd > 0) (metrics.cryptoValueUsd / metrics.netWorthUsd) * 100.0 else 0.0,
                                    color = NeonCyan
                                )
                                AllocationLegendRow(
                                    label = "Mutual Funds",
                                    valueUsd = metrics.mutualFundsValueUsd,
                                    pct = if (metrics.netWorthUsd > 0) (metrics.mutualFundsValueUsd / metrics.netWorthUsd) * 100.0 else 0.0,
                                    color = PurpleAccent
                                )
                                AllocationLegendRow(
                                    label = "USD Cash",
                                    valueUsd = metrics.cashBalanceUsd,
                                    pct = if (metrics.netWorthUsd > 0) (metrics.cashBalanceUsd / metrics.netWorthUsd) * 100.0 else 0.0,
                                    color = BullGreen
                                )
                            }
                        }
                    }
                }
            }

            // Portfolio Subtabs
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurfaceVariant,
                    contentColor = NeonCyan,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Crypto (${holdings.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Funds (${mfHoldings.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Trade Ledger", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // Crypto Holdings
                    if (holdings.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = "No Crypto Holdings Yet",
                                message = "Start buying Bitcoin, Ethereum, or Solana on the Markets tab to build your portfolio."
                            )
                        }
                    } else {
                        items(holdings, key = { it.symbol }) { holding ->
                            CryptoHoldingCard(
                                item = holding,
                                onClick = {
                                    val coin = viewModel.cryptoMarkets.value.find { it.symbol == holding.symbol }
                                    if (coin != null) viewModel.openCoinDetail(coin)
                                }
                            )
                        }
                    }
                }
                1 -> {
                    // Mutual Funds
                    if (mfHoldings.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = "No Mutual Funds Invested",
                                message = "Diversify your wealth in low-cost index funds and crypto trusts on the Wealth & Tax tab."
                            )
                        }
                    } else {
                        val navMap = mutualFunds.associateBy({ it.code }, { it.nav })
                        val nameMap = mutualFunds.associateBy({ it.code }, { it.name })

                        items(mfHoldings, key = { it.fundCode }) { mfh ->
                            val currentNav = navMap[mfh.fundCode] ?: mfh.avgNav
                            val currentValue = mfh.units * currentNav
                            val pnl = currentValue - mfh.totalInvested
                            val pnlPct = if (mfh.totalInvested > 0) (pnl / mfh.totalInvested) * 100.0 else 0.0

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(mfh.fundCode, fontWeight = FontWeight.Bold, color = PurpleAccent, fontSize = 15.sp)
                                            Text(nameMap[mfh.fundCode] ?: mfh.fundName, fontSize = 12.sp, color = TextSecondary)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                "\$${String.format("%,.2f", currentValue)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = TextPrimary
                                            )
                                            Text(
                                                "${if (pnl >= 0) "+" else ""}\$${String.format("%.2f", pnl)} (${String.format("%.2f", pnlPct)}%)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (pnl >= 0) BullGreen else BearRed
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Units: ${String.format("%.3f", mfh.units)}", fontSize = 11.sp, color = TextMuted)
                                        Text("Avg NAV: \$${String.format("%.2f", mfh.avgNav)}", fontSize = 11.sp, color = TextMuted)
                                        Text("Current NAV: \$${String.format("%.2f", currentNav)}", fontSize = 11.sp, color = TextMuted)
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Trade Ledger
                    if (trades.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = "No Trade History",
                                message = "Execute your first trade order to view your on-chain and exchange transaction history."
                            )
                        }
                    } else {
                        items(trades, key = { it.id }) { trade ->
                            TradeLedgerRow(trade = trade)
                        }
                    }
                }
            }
        }

        // Deposit Cash Modal Bottom Sheet
        if (showDepositSheet) {
            DepositCashModalSheet(
                currentBalance = metrics.cashBalanceUsd,
                onDismiss = { viewModel.closeDepositSheet() },
                onDeposit = { amount -> viewModel.addDeposit(amount) }
            )
        }
    }
}

@Composable
fun CryptoHoldingCard(
    item: HoldingDisplayItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("holding_card_${item.symbol}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(item.symbol.take(3), fontWeight = FontWeight.Bold, color = NeonCyan, fontSize = 12.sp)
                    }
                    Column {
                        Text(item.symbol, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                        Text("${String.format("%.4f", item.quantity)} ${item.symbol}", fontSize = 12.sp, color = TextSecondary)
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "\$${String.format("%,.2f", item.currentValueUsd)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                    Text(
                        "${if (item.profitLossUsd >= 0) "+" else ""}\$${String.format("%.2f", item.profitLossUsd)} (${String.format("%.2f", item.profitLossPct)}%)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.profitLossUsd >= 0) BullGreen else BearRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Avg Buy: \$${formatCryptoPrice(item.avgBuyPrice)}", fontSize = 11.sp, color = TextMuted)
                Text("Price: \$${formatCryptoPrice(item.currentPrice)}", fontSize = 11.sp, color = TextMuted)
                Text("Portfolio Share: ${String.format("%.1f", item.portfolioSharePct)}%", fontSize = 11.sp, color = NeonCyan)
            }
        }
    }
}

@Composable
fun TradeLedgerRow(trade: TradeTransactionEntity) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US) }
    val dateStr = remember(trade.timestamp) { dateFormat.format(Date(trade.timestamp)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
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
                        .background(
                            if (trade.action == "BUY") BullGreen.copy(alpha = 0.2f) else BearRed.copy(alpha = 0.2f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        trade.action,
                        color = if (trade.action == "BUY") BullGreen else BearRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column {
                    Text("${trade.symbol} • ${trade.orderType}", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                    Text(dateStr, fontSize = 11.sp, color = TextMuted)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${if (trade.action == "BUY") "-" else "+"}\$${String.format("%,.2f", trade.totalAmountUsd)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
                Text(
                    "${String.format("%.4f", trade.quantity)} @ \$${formatCryptoPrice(trade.pricePerUnit)}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun AllocationLegendRow(
    label: String,
    valueUsd: Double,
    pct: Double,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(label, fontSize = 12.sp, color = TextSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("\$${String.format("%,.0f", valueUsd)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("(${String.format("%.1f", pct)}%)", fontSize = 11.sp, color = TextMuted)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositCashModalSheet(
    currentBalance: Double,
    onDismiss: () -> Unit,
    onDeposit: (Double) -> Unit
) {
    var depositAmount by remember { mutableStateOf("5000") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = DarkBorder) },
        modifier = Modifier.testTag("deposit_modal_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Deposit USD Fiat Capital", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("Current Cash Balance: \$${String.format("%,.2f", currentBalance)}", fontSize = 12.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = depositAmount,
                onValueChange = { depositAmount = it },
                label = { Text("Deposit Amount (USD)") },
                prefix = { Text("\$ ", color = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().testTag("deposit_amount_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = DarkBorder
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Deposit Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1000.0, 5000.0, 10000.0, 25000.0).forEach { amt ->
                    OutlinedButton(
                        onClick = { depositAmount = amt.toInt().toString() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Text("\$${amt.toInt()}", fontSize = 11.sp, color = TextPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    val amt = depositAmount.toDoubleOrNull() ?: 0.0
                    if (amt > 0) onDeposit(amt)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("confirm_deposit_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBackground)
            ) {
                Text("Confirm Deposit", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun EmptyStateCard(title: String, message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = TextMuted, modifier = Modifier.size(44.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(message, color = TextSecondary, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
