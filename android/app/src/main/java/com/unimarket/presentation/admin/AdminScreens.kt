package com.unimarket.presentation.admin

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
    viewModel : AdminViewModel,
    onLogout  : () -> Unit
) {
    val usersState  by viewModel.users.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadUsers() }
    LaunchedEffect(Unit) {
        viewModel.toast.collect { msg -> snackbarHost.showSnackbar(msg) }
    }

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        snackbarHost   = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title  = { Text("Admin Panel", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = UniNavy, titleContentColor = Color.White),
                actions = {
                    IconButton(onClick = onLogout) { Icon(Icons.Filled.Logout, null, tint = Color.White) }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            // Summary cards
            when (val s = usersState) {
                is UiState.Success -> {
                    AdminSummaryBar(users = s.data)
                }
                else -> Unit
            }

            Text(
                "User Management",
                fontWeight = FontWeight.Bold,
                fontSize   = 18.sp,
                modifier   = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            when (val s = usersState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = UniAccent)
                }
                is UiState.Error   -> ErrorCard(s.msg) { viewModel.loadUsers() }
                is UiState.Success -> {
                    if (s.data.isEmpty()) {
                        EmptyState("No users found", Icons.Filled.People)
                    } else {
                        LazyColumn(
                            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(s.data, key = { it.id }) { user ->
                                AdminUserCard(
                                    user     = user,
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
}

@Composable
fun AdminSummaryBar(users: List<User>) {
    val total    = users.size
    val active   = users.count { it.isActive }
    val buyers   = users.count { it.role == "BUYER" }
    val sellers  = users.count { it.role == "SELLER" }

    Row(
        modifier              = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SummaryCard("Total",   "$total",   Icons.Filled.People,       Color(0xFF1A73E8), Modifier.weight(1f))
        SummaryCard("Active",  "$active",  Icons.Filled.CheckCircle,  UniAccent,        Modifier.weight(1f))
        SummaryCard("Buyers",  "$buyers",  Icons.Filled.ShoppingBag,  Color(0xFF9C27B0), Modifier.weight(1f))
        SummaryCard("Sellers", "$sellers", Icons.Filled.Store,        Color(0xFFFF9800), Modifier.weight(1f))
    }
}

@Composable
fun SummaryCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier) {
    Card(
        modifier  = modifier,
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp),
        shape     = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier            = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
            Text(label, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun AdminUserCard(user: User, onToggle: () -> Unit) {
    val roleColor = when (user.role) {
        "ADMIN"  -> Color(0xFF9C27B0)
        "SELLER" -> Color(0xFFFF9800)
        else     -> Color(0xFF1A73E8)
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        shape     = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier              = Modifier.padding(14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar circle
            Surface(
                modifier = Modifier.size(46.dp),
                shape    = RoundedCornerShape(23.dp),
                color    = roleColor.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "${user.firstName.firstOrNull() ?: "?"}${user.lastName.firstOrNull() ?: ""}",
                        fontWeight = FontWeight.Bold,
                        color      = roleColor,
                        fontSize   = 16.sp
                    )
                }
            }

            Column(Modifier.weight(1f)) {
                Text("${user.firstName} ${user.lastName}", fontWeight = FontWeight.SemiBold)
                Text(user.email, fontSize = 12.sp, color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(
                        onClick = {},
                        label   = { Text(user.role, fontSize = 11.sp) },
                        colors  = AssistChipDefaults.assistChipColors(containerColor = roleColor.copy(alpha = 0.1f))
                    )
                    if (!user.isActive) {
                        AssistChip(
                            onClick = {},
                            label   = { Text("INACTIVE", fontSize = 11.sp) },
                            colors  = AssistChipDefaults.assistChipColors(containerColor = Color.Red.copy(alpha = 0.1f))
                        )
                    }
                }
            }

            // Don't allow toggling the admin account
            if (user.role != "ADMIN") {
                Switch(
                    checked         = user.isActive,
                    onCheckedChange = { onToggle() },
                    colors          = SwitchDefaults.colors(checkedThumbColor = UniAccent, checkedTrackColor = UniAccent.copy(alpha = 0.4f))
                )
            }
        }
    }
}
