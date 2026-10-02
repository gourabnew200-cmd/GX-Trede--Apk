package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MutualFundItem
import com.example.data.model.TaxAccountingMethod
import com.example.data.model.TaxableEvent
import com.example.ui.MainViewModel
import com.example.ui.components.SparklineAreaChart
import com.example.ui.theme.*

@Composable
fun WealthAndTaxScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedSubTab by remember { mutableStateOf(0) } // 0: Mutual Funds, 1: Tax Reporting

    val mutualFunds by viewModel.mutualFunds.collectAsState()
    val mfHoldings by viewModel.mutualFundHoldings.collectAsState()
    val wallet by viewModel.wallet.collectAsState()
    val investSheetFund by viewModel.showMutualFundInvestSheet.collectAsState()

    val taxYear by viewModel.taxYear.collectAsState()
    val taxMethod by viewModel.taxMethod.collectAsState()
    val taxReport by viewModel.taxReport.collectAsState()

    Box(modifier = modifier.fillMaxSize().background(DarkBackground)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(8.dp))

            // Sub-Tab Switcher
            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = DarkSurfaceVariant,
                contentColor = NeonCyan,
                modifier = Modifier.clip(RoundedCornerShape(10.dp))
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Mutual Funds & ETFs", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.testTag("subtab_mutual_funds")
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Tax Reporting", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.testTag("subtab_tax_reporting")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedSubTab == 0) {
                // ==================== MUTUAL FUNDS TAB ====================
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("mutual_funds_lazy_column"),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Long-Term Wealth Allocation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("Institutional index funds alongside crypto", fontSize = 12.sp, color = TextSecondary)
                                    }
                                    Box(
                                        modifier = Modifier.background(PurpleAccent.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("LOW EXPENSE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PurpleAccent)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                val totalInvested = mfHoldings.sumOf { it.totalInvested }
                                val navMap = mutualFunds.associateBy({ it.code }, { it.nav })
                                val totalCurrentVal = mfHoldings.sumOf { it.units * (navMap[it.fundCode] ?: it.avgNav) }
                                val totalPnl = totalCurrentVal - totalInvested

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StatBox(label = "Total Invested", value = "\$${String.format("%,.2f", totalInvested)}", modifier = Modifier.weight(1f))
                                    StatBox(
                                        label = "Current Value",
                                        value = "\$${String.format("%,.2f", totalCurrentVal)}",
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatBox(
                                        label = "Total Gain",
                                        value = "${if (totalPnl >= 0) "+" else ""}\$${String.format("%,.2f", totalPnl)}",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            "Available Funds & Crypto Equity Trusts",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    items(mutualFunds, key = { it.code }) { fund ->
                        MutualFundCard(
                            fund = fund,
                            onInvestClick = { viewModel.openInvestMutualFundSheet(fund) }
                        )
                    }
                }
            } else {
                // ==================== TAX REPORTING TAB ====================
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("tax_lazy_column"),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Tax Year & Method Selector Controls
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("tax_controls_card"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("End-of-Year Compliance Engine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("IRS Form 8949 capital gains & loss calculations", fontSize = 12.sp, color = TextSecondary)

                                Spacer(modifier = Modifier.height(14.dp))

                                // Tax Year selector
                                Text("Tax Filing Year", fontSize = 12.sp, color = TextMuted)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(2026, 2025, 2024).forEach { yr ->
                                        FilterChip(
                                            selected = taxYear == yr,
                                            onClick = { viewModel.setTaxYear(yr) },
                                            label = { Text("$yr", fontWeight = FontWeight.Bold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                                selectedLabelColor = NeonCyan
                                            ),
                                            modifier = Modifier.testTag("tax_year_$yr")
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Accounting Method selector
                                Text("Cost Basis Accounting Method", fontSize = 12.sp, color = TextMuted)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TaxAccountingMethod.values().forEach { method ->
                                        FilterChip(
                                            selected = taxMethod == method,
                                            onClick = { viewModel.setTaxMethod(method) },
                                            label = { Text(method.title) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = PurpleAccent.copy(alpha = 0.2f),
                                                selectedLabelColor = PurpleAccent
                                            ),
                                            modifier = Modifier.testTag("method_${method.name}")
                                        )
                                    }
                                }
                                Text(
                                    taxMethod.description,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    // Tax Summary Overview
                    taxReport?.let { report ->
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("tax_summary_overview_card"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${report.taxYear} Realized Capital Gain", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (report.netCapitalGain >= 0) BullGreen.copy(alpha = 0.15f) else BearRed.copy(alpha = 0.15f),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                "${if (report.netCapitalGain >= 0) "+" else ""}\$${String.format("%,.2f", report.netCapitalGain)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (report.netCapitalGain >= 0) BullGreen else BearRed
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        StatBox(label = "Total Proceeds", value = "\$${String.format("%,.2f", report.totalProceeds)}", modifier = Modifier.weight(1f))
                                        StatBox(label = "Total Cost Basis", value = "\$${String.format("%,.2f", report.totalCostBasis)}", modifier = Modifier.weight(1f))
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Estimated Tax Liability Card
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("Estimated Tax Liability", fontSize = 12.sp, color = TextSecondary)
                                                Text("Based on 24% ST / 15% LT rates", fontSize = 10.sp, color = TextMuted)
                                            }
                                            Text(
                                                "\$${String.format("%,.2f", report.estimatedTaxLiability)}",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 18.sp,
                                                color = WarningAmber,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Short-Term vs Long-Term breakdown
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text("Short-Term (<1yr)", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                                Text(
                                                    "Gains: \$${String.format("%,.2f", report.shortTermGains)}",
                                                    fontSize = 11.sp,
                                                    color = BullGreen
                                                )
                                                Text(
                                                    "Losses: \$${String.format("%,.2f", report.shortTermLosses)}",
                                                    fontSize = 11.sp,
                                                    color = BearRed
                                                )
                                                Text(
                                                    "Net: \$${String.format("%,.2f", report.netShortTerm)}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                            }
                                        }

                                        Card(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text("Long-Term (>=1yr)", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                                Text(
                                                    "Gains: \$${String.format("%,.2f", report.longTermGains)}",
                                                    fontSize = 11.sp,
                                                    color = BullGreen
                                                )
                                                Text(
                                                    "Losses: \$${String.format("%,.2f", report.longTermLosses)}",
                                                    fontSize = 11.sp,
                                                    color = BearRed
                                                )
                                                Text(
                                                    "Net: \$${String.format("%,.2f", report.netLongTerm)}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Export Report Button
                                    Button(
                                        onClick = { viewModel.exportTaxSummary(context) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(46.dp)
                                            .testTag("export_tax_report_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBackground)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Export Tax Summary & Form 8949", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Taxable Events Ledger (Form 8949)
                        item {
                            Text(
                                "Form 8949 Taxable Sales (${report.taxableEventsCount})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        if (report.taxableEvents.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = "No Taxable Sales in ${report.taxYear}",
                                    message = "Sell cryptocurrency to realize gains or losses for this tax year."
                                )
                            }
                        } else {
                            items(report.taxableEvents, key = { it.id }) { event ->
                                TaxableEventCard(event = event)
                            }
                        }
                    }
                }
            }
        }

        // Invest in Mutual Fund Modal Bottom Sheet
        investSheetFund?.let { fund ->
            InvestMutualFundSheet(
                fund = fund,
                cashBalance = wallet?.cashBalance ?: 0.0,
                onDismiss = { viewModel.closeInvestMutualFundSheet() },
                onInvest = { amount -> viewModel.investInMutualFund(fund.code, amount) }
            )
        }
    }
}

@Composable
fun MutualFundCard(
    fund: MutualFundItem,
    onInvestClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("fund_card_${fund.code}"),
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(fund.code.take(3), fontWeight = FontWeight.Bold, color = PurpleAccent, fontSize = 12.sp)
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(fund.code, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                            Box(
                                modifier = Modifier
                                    .background(DarkSurfaceVariant, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(fund.category, fontSize = 10.sp, color = TextSecondary)
                            }
                        }
                        Text(fund.provider, fontSize = 12.sp, color = TextSecondary)
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "\$${String.format("%.2f", fund.nav)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                    Text(
                        "${if (fund.changePct >= 0) "+" else ""}${String.format("%.2f", fund.changePct)}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (fund.changePct >= 0) BullGreen else BearRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = fund.description,
                fontSize = 11.sp,
                color = TextMuted,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Mini sparkline & Key Returns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column {
                        Text("1Y Return", fontSize = 10.sp, color = TextMuted)
                        Text("+${fund.return1YPct}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BullGreen)
                    }
                    Column {
                        Text("5Y Return", fontSize = 10.sp, color = TextMuted)
                        Text("+${fund.return5YPct}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BullGreen)
                    }
                    Column {
                        Text("Expense", fontSize = 10.sp, color = TextMuted)
                        Text("${fund.expenseRatioPct}%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                }

                SparklineAreaChart(
                    dataPoints = fund.historyNavPoints,
                    lineColor = PurpleAccent,
                    showGradient = false,
                    strokeWidth = 2.5f,
                    modifier = Modifier.width(60.dp).height(24.dp)
                )

                Button(
                    onClick = onInvestClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent, contentColor = TextPrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("invest_button_${fund.code}")
                ) {
                    Text("Invest", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TaxableEventCard(event: TaxableEvent) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("taxable_event_${event.id}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${String.format("%.4f", event.quantity)} ${event.assetSymbol}",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                    Box(
                        modifier = Modifier
                            .background(
                                if (event.isLongTerm) PurpleAccent.copy(alpha = 0.2f) else WarningAmber.copy(alpha = 0.2f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            if (event.isLongTerm) "LONG TERM (15%)" else "SHORT TERM (24%)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (event.isLongTerm) PurpleAccent else WarningAmber
                        )
                    }
                }

                Text(
                    "${if (event.gainLossUsd >= 0) "+" else ""}\$${String.format("%,.2f", event.gainLossUsd)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (event.gainLossUsd >= 0) BullGreen else BearRed
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Acquired: ${event.dateAcquired}", fontSize = 11.sp, color = TextMuted)
                Text("Sold: ${event.dateSold}", fontSize = 11.sp, color = TextMuted)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Cost Basis: \$${String.format("%,.2f", event.costBasisUsd)}", fontSize = 11.sp, color = TextSecondary)
                Text("Proceeds: \$${String.format("%,.2f", event.proceedsUsd)}", fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestMutualFundSheet(
    fund: MutualFundItem,
    cashBalance: Double,
    onDismiss: () -> Unit,
    onInvest: (Double) -> Unit
) {
    var amountInput by remember { mutableStateOf("1000") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val amount = amountInput.toDoubleOrNull() ?: 0.0
    val estUnits = if (fund.nav > 0) amount / fund.nav else 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = DarkBorder) }
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
                Column {
                    Text("Invest in ${fund.code}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(fund.name, fontSize = 12.sp, color = TextSecondary)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text("Available Cash: \$${String.format("%,.2f", cashBalance)}", fontSize = 12.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = amountInput,
                onValueChange = {
                    amountInput = it
                    errorMessage = null
                },
                label = { Text("Investment Amount (USD)") },
                prefix = { Text("\$ ", color = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().testTag("fund_amount_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurpleAccent,
                    unfocusedBorderColor = DarkBorder
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(500.0, 1000.0, 2500.0, 5000.0).forEach { amt ->
                    OutlinedButton(
                        onClick = { amountInput = amt.toInt().toString() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Text("\$${amt.toInt()}", fontSize = 11.sp, color = TextPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Current NAV:", fontSize = 12.sp, color = TextSecondary)
                        Text("\$${String.format("%.2f", fund.nav)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Estimated Units:", fontSize = 12.sp, color = TextSecondary)
                        Text("${String.format("%.4f", estUnits)} shares", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PurpleAccent)
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorMessage ?: "", color = BearRed, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    if (amount <= 0) {
                        errorMessage = "Please enter an amount greater than 0"
                        return@Button
                    }
                    if (amount > cashBalance) {
                        errorMessage = "Insufficient cash balance"
                        return@Button
                    }
                    onInvest(amount)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("confirm_invest_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent, contentColor = TextPrimary)
            ) {
                Text("Confirm Investment", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
