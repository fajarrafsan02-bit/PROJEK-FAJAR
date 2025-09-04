package com.projek.tokweb.controller.admin;

import com.projek.tokweb.dto.admin.RevenueStatisticsDto;
import com.projek.tokweb.service.admin.RevenueService;
import com.projek.tokweb.service.admin.DashboardService;
import com.projek.tokweb.utils.AuthUtils;
import com.projek.tokweb.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/api/dashboard")
public class AdminDashboardController {
    
    @Autowired
    private RevenueService revenueService;
    
    @Autowired
    private DashboardService dashboardService;
    
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
}
