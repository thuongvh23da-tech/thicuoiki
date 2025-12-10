package com.example.thigiuaki.model

data class CartItem(
    var id: String = "",
    var productId: String = "",
    var productName: String = "",
    var productImageUrl: String = "",
    var price: Double = 0.0,
    var quantity: Int = 1,
    var selectedSize: String = "",
    var selectedColor: String = "",
    var userId: String = "" // To track which user's cart
)
