package com.projek.tokweb.service.admin;

import com.projek.tokweb.dto.admin.NotificationDto;
import com.projek.tokweb.models.customer.Order;
import com.projek.tokweb.models.customer.OrderStatus;
import com.projek.tokweb.models.admin.Product;
import com.projek.tokweb.repository.customer.OrderRepository;
import com.projek.tokweb.repository.admin.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AdminNotificationService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    /**
     * Get all admin notifications with real data
     */
    public List<NotificationDto> getAdminNotifications(int limit) {
        List<NotificationDto> notifications = new ArrayList<>();
        
        // 1. New pending payment orders
        addPendingPaymentNotifications(notifications);
        
        // 2. Pending confirmation orders (payment proof uploaded)
        addPendingConfirmationNotifications(notifications);
        
        // 3. Low stock products
        addLowStockNotifications(notifications);
        
        // 4. Expired payment orders
        addExpiredPaymentNotifications(notifications);
        
        // 5. Recent orders needing attention
        addRecentOrderNotifications(notifications);
        
        // Sort by creation date (newest first) and limit
        return notifications.stream()
                .sorted(Comparator.comparing(NotificationDto::getCreatedAt, Comparator.reverseOrder()))
                .limit(limit)
                .toList();
    }
    
    /**
     * Get unread notifications count
     */
    public long getUnreadNotificationsCount() {
        long count = 0;
        
        try {
            // Count pending payment orders
            count += orderRepository.countByStatus(OrderStatus.PENDING_PAYMENT);
            
            // Count pending confirmation orders
            count += orderRepository.countByStatus(OrderStatus.PENDING_CONFIRMATION);
            
            // Count low stock products (stock <= 5)
            List<Product> lowStockProducts = productRepository.findByStockLessThanEqualAndIsActiveTrue(5);
            count += lowStockProducts.size();
            
            // Count expired orders
            List<Order> expiredOrders = orderRepository.findByStatusAndExpiresAtBefore(OrderStatus.PENDING_PAYMENT, LocalDateTime.now());
            count += expiredOrders.size();
        } catch (Exception e) {
            System.err.println("Error counting notifications: " + e.getMessage());
        }
        
        return count;
    }
    
    private void addPendingPaymentNotifications(List<NotificationDto> notifications) {
        try {
            List<Order> pendingPaymentOrders = orderRepository.findByStatus(OrderStatus.PENDING_PAYMENT);
            
            for (Order order : pendingPaymentOrders.stream().limit(5).toList()) {
                notifications.add(NotificationDto.builder()
                        .type("order")
                        .title("Pesanan Baru")
                        .message(String.format("Pesanan %s menunggu pembayaran dari %s", 
                                order.getOrderNumber(), order.getCustomerName()))
                        .icon("fas fa-shopping-cart")
                        .iconClass("order")
                        .url("/admin/pesanan")
                        .isRead(false)
                        .priority("medium")
                        .createdAt(order.getCreatedAt())
                        .timeAgo(formatTimeAgo(order.getCreatedAt()))
                        .relatedEntityType("ORDER")
                        .relatedEntityId(order.getId())
                        .build());
            }
        } catch (Exception e) {
            System.err.println("Error loading pending payment notifications: " + e.getMessage());
        }
    }
    
    private void addPendingConfirmationNotifications(List<NotificationDto> notifications) {
        try {
            List<Order> pendingConfirmationOrders = orderRepository.findByStatus(OrderStatus.PENDING_CONFIRMATION);
            
            for (Order order : pendingConfirmationOrders.stream().limit(5).toList()) {
                notifications.add(NotificationDto.builder()
                        .type("confirmation")
                        .title("Konfirmasi Pembayaran")
                        .message(String.format("Pesanan %s memerlukan konfirmasi pembayaran", 
                                order.getOrderNumber()))
                        .icon("fas fa-check-circle")
                        .iconClass("confirmation")
                        .url("/admin/pesanan")
                        .isRead(false)
                        .priority("high")
                        .createdAt(order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt())
                        .timeAgo(formatTimeAgo(order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt()))
                        .relatedEntityType("ORDER")
                        .relatedEntityId(order.getId())
                        .build());
            }
        } catch (Exception e) {
            System.err.println("Error loading pending confirmation notifications: " + e.getMessage());
        }
    }
    
    private void addLowStockNotifications(List<NotificationDto> notifications) {
        try {
            List<Product> lowStockProducts = productRepository.findByStockLessThanEqualAndIsActiveTrue(5);
            
            for (Product product : lowStockProducts.stream().limit(3).toList()) {
                notifications.add(NotificationDto.builder()
                        .type("stock")
                        .title("Stok Menipis")
                        .message(String.format("Produk %s tersisa %d unit", 
                                product.getName(), product.getStock()))
                        .icon("fas fa-exclamation-triangle")
                        .iconClass("warning")
                        .url("/admin/produk")
                        .isRead(false)
                        .priority("medium")
                        .createdAt(LocalDateTime.now())
                        .timeAgo("Baru saja")
                        .relatedEntityType("PRODUCT")
                        .relatedEntityId(product.getId())
                        .build());
            }
        } catch (Exception e) {
            System.err.println("Error loading low stock notifications: " + e.getMessage());
        }
    }
    
    private void addExpiredPaymentNotifications(List<NotificationDto> notifications) {
        try {
            List<Order> expiredOrders = orderRepository.findByStatusAndExpiresAtBefore(
                    OrderStatus.PENDING_PAYMENT, LocalDateTime.now());
            
            for (Order order : expiredOrders.stream().limit(3).toList()) {
                notifications.add(NotificationDto.builder()
                        .type("expired")
                        .title("Pembayaran Kedaluwarsa")
                        .message(String.format("Pesanan %s telah kedaluwarsa", order.getOrderNumber()))
                        .icon("fas fa-clock")
                        .iconClass("expired")
                        .url("/admin/pesanan")
                        .isRead(false)
                        .priority("high")
                        .createdAt(order.getExpiresAt())
                        .timeAgo(formatTimeAgo(order.getExpiresAt()))
                        .relatedEntityType("ORDER")
                        .relatedEntityId(order.getId())
                        .build());
            }
        } catch (Exception e) {
            System.err.println("Error loading expired payment notifications: " + e.getMessage());
        }
    }
    
    private void addRecentOrderNotifications(List<NotificationDto> notifications) {
        try {
            // Get recent orders that need attention (last 24 hours)
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            List<Order> allRecentOrders = orderRepository.findTop20ByOrderByCreatedAtDesc();
            
            // Filter orders from last 24 hours
            List<Order> recentOrders = allRecentOrders.stream()
                    .filter(order -> order.getCreatedAt().isAfter(yesterday))
                    .toList();
            
            for (Order order : recentOrders.stream().limit(3).toList()) {
                if (order.getStatus() == OrderStatus.PENDING_PAYMENT || 
                    order.getStatus() == OrderStatus.PENDING_CONFIRMATION) {
                    continue; // Skip as these are already handled above
                }
                
                String statusText = getOrderStatusText(order.getStatus());
                notifications.add(NotificationDto.builder()
                        .type("info")
                        .title("Pesanan Terbaru")
                        .message(String.format("Pesanan %s dari %s - %s", 
                                order.getOrderNumber(), order.getCustomerName(), statusText))
                        .icon("fas fa-info-circle")
                        .iconClass("info")
                        .url("/admin/pesanan")
                        .isRead(false)
                        .priority("low")
                        .createdAt(order.getCreatedAt())
                        .timeAgo(formatTimeAgo(order.getCreatedAt()))
                        .relatedEntityType("ORDER")
                        .relatedEntityId(order.getId())
                        .build());
            }
        } catch (Exception e) {
            System.err.println("Error loading recent order notifications: " + e.getMessage());
        }
    }
    
    private String getOrderStatusText(OrderStatus status) {
        return switch (status) {
            case PENDING_PAYMENT -> "Menunggu Pembayaran";
            case PENDING_CONFIRMATION -> "Menunggu Konfirmasi";
            case PAID -> "Sudah Dibayar";
            case PROCESSING -> "Sedang Diproses";
            case SHIPPED -> "Sedang Dikirim";
            case DELIVERED -> "Telah Diterima";
            case CANCELLED -> "Dibatalkan";
            case REFUNDED -> "Dikembalikan";
            default -> "Unknown";
        };
    }
    
    private String formatTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "Unknown";
        
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(dateTime, now);
        long hours = ChronoUnit.HOURS.between(dateTime, now);
        long days = ChronoUnit.DAYS.between(dateTime, now);
        
        if (minutes < 1) {
            return "Baru saja";
        } else if (minutes < 60) {
            return minutes + " menit yang lalu";
        } else if (hours < 24) {
            return hours + " jam yang lalu";
        } else if (days < 7) {
            return days + " hari yang lalu";
        } else {
            return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"));
        }
    }
}