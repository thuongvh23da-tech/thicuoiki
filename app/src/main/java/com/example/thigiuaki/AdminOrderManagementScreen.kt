package com.example.thigiuaki

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.thigiuaki.model.Order
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrderManagementScreen() {
    val db = FirebaseFirestore.getInstance()
    var orders by remember { mutableStateOf(listOf<Order>()) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(selectedStatus) {
        var query = db.collection("orders").orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
        
        if (selectedStatus != null) {
            query = query.whereEqualTo("status", selectedStatus)
        }
        
        query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("AdminOrders", "Error: ${error.message}")
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
                title = { Text("Quản lý đơn hàng") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedStatus == null,
                    onClick = { selectedStatus = null },
                    label = { Text("Tất cả") }
                )
                FilterChip(
                    selected = selectedStatus == "pending",
                    onClick = { selectedStatus = "pending" },
                    label = { Text("Chờ xử lý") }
                )
                FilterChip(
                    selected = selectedStatus == "confirmed",
                    onClick = { selectedStatus = "confirmed" },
                    label = { Text("Đã xác nhận") }
                )
                FilterChip(
                    selected = selectedStatus == "delivered",
                    onClick = { selectedStatus = "delivered" },
                    label = { Text("Đã giao") }
                )
            }
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (orders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Không có đơn hàng nào")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(orders) { order ->
                        AdminOrderCard(
                            order = order,
                            onStatusChange = { newStatus, isProcessed ->
                                db.collection("orders").document(order.id)
                                    .update(
                                        "status", newStatus,
                                        "isProcessed", isProcessed
                                    )
                                    .addOnSuccessListener {
                                        if (newStatus == "confirmed") {
                                            db.collection("orders").document(order.id)
                                                .update("confirmedAt", Timestamp.now())
                                        } else if (newStatus == "delivered") {
                                            db.collection("orders").document(order.id)
                                                .update("deliveredAt", Timestamp.now())
                                        }
                                    }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminOrderCard(
    order: Order,
    onStatusChange: (String, Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Đơn hàng #${order.id.take(8)}",
                    style = MaterialTheme.typography.titleMedium
                )
                Surface(
                    color = when (order.status) {
                        "pending" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                        "confirmed" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        "delivered" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = when (order.status) {
                            "pending" -> "Chờ xử lý"
                            "confirmed" -> "Đã xác nhận"
                            "delivered" -> "Đã giao"
                            else -> order.status
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            Text("Ngày đặt: ${order.orderDate}")
            Text("Tổng tiền: ${order.totalAmount.toInt()} VND")
            if (order.orderNotes.isNotBlank()) {
                Text("Ghi chú: ${order.orderNotes}")
            }
            
            Spacer(Modifier.height(8.dp))
            Divider()
            Spacer(Modifier.height(8.dp))
            
            // Status change buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (order.status == "pending") {
                    Button(
                        onClick = { onStatusChange("confirmed", true) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Xác nhận")
                    }
                }
                if (order.status == "confirmed") {
                    Button(
                        onClick = { onStatusChange("delivered", true) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Đã giao")
                    }
                }
                if (!order.isProcessed) {
                    OutlinedButton(
                        onClick = { onStatusChange(order.status, true) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Đánh dấu đã xử lý")
                    }
                }
            }
        }
    }
}

