package com.unimarket.services

import com.unimarket.database.*
import com.unimarket.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object OrderService {

    suspend fun checkout(buyerId: Int, req: CheckoutRequest): OrderResponse {
        // Validate card number (16 digits)
        val digitsOnly = req.cardNumber.replace(" ", "")
        if (!digitsOnly.matches(Regex("\\d{16}")))
            throw IllegalArgumentException("Invalid card number — must be 16 digits")

        // Validate expiry MM/YY
        if (!req.cardExpiry.matches(Regex("(0[1-9]|1[0-2])/\\d{2}")))
            throw IllegalArgumentException("Invalid expiry date — use MM/YY format")

        // Validate CVV
        if (!req.cardCvv.matches(Regex("\\d{3,4}")))
            throw IllegalArgumentException("Invalid CVV")

        val cart = CartService.getCart(buyerId)
        if (cart.items.isEmpty()) throw IllegalStateException("Cart is empty")

        val lastFour = digitsOnly.takeLast(4)

        val orderId = dbQuery {
            val oid = Orders.insertAndGetId {
                it[Orders.buyerId]      = buyerId
                it[Orders.totalAmount]  = cart.totalAmount
                it[Orders.status]       = "CONFIRMED"
                it[Orders.cardLastFour] = lastFour
                it[Orders.createdAt]    = System.currentTimeMillis()
            }.value

            cart.items.forEach { item ->
                OrderItems.insert {
                    it[OrderItems.orderId]         = oid
                    it[OrderItems.listingId]       = item.listing.id
                    it[OrderItems.quantity]        = item.quantity
                    it[OrderItems.priceAtPurchase] = item.listing.price
                }
            }
            oid
        }

        // Mark each purchased listing as sold (disappears from browse)
        cart.items.forEach { item ->
            ListingService.markSold(item.listing.id)
        }

        CartService.clearCart(buyerId)
        return getOrder(orderId)!!
    }

    suspend fun getOrder(orderId: Int): OrderResponse? = dbQuery {
        val orderRow = Orders.selectAll()
            .where { Orders.id eq orderId }.firstOrNull() ?: return@dbQuery null

        val items = OrderItems
            .join(Listings, JoinType.INNER, OrderItems.listingId, Listings.id)
            .selectAll()
            .where { OrderItems.orderId eq orderId }
            .map { row ->
                val qty   = row[OrderItems.quantity]
                val price = row[OrderItems.priceAtPurchase]
                OrderItemResponse(
                    listingId       = row[Listings.id].value,
                    title           = row[Listings.title],
                    quantity        = qty,
                    priceAtPurchase = price,
                    subtotal        = qty * price
                )
            }

        OrderResponse(
            id           = orderRow[Orders.id].value,
            buyerId      = orderRow[Orders.buyerId],
            items        = items,
            totalAmount  = orderRow[Orders.totalAmount],
            status       = orderRow[Orders.status],
            cardLastFour = orderRow[Orders.cardLastFour],
            createdAt    = orderRow[Orders.createdAt]
        )
    }

    suspend fun getOrdersByBuyer(buyerId: Int): List<OrderResponse> = dbQuery {
        Orders.selectAll()
            .where { Orders.buyerId eq buyerId }
            .orderBy(Orders.createdAt, SortOrder.DESC)
            .mapNotNull { orderRow ->
                val oid   = orderRow[Orders.id].value
                val items = OrderItems
                    .join(Listings, JoinType.INNER, OrderItems.listingId, Listings.id)
                    .selectAll()
                    .where { OrderItems.orderId eq oid }
                    .map { r ->
                        val qty   = r[OrderItems.quantity]
                        val price = r[OrderItems.priceAtPurchase]
                        OrderItemResponse(r[Listings.id].value, r[Listings.title], qty, price, qty * price)
                    }
                OrderResponse(
                    id           = oid,
                    buyerId      = orderRow[Orders.buyerId],
                    items        = items,
                    totalAmount  = orderRow[Orders.totalAmount],
                    status       = orderRow[Orders.status],
                    cardLastFour = orderRow[Orders.cardLastFour],
                    createdAt    = orderRow[Orders.createdAt]
                )
            }
    }
}
