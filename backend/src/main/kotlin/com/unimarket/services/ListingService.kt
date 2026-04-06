package com.unimarket.services

import com.unimarket.database.Listings
import com.unimarket.database.Users
import com.unimarket.database.dbQuery
import com.unimarket.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object ListingService {

    private val DAY_MS = 86_400_000L

    private fun toResponse(row: ResultRow, sellerName: String) = ListingResponse(
        id            = row[Listings.id].value,
        sellerId      = row[Listings.sellerId],
        sellerName    = sellerName,
        title         = row[Listings.title],
        description   = row[Listings.description],
        price         = row[Listings.price],
        category      = row[Listings.category],
        condition     = row[Listings.condition],
        sellerContact = row[Listings.sellerContact],
        imageUrl      = row[Listings.imageUrl],
        isActive      = row[Listings.isActive],
        expiresAt     = row[Listings.expiresAt],
        createdAt     = row[Listings.createdAt]
    )

    suspend fun getAll(keyword: String? = null, category: String? = null): List<ListingResponse> = dbQuery {
        val now = System.currentTimeMillis()
        var query = Listings
            .join(Users, JoinType.INNER, Listings.sellerId, Users.id)
            .selectAll()
            .where { (Listings.isActive eq true) and (Listings.expiresAt greater now) }

        keyword?.let {
            query = query.andWhere {
                (Listings.title like "%$it%") or (Listings.description like "%$it%")
            }
        }
        category?.let { query = query.andWhere { Listings.category eq it } }

        query.orderBy(Listings.createdAt, SortOrder.DESC).map { row ->
            toResponse(row, "${row[Users.firstName]} ${row[Users.lastName]}")
        }
    }

    suspend fun getById(id: Int): ListingResponse? = dbQuery {
        Listings
            .join(Users, JoinType.INNER, Listings.sellerId, Users.id)
            .selectAll()
            .where { Listings.id eq id }
            .firstOrNull()
            ?.let { row -> toResponse(row, "${row[Users.firstName]} ${row[Users.lastName]}") }
    }

    suspend fun getBySeller(sellerId: Int): List<ListingResponse> = dbQuery {
        val name = Users.selectAll().where { Users.id eq sellerId }
            .firstOrNull()?.let { "${it[Users.firstName]} ${it[Users.lastName]}" } ?: "Unknown"
        Listings.selectAll()
            .where { Listings.sellerId eq sellerId }
            .orderBy(Listings.createdAt, SortOrder.DESC)
            .map { toResponse(it, name) }
    }

    suspend fun create(sellerId: Int, req: CreateListingRequest): ListingResponse {
        val now   = System.currentTimeMillis()
        val newId = dbQuery {
            Listings.insertAndGetId {
                it[Listings.sellerId]      = sellerId
                it[Listings.title]         = req.title
                it[Listings.description]   = req.description
                it[Listings.price]         = req.price
                it[Listings.category]      = req.category
                it[Listings.condition]     = req.condition
                it[Listings.sellerContact] = req.sellerContact
                it[Listings.imageUrl]      = req.imageUrl
                it[Listings.isActive]      = true
                it[Listings.expiresAt]     = now + (req.expiryDays * DAY_MS)
                it[Listings.createdAt]     = now
            }.value
        }
        return getById(newId)!!
    }

    suspend fun update(id: Int, sellerId: Int, req: UpdateListingRequest): ListingResponse {
        val listing = getById(id) ?: throw NoSuchElementException("Listing not found")
        if (listing.sellerId != sellerId) throw IllegalAccessException("Not your listing")
        val now = System.currentTimeMillis()
        dbQuery {
            Listings.update({ Listings.id eq id }) {
                req.title?.let         { v -> it[title]         = v }
                req.description?.let   { v -> it[description]   = v }
                req.price?.let         { v -> it[price]         = v }
                req.category?.let      { v -> it[category]      = v }
                req.condition?.let     { v -> it[condition]     = v }
                req.sellerContact?.let { v -> it[sellerContact] = v }
                req.imageUrl?.let      { v -> it[imageUrl]      = v }
                req.expiryDays?.let    { v -> it[expiresAt]     = now + (v * DAY_MS) }
            }
        }
        return getById(id)!!
    }

    suspend fun delete(id: Int, sellerId: Int) {
        val listing = getById(id) ?: throw NoSuchElementException("Listing not found")
        if (listing.sellerId != sellerId) throw IllegalAccessException("Not your listing")
        dbQuery { Listings.update({ Listings.id eq id }) { it[isActive] = false } }
    }

    suspend fun adminDelete(id: Int) {
        dbQuery { Listings.update({ Listings.id eq id }) { it[isActive] = false } }
    }

    /** Called after purchase — deactivates listing so it disappears */
    suspend fun markSold(id: Int) {
        dbQuery { Listings.update({ Listings.id eq id }) { it[isActive] = false } }
    }
}
