package com.projek.tokweb.controller.user;

import com.projek.tokweb.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;

import jakarta.servlet.http.HttpSession;
import java.util.Map;

@Controller
@RequestMapping("/user/order-notification")
public class OrderNotificationController {

    @Autowired
    private NotificationService notificationService;

    // Endpoint untuk membuat notifikasi pesanan
    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createOrderNotification(
            @RequestBody Map<String, Object> payload, 
            HttpSession session) {
        
        try {
            String userId = (String) session.getAttribute("userId");
            String orderId = (String) payload.get("orderId");
            String status = (String) payload.get("status");
            
            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.badRequest().body(new java.util.HashMap<String, Object>() {{
                    put("success", false);
                    put("message", "User tidak ditemukan");
                }});
            }
            
            if (orderId == null || orderId.isEmpty()) {
                return ResponseEntity.badRequest().body(new java.util.HashMap<String, Object>() {{
                    put("success", false);
                    put("message", "OrderId tidak valid");
                }});
            }
            
            if (status == null || status.isEmpty()) {
                return ResponseEntity.badRequest().body(new java.util.HashMap<String, Object>() {{
                    put("success", false);
                    put("message", "Status tidak valid");
                }});
            }
            
            // Buat notifikasi berdasarkan perubahan status pesanan
            notificationService.createOrderStatusNotification(userId, orderId, status);
            
            return ResponseEntity.ok(new java.util.HashMap<String, Object>() {{
                put("success", true);
                put("message", "Notifikasi berhasil dibuat");
            }});
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new java.util.HashMap<String, Object>() {{
                put("success", false);
                put("message", "Terjadi kesalahan: " + e.getMessage());
            }});
        }
    }
}