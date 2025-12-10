package com.example.thigiuaki

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.example.thigiuaki.ui.theme.ThigiuakiTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // FIX LỖI UI XML
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Khởi tạo Firebase
        FirebaseApp.initializeApp(this)

        setContent {
            ThigiuakiApp()
        }
    }
}

// Định nghĩa các trạng thái/màn hình điều hướng
sealed class Screen {
    object RoleSelection : Screen()
    object AdminLogin : Screen()
    object CustomerLogin : Screen()
    object Register : Screen()
    object AdminManager : Screen()
    object CustomerHome : Screen()
    data class ProductDetails(val productId: String) : Screen()
    object Cart : Screen()
    object Checkout : Screen()
    object OrderHistory : Screen()
    object Wishlist : Screen()
    data class Message(val orderId: String, val otherUserId: String, val otherUserName: String) : Screen()
}

@Composable
fun ThigiuakiApp() {
    val auth = FirebaseAuth.getInstance()
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

    ThigiuakiTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            var currentScreen by remember { mutableStateOf<Screen>(Screen.RoleSelection) }
            var cartItems by remember { mutableStateOf(listOf<com.example.thigiuaki.model.CartItem>()) }

            // Hàm xử lý đăng xuất chung
            val handleLogout: () -> Unit = {
                auth.signOut()
                currentScreen = Screen.RoleSelection
            }

            // Load cart items for checkout
            LaunchedEffect(auth.currentUser?.uid) {
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    db.collection("cart")
                        .whereEqualTo("userId", userId)
                        .addSnapshotListener { snapshot, _ ->
                            val items: List<com.example.thigiuaki.model.CartItem> = snapshot?.documents?.mapNotNull { doc ->
                                try {
                                    val item = doc.toObject(com.example.thigiuaki.model.CartItem::class.java)
                                    item?.apply {
                                        id = doc.id
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            } ?: emptyList()
                            cartItems = items
                        }
                }
            }

            when (currentScreen) {
                is Screen.RoleSelection -> RoleSelectionScreen(
                    onAdminSelected = { currentScreen = Screen.AdminLogin },
                    onCustomerSelected = { currentScreen = Screen.CustomerLogin }
                )

                is Screen.AdminLogin -> LoginScreen(
                    onLoginSuccess = { role ->
                        currentScreen = if (role == "admin") {
                            Screen.AdminManager
                        } else {
                            Screen.CustomerHome
                        }
                    },
                    onBack = { currentScreen = Screen.RoleSelection }
                )

                is Screen.CustomerLogin -> LoginScreen(
                    onLoginSuccess = { role ->
                        currentScreen = Screen.CustomerHome
                    },
                    onBack = { currentScreen = Screen.RoleSelection },
                    onRegisterClick = { currentScreen = Screen.Register }
                )

                is Screen.Register -> RegisterScreen(
                    onRegisterSuccess = { currentScreen = Screen.CustomerHome },
                    onBack = { currentScreen = Screen.CustomerLogin }
                )

                is Screen.AdminManager -> AdminScreen(
                    onLogout = handleLogout
                )

                is Screen.CustomerHome -> CustomerHomeScreen(
                    onLogout = handleLogout,
                    onNavigateToProductDetails = { productId ->
                        currentScreen = Screen.ProductDetails(productId)
                    },
                    onNavigateToCheckout = {
                        if (cartItems.isNotEmpty()) {
                            currentScreen = Screen.Checkout
                        }
                    },
                    onNavigateToWishlist = {
                        currentScreen = Screen.Wishlist
                    },
                    onNavigateToMessage = { orderId, otherUserId, otherUserName ->
                        currentScreen = Screen.Message(orderId, otherUserId, otherUserName)
                    }
                )

                is Screen.ProductDetails -> {
                    val productId = (currentScreen as Screen.ProductDetails).productId
                    ProductDetailsScreen(
                        productId = productId,
                        onBack = { currentScreen = Screen.CustomerHome },
                        onAddToCart = { product, size, color ->
                            val userId = auth.currentUser?.uid ?: return@ProductDetailsScreen
                            val cartItem = com.example.thigiuaki.model.CartItem(
                                productId = product.id,
                                productName = product.name,
                                productImageUrl = product.imageUrl,
                                price = product.price,
                                quantity = 1,
                                selectedSize = size,
                                selectedColor = color,
                                userId = userId
                            )
                            db.collection("cart").add(cartItem)
                            currentScreen = Screen.CustomerHome
                        }
                    )
                }

                is Screen.Cart -> CartScreen(
                    onCheckout = {
                        if (cartItems.isNotEmpty()) {
                            currentScreen = Screen.Checkout
                        }
                    },
                    onNavigateToProductDetails = { productId ->
                        currentScreen = Screen.ProductDetails(productId)
                    }
                )

                is Screen.Checkout -> CheckoutScreen(
                    cartItems = cartItems,
                    onBack = { currentScreen = Screen.Cart },
                    onOrderPlaced = {
                        currentScreen = Screen.CustomerHome
                    }
                )

                is Screen.OrderHistory -> OrderHistoryScreen(
                    onNavigateToMessage = { orderId, otherUserId, otherUserName ->
                        currentScreen = Screen.Message(orderId, otherUserId, otherUserName)
                    }
                )
                
                is Screen.Message -> {
                    val messageScreen = currentScreen as Screen.Message
                    MessageScreen(
                        orderId = messageScreen.orderId,
                        otherUserId = messageScreen.otherUserId,
                        otherUserName = messageScreen.otherUserName,
                        onBack = { currentScreen = Screen.OrderHistory }
                    )
                }

                is Screen.Wishlist -> WishlistScreen(
                    onNavigateToProductDetails = { productId ->
                        currentScreen = Screen.ProductDetails(productId)
                    },
                    onBack = {
                        currentScreen = Screen.CustomerHome
                    }
                )


            }
        }
    }
}