package com.unimarket.presentation.admin

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.unimarket.domain.model.Listing
import com.unimarket.domain.model.User
import com.unimarket.presentation.AdminViewModel
import com.unimarket.presentation.UiState
import com.unimarket.presentation.auth.UniAccent
import com.unimarket.presentation.auth.UniNavy
import com.unimarket.presentation.buyer.EmptyState
import com.unimarket.presentation.buyer.ErrorCard
import com.unimarket.presentation.buyer.fmt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit
) {
    val usersState by viewModel.users.collectAsState()
    val listingsState by viewModel.listings.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadAdminData() }
    LaunchedEffect(Unit) {
        viewModel.toast.collect { msg -> snackbarHost.showSnackbar(msg) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = UniNavy,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Filled.Person, null, tint = Color.White)
                    }
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
                ErrorCard(state.msg) { viewModel.loadAdminData() }
            }

            is UiState.Success -> {
                val listingsBySeller = (listingsState as? UiState.Success<List<Listing>>)
                    ?.data
                    .orEmpty()
                    .groupBy { it.sellerId }
                val listingsLoading = listingsState is UiState.Loading

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
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }

                        items(state.data, key = { it.id }) { user ->
                            AdminUserCard(
                                user = user,
                                listings = listingsBySeller[user.id].orEmpty(),
                                listingsLoading = listingsLoading,
                                onRemoveListing = { listing ->
                                    viewModel.deleteListing(listing.id)
                                },
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(caption, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AdminUserCard(
    user: User,
    listings: List<Listing>,
    listingsLoading: Boolean,
    onRemoveListing: (Listing) -> Unit,
    onToggle: () -> Unit
) {
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = user.email,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "User ID: ${user.userId}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
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

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Listed Items",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (listingsLoading) "Loading..." else "${listings.size} visible",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (listingsLoading) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = UniAccent,
                                strokeWidth = 2.dp
                            )
                            Text(
                                "Loading this user's listings",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else if (listings.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (user.listingsPosted > 0) {
                                "No active marketplace listings are visible right now."
                            } else {
                                "This user has not listed any items yet."
                            },
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    listings.forEach { listing ->
                        AdminListingCard(
                            listing = listing,
                            onRemove = { onRemoveListing(listing) }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun AdminListingCard(
    listing: Listing,
    onRemove: () -> Unit
) {
    var showConfirmRemove by remember { mutableStateOf(false) }

    if (showConfirmRemove) {
        AlertDialog(
            onDismissRequest = { showConfirmRemove = false },
            title = { Text("Remove listing?") },
            text = {
                Text(
                    "This will remove \"${listing.title}\" from the marketplace for every user."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmRemove = false
                        onRemove()
                    }
                ) {
                    Text("Remove", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmRemove = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AdminListingImage(
                imageUrl = firstListingImageUrl(listing.imageUrl),
                contentDescription = listing.title,
                modifier = Modifier
                    .size(74.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = listing.title,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = fmt.format(listing.price),
                        color = UniAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = listing.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    listing.category,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = UniAccent.copy(alpha = 0.14f),
                                labelColor = UniAccent
                            )
                        )
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    if (listing.isActive) "LIVE" else "INACTIVE",
                                    fontSize = 10.sp
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (listing.isActive) {
                                    UniAccent.copy(alpha = 0.14f)
                                } else {
                                    Color.Red.copy(alpha = 0.12f)
                                },
                                labelColor = if (listing.isActive) UniAccent else Color.Red
                            )
                        )
                    }

                    FilledTonalButton(
                        onClick = { showConfirmRemove = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.Red.copy(alpha = 0.12f),
                            contentColor = Color.Red
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Remove", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminListingImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val decodedBitmap = remember(imageUrl) {
        val raw = imageUrl?.trim().orEmpty()
        if (!raw.startsWith("data:image/", ignoreCase = true)) {
            null
        } else {
            val payload = raw.substringAfter("base64,", missingDelimiterValue = "")
            runCatching {
                val bytes = Base64.decode(payload, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
        }
    }

    if (decodedBitmap != null) {
        Image(
            bitmap = decodedBitmap.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        AsyncImage(
            model = imageUrl ?: "https://placehold.co/160x160/png",
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

private fun firstListingImageUrl(value: String?): String? {
    return value
        ?.lines()
        ?.map { it.trim() }
        ?.firstOrNull { it.isNotBlank() }
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
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
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
                tint = UniAccent,
                modifier = Modifier.size(16.dp)
            )
            Column {
                Text(
                    text = value,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}
