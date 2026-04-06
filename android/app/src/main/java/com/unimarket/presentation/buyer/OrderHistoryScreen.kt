package com.unimarket.presentation.buyer

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.unimarket.domain.model.Order
import com.unimarket.presentation.BuyerViewModel
import com.unimarket.presentation.UiState
import com.unimarket.presentation.auth.UniAccent
import com.unimarket.presentation.auth.UniNavy
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    viewModel : BuyerViewModel,
    onBack    : () -> Unit
) {
    val ordersState by viewModel.orders.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadOrders() }

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        topBar = {
            TopAppBar(
                title  = { Text("My Orders", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = UniNavy, titleContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        when (val s = ordersState) {
            is UiState.Loading -> Box(
                Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = UniAccent) }

            is UiState.Error -> ErrorCard(s.msg) { viewModel.loadOrders() }

            is UiState.Success -> {
                if (s.data.isEmpty()) {
                    EmptyState("No orders yet", Icons.Filled.ShoppingBag)
                } else {
                    LazyColumn(
                        modifier            = Modifier.padding(padding),
                        contentPadding      = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(s.data, key = { it.id }) { order ->
                            OrderCard(order = order)
                        }
                    }
                }
            }
            else -> Unit
        }
    }
}

@Composable
fun OrderCard(order: Order) {
    var expanded by remember { mutableStateOf(false) }

    val dateStr = remember(order.createdAt) {
        SimpleDateFormat("MMM dd, yyyy  hh:mm a", Locale.US).format(Date(order.createdAt))
    }

    Card(
        onClick    = { expanded = !expanded },
        modifier   = Modifier.fillMaxWidth(),
        elevation  = CardDefaults.cardElevation(3.dp),
        colors     = CardDefaults.cardColors(containerColor = Color.White),
        shape      = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Order #${order.id}",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp
                    )
                    Text(dateStr, fontSize = 11.sp, color = Color.Gray)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = UniAccent.copy(alpha = 0.12f)
                    ) {
                        Text(
                            order.status,
                            modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color      = UniAccent,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        null, tint = Color.Gray, modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Summary line (always visible)
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${order.items.size} item${if (order.items.size != 1) "s" else ""}",
                    fontSize = 13.sp, color = Color.Gray
                )
                Text(
                    fmt.format(order.totalAmount),
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = UniAccent
                )
            }

            // Expanded item breakdown
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 10.dp))
                    order.items.forEach { item ->
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${item.title} ×${item.quantity}",
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            Text(
                                fmt.format(item.subtotal),
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = Color(0xFF4A5568)
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total", fontWeight = FontWeight.Bold)
                        Text(
                            fmt.format(order.totalAmount),
                            fontWeight = FontWeight.Bold,
                            color      = UniAccent
                        )
                    }
                }
            }
        }
    }
}
