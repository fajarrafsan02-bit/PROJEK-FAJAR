package com.projek.tokweb.service.admin;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projek.tokweb.models.customer.Order;
import com.projek.tokweb.models.customer.OrderItem;
import com.projek.tokweb.models.customer.OrderStatus;
import com.projek.tokweb.models.customer.PaymentTransaction;
import com.projek.tokweb.models.customer.PaymentStatus;
import com.projek.tokweb.repository.customer.OrderRepository;
import com.projek.tokweb.repository.customer.OrderItemRepository;
import com.projek.tokweb.repository.customer.PaymentTransactionRepository;
import com.projek.tokweb.repository.admin.ProductRepository;
import com.projek.tokweb.models.admin.Product;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import com.projek.tokweb.models.activity.ActivityType;
import com.projek.tokweb.service.activity.ActivityLogService;
import com.projek.tokweb.service.customer.CheckoutService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderManagementService {
    
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ActivityLogService activityLogService;
    private final CheckoutService checkoutService;
    private final ProductRepository productRepository;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private RevenueService revenueService;
    
    @Autowired
    private BestSellingProductService bestSellingProductService;
    
    /**
     * Get all orders with pagination
     */
    public Page<Order> getAllOrders(int page, int size, String sortBy, String sortDirection) {
        try {
            log.info("📦 Getting orders - page: {}, size: {}, sortBy: {}, sortDirection: {}", page, size, sortBy, sortDirection);

            Sort sort = sortDirection.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<Order> orders = orderRepository.findAllOrderByCreatedAtDesc(pageable);
            log.info("✅ Found {} orders out of {} total", orders.getNumberOfElements(), orders.getTotalElements());

            return orders;
        } catch (Exception e) {
            log.error("❌ Error getting all orders: {}", e.getMessage(), e);
            // Return empty page instead of throwing exception
            return Page.empty();
        }
    }
    
    /**
     * Get orders by status with pagination
     */
    public Page<Order> getOrdersByStatus(OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return orderRepository.findByStatus(status, pageable);
    }
    
    /**
     * Get orders by multiple statuses
     */
    public List<Order> getOrdersByStatuses(List<OrderStatus> statuses) {
        return orderRepository.findByStatusInOrderByCreatedAtDesc(statuses);
    }
    
    /**
     * Get order by ID
     */
    public Optional<Order> getOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }
    
    /**
     * Get order by order number
     */
    public Optional<Order> getOrderByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }
    
    /**
     * Search orders by customer name or phone
     */
    public List<Order> searchOrdersByCustomer(String searchTerm) {
        return orderRepository.findByCustomerNameOrPhoneContainingOrderByCreatedAtDesc(searchTerm, searchTerm);
    }
    
    /**
     * Search orders by order number
     */
    public List<Order> searchOrdersByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumberContainingOrderByCreatedAtDesc(orderNumber);
    }
    
    /**
     * Confirm order (change status to PAID) - USES CHECKOUT SERVICE FOR STOCK MANAGEMENT
     */
    @Transactional
    public Order confirmOrder(Long orderId, String adminNotes) {
        log.info("🔄 [ORDER_CONFIRM] Starting order confirmation for ID: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order tidak ditemukan: " + orderId));
        
        if (!order.canBeMarkedAsPaid()) {
            throw new IllegalStateException("Order tidak dapat dikonfirmasi dengan status: " + order.getStatus());
        }
        
        // Get external payment ID from payment transaction
        List<PaymentTransaction> transactions = paymentTransactionRepository.findByOrder(order);
        if (transactions.isEmpty()) {
            throw new IllegalStateException("Tidak ada payment transaction untuk order: " + orderId);
        }
        
        PaymentTransaction latestTransaction = transactions.get(0);
        String externalId = latestTransaction.getExternalPaymentId();
        
        log.info("💳 [ORDER_CONFIRM] Using external payment ID: {} for order: {}", externalId, orderId);
        
        // USE CHECKOUT SERVICE TO CONFIRM PAYMENT WITH STOCK MANAGEMENT
        try {
            log.info("🔄 [ORDER_CONFIRM] Calling CheckoutService.confirmPayment for external ID: {}", externalId);
            checkoutService.confirmPayment(externalId);
            log.info("✅ [ORDER_CONFIRM] CheckoutService.confirmPayment completed successfully");
        } catch (Exception e) {
            log.error("❌ [ORDER_CONFIRM] Error in CheckoutService.confirmPayment: {}", e.getMessage(), e);
            throw new IllegalStateException("Gagal mengkonfirmasi pembayaran: " + e.getMessage(), e);
        }
        
        // Refresh order from database after CheckoutService update
        Order confirmedOrder = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order tidak ditemukan setelah konfirmasi: " + orderId));
        
        // Update admin notes if provided
        if (adminNotes != null && !adminNotes.trim().isEmpty()) {
            String existingNotes = confirmedOrder.getNotes();
            String newNotes = existingNotes != null ? existingNotes + "\n" + adminNotes : adminNotes;
            confirmedOrder.setNotes(newNotes);
            confirmedOrder = orderRepository.save(confirmedOrder);
        }
        
        log.info("🏆 [ORDER_CONFIRM] Order {} dikonfirmasi oleh admin dengan status: {}", orderId, confirmedOrder.getStatus());
        
        // Update best selling products data
        try {
            updateBestSellingProducts(confirmedOrder);
            log.info("📈 [ORDER_CONFIRM] Best selling products updated for order: {}", orderId);
        } catch (Exception e) {
            log.error("❌ [ORDER_CONFIRM] Error updating best selling products for order {}: {}", orderId, e.getMessage(), e);
            // Continue even if updating best selling products fails
        }
        
        // Record revenue when order is confirmed (non-blocking)
        try {
            String confirmedBy = "Admin"; // You can get actual admin name from security context
            var revenueResult = revenueService.recordRevenue(confirmedOrder, confirmedBy);
            if (revenueResult != null) {
                log.info("Revenue recorded for order: {} with amount: {}", confirmedOrder.getOrderNumber(), confirmedOrder.getTotalAmount());
            } else {
                log.warn("Failed to record revenue for order: {}, but order confirmation continues", confirmedOrder.getOrderNumber());
            }
        } catch (Exception e) {
            log.error("Failed to record revenue for order: {}", confirmedOrder.getOrderNumber(), e);
            // Don't throw exception here to avoid breaking the order confirmation process
        }
        
        return confirmedOrder;
    }
    
    /**
     * Update order status
     */
    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus, String adminNotes) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order tidak ditemukan: " + orderId));
        
        if (!order.getStatus().canTransitionTo(newStatus)) {
            throw new IllegalStateException("Tidak dapat mengubah status dari " + order.getStatus() + " ke " + newStatus);
        }
        
        // Update order status
        order.setStatus(newStatus);
        order.setNotes(adminNotes != null ? adminNotes : "Status diubah oleh admin");
        
        // Set specific timestamps based on status
        switch (newStatus) {
            case PAID:
                order.setPaidAt(LocalDateTime.now());
                break;
            case CANCELLED:
                order.setCancelledAt(LocalDateTime.now());
                break;
            default:
                break;
        }
        
        Order savedOrder = orderRepository.save(order);
        log.info("Order {} status diubah dari {} ke {} oleh admin", orderId, order.getStatus(), newStatus);
        
        // Log aktivitas berdasarkan status baru
        ActivityType activityType = getActivityTypeForStatus(newStatus);
        String activityTitle = getActivityTitleForStatus(newStatus);
        String activityDescription = String.format("Pesanan %s dari %s berubah status ke %s oleh admin", 
            savedOrder.getOrderNumber(),
            savedOrder.getCustomerName(),
            newStatus.getDisplayName());
            
        activityLogService.logActivity(
            activityType,
            activityTitle,
            activityDescription,
            "ADMIN",
            "Admin System",
            "ADMIN"
        );
        
        return savedOrder;
    }
    
    /**
     * Cancel order - USES CHECKOUT SERVICE FOR STOCK RESTORE
     */
    @Transactional
    public Order cancelOrder(Long orderId, String reason) {
        log.info("🚫 [ORDER_CANCEL] Starting order cancellation for ID: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order tidak ditemukan: " + orderId));
        
        if (!order.canBeCancelled()) {
            throw new IllegalStateException("Order tidak dapat dibatalkan dengan status: " + order.getStatus());
        }
        
        // Get external payment ID from payment transaction
        List<PaymentTransaction> transactions = paymentTransactionRepository.findByOrder(order);
        if (!transactions.isEmpty()) {
            PaymentTransaction latestTransaction = transactions.get(0);
            String externalId = latestTransaction.getExternalPaymentId();
            
            log.info("💳 [ORDER_CANCEL] Using external payment ID: {} for order: {}", externalId, orderId);
            
            // USE CHECKOUT SERVICE TO CANCEL ORDER WITH STOCK RESTORE
            try {
                log.info("🔄 [ORDER_CANCEL] Calling CheckoutService.cancelOrder for external ID: {}", externalId);
                checkoutService.cancelOrder(externalId, reason);
                log.info("✅ [ORDER_CANCEL] CheckoutService.cancelOrder completed successfully");
            } catch (Exception e) {
                log.error("❌ [ORDER_CANCEL] Error in CheckoutService.cancelOrder: {}", e.getMessage(), e);
                throw new IllegalStateException("Gagal membatalkan pesanan: " + e.getMessage(), e);
            }
            
            // Refresh order from database after CheckoutService update
            Order cancelledOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order tidak ditemukan setelah pembatalan: " + orderId));
            
            log.info("🏆 [ORDER_CANCEL] Order {} dibatalkan oleh admin dengan status: {}", orderId, cancelledOrder.getStatus());
            
            return cancelledOrder;
        } else {
            // Fallback: No payment transaction found, just update order status locally
            log.warn("⚠️ [ORDER_CANCEL] No payment transaction found for order {}, updating status locally only", orderId);
            
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancelledAt(LocalDateTime.now());
            order.setNotes("Dibatalkan oleh admin. Alasan: " + (reason != null ? reason : "Tidak ada alasan"));
            
            Order savedOrder = orderRepository.save(order);
            log.info("Order {} dibatalkan oleh admin. Alasan: {}", orderId, reason);
            
            return savedOrder;
        }
    }
    
    /**
     * Get order statistics
     */
    public OrderStatistics getOrderStatistics() {
        try {
            long totalOrders = orderRepository.count();
            long pendingPayment = orderRepository.countByStatus(OrderStatus.PENDING_PAYMENT);
            long pendingConfirmation = orderRepository.countByStatus(OrderStatus.PENDING_CONFIRMATION);
            long paid = orderRepository.countByStatus(OrderStatus.PAID);
            long processing = orderRepository.countByStatus(OrderStatus.PROCESSING);
            long shipped = orderRepository.countByStatus(OrderStatus.SHIPPED);
            long delivered = orderRepository.countByStatus(OrderStatus.DELIVERED);
            long cancelled = orderRepository.countByStatus(OrderStatus.CANCELLED);

            return OrderStatistics.builder()
                .totalOrders(totalOrders)
                .pendingPayment(pendingPayment)
                .pendingConfirmation(pendingConfirmation)
                .paid(paid)
                .processing(processing)
                .shipped(shipped)
                .delivered(delivered)
                .cancelled(cancelled)
                .build();
        } catch (Exception e) {
            log.error("❌ Error getting order statistics: {}", e.getMessage(), e);
            // Return empty statistics instead of throwing exception
            return OrderStatistics.builder()
                .totalOrders(0)
                .pendingPayment(0)
                .pendingConfirmation(0)
                .paid(0)
                .processing(0)
                .shipped(0)
                .delivered(0)
                .cancelled(0)
                .build();
        }
    }
    
    /**
     * Get recent orders (last 7 days)
     */
    public List<Order> getRecentOrders() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        return orderRepository.findAll().stream()
            .filter(order -> order.getCreatedAt().isAfter(sevenDaysAgo))
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .limit(10)
            .toList();
    }
    
    /**
     * Get orders that need attention (pending payment, pending confirmation)
     */
    public List<Order> getOrdersNeedingAttention() {
        List<OrderStatus> attentionStatuses = List.of(
            OrderStatus.PENDING_PAYMENT, 
            OrderStatus.PENDING_CONFIRMATION
        );
        return orderRepository.findByStatusInOrderByCreatedAtDesc(attentionStatuses);
    }

    /**
     * Confirm payment (change status from PENDING_CONFIRMATION to PAID) - Admin version without stock reduction
     */
    @Transactional
    public Order confirmPayment(Long orderId, String adminNotes) {
        log.info("🔄 [ADMIN_CONFIRM] Starting payment confirmation for order: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order tidak ditemukan: " + orderId));
        
        // Check if order can be confirmed
        if (order.getStatus() != OrderStatus.PENDING_CONFIRMATION) {
            log.error("❌ [ADMIN_CONFIRM] Cannot confirm order {} with status: {}", orderId, order.getStatus());
            throw new IllegalStateException("Order tidak dapat dikonfirmasi dengan status: " + order.getStatus().getDisplayName());
        }
        
        try {
            // PERBAIKAN: Kurangi stok produk terlebih dahulu sebelum mengubah status
            try {
                log.info("📉 [ADMIN_CONFIRM] Reducing product stock for order: {}", order.getOrderNumber());
                reduceProductStockForOrder(order);
                log.info("✅ [ADMIN_CONFIRM] Stock reduction completed successfully");
            } catch (Exception e) {
                log.error("❌ [ADMIN_CONFIRM] Failed to reduce stock for order {}: {}", orderId, e.getMessage());
                throw new IllegalStateException("Gagal mengurangi stok produk: " + e.getMessage(), e);
            }
            
            // Update order status to PAID
            order.setStatus(OrderStatus.PAID);
            order.setPaidAt(LocalDateTime.now());
            
            // Update admin notes
            if (adminNotes != null && !adminNotes.trim().isEmpty()) {
                String existingNotes = order.getNotes();
                String newNotes = existingNotes != null ? existingNotes + "\n" + adminNotes : adminNotes;
                order.setNotes(newNotes);
            } else {
                order.setNotes("Pembayaran dikonfirmasi oleh admin");
            }
            
            Order savedOrder = orderRepository.save(order);
            log.info("✅ [ADMIN_CONFIRM] Order saved with PAID status: {}", orderId);
            
            // Update best selling products data
            try {
                updateBestSellingProducts(savedOrder);
                log.info("📈 [ADMIN_CONFIRM] Best selling products updated for order: {}", orderId);
            } catch (Exception e) {
                log.error("❌ [ADMIN_CONFIRM] Error updating best selling products for order {}: {}", orderId, e.getMessage(), e);
                // Continue even if updating best selling products fails
            }
            
            // Log aktivitas: pembayaran dikonfirmasi (async to avoid transaction issues)
            try {
                // Use a separate thread to avoid transaction rollback issues
                CompletableFuture.runAsync(() -> {
                    try {
                        activityLogService.logActivity(
                            ActivityType.ORDER_CONFIRMED,
                            "Pembayaran Dikonfirmasi",
                            String.format("Pembayaran pesanan %s dari %s telah dikonfirmasi oleh admin", 
                                savedOrder.getOrderNumber(),
                                savedOrder.getCustomerName()),
                            "ADMIN",
                            "Admin System",
                            "ADMIN"
                        );
                        log.info("✅ [ADMIN_CONFIRM] Activity log recorded for order: {}", orderId);
                    } catch (Exception e) {
                        log.warn("⚠️ [ADMIN_CONFIRM] Failed to log activity for order {}: {}", orderId, e.getMessage());
                    }
                });
            } catch (Exception e) {
                log.warn("⚠️ [ADMIN_CONFIRM] Failed to start async activity logging for order {}: {}", orderId, e.getMessage());
                // Don't throw exception here
            }
            
            // Record revenue when payment is confirmed
            try {
                String confirmedBy = "Admin"; // You can get actual admin name from security context
                revenueService.recordRevenue(savedOrder, confirmedBy);
                log.info("✅ [ADMIN_CONFIRM] Revenue recorded for payment confirmation: {} with amount: {}", savedOrder.getOrderNumber(), savedOrder.getTotalAmount());
            } catch (Exception e) {
                log.warn("⚠️ [ADMIN_CONFIRM] Failed to record revenue for payment confirmation: {}", savedOrder.getOrderNumber(), e);
                // Don't throw exception here to avoid breaking the payment confirmation process
            }
            
            log.info("🏆 [ADMIN_CONFIRM] Payment confirmation completed successfully for order: {}", orderId);
            return savedOrder;
            
        } catch (Exception e) {
            log.error("❌ [ADMIN_CONFIRM] Error confirming payment for order {}: {}", orderId, e.getMessage(), e);
            throw new IllegalStateException("Gagal mengkonfirmasi pembayaran: " + e.getMessage(), e);
        }
    }

    /**
     * Ship order (change status to SHIPPED)
     */
    @Transactional
    public Order shipOrder(Long orderId, String notes, String trackingNumber) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order tidak ditemukan: " + orderId));
        
        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.PROCESSING) {
            throw new IllegalStateException("Order tidak dapat dikirim dengan status: " + order.getStatus());
        }
        
        // Update order status
        order.setStatus(OrderStatus.SHIPPED);
        order.setNotes(notes != null ? notes : "Pesanan dikirim");
        
        // Add tracking number to notes if provided
        if (trackingNumber != null && !trackingNumber.trim().isEmpty()) {
            String currentNotes = order.getNotes() != null ? order.getNotes() : "";
            order.setNotes(currentNotes + "\nTracking Number: " + trackingNumber);
        }
        
        return orderRepository.save(order);
    }

    /**
     * Delete order and all related data
     */
    @Transactional
    public void deleteOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order tidak ditemukan: " + orderId));

        // Delete order items first
        List<OrderItem> orderItems = orderItemRepository.findByOrderIdWithProduct(orderId);
        if (!orderItems.isEmpty()) {
            orderItemRepository.deleteAll(orderItems);
            log.info("Deleted {} order items for order {}", orderItems.size(), orderId);
        }

        // Delete payment transactions
        List<PaymentTransaction> transactions = paymentTransactionRepository.findByOrder(order);
        if (!transactions.isEmpty()) {
            paymentTransactionRepository.deleteAll(transactions);
            log.info("Deleted {} payment transactions for order {}", transactions.size(), orderId);
        }

        // Delete the order
        orderRepository.delete(order);
        log.info("Order {} deleted successfully", orderId);
    }

    /**
     * Get order items
     */
    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemRepository.findByOrderIdWithProduct(orderId);
    }

    /**
     * Export orders to Excel
     */
    public byte[] exportOrders(String status, String search) {
        // This is a placeholder implementation
        // In a real application, you would use Apache POI or similar library
        // For now, we'll return a simple CSV-like format
        
        List<Order> orders;
        if (status != null && !status.isEmpty()) {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            orders = orderRepository.findByStatus(orderStatus);
        } else if (search != null && !search.isEmpty()) {
            orders = orderRepository.findByCustomerNameOrPhoneContainingOrderByCreatedAtDesc(search, search);
        } else {
            orders = orderRepository.findAllOrderByCreatedAtDesc();
        }
        
        // Create CSV content
        StringBuilder csv = new StringBuilder();
        csv.append("Order Number,Customer Name,Customer Phone,Customer Email,Total Amount,Status,Created At\n");
        
        for (Order order : orders) {
            csv.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",%.2f,\"%s\",\"%s\"\n",
                order.getOrderNumber(),
                order.getCustomerName() != null ? order.getCustomerName() : "",
                order.getCustomerPhone() != null ? order.getCustomerPhone() : "",
                order.getCustomerEmail() != null ? order.getCustomerEmail() : "",
                order.getTotalAmount(),
                order.getStatus().getDisplayName(),
                order.getCreatedAt()
            ));
        }
        
        return csv.toString().getBytes();
    }
    
    /**
     * Helper method to get ActivityType for order status
     */
    private ActivityType getActivityTypeForStatus(OrderStatus status) {
        switch (status) {
            case PAID:
                return ActivityType.ORDER_CONFIRMED;
            case PROCESSING:
                return ActivityType.ORDER_PROCESSING;
            case SHIPPED:
                return ActivityType.ORDER_SHIPPED;
            case DELIVERED:
                return ActivityType.ORDER_COMPLETED;
            case CANCELLED:
                return ActivityType.ORDER_CANCELLED;
            default:
                return ActivityType.ORDER_STATUS_UPDATE;
        }
    }
    
    /**
     * Helper method to get activity title for order status
     */
    private String getActivityTitleForStatus(OrderStatus status) {
        switch (status) {
            case PAID:
                return "Pesanan Dikonfirmasi";
            case PROCESSING:
                return "Pesanan Sedang Diproses";
            case SHIPPED:
                return "Pesanan Dikirim";
            case DELIVERED:
                return "Pesanan Selesai";
            case CANCELLED:
                return "Pesanan Dibatalkan";
            default:
                return "Status Pesanan Diubah";
        }
    }
    
    // Inner class for statistics
    public static class OrderStatistics {
        private final long totalOrders;
        private final long pendingPayment;
        private final long pendingConfirmation;
        private final long paid;
        private final long processing;
        private final long shipped;
        private final long delivered;
        private final long cancelled;
        
        public OrderStatistics(long totalOrders, long pendingPayment, long pendingConfirmation, 
                             long paid, long processing, long shipped, long delivered, long cancelled) {
            this.totalOrders = totalOrders;
            this.pendingPayment = pendingPayment;
            this.pendingConfirmation = pendingConfirmation;
            this.paid = paid;
            this.processing = processing;
            this.shipped = shipped;
            this.delivered = delivered;
            this.cancelled = cancelled;
        }
        
        // Getters
        public long getTotalOrders() { return totalOrders; }
        public long getPendingPayment() { return pendingPayment; }
        public long getPendingConfirmation() { return pendingConfirmation; }
        public long getPaid() { return paid; }
        public long getProcessing() { return processing; }
        public long getShipped() { return shipped; }
        public long getDelivered() { return delivered; }
        public long getCancelled() { return cancelled; }
        
        public static OrderStatisticsBuilder builder() {
            return new OrderStatisticsBuilder();
        }
        
        public static class OrderStatisticsBuilder {
            private long totalOrders;
            private long pendingPayment;
            private long pendingConfirmation;
            private long paid;
            private long processing;
            private long shipped;
            private long delivered;
            private long cancelled;
            
            public OrderStatisticsBuilder totalOrders(long totalOrders) {
                this.totalOrders = totalOrders;
                return this;
            }
            
            public OrderStatisticsBuilder pendingPayment(long pendingPayment) {
                this.pendingPayment = pendingPayment;
                return this;
            }
            
            public OrderStatisticsBuilder pendingConfirmation(long pendingConfirmation) {
                this.pendingConfirmation = pendingConfirmation;
                return this;
            }
            
            public OrderStatisticsBuilder paid(long paid) {
                this.paid = paid;
                return this;
            }
            
            public OrderStatisticsBuilder processing(long processing) {
                this.processing = processing;
                return this;
            }
            
            public OrderStatisticsBuilder shipped(long shipped) {
                this.shipped = shipped;
                return this;
            }
            
            public OrderStatisticsBuilder delivered(long delivered) {
                this.delivered = delivered;
                return this;
            }
            
            public OrderStatisticsBuilder cancelled(long cancelled) {
                this.cancelled = cancelled;
                return this;
            }
            
            public OrderStatistics build() {
                return new OrderStatistics(totalOrders, pendingPayment, pendingConfirmation, 
                                        paid, processing, shipped, delivered, cancelled);
            }
        }
    }

    /**
     * Reduce product stock when admin confirms order payment
     */
    @Transactional
    private void reduceProductStockForOrder(Order order) {
        if (order == null || order.getItems() == null || order.getItems().isEmpty()) {
            log.warn("⚠️ [STOCK_REDUCTION] Order or order items is null/empty");
            return;
        }
        
        log.info("🔄 [STOCK_REDUCTION] Processing stock reduction for order: {} (ID: {})", order.getOrderNumber(), order.getId());
        log.info("🔄 [STOCK_REDUCTION] Order status: {}, Items count: {}", order.getStatus(), order.getItems().size());
        
        // Log all order items first for debugging
        for (int i = 0; i < order.getItems().size(); i++) {
            OrderItem item = order.getItems().get(i);
            log.info("📋 [STOCK_REDUCTION] Item {}: Product ID={}, Name='{}', Quantity={}, Unit Price={}", 
                     i + 1, 
                     item.getProduct() != null ? item.getProduct().getId() : "NULL",
                     item.getProduct() != null ? item.getProduct().getName() : "NULL",
                     item.getQuantity(),
                     item.getUnitPrice());
        }
        
        for (OrderItem orderItem : order.getItems()) {
            try {
                Product product = orderItem.getProduct();
                if (product == null) {
                    log.error("❌ [STOCK_REDUCTION] Product is null for order item in order: {}", order.getOrderNumber());
                    throw new IllegalStateException("Product tidak ditemukan untuk item pesanan");
                }
                
                int orderedQuantity = orderItem.getQuantity();
                
                log.info("🔍 [STOCK_REDUCTION] Processing product: {} (ID: {}) | Current stock: {} | Ordered qty: {}", 
                         product.getName(), product.getId(), product.getStock(), orderedQuantity);
                
                // Lock product untuk mencegah race condition
                Product lockedProduct = productRepository.findByIdForUpdate(product.getId())
                        .orElseThrow(() -> {
                            log.error("❌ [STOCK_REDUCTION] Product not found with ID: {}", product.getId());
                            return new IllegalStateException("Product tidak ditemukan: " + product.getId());
                        });
                
                int currentStock = lockedProduct.getStock();
                log.info("🔒 [STOCK_REDUCTION] Locked product: {} | Fresh stock from DB: {}", 
                         lockedProduct.getName(), currentStock);
                
                // Validasi stock masih mencukupi
                if (currentStock < orderedQuantity) {
                    String errorMsg = String.format(
                        "Stock tidak mencukupi untuk produk '%s' (ID: %d). Tersedia: %d, Diminta: %d",
                        lockedProduct.getName(), lockedProduct.getId(), currentStock, orderedQuantity);
                    log.error("❌ [STOCK_REDUCTION] {}", errorMsg);
                    
                    // Check if there are other pending orders that might have reserved stock
                    log.info("🔍 [STOCK_REDUCTION] Checking other pending orders for product: {}", lockedProduct.getName());
                    
                    throw new IllegalStateException(errorMsg);
                }
                
                // Kurangi stock
                int newStock = currentStock - orderedQuantity;
                lockedProduct.setStock(newStock);
                Product savedProduct = productRepository.save(lockedProduct);
                
                log.info("✅ [STOCK_REDUCTION] Stock updated for: {} (ID: {}) | {} -> {} | Saved stock: {}", 
                         lockedProduct.getName(), lockedProduct.getId(), 
                         currentStock, newStock, savedProduct.getStock());
                
            } catch (Exception e) {
                log.error("❌ [STOCK_REDUCTION] Error reducing stock for product ID {} in order {}: {}", 
                         orderItem.getProduct() != null ? orderItem.getProduct().getId() : "NULL", 
                         order.getOrderNumber(), e.getMessage());
                e.printStackTrace();
                
                String productName = orderItem.getProduct() != null ? orderItem.getProduct().getName() : "Unknown Product";
                throw new IllegalStateException("Gagal mengurangi stok untuk produk '" + 
                    productName + "': " + e.getMessage(), e);
            }
        }
        
        // Force flush untuk memastikan perubahan stock ter-commit
        try {
            entityManager.flush();
            log.info("💾 [STOCK_REDUCTION] Stock changes flushed to database for order: {}", order.getOrderNumber());
        } catch (Exception e) {
            log.error("⚠️ [STOCK_REDUCTION] Error flushing stock changes for order {}: {}", order.getOrderNumber(), e.getMessage());
            e.printStackTrace();
            throw new IllegalStateException("Gagal menyimpan perubahan stok: " + e.getMessage(), e);
        }
        
        log.info("🏁 [STOCK_REDUCTION] Stock reduction completed successfully for order: {} (ID: {})", 
                 order.getOrderNumber(), order.getId());
    }
    
    /**
     * Update best selling products data based on confirmed order
     */
    private void updateBestSellingProducts(Order order) {
        if (order == null || order.getItems() == null || order.getItems().isEmpty()) {
            log.warn("⚠️ [BEST_SELLING] Order or order items is null/empty");
            return;
        }
        
        log.info("🔄 [BEST_SELLING] Updating best selling products for order: {} (ID: {})", order.getOrderNumber(), order.getId());
        
        for (OrderItem orderItem : order.getItems()) {
            try {
                Product product = orderItem.getProduct();
                if (product == null) {
                    log.error("❌ [BEST_SELLING] Product is null for order item in order: {}", order.getOrderNumber());
                    continue;
                }
                
                int orderedQuantity = orderItem.getQuantity();
                
                log.info("🔍 [BEST_SELLING] Updating sales count for product: {} (ID: {}) | Ordered qty: {}", 
                         product.getName(), product.getId(), orderedQuantity);
                
                // Update best selling product data
                bestSellingProductService.updateBestSellingProduct(product.getId(), orderedQuantity);
                
                log.info("✅ [BEST_SELLING] Sales count updated for: {} (ID: {}) | Quantity: {}", 
                         product.getName(), product.getId(), orderedQuantity);
                
            } catch (Exception e) {
                log.error("❌ [BEST_SELLING] Error updating best selling product for product ID {} in order {}: {}", 
                         orderItem.getProduct() != null ? orderItem.getProduct().getId() : "NULL", 
                         order.getOrderNumber(), e.getMessage());
                // Continue with other items even if one fails
            }
        }
        
        log.info("🏁 [BEST_SELLING] Best selling products update completed for order: {} (ID: {})", 
                 order.getOrderNumber(), order.getId());
    }
}