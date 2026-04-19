package com.unimarket.presentation.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unimarket.R
import com.unimarket.presentation.AuthViewModel
import com.unimarket.presentation.UiState
import com.unimarket.presentation.common.ProfileImage
import com.unimarket.presentation.common.bitmapToProfileImageData
import com.unimarket.presentation.common.uriToProfileImageData
import kotlinx.coroutines.launch

val UniBlue = Color(0xFF60A5FA)
val UniNavy = Color(0xFF0B1F33)
val UniNavyLight = Color(0xFF12314C)
val UniSurface = Color(0xFFFFFFFF)
val UniSurfaceBorder = Color(0x140B1F33)
val UniAccent = Color(0xFFF58025)
val UniTextMuted = Color(0xFF64748B)
val UniError = Color(0xFFDC2626)
val UniPage = Color(0xFFF7F8FA)
val UniTextPrimary = Color(0xFF132033)

private fun isValidUtaEmail(email: String): Boolean {
    return email.trim().lowercase()
        .matches(Regex("^[a-zA-Z0-9._%+-]+@mavs\\.uta\\.edu$"))
}

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onSuccess: () -> Unit,
    onRegister: () -> Unit
) {
    val state by viewModel.authState.collectAsState()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPwd by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state) {
        if (state is UiState.Success) {
            onSuccess()
            viewModel.resetState()
        }

        if (state is UiState.Error) {
            val msg = (state as UiState.Error).msg
            errorMsg = if (msg.contains("Failed to connect", ignoreCase = true)) {
                null
            } else {
                msg
            }
        }
    }

    AuthScreenContainer {
        AuthHeader(
            title = "UniMarket",
            subtitle = "Buy and sell across campus"
        )

        Spacer(Modifier.height(24.dp))

        AuthCard {
            Text(
                text = "Welcome back",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Sign in to continue",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            UniTextField(
                value = username,
                onValueChange = {
                    username = it
                    errorMsg = null
                },
                label = "Username",
                leadingIcon = Icons.Filled.Person
            )

            Spacer(Modifier.height(12.dp))

            UniTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMsg = null
                },
                label = "Password",
                leadingIcon = Icons.Filled.Lock,
                isPassword = true,
                showPassword = showPwd,
                onTogglePwd = { showPwd = !showPwd }
            )

            Spacer(Modifier.height(14.dp))

            AnimatedVisibility(visible = errorMsg != null) {
                Text(
                    text = errorMsg ?: "",
                    color = UniError,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (errorMsg != null) {
                Spacer(Modifier.height(10.dp))
            }

            Button(
                onClick = { viewModel.login(username.trim(), password) },
                enabled = username.isNotBlank() && password.isNotBlank() && state !is UiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = UniAccent,
                    disabledContainerColor = UniAccent.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (state is UiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Sign In",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            TextButton(
                onClick = onRegister,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Don't have an account? Register",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onSuccess: () -> Unit,
    onLogin: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.authState.collectAsState()

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPwd by remember { mutableStateOf(false) }
    var profileImage by remember { mutableStateOf<String?>(null) }
    var imageBusy by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imageBusy = true
            scope.launch {
                profileImage = uriToProfileImageData(context, uri)
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
                profileImage = bitmapToProfileImageData(bitmap)
                imageBusy = false
            }
        }
    }

    LaunchedEffect(state) {
        if (state is UiState.Success) {
            onSuccess()
            viewModel.resetState()
        }

        if (state is UiState.Error) {
            val msg = (state as UiState.Error).msg
            errorMsg = if (msg.contains("Failed to connect", ignoreCase = true)) {
                null
            } else {
                msg
            }
        }
    }

    AuthScreenContainer(scrollable = true) {
        AuthHeader(
            title = "Create Account",
            subtitle = "Join the UTA marketplace"
        )

        Spacer(Modifier.height(24.dp))

        AuthCard {
            Text(
                text = "Student sign up",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "One account can both buy and sell",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    color = UniAccent.copy(alpha = 0.16f)
                ) {
                    ProfileImage(
                        imageData = profileImage,
                        contentDescription = "Selected profile picture",
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Filled.Person, null, tint = UniAccent, modifier = Modifier.size(34.dp))
                        }
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Profile picture",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Optional, you can change it later.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = {
                                galleryLauncher.launch("image/*")
                                errorMsg = null
                            },
                            enabled = !imageBusy && state !is UiState.Loading,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = UniAccent.copy(alpha = 0.15f),
                                contentColor = UniAccent
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Gallery", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        FilledTonalButton(
                            onClick = {
                                cameraLauncher.launch(null)
                                errorMsg = null
                            },
                            enabled = !imageBusy && state !is UiState.Loading,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = UniAccent.copy(alpha = 0.15f),
                                contentColor = UniAccent
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.PhotoCamera, null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Camera", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (profileImage != null) {
                        TextButton(onClick = { profileImage = null }, enabled = state !is UiState.Loading) {
                            Text("Remove photo", color = Color.Red, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                UniTextField(
                    value = firstName,
                    onValueChange = {
                        firstName = it
                        errorMsg = null
                    },
                    label = "First Name",
                    leadingIcon = Icons.Filled.Person,
                    modifier = Modifier.weight(1f)
                )

                UniTextField(
                    value = lastName,
                    onValueChange = {
                        lastName = it
                        errorMsg = null
                    },
                    label = "Last Name",
                    leadingIcon = Icons.Filled.Person,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            UniTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMsg = null
                },
                label = "UTA Email",
                leadingIcon = Icons.Filled.Email,
                keyboardType = KeyboardType.Email
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "UTA students only: use your @mavs.uta.edu email",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            UniTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    errorMsg = null
                },
                label = "Phone Number",
                leadingIcon = Icons.Filled.Phone,
                keyboardType = KeyboardType.Phone
            )

            Spacer(Modifier.height(12.dp))

            UniTextField(
                value = username,
                onValueChange = {
                    username = it
                    errorMsg = null
                },
                label = "Username",
                leadingIcon = Icons.Filled.Badge
            )

            Spacer(Modifier.height(12.dp))

            UniTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMsg = null
                },
                label = "Password",
                leadingIcon = Icons.Filled.Lock,
                isPassword = true,
                showPassword = showPwd,
                onTogglePwd = { showPwd = !showPwd }
            )

            Spacer(Modifier.height(14.dp))

            AnimatedVisibility(visible = errorMsg != null) {
                Text(
                    text = errorMsg ?: "",
                    color = UniError,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (errorMsg != null) {
                Spacer(Modifier.height(10.dp))
            }

            Button(
                onClick = {
                    val trimmedEmail = email.trim()

                    when {
                        firstName.isBlank() ||
                                lastName.isBlank() ||
                                trimmedEmail.isBlank() ||
                                username.isBlank() ||
                                password.isBlank() -> {
                            errorMsg = "Please fill in all required fields."
                        }

                        !isValidUtaEmail(trimmedEmail) -> {
                            errorMsg = "Please use your UTA email ending with @mavs.uta.edu"
                        }

                        else -> {
                            errorMsg = null
                            viewModel.register(
                                firstName = firstName.trim(),
                                lastName = lastName.trim(),
                                email = trimmedEmail,
                                phone = phone.trim(),
                                userId = username.trim(),
                                password = password,
                                profileImage = profileImage
                            )
                        }
                    }
                },
                enabled = state !is UiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = UniAccent,
                    disabledContainerColor = UniAccent.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (state is UiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Create Account",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            TextButton(
                onClick = onLogin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Already have an account? Sign in",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun AuthScreenContainer(
    scrollable: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val baseModifier = Modifier
        .fillMaxSize()
        .background(
            Brush.verticalGradient(
                listOf(
                    colorScheme.surfaceVariant,
                    colorScheme.background,
                    colorScheme.primaryContainer.copy(alpha = 0.55f)
                )
            )
        )

    if (scrollable) {
        Column(
            modifier = baseModifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    } else {
        Column(
            modifier = baseModifier
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = content
        )
    }
}

@Composable
private fun AuthHeader(
    title: String,
    subtitle: String
) {
    Icon(
        painter = painterResource(id = R.drawable.unimarket_logo_mark),
        contentDescription = null,
        tint = Color.Unspecified,
        modifier = Modifier.size(86.dp)
    )

    Spacer(Modifier.height(14.dp))

    Text(
        text = title,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )

    Text(
        text = subtitle,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun AuthCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        ),
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            content = content
        )
    }
}

@Composable
fun UniTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier.fillMaxWidth(),
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePwd: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = UniAccent
            )
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { onTogglePwd?.invoke() }) {
                    Icon(
                        imageVector = if (showPassword) {
                            Icons.Filled.VisibilityOff
                        } else {
                            Icons.Filled.Visibility
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !showPassword) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = UniAccent,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = UniAccent,
            focusedLabelColor = UniAccent,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp)
    )
}
