package com.unimarket.models

import kotlinx.serialization.Serializable

// ── AUTH ──────────────────────────────────────────────────────────────────────

@Serializable
data class RegisterRequest(
    val firstName   : String,
    val lastName    : String,
    val email       : String,      // must end in @uta.edu
    val phoneNumber : String,
    val userId      : String,
    val password    : String,      // min 8 chars enforced server-side
    val role        : String       // BUYER | SELLER | BUYER_SELLER
)

@Serializable
data class LoginRequest(
    val userId   : String,
    val password : String
)

@Serializable
data class AuthResponse(
    val token : String,
    val user  : UserResponse
)

// ── USER ──────────────────────────────────────────────────────────────────────

@Serializable
data class UserResponse(
    val id          : Int,
    val firstName   : String,
    val lastName    : String,
    val email       : String,
    val phoneNumber : String,
    val userId      : String,
    val role        : String,
    val isActive    : Boolean
)

// ── LISTING ───────────────────────────────────────────────────────────────────

@Serializable
data class CreateListingRequest(
    val title         : String,
    val description   : String,
    val price         : Double,
    val category      : String,
    val condition     : String  = "Good",
    val sellerContact : String,
    val imageUrl      : String? = null,
    val expiryDays    : Int     = 30    // listing expires in N days
)

@Serializable
data class UpdateListingRequest(
    val title         : String? = null,
    val description   : String? = null,
    val price         : Double? = null,
    val category      : String? = null,
    val condition     : String? = null,
    val sellerContact : String? = null,
    val imageUrl      : String? = null,
    val expiryDays    : Int?    = null
)

@Serializable
data class ListingResponse(
    val id            : Int,
    val sellerId      : Int,
    val sellerName    : String,
    val title         : String,
    val description   : String,
    val price         : Double,
    val category      : String,
    val condition     : String,
    val sellerContact : String,
    val imageUrl      : String?,
    val isActive      : Boolean,
    val expiresAt     : Long,
    val createdAt     : Long
)

// ── CART ──────────────────────────────────────────────────────────────────────

@Serializable
data class AddToCartRequest(
    val listingId : Int,
    val quantity  : Int = 1
)

@Serializable
data class CartItemResponse(
    val id       : Int,
    val listing  : ListingResponse,
    val quantity : Int,
    val subtotal : Double
)

@Serializable
data class CartResponse(
    val items       : List<CartItemResponse>,
    val totalAmount : Double
)

// ── CHECKOUT ──────────────────────────────────────────────────────────────────

@Serializable
data class CheckoutRequest(
    val cardNumber  : String,   // 16 digits - validated, only last 4 stored
    val cardExpiry  : String,   // MM/YY
    val cardCvv     : String,   // 3-4 digits
    val cardHolder  : String
)

// ── ORDER ─────────────────────────────────────────────────────────────────────

@Serializable
data class OrderItemResponse(
    val listingId       : Int,
    val title           : String,
    val quantity        : Int,
    val priceAtPurchase : Double,
    val subtotal        : Double
)

@Serializable
data class OrderResponse(
    val id           : Int,
    val buyerId      : Int,
    val items        : List<OrderItemResponse>,
    val totalAmount  : Double,
    val status       : String,
    val cardLastFour : String?,
    val createdAt    : Long
)

// ── GENERIC ───────────────────────────────────────────────────────────────────

@Serializable data class MessageResponse(val message : String)
@Serializable data class ErrorResponse  (val error   : String)
