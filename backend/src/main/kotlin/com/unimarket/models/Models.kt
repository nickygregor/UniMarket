package com.unimarket.models

import kotlinx.serialization.Serializable

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
    val user: UserResponse
)

@Serializable
data class UserResponse(
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

@Serializable
data class CreateListingRequest(
    val title: String,
    val description: String,
    val price: Double,
    val category: String,
    val condition: String = "Good",
    val sellerContact: String = "",
    val imageUrl: String? = null,
    val expiryDays: Int = 30
)

@Serializable
data class UpdateListingRequest(
    val title: String? = null,
    val description: String? = null,
    val price: Double? = null,
    val category: String? = null,
    val condition: String? = null,
    val sellerContact: String? = null,
    val imageUrl: String? = null,
    val expiryDays: Int? = null
)

@Serializable
data class ListingResponse(
    val id: Int,
    val sellerId: Int,
    val sellerName: String,
    val title: String,
    val description: String,
    val price: Double,
    val category: String,
    val condition: String,
    val sellerContact: String,
    val imageUrl: String?,
    val isActive: Boolean,
    val expiresAt: Long,
    val createdAt: Long
)

@Serializable
data class AddToCartRequest(
    val listingId: Int,
    val quantity: Int = 1
)

@Serializable
data class CartItemResponse(
    val id: Int,
    val listing: ListingResponse,
    val quantity: Int,
    val subtotal: Double
)

@Serializable
data class CartResponse(
    val items: List<CartItemResponse>,
    val totalAmount: Double
)

@Serializable
data class CheckoutRequest(
    val cardNumber: String,
    val cardExpiry: String,
    val cardCvv: String,
    val cardHolder: String,
    val fulfillmentMethod: String = "PICKUP",
    val fulfillmentLocation: String = ""
)

@Serializable
data class OrderItemResponse(
    val listingId: Int,
    val title: String,
    val quantity: Int,
    val priceAtPurchase: Double,
    val subtotal: Double
)

@Serializable
data class OrderResponse(
    val id: Int,
    val buyerId: Int,
    val items: List<OrderItemResponse>,
    val totalAmount: Double,
    val status: String,
    val cardLastFour: String?,
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
data class ListingCommentResponse(
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
data class ChatMessageResponse(
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
data class ConversationResponse(
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
data class SellerNotificationResponse(
    val commentCount: Int,
    val unreadMessageCount: Int
)

@Serializable
data class MessageResponse(val message: String)

@Serializable
data class ErrorResponse(val error: String)
