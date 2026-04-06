package com.unimarket.services

import com.unimarket.database.Users
import com.unimarket.database.dbQuery
import com.unimarket.models.UserResponse
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object AdminService {

    suspend fun getAllUsers(): List<UserResponse> = dbQuery {
        Users.selectAll().map { row ->
            UserResponse(
                id          = row[Users.id].value,
                firstName   = row[Users.firstName],
                lastName    = row[Users.lastName],
                email       = row[Users.email],
                phoneNumber = row[Users.phoneNumber],
                userId      = row[Users.userId],
                role        = row[Users.role],
                isActive    = row[Users.isActive]
            )
        }
    }

    suspend fun setUserActive(userId: Int, active: Boolean) {
        dbQuery {
            val updated = Users.update({ Users.id eq userId }) {
                it[Users.isActive] = active
            }
            if (updated == 0) throw NoSuchElementException("User not found")
        }
    }

    suspend fun removeListingAdmin(listingId: Int) = ListingService.adminDelete(listingId)
}
