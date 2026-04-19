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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MarkUnreadChatAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.unimarket.ChatTarget
import com.unimarket.domain.model.Conversation
import com.unimarket.domain.model.Listing
import com.unimarket.domain.model.ListingComment
import com.unimarket.presentation.SellerViewModel
import com.unimarket.presentation.UiState
import com.unimarket.presentation.auth.UniAccent
import com.unimarket.presentation.auth.UniBlue
import com.unimarket.presentation.auth.UniNavy
import com.unimarket.presentation.buyer.ChatScreenScaffold
import com.unimarket.presentation.buyer.ConversationCard
import com.unimarket.presentation.buyer.EmptyState
import com.unimarket.presentation.buyer.ErrorCard
import com.unimarket.presentation.buyer.fmt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.math.max

private val FormTextColor = Color(0xFF132033)
private val FormMutedText = Color(0xFF6B7280)
private val FormBgColor = Color(0xFFF5F7FA)
private const val MaxListingImages = 4
private val ListingConditions = listOf("New", "Like New", "Good", "Fairly Used", "Used")
private val ListingExpiryOptions = listOf(30, 60, 90)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerDashboardScreen(
    viewModel: SellerViewModel,
    onAddListing: () -> Unit,
    onEdit: (Listing) -> Unit,
    onLogout: () -> Unit,
    onProfileClick: () -> Unit,
    onMessagesClick: () -> Unit,
    onCommentsClick: () -> Unit
) {
    val listingsState by viewModel.listings.collectAsState()
    val commentsState by viewModel.sellerComments.collectAsState()
    val notificationsState by viewModel.notifications.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val notifications = (notificationsState as? UiState.Success)?.data
    val unreadMessages = notifications?.unreadMessageCount ?: 0
    val commentCount = notifications?.commentCount ?: 0

    LaunchedEffect(Unit) {
        viewModel.loadMyListings()
        viewModel.loadSellerInteractions()
    }

    LaunchedEffect(Unit) {
        viewModel.toast.collect { msg ->
            snackbarHost.showSnackbar(msg)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                    IconButton(onClick = onCommentsClick) {
                        BadgedBox(
                            badge = {
                                if (commentCount > 0) {
                                    Badge { Text("$commentCount") }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.QuestionAnswer, contentDescription = "Comments", tint = Color.White)
                        }
                    }
                    IconButton(onClick = onMessagesClick) {
                        BadgedBox(
                            badge = {
                                if (unreadMessages > 0) {
                                    Badge { Text("$unreadMessages") }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.MarkUnreadChatAlt, contentDescription = "Messages", tint = Color.White)
                        }
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
                        item {
                            SellerNotificationCard(
                                notifications = notifications,
                                onMessagesClick = onMessagesClick,
                                onCommentsClick = onCommentsClick
                            )
                        }

                        items(s.data, key = { it.id }) { listing ->
                            val listingComments = (commentsState as? UiState.Success<List<ListingComment>>)
                                ?.data
                                .orEmpty()
                                .filter { it.listingId == listing.id && it.parentCommentId == null }
                            SellerListingCard(
                                listing = listing,
                                comments = listingComments,
                                onReply = { comment, message ->
                                    viewModel.replyToComment(listing.id, comment.id, message)
                                },
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
    comments: List<ListingComment>,
    onReply: (ListingComment, String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var replyingTo by remember { mutableStateOf<ListingComment?>(null) }
    var replyText by remember { mutableStateOf("") }

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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "${listing.category} • ${listing.condition}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = expiryStatusText(listing),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = UniAccent)
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.75f))
                }
            }
        }

        if (comments.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Comments from buyers",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                comments.take(3).forEach { comment ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                comment.authorName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                comment.message,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            TextButton(onClick = {
                                replyingTo = comment
                                replyText = ""
                            }) {
                                Icon(Icons.Filled.Reply, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Reply")
                            }
                        }
                    }
                }
            }
        }
    }

    replyingTo?.let { comment ->
        AlertDialog(
            onDismissRequest = { replyingTo = null },
            title = { Text("Reply to ${comment.authorName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(comment.message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        label = { Text("Your reply") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (replyText.isNotBlank()) {
                            onReply(comment, replyText.trim())
                            replyingTo = null
                            replyText = ""
                        }
                    },
                    enabled = replyText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = UniAccent)
                ) {
                    Text("Reply")
                }
            },
            dismissButton = {
                TextButton(onClick = { replyingTo = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SellerNotificationCard(
    notifications: com.unimarket.domain.model.SellerNotificationSummary?,
    onMessagesClick: () -> Unit,
    onCommentsClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.MarkUnreadChatAlt, null, tint = UniAccent, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Seller notifications", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "${notifications?.commentCount ?: 0} comments on your listings • ${notifications?.unreadMessageCount ?: 0} unread messages",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalButton(onClick = onCommentsClick) {
                    Text("Comments")
                }
                FilledTonalButton(onClick = onMessagesClick) {
                    Text("Messages")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerCommentsScreen(
    viewModel: SellerViewModel,
    onBack: () -> Unit
) {
    val commentsState by viewModel.sellerComments.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadSellerComments()
        viewModel.loadNotifications()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Listing Comments", fontWeight = FontWeight.Bold) },
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
        when (val state = commentsState) {
            is UiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = UniAccent)
            }
            is UiState.Error -> Box(Modifier.fillMaxSize().padding(padding)) {
                ErrorCard(state.msg) { viewModel.loadSellerComments() }
            }
            is UiState.Success -> {
                val parentComments = state.data.filter { it.parentCommentId == null }
                if (parentComments.isEmpty()) {
                    EmptyState("No listing comments yet", Icons.Filled.QuestionAnswer)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(parentComments, key = { it.id }) { comment ->
                            SellerCommentInboxCard(
                                comment = comment,
                                replies = state.data.filter { it.parentCommentId == comment.id },
                                onReply = { message ->
                                    viewModel.replyToComment(comment.listingId, comment.id, message)
                                }
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
private fun SellerCommentInboxCard(
    comment: ListingComment,
    replies: List<ListingComment>,
    onReply: (String) -> Unit
) {
    var replyText by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(comment.authorName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    com.unimarket.presentation.buyer.formatOrderDate(comment.createdAt),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
            Text(comment.message, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)

            replies.forEach { reply ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp),
                    color = UniAccent.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Text(reply.authorName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(reply.message, fontSize = 12.sp)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = { Text("Reply publicly...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    maxLines = 3
                )
                IconButton(
                    onClick = {
                        if (replyText.isNotBlank()) {
                            onReply(replyText.trim())
                            replyText = ""
                        }
                    },
                    enabled = replyText.isNotBlank()
                ) {
                    Icon(Icons.Filled.Send, null, tint = UniAccent)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerMessagesScreen(
    viewModel: SellerViewModel,
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
                        Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White)
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
                    EmptyState("No messages yet", Icons.Filled.MarkUnreadChatAlt)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.data) { conversation ->
                            SellerConversationCard(
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
private fun SellerConversationCard(
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
            SellerListingImage(
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
fun SellerChatScreen(
    viewModel: SellerViewModel,
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

private fun expiryDaysFromListing(listing: Listing?): Int {
    if (listing == null || listing.expiresAt <= 0L) return 30

    val remainingDays = ((listing.expiresAt - System.currentTimeMillis()) / 86_400_000L)
        .coerceAtLeast(1L)
        .toInt()

    return ListingExpiryOptions.minBy { option -> abs(option - remainingDays) }
}

private fun expiryOptionLabel(days: Int): String = "$days days"

private fun expiryStatusText(listing: Listing): String {
    if (listing.expiresAt <= 0L) return "Expires after 30 days"

    val remainingDays = ((listing.expiresAt - System.currentTimeMillis()) / 86_400_000L)
        .coerceAtLeast(0L)

    return if (remainingDays == 0L) {
        "Expires today"
    } else {
        "Expires in $remainingDays day${if (remainingDays == 1L) "" else "s"}"
    }
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
    ListingOptionSelector(
        label = "Category *",
        selectedValue = selectedCategory,
        options = categories,
        leadingIcon = Icons.Filled.Category,
        onOptionSelected = onCategorySelected
    )
}

@Composable
private fun ListingOptionSelector(
    label: String,
    selectedValue: String,
    options: List<String>,
    leadingIcon: ImageVector,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(6.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                            leadingIcon,
                            contentDescription = null,
                            tint = UniAccent
                        )
                        Text(
                            text = selectedValue,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        )
                    }

                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .background(MaterialTheme.colorScheme.surface)
                    .offset(y = 4.dp)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onOptionSelected(option)
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
    var condition by remember { mutableStateOf(existingListing?.condition ?: "Good") }
    var expiryDays by remember { mutableStateOf(expiryDaysFromListing(existingListing)) }
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
        containerColor = MaterialTheme.colorScheme.background,
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
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "All fields below are required, including the image link.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = UniAccent,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = UniAccent,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
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
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = UniAccent,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = UniAccent,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
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
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = UniAccent,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = UniAccent,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
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
                ListingOptionSelector(
                    label = "Item Condition *",
                    selectedValue = condition,
                    options = ListingConditions,
                    leadingIcon = Icons.Filled.Inventory2,
                    onOptionSelected = {
                        condition = it
                        localError = null
                    }
                )
            }

            item {
                ListingOptionSelector(
                    label = "Ad Expiry *",
                    selectedValue = expiryOptionLabel(expiryDays),
                    options = ListingExpiryOptions.map(::expiryOptionLabel),
                    leadingIcon = Icons.Filled.CalendarMonth,
                    onOptionSelected = {
                        expiryDays = it.substringBefore(" ").toIntOrNull() ?: 30
                        localError = null
                    }
                )
            }

            item {
                Text(
                    text = "Add Images",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground
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
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = UniAccent,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = UniAccent,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            item {
                Text(
                    text = "Choose up to $MaxListingImages images from Photos or Files on the device, or paste one direct image link. The first image is used on listing cards.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    condition.isBlank() ||
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
                                        trimmedImageUrl,
                                        condition,
                                        expiryDays
                                    )
                                } else {
                                    viewModel.createListing(
                                        trimmedTitle,
                                        trimmedDescription,
                                        parsedPrice,
                                        category,
                                        trimmedImageUrl,
                                        condition,
                                        expiryDays
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
