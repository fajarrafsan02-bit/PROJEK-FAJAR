package com.projek.tokweb.controller.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.projek.tokweb.dto.ApiResponse;
import com.projek.tokweb.dto.admin.OrderStatusUpdateRequest;
import com.projek.tokweb.models.customer.Order;
import com.projek.tokweb.models.customer.OrderStatus;
import com.projek.tokweb.service.admin.OrderManagementService;
import com.projek.tokweb.service.admin.OrderManagementService.OrderStatistics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/admin/api/orders")
@RequiredArgsConstructor
@Slf4j
public class AdminOrderController {
    
    private final OrderManagementService orderManagementService;
    
    /**
     * Get all orders with pagination
     */
    @GetMapping
    public ResponseEntity<?> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        try {
            var orders = orderManagementService.getAllOrders(page, size, sortBy, sortDirection);
            return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orders));
        } catch (Exception e) {
            log.error("Error getting all orders: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Gagal mengambil data pesanan: " + e.getMessage()));
        }
    }
    
    /**
     * Get orders by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<?> getOrdersByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            var orders = orderManagementService.getOrdersByStatus(orderStatus, page, size);
            return ResponseEntity.ok(ApiResponse.success("Orders by status retrieved successfully", orders));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Status tidak valid: " + status));
        } catch (Exception e) {
            log.error("Error getting orders by status: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Gagal mengambil data pesanan: " + e.getMessage()));
        }
    }
    
    /**
     * Get order by ID
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderById(@PathVariable Long orderId) {
        try {
            var orderOpt = orderManagementService.getOrderById(orderId);
            if (orderOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Order tidak ditemukan dengan ID: " + orderId));
            }
            return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", orderOpt.get()));
        } catch (Exception e) {
            log.error("Error getting order by ID: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Gagal mengambil data pesanan: " + e.getMessage()));
        }
    }
    
    /**
     * Get order by order number
     */
    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<?> getOrderByOrderNumber(@PathVariable String orderNumber) {
        try {
            var orderOpt = orderManagementService.getOrderByOrderNumber(orderNumber);
            if (orderOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Order tidak ditemukan dengan nomor: " + orderNumber));
            }
            return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", orderOpt.get()));
        } catch (Exception e) {
            log.error("Error getting order by order number: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Gagal mengambil data pesanan: " + e.getMessage()));
        }
    }
    
    /**
     * Search orders by customer name or phone
     */
    @GetMapping("/search/customer")
    public ResponseEntity<?> searchOrdersByCustomer(@RequestParam String searchTerm) {
        try {
            var orders = orderManagementService.searchOrdersByCustomer(searchTerm);
            return ResponseEntity.ok(ApiResponse.success("Orders search completed", orders));
        } catch (Exception e) {
            log.error("Error searching orders by customer: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Gagal mencari pesanan: " + e.getMessage()));
        }
    }
    
    /**
     * Search orders by order number
     */
    @GetMapping("/search/order-number")
    public ResponseEntity<?> searchOrdersByOrderNumber(@RequestParam String orderNumber) {
        try {
            var orders = orderManagementService.searchOrdersByOrderNumber(orderNumber);
            return ResponseEntity.ok(ApiResponse.success("Orders search completed", orders));
        } catch (Exception e) {
            log.error("Error searching orders by order number: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Gagal mencari pesanan: " + e.getMessage()));
        }
    }
    
    /**
     * Confirm order (mark as PAID)
     */
    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<?> confirmOrder(
            @PathVariable Long orderId,
            @RequestBody(required = false) OrderStatusUpdateRequest request) {
        try {
            String adminNotes = request != null ? request.getNotes() : null;
            Order confirmedOrder = orderManagementService.confirmOrder(orderId, adminNotes);
            return ResponseEntity.ok(ApiResponse.success("Order berhasil dikonfirmasi", confirmedOrder));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error confirming order: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Gagal mengkonfirmasi pesanan: " + e.getMessage()));
        }
    }
    
    /**
     * Update order status
     */
    @PutMapping("/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody OrderStatusUpdateRequest request) {
        try {
            OrderStatus newStatus = OrderStatus.valueOf(request.getStatus().toUpperCase());
            Order updatedOrder = orderManagementService.updateOrderStatus(orderId, newStatus, request.getNotes());
            return ResponseEntity.ok(ApiResponse.success("Status pesanan berhasil diubah", updatedOrder));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating order status: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Gagal mengubah status pesanan: " + e.getMessage()));
        }
    }
    
    /**
     * Cancel order
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable Long orderId,
            @RequestBody(required = false) OrderStatusUpdateRequest request) {
        try {
            String reason = request != null ? request.getNotes() : null;
            Order cancelledOrder = orderManagementService.cancelOrder(orderId, reason);
            return ResponseEntity.ok(ApiResponse.success("Order berhasil dibatalkan", cancelledOrder));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error cancelling order: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Gagal membatalkan pesanan: " + e.getMessage()));
        }
    }
    
    /**
     * Get order statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getOrderStatistics() {
        try {
            OrderStatistics stats = orderManagementService.getOrderStatistics();
            return ResponseEntity.ok(ApiResponse.success("Order statistics retrieved successfully", stats));
        } catch (Exception e) {
            log.error("Error getting order statistics: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Gagal mengambil statistik pesanan: " + e.getMessage()));
        }
    }
    
    /**
     * Get recent orders (last 7 days)
     */
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentOrders() {
        try {
            List<Order> recentOrders = orderManagementService.getRecentOrders();
            return ResponseEntity.ok(ApiResponse.success("Recent orders retrieved successfully", recentOrders));
        } catch (Exception e) {
            log.error("Error getting recent orders: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Gagal mengambil pesanan terbaru: " + e.getMessage()));
        }
    }
    
    /**
     * Get orders that need attention
     */
    @GetMapping("/attention")
    public ResponseEntity<?> getOrdersNeedingAttention() {
        try {
            List<Order> attentionOrders = orderManagementService.getOrdersNeedingAttention();
            return ResponseEntity.ok(ApiResponse.success("Orders needing attention retrieved successfully", attentionOrders));
        } catch (Exception e) {
            log.error("Error getting orders needing attention: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Gagal mengambil pesanan yang memerlukan perhatian: " + e.getMessage()));
        }
    }
    
    /**
     * Get all available order statuses
     */
    @GetMapping("/statuses")
    public ResponseEntity<?> getAllOrderStatuses() {
        try {
            OrderStatus[] statuses = OrderStatus.values();
            return ResponseEntity.ok(ApiResponse.success("Order statuses retrieved successfully", statuses));
        } catch (Exception e) {
            log.error("Error getting order statuses: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Gagal mengambil status pesanan: " + e.getMessage()));
        }
    }
}