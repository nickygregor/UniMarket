package com.unimarket.domain.model

import kotlinx.serialization.Serializable

// AUTH

@Serializable
data class RegisterRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val userId: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val userId: String,
    val password: String
)

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: User
)

// USER

@Serializable
data class User(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val userId: String,
    val role: String,
    val isActive: Boolean,
    val listingsPosted: Int = 0,
    val activeListings: Int = 0,
    val ordersPlaced: Int = 0,
    val itemsBought: Int = 0
)

// LISTING

@Serializable
data class Listing(
    val id: Int,
    val sellerId: Int,
    val sellerName: String,
    val title: String,
    val description: String,
    val price: Double,
    val category: String,
    val condition: String = "Good",
    val sellerContact: String = "",
    val imageUrl: String?,
    val isActive: Boolean,
    val expiresAt: Long = 0L,
    val createdAt: Long
)

@Serializable
data class CreateListingRequest(
    val title: String,
    val description: String,
    val price: Double,
    val category: String,
    val condition: String = "Good",
    val imageUrl: String? = null,
    val sellerContact: String = "",
    val expiryDays: Int = 30
)

@Serializable
data class UpdateListingRequest(
    val title: String? = null,
    val description: String? = null,
    val price: Double? = null,
    val category: String? = null,
    val condition: String? = null,
    val imageUrl: String? = null,
    val expiryDays: Int? = null
)

// CART

@Serializable
data class AddToCartRequest(
    val listingId: Int,
    val quantity: Int = 1
)

@Serializable
data class CartItem(
    val id: Int,
    val listing: Listing,
    val quantity: Int,
    val subtotal: Double
)

@Serializable
data class Cart(
    val items: List<CartItem>,
    val totalAmount: Double
)

// CHECKOUT

@Serializable
data class CheckoutRequest(
    val cardNumber: String,
    val cardExpiry: String,
    val cardCvv: String,
    val cardHolder: String,
    val fulfillmentMethod: String = "PICKUP",
    val fulfillmentLocation: String = ""
)

// ORDER

@Serializable
data class OrderItem(
    val listingId: Int,
    val title: String,
    val quantity: Int,
    val priceAtPurchase: Double,
    val subtotal: Double
)

@Serializable
data class Order(
    val id: Int,
    val buyerId: Int,
    val items: List<OrderItem>,
    val totalAmount: Double,
    val status: String,
    val cardLastFour: String? = null,
    val fulfillmentMethod: String = "PICKUP",
    val fulfillmentLocation: String = "",
    val createdAt: Long
)

@Serializable
data class CreateCommentRequest(
    val message: String,
    val parentCommentId: Int? = null
)

@Serializable
data class ListingComment(
    val id: Int,
    val listingId: Int,
    val authorId: Int,
    val authorName: String,
    val authorRole: String,
    val parentCommentId: Int? = null,
    val message: String,
    val createdAt: Long
)

@Serializable
data class SendMessageRequest(
    val message: String
)

@Serializable
data class ChatMessage(
    val id: Int,
    val listingId: Int,
    val listingTitle: String,
    val buyerId: Int,
    val buyerName: String,
    val sellerId: Int,
    val sellerName: String,
    val senderId: Int,
    val senderName: String,
    val message: String,
    val createdAt: Long,
    val readAt: Long? = null
)

@Serializable
data class Conversation(
    val listingId: Int,
    val listingTitle: String,
    val listingImageUrl: String?,
    val otherUserId: Int,
    val otherUserName: String,
    val lastMessage: String,
    val lastMessageAt: Long,
    val unreadCount: Int
)

@Serializable
data class SellerNotificationSummary(
    val commentCount: Int,
    val unreadMessageCount: Int
)

// GENERIC

@Serializable
data class MessageResponse(val message: String)

@Serializable
data class ErrorResponse(val error: String)
