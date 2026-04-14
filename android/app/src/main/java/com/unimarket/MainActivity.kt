package com.unimarket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.unimarket.data.local.TokenManager
import com.unimarket.data.local.UniMarketDatabase
import com.unimarket.data.remote.RetrofitClient
import com.unimarket.data.repository.AdminRepository
import com.unimarket.data.repository.AuthRepository
import com.unimarket.data.repository.CartRepository
import com.unimarket.data.repository.ListingRepository
import com.unimarket.data.repository.OrderRepository
import com.unimarket.domain.model.Listing
import com.unimarket.presentation.AdminViewModel
import com.unimarket.presentation.AdminViewModelFactory
import com.unimarket.presentation.AuthViewModel
import com.unimarket.presentation.AuthViewModelFactory
import com.unimarket.presentation.BuyerViewModel
import com.unimarket.presentation.BuyerViewModelFactory
import com.unimarket.presentation.SellerViewModel
import com.unimarket.presentation.SellerViewModelFactory
import com.unimarket.presentation.admin.AdminDashboardScreen
import com.unimarket.presentation.auth.LoginScreen
import com.unimarket.presentation.auth.RegisterScreen
import com.unimarket.presentation.buyer.BrowseScreen
import com.unimarket.presentation.buyer.CartScreen
import com.unimarket.presentation.buyer.ListingDetailScreen
import com.unimarket.presentation.buyer.OrderConfirmationScreen
import com.unimarket.presentation.buyer.OrderHistoryScreen
import com.unimarket.presentation.common.ProfileScreen
import com.unimarket.presentation.seller.CreateEditListingScreen
import com.unimarket.presentation.seller.SellerDashboardScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"

    const val BUYER_BROWSE = "buyer_browse"
    const val BUYER_CART = "buyer_cart"
    const val BUYER_DETAIL = "buyer_detail"
    const val BUYER_CONFIRMATION = "buyer_confirmation"
    const val BUYER_ORDERS = "buyer_orders"

    const val SELLER_DASHBOARD = "seller_dashboard"
    const val SELLER_CREATE = "seller_create"
    const val SELLER_EDIT = "seller_edit"

    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val PROFILE = "profile"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tokenManager = TokenManager(this)
        val api = RetrofitClient.api
        val db = UniMarketDatabase.getInstance(this)

        val startDest = runBlocking {
            when (tokenManager.user.first()?.role) {
                "BUYER" -> Routes.BUYER_BROWSE
                "SELLER" -> Routes.SELLER_DASHBOARD
                "ADMIN" -> Routes.ADMIN_DASHBOARD
                else -> Routes.LOGIN
            }
        }

        setContent {
            UniMarketApp(
                tokenManager = tokenManager,
                api = api,
                db = db,
                startDest = startDest
            )
        }
    }
}

@Composable
fun UniMarketApp(
    tokenManager: TokenManager,
    api: com.unimarket.data.remote.UniMarketApi,
    db: UniMarketDatabase,
    startDest: String
) {
    val navController = rememberNavController()

    val authRepo = remember { AuthRepository(api, tokenManager) }
    val listingRepo = remember { ListingRepository(api, db.listingDao(), tokenManager) }
    val cartRepo = remember { CartRepository(api, tokenManager) }
    val orderRepo = remember { OrderRepository(api, tokenManager) }
    val adminRepo = remember { AdminRepository(api, tokenManager) }

    val authVM = viewModel<AuthViewModel>(
        factory = AuthViewModelFactory(authRepo, tokenManager)
    )
    val buyerVM = viewModel<BuyerViewModel>(
        factory = BuyerViewModelFactory(listingRepo, cartRepo, orderRepo)
    )
    val sellerVM = viewModel<SellerViewModel>(
        factory = SellerViewModelFactory(listingRepo)
    )
    val adminVM = viewModel<AdminViewModel>(
        factory = AdminViewModelFactory(adminRepo)
    )

    val currentUser by authVM.currentUser.collectAsState()

    var selectedListing by remember { mutableStateOf<Listing?>(null) }
    var editingListing by remember { mutableStateOf<Listing?>(null) }

    fun logout() {
        authVM.logout()
        navController.navigate(Routes.LOGIN) {
            popUpTo(0) { inclusive = true }
        }
    }

    fun routeByRole(role: String) {
        val dest = when (role) {
            "BUYER" -> Routes.BUYER_BROWSE
            "SELLER" -> Routes.SELLER_DASHBOARD
            "ADMIN" -> Routes.ADMIN_DASHBOARD
            else -> Routes.LOGIN
        }
        navController.navigate(dest) {
            popUpTo(Routes.LOGIN) { inclusive = true }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDest
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel = authVM,
                onSuccess = { routeByRole("BUYER") },
                onRegister = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                viewModel = authVM,
                onSuccess = { routeByRole("BUYER") },
                onLogin = { navController.popBackStack() }
            )
        }

        composable(Routes.BUYER_BROWSE) {
            BrowseScreen(
                viewModel = buyerVM,
                onViewListing = { listing ->
                    selectedListing = listing
                    navController.navigate(Routes.BUYER_DETAIL)
                },
                onCartClick = { navController.navigate(Routes.BUYER_CART) },
                onLogout = { logout() },
                onOrdersClick = { navController.navigate(Routes.BUYER_ORDERS) },
                onProfileClick = { navController.navigate(Routes.PROFILE) },
                onMyListingsClick = { navController.navigate(Routes.SELLER_DASHBOARD) },
                onSellClick = { navController.navigate(Routes.SELLER_CREATE) }
            )
        }

        composable(Routes.BUYER_DETAIL) {
            selectedListing?.let { listing ->
                ListingDetailScreen(
                    listing = listing,
                    viewModel = buyerVM,
                    onBack = { navController.popBackStack() },
                    onCartOpen = { navController.navigate(Routes.BUYER_CART) }
                )
            }
        }

        composable(Routes.BUYER_CART) {
            CartScreen(
                viewModel = buyerVM,
                onBack = { navController.popBackStack() },
                onCheckout = { navController.navigate(Routes.BUYER_CONFIRMATION) }
            )
        }

        composable(Routes.BUYER_CONFIRMATION) {
            OrderConfirmationScreen(
                viewModel = buyerVM,
                onDone = {
                    navController.navigate(Routes.BUYER_BROWSE) {
                        popUpTo(Routes.BUYER_BROWSE) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.BUYER_ORDERS) {
            OrderHistoryScreen(
                viewModel = buyerVM,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SELLER_DASHBOARD) {
            SellerDashboardScreen(
                viewModel = sellerVM,
                onAddListing = { navController.navigate(Routes.SELLER_CREATE) },
                onEdit = { listing ->
                    editingListing = listing
                    navController.navigate(Routes.SELLER_EDIT)
                },
                onLogout = { logout() },
                onProfileClick = { navController.navigate(Routes.PROFILE) }
            )
        }

        composable(Routes.SELLER_CREATE) {
            CreateEditListingScreen(
                viewModel = sellerVM,
                existingListing = null,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SELLER_EDIT) {
            CreateEditListingScreen(
                viewModel = sellerVM,
                existingListing = editingListing,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ADMIN_DASHBOARD) {
            AdminDashboardScreen(
                viewModel = adminVM,
                onLogout = { logout() }
            )
        }

        composable(Routes.PROFILE) {
            currentUser?.let { user ->
                ProfileScreen(
                    user = user,
                    onLogout = { logout() },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}