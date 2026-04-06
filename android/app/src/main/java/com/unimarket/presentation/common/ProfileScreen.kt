package com.unimarket.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.unimarket.domain.model.User
import com.unimarket.presentation.auth.UniAccent
import com.unimarket.presentation.auth.UniNavy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user     : User,
    onLogout : () -> Unit,
    onBack   : () -> Unit
) {
    val roleColor = when (user.role) {
        "ADMIN"  -> Color(0xFF9C27B0)
        "SELLER" -> Color(0xFFFF9800)
        else     -> UniAccent
    }
    val roleEmoji = when (user.role) {
        "ADMIN"  -> "⚙️"
        "SELLER" -> "🏪"
        else     -> "🛍"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("My Profile", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Avatar header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(UniNavy, Color(0xFF1C2B3A))))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(88.dp),
                        shape    = RoundedCornerShape(44.dp),
                        color    = roleColor.copy(alpha = 0.25f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "${user.firstName.firstOrNull() ?: ""}${user.lastName.firstOrNull() ?: ""}",
                                fontSize   = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = roleColor
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "${user.firstName} ${user.lastName}",
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = roleColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            "$roleEmoji ${user.role}",
                            modifier   = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                            color      = roleColor,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Info rows
            Column(modifier = Modifier.padding(20.dp)) {
                ProfileInfoRow(Icons.Filled.Badge,   "User ID",  user.userId)
                ProfileInfoRow(Icons.Filled.Email,   "Email",    user.email)
                ProfileInfoRow(Icons.Filled.Phone,   "Phone",    user.phoneNumber)
                ProfileInfoRow(
                    if (user.isActive) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                    "Account Status",
                    if (user.isActive) "Active" else "Inactive",
                    if (user.isActive) UniAccent else Color.Red
                )

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick  = onLogout,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFEBEB),
                        contentColor   = Color.Red
                    ),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Logout, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon  : ImageVector,
    label : String,
    value : String,
    valueColor: Color = Color(0xFF1A202C)
) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape     = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier              = Modifier.padding(14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(icon, null, tint = UniAccent, modifier = Modifier.size(20.dp))
            Column {
                Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                Text(value, fontSize = 14.sp, color = valueColor, fontWeight = FontWeight.Medium)
            }
        }
    }
}
