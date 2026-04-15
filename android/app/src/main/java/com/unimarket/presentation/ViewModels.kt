package com.unimarket.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unimarket.data.local.TokenManager
import com.unimarket.data.repository.AdminRepository
import com.unimarket.data.repository.AuthRepository
import com.unimarket.data.repository.CartRepository
import com.unimarket.data.repository.ListingRepository
import com.unimarket.data.repository.OrderRepository
import com.unimarket.data.repository.Result
import com.unimarket.domain.model.AddToCartRequest
import com.unimarket.domain.model.AuthResponse
import com.unimarket.domain.model.CheckoutRequest
import com.unimarket.domain.model.CreateListingRequest
import com.unimarket.domain.model.Listing
import com.unimarket.domain.model.LoginRequest
import com.unimarket.domain.model.Order
import com.unimarket.domain.model.RegisterRequest
import com.unimarket.domain.model.UpdateListingRequest
import com.unimarket.domain.model.User
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val msg: String) : UiState<Nothing>()
}

class AuthViewModel(
    private val repo: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _authState = MutableStateFlow<UiState<AuthResponse>>(UiState.Idle)
    val authState: StateFlow<UiState<AuthResponse>> = _authState.asStateFlow()

    val currentUser: StateFlow<User?> = tokenManager.user
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun login(userId: String, password: String) = viewModelScope.launch {
        _authState.value = UiState.Loading
        when (val r = repo.login(LoginRequest(userId, password))) {
            is Result.Success -> {
                tokenManager.saveSession(r.data.token, r.data.user)
                _authState.value = UiState.Success(r.data)
            }
            is Result.Error -> _authState.value = UiState.Error(r.message)
        }
    }

    fun register(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        userId: String,
        password: String
    ) = viewModelScope.launch {
        _authState.value = UiState.Loading
        val req = RegisterRequest(firstName, lastName, email, phone, userId, password)
        when (val r = repo.register(req)) {
            is Result.Success -> {
                tokenManager.saveSession(r.data.token, r.data.user)
                _authState.value = UiState.Success(r.data)
            }
            is Result.Error -> _authState.value = UiState.Error(r.message)
        }
    }

    fun logout() = viewModelScope.launch {
        repo.logout()
        _authState.value = UiState.Idle
    }

    fun resetState() {
        _authState.value = UiState.Idle
    }
}

class BuyerViewModel(
    private val listingRepo: ListingRepository,
    private val cartRepo: CartRepository,
    private val orderRepo: OrderRepository
) : ViewModel() {

    private val _listings = MutableStateFlow<UiState<List<Listing>>>(UiState.Idle)
    val listings: StateFlow<UiState<List<Listing>>> = _listings.asStateFlow()

    private val _cart = MutableStateFlow<UiState<com.unimarket.domain.model.Cart>>(UiState.Idle)
    val cart: StateFlow<UiState<com.unimarket.domain.model.Cart>> = _cart.asStateFlow()

    private val _orders = MutableStateFlow<UiState<List<Order>>>(UiState.Idle)
    val orders: StateFlow<UiState<List<Order>>> = _orders.asStateFlow()

    private val _checkoutResult = MutableStateFlow<UiState<Order>>(UiState.Idle)
    val checkoutResult: StateFlow<UiState<Order>> = _checkoutResult.asStateFlow()

    private val _toast = MutableSharedFlow<String>()
    val toast: SharedFlow<String> = _toast.asSharedFlow()

    fun loadListings(keyword: String? = null, category: String? = null) = viewModelScope.launch {
        _listings.value = UiState.Loading
        when (val r = listingRepo.getListings(keyword, category)) {
            is Result.Success -> _listings.value = UiState.Success(r.data)
            is Result.Error -> _listings.value = UiState.Error(r.message)
        }
    }

    fun loadCart() = viewModelScope.launch {
        _cart.value = UiState.Loading
        when (val r = cartRepo.getCart()) {
            is Result.Success -> _cart.value = UiState.Success(r.data)
            is Result.Error -> _cart.value = UiState.Error(r.message)
        }
    }

    fun addToCart(listingId: Int, qty: Int = 1) = viewModelScope.launch {
        when (val r = cartRepo.addItem(AddToCartRequest(listingId, qty))) {
            is Result.Success -> {
                _toast.emit("Added to cart")
                loadCart()
            }
            is Result.Error -> _toast.emit(r.message)
        }
    }

    fun removeFromCart(cartItemId: Int) = viewModelScope.launch {
        when (val r = cartRepo.removeItem(cartItemId)) {
            is Result.Success -> {
                _toast.emit("Item removed")
                loadCart()
            }
            is Result.Error -> _toast.emit(r.message)
        }
    }

    fun checkout(req: CheckoutRequest) = viewModelScope.launch {
        _checkoutResult.value = UiState.Loading
        when (val r = orderRepo.checkout(req)) {
            is Result.Success -> {
                _checkoutResult.value = UiState.Success(r.data)
                loadCart()
            }
            is Result.Error -> {
                _checkoutResult.value = UiState.Error(r.message)
                _toast.emit(r.message)
            }
        }
    }

    fun loadOrders() = viewModelScope.launch {
        _orders.value = UiState.Loading
        when (val r = orderRepo.getOrders()) {
            is Result.Success -> _orders.value = UiState.Success(r.data)
            is Result.Error -> _orders.value = UiState.Error(r.message)
        }
    }

    fun resetCheckout() {
        _checkoutResult.value = UiState.Idle
    }
}

class SellerViewModel(
    private val repo: ListingRepository
) : ViewModel() {

    private val _listings = MutableStateFlow<UiState<List<Listing>>>(UiState.Idle)
    val listings: StateFlow<UiState<List<Listing>>> = _listings.asStateFlow()

    private val _actionResult = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val actionResult: StateFlow<UiState<Unit>> = _actionResult.asStateFlow()

    private val _toast = MutableSharedFlow<String>()
    val toast: SharedFlow<String> = _toast.asSharedFlow()

    fun loadMyListings() = viewModelScope.launch {
        _listings.value = UiState.Loading
        when (val r = repo.getMyListings()) {
            is Result.Success -> _listings.value = UiState.Success(r.data)
            is Result.Error -> _listings.value = UiState.Error(r.message)
        }
    }

    fun createListing(
        title: String,
        desc: String,
        price: Double,
        cat: String,
        img: String?
    ) = viewModelScope.launch {
        _actionResult.value = UiState.Loading

        val req = CreateListingRequest(
            title = title,
            description = desc,
            price = price,
            category = cat,
            imageUrl = img,
            sellerContact = ""
        )

        when (val r = repo.create(req)) {
            is Result.Success -> {
                _toast.emit("Listing created")
                loadMyListings()
                _actionResult.value = UiState.Success(Unit)
            }
            is Result.Error -> {
                _toast.emit(r.message)
                _actionResult.value = UiState.Error(r.message)
            }
        }
    }

    fun updateListing(
        id: Int,
        title: String?,
        desc: String?,
        price: Double?,
        cat: String?
    ) = viewModelScope.launch {
        _actionResult.value = UiState.Loading

        val req = UpdateListingRequest(
            title = title,
            description = desc,
            price = price,
            category = cat
        )

        when (val r = repo.update(id, req)) {
            is Result.Success -> {
                _toast.emit("Listing updated")
                loadMyListings()
                _actionResult.value = UiState.Success(Unit)
            }
            is Result.Error -> {
                _toast.emit(r.message)
                _actionResult.value = UiState.Error(r.message)
            }
        }
    }

    fun deleteListing(id: Int) = viewModelScope.launch {
        when (val r = repo.delete(id)) {
            is Result.Success -> {
                _toast.emit("Listing deleted")
                loadMyListings()
            }
            is Result.Error -> _toast.emit(r.message)
        }
    }

    fun resetAction() {
        _actionResult.value = UiState.Idle
    }
}

class AdminViewModel(
    private val repo: AdminRepository
) : ViewModel() {

    private val _users = MutableStateFlow<UiState<List<User>>>(UiState.Idle)
    val users: StateFlow<UiState<List<User>>> = _users.asStateFlow()

    private val _toast = MutableSharedFlow<String>()
    val toast: SharedFlow<String> = _toast.asSharedFlow()

    fun loadUsers() = viewModelScope.launch {
        _users.value = UiState.Loading
        when (val r = repo.getAllUsers()) {
            is Result.Success -> _users.value = UiState.Success(r.data)
            is Result.Error -> _users.value = UiState.Error(r.message)
        }
    }

    fun toggleUser(id: Int, currentlyActive: Boolean) = viewModelScope.launch {
        val r = if (currentlyActive) repo.deactivateUser(id) else repo.activateUser(id)
        when (r) {
            is Result.Success -> {
                _toast.emit(r.data.message)
                loadUsers()
            }
            is Result.Error -> _toast.emit(r.message)
        }
    }

    fun deleteListing(id: Int) = viewModelScope.launch {
        when (val r = repo.deleteListing(id)) {
            is Result.Success -> _toast.emit("Listing removed")
            is Result.Error -> _toast.emit(r.message)
        }
    }
}
