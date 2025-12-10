package com.example.thigiuaki

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.thigiuaki.model.Order
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    onNavigateToMessage: ((String, String, String) -> Unit)? = null
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: ""
    
    var orders by remember { mutableStateOf(listOf<Order>()) }
    var isLoading by remember { mutableStateOf(true) }
    var adminUserId by remember { mutableStateOf("") }
    var adminUserName by remember { mutableStateOf("Admin") }
    
    // Get admin user info
    LaunchedEffect(Unit) {
        db.collection("users")
            .whereEqualTo("role", "admin")
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val adminDoc = snapshot.documents.first()
                    adminUserId = adminDoc.id
                    adminUserName = adminDoc.getString("name") ?: "Admin"
                }
            }
    }

    LaunchedEffect(userId) {
        if (userId.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }
        
        db.collection("orders")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("OrderHistory", "Error: ${error.message}")
                    isLoading = false
                    return@addSnapshotListener
                }
                
                val orderList = snapshot?.documents?.mapNotNull { doc ->
                    val order = doc.toObject<Order>()
                    order?.copy(id = doc.id)
                } ?: emptyList()
                
                orders = orderList
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lịch sử đơn hàng") }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (orders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Chưa có đơn hàng nào",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Đặt hàng ngay để xem lịch sử",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(orders, key = { it.id }) { order ->
                    OrderCard(
                        order = order,
                        onMessageClick = {
                            if (onNavigateToMessage != null && adminUserId.isNotBlank()) {
                                onNavigateToMessage(order.id, adminUserId, adminUserName)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun OrderCard(
    order: Order,
    onMessageClick: (() -> Unit)? = null
) {
    val statusColor = when (order.status) {
        "pending" -> MaterialTheme.colorScheme.tertiary
        "confirmed" -> MaterialTheme.colorScheme.primary
        "shipped" -> MaterialTheme.colorScheme.secondary
        "delivered" -> MaterialTheme.colorScheme.primaryContainer
        "cancelled" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Đơn hàng #${order.id.take(8)}",
                    style = MaterialTheme.typography.titleMedium
                )
                Surface(
                    color = statusColor.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = when (order.status) {
                            "pending" -> "Chờ xử lý"
                            "confirmed" -> "Đã xác nhận"
                            "shipped" -> "Đang giao"
                            "delivered" -> "Đã giao"
                            "cancelled" -> "Đã hủy"
                            else -> order.status
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                "Ngày đặt: ${order.orderDate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            
            Spacer(Modifier.height(8.dp))
            
            order.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${item.productName} x${item.quantity}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "${(item.price * item.quantity).toInt()} VND",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            Divider()
            Spacer(Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Tổng tiền:",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "${order.totalAmount.toInt()} VND",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (onMessageClick != null) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onMessageClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Message, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Nhắn tin với admin")
                }
            }
        }
    }
}
