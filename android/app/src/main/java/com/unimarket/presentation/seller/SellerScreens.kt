package com.unimarket.presentation.seller

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.unimarket.domain.model.Listing
import com.unimarket.presentation.SellerViewModel
import com.unimarket.presentation.UiState
import com.unimarket.presentation.auth.UniAccent
import com.unimarket.presentation.auth.UniNavy
import com.unimarket.presentation.auth.UniTextField
import com.unimarket.presentation.buyer.EmptyState
import com.unimarket.presentation.buyer.ErrorCard
import com.unimarket.presentation.buyer.fmt

// ════════════════════════════════════════════════════════════════════
//  SELLER DASHBOARD - MY LISTINGS
// ════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerDashboardScreen(
    viewModel    : SellerViewModel,
    onAddListing : () -> Unit,
    onEdit       : (Listing) -> Unit,
    onLogout     : () -> Unit
) {
    val listingsState by viewModel.listings.collectAsState()
    val snackbarHost   = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadMyListings() }

    LaunchedEffect(Unit) {
        viewModel.toast.collect { msg -> snackbarHost.showSnackbar(msg) }
    }

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        snackbarHost   = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title  = { Text("My Listings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = UniNavy, titleContentColor = Color.White),
                actions = {
                    IconButton(onClick = onLogout) { Icon(Icons.Filled.Logout, null, tint = Color.White) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick           = onAddListing,
                containerColor    = UniAccent,
                contentColor      = Color.White
            ) { Icon(Icons.Filled.Add, "Add listing") }
        }
    ) { padding ->
        when (val s = listingsState) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = UniAccent) }
            is UiState.Error   -> ErrorCard(s.msg) { viewModel.loadMyListings() }
            is UiState.Success -> {
                if (s.data.isEmpty()) {
                    EmptyState("No listings yet. Tap + to add one!", Icons.Filled.Inventory)
                } else {
                    LazyColumn(
                        contentPadding      = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier            = Modifier.padding(padding)
                    ) {
                        items(s.data, key = { it.id }) { listing ->
                            SellerListingCard(
                                listing  = listing,
                                onEdit   = { onEdit(listing) },
                                onDelete = { viewModel.deleteListing(listing.id) }
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
fun SellerListingCard(listing: Listing, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title   = { Text("Delete Listing") },
            text    = { Text("Are you sure you want to delete \"${listing.title}\"?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        shape     = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier              = Modifier.padding(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model              = listing.imageUrl ?: "https://placehold.co/80x80/1A73E8/white?text=${listing.category[0]}",
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp))
            )
            Column(Modifier.weight(1f)) {
                Text(listing.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(listing.category, fontSize = 12.sp, color = Color.Gray)
                Text(fmt.format(listing.price), fontWeight = FontWeight.Bold, color = UniAccent)
                if (!listing.isActive) {
                    Text("INACTIVE", fontSize = 11.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, null, tint = UniNavy)
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Filled.Delete, null, tint = Color.Red.copy(alpha = 0.7f))
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
//  CREATE / EDIT LISTING SCREEN
// ════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditListingScreen(
    viewModel      : SellerViewModel,
    existingListing : Listing? = null,
    onBack         : () -> Unit
) {
    val actionResult by viewModel.actionResult.collectAsState()
    var title        by remember { mutableStateOf(existingListing?.title       ?: "") }
    var description  by remember { mutableStateOf(existingListing?.description ?: "") }
    var priceStr     by remember { mutableStateOf(existingListing?.price?.toString() ?: "") }
    var category     by remember { mutableStateOf(existingListing?.category    ?: "Books") }
    var imageUrl     by remember { mutableStateOf(existingListing?.imageUrl    ?: "") }
    val snackbarHost  = remember { SnackbarHostState() }

    val isEditing = existingListing != null
    val categories = listOf("Books", "Electronics", "Furniture", "Clothing", "Other")
    var expanded   by remember { mutableStateOf(false) }

    LaunchedEffect(actionResult) {
        if (actionResult is UiState.Success) onBack()
    }

    LaunchedEffect(Unit) {
        viewModel.toast.collect { msg -> snackbarHost.showSnackbar(msg) }
    }

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        snackbarHost   = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title  = { Text(if (isEditing) "Edit Listing" else "New Listing", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = UniNavy, titleContentColor = Color.White),
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null, tint = Color.White) }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding      = PaddingValues(vertical = 20.dp)
        ) {
            item {
                UniTextField(title, { title = it }, "Product Title", Icons.Filled.Title)
            }
            item {
                OutlinedTextField(
                    value         = description,
                    onValueChange = { description = it },
                    label         = { Text("Description") },
                    leadingIcon   = { Icon(Icons.Filled.Description, null) },
                    modifier      = Modifier.fillMaxWidth(),
                    minLines      = 3,
                    shape         = RoundedCornerShape(12.dp)
                )
            }
            item {
                UniTextField(priceStr, { priceStr = it }, "Price (\$)", Icons.Filled.AttachMoney, keyboardType = KeyboardType.Decimal)
            }
            item {
                // Category dropdown
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value           = category,
                        onValueChange   = {},
                        readOnly        = true,
                        label           = { Text("Category") },
                        leadingIcon     = { Icon(Icons.Filled.Category, null) },
                        trailingIcon    = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier        = Modifier.fillMaxWidth().menuAnchor(),
                        shape           = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text    = { Text(cat) },
                                onClick = { category = cat; expanded = false }
                            )
                        }
                    }
                }
            }
            item {
                UniTextField(imageUrl, { imageUrl = it }, "Image URL (optional)", Icons.Filled.Image)
            }
            item {
                // Preview image
                if (imageUrl.isNotBlank()) {
                    AsyncImage(
                        model              = imageUrl,
                        contentDescription = "Preview",
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp))
                    )
                }
            }
            item {
                if (actionResult is UiState.Error) {
                    Text((actionResult as UiState.Error).msg, color = Color.Red, fontSize = 13.sp)
                }
                Button(
                    onClick = {
                        val price = priceStr.toDoubleOrNull() ?: 0.0
                        if (isEditing) {
                            viewModel.updateListing(existingListing!!.id, title, description, price, category)
                        } else {
                            viewModel.createListing(title, description, price, category, imageUrl.ifBlank { null })
                        }
                    },
                    enabled  = title.isNotBlank() && description.isNotBlank() && priceStr.isNotBlank() && actionResult !is UiState.Loading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = UniAccent),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    if (actionResult is UiState.Loading)
                        CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else
                        Text(if (isEditing) "Update Listing" else "Publish Listing", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
