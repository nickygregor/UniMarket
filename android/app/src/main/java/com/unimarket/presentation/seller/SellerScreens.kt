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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.unimarket.domain.model.Listing
import com.unimarket.presentation.SellerViewModel
import com.unimarket.presentation.UiState
import com.unimarket.presentation.auth.UniAccent
import com.unimarket.presentation.auth.UniBlue
import com.unimarket.presentation.auth.UniNavy
import com.unimarket.presentation.auth.UniTextField
import com.unimarket.presentation.buyer.EmptyState
import com.unimarket.presentation.buyer.ErrorCard
import com.unimarket.presentation.buyer.fmt

private val FormTextColor = Color(0xFF132033)
private val FormMutedText = Color(0xFF6B7280)
private val FormBgColor = Color(0xFFF5F7FA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerDashboardScreen(
    viewModel: SellerViewModel,
    onAddListing: () -> Unit,
    onEdit: (Listing) -> Unit,
    onLogout: () -> Unit,
    onProfileClick: () -> Unit
) {
    val listingsState by viewModel.listings.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadMyListings()
    }

    LaunchedEffect(Unit) {
        viewModel.toast.collect { msg ->
            snackbarHost.showSnackbar(msg)
        }
    }

    Scaffold(
        containerColor = FormBgColor,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("My Listings", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Manage what you're selling",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = UniNavy,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Filled.Person, contentDescription = "Profile", tint = Color.White)
                    }
                    IconButton(onClick = onAddListing) {
                        Icon(Icons.Filled.AddCircle, contentDescription = "Add Listing", tint = Color.White)
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Filled.Logout, contentDescription = "Logout", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddListing,
                containerColor = UniAccent,
                contentColor = Color.White,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New Listing", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        when (val s = listingsState) {
            is UiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = UniAccent)
            }

            is UiState.Error -> Box(modifier = Modifier.padding(padding)) {
                ErrorCard(s.msg) { viewModel.loadMyListings() }
            }

            is UiState.Success -> {
                if (s.data.isEmpty()) {
                    Box(modifier = Modifier.padding(padding)) {
                        EmptyState(
                            text = "You have no listings yet. Tap New Listing to post your first item.",
                            icon = Icons.Filled.Inventory2
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(padding)
                    ) {
                        items(s.data, key = { it.id }) { listing ->
                            SellerListingCard(
                                listing = listing,
                                onEdit = { onEdit(listing) },
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
fun SellerListingCard(
    listing: Listing,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Listing") },
            text = { Text("Are you sure you want to delete \"${listing.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AsyncImage(
                model = listing.imageUrl ?: "https://placehold.co/100x100/png",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = listing.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = listing.category,
                    fontSize = 12.sp,
                    color = FormMutedText
                )

                Text(
                    text = fmt.format(listing.price),
                    fontWeight = FontWeight.Bold,
                    color = UniAccent,
                    fontSize = 15.sp
                )

                Text(
                    text = if (listing.isActive) "Active" else "Inactive",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (listing.isActive) UniBlue else Color.Red
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = UniNavy)
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.75f))
                }
            }
        }
    }
}

private fun isValidImageUrl(url: String): Boolean {
    val trimmed = url.trim().lowercase()
    return (trimmed.startsWith("http://") || trimmed.startsWith("https://")) &&
            (trimmed.endsWith(".jpg")
                    || trimmed.endsWith(".jpeg")
                    || trimmed.endsWith(".png")
                    || trimmed.endsWith(".webp")
                    || trimmed.contains("placehold.co")
                    || trimmed.contains("imgur")
                    || trimmed.contains("cloudinary"))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditListingScreen(
    viewModel: SellerViewModel,
    existingListing: Listing? = null,
    onBack: () -> Unit
) {
    val actionResult by viewModel.actionResult.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    var title by remember { mutableStateOf(existingListing?.title ?: "") }
    var description by remember { mutableStateOf(existingListing?.description ?: "") }
    var priceStr by remember { mutableStateOf(existingListing?.price?.toString() ?: "") }
    var category by remember { mutableStateOf(existingListing?.category ?: "Books") }
    var imageUrl by remember { mutableStateOf(existingListing?.imageUrl ?: "") }
    var localError by remember { mutableStateOf<String?>(null) }

    val isEditing = existingListing != null
    val categories = listOf("Books", "Electronics", "Furniture", "Clothing", "Other")
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(actionResult) {
        if (actionResult is UiState.Success) {
            onBack()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.toast.collect { msg ->
            snackbarHost.showSnackbar(msg)
        }
    }

    Scaffold(
        containerColor = FormBgColor,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isEditing) "Edit Listing" else "Create Listing",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isEditing) "Update your item details" else "Post a new item for sale",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = UniNavy,
                    titleContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 20.dp)
        ) {
            item {
                Text(
                    text = "Listing Details",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = FormTextColor
                )
                Text(
                    text = "All fields below are required, including the image link.",
                    fontSize = 13.sp,
                    color = FormMutedText
                )
            }

            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        localError = null
                    },
                    label = { Text("Product Title *") },
                    leadingIcon = {
                        Icon(Icons.Filled.Title, contentDescription = null, tint = UniAccent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = UniAccent,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                        focusedTextColor = FormTextColor,
                        unfocusedTextColor = FormTextColor,
                        focusedLabelColor = UniAccent,
                        unfocusedLabelColor = FormMutedText,
                        cursorColor = UniAccent,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }

            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        localError = null
                    },
                    label = { Text("Description *") },
                    leadingIcon = {
                        Icon(Icons.Filled.Description, contentDescription = null, tint = UniAccent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = UniAccent,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                        focusedTextColor = FormTextColor,
                        unfocusedTextColor = FormTextColor,
                        focusedLabelColor = UniAccent,
                        unfocusedLabelColor = FormMutedText,
                        cursorColor = UniAccent,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }

            item {
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = {
                        priceStr = it
                        localError = null
                    },
                    label = { Text("Price ($) *") },
                    leadingIcon = {
                        Icon(Icons.Filled.AttachMoney, contentDescription = null, tint = UniAccent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = UniAccent,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                        focusedTextColor = FormTextColor,
                        unfocusedTextColor = FormTextColor,
                        focusedLabelColor = UniAccent,
                        unfocusedLabelColor = FormMutedText,
                        cursorColor = UniAccent,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    )
                )
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category *") },
                        leadingIcon = {
                            Icon(Icons.Filled.Category, contentDescription = null, tint = UniAccent)
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = UniAccent,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                            focusedTextColor = FormTextColor,
                            unfocusedTextColor = FormTextColor,
                            focusedLabelColor = UniAccent,
                            unfocusedLabelColor = FormMutedText,
                            cursorColor = UniAccent,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    expanded = false
                                    localError = null
                                }
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Add Image",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = FormTextColor
                )
            }

            item {
                FilledTonalButton(
                    onClick = {
                        imageUrl = "https://placehold.co/600x400/png"
                        localError = null
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = UniAccent.copy(alpha = 0.15f),
                        contentColor = UniAccent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Use Sample Image URL")
                }
            }

            item {
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = {
                        imageUrl = it
                        localError = null
                    },
                    label = { Text("Paste Image URL *") },
                    leadingIcon = {
                        Icon(Icons.Filled.Image, contentDescription = null, tint = UniAccent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = UniAccent,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                        focusedTextColor = FormTextColor,
                        unfocusedTextColor = FormTextColor,
                        focusedLabelColor = UniAccent,
                        unfocusedLabelColor = FormMutedText,
                        cursorColor = UniAccent,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }

            item {
                Text(
                    text = "Paste a direct image link starting with http/https, or use the sample button above.",
                    fontSize = 12.sp,
                    color = FormMutedText
                )
            }

            item {
                if (imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                }
            }

            item {
                localError?.let {
                    Text(
                        text = it,
                        color = Color.Red,
                        fontSize = 13.sp
                    )
                }

                if (actionResult is UiState.Error) {
                    Text(
                        text = (actionResult as UiState.Error).msg,
                        color = Color.Red,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = {
                        val trimmedTitle = title.trim()
                        val trimmedDescription = description.trim()
                        val trimmedImageUrl = imageUrl.trim()
                        val parsedPrice = priceStr.toDoubleOrNull()

                        when {
                            trimmedTitle.isBlank() ||
                                    trimmedDescription.isBlank() ||
                                    priceStr.isBlank() ||
                                    category.isBlank() ||
                                    trimmedImageUrl.isBlank() -> {
                                localError = "Please fill in all required listing fields."
                            }

                            parsedPrice == null || parsedPrice <= 0.0 -> {
                                localError = "Please enter a valid price greater than 0."
                            }

                            !isValidImageUrl(trimmedImageUrl) -> {
                                localError = "Please enter a valid image URL."
                            }

                            else -> {
                                localError = null

                                if (isEditing) {
                                    viewModel.updateListing(
                                        existingListing!!.id,
                                        trimmedTitle,
                                        trimmedDescription,
                                        parsedPrice,
                                        category
                                    )
                                } else {
                                    viewModel.createListing(
                                        trimmedTitle,
                                        trimmedDescription,
                                        parsedPrice,
                                        category,
                                        trimmedImageUrl
                                    )
                                }
                            }
                        }
                    },
                    enabled = actionResult !is UiState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = UniAccent),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (actionResult is UiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isEditing) "Update Listing" else "Publish Listing",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}