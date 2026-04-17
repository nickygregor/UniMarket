package com.unimarket.presentation.seller

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.unimarket.presentation.buyer.EmptyState
import com.unimarket.presentation.buyer.ErrorCard
import com.unimarket.presentation.buyer.fmt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

private val FormTextColor = Color(0xFF132033)
private val FormMutedText = Color(0xFF6B7280)
private val FormBgColor = Color(0xFFF5F7FA)
private const val MaxListingImages = 4

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
            SellerListingImage(
                imageUrl = firstListingImageUrl(listing.imageUrl),
                fallbackUrl = "https://placehold.co/100x100/png",
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
                    text = if (listing.isActive) "Active" else "Sold / Inactive",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (listing.isActive) UniBlue else Color.Red
                )

                if (!listing.isActive) {
                    Text(
                        text = "This item was purchased or removed from the marketplace.",
                        fontSize = 11.sp,
                        color = FormMutedText
                    )
                }
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
    val images = listingImageUrls(url)
    return images.isNotEmpty() && images.all { image ->
        val lower = image.lowercase()
        lower.startsWith("data:image/") ||
                lower.startsWith("http://") ||
                lower.startsWith("https://")
    }
}

private fun listingImageUrls(value: String?): List<String> {
    return value
        ?.lines()
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        .orEmpty()
}

private fun firstListingImageUrl(value: String?): String? {
    return listingImageUrls(value).firstOrNull()
}

@Composable
private fun SellerListingImage(
    imageUrl: String?,
    fallbackUrl: String,
    contentDescription: String?,
    contentScale: ContentScale,
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
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        AsyncImage(
            model = imageUrl ?: fallbackUrl,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}

private fun uriToInlineImageData(context: Context, uri: Uri): String? {
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }

        val largestSide = max(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
        val targetSide = 480
        var sampleSize = 1
        while (largestSide / sampleSize > targetSide) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }

        val originalBitmap = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        } ?: return null

        val maxSide = max(originalBitmap.width, originalBitmap.height).coerceAtLeast(1)
        val scale = if (maxSide > targetSide) targetSide.toFloat() / maxSide else 1f
        val targetWidth = (originalBitmap.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (originalBitmap.height * scale).toInt().coerceAtLeast(1)

        val scaledBitmap = if (scale < 1f) {
            android.graphics.Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
        } else {
            originalBitmap
        }

        val out = ByteArrayOutputStream()
        var quality = 50
        scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, out)
        while (out.size() > 95_000 && quality > 24) {
            out.reset()
            quality -= 6
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, out)
        }

        if (scaledBitmap !== originalBitmap) {
            scaledBitmap.recycle()
        }
        originalBitmap.recycle()

        val encoded = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        "data:image/jpeg;base64,$encoded"
    }.getOrNull()
}

@Composable
private fun CategorySelector(
    selectedCategory: String,
    categories: List<String>,
    onCategorySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Category *",
            fontSize = 13.sp,
            color = FormMutedText,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(6.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = UniAccent.copy(alpha = 0.7f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Filled.Category,
                            contentDescription = null,
                            tint = UniAccent
                        )
                        Text(
                            text = selectedCategory,
                            color = FormTextColor,
                            fontSize = 15.sp
                        )
                    }

                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .background(Color.White)
                    .offset(y = 4.dp)
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            onCategorySelected(category)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(existingListing?.title ?: "") }
    var description by remember { mutableStateOf(existingListing?.description ?: "") }
    var priceStr by remember { mutableStateOf(existingListing?.price?.toString() ?: "") }
    var category by remember { mutableStateOf(existingListing?.category ?: "Books") }
    var imageUrl by remember { mutableStateOf(existingListing?.imageUrl ?: "") }
    var localError by remember { mutableStateOf<String?>(null) }
    var isImageProcessing by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            isImageProcessing = true
            localError = null

            scope.launch {
                val currentImages = listingImageUrls(imageUrl)
                val openSlots = (MaxListingImages - currentImages.size).coerceAtLeast(0)

                if (openSlots == 0) {
                    localError = "You can add up to $MaxListingImages photos per listing."
                    isImageProcessing = false
                    return@launch
                }

                val newImages = withContext(Dispatchers.IO) {
                    uris.take(openSlots).mapNotNull { uri ->
                        uriToInlineImageData(context, uri)
                    }
                }

                if (newImages.isNotEmpty()) {
                    imageUrl = (currentImages + newImages)
                        .take(MaxListingImages)
                        .joinToString("\n")
                    localError = null
                } else {
                    localError = "Couldn't read those images. Please try different photos."
                }

                isImageProcessing = false
            }
        }
    }

    val isEditing = existingListing != null
    val categories = listOf("Books", "Electronics", "Furniture", "Clothing", "Food", "Other")

    LaunchedEffect(Unit) {
        viewModel.resetAction()
    }

    LaunchedEffect(actionResult) {
        if (actionResult is UiState.Success) {
            viewModel.resetAction()
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
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    )
                )
            }

            item {
                CategorySelector(
                    selectedCategory = category,
                    categories = categories,
                    onCategorySelected = {
                        category = it
                        localError = null
                    }
                )
            }

            item {
                Text(
                    text = "Add Images",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = FormTextColor
                )
            }

            item {
                val selectedImageCount = listingImageUrls(imageUrl).size

                FilledTonalButton(
                    onClick = {
                        imagePickerLauncher.launch("image/*")
                        localError = null
                    },
                    enabled = !isImageProcessing,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = UniAccent.copy(alpha = 0.15f),
                        contentColor = UniAccent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isImageProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = UniAccent
                        )
                    } else {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isImageProcessing) {
                            "Preparing Images..."
                        } else {
                            "Choose Images From Device ($selectedImageCount/$MaxListingImages)"
                        }
                    )
                }
            }

            item {
                val selectedImages = listingImageUrls(imageUrl)
                val hasDeviceImage = selectedImages.any { it.startsWith("data:image/", ignoreCase = true) }

                OutlinedTextField(
                    value = if (hasDeviceImage) {
                        "${selectedImages.size} photo${if (selectedImages.size == 1) "" else "s"} selected from device"
                    } else {
                        imageUrl
                    },
                    onValueChange = {
                        if (!hasDeviceImage) {
                            imageUrl = it
                            localError = null
                        }
                    },
                    label = { Text("Paste Image URL (Optional if photo chosen)") },
                    leadingIcon = {
                        Icon(Icons.Filled.Image, contentDescription = null, tint = UniAccent)
                    },
                    enabled = !hasDeviceImage,
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
                    text = "Choose up to $MaxListingImages images from Photos or Files on the device, or paste one direct image link. The first image is used on listing cards.",
                    fontSize = 12.sp,
                    color = FormMutedText
                )
            }

            item {
                if (imageUrl.isNotBlank()) {
                    TextButton(
                        onClick = {
                            imageUrl = ""
                            localError = null
                        }
                    ) {
                        Text("Remove Selected Images", color = Color.Red)
                    }
                }
            }

            item {
                val selectedImages = listingImageUrls(imageUrl)

                if (selectedImages.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(end = 8.dp)
                    ) {
                        items(selectedImages) { selectedImage ->
                            SellerListingImage(
                                imageUrl = selectedImage,
                                fallbackUrl = "https://placehold.co/600x400/png",
                                contentDescription = "Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 180.dp, height = 140.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                        }
                    }
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
                                        category,
                                        trimmedImageUrl
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
