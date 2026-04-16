package com.unimarket.database

import org.jetbrains.exposed.dao.id.IntIdTable

object Users : IntIdTable("users") {
    val firstName    = varchar("first_name",    100)
    val lastName     = varchar("last_name",     100)
    val email        = varchar("email",         255).uniqueIndex()
    val phoneNumber  = varchar("phone_number",   20)
    val userId       = varchar("user_id",       100).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    // BUYER | SELLER | BUYER_SELLER | ADMIN
    val role         = varchar("role",           20)
    val isActive     = bool("is_active").default(true)
    val createdAt    = long("created_at")
}

object Listings : IntIdTable("listings") {
    val sellerId      = integer("seller_id").references(Users.id)
    val title         = varchar("title",       255)
    val description   = text("description")
    val price         = double("price")
    val category      = varchar("category",    100)
    val condition     = varchar("condition",    50).default("Good")  // New | Like New | Good | Fair | Poor
    val sellerContact = varchar("seller_contact", 255)               // phone or email
    val imageUrl      = text("image_url").nullable()
    val isActive      = bool("is_active").default(true)
    val expiresAt     = long("expires_at")                           // epoch ms
    val createdAt     = long("created_at")
}

object CartItems : IntIdTable("cart_items") {
    val buyerId   = integer("buyer_id").references(Users.id)
    val listingId = integer("listing_id").references(Listings.id)
    val quantity  = integer("quantity").default(1)
}

object Orders : IntIdTable("orders") {
    val buyerId         = integer("buyer_id").references(Users.id)
    val totalAmount     = double("total_amount")
    val status          = varchar("status", 50).default("CONFIRMED")
    // Mock credit card - store last 4 digits only
    val cardLastFour    = varchar("card_last_four", 4).nullable()
    val createdAt       = long("created_at")
}

object OrderItems : IntIdTable("order_items") {
    val orderId         = integer("order_id").references(Orders.id)
    val listingId       = integer("listing_id").references(Listings.id)
    val quantity        = integer("quantity")
    val priceAtPurchase = double("price_at_purchase")
}
