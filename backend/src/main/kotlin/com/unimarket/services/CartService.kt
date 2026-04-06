package com.unimarket.services

import com.unimarket.database.CartItems
import com.unimarket.database.Listings
import com.unimarket.database.Users
import com.unimarket.database.dbQuery
import com.unimarket.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object CartService {

    suspend fun getCart(buyerId: Int): CartResponse = dbQuery {
        val rows = (CartItems
            .join(Listings, JoinType.INNER, CartItems.listingId, Listings.id)
            .join(Users,    JoinType.INNER, Listings.sellerId,   Users.id))
            .selectAll()
            .where { CartItems.buyerId eq buyerId }
            .toList()

        val items = rows.map { row ->
            val price = row[Listings.price]
            val qty   = row[CartItems.quantity]
            CartItemResponse(
                id       = row[CartItems.id].value,
                quantity = qty,
                subtotal = price * qty,
                listing  = ListingResponse(
                    id            = row[Listings.id].value,
                    sellerId      = row[Listings.sellerId],
                    sellerName    = "${row[Users.firstName]} ${row[Users.lastName]}",
                    title         = row[Listings.title],
                    description   = row[Listings.description],
                    price         = price,
                    category      = row[Listings.category],
                    condition     = row[Listings.condition],
                    sellerContact = row[Listings.sellerContact],
                    imageUrl      = row[Listings.imageUrl],
                    isActive      = row[Listings.isActive],
                    expiresAt     = row[Listings.expiresAt],
                    createdAt     = row[Listings.createdAt]
                )
            )
        }
        CartResponse(items = items, totalAmount = items.sumOf { it.subtotal })
    }

    suspend fun addItem(buyerId: Int, req: AddToCartRequest) {
        dbQuery {
            Listings.selectAll()
                .where { (Listings.id eq req.listingId) and (Listings.isActive eq true) }
                .firstOrNull()
        } ?: throw NoSuchElementException("Listing not found or inactive")

        dbQuery {
            val existing = CartItems.selectAll()
                .where { (CartItems.buyerId eq buyerId) and (CartItems.listingId eq req.listingId) }
                .firstOrNull()

            if (existing != null) {
                CartItems.update({
                    (CartItems.buyerId eq buyerId) and (CartItems.listingId eq req.listingId)
                }) { it[quantity] = existing[CartItems.quantity] + req.quantity }
            } else {
                CartItems.insert {
                    it[CartItems.buyerId]   = buyerId
                    it[CartItems.listingId] = req.listingId
                    it[CartItems.quantity]  = req.quantity
                }
            }
        }
    }

    suspend fun removeItem(buyerId: Int, cartItemId: Int) {
        dbQuery {
            CartItems.deleteWhere {
                (CartItems.id eq cartItemId) and (CartItems.buyerId eq buyerId)
            }
        }
    }

    suspend fun clearCart(buyerId: Int) {
        dbQuery { CartItems.deleteWhere { CartItems.buyerId eq buyerId } }
    }
}
