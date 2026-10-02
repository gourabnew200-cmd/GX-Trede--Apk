package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.LiveIndicator
import com.example.ui.components.TradeExecutionSheet
import com.example.ui.screens.*
import com.example.ui.theme.*

data class NavTabItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoPulseApp(
    viewModel: MainViewModel = viewModel()
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val wallet by viewModel.wallet.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val holdings by viewModel.holdings.collectAsState()
    val tradingCoin by viewModel.tradingCoin.collectAsState()

    val triggeredCount = remember(alerts) { alerts.count { it.isTriggered } }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
        }
    }

    // BackHandler: if not on Markets tab, navigate back to Markets tab
    BackHandler(enabled = currentTab != 0) {
        viewModel.selectTab(0)
    }

    val navItems = listOf(
        NavTabItem("Markets", Icons.Filled.ShowChart, Icons.Outlined.ShowChart, "tab_markets"),
        NavTabItem("Portfolio", Icons.Filled.PieChart, Icons.Outlined.PieChart, "tab_portfolio"),
        NavTabItem("Alerts", Icons.Filled.NotificationsActive, Icons.Outlined.Notifications, "tab_alerts"),
        NavTabItem("Sentiment", Icons.Filled.Insights, Icons.Outlined.Insights, "tab_sentiment"),
        NavTabItem("Wealth & Tax", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong, "tab_wealth_tax")
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_root_scaffold"),
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets.statusBars,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 80.dp)
            ) { data ->
                Snackbar(
                    containerColor = DarkSurfaceElevated,
                    contentColor = TextPrimary,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(data.visuals.message, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        },
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // App Logo and Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonCyan),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.TrendingUp,
                                contentDescription = null,
                                tint = DarkBackground,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "CryptoPulse",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                            LiveIndicator()
                        }
                    }

                    // Cash Balance Pill
                    val cash = wallet?.cashBalance ?: 0.0
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(DarkSurfaceVariant)
                            .clickable { viewModel.openDepositSheet() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("top_bar_wallet_pill"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = BullGreen, modifier = Modifier.size(16.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text("USD Balance", fontSize = 9.sp, color = TextMuted)
                            Text(
                                "\$${String.format("%,.0f", cash)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DarkBorder))
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_navigation_bar"),
                containerColor = DarkSurface,
                tonalElevation = 8.dp
            ) {
                navItems.forEachIndexed { index, item ->
                    val isSelected = currentTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.selectTab(index) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (index == 2 && triggeredCount > 0) {
                                        Badge(containerColor = BearRed) {
                                            Text("$triggeredCount", color = TextPrimary, fontSize = 10.sp)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title,
                                    tint = if (isSelected) NeonCyan else TextMuted
                                )
                            }
                        },
                        label = {
                            Text(
                                text = item.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NeonCyan else TextMuted
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = NeonCyan,
                            indicatorColor = NeonCyan.copy(alpha = 0.15f),
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag(item.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> MarketsScreen(viewModel = viewModel)
                1 -> PortfolioScreen(viewModel = viewModel)
                2 -> AlertsScreen(viewModel = viewModel)
                3 -> SentimentScreen(viewModel = viewModel)
                4 -> WealthAndTaxScreen(viewModel = viewModel)
            }
        }
    }

    // Trade Execution Modal Sheet
    tradingCoin?.let { coin ->
        val cashBal = wallet?.cashBalance ?: 0.0
        val holdingQty = holdings.find { it.symbol == coin.symbol }?.quantity ?: 0.0
        TradeExecutionSheet(
            coin = coin,
            cashBalance = cashBal,
            holdingQuantity = holdingQty,
            onDismiss = { viewModel.closeTradeSheet() },
            onExecuteTrade = { action, orderType, qty, price ->
                viewModel.executeTrade(action, orderType, qty, price)
            }
        )
    }
}
