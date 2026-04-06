package com.unimarket.presentation.buyer

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.unimarket.domain.model.Listing
import com.unimarket.presentation.BuyerViewModel
import com.unimarket.presentation.UiState
import com.unimarket.presentation.auth.UniAccent
import com.unimarket.presentation.auth.UniNavy
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingDetailScreen(
    listing   : Listing,
    viewModel : BuyerViewModel,
    onBack    : () -> Unit,
    onCartOpen: () -> Unit
) {
    val cartState by viewModel.cart.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    val cartCount = if (cartState is UiState.Success)
        (cartState as UiState.Success).data.items.size else 0

    LaunchedEffect(Unit) {
        viewModel.toast.collect { msg -> snackbarHost.showSnackbar(msg) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = Color(0xFFF5F7FA),
        topBar = {
            TopAppBar(
                title  = { Text("Product Detail", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = UniNavy, titleContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    BadgedBox(badge = {
                        if (cartCount > 0) Badge { Text("$cartCount") }
                    }) {
                        IconButton(onClick = onCartOpen) {
                            Icon(Icons.Filled.ShoppingCart, null, tint = Color.White)
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = Color.White) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Price", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            fmt.format(listing.price),
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = UniAccent
                        )
                    }
                    Button(
                        onClick  = { viewModel.addToCart(listing.id) },
                        modifier = Modifier.height(52.dp).weight(1.2f),
                        colors   = ButtonDefaults.buttonColors(containerColor = UniAccent),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.AddShoppingCart, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add to Cart", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Product image
            AsyncImage(
                model              = listing.imageUrl
                    ?: "https://placehold.co/600x350/00C896/white?text=${listing.category}",
                contentDescription = listing.title,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            )

            Column(modifier = Modifier.padding(20.dp)) {

                // Category chip
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = UniAccent.copy(alpha = 0.12f),
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    Text(
                        listing.category,
                        modifier   = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color      = UniAccent,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    listing.title,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 28.sp
                )

                Spacer(Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Filled.StoreMallDirectory, null,
                        tint     = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "Sold by ${listing.sellerName}",
                        fontSize = 13.sp,
                        color    = Color.Gray
                    )
                }

                Spacer(Modifier.height(4.dp))

                val dateStr = remember(listing.createdAt) {
                    SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(listing.createdAt))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Filled.CalendarToday, null,
                        tint     = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Text("Listed on $dateStr", fontSize = 12.sp, color = Color.Gray)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                Text("Description", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    listing.description,
                    fontSize   = 14.sp,
                    lineHeight = 22.sp,
                    color      = Color(0xFF4A5568)
                )

                Spacer(Modifier.height(24.dp))

                // Active status indicator
                if (!listing.isActive) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEB)),
                        shape  = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier  = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Warning, null, tint = Color.Red)
                            Text(
                                "This listing is no longer active",
                                color    = Color.Red,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
