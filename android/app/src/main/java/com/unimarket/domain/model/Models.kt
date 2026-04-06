package com.unimarket.domain.model

import kotlinx.serialization.Serializable

// ════════════════════════════════════════════════════════════════════
//  AUTH
// ════════════════════════════════════════════════════════════════════

@Serializable
data class RegisterRequest(
    val firstName   : String,
    val lastName    : String,
    val email       : String,
    val phoneNumber : String,
    val userId      : String,
    val password    : String,
    val role        : String
)

@Serializable
data class LoginRequest(
    val userId   : String,
    val password : String
)

@Serializable
data class AuthResponse(
    val token : String,
    val user  : User
)

// ════════════════════════════════════════════════════════════════════
//  USER
// ════════════════════════════════════════════════════════════════════

@Serializable
data class User(
    val id          : Int,
    val firstName   : String,
    val lastName    : String,
    val email       : String,
    val phoneNumber : String,
    val userId      : String,
    val role        : String,
    val isActive    : Boolean
)

// ════════════════════════════════════════════════════════════════════
//  LISTING
// ════════════════════════════════════════════════════════════════════

@Serializable
data class Listing(
    val id          : Int,
    val sellerId    : Int,
    val sellerName  : String,
    val title       : String,
    val description : String,
    val price       : Double,
    val category    : String,
    val imageUrl    : String?,
    val isActive    : Boolean,
    val createdAt   : Long
)

@Serializable
data class CreateListingRequest(
    val title       : String,
    val description : String,
    val price       : Double,
    val category    : String,
    val imageUrl    : String? = null
)

@Serializable
data class UpdateListingRequest(
    val title       : String? = null,
    val description : String? = null,
    val price       : Double? = null,
    val category    : String? = null,
    val imageUrl    : String? = null
)

// ════════════════════════════════════════════════════════════════════
//  CART
// ════════════════════════════════════════════════════════════════════

@Serializable
data class AddToCartRequest(
    val listingId : Int,
    val quantity  : Int = 1
)

@Serializable
data class CartItem(
    val id       : Int,
    val listing  : Listing,
    val quantity : Int,
    val subtotal : Double
)

@Serializable
data class Cart(
    val items       : List<CartItem>,
    val totalAmount : Double
)

// ════════════════════════════════════════════════════════════════════
//  ORDER
// ════════════════════════════════════════════════════════════════════

@Serializable
data class OrderItem(
    val listingId       : Int,
    val title           : String,
    val quantity        : Int,
    val priceAtPurchase : Double,
    val subtotal        : Double
)

@Serializable
data class Order(
    val id          : Int,
    val buyerId     : Int,
    val items       : List<OrderItem>,
    val totalAmount : Double,
    val status      : String,
    val createdAt   : Long
)

// ════════════════════════════════════════════════════════════════════
//  GENERIC
// ════════════════════════════════════════════════════════════════════

@Serializable
data class MessageResponse(val message : String)
@Serializable
data class ErrorResponse  (val error   : String)
