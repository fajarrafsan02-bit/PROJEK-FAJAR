package com.projek.tokweb.controller.admin;

import java.util.List;
import java.util.Map;

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

import com.projek.tokweb.models.customer.Order;
import com.projek.tokweb.models.customer.OrderStatus;
import com.projek.tokweb.service.admin.OrderManagementService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/api/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderManagementService orderManagementService;

    @GetMapping
    public ResponseEntity<?> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        try {
            var orders = orderManagementService.getAllOrders(page, size, sortBy, sortDirection);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", orders.getContent(),
                "total", orders.getTotalElements(),
                "page", orders.getNumber(),
                "size", orders.getSize(),
                "totalPages", orders.getTotalPages()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderDetail(@PathVariable Long orderId) {
        try {
            var orderOpt = orderManagementService.getOrderById(orderId);
            if (orderOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Order tidak ditemukan"
                ));
            }

            var order = orderOpt.get();
            var orderItems = orderManagementService.getOrderItems(orderId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                    "order", order,
                    "items", orderItems
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }


    @PostMapping("/{orderId}/confirm-payment")
    public ResponseEntity<?> confirmPayment(@PathVariable Long orderId, @RequestBody Map<String, String> request) {
        try {
            String adminNotes = request.get("notes");
            var order = orderManagementService.confirmOrder(orderId, adminNotes);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Pembayaran berhasil dikonfirmasi",
                "data", order
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/{orderId}/start-processing")
    public ResponseEntity<?> startProcessing(@PathVariable Long orderId, @RequestBody Map<String, String> request) {
        try {
            String adminNotes = request.get("notes");
            var order = orderManagementService.updateOrderStatus(orderId, OrderStatus.PROCESSING, adminNotes);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Pesanan mulai diproses",
                "data", order
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/{orderId}/ship")
    public ResponseEntity<?> shipOrder(@PathVariable Long orderId, @RequestBody Map<String, String> request) {
        try {
            String notes = request.get("notes");
            String trackingNumber = request.get("trackingNumber");
            var order = orderManagementService.shipOrder(orderId, notes, trackingNumber);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Pesanan berhasil dikirim",
                "data", order
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/{orderId}/deliver")
    public ResponseEntity<?> markAsDelivered(@PathVariable Long orderId, @RequestBody Map<String, String> request) {
        try {
            String adminNotes = request.get("notes");
            var order = orderManagementService.updateOrderStatus(orderId, OrderStatus.DELIVERED, adminNotes);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Pesanan berhasil ditandai sebagai terkirim",
                "data", order
            ));
        } catch (Exception e) {
            System.err.println("❌ Error in getAllOrders: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Gagal memuat data pesanan: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId, @RequestBody Map<String, String> request) {
        try {
            String reason = request.get("reason");
            var order = orderManagementService.cancelOrder(orderId, reason);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Pesanan berhasil dibatalkan",
                "data", order
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/{orderId}/items")
    public ResponseEntity<?> getOrderItems(@PathVariable Long orderId) {
        try {
            System.out.println("📦 Getting order items for order ID: " + orderId);
            var orderItems = orderManagementService.getOrderItems(orderId);
            System.out.println("✅ Found " + orderItems.size() + " items for order " + orderId);

            // Log each item for debugging
            for (int i = 0; i < orderItems.size(); i++) {
                var item = orderItems.get(i);
                System.out.println("📦 Item " + (i+1) + ": Product=" +
                    (item.getProduct() != null ? item.getProduct().getName() : "null") +
                    ", Quantity=" + item.getQuantity() +
                    ", Price=" + item.getUnitPrice());
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", orderItems
            ));
        } catch (Exception e) {
            System.out.println("❌ Error getting order items for order " + orderId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<?> confirmOrder(@PathVariable Long orderId, @RequestBody Map<String, String> request) {
        try {
            String adminNotes = request.get("notes");
            System.out.println("✅ Confirming order - OrderId: " + orderId + ", Notes: " + adminNotes);

            var order = orderManagementService.confirmOrder(orderId, adminNotes);
            System.out.println("✅ Order confirmed successfully - OrderId: " + orderId + ", New Status: " + order.getStatus());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Pesanan berhasil dikonfirmasi",
                "data", order
            ));
        } catch (Exception e) {
            System.out.println("❌ Error confirming order " + orderId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long orderId, @RequestBody Map<String, Object> request) {
        try {
            String newStatus = (String) request.get("status");
            String notes = (String) request.get("notes");

            System.out.println("🔄 Updating order status - OrderId: " + orderId + ", NewStatus: " + newStatus + ", Notes: " + notes);

            OrderStatus status = OrderStatus.valueOf(newStatus);
            var order = orderManagementService.updateOrderStatus(orderId, status, notes);

            System.out.println("✅ Order status updated successfully for order: " + orderId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Status pesanan berhasil diupdate",
                "data", order
            ));
        } catch (Exception e) {
            System.out.println("❌ Error updating order status for order " + orderId + ": " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getOrderStatistics() {
        try {
            var stats = orderManagementService.getOrderStatistics();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", stats
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<?> deleteOrder(@PathVariable Long orderId) {
        try {
            System.out.println("🗑️ Deleting order - OrderId: " + orderId);
            orderManagementService.deleteOrder(orderId);
            System.out.println("✅ Order deleted successfully - OrderId: " + orderId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Pesanan berhasil dihapus"
            ));
        } catch (Exception e) {
            System.out.println("❌ Error deleting order " + orderId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
}