package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CryptoMarketItem
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePriceAlertSheet(
    coins: List<CryptoMarketItem>,
    preselectedSymbol: String? = null,
    onDismiss: () -> Unit,
    onSaveAlert: (symbol: String, condition: String, targetPrice: Double, note: String) -> Unit
) {
    var selectedSymbol by remember { mutableStateOf(preselectedSymbol ?: coins.firstOrNull()?.symbol ?: "BTC") }
    var selectedCondition by remember { mutableStateOf("ABOVE") }
    var noteInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val currentCoin = remember(selectedSymbol, coins) {
        coins.find { it.symbol == selectedSymbol } ?: coins.firstOrNull()
    }
    val currentPrice = currentCoin?.currentPrice ?: 1000.0

    var targetInput by remember(currentPrice, selectedCondition) {
        val defaultTarget = when (selectedCondition) {
            "ABOVE" -> currentPrice * 1.05
            "BELOW" -> currentPrice * 0.95
            "PCT_UP" -> 5.0
            "PCT_DOWN" -> 5.0
            else -> currentPrice * 1.05
        }
        mutableStateOf(if (selectedCondition.startsWith("PCT")) String.format("%.1f", defaultTarget) else String.format("%.2f", defaultTarget))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = DarkBorder) },
        modifier = Modifier.testTag("create_alert_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = WarningAmber)
                    Text(
                        text = "New Price Alert",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_alert_sheet")) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Select Asset
            Text("Select Cryptocurrency", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            ScrollableTabRow(
                selectedTabIndex = coins.indexOfFirst { it.symbol == selectedSymbol }.coerceAtLeast(0),
                edgePadding = 0.dp,
                containerColor = DarkSurfaceVariant,
                indicator = {},
                divider = {}
            ) {
                coins.forEach { coin ->
                    Tab(
                        selected = coin.symbol == selectedSymbol,
                        onClick = {
                            selectedSymbol = coin.symbol
                            targetInput = String.format("%.2f", coin.currentPrice * 1.05)
                        },
                        text = {
                            Text(
                                coin.symbol,
                                fontWeight = if (coin.symbol == selectedSymbol) FontWeight.Bold else FontWeight.Normal,
                                color = if (coin.symbol == selectedSymbol) NeonCyan else TextSecondary
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Current Price Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Live Market Price:", fontSize = 12.sp, color = TextSecondary)
                    Text(
                        "\$${String.format("%,.2f", currentPrice)}",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Alert Condition Trigger
            Text("Trigger Condition", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedCondition == "ABOVE",
                        onClick = {
                            selectedCondition = "ABOVE"
                            targetInput = String.format("%.2f", currentPrice * 1.05)
                        },
                        label = { Text("Price Rises Above") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BullGreen.copy(alpha = 0.2f),
                            selectedLabelColor = BullGreen
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedCondition == "BELOW",
                        onClick = {
                            selectedCondition = "BELOW"
                            targetInput = String.format("%.2f", currentPrice * 0.95)
                        },
                        label = { Text("Price Drops Below") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BearRed.copy(alpha = 0.2f),
                            selectedLabelColor = BearRed
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedCondition == "PCT_UP",
                        onClick = {
                            selectedCondition = "PCT_UP"
                            targetInput = "5.0"
                        },
                        label = { Text("+ % Surge") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WarningAmber.copy(alpha = 0.2f),
                            selectedLabelColor = WarningAmber
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedCondition == "PCT_DOWN",
                        onClick = {
                            selectedCondition = "PCT_DOWN"
                            targetInput = "5.0"
                        },
                        label = { Text("- % Dip") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WarningAmber.copy(alpha = 0.2f),
                            selectedLabelColor = WarningAmber
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Target Input
            OutlinedTextField(
                value = targetInput,
                onValueChange = {
                    targetInput = it
                    errorMessage = null
                },
                label = {
                    Text(if (selectedCondition.startsWith("PCT")) "Threshold Percentage (%)" else "Target Price (USD)")
                },
                prefix = {
                    Text(if (selectedCondition.startsWith("PCT")) "% " else "\$ ", color = TextSecondary)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("alert_target_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WarningAmber,
                    unfocusedBorderColor = DarkBorder
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Preset Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    Pair("+3%", currentPrice * 1.03),
                    Pair("+5%", currentPrice * 1.05),
                    Pair("+10%", currentPrice * 1.10),
                    Pair("-5%", currentPrice * 0.95),
                    Pair("-10%", currentPrice * 0.90)
                ).forEach { (lbl, valPrice) ->
                    AssistChip(
                        onClick = {
                            if (selectedCondition.startsWith("PCT")) {
                                targetInput = lbl.replace("+", "").replace("-", "").replace("%", "")
                            } else {
                                targetInput = String.format("%.2f", valPrice)
                                selectedCondition = if (lbl.startsWith("+")) "ABOVE" else "BELOW"
                            }
                        },
                        label = { Text(lbl, fontSize = 11.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = DarkSurfaceElevated,
                            labelColor = if (lbl.startsWith("+")) BullGreen else BearRed
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Alert Note
            OutlinedTextField(
                value = noteInput,
                onValueChange = { noteInput = it },
                label = { Text("Custom Note (Optional)") },
                placeholder = { Text("e.g. Breakout retest / Take profit") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("alert_note_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = DarkBorder
                ),
                singleLine = true
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorMessage ?: "", color = BearRed, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    val targetNum = targetInput.toDoubleOrNull()
                    if (targetNum == null || targetNum <= 0) {
                        errorMessage = "Please enter a valid positive target"
                        return@Button
                    }
                    onSaveAlert(selectedSymbol, selectedCondition, targetNum, noteInput)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_alert_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WarningAmber,
                    contentColor = DarkBackground
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Set Real-Time Price Alert", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
