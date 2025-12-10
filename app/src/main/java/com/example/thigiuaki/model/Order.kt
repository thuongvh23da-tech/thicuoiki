package com.example.thigiuaki.model

import com.google.firebase.Timestamp

data class Order(
    var id: String = "",
    var userId: String = "",
    var items: List<CartItem> = emptyList(),
    var subtotal: Double = 0.0,
    var shippingCost: Double = 0.0,
    var tax: Double = 0.0,
    var discount: Double = 0.0,
    var couponCode: String = "",
    var totalAmount: Double = 0.0,
    var shippingAddress: Address = Address(),
    var deliveryType: String = "home_delivery", // home_delivery, store_pickup
    var pickupTime: Timestamp? = null,
    var orderNotes: String = "",
    var status: String = "pending", // pending, confirmed, packed, ready_to_ship, shipped, out_for_delivery, delivered, cancelled
    var isProcessed: Boolean = false,
    var paymentMethod: String = "cash", // cash, card, online, cod
    var paymentStatus: String = "pending", // pending, paid, failed, refunded
    var trackingNumber: String = "",
    var createdAt: Timestamp? = null,
    var confirmedAt: Timestamp? = null,
    var packedAt: Timestamp? = null,
    var shippedAt: Timestamp? = null,
    var deliveredAt: Timestamp? = null,
    var cancelledAt: Timestamp? = null,
    var orderDate: String = "",
    var canCancel: Boolean = true, // Can customer cancel?
    var canReturn: Boolean = false // Can customer return? (after delivery)
)
