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
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.unimarket.ChatTarget
import com.unimarket.domain.model.Cart
import com.unimarket.domain.model.CartItem
import com.unimarket.domain.model.ChatMessage
import com.unimarket.domain.model.CheckoutRequest
import com.unimarket.domain.model.Conversation
import com.unimarket.domain.model.ListingComment
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

private val UtaPickupLocations = listOf(
    "E.H. Hereford University Center",
    "Central Library",
    "The Commons",
    "College Park Center",
    "Nedderman Hall",
    "Pickard Hall",
    "Arlington Hall"
)

private fun fulfillmentLabel(method: String): String =
    if (method.equals("DELIVERY", ignoreCase = true)) "Delivery" else "Pickup"

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

fun formatOrderDate(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.US).format(Date(timestamp))
}

private fun formatListingExpiry(timestamp: Long): String {
    return if (timestamp <= 0L) {
        "Expires after 30 days"
    } else {
        "Expires ${SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(timestamp))}"
    }
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
    onMessagesClick: () -> Unit = {},
    onSellClick: () -> Unit = {},
    onMyListingsClick: () -> Unit = {}
) {
    val listingsState by viewModel.listings.collectAsState()
    val cartState by viewModel.cart.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val cartValue = (cartState as? UiState.Success<Cart>)?.data
    val conversationsState by viewModel.conversations.collectAsState()
    val badgeCount = cartCount(cartValue)
    val unreadMessages = (conversationsState as? UiState.Success<List<Conversation>>)
        ?.data
        .orEmpty()
        .sumOf { it.unreadCount }
    val listings = (listingsState as? UiState.Success<List<Listing>>)?.data.orEmpty()
    val liveCount = listings.count { it.isActive }
    val categoryCount = listings.map { it.category }.distinct().size

    LaunchedEffect(Unit) {
        viewModel.loadListings()
        viewModel.loadCart()
        viewModel.loadConversations()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                    IconButton(onClick = onMessagesClick) {
                        BadgedBox(
                            badge = {
                                if (unreadMessages > 0) {
                                    Badge { Text("$unreadMessages") }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Send, null, tint = Color.White)
                        }
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
                            leadingIcon = { Icon(Icons.Filled.Search, null, tint = UniAccent) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = UniAccent,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 19.sp
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    "Sold by ${listing.sellerName}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    "${listing.condition} • ${formatListingExpiry(listing.expiresAt)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                            tint = if (isOwnListing) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f) else UniAccent,
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
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected == cat,
                    borderColor = if (selected == cat) UniNavy else MaterialTheme.colorScheme.outline
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
    onCartOpen: () -> Unit,
    onMessageSeller: (Listing) -> Unit
) {
    val cartState by viewModel.cart.collectAsState()
    val commentsState by viewModel.comments.collectAsState()
    val cartValue = (cartState as? UiState.Success<Cart>)?.data
    val badgeCount = cartCount(cartValue)
    val isOwnListing = currentUserId != null && listing.sellerId == currentUserId

    LaunchedEffect(Unit) {
        viewModel.loadCart()
        viewModel.loadComments(listing.id)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Price", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
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
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Store, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sold by ${listing.sellerName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    if (!isOwnListing) {
                        Spacer(Modifier.height(12.dp))
                        FilledTonalButton(
                            onClick = { onMessageSeller(listing) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = UniAccent.copy(alpha = 0.15f),
                                contentColor = UniAccent
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.Send, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Private message seller", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CalendarMonth, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Listed on ${formatOrderDate(listing.createdAt)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Condition: ${listing.condition}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = formatListingExpiry(listing.expiresAt),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Description",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        listing.description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    )

                    Spacer(Modifier.height(20.dp))

                    ListingCommentsSection(
                        commentsState = commentsState,
                        currentUserId = currentUserId,
                        onPost = { message -> viewModel.addComment(listing.id, message) },
                        onRetry = { viewModel.loadComments(listing.id) }
                    )

                    Spacer(Modifier.height(120.dp))
                }
            }
        }
    }
}

@Composable
private fun ListingCommentsSection(
    commentsState: UiState<List<ListingComment>>,
    currentUserId: Int?,
    onPost: (String) -> Unit,
    onRetry: () -> Unit
) {
    var message by remember { mutableStateOf("") }

    Text(
        text = "Questions & Comments",
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            placeholder = { Text("Ask the seller a question...") },
            modifier = Modifier.weight(1f),
            minLines = 1,
            maxLines = 3,
            shape = RoundedCornerShape(14.dp)
        )
        IconButton(
            onClick = {
                if (message.isNotBlank()) {
                    onPost(message.trim())
                    message = ""
                }
            },
            enabled = message.isNotBlank()
        ) {
            Icon(Icons.Filled.Send, null, tint = UniAccent)
        }
    }

    Spacer(Modifier.height(12.dp))

    when (commentsState) {
        is UiState.Loading -> CircularProgressIndicator(color = UniAccent, modifier = Modifier.size(24.dp))
        is UiState.Error -> ErrorCard(commentsState.msg, onRetry)
        is UiState.Success -> {
            val comments = commentsState.data
            if (comments.isEmpty()) {
                Text(
                    "No comments yet. Be the first to ask.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    comments.forEach { comment ->
                        CommentCard(
                            comment = comment,
                            isMine = currentUserId == comment.authorId
                        )
                    }
                }
            }
        }
        else -> Unit
    }
}

@Composable
private fun CommentCard(
    comment: ListingComment,
    isMine: Boolean
) {
    val isReply = comment.parentCommentId != null
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isReply) 22.dp else 0.dp),
        color = if (isMine) {
            UniAccent.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(comment.authorName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(formatOrderDate(comment.createdAt), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            }
            Text(comment.message, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
        }
    }
}

@Composable
fun BuyerChatScreen(
    viewModel: BuyerViewModel,
    currentUserId: Int?,
    target: ChatTarget,
    onBack: () -> Unit
) {
    ChatScreenScaffold(
        title = target.otherUserName,
        subtitle = target.title,
        messagesState = viewModel.messages.collectAsState().value,
        currentUserId = currentUserId,
        onBack = onBack,
        onLoad = { viewModel.loadMessages(target.listingId, target.otherUserId) },
        onSend = { viewModel.sendMessage(target.listingId, target.otherUserId, it) }
    )
}

@Composable
fun BuyerMessagesScreen(
    viewModel: BuyerViewModel,
    onBack: () -> Unit,
    onOpenChat: (Conversation) -> Unit
) {
    val conversationsState by viewModel.conversations.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadConversations()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Messages", fontWeight = FontWeight.Bold) },
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
        when (val state = conversationsState) {
            is UiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = UniAccent)
            }
            is UiState.Error -> Box(Modifier.fillMaxSize().padding(padding)) {
                ErrorCard(state.msg) { viewModel.loadConversations() }
            }
            is UiState.Success -> {
                if (state.data.isEmpty()) {
                    EmptyState("No messages yet", Icons.Filled.Send)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.data) { conversation ->
                            ConversationCard(
                                conversation = conversation,
                                onClick = { onOpenChat(conversation) }
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
fun ConversationCard(
    conversation: Conversation,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ListingImage(
                imageUrl = firstListingImageUrl(conversation.listingImageUrl),
                fallbackUrl = "https://placehold.co/100x100/png",
                contentDescription = conversation.listingTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        conversation.otherUserName,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (conversation.unreadCount > 0) {
                        Badge { Text("${conversation.unreadCount}") }
                    }
                }
                Text(
                    conversation.listingTitle,
                    color = UniAccent,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    conversation.lastMessage,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ChatScreenScaffold(
    title: String,
    subtitle: String,
    messagesState: UiState<List<ChatMessage>>,
    currentUserId: Int?,
    onBack: () -> Unit,
    onLoad: () -> Unit,
    onSend: (String) -> Unit
) {
    var message by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { onLoad() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, fontWeight = FontWeight.Bold)
                        Text(subtitle, fontSize = 11.sp, color = Color.White.copy(alpha = 0.82f), maxLines = 1)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = UniNavy, titleContentColor = Color.White),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
                    }
                }
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        placeholder = { Text("Write a message...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        maxLines = 4
                    )
                    IconButton(
                        onClick = {
                            if (message.isNotBlank()) {
                                onSend(message.trim())
                                message = ""
                            }
                        },
                        enabled = message.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Send, null, tint = UniAccent)
                    }
                }
            }
        }
    ) { padding ->
        when (messagesState) {
            is UiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = UniAccent)
            }
            is UiState.Error -> Box(Modifier.fillMaxSize().padding(padding)) {
                ErrorCard(messagesState.msg, onLoad)
            }
            is UiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messagesState.data, key = { it.id }) { msg ->
                        val mine = currentUserId == msg.senderId
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(0.78f),
                                color = if (mine) UniAccent else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        msg.senderName,
                                        fontSize = 11.sp,
                                        color = if (mine) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        msg.message,
                                        color = if (mine) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            else -> Unit
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
    var fulfillmentMethod by remember { mutableStateOf("PICKUP") }
    var pickupLocation by remember { mutableStateOf(UtaPickupLocations.first()) }
    var deliveryStreet by remember { mutableStateOf("") }
    var deliveryCity by remember { mutableStateOf("") }
    var deliveryZip by remember { mutableStateOf("") }
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
        containerColor = MaterialTheme.colorScheme.background,
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

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
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
                                val fulfillmentLocation = if (fulfillmentMethod == "PICKUP") {
                                    pickupLocation
                                } else {
                                    "${deliveryStreet.trim()}, ${deliveryCity.trim()}, TX ${deliveryZip.trim()}"
                                }
                                when {
                                    hasOwnItems -> localError = "You cannot buy your own listing."
                                    hasUnavailableItems -> localError = "Remove unavailable items from your cart first."
                                    cart.items.isEmpty() -> localError = "Your cart is empty."
                                    fulfillmentMethod == "DELIVERY" && deliveryStreet.isBlank() -> localError = "Enter a street address."
                                    fulfillmentMethod == "DELIVERY" && deliveryCity.isBlank() -> localError = "Enter a city."
                                    fulfillmentMethod == "DELIVERY" && !deliveryZip.matches(Regex("\\d{5}")) -> localError = "ZIP code must be 5 digits."
                                    fulfillmentLocation.isBlank() -> localError = "Choose a pickup spot or enter a delivery location."
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
                                                cardHolder = cardHolder.trim(),
                                                fulfillmentMethod = fulfillmentMethod,
                                                fulfillmentLocation = fulfillmentLocation
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
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(3.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Order Summary",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
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
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(3.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Pickup or Delivery",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(12.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        FilterChip(
                                            selected = fulfillmentMethod == "PICKUP",
                                            onClick = {
                                                fulfillmentMethod = "PICKUP"
                                                localError = null
                                            },
                                            label = { Text("Pickup") },
                                            leadingIcon = {
                                                Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = UniAccent.copy(alpha = 0.18f),
                                                selectedLabelColor = UniAccent,
                                                selectedLeadingIconColor = UniAccent
                                            )
                                        )
                                        FilterChip(
                                            selected = fulfillmentMethod == "DELIVERY",
                                            onClick = {
                                                fulfillmentMethod = "DELIVERY"
                                                localError = null
                                            },
                                            label = { Text("Delivery") },
                                            leadingIcon = {
                                                Icon(Icons.Filled.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = UniAccent.copy(alpha = 0.18f),
                                                selectedLabelColor = UniAccent,
                                                selectedLeadingIconColor = UniAccent
                                            )
                                        )
                                    }

                                    Spacer(Modifier.height(12.dp))

                                    if (fulfillmentMethod == "PICKUP") {
                                        Text(
                                            text = "Recommended UTA pickup spots",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(UtaPickupLocations) { location ->
                                                FilterChip(
                                                    selected = pickupLocation == location,
                                                    onClick = {
                                                        pickupLocation = location
                                                        localError = null
                                                    },
                                                    label = {
                                                        Text(
                                                            location,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = UniAccent.copy(alpha = 0.18f),
                                                        selectedLabelColor = UniAccent
                                                    )
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "Selected: $pickupLocation",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp
                                        )
                                    } else {
                                        OutlinedTextField(
                                            value = deliveryStreet,
                                            onValueChange = {
                                                deliveryStreet = it
                                                localError = null
                                            },
                                            label = { Text("Street Address") },
                                            leadingIcon = { Icon(Icons.Filled.LocationOn, null) },
                                            placeholder = { Text("Example: 300 W 1st St") },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true
                                        )

                                        Spacer(Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = deliveryCity,
                                                onValueChange = {
                                                    deliveryCity = it
                                                    localError = null
                                                },
                                                label = { Text("City") },
                                                placeholder = { Text("Arlington") },
                                                modifier = Modifier.weight(1.25f),
                                                shape = RoundedCornerShape(12.dp),
                                                singleLine = true
                                            )

                                            OutlinedTextField(
                                                value = deliveryZip,
                                                onValueChange = {
                                                    deliveryZip = it.filter { ch -> ch.isDigit() }.take(5)
                                                    localError = null
                                                },
                                                label = { Text("ZIP Code") },
                                                placeholder = { Text("76010") },
                                                modifier = Modifier.weight(1f),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                shape = RoundedCornerShape(12.dp),
                                                singleLine = true
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(3.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Payment Details",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Qty: ${item.quantity}  •  ${item.listing.category}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Seller: ${item.listing.sellerName}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        when (val s = checkoutState) {
            is UiState.Success -> {
                val order = s.data

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                Text(
                                    "Order Confirmed!",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("Order #${order.id}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    formatOrderDate(order.createdAt),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))

                        SummaryRow("Status", order.status, valueColor = UniAccent, bold = true)
                        SummaryRow("Items Purchased", "${orderItemCount(order)}")
                        SummaryRow("Order Total", fmt.format(order.totalAmount), valueColor = UniAccent, bold = true)
                        SummaryRow(
                            "Payment",
                            order.cardLastFour?.let { "Card ending in $it" } ?: "Card processed"
                        )
                        SummaryRow("Fulfillment", fulfillmentLabel(order.fulfillmentMethod))
                        SummaryRow("Location", order.fulfillmentLocation)

                        HorizontalDivider()

                        Text(
                            text = "Purchased Items",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        order.items.forEach { item ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = item.title,
                                        fontWeight = FontWeight.SemiBold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface
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
        containerColor = MaterialTheme.colorScheme.background,
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Order #${order.id}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        formatOrderDate(order.createdAt),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
            Text(
                "${orderItemCount(order)} items",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                fmt.format(order.totalAmount),
                color = UniAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${fulfillmentLabel(order.fulfillmentMethod)}: ${order.fulfillmentLocation}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: Color? = null,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            modifier = Modifier.weight(1.2f),
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
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
            Icon(
                icon,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.size(72.dp)
            )
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
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
