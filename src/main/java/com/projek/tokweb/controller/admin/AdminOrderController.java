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
import com.projek.tokweb.service.customer.BuktiPembayaranService;
import com.projek.tokweb.models.customer.BuktiPembayaran;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/api/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderManagementService orderManagementService;
    private final BuktiPembayaranService buktiPembayaranService;

    @GetMapping
    public ResponseEntity<?> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        try {
            System.out.println("📦 Getting all orders with page: " + page + ", size: " + size);
            var orders = orderManagementService.getAllOrders(page, size, sortBy, sortDirection);
            
            // Load bukti pembayaran for each order
            var ordersWithBukti = orders.getContent().stream().map(order -> {
                try {
                    // Load bukti pembayaran if exists
                    var bukti = buktiPembayaranService.getByOrderId(order.getId());
                    order.setBukti(bukti);
                    
                    System.out.println("📋 Order ID: " + order.getId() + ", Status: " + order.getStatus() + 
                                     ", Has bukti: " + (bukti != null ? "Yes (ID: " + bukti.getId() + ")" : "No"));
                } catch (Exception e) {
                    System.out.println("⚠️ Error loading bukti for order " + order.getId() + ": " + e.getMessage());
                }
                return order;
            }).toList();
            
            System.out.println("✅ Loaded " + ordersWithBukti.size() + " orders with bukti data");
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", ordersWithBukti,
                "total", orders.getTotalElements(),
                "page", orders.getNumber(),
                "size", orders.getSize(),
                "totalPages", orders.getTotalPages()
            ));
        } catch (Exception e) {
            System.out.println("❌ Error in getAllOrders: " + e.getMessage());
            e.printStackTrace();
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
            System.out.println("🔄 [ADMIN] Confirming payment for order: " + orderId + ", Notes: " + adminNotes);
            
            var order = orderManagementService.confirmPayment(orderId, adminNotes);
            System.out.println("✅ [ADMIN] Payment confirmed successfully for order: " + orderId);
            
            // PERBAIKAN: Load dan sertakan data bukti pembayaran dalam response
            try {
                var bukti = buktiPembayaranService.getByOrderId(order.getId());
                order.setBukti(bukti);
                System.out.println("📋 [ADMIN] Bukti pembayaran loaded for confirmed order: " + 
                                 (bukti != null ? "Yes (ID: " + bukti.getId() + ")" : "No"));
            } catch (Exception e) {
                System.out.println("⚠️ [ADMIN] Error loading bukti for confirmed order " + order.getId() + ": " + e.getMessage());
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Pembayaran berhasil dikonfirmasi",
                "data", order
            ));
        } catch (Exception e) {
            System.out.println("❌ [ADMIN] Error confirming payment for order " + orderId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Gagal mengkonfirmasi pesanan: " + e.getMessage()
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
    
    /**
     * Endpoint untuk mengecek apakah ada bukti pembayaran untuk order tertentu
     */
    @GetMapping("/{orderId}/payment-proof/check")
    public ResponseEntity<?> checkPaymentProof(@PathVariable Long orderId) {
        try {
            System.out.println("🔍 Checking payment proof for order: " + orderId);
            
            BuktiPembayaran bukti = buktiPembayaranService.getByOrderId(orderId);
            boolean hasProof = (bukti != null);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "hasPaymentProof", hasProof,
                "fileName", hasProof ? bukti.getFileName() : null,
                "contentType", hasProof ? bukti.getContentType() : null,
                "uploadedAt", hasProof ? "File tersedia" : null
            );
            
            System.out.println("✅ Payment proof check result for order " + orderId + ": " + hasProof);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Error checking payment proof for order " + orderId + ": " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Gagal mengecek bukti pembayaran: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Endpoint untuk menampilkan gambar bukti pembayaran berdasarkan order ID
     */
    @GetMapping("/{orderId}/payment-proof/image")
    public ResponseEntity<byte[]> getPaymentProofImage(@PathVariable Long orderId) {
        try {
            System.out.println("🖼️ Getting payment proof image for order: " + orderId);
            
            BuktiPembayaran bukti = buktiPembayaranService.getByOrderId(orderId);
            
            if (bukti == null) {
                System.out.println("❌ No payment proof found for order: " + orderId);
                return ResponseEntity.notFound().build();
            }
            
            if (bukti.getFileData() == null) {
                System.out.println("❌ File data is null for order: " + orderId);
                return ResponseEntity.notFound().build();
            }
            
            // Set appropriate headers for image display
            HttpHeaders headers = new HttpHeaders();
            
            // Determine content type
            String contentType = bukti.getContentType();
            if (contentType == null || contentType.trim().isEmpty()) {
                // Default to jpeg if no content type
                contentType = "image/jpeg";
            }
            headers.setContentType(MediaType.parseMediaType(contentType));
            
            // Set content disposition for inline display
            headers.add(HttpHeaders.CONTENT_DISPOSITION, 
                "inline; filename=\"" + bukti.getFileName() + "\"");
            
            headers.setContentLength(bukti.getFileData().length);
            
            System.out.println("✅ Serving payment proof image for order " + orderId + 
                             " - Size: " + bukti.getFileData().length + " bytes, Type: " + contentType);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(bukti.getFileData());
                    
        } catch (Exception e) {
            System.out.println("❌ Error serving payment proof image for order " + orderId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }
}