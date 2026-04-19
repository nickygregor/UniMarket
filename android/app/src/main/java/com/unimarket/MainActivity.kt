package com.unimarket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.unimarket.data.local.TokenManager
import com.unimarket.data.local.UniMarketDatabase
import com.unimarket.data.remote.RetrofitClient
import com.unimarket.data.remote.UniMarketApi
import com.unimarket.data.repository.AdminRepository
import com.unimarket.data.repository.AuthRepository
import com.unimarket.data.repository.CartRepository
import com.unimarket.data.repository.InteractionRepository
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
import com.unimarket.presentation.buyer.BuyerChatScreen
import com.unimarket.presentation.buyer.BuyerMessagesScreen
import com.unimarket.presentation.buyer.ListingDetailScreen
import com.unimarket.presentation.buyer.OrderConfirmationScreen
import com.unimarket.presentation.buyer.OrderHistoryScreen
import com.unimarket.presentation.common.ProfileScreen
import com.unimarket.presentation.seller.CreateEditListingScreen
import com.unimarket.presentation.seller.SellerCommentsScreen
import com.unimarket.presentation.seller.SellerMessagesScreen
import com.unimarket.presentation.seller.SellerDashboardScreen
import com.unimarket.presentation.seller.SellerChatScreen
import com.unimarket.presentation.theme.UniMarketTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"

    const val BUYER_BROWSE = "buyer_browse"
    const val BUYER_DETAIL = "buyer_detail"
    const val BUYER_CART = "buyer_cart"
    const val BUYER_CONFIRMATION = "buyer_confirmation"
    const val BUYER_ORDERS = "buyer_orders"
    const val BUYER_MESSAGES = "buyer_messages"
    const val BUYER_CHAT = "buyer_chat"

    const val SELLER_DASHBOARD = "seller_dashboard"
    const val SELLER_CREATE = "seller_create"
    const val SELLER_EDIT = "seller_edit"
    const val SELLER_COMMENTS = "seller_comments"
    const val SELLER_MESSAGES = "seller_messages"
    const val SELLER_CHAT = "seller_chat"

    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val PROFILE = "profile"
}

data class ChatTarget(
    val listingId: Int,
    val otherUserId: Int,
    val title: String,
    val otherUserName: String
)

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
                "BUYER_SELLER" -> Routes.BUYER_BROWSE
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
    api: UniMarketApi,
    db: UniMarketDatabase,
    startDest: String
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val authRepo = remember { AuthRepository(api, tokenManager) }
    val listingRepo = remember { ListingRepository(api, db.listingDao(), tokenManager) }
    val cartRepo = remember { CartRepository(api, tokenManager) }
    val orderRepo = remember { OrderRepository(api, tokenManager) }
    val adminRepo = remember { AdminRepository(api, tokenManager) }
    val interactionRepo = remember { InteractionRepository(api, tokenManager) }

    val authVM = viewModel<AuthViewModel>(
        factory = AuthViewModelFactory(authRepo, tokenManager)
    )
    val buyerVM = viewModel<BuyerViewModel>(
        factory = BuyerViewModelFactory(listingRepo, cartRepo, orderRepo, interactionRepo)
    )
    val sellerVM = viewModel<SellerViewModel>(
        factory = SellerViewModelFactory(listingRepo, interactionRepo)
    )
    val adminVM = viewModel<AdminViewModel>(
        factory = AdminViewModelFactory(adminRepo)
    )

    val currentUser by authVM.currentUser.collectAsState()
    val darkModeEnabled by tokenManager.darkMode.collectAsState(initial = false)
    var selectedListing by remember { mutableStateOf<Listing?>(null) }
    var editingListing by remember { mutableStateOf<Listing?>(null) }
    var buyerChatTarget by remember { mutableStateOf<ChatTarget?>(null) }
    var sellerChatTarget by remember { mutableStateOf<ChatTarget?>(null) }

    fun logout() {
        authVM.logout()
        selectedListing = null
        editingListing = null
        buyerChatTarget = null
        sellerChatTarget = null
        navController.navigate(Routes.LOGIN) {
            popUpTo(id = 0) { inclusive = true }
        }
    }

    fun routeByRole(role: String) {
        val dest = when (role) {
            "BUYER", "BUYER_SELLER" -> Routes.BUYER_BROWSE
            "SELLER" -> Routes.SELLER_DASHBOARD
            "ADMIN" -> Routes.ADMIN_DASHBOARD
            else -> Routes.LOGIN
        }

        navController.navigate(dest) {
            popUpTo(Routes.LOGIN) { inclusive = true }
        }
    }

    UniMarketTheme(darkTheme = darkModeEnabled) {
        NavHost(
            navController = navController,
            startDestination = startDest
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    viewModel = authVM,
                    onSuccess = {
                        routeByRole(currentUser?.role ?: "BUYER_SELLER")
                    },
                    onRegister = {
                        navController.navigate(Routes.REGISTER)
                    }
                )
            }

            composable(Routes.REGISTER) {
                RegisterScreen(
                    viewModel = authVM,
                    onSuccess = {
                        routeByRole(currentUser?.role ?: "BUYER_SELLER")
                    },
                    onLogin = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Routes.BUYER_BROWSE) {
                BrowseScreen(
                    viewModel = buyerVM,
                    currentUserId = currentUser?.id,
                    onViewListing = { listing ->
                        selectedListing = listing
                        navController.navigate(Routes.BUYER_DETAIL)
                    },
                    onCartClick = {
                        navController.navigate(Routes.BUYER_CART)
                    },
                    onProfileClick = {
                        navController.navigate(Routes.PROFILE)
                    },
                    onLogout = {
                        logout()
                    },
                    onOrdersClick = {
                        navController.navigate(Routes.BUYER_ORDERS)
                    },
                    onMessagesClick = {
                        navController.navigate(Routes.BUYER_MESSAGES)
                    },
                    onSellClick = {
                        navController.navigate(Routes.SELLER_CREATE)
                    },
                    onMyListingsClick = {
                        navController.navigate(Routes.SELLER_DASHBOARD)
                    }
                )
            }

        composable(Routes.BUYER_DETAIL) {
            selectedListing?.let { listing ->
                ListingDetailScreen(
                    listing = listing,
                    currentUserId = currentUser?.id,
                    viewModel = buyerVM,
                    onBack = { navController.popBackStack() },
                    onCartOpen = { navController.navigate(Routes.BUYER_CART) },
                    onMessageSeller = { targetListing ->
                        buyerChatTarget = ChatTarget(
                            listingId = targetListing.id,
                            otherUserId = targetListing.sellerId,
                            title = targetListing.title,
                            otherUserName = targetListing.sellerName
                        )
                        navController.navigate(Routes.BUYER_CHAT)
                    }
                )
            }
        }

        composable(Routes.BUYER_CHAT) {
            buyerChatTarget?.let { target ->
                BuyerChatScreen(
                    viewModel = buyerVM,
                    currentUserId = currentUser?.id,
                    target = target,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Routes.BUYER_MESSAGES) {
            BuyerMessagesScreen(
                viewModel = buyerVM,
                onBack = { navController.popBackStack() },
                onOpenChat = { conversation ->
                    buyerChatTarget = ChatTarget(
                        listingId = conversation.listingId,
                        otherUserId = conversation.otherUserId,
                        title = conversation.listingTitle,
                        otherUserName = conversation.otherUserName
                    )
                    navController.navigate(Routes.BUYER_CHAT)
                }
            )
        }

        composable(Routes.BUYER_CART) {
            CartScreen(
                viewModel = buyerVM,
                currentUserId = currentUser?.id,
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
                onAddListing = {
                    navController.navigate(Routes.SELLER_CREATE)
                },
                onEdit = { listing ->
                    editingListing = listing
                    navController.navigate(Routes.SELLER_EDIT)
                },
                onProfileClick = {
                    navController.navigate(Routes.PROFILE)
                },
                onLogout = {
                    logout()
                },
                onMessagesClick = {
                    navController.navigate(Routes.SELLER_MESSAGES)
                },
                onCommentsClick = {
                    navController.navigate(Routes.SELLER_COMMENTS)
                }
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

        composable(Routes.SELLER_MESSAGES) {
            SellerMessagesScreen(
                viewModel = sellerVM,
                onBack = { navController.popBackStack() },
                onOpenChat = { conversation ->
                    sellerChatTarget = ChatTarget(
                        listingId = conversation.listingId,
                        otherUserId = conversation.otherUserId,
                        title = conversation.listingTitle,
                        otherUserName = conversation.otherUserName
                    )
                    navController.navigate(Routes.SELLER_CHAT)
                }
            )
        }

        composable(Routes.SELLER_COMMENTS) {
            SellerCommentsScreen(
                viewModel = sellerVM,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SELLER_CHAT) {
            sellerChatTarget?.let { target ->
                SellerChatScreen(
                    viewModel = sellerVM,
                    currentUserId = currentUser?.id,
                    target = target,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Routes.ADMIN_DASHBOARD) {
            AdminDashboardScreen(
                viewModel = adminVM,
                onProfileClick = {
                    navController.navigate(Routes.PROFILE)
                },
                onLogout = { logout() }
            )
        }

            composable(Routes.PROFILE) {
                currentUser?.let { user ->
                    val profileImage by tokenManager.profileImage(user.id).collectAsState(initial = null)
                    val passwordState by authVM.passwordState.collectAsState()
                    val sellerNotificationsState by sellerVM.notifications.collectAsState()
                    val sellerNotifications = (sellerNotificationsState as? com.unimarket.presentation.UiState.Success)?.data

                    LaunchedEffect(user.id, user.role) {
                        if (user.role in listOf("SELLER", "BUYER_SELLER")) {
                            sellerVM.loadNotifications()
                        }
                    }

                    ProfileScreen(
                        user = user,
                        darkModeEnabled = darkModeEnabled,
                        profileImage = profileImage,
                        passwordState = passwordState,
                        sellerCommentCount = sellerNotifications?.commentCount ?: 0,
                        sellerUnreadMessageCount = sellerNotifications?.unreadMessageCount ?: 0,
                        onSellerCommentsClick = {
                            navController.navigate(Routes.SELLER_COMMENTS)
                        },
                        onSellerMessagesClick = {
                            navController.navigate(Routes.SELLER_MESSAGES)
                        },
                        onDarkModeChange = { enabled ->
                            scope.launch { tokenManager.saveDarkMode(enabled) }
                        },
                        onProfileImageChange = { imageData ->
                            scope.launch { tokenManager.saveProfileImage(user.id, imageData) }
                        },
                        onProfileImageRemove = {
                            scope.launch { tokenManager.clearProfileImage(user.id) }
                        },
                        onChangePassword = { currentPassword, newPassword ->
                            authVM.changePassword(currentPassword, newPassword)
                        },
                        onResetPasswordState = {
                            authVM.resetPasswordState()
                        },
                        onLogout = { logout() },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
