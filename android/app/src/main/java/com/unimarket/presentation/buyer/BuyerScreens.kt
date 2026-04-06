package com.unimarket.presentation.buyer

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.unimarket.domain.model.*
import com.unimarket.presentation.BuyerViewModel
import com.unimarket.presentation.UiState
import com.unimarket.presentation.auth.UniAccent
import com.unimarket.presentation.auth.UniNavy
import java.text.NumberFormat
import java.util.Locale

val fmt: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US)

// ════════════════════════════════════════════════════════════════════
//  BROWSE / MARKETPLACE SCREEN
// ════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    viewModel     : BuyerViewModel,
    onViewListing : (Listing) -> Unit,
    onCartClick   : () -> Unit,
    onLogout      : () -> Unit,
    onOrdersClick : () -> Unit = {}
) {
    val listingsState by viewModel.listings.collectAsState()
    val cartState     by viewModel.cart.collectAsState()
    var searchQuery   by remember { mutableStateOf("") }

    val cartCount = if (cartState is UiState.Success)
        (cartState as UiState.Success<Cart>).data.items.size else 0

    LaunchedEffect(Unit) {
        viewModel.loadListings()
        viewModel.loadCart()
    }

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        topBar = {
            TopAppBar(
                title = { Text("UniMarket", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = UniNavy, titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = onOrdersClick) {
                        Icon(Icons.Filled.ShoppingBag, null, tint = Color.White)
                    }
                    BadgedBox(
                        badge = { if (cartCount > 0) Badge { Text("$cartCount") } }
                    ) {
                        IconButton(onClick = onCartClick) {
                            Icon(Icons.Filled.ShoppingCart, null, tint = Color.White)
                        }
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Filled.Logout, null, tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            // Search bar
            OutlinedTextField(
                value         = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.loadListings(keyword = it.ifBlank { null })
                },
                placeholder   = { Text("Search products...") },
                leadingIcon   = { Icon(Icons.Filled.Search, null) },
                singleLine    = true,
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape         = RoundedCornerShape(24.dp)
            )

            // Category chips
            CategoryChips { cat -> viewModel.loadListings(category = cat) }

            when (val s = listingsState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = UniAccent)
                }
                is UiState.Error   -> ErrorCard(s.msg) { viewModel.loadListings() }
                is UiState.Success -> {
                    LazyVerticalGrid(
                        columns      = GridCells.Fixed(2),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement   = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(s.data) { listing ->
                            ListingCard(
                                listing   = listing,
                                onClick   = { onViewListing(listing) },
                                onAddCart = { viewModel.addToCart(listing.id) }
                            )
                        }
                    }
                }
                else -> Unit
            }
        }
    }
}

@Composable
fun ListingCard(listing: Listing, onClick: () -> Unit, onAddCart: () -> Unit) {
    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        shape     = RoundedCornerShape(12.dp)
    ) {
        Column {
            AsyncImage(
                model             = listing.imageUrl ?: "https://placehold.co/300x200/1A73E8/white?text=${listing.category}",
                contentDescription = listing.title,
                contentScale      = ContentScale.Crop,
                modifier          = Modifier.fillMaxWidth().height(130.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            )
            Column(modifier = Modifier.padding(10.dp)) {
                Text(listing.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                Text(listing.sellerName, fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(fmt.format(listing.price), fontWeight = FontWeight.Bold, color = UniAccent, fontSize = 15.sp)
                    IconButton(onClick = onAddCart, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.AddShoppingCart, null, tint = UniAccent, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChips(onSelect: (String?) -> Unit) {
    val categories = listOf("All", "Books", "Electronics", "Furniture", "Clothing", "Other")
    var selected   by remember { mutableStateOf("All") }
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier              = Modifier.padding(vertical = 4.dp)
    ) {
        items(categories) { cat ->
            FilterChip(
                selected = selected == cat,
                onClick  = { selected = cat; onSelect(if (cat == "All") null else cat) },
                label    = { Text(cat, fontSize = 12.sp) },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = UniAccent,
                    selectedLabelColor     = Color.White
                )
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════
//  CART SCREEN
// ════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    viewModel  : BuyerViewModel,
    onBack     : () -> Unit,
    onCheckout : () -> Unit
) {
    val cartState      by viewModel.cart.collectAsState()
    val checkoutState  by viewModel.checkoutResult.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadCart() }

    LaunchedEffect(checkoutState) {
        if (checkoutState is UiState.Success) onCheckout()
    }

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        topBar = {
            TopAppBar(
                title  = { Text("My Cart", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = UniNavy, titleContentColor = Color.White),
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null, tint = Color.White) }
                }
            )
        },
        bottomBar = {
            if (cartState is UiState.Success) {
                val cart = (cartState as UiState.Success<Cart>).data
                Surface(shadowElevation = 8.dp) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Total", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(fmt.format(cart.totalAmount), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = UniAccent)
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick  = { viewModel.checkout() },
                            enabled  = cart.items.isNotEmpty() && checkoutState !is UiState.Loading,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = UniAccent),
                            shape    = RoundedCornerShape(12.dp)
                        ) {
                            if (checkoutState is UiState.Loading)
                                CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            else
                                Text("Checkout", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    ) { padding ->
        when (val s = cartState) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = UniAccent) }
            is UiState.Error   -> ErrorCard(s.msg) { viewModel.loadCart() }
            is UiState.Success -> {
                if (s.data.items.isEmpty()) {
                    EmptyState("Your cart is empty", Icons.Filled.ShoppingCart)
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(padding)
                    ) {
                        items(s.data.items, key = { it.id }) { item ->
                            CartItemRow(item = item, onRemove = { viewModel.removeFromCart(item.id) })
                        }
                    }
                }
            }
            else -> Unit
        }
    }
}

@Composable
fun CartItemRow(item: CartItem, onRemove: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        shape     = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier            = Modifier.padding(12.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model              = item.listing.imageUrl ?: "https://placehold.co/80x80",
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
            )
            Column(Modifier.weight(1f)) {
                Text(item.listing.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Qty: ${item.quantity}", fontSize = 12.sp, color = Color.Gray)
                Text(fmt.format(item.subtotal), fontWeight = FontWeight.Bold, color = UniAccent)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, null, tint = Color.Red.copy(alpha = 0.7f))
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
//  ORDER CONFIRMATION SCREEN
// ════════════════════════════════════════════════════════════════════

@Composable
fun OrderConfirmationScreen(viewModel: BuyerViewModel, onDone: () -> Unit) {
    val checkoutState by viewModel.checkoutResult.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF5F7FA)),
        contentAlignment = Alignment.Center
    ) {
        when (val s = checkoutState) {
            is UiState.Success -> {
                val order = s.data
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, null, tint = UniAccent, modifier = Modifier.size(80.dp))
                    Text("Order Confirmed!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Order #${order.id}", color = Color.Gray)
                    HorizontalDivider()
                    order.items.forEach { item ->
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("${item.title} ×${item.quantity}", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(fmt.format(item.subtotal), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Total", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(fmt.format(order.totalAmount), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = UniAccent)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick  = { viewModel.resetCheckout(); onDone() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = UniAccent),
                        shape    = RoundedCornerShape(12.dp)
                    ) { Text("Continue Shopping", fontWeight = FontWeight.Bold) }
                }
            }
            else -> CircularProgressIndicator(color = UniAccent)
        }
    }
}

// ════════════════════════════════════════════════════════════════════
//  SHARED HELPERS
// ════════════════════════════════════════════════════════════════════

@Composable
fun EmptyState(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = Color.LightGray, modifier = Modifier.size(72.dp))
            Text(text, color = Color.Gray, fontSize = 16.sp)
        }
    }
}

@Composable
fun ErrorCard(msg: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Filled.ErrorOutline, null, tint = Color.Red, modifier = Modifier.size(48.dp))
            Text(msg, color = Color.Red)
            TextButton(onClick = onRetry) { Text("Retry") }
        }
    }
}
