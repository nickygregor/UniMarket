package com.unimarket.presentation.auth

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import com.unimarket.presentation.AuthViewModel
import com.unimarket.presentation.UiState

// ── Brand Colors ──────────────────────────────────────────────────────────────
val UniBlue   = Color(0xFF1A73E8)
val UniNavy   = Color(0xFF0D1B2A)
val UniAccent = Color(0xFF00C896)

// ════════════════════════════════════════════════════════════════════
//  LOGIN SCREEN
// ════════════════════════════════════════════════════════════════════

@Composable
fun LoginScreen(
    viewModel  : AuthViewModel,
    onSuccess  : (role: String) -> Unit,
    onRegister : () -> Unit
) {
    val state by viewModel.authState.collectAsState()
    var userId   by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPwd  by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state) {
        if (state is UiState.Success) {
            val role = (state as UiState.Success).data.user.role
            onSuccess(role)
            viewModel.resetState()
        }
        if (state is UiState.Error) {
            errorMsg = (state as UiState.Error).msg
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(UniNavy, Color(0xFF1C2B3A))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Logo / Title
            Icon(
                imageVector = Icons.Filled.Store,
                contentDescription = null,
                tint    = UniAccent,
                modifier = Modifier.size(72.dp)
            )
            Text("UniMarket", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Your campus marketplace", fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f))

            Spacer(Modifier.height(8.dp))

            // User ID field
            UniTextField(
                value        = userId,
                onValueChange = { userId = it; errorMsg = null },
                label        = "User ID",
                leadingIcon  = Icons.Filled.Person
            )

            // Password field
            UniTextField(
                value         = password,
                onValueChange = { password = it; errorMsg = null },
                label         = "Password",
                leadingIcon   = Icons.Filled.Lock,
                isPassword    = true,
                showPassword  = showPwd,
                onTogglePwd   = { showPwd = !showPwd }
            )

            // Error
            AnimatedVisibility(visible = errorMsg != null) {
                Text(
                    text     = errorMsg ?: "",
                    color    = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }

            // Login Button
            Button(
                onClick  = { viewModel.login(userId.trim(), password) },
                enabled  = userId.isNotBlank() && password.isNotBlank() && state !is UiState.Loading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = UniAccent),
                shape    = RoundedCornerShape(12.dp)
            ) {
                if (state is UiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            TextButton(onClick = onRegister) {
                Text("Don't have an account? Register", color = UniBlue)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
//  REGISTER SCREEN
// ════════════════════════════════════════════════════════════════════

@Composable
fun RegisterScreen(
    viewModel : AuthViewModel,
    onSuccess : (role: String) -> Unit,
    onLogin   : () -> Unit
) {
    val state by viewModel.authState.collectAsState()
    var firstName by remember { mutableStateOf("") }
    var lastName  by remember { mutableStateOf("") }
    var email     by remember { mutableStateOf("") }
    var phone     by remember { mutableStateOf("") }
    var userId    by remember { mutableStateOf("") }
    var password  by remember { mutableStateOf("") }
    var role      by remember { mutableStateOf("BUYER") }
    var showPwd   by remember { mutableStateOf(false) }
    var errorMsg  by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state) {
        if (state is UiState.Success) {
            onSuccess((state as UiState.Success).data.user.role)
            viewModel.resetState()
        }
        if (state is UiState.Error) errorMsg = (state as UiState.Error).msg
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(UniNavy, Color(0xFF1C2B3A))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(32.dp))
            Text("Create Account", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Join UniMarket today", fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f))
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UniTextField(firstName, { firstName = it; errorMsg = null }, "First Name", Icons.Filled.Person, Modifier.weight(1f))
                UniTextField(lastName,  { lastName  = it; errorMsg = null }, "Last Name",  Icons.Filled.Person, Modifier.weight(1f))
            }
            UniTextField(email,    { email  = it; errorMsg = null }, "Email",        Icons.Filled.Email,  keyboardType = KeyboardType.Email)
            UniTextField(phone,    { phone  = it; errorMsg = null }, "Phone Number", Icons.Filled.Phone,  keyboardType = KeyboardType.Phone)
            UniTextField(userId,   { userId = it; errorMsg = null }, "User ID",      Icons.Filled.Badge)
            UniTextField(
                value         = password,
                onValueChange = { password = it; errorMsg = null },
                label         = "Password",
                leadingIcon   = Icons.Filled.Lock,
                isPassword    = true,
                showPassword  = showPwd,
                onTogglePwd   = { showPwd = !showPwd }
            )

            // Role selector
            Text("I am a:", color = Color.White, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("BUYER", "SELLER").forEach { r ->
                    FilterChip(
                        selected = role == r,
                        onClick  = { role = r },
                        label    = { Text(r.lowercase().replaceFirstChar { it.uppercase() }) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = UniAccent,
                            selectedLabelColor     = Color.White
                        )
                    )
                }
            }

            AnimatedVisibility(visible = errorMsg != null) {
                Text(errorMsg ?: "", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            Button(
                onClick  = { viewModel.register(firstName, lastName, email, phone, userId, password, role) },
                enabled  = listOf(firstName, lastName, email, userId, password).all { it.isNotBlank() } && state !is UiState.Loading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = UniAccent),
                shape    = RoundedCornerShape(12.dp)
            ) {
                if (state is UiState.Loading)
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else
                    Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            TextButton(onClick = onLogin) {
                Text("Already have an account? Sign in", color = UniBlue)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ════════════════════════════════════════════════════════════════════
//  SHARED UI COMPONENT
// ════════════════════════════════════════════════════════════════════

@Composable
fun UniTextField(
    value          : String,
    onValueChange  : (String) -> Unit,
    label          : String,
    leadingIcon    : androidx.compose.ui.graphics.vector.ImageVector,
    modifier       : Modifier    = Modifier.fillMaxWidth(),
    isPassword     : Boolean     = false,
    showPassword   : Boolean     = false,
    onTogglePwd    : (() -> Unit)? = null,
    keyboardType   : KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label, color = Color.White.copy(alpha = 0.7f)) },
        leadingIcon   = { Icon(leadingIcon, contentDescription = null, tint = UniAccent) },
        trailingIcon  = if (isPassword) ({
            IconButton(onClick = { onTogglePwd?.invoke() }) {
                Icon(
                    if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = null, tint = Color.White.copy(alpha = 0.6f)
                )
            }
        }) else null,
        visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions      = KeyboardOptions(keyboardType = keyboardType),
        singleLine           = true,
        modifier             = modifier,
        colors               = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = UniAccent,
            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
            focusedTextColor     = Color.White,
            unfocusedTextColor   = Color.White,
            cursorColor          = UniAccent
        ),
        shape = RoundedCornerShape(12.dp)
    )
}
