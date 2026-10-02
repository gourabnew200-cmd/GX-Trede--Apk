package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PriceAlertEntity
import com.example.ui.MainViewModel
import com.example.ui.components.CreatePriceAlertSheet
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@Composable
fun AlertsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val alerts by viewModel.alerts.collectAsState()
    val cryptoMarkets by viewModel.cryptoMarkets.collectAsState()
    val showCreateSheet by viewModel.showCreateAlertSheet.collectAsState()
    val preselectedSymbol by viewModel.preselectedAlertSymbol.collectAsState()

    val activeAlerts = remember(alerts) { alerts.filter { !it.isTriggered } }
    val triggeredAlerts = remember(alerts) { alerts.filter { it.isTriggered } }

    val priceMap = remember(cryptoMarkets) {
        cryptoMarkets.associateBy({ it.symbol }, { it.currentPrice })
    }

    Box(modifier = modifier.fillMaxSize().background(DarkBackground)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("alerts_lazy_column"),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Hero Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("alerts_summary_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Real-Time Price Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Automated volatility & target triggers", fontSize = 12.sp, color = TextSecondary)
                            }

                            Row(
                                modifier = Modifier
                                    .background(BullGreen.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(BullGreen))
                                Text("Active Engine", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BullGreen)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatBox(label = "Active Alerts", value = "${activeAlerts.size}", modifier = Modifier.weight(1f))
                            StatBox(label = "Triggered Events", value = "${triggeredAlerts.size}", modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { viewModel.openCreateAlertSheet() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("create_new_alert_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = WarningAmber, contentColor = DarkBackground),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AddAlert, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create New Alert", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Active Alerts Section
            item {
                Text(
                    text = "Active Watchlist Alerts (${activeAlerts.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            if (activeAlerts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No Active Alerts", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("Tap 'Create New Alert' to track breakouts or price drops.", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
            } else {
                items(activeAlerts, key = { it.id }) { alert ->
                    val currentPrice = priceMap[alert.symbol] ?: alert.initialPrice
                    ActiveAlertCard(
                        alert = alert,
                        currentPrice = currentPrice,
                        onToggle = { enabled -> viewModel.toggleAlert(alert.id, enabled) },
                        onDelete = { viewModel.deleteAlert(alert.id) }
                    )
                }
            }

            // Triggered Alerts History Section
            if (triggeredAlerts.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Triggered Alerts Log (${triggeredAlerts.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                items(triggeredAlerts, key = { it.id }) { alert ->
                    TriggeredAlertCard(
                        alert = alert,
                        onReset = { viewModel.resetAlert(alert) },
                        onDelete = { viewModel.deleteAlert(alert.id) }
                    )
                }
            }
        }

        // Create Alert Bottom Sheet
        if (showCreateSheet) {
            CreatePriceAlertSheet(
                coins = cryptoMarkets,
                preselectedSymbol = preselectedSymbol,
                onDismiss = { viewModel.closeCreateAlertSheet() },
                onSaveAlert = { symbol, condition, target, note ->
                    viewModel.createPriceAlert(symbol, condition, target, note)
                }
            )
        }
    }
}

@Composable
fun ActiveAlertCard(
    alert: PriceAlertEntity,
    currentPrice: Double,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val distancePct = remember(currentPrice, alert.targetPrice, alert.condition) {
        if (alert.condition.startsWith("PCT")) {
            val actualChange = ((currentPrice - alert.initialPrice) / alert.initialPrice) * 100.0
            alert.targetPrice - abs(actualChange)
        } else {
            val dist = ((alert.targetPrice - currentPrice) / currentPrice) * 100.0
            dist
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("alert_card_${alert.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(alert.symbol, fontWeight = FontWeight.Bold, color = WarningAmber, fontSize = 12.sp)
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(alert.symbol, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                            AlertConditionChip(condition = alert.condition)
                        }
                        if (alert.note.isNotEmpty()) {
                            Text(alert.note, fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Switch(
                        checked = alert.isEnabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkBackground,
                            checkedTrackColor = WarningAmber
                        ),
                        modifier = Modifier.testTag("toggle_alert_${alert.id}")
                    )
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp).testTag("delete_alert_${alert.id}")) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Target Value", fontSize = 11.sp, color = TextMuted)
                    Text(
                        if (alert.condition.startsWith("PCT")) "${alert.targetPrice}%" else "\$${String.format("%,.2f", alert.targetPrice)}",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Current Live", fontSize = 11.sp, color = TextMuted)
                    Text(
                        "\$${String.format("%,.2f", currentPrice)}",
                        fontWeight = FontWeight.SemiBold,
                        color = NeonCyan,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Distance", fontSize = 11.sp, color = TextMuted)
                    Text(
                        "${if (distancePct > 0) "+" else ""}${String.format("%.1f", distancePct)}%",
                        fontWeight = FontWeight.SemiBold,
                        color = if (abs(distancePct) < 2.0) WarningAmber else TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TriggeredAlertCard(
    alert: PriceAlertEntity,
    onReset: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.US) }
    val timeStr = remember(alert.triggeredAt) {
        if (alert.triggeredAt != null) dateFormat.format(Date(alert.triggeredAt)) else "Recently"
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("triggered_alert_${alert.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(BearRed.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = BearRed, modifier = Modifier.size(16.dp))
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${alert.symbol} Triggered!", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                        Text(timeStr, fontSize = 11.sp, color = TextMuted)
                    }
                    Text(
                        "Target: ${if (alert.condition.startsWith("PCT")) "${alert.targetPrice}%" else "\$${String.format("%,.2f", alert.targetPrice)}"}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(
                    onClick = onReset,
                    colors = ButtonDefaults.textButtonColors(contentColor = NeonCyan)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Re-Arm", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun AlertConditionChip(condition: String) {
    val (text, color) = when (condition) {
        "ABOVE" -> "Above" to BullGreen
        "BELOW" -> "Below" to BearRed
        "PCT_UP" -> "+% Surge" to BullGreen
        "PCT_DOWN" -> "-% Dip" to BearRed
        else -> condition to TextSecondary
    }

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
