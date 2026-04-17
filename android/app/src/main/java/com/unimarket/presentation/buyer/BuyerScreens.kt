@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.unimarket.presentation.buyer

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.unimarket.R
import com.unimarket.domain.model.Cart
import com.unimarket.domain.model.CartItem
import com.unimarket.domain.model.CheckoutRequest
import com.unimarket.domain.model.Listing
import com.unimarket.domain.model.Order
import com.unimarket.presentation.BuyerViewModel
import com.unimarket.presentation.UiState
import com.unimarket.presentation.auth.UniAccent
import com.unimarket.presentation.auth.UniNavy
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val fmt: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US)

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

private fun detectCardType(number: String): String {
    val digits = number.replace(" ", "")
    return when {
        digits.startsWith("4") -> "Visa"
        digits.startsWith("34") || digits.startsWith("37") -> "American Express"
        digits.startsWith("6011") || digits.startsWith("65") || digits.matches(Regex("^64[4-9].*")) -> "Discover"
        digits.matches(Regex("^5[1-5].*")) || digits.matches(Regex("^2(2[2-9]|[3-6][0-9]|7[01]).*")) -> "Mastercard"
        digits.isBlank() -> ""
        else -> "Unknown"
    }
}

private fun formatCardNumber(input: String): String {
    val digits = input.filter { it.isDigit() }.take(16)
    return digits.chunked(4).joinToString(" ")
}

private fun formatExpiry(input: String): String {
    val digits = input.filter { it.isDigit() }.take(4)
    return when {
        digits.length <= 2 -> digits
        else -> digits.substring(0, 2) + "/" + digits.substring(2)
    }
}

private fun isValidExpiry(expiry: String): Boolean {
    return expiry.matches(Regex("(0[1-9]|1[0-2])/\\d{2}"))
}

private fun formatOrderDate(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.US).format(Date(timestamp))
}

private fun cartCount(cart: Cart?): Int {
    return cart?.items?.sumOf { it.quantity } ?: 0
}

private fun orderItemCount(order: Order): Int {
    return order.items.sumOf { it.quantity }
}

@Composable
private fun ListingImage(
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

@Composable
fun BrowseScreen(
    viewModel: BuyerViewModel,
    currentUserId: Int?,
    onViewListing: (Listing) -> Unit,
    onCartClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit,
    onOrdersClick: () -> Unit = {},
    onSellClick: () -> Unit = {},
    onMyListingsClick: () -> Unit = {}
) {
    val listingsState by viewModel.listings.collectAsState()
    val cartState by viewModel.cart.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val cartValue = (cartState as? UiState.Success<Cart>)?.data
    val badgeCount = cartCount(cartValue)
    val listings = (listingsState as? UiState.Success<List<Listing>>)?.data.orEmpty()
    val liveCount = listings.count { it.isActive }
    val categoryCount = listings.map { it.category }.distinct().size

    LaunchedEffect(Unit) {
        viewModel.loadListings()
        viewModel.loadCart()
    }

    Scaffold(
        containerColor = Color(0xFFF7F8FA),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.unimarket_logo_mark),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(28.dp)
                        )
                        Text("UniMarket", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = UniNavy,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Filled.Person, null, tint = Color.White)
                    }
                    IconButton(onClick = onCartClick) {
                        BadgedBox(
                            badge = {
                                if (badgeCount > 0) {
                                    Badge { Text("$badgeCount") }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.ShoppingCart, null, tint = Color.White)
                        }
                    }
                    IconButton(onClick = onOrdersClick) {
                        Icon(Icons.Filled.ShoppingBag, null, tint = Color.White)
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Filled.Logout, null, tint = Color.White)
                    }
                }
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

            is UiState.Error -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                ErrorCard(s.msg) {
                    viewModel.loadListings()
                }
            }

            is UiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    item {
                        BrowseHeroCard(
                            itemCount = liveCount,
                            categoryCount = categoryCount,
                            onSellClick = onSellClick,
                            onMyListingsClick = onMyListingsClick
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                viewModel.loadListings(keyword = it.ifBlank { null })
                            },
                            placeholder = { Text("Search books, bikes, electronics...") },
                            leadingIcon = { Icon(Icons.Filled.Search, null, tint = UniNavy) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = UniAccent,
                                unfocusedBorderColor = Color(0xFFD8E0EB),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedTextColor = Color(0xFF132033),
                                unfocusedTextColor = Color(0xFF132033),
                                cursorColor = UniAccent
                            )
                        )
                    }

                    item {
                        BrowseSectionTitle(
                            title = "Browse by category",
                            subtitle = "Quick campus-friendly filters"
                        )
                    }

                    item {
                        CategoryChips { cat ->
                            viewModel.loadListings(category = cat)
                        }
                    }

                    item {
                        BrowseSectionTitle(
                            title = "Fresh on campus",
                            subtitle = if (liveCount > 0) {
                                "$liveCount active listings ready to explore"
                            } else {
                                "No active listings yet"
                            }
                        )
                    }

                    item {
                        ListingsGridSection(
                            listings = s.data,
                            currentUserId = currentUserId,
                            onViewListing = onViewListing,
                            onAddCart = { listingId -> viewModel.addToCart(listingId) }
                        )
                    }
                }
            }

            else -> Unit
        }
    }
}

@Composable
private fun ListingsGridSection(
    listings: List<Listing>,
    currentUserId: Int?,
    onViewListing: (Listing) -> Unit,
    onAddCart: (Int) -> Unit
) {
    val rows = listings.chunked(2)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { listing ->
                    val isOwnListing = currentUserId != null && listing.sellerId == currentUserId

                    Box(modifier = Modifier.weight(1f)) {
                        ListingCard(
                            listing = listing,
                            isOwnListing = isOwnListing,
                            onClick = { onViewListing(listing) },
                            onAddCart = { onAddCart(listing.id) }
                        )
                    }
                }

                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BrowseHeroCard(
    itemCount: Int,
    categoryCount: Int,
    onSellClick: () -> Unit,
    onMyListingsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            UniNavy,
                            Color(0xFF11406E),
                            Color(0xFF1F6DB2)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.16f)
            ) {
                Text(
                    text = "UTA Maverick Market",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Find trusted campus deals faster.",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 30.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Shop textbooks, tech, room essentials, and more from students around UTA.",
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HeroStatCard(
                    label = "Active Listings",
                    value = itemCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                HeroStatCard(
                    label = "Categories",
                    value = categoryCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSellClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = UniAccent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.AddShoppingCart, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Sell now", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onMyListingsClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = UniNavy
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Store, null)
                    Spacer(Modifier.width(6.dp))
                    Text("My listings", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HeroStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.13f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = value,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun BrowseSectionTitle(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color(0xFF132033)
        )
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = Color(0xFF6B7280)
        )
    }
}

@Composable
fun ListingCard(
    listing: Listing,
    isOwnListing: Boolean,
    onClick: () -> Unit,
    onAddCart: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column {
            Box {
                ListingImage(
                    imageUrl = firstListingImageUrl(listing.imageUrl),
                    fallbackUrl = "https://placehold.co/300x200/png",
                    contentDescription = listing.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(144.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                )

                Surface(
                    modifier = Modifier.padding(10.dp),
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.9f)
                ) {
                    Text(
                        text = listing.category,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontSize = 10.sp,
                        color = UniNavy,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                if (isOwnListing) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = UniAccent.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "Your Listing",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = UniAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }

                Text(
                    listing.title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 15.sp,
                    color = Color(0xFF132033),
                    lineHeight = 19.sp
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    "Sold by ${listing.sellerName}",
                    fontSize = 11.sp,
                    color = Color(0xFF6B7280),
                    maxLines = 1
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        fmt.format(listing.price),
                        fontWeight = FontWeight.Bold,
                        color = UniAccent,
                        fontSize = 17.sp
                    )

                    IconButton(
                        onClick = {
                            if (!isOwnListing) onAddCart()
                        },
                        enabled = !isOwnListing,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.AddShoppingCart,
                            null,
                            tint = if (isOwnListing) Color.LightGray else UniNavy,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChips(onSelect: (String?) -> Unit) {
    val categories = listOf("All", "Books", "Electronics", "Furniture", "Clothing", "Food", "Other")
    var selected by remember { mutableStateOf("All") }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 10.dp)
    ) {
        items(categories) { cat ->
            FilterChip(
                selected = selected == cat,
                onClick = {
                    selected = cat
                    onSelect(if (cat == "All") null else cat)
                },
                label = { Text(cat, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = UniNavy,
                    selectedLabelColor = Color.White,
                    containerColor = Color.White,
                    labelColor = Color(0xFF3B4B60)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected == cat,
                    borderColor = if (selected == cat) UniNavy else Color(0xFFD9E1EA)
                )
            )
        }
    }
}

@Composable
private fun ListingImageGallery(listing: Listing) {
    val images = listingImageUrls(listing.imageUrl)

    if (images.size <= 1) {
        ListingImage(
            imageUrl = images.firstOrNull(),
            fallbackUrl = "https://placehold.co/800x500/png",
            contentDescription = listing.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        )
    } else {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
        ) {
            items(images) { image ->
                ListingImage(
                    imageUrl = image,
                    fallbackUrl = "https://placehold.co/800x500/png",
                    contentDescription = listing.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 300.dp, height = 230.dp)
                        .clip(RoundedCornerShape(22.dp))
                )
            }
        }
    }
}

@Composable
fun ListingDetailScreen(
    listing: Listing,
    currentUserId: Int?,
    viewModel: BuyerViewModel,
    onBack: () -> Unit,
    onCartOpen: () -> Unit
) {
    val cartState by viewModel.cart.collectAsState()
    val cartValue = (cartState as? UiState.Success<Cart>)?.data
    val badgeCount = cartCount(cartValue)
    val isOwnListing = currentUserId != null && listing.sellerId == currentUserId

    LaunchedEffect(Unit) {
        viewModel.loadCart()
    }

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        topBar = {
            TopAppBar(
                title = { Text("Product Detail", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = UniNavy,
                    titleContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    BadgedBox(
                        badge = {
                            if (badgeCount > 0) {
                                Badge { Text("$badgeCount") }
                            }
                        }
                    ) {
                        IconButton(onClick = onCartOpen) {
                            Icon(Icons.Filled.ShoppingCart, null, tint = Color.White)
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 10.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Price", color = Color.Gray, fontSize = 12.sp)
                        Text(
                            fmt.format(listing.price),
                            color = UniAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (!isOwnListing) {
                                viewModel.addToCart(listing.id)
                            }
                        },
                        enabled = !isOwnListing,
                        modifier = Modifier.weight(1.4f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOwnListing) Color.LightGray else UniAccent
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isOwnListing) {
                            Text("Your Listing", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Filled.AddShoppingCart, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add to Cart", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                ListingImageGallery(listing = listing)
            }

            item {
                Column(modifier = Modifier.padding(20.dp)) {
                    if (isOwnListing) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = UniAccent.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "This is your listing",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = UniAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = UniAccent.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = listing.category,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = UniAccent,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = listing.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = Color.Black
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Store, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sold by ${listing.sellerName}", color = Color.Gray)
                    }

                    Spacer(Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CalendarMonth, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Listed on ${formatOrderDate(listing.createdAt)}", color = Color.Gray)
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Description",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        listing.description,
                        color = Color(0xFF4B5563),
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    )

                    Spacer(Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
fun CartScreen(
    viewModel: BuyerViewModel,
    currentUserId: Int?,
    onBack: () -> Unit,
    onCheckout: () -> Unit
) {
    val cartState by viewModel.cart.collectAsState()
    val checkoutState by viewModel.checkoutResult.collectAsState()

    var cardHolder by remember { mutableStateOf("") }
    var cardNumberField by remember { mutableStateOf(TextFieldValue("")) }
    var expiryField by remember { mutableStateOf(TextFieldValue("")) }
    var cvvField by remember { mutableStateOf(TextFieldValue("")) }
    var localError by remember { mutableStateOf<String?>(null) }

    val cardNumber = cardNumberField.text
    val expiry = expiryField.text
    val cvv = cvvField.text
    val detectedCardType = detectCardType(cardNumber)

    LaunchedEffect(Unit) {
        viewModel.loadCart()
    }

    LaunchedEffect(checkoutState) {
        if (checkoutState is UiState.Success) {
            onCheckout()
        }
    }

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        topBar = {
            TopAppBar(
                title = { Text("My Cart", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = UniNavy,
                    titleContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
                    }
                }
            )
        },
        bottomBar = {
            if (cartState is UiState.Success) {
                val cart = (cartState as UiState.Success<Cart>).data
                val hasUnavailableItems = cart.items.any { !it.listing.isActive }
                val hasOwnItems = currentUserId != null && cart.items.any { it.listing.sellerId == currentUserId }

                Surface(shadowElevation = 8.dp) {
                    Column(Modifier.padding(16.dp)) {
                        SummaryRow(
                            label = "Items",
                            value = "${cart.items.sumOf { it.quantity }}"
                        )
                        Spacer(Modifier.height(6.dp))
                        SummaryRow(
                            label = "Total",
                            value = fmt.format(cart.totalAmount),
                            valueColor = UniAccent,
                            bold = true
                        )

                        if (hasOwnItems) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Remove your own listing from cart before checkout.",
                                color = Color.Red,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (hasUnavailableItems) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Remove sold or unavailable items before checkout.",
                                color = Color.Red,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val digitsOnly = cardNumber.replace(" ", "")
                                when {
                                    hasOwnItems -> localError = "You cannot buy your own listing."
                                    hasUnavailableItems -> localError = "Remove unavailable items from your cart first."
                                    cart.items.isEmpty() -> localError = "Your cart is empty."
                                    cardHolder.isBlank() -> localError = "Enter cardholder name."
                                    digitsOnly.length != 16 -> localError = "Card number must be 16 digits."
                                    !isValidExpiry(expiry) -> localError = "Use expiry format MM/YY."
                                    cvv.length !in 3..4 || !cvv.all { it.isDigit() } -> localError = "CVV must be 3 or 4 digits."
                                    else -> {
                                        localError = null
                                        viewModel.checkout(
                                            CheckoutRequest(
                                                cardNumber = digitsOnly,
                                                cardExpiry = expiry,
                                                cardCvv = cvv,
                                                cardHolder = cardHolder.trim()
                                            )
                                        )
                                    }
                                }
                            },
                            enabled = cart.items.isNotEmpty() &&
                                    !hasUnavailableItems &&
                                    !hasOwnItems &&
                                    checkoutState !is UiState.Loading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = UniAccent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (checkoutState is UiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Pay & Place Order", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }

                        if (localError != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(localError ?: "", color = Color.Red, fontSize = 13.sp)
                        }

                        if (checkoutState is UiState.Error) {
                            Spacer(Modifier.height(8.dp))
                            Text((checkoutState as UiState.Error).msg, color = Color.Red, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    ) { padding ->
        when (val s = cartState) {
            is UiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = UniAccent)
            }

            is UiState.Error -> ErrorCard(s.msg) {
                viewModel.loadCart()
            }

            is UiState.Success -> {
                if (s.data.items.isEmpty()) {
                    EmptyState("Your cart is empty", Icons.Filled.ShoppingCart)
                } else {
                    val itemCount = s.data.items.sumOf { it.quantity }
                    val unavailableItems = s.data.items.filter { !it.listing.isActive }
                    val ownItems = if (currentUserId != null) {
                        s.data.items.filter { it.listing.sellerId == currentUserId }
                    } else {
                        emptyList()
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(padding)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(3.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Order Summary",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    SummaryRow("Items in Cart", "$itemCount")
                                    Spacer(Modifier.height(6.dp))
                                    SummaryRow(
                                        label = "Estimated Total",
                                        value = fmt.format(s.data.totalAmount),
                                        valueColor = UniAccent,
                                        bold = true
                                    )

                                    if (ownItems.isNotEmpty()) {
                                        Spacer(Modifier.height(10.dp))
                                        Text(
                                            text = "Your own listing is in this cart. Remove it before checkout.",
                                            color = Color.Red,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    if (unavailableItems.isNotEmpty()) {
                                        Spacer(Modifier.height(10.dp))
                                        Text(
                                            text = "Some items in your cart are already sold or unavailable.",
                                            color = Color.Red,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = "You're buying:",
                                        fontSize = 13.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        items(s.data.items, key = { it.id }) { item ->
                            val isOwnListing = currentUserId != null && item.listing.sellerId == currentUserId
                            CartItemRow(
                                item = item,
                                isOwnListing = isOwnListing,
                                onRemove = { viewModel.removeFromCart(item.id) }
                            )
                        }

                        item {
                            Spacer(Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(3.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Payment Details", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Spacer(Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = cardHolder,
                                        onValueChange = {
                                            cardHolder = it
                                            localError = null
                                        },
                                        label = { Text("Cardholder Name") },
                                        leadingIcon = { Icon(Icons.Filled.Person, null) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )

                                    Spacer(Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = cardNumberField,
                                        onValueChange = {
                                            val formatted = formatCardNumber(it.text)
                                            cardNumberField = TextFieldValue(
                                                text = formatted,
                                                selection = TextRange(formatted.length)
                                            )
                                            localError = null
                                        },
                                        label = { Text("Card Number") },
                                        leadingIcon = { Icon(Icons.Filled.CreditCard, null) },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )

                                    if (detectedCardType.isNotBlank()) {
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = "Detected Card Type: $detectedCardType",
                                            color = UniAccent,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp
                                        )
                                    }

                                    Spacer(Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = expiryField,
                                            onValueChange = {
                                                val formatted = formatExpiry(it.text)
                                                expiryField = TextFieldValue(
                                                    text = formatted,
                                                    selection = TextRange(formatted.length)
                                                )
                                                localError = null
                                            },
                                            label = { Text("Expiry (MM/YY)") },
                                            leadingIcon = { Icon(Icons.Filled.CalendarMonth, null) },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true
                                        )

                                        OutlinedTextField(
                                            value = cvvField,
                                            onValueChange = {
                                                val digits = it.text.filter { ch -> ch.isDigit() }.take(4)
                                                cvvField = TextFieldValue(
                                                    text = digits,
                                                    selection = TextRange(digits.length)
                                                )
                                                localError = null
                                            },
                                            label = { Text("CVV") },
                                            leadingIcon = { Icon(Icons.Filled.Lock, null) },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                            visualTransformation = PasswordVisualTransformation(),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(Modifier.height(90.dp))
                        }
                    }
                }
            }

            else -> Unit
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    isOwnListing: Boolean,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ListingImage(
                imageUrl = firstListingImageUrl(item.listing.imageUrl),
                fallbackUrl = "https://placehold.co/80x80/png",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Column(Modifier.weight(1f)) {
                if (isOwnListing) {
                    Text(
                        "Your Listing",
                        color = UniAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    item.listing.title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Qty: ${item.quantity}  •  ${item.listing.category}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    "Seller: ${item.listing.sellerName}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                if (!item.listing.isActive) {
                    Text(
                        "Sold out / unavailable",
                        fontSize = 12.sp,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    fmt.format(item.subtotal),
                    fontWeight = FontWeight.Bold,
                    color = UniAccent
                )
            }

            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, null, tint = Color.Red.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun OrderConfirmationScreen(
    viewModel: BuyerViewModel,
    onDone: () -> Unit
) {
    val checkoutState by viewModel.checkoutResult.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        when (val s = checkoutState) {
            is UiState.Success -> {
                val order = s.data

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                null,
                                tint = UniAccent,
                                modifier = Modifier.size(64.dp)
                            )
                            Column {
                                Text("Order Confirmed!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                Text("Order #${order.id}", color = Color.Gray)
                                Text(formatOrderDate(order.createdAt), color = Color.Gray, fontSize = 12.sp)
                            }
                        }

                        HorizontalDivider()

                        SummaryRow("Status", order.status, valueColor = UniAccent, bold = true)
                        SummaryRow("Items Purchased", "${orderItemCount(order)}")
                        SummaryRow("Order Total", fmt.format(order.totalAmount), valueColor = UniAccent, bold = true)
                        SummaryRow(
                            "Payment",
                            order.cardLastFour?.let { "Card ending in $it" } ?: "Card processed"
                        )

                        HorizontalDivider()

                        Text(
                            text = "Purchased Items",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        order.items.forEach { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = item.title,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    SummaryRow("Quantity", "${item.quantity}")
                                    SummaryRow("Price Each", fmt.format(item.priceAtPurchase))
                                    SummaryRow("Subtotal", fmt.format(item.subtotal), valueColor = UniAccent)
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = {
                                viewModel.resetCheckout()
                                onDone()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = UniAccent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Continue Shopping", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            is UiState.Error -> {
                Text(s.msg, color = Color.Red)
            }

            else -> {
                CircularProgressIndicator(color = UniAccent)
            }
        }
    }
}

@Composable
fun OrderHistoryScreen(
    viewModel: BuyerViewModel,
    onBack: () -> Unit
) {
    val ordersState by viewModel.orders.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadOrders()
    }

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        topBar = {
            TopAppBar(
                title = { Text("My Orders", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = UniNavy,
                    titleContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        when (val s = ordersState) {
            is UiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = UniAccent)
            }

            is UiState.Error -> ErrorCard(s.msg) {
                viewModel.loadOrders()
            }

            is UiState.Success -> {
                if (s.data.isEmpty()) {
                    EmptyState("No orders yet", Icons.Filled.ShoppingBag)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(s.data) { order ->
                            OrderCard(order)
                        }
                    }
                }
            }

            else -> Unit
        }
    }
}

@Composable
fun OrderCard(order: Order) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Order #${order.id}", fontWeight = FontWeight.Bold)
                    Text(
                        formatOrderDate(order.createdAt),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Surface(
                    color = UniAccent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = order.status,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = UniAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("${orderItemCount(order)} items", color = Color.Gray, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                fmt.format(order.totalAmount),
                color = UniAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: Color = Color(0xFF111827),
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 13.sp
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun EmptyState(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = Color.LightGray, modifier = Modifier.size(72.dp))
            Text(text, color = Color.Gray, fontSize = 16.sp)
        }
    }
}

@Composable
fun ErrorCard(
    msg: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.ErrorOutline, null, tint = Color.Red, modifier = Modifier.size(48.dp))
            Text(msg, color = Color.Red)
            TextButton(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}
