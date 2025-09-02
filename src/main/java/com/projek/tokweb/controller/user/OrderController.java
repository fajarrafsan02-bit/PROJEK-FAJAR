package com.projek.tokweb.controller.user;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projek.tokweb.dto.ApiResponse;
import com.projek.tokweb.models.customer.Order;
import com.projek.tokweb.models.customer.OrderItem;
import com.projek.tokweb.models.customer.OrderStatus;
import com.projek.tokweb.repository.customer.OrderItemRepository;
import com.projek.tokweb.repository.customer.OrderRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @GetMapping("/{orderId}/status")
    public ResponseEntity<?> getOrderStatus(@PathVariable Long orderId) {
        try {
            Optional<Order> orderOpt = orderRepository.findById(orderId);

            if (orderOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Order tidak ditemukan dengan ID: " + orderId));
            }

            Order order = orderOpt.get();
            String status = order.getStatus().name();

            return ResponseEntity.ok(ApiResponse.success("Order status retrieved",
                Map.of("status", status, "orderId", orderId)));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/my-orders")
    public ResponseEntity<?> getMyOrders() {
        try {
            // For now, use a dummy user ID. In production, get from JWT token or session
            Long userId = 1L; // TODO: Get from authentication context

            List<Order> userOrders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
            System.out.println("📦 Found " + userOrders.size() + " orders for user ID: " + userId);

            // Log each order found
            for (Order order : userOrders) {
                System.out.println("📦 Order: " + order.getOrderNumber() + " - Status: " + order.getStatus());
            }

            // Get order items for each order
            List<Map<String, Object>> ordersWithItems = userOrders.stream()
                .map(order -> {
                    List<OrderItem> items = orderItemRepository.findByOrderIdWithProduct(order.getId());
                    System.out.println("📦 Order " + order.getOrderNumber() + " has " + items.size() + " items");

                    Map<String, Object> orderMap = new HashMap<>();
                    orderMap.put("id", order.getId());
                    orderMap.put("orderNumber", order.getOrderNumber());
                    orderMap.put("status", order.getStatus());
                    orderMap.put("statusDisplayName", order.getStatus().getDisplayName());
                    orderMap.put("totalAmount", order.getTotalAmount());
                    orderMap.put("createdAt", order.getCreatedAt());
                    orderMap.put("updatedAt", order.getUpdatedAt());
                    orderMap.put("customerName", order.getCustomerName());
                    orderMap.put("customerEmail", order.getCustomerEmail());
                    orderMap.put("customerPhone", order.getCustomerPhone());
                    orderMap.put("shippingAddress", order.getShippingAddress());
                    orderMap.put("paymentMethod", order.getPaymentMethod());
                    orderMap.put("notes", order.getNotes());
                    orderMap.put("items", items.stream()
                        .map(item -> {
                            System.out.println("📦 Item: " + item.getProduct().getName() + " (Qty: " + item.getQuantity() + ")");

                            Map<String, Object> itemMap = new HashMap<>();
                            itemMap.put("id", item.getId());
                            itemMap.put("productName", item.getProduct().getName());
                            itemMap.put("productImage", item.getProduct().getImageUrl());
                            itemMap.put("quantity", item.getQuantity());
                            itemMap.put("unitPrice", item.getUnitPrice());
                            itemMap.put("subtotal", item.getSubtotal());
                            itemMap.put("weight", item.getProduct().getWeight());
                            itemMap.put("purity", item.getProduct().getPurity());
                            return itemMap;
                        })
                        .collect(Collectors.toList()));
                    return orderMap;
                })
                .collect(Collectors.toList());

            // Count orders by status
            Map<String, Long> statusCounts = Map.of(
                "all", (long) userOrders.size(),
                "pending", userOrders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING_PAYMENT || o.getStatus() == OrderStatus.PENDING_CONFIRMATION).count(),
                "processing", userOrders.stream().filter(o -> o.getStatus() == OrderStatus.PROCESSING || o.getStatus() == OrderStatus.PAID).count(),
                "shipped", userOrders.stream().filter(o -> o.getStatus() == OrderStatus.SHIPPED).count(),
                "delivered", userOrders.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).count()
            );

            return ResponseEntity.ok(ApiResponse.success("User orders retrieved",
                Map.of(
                    "orders", ordersWithItems,
                    "total", userOrders.size(),
                    "statusCounts", statusCounts
                )));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderDetail(@PathVariable Long orderId) {
        try {
            Optional<Order> orderOpt = orderRepository.findById(orderId);

            if (orderOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Order tidak ditemukan dengan ID: " + orderId));
            }

            Order order = orderOpt.get();
            List<OrderItem> items = orderItemRepository.findByOrderIdWithProduct(orderId);

            Map<String, Object> orderDetail = new HashMap<>();
            orderDetail.put("id", order.getId());
            orderDetail.put("orderNumber", order.getOrderNumber());
            orderDetail.put("status", order.getStatus());
            orderDetail.put("statusDisplayName", order.getStatus().getDisplayName());
            orderDetail.put("totalAmount", order.getTotalAmount());
            orderDetail.put("createdAt", order.getCreatedAt());
            orderDetail.put("updatedAt", order.getUpdatedAt());
            orderDetail.put("customerName", order.getCustomerName());
            orderDetail.put("customerEmail", order.getCustomerEmail());
            orderDetail.put("customerPhone", order.getCustomerPhone());
            orderDetail.put("shippingAddress", order.getShippingAddress());
            orderDetail.put("paymentMethod", order.getPaymentMethod());
            orderDetail.put("notes", order.getNotes());
            orderDetail.put("items", items.stream()
                .map(item -> {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("id", item.getId());
                    itemMap.put("productName", item.getProduct().getName());
                    itemMap.put("productImage", item.getProduct().getImageUrl());
                    itemMap.put("quantity", item.getQuantity());
                    itemMap.put("unitPrice", item.getUnitPrice());
                    itemMap.put("subtotal", item.getSubtotal());
                    itemMap.put("weight", item.getProduct().getWeight());
                    itemMap.put("purity", item.getProduct().getPurity());
                    return itemMap;
                })
                .collect(Collectors.toList()));

            return ResponseEntity.ok(ApiResponse.success("Order detail retrieved", orderDetail));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
