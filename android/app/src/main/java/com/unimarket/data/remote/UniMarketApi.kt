package com.unimarket.data.remote

import com.unimarket.domain.model.*
import retrofit2.Response
import retrofit2.http.*

interface UniMarketApi {

    // AUTH
    @POST("auth/register")
    suspend fun register(
        @Body req: RegisterRequest
    ): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(
        @Body req: LoginRequest
    ): Response<AuthResponse>

    // LISTINGS - PUBLIC
    @GET("listings")
    suspend fun getListings(
        @Query("keyword") keyword: String? = null,
        @Query("category") category: String? = null
    ): Response<List<Listing>>

    @GET("listings/{id}")
    suspend fun getListingById(
        @Path("id") id: Int
    ): Response<Listing>

    // SELLER LISTINGS
    @GET("seller/listings")
    suspend fun getMyListings(
        @Header("Authorization") token: String
    ): Response<List<Listing>>

    @POST("seller/listings")
    suspend fun createListing(
        @Header("Authorization") token: String,
        @Body req: CreateListingRequest
    ): Response<Listing>

    @PUT("seller/listings/{id}")
    suspend fun updateListing(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body req: UpdateListingRequest
    ): Response<Listing>

    @DELETE("seller/listings/{id}")
    suspend fun deleteListing(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<MessageResponse>

    // CART
    @GET("buyer/cart")
    suspend fun getCart(
        @Header("Authorization") token: String
    ): Response<Cart>

    @POST("buyer/cart/add")
    suspend fun addToCart(
        @Header("Authorization") token: String,
        @Body req: AddToCartRequest
    ): Response<MessageResponse>

    @DELETE("buyer/cart/remove/{cartItemId}")
    suspend fun removeFromCart(
        @Header("Authorization") token: String,
        @Path("cartItemId") cartItemId: Int
    ): Response<MessageResponse>

    // ORDERS / CHECKOUT
    @POST("orders/checkout")
    suspend fun checkout(
        @Header("Authorization") token: String,
        @Body req: CheckoutRequest
    ): Response<Order>

    @GET("orders")
    suspend fun getOrders(
        @Header("Authorization") token: String
    ): Response<List<Order>>

    // ADMIN
    @GET("admin/users")
    suspend fun getAllUsers(
        @Header("Authorization") token: String
    ): Response<List<User>>

    @POST("admin/users/{id}/activate")
    suspend fun activateUser(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<MessageResponse>

    @POST("admin/users/{id}/deactivate")
    suspend fun deactivateUser(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<MessageResponse>

    @DELETE("admin/listings/{id}")
    suspend fun adminDeleteListing(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<MessageResponse>
}