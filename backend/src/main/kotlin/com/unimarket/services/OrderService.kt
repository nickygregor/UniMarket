package com.unimarket.services

import com.unimarket.database.OrderItems
import com.unimarket.database.Orders
import com.unimarket.database.Listings
import com.unimarket.database.dbQuery
import com.unimarket.models.CheckoutRequest
import com.unimarket.models.OrderItemResponse
import com.unimarket.models.OrderResponse
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object OrderService {

    suspend fun checkout(buyerId: Int, req: CheckoutRequest): OrderResponse {
        val digitsOnly = req.cardNumber.replace(" ", "")
        if (!digitsOnly.matches(Regex("\\d{16}"))) {
            throw IllegalArgumentException("Invalid card number — must be 16 digits")
        }

        if (!req.cardExpiry.matches(Regex("(0[1-9]|1[0-2])/\\d{2}"))) {
            throw IllegalArgumentException("Invalid expiry date — use MM/YY format")
        }

        if (!req.cardCvv.matches(Regex("\\d{3,4}"))) {
            throw IllegalArgumentException("Invalid CVV")
        }

        val fulfillmentMethod = req.fulfillmentMethod.uppercase()
        if (fulfillmentMethod !in setOf("PICKUP", "DELIVERY")) {
            throw IllegalArgumentException("Choose pickup or delivery")
        }

        val fulfillmentLocation = req.fulfillmentLocation.trim()
        if (fulfillmentLocation.isBlank()) {
            throw IllegalArgumentException("Pickup or delivery location is required")
        }

        val cart = CartService.getCart(buyerId)
        if (cart.items.isEmpty()) {
            throw IllegalStateException("Cart is empty")
        }

        if (cart.items.any { it.listing.sellerId == buyerId }) {
            throw IllegalStateException("You can't buy your own listing")
        }

        if (cart.items.any { !it.listing.isActive }) {
            throw IllegalStateException("Remove unavailable items from your cart before checkout")
        }

        val lastFour = digitsOnly.takeLast(4)

        val orderId = dbQuery {
            val oid = Orders.insertAndGetId {
                it[Orders.buyerId] = buyerId
                it[Orders.totalAmount] = cart.totalAmount
                it[Orders.status] = "CONFIRMED"
                it[Orders.cardLastFour] = lastFour
                it[Orders.fulfillmentMethod] = fulfillmentMethod
                it[Orders.fulfillmentLocation] = fulfillmentLocation
                it[Orders.createdAt] = System.currentTimeMillis()
            }.value

            cart.items.forEach { item ->
                OrderItems.insert {
                    it[OrderItems.orderId] = oid
                    it[OrderItems.listingId] = item.listing.id
                    it[OrderItems.quantity] = item.quantity
                    it[OrderItems.priceAtPurchase] = item.listing.price
                }
            }

            oid
        }

        cart.items.forEach { item ->
            ListingService.markSold(item.listing.id)
        }

        CartService.clearCart(buyerId)
        return getOrder(orderId)!!
    }

    suspend fun getOrder(orderId: Int): OrderResponse? = dbQuery {
        val orderRow = Orders.selectAll()
            .where { Orders.id eq orderId }
            .firstOrNull() ?: return@dbQuery null

        val items = OrderItems
            .join(Listings, JoinType.INNER, OrderItems.listingId, Listings.id)
            .selectAll()
            .where { OrderItems.orderId eq orderId }
            .map { row ->
                val qty = row[OrderItems.quantity]
                val price = row[OrderItems.priceAtPurchase]
                OrderItemResponse(
                    listingId = row[Listings.id].value,
                    title = row[Listings.title],
                    quantity = qty,
                    priceAtPurchase = price,
                    subtotal = qty * price
                )
            }

        OrderResponse(
            id = orderRow[Orders.id].value,
            buyerId = orderRow[Orders.buyerId],
            items = items,
            totalAmount = orderRow[Orders.totalAmount],
            status = orderRow[Orders.status],
            cardLastFour = orderRow[Orders.cardLastFour],
            fulfillmentMethod = orderRow[Orders.fulfillmentMethod],
            fulfillmentLocation = orderRow[Orders.fulfillmentLocation],
            createdAt = orderRow[Orders.createdAt]
        )
    }

    suspend fun getOrdersByBuyer(buyerId: Int): List<OrderResponse> = dbQuery {
        Orders.selectAll()
            .where { Orders.buyerId eq buyerId }
            .orderBy(Orders.createdAt, SortOrder.DESC)
            .mapNotNull { orderRow ->
                val oid = orderRow[Orders.id].value
                val items = OrderItems
                    .join(Listings, JoinType.INNER, OrderItems.listingId, Listings.id)
                    .selectAll()
                    .where { OrderItems.orderId eq oid }
                    .map { row ->
                        val qty = row[OrderItems.quantity]
                        val price = row[OrderItems.priceAtPurchase]
                        OrderItemResponse(
                            listingId = row[Listings.id].value,
                            title = row[Listings.title],
                            quantity = qty,
                            priceAtPurchase = price,
                            subtotal = qty * price
                        )
                    }

                OrderResponse(
                    id = oid,
                    buyerId = orderRow[Orders.buyerId],
                    items = items,
                    totalAmount = orderRow[Orders.totalAmount],
                    status = orderRow[Orders.status],
                    cardLastFour = orderRow[Orders.cardLastFour],
                    fulfillmentMethod = orderRow[Orders.fulfillmentMethod],
                    fulfillmentLocation = orderRow[Orders.fulfillmentLocation],
                    createdAt = orderRow[Orders.createdAt]
                )
            }
    }
}
