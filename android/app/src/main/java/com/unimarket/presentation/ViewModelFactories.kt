package com.unimarket.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.unimarket.data.local.TokenManager
import com.unimarket.data.repository.*

// ── AuthViewModel Factory ─────────────────────────────────────────────────────
class AuthViewModelFactory(
    private val repo         : AuthRepository,
    private val tokenManager : TokenManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AuthViewModel(repo, tokenManager) as T
}

// ── BuyerViewModel Factory ────────────────────────────────────────────────────
class BuyerViewModelFactory(
    private val listingRepo : ListingRepository,
    private val cartRepo    : CartRepository,
    private val orderRepo   : OrderRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        BuyerViewModel(listingRepo, cartRepo, orderRepo) as T
}

// ── SellerViewModel Factory ───────────────────────────────────────────────────
class SellerViewModelFactory(
    private val repo: ListingRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SellerViewModel(repo) as T
}

// ── AdminViewModel Factory ────────────────────────────────────────────────────
class AdminViewModelFactory(
    private val repo: AdminRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AdminViewModel(repo) as T
}
