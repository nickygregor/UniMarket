package com.unimarket.services

import com.unimarket.database.Users
import com.unimarket.database.Listings
import com.unimarket.database.Orders
import com.unimarket.database.OrderItems
import com.unimarket.database.dbQuery
import com.unimarket.models.UserResponse
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object AdminService {

    suspend fun getAllUsers(): List<UserResponse> = dbQuery {
        val userRows = Users.selectAll().toList()
        val userIds = userRows.map { it[Users.id].value }

        val listingCounts = if (userIds.isEmpty()) {
            emptyMap()
        } else {
            Listings.select(Listings.sellerId, Listings.id.count())
                .where { Listings.sellerId inList userIds }
                .groupBy(Listings.sellerId)
                .associate { row ->
                    row[Listings.sellerId] to row[Listings.id.count()].toInt()
                }
        }

        val activeListingCounts = if (userIds.isEmpty()) {
            emptyMap()
        } else {
            Listings.select(Listings.sellerId, Listings.id.count())
                .where { (Listings.sellerId inList userIds) and (Listings.isActive eq true) }
                .groupBy(Listings.sellerId)
                .associate { row ->
                    row[Listings.sellerId] to row[Listings.id.count()].toInt()
                }
        }

        val orderCounts = if (userIds.isEmpty()) {
            emptyMap()
        } else {
            Orders.select(Orders.buyerId, Orders.id.count())
                .where { Orders.buyerId inList userIds }
                .groupBy(Orders.buyerId)
                .associate { row ->
                    row[Orders.buyerId] to row[Orders.id.count()].toInt()
                }
        }

        val itemCounts = if (userIds.isEmpty()) {
            emptyMap()
        } else {
            (Orders innerJoin OrderItems)
                .select(Orders.buyerId, OrderItems.quantity.sum())
                .where { Orders.buyerId inList userIds }
                .groupBy(Orders.buyerId)
                .associate { row ->
                    row[Orders.buyerId] to (row[OrderItems.quantity.sum()]?.toInt() ?: 0)
                }
        }

        userRows.map { row ->
            val userId = row[Users.id].value
            UserResponse(
                id          = userId,
                firstName   = row[Users.firstName],
                lastName    = row[Users.lastName],
                email       = row[Users.email],
                phoneNumber = row[Users.phoneNumber],
                userId      = row[Users.userId],
                role        = row[Users.role],
                isActive    = row[Users.isActive],
                listingsPosted = listingCounts[userId] ?: 0,
                activeListings = activeListingCounts[userId] ?: 0,
                ordersPlaced = orderCounts[userId] ?: 0,
                itemsBought = itemCounts[userId] ?: 0
            )
        }
    }

    suspend fun setUserActive(userId: Int, active: Boolean) {
        dbQuery {
            val targetUser = Users.selectAll()
                .where { Users.id eq userId }
                .firstOrNull() ?: throw NoSuchElementException("User not found")

            if (targetUser[Users.role] == "ADMIN") {
                throw IllegalArgumentException("Admin accounts cannot be deactivated")
            }

            val updated = Users.update({ Users.id eq userId }) {
                it[Users.isActive] = active
            }
            if (updated == 0) throw NoSuchElementException("User not found")
        }
    }

    suspend fun removeListingAdmin(listingId: Int) = ListingService.adminDelete(listingId)
}
