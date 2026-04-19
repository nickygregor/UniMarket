package com.unimarket.presentation.common

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.*
import com.unimarket.domain.model.User
import com.unimarket.presentation.UiState
import com.unimarket.presentation.auth.UniAccent
import com.unimarket.presentation.auth.UniNavy
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: User,
    darkModeEnabled: Boolean,
    profileImage: String?,
    passwordState: UiState<Unit>,
    sellerCommentCount: Int = 0,
    sellerUnreadMessageCount: Int = 0,
    onSellerCommentsClick: () -> Unit = {},
    onSellerMessagesClick: () -> Unit = {},
    onDarkModeChange: (Boolean) -> Unit,
    onProfileImageChange: (String) -> Unit,
    onProfileImageRemove: () -> Unit,
    onChangePassword: (currentPassword: String, newPassword: String) -> Unit,
    onResetPasswordState: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showPasswordDialog by remember { mutableStateOf(false) }
    var imageBusy by remember { mutableStateOf(false) }
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

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imageBusy = true
            scope.launch {
                uriToProfileImageData(context, uri)?.let(onProfileImageChange)
                imageBusy = false
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            imageBusy = true
            scope.launch {
                bitmapToProfileImageData(bitmap)?.let(onProfileImageChange)
                imageBusy = false
            }
        }
    }

    if (showPasswordDialog) {
        ChangePasswordDialog(
            passwordState = passwordState,
            onChangePassword = onChangePassword,
            onDismiss = {
                showPasswordDialog = false
                onResetPasswordState()
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            // Avatar header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                UniNavy,
                                if (darkModeEnabled) Color(0xFF07111D) else Color(0xFF1C2B3A)
                            )
                        )
                    )
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(96.dp),
                        shape    = CircleShape,
                        color    = roleColor.copy(alpha = 0.25f)
                    ) {
                        ProfileImage(
                            imageData = profileImage,
                            contentDescription = "Profile picture",
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(
                                    "${user.firstName.firstOrNull() ?: ""}${user.lastName.firstOrNull() ?: ""}",
                                    fontSize   = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color      = roleColor
                                )
                            }
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
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            enabled = !imageBusy,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.White.copy(alpha = 0.14f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Gallery", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        FilledTonalButton(
                            onClick = { cameraLauncher.launch(null) },
                            enabled = !imageBusy,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.White.copy(alpha = 0.14f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Filled.PhotoCamera, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Camera", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (!profileImage.isNullOrBlank()) {
                        TextButton(onClick = onProfileImageRemove, enabled = !imageBusy) {
                            Text("Remove profile picture", color = Color.White)
                        }
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

                Spacer(Modifier.height(18.dp))

                if (user.role in listOf("SELLER", "BUYER_SELLER")) {
                    SettingsActionRow(
                        icon = Icons.Filled.QuestionAnswer,
                        label = "Listing Comments",
                        value = if (sellerCommentCount == 0) {
                            "No buyer questions yet"
                        } else {
                            "$sellerCommentCount buyer question${if (sellerCommentCount == 1) "" else "s"}"
                        },
                        onClick = onSellerCommentsClick
                    )

                    SettingsActionRow(
                        icon = Icons.Filled.MarkUnreadChatAlt,
                        label = "Private Messages",
                        value = if (sellerUnreadMessageCount == 0) {
                            "No unread seller messages"
                        } else {
                            "$sellerUnreadMessageCount unread message${if (sellerUnreadMessageCount == 1) "" else "s"}"
                        },
                        onClick = onSellerMessagesClick
                    )
                }

                SettingsActionRow(
                    icon = Icons.Filled.Lock,
                    label = "Security",
                    value = "Change password",
                    onClick = {
                        onResetPasswordState()
                        showPasswordDialog = true
                    }
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(1.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = if (darkModeEnabled) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                            contentDescription = null,
                            tint = UniAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Appearance",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (darkModeEnabled) "Dark Mode" else "Light Mode",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Switch(
                            checked = darkModeEnabled,
                            onCheckedChange = onDarkModeChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = UniAccent,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }

                SettingsActionRow(
                    icon = Icons.Filled.Notifications,
                    label = "Notifications",
                    value = "Marketplace alerts coming soon",
                    enabled = false,
                    onClick = {}
                )

                SettingsActionRow(
                    icon = Icons.Filled.PrivacyTip,
                    label = "Privacy",
                    value = "Your profile photo is stored on this device",
                    enabled = false,
                    onClick = {}
                )

                Spacer(Modifier.height(16.dp))

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
private fun SettingsActionRow(
    icon: ImageVector,
    label: String,
    value: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(icon, null, tint = UniAccent, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = value,
                    fontSize = 14.sp,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = FontWeight.Medium
                )
            }
            if (enabled) {
                Icon(
                    Icons.Filled.ChevronRight,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ChangePasswordDialog(
    passwordState: UiState<Unit>,
    onChangePassword: (currentPassword: String, newPassword: String) -> Unit,
    onDismiss: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPasswords by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(passwordState) {
        if (passwordState is UiState.Success) {
            currentPassword = ""
            newPassword = ""
            confirmPassword = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                        localError = null
                    },
                    label = { Text("Current password") },
                    leadingIcon = { Icon(Icons.Filled.Lock, null, tint = UniAccent) },
                    visualTransformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    enabled = passwordState !is UiState.Loading
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        localError = null
                    },
                    label = { Text("New password") },
                    leadingIcon = { Icon(Icons.Filled.Password, null, tint = UniAccent) },
                    visualTransformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    enabled = passwordState !is UiState.Loading
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        localError = null
                    },
                    label = { Text("Confirm new password") },
                    leadingIcon = { Icon(Icons.Filled.VerifiedUser, null, tint = UniAccent) },
                    visualTransformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    enabled = passwordState !is UiState.Loading
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = showPasswords,
                        onCheckedChange = { showPasswords = it },
                        colors = CheckboxDefaults.colors(checkedColor = UniAccent)
                    )
                    Text("Show passwords", fontSize = 13.sp)
                }
                localError?.let {
                    Text(it, color = Color.Red, fontSize = 13.sp)
                }
                when (passwordState) {
                    is UiState.Error -> Text(passwordState.msg, color = Color.Red, fontSize = 13.sp)
                    is UiState.Success -> Text("Password changed successfully.", color = UniAccent, fontSize = 13.sp)
                    else -> Unit
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank() -> {
                            localError = "Please fill in all password fields."
                        }
                        newPassword.length < 8 -> {
                            localError = "New password must be at least 8 characters."
                        }
                        newPassword != confirmPassword -> {
                            localError = "New password and confirmation do not match."
                        }
                        else -> {
                            localError = null
                            onChangePassword(currentPassword, newPassword)
                        }
                    }
                },
                enabled = passwordState !is UiState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = UniAccent)
            ) {
                if (passwordState is UiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Update")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (passwordState is UiState.Success) "Done" else "Cancel")
            }
        }
    )
}

@Composable
private fun ProfileInfoRow(
    icon  : ImageVector,
    label : String,
    value : String,
    valueColor: Color? = null
) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                Text(
                    label,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    value,
                    fontSize = 14.sp,
                    color = valueColor ?: MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
