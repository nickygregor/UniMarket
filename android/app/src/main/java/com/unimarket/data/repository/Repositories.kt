package com.unimarket.data.repository

import com.unimarket.data.local.ListingDao
import com.unimarket.data.local.ListingEntity
import com.unimarket.data.local.TokenManager
import com.unimarket.data.remote.UniMarketApi
import com.unimarket.domain.model.*
import kotlinx.coroutines.flow.first

/** Sealed wrapper so every use-case gets typed success/error */
sealed class Result<out T> {
    data class Success<T>(val data: T)  : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}

// ── Auth Repository ───────────────────────────────────────────────────────────

class AuthRepository(
    private val api          : UniMarketApi,
    private val tokenManager : TokenManager
) {
    suspend fun register(req: RegisterRequest): Result<AuthResponse> =
        safeCall { api.register(req) }

    suspend fun login(req: LoginRequest): Result<AuthResponse> =
        safeCall { api.login(req) }

    suspend fun logout() = tokenManager.clearSession()

    suspend fun cachedUser(): User? = tokenManager.user.first()
    suspend fun bearerToken(): String? = tokenManager.token.first()?.let { "Bearer $it" }
}

// ── Listing Repository ────────────────────────────────────────────────────────

class ListingRepository(
    private val api        : UniMarketApi,
    private val dao        : ListingDao,
    private val tokenMgr   : TokenManager
) {
    /** Network-first, fallback to Room cache */
    suspend fun getListings(keyword: String? = null, category: String? = null): Result<List<Listing>> {
        return try {
            val resp = api.getListings(keyword, category)
            if (resp.isSuccessful) {
                val listings = resp.body()!!
                // refresh cache only when no filters applied
                if (keyword == null && category == null) {
                    dao.clearAll()
                    dao.upsertAll(listings.map { it.toEntity() })
                }
                Result.Success(listings)
            } else {
                Result.Error(resp.errorBody()?.string() ?: "Failed to load listings")
            }
        } catch (e: Exception) {
            // offline fallback
            val cached = when {
                keyword  != null -> dao.search(keyword)
                category != null -> dao.byCategory(category)
                else             -> dao.getAll()
            }
            if (cached.isNotEmpty()) Result.Success(cached.map { it.toListing() })
            else Result.Error("No internet and no cached data")
        }
    }

    suspend fun getById(id: Int): Result<Listing> = safeCall { api.getListingById(id) }

    suspend fun getMyListings(): Result<List<Listing>> {
        val token = tokenMgr.token.first()?.let { "Bearer $it" }
            ?: return Result.Error("Not authenticated")
        return safeCall { api.getMyListings(token) }
    }

    suspend fun create(req: CreateListingRequest): Result<Listing> {
        val token = tokenMgr.token.first()?.let { "Bearer $it" } ?: return Result.Error("Not authenticated")
        return safeCall { api.createListing(token, req) }
    }

    suspend fun update(id: Int, req: UpdateListingRequest): Result<Listing> {
        val token = tokenMgr.token.first()?.let { "Bearer $it" } ?: return Result.Error("Not authenticated")
        return safeCall { api.updateListing(token, id, req) }
    }

    suspend fun delete(id: Int): Result<MessageResponse> {
        val token = tokenMgr.token.first()?.let { "Bearer $it" } ?: return Result.Error("Not authenticated")
        return safeCall { api.deleteListing(token, id) }
    }
}

// ── Cart Repository ───────────────────────────────────────────────────────────

class CartRepository(
    private val api      : UniMarketApi,
    private val tokenMgr : TokenManager
) {
    private suspend fun token() = tokenMgr.token.first()?.let { "Bearer $it" }

    suspend fun getCart(): Result<Cart> {
        val t = token() ?: return Result.Error("Not authenticated")
        return safeCall { api.getCart(t) }
    }

    suspend fun addItem(req: AddToCartRequest): Result<MessageResponse> {
        val t = token() ?: return Result.Error("Not authenticated")
        return safeCall { api.addToCart(t, req) }
    }

    suspend fun removeItem(cartItemId: Int): Result<MessageResponse> {
        val t = token() ?: return Result.Error("Not authenticated")
        return safeCall { api.removeFromCart(t, cartItemId) }
    }
}

// ── Order Repository ──────────────────────────────────────────────────────────

class OrderRepository(
    private val api      : UniMarketApi,
    private val tokenMgr : TokenManager
) {
    private suspend fun token() = tokenMgr.token.first()?.let { "Bearer $it" }

    suspend fun checkout(): Result<Order> {
        val t = token() ?: return Result.Error("Not authenticated")
        return safeCall { api.checkout(t) }
    }

    suspend fun getOrders(): Result<List<Order>> {
        val t = token() ?: return Result.Error("Not authenticated")
        return safeCall { api.getOrders(t) }
    }
}

// ── Admin Repository ──────────────────────────────────────────────────────────

class AdminRepository(
    private val api      : UniMarketApi,
    private val tokenMgr : TokenManager
) {
    private suspend fun token() = tokenMgr.token.first()?.let { "Bearer $it" }

    suspend fun getAllUsers(): Result<List<User>> {
        val t = token() ?: return Result.Error("Not authenticated")
        return safeCall { api.getAllUsers(t) }
    }

    suspend fun activateUser(id: Int): Result<MessageResponse> {
        val t = token() ?: return Result.Error("Not authenticated")
        return safeCall { api.activateUser(t, id) }
    }

    suspend fun deactivateUser(id: Int): Result<MessageResponse> {
        val t = token() ?: return Result.Error("Not authenticated")
        return safeCall { api.deactivateUser(t, id) }
    }

    suspend fun deleteListing(id: Int): Result<MessageResponse> {
        val t = token() ?: return Result.Error("Not authenticated")
        return safeCall { api.adminDeleteListing(t, id) }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private suspend fun <T> safeCall(call: suspend () -> retrofit2.Response<T>): Result<T> =
    try {
        val resp = call()
        if (resp.isSuccessful) Result.Success(resp.body()!!)
        else Result.Error(resp.errorBody()?.string() ?: "Unknown error (${resp.code()})")
    } catch (e: Exception) {
        Result.Error(e.localizedMessage ?: "Network error")
    }

private fun Listing.toEntity() = ListingEntity(id, sellerId, sellerName, title, description, price, category, imageUrl, isActive, createdAt)
private fun ListingEntity.toListing() = Listing(id, sellerId, sellerName, title, description, price, category, imageUrl, isActive, createdAt)
