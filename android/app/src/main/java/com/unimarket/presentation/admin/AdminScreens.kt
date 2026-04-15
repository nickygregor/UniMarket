package com.unimarket.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unimarket.domain.model.User
import com.unimarket.presentation.AdminViewModel
import com.unimarket.presentation.UiState
import com.unimarket.presentation.auth.UniAccent
import com.unimarket.presentation.auth.UniNavy
import com.unimarket.presentation.buyer.EmptyState
import com.unimarket.presentation.buyer.ErrorCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel,
    onLogout: () -> Unit
) {
    val usersState by viewModel.users.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadUsers() }
    LaunchedEffect(Unit) {
        viewModel.toast.collect { msg -> snackbarHost.showSnackbar(msg) }
    }

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = UniNavy,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Filled.Logout, null, tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        when (val state = usersState) {
            is UiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = UniAccent)
            }

            is UiState.Error -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                ErrorCard(state.msg) { viewModel.loadUsers() }
            }

            is UiState.Success -> {
                if (state.data.isEmpty()) {
                    EmptyState("No users found", Icons.Filled.People)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            AdminSummaryBar(users = state.data)
                        }

                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "User Management",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "Review each account, check how active they are in the marketplace, and toggle access when needed.",
                                        color = Color(0xFF6B7280),
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }

                        items(state.data, key = { it.id }) { user ->
                            AdminUserCard(
                                user = user,
                                onToggle = { viewModel.toggleUser(user.id, user.isActive) }
                            )
                        }
                    }
                }
            }

            else -> Unit
        }
    }
}

@Composable
fun AdminSummaryBar(users: List<User>) {
    val totalUsers = users.size
    val activeUsers = users.count { it.isActive }
    val totalListings = users.sumOf { it.listingsPosted }
    val totalItemsBought = users.sumOf { it.itemsBought }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryCard(
                label = "Users",
                value = "$totalUsers",
                caption = "Registered",
                icon = Icons.Filled.People,
                color = Color(0xFF2563EB),
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                label = "Active",
                value = "$activeUsers",
                caption = "Can sign in",
                icon = Icons.Filled.CheckCircle,
                color = UniAccent,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryCard(
                label = "Listings",
                value = "$totalListings",
                caption = "Posted so far",
                icon = Icons.Filled.Store,
                color = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                label = "Bought",
                value = "$totalItemsBought",
                caption = "Items purchased",
                icon = Icons.Filled.ShoppingBag,
                color = Color(0xFF8B5CF6),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SummaryCard(
    label: String,
    value: String,
    caption: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.12f)
            ) {
                Icon(
                    icon,
                    null,
                    tint = color,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = color)
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(caption, fontSize = 11.sp, color = Color(0xFF6B7280))
        }
    }
}

@Composable
fun AdminUserCard(user: User, onToggle: () -> Unit) {
    val roleColor = when (user.role) {
        "ADMIN" -> Color(0xFF8B5CF6)
        "SELLER" -> Color(0xFFF59E0B)
        "BUYER_SELLER" -> UniAccent
        else -> Color(0xFF2563EB)
    }

    val statusColor = if (user.isActive) UniAccent else Color(0xFFEF4444)
    val canToggle = user.role != "ADMIN"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(50.dp),
                    shape = CircleShape,
                    color = roleColor.copy(alpha = 0.14f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = buildString {
                                append(user.firstName.firstOrNull() ?: '?')
                                append(user.lastName.firstOrNull() ?: ' ')
                            }.trim(),
                            fontWeight = FontWeight.Bold,
                            color = roleColor,
                            fontSize = 16.sp
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${user.firstName} ${user.lastName}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = user.email,
                        color = Color(0xFF6B7280),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "User ID: ${user.userId}",
                        color = Color(0xFF9CA3AF),
                        fontSize = 11.sp
                    )
                }

                if (canToggle) {
                    Switch(
                        checked = user.isActive,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = UniAccent,
                            checkedTrackColor = UniAccent.copy(alpha = 0.4f)
                        )
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text(user.role, fontSize = 11.sp) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = roleColor.copy(alpha = 0.12f),
                        labelColor = roleColor
                    )
                )
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            if (user.isActive) "ACTIVE" else "INACTIVE",
                            fontSize = 11.sp
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = statusColor.copy(alpha = 0.12f),
                        labelColor = statusColor
                    )
                )
            }

            HorizontalDivider(color = Color(0xFFE5E7EB))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                UserStatPill(
                    label = "Listings Posted",
                    value = user.listingsPosted.toString(),
                    icon = Icons.Filled.Store,
                    modifier = Modifier.weight(1f)
                )
                UserStatPill(
                    label = "Live Ads",
                    value = user.activeListings.toString(),
                    icon = Icons.Filled.Inventory2,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                UserStatPill(
                    label = "Orders Placed",
                    value = user.ordersPlaced.toString(),
                    icon = Icons.Filled.ShoppingBag,
                    modifier = Modifier.weight(1f)
                )
                UserStatPill(
                    label = "Items Bought",
                    value = user.itemsBought.toString(),
                    icon = Icons.Filled.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = if (canToggle) {
                    if (user.isActive) "Turn this off to block login and marketplace activity for this account."
                    else "Turn this on to restore login and marketplace activity for this account."
                } else {
                    "Admin accounts stay protected from activation changes."
                },
                color = Color(0xFF6B7280),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun UserStatPill(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                null,
                tint = UniNavy,
                modifier = Modifier.size(16.dp)
            )
            Column {
                Text(
                    text = value,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = label,
                    color = Color(0xFF6B7280),
                    fontSize = 11.sp
                )
            }
        }
    }
}
