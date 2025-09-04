package com.projek.tokweb.controller.admin;

import com.projek.tokweb.dto.admin.RevenueStatisticsDto;
import com.projek.tokweb.service.admin.RevenueService;
import com.projek.tokweb.service.admin.DashboardService;
import com.projek.tokweb.utils.AuthUtils;
import com.projek.tokweb.models.Role;
import com.projek.tokweb.models.User;
import com.projek.tokweb.models.customer.Order;
import com.projek.tokweb.models.customer.OrderStatus;
import com.projek.tokweb.repository.UserRespository;
import com.projek.tokweb.repository.admin.ProductRepository;
import com.projek.tokweb.repository.customer.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/api/dashboard")
public class AdminDashboardController {
    
    @Autowired
    private RevenueService revenueService;
    
    @Autowired
    private DashboardService dashboardService;
    
    @Autowired
    private UserRespository userRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    // REST API endpoints untuk dashboard
    @GetMapping("/revenue")
    public ResponseEntity<Map<String, Object>> getDashboardRevenue() {
        try {
            // Check if user is authenticated and is admin
            if (!AuthUtils.isAuthenticated()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Anda harus login terlebih dahulu");
                return ResponseEntity.status(401).body(errorResponse);
            }
            
            if (!AuthUtils.isAdmin()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Akses ditolak. Hanya admin yang dapat mengakses data ini.");
                return ResponseEntity.status(403).body(errorResponse);
            }
            
            // Get current user info for logging
            User currentUser = AuthUtils.getCurrentUser();
            System.out.println("Admin " + currentUser.getNamaLengkap() + " mengakses dashboard revenue data");
            
            RevenueStatisticsDto dashboardData = revenueService.getDashboardStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", dashboardData);
            response.put("user", currentUser.getNamaLengkap()); // Add current user info to response
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Gagal mengambil data revenue dashboard: " + e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Endpoint untuk mengambil data grafik penjualan
     */
    @GetMapping("/sales-chart")
    public ResponseEntity<Map<String, Object>> getSalesChartData(
            @RequestParam(defaultValue = "7days") String period) {
        try {
            // Check authentication
            if (!AuthUtils.isAuthenticated() || !AuthUtils.isAdmin()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Akses ditolak");
                return ResponseEntity.status(403).body(errorResponse);
            }
            
            System.out.println("📈 Getting sales chart data for period: " + period);
            var chartData = dashboardService.getSalesChartData(period);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", chartData);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("❌ Error getting sales chart data: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Gagal mengambil data grafik penjualan: " + e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Endpoint untuk mengambil aktivitas terbaru
     */
    @GetMapping("/recent-activities")
    public ResponseEntity<Map<String, Object>> getRecentActivities(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            // Check authentication
            if (!AuthUtils.isAuthenticated() || !AuthUtils.isAdmin()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Akses ditolak");
                return ResponseEntity.status(403).body(errorResponse);
            }
            
            System.out.println("📋 Getting recent activities with limit: " + limit);
            var activities = dashboardService.getRecentActivities(limit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", activities);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("❌ Error getting recent activities: " + e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Gagal mengambil aktivitas terbaru: " + e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Endpoint untuk mengambil jumlah produk aktif
     */
    @GetMapping("/products/active-count")
    public ResponseEntity<Map<String, Object>> getActiveProductsCount() {
        try {
            // Check authentication
            if (!AuthUtils.isAuthenticated() || !AuthUtils.isAdmin()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Akses ditolak");
                return ResponseEntity.status(403).body(errorResponse);
            }
            
            System.out.println("📦 Getting active products count");
            
            // Count active products using existing repository method
            long activeProductsCount = productRepository.countByIsActiveTrue();
            
            Map<String, Object> data = new HashMap<>();
            data.put("activeProductsCount", activeProductsCount);
            data.put("calculatedAt", LocalDateTime.now());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", data);
            response.put("message", "Jumlah produk aktif berhasil diambil");
            
            System.out.println("✅ Active products count: " + activeProductsCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("❌ Error getting active products count: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Gagal mengambil jumlah produk aktif: " + e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Endpoint untuk mengambil jumlah customer aktif
     */
    @GetMapping("/customers/active-count")
    public ResponseEntity<Map<String, Object>> getActiveCustomersCount() {
        try {
            // Check authentication
            if (!AuthUtils.isAuthenticated() || !AuthUtils.isAdmin()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Akses ditolak");
                return ResponseEntity.status(403).body(errorResponse);
            }
            
            System.out.println("👥 Getting active customers count");
            
            // Count only users with role USER (exclude ADMIN) created in last 30 days
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            long activeCustomersCount = userRepository.countByWaktuBuatAfterAndRoleNot(thirtyDaysAgo, Role.ADMIN);
            
            // Jika tidak ada user baru dalam 30 hari, ambil total user kecuali admin
            if (activeCustomersCount == 0) {
                activeCustomersCount = userRepository.countByRoleNot(Role.ADMIN);
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("activeCustomersCount", activeCustomersCount);
            data.put("calculatedAt", LocalDateTime.now());
            data.put("periodDays", 30);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", data);
            response.put("message", "Jumlah customer aktif berhasil diambil");
            
            System.out.println("✅ Active customers count: " + activeCustomersCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("❌ Error getting active customers count: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Gagal mengambil jumlah customer aktif: " + e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Endpoint untuk mengambil pesanan terbaru
     */
    @GetMapping("/recent-orders")
    public ResponseEntity<Map<String, Object>> getRecentOrders(
            @RequestParam(defaultValue = "5") int limit) {
        try {
            // Check authentication
            if (!AuthUtils.isAuthenticated() || !AuthUtils.isAdmin()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Akses ditolak");
                return ResponseEntity.status(403).body(errorResponse);
            }
            
            System.out.println("📦 Getting recent orders with limit: " + limit);
            
            // Get recent orders from repository
            List<Order> recentOrders = orderRepository.findTop10ByOrderByCreatedAtDesc();
            
            // Limit the results
            List<Order> limitedOrders = recentOrders.stream()
                    .limit(limit)
                    .collect(Collectors.toList());
            
            // Convert to response format
            List<Map<String, Object>> ordersList = new ArrayList<>();
            for (Order order : limitedOrders) {
                Map<String, Object> orderData = new HashMap<>();
                orderData.put("orderNumber", order.getOrderNumber());
                orderData.put("customerName", order.getCustomerName() != null ? order.getCustomerName() : "Customer");
                orderData.put("totalAmount", order.getTotalAmount());
                orderData.put("formattedAmount", formatCurrency(order.getTotalAmount()));
                orderData.put("status", order.getStatus().toString());
                orderData.put("statusLabel", getStatusLabel(order.getStatus()));
                orderData.put("statusClass", getStatusClass(order.getStatus()));
                orderData.put("createdAt", order.getCreatedAt());
                orderData.put("timeAgo", formatTimeAgo(order.getCreatedAt()));
                
                ordersList.add(orderData);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", ordersList);
            response.put("message", "Pesanan terbaru berhasil diambil");
            response.put("totalCount", limitedOrders.size());
            
            System.out.println("✅ Recent orders retrieved: " + limitedOrders.size() + " orders");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("❌ Error getting recent orders: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Gagal mengambil pesanan terbaru: " + e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Helper method to format currency
     */
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "Rp 0";
        
        DecimalFormat formatter = new DecimalFormat("#,###");
        return "Rp " + formatter.format(amount);
    }
    
    /**
     * Helper method to format time ago
     */
    private String formatTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "-";
        
        LocalDateTime now = LocalDateTime.now();
        java.time.Duration duration = java.time.Duration.between(dateTime, now);
        
        long minutes = duration.toMinutes();
        if (minutes < 60) {
            return minutes + " menit yang lalu";
        }
        
        long hours = duration.toHours();
        if (hours < 24) {
            return hours + " jam yang lalu";
        }
        
        long days = duration.toDays();
        if (days < 7) {
            return days + " hari yang lalu";
        }
        
        return days / 7 + " minggu yang lalu";
    }
    
    /**
     * Helper method to get status label in Indonesian
     */
    private String getStatusLabel(OrderStatus status) {
        switch (status) {
            case PENDING_PAYMENT: return "Menunggu Pembayaran";
            case PENDING_CONFIRMATION: return "Menunggu Konfirmasi";
            case PAID: return "Dibayar";
            case PROCESSING: return "Diproses";
            case SHIPPED: return "Dikirim";
            case DELIVERED: return "Selesai";
            case CANCELLED: return "Dibatalkan";
            case REFUNDED: return "Dikembalikan";
            default: return status.toString();
        }
    }
    
    /**
     * Helper method to get status CSS class
     */
    private String getStatusClass(OrderStatus status) {
        switch (status) {
            case PENDING_PAYMENT: return "status-pending";
            case PENDING_CONFIRMATION: return "status-pending";
            case PAID: return "status-processing";
            case PROCESSING: return "status-processing";
            case SHIPPED: return "status-processing";
            case DELIVERED: return "status-delivered";
            case CANCELLED: return "status-cancelled";
            case REFUNDED: return "status-cancelled";
            default: return "status-pending";
        }
    }
}
