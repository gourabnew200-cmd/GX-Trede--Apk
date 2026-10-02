package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.example.data.model.OrderType
import com.example.data.model.TradeAction
import com.example.ui.theme.*
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeExecutionSheet(
    coin: CryptoMarketItem,
    cashBalance: Double,
    holdingQuantity: Double,
    onDismiss: () -> Unit,
    onExecuteTrade: (action: TradeAction, orderType: OrderType, quantity: Double, price: Double) -> Unit
) {
    var selectedAction by remember { mutableStateOf(TradeAction.BUY) }
    var selectedOrderType by remember { mutableStateOf(OrderType.MARKET) }
    var amountInput by remember { mutableStateOf("") }
    var limitPriceInput by remember(coin.currentPrice) { mutableStateOf(String.format("%.2f", coin.currentPrice)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val effectivePrice = remember(selectedOrderType, limitPriceInput, coin.currentPrice) {
        if (selectedOrderType == OrderType.LIMIT) {
            limitPriceInput.toDoubleOrNull() ?: coin.currentPrice
        } else {
            coin.currentPrice
        }
    }

    val quantity = remember(amountInput) {
        amountInput.toDoubleOrNull() ?: 0.0
    }

    val totalUsd = remember(quantity, effectivePrice) {
        quantity * effectivePrice
    }

    val feeUsd = remember(totalUsd) {
        totalUsd * 0.001
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = DarkBorder) },
        modifier = Modifier.testTag("trade_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Trade ${coin.name} (${coin.symbol})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Market Price: \$${String.format("%,.2f", coin.currentPrice)}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_trade_sheet")) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Buy / Sell Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = {
                        selectedAction = TradeAction.BUY
                        errorMessage = null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_buy_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedAction == TradeAction.BUY) BullGreen else Color.Transparent,
                        contentColor = if (selectedAction == TradeAction.BUY) DarkBackground else TextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("BUY", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        selectedAction = TradeAction.SELL
                        errorMessage = null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_sell_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedAction == TradeAction.SELL) BearRed else Color.Transparent,
                        contentColor = if (selectedAction == TradeAction.SELL) TextPrimary else TextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SELL", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Order Type Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedOrderType == OrderType.MARKET,
                    onClick = { selectedOrderType = OrderType.MARKET },
                    label = { Text("Market Order") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                        selectedLabelColor = NeonCyan
                    ),
                    modifier = Modifier.testTag("order_type_market")
                )
                FilterChip(
                    selected = selectedOrderType == OrderType.LIMIT,
                    onClick = { selectedOrderType = OrderType.LIMIT },
                    label = { Text("Limit Order") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                        selectedLabelColor = NeonCyan
                    ),
                    modifier = Modifier.testTag("order_type_limit")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Limit Price Input if limit order
            if (selectedOrderType == OrderType.LIMIT) {
                OutlinedTextField(
                    value = limitPriceInput,
                    onValueChange = { limitPriceInput = it },
                    label = { Text("Limit Price (USD)") },
                    prefix = { Text("\$ ", color = TextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("limit_price_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkBorder
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Quantity Input
            OutlinedTextField(
                value = amountInput,
                onValueChange = {
                    amountInput = it
                    errorMessage = null
                },
                label = { Text("Amount (${coin.symbol})") },
                suffix = { Text(coin.symbol, color = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trade_quantity_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (selectedAction == TradeAction.BUY) BullGreen else BearRed,
                    unfocusedBorderColor = DarkBorder
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Percentage Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(0.25 to "25%", 0.50 to "50%", 0.75 to "75%", 1.0 to "100%").forEach { (pct, label) ->
                    OutlinedButton(
                        onClick = {
                            if (selectedAction == TradeAction.BUY) {
                                val usableCash = cashBalance * pct
                                val calcQty = usableCash / max(0.0001, effectivePrice)
                                amountInput = String.format("%.4f", calcQty)
                            } else {
                                val calcQty = holdingQuantity * pct
                                amountInput = String.format("%.4f", calcQty)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .testTag("pct_button_$label"),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Text(label, fontSize = 12.sp, color = TextPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Balance & Estimation Summary Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (selectedAction == TradeAction.BUY) "Available Cash:" else "Available ${coin.symbol}:",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = if (selectedAction == TradeAction.BUY)
                                "\$${String.format("%,.2f", cashBalance)}"
                            else
                                "${String.format("%.4f", holdingQuantity)} ${coin.symbol}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Est. Value:", fontSize = 12.sp, color = TextSecondary)
                        Text(
                            "\$${String.format("%,.2f", totalUsd)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Est. Fee (0.1%):", fontSize = 11.sp, color = TextMuted)
                        Text(
                            "\$${String.format("%.2f", feeUsd)}",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            // Error display
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage ?: "",
                    color = BearRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Execution Button
            Button(
                onClick = {
                    if (quantity <= 0.0) {
                        errorMessage = "Please enter a valid amount greater than 0"
                        return@Button
                    }
                    if (selectedAction == TradeAction.BUY && (totalUsd + feeUsd) > cashBalance) {
                        errorMessage = "Insufficient USD cash balance to complete order"
                        return@Button
                    }
                    if (selectedAction == TradeAction.SELL && quantity > holdingQuantity) {
                        errorMessage = "Cannot sell more than available holdings (${String.format("%.4f", holdingQuantity)})"
                        return@Button
                    }
                    onExecuteTrade(selectedAction, selectedOrderType, quantity, effectivePrice)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("confirm_order_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedAction == TradeAction.BUY) BullGreen else BearRed,
                    contentColor = if (selectedAction == TradeAction.BUY) DarkBackground else TextPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (selectedAction == TradeAction.BUY)
                        "Confirm Buy ${coin.symbol} (\$${String.format("%,.2f", totalUsd)})"
                    else
                        "Confirm Sell ${coin.symbol} (\$${String.format("%,.2f", totalUsd)})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
