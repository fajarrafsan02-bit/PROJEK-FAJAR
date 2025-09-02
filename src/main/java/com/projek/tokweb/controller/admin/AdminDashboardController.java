package com.projek.tokweb.controller.admin;

import com.projek.tokweb.dto.admin.RevenueStatisticsDto;
import com.projek.tokweb.service.admin.RevenueService;
import com.projek.tokweb.utils.AuthUtils;
import com.projek.tokweb.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/api/dashboard")
public class AdminDashboardController {
    
    @Autowired
    private RevenueService revenueService;
    
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
}
