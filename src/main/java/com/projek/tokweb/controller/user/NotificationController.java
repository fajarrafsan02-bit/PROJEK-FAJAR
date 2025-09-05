package com.projek.tokweb.controller.user;

import com.projek.tokweb.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    // Endpoint untuk mendapatkan notifikasi pengguna
    @GetMapping("/get")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUserNotifications(HttpSession session) {
        // Dalam implementasi sebenarnya, Anda akan mengambil userId dari session
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            userId = "user-default"; // Untuk demo saja
        }
        
        List<Map<String, Object>> notifications = notificationService.getUserNotifications(userId);
        
        return ResponseEntity.ok(new java.util.HashMap<String, Object>() {{
            put("success", true);
            put("data", notifications);
        }});
    }

    // Endpoint untuk menandai notifikasi sebagai sudah dibaca
    @PostMapping("/mark-as-read")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markAsRead(@RequestBody Map<String, Object> payload, HttpSession session) {
        String notificationId = (String) payload.get("notificationId");
        if (notificationId != null) {
            notificationService.markAsRead(notificationId);
        }
        
        return ResponseEntity.ok(new java.util.HashMap<String, Object>() {{
            put("success", true);
            put("message", "Notifikasi berhasil ditandai sebagai sudah dibaca");
        }});
    }

    // Endpoint untuk menghapus notifikasi
    @DeleteMapping("/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteNotification(@RequestBody Map<String, Object> payload, HttpSession session) {
        String notificationId = (String) payload.get("notificationId");
        if (notificationId != null) {
            notificationService.deleteNotification(notificationId);
        }
        
        return ResponseEntity.ok(new java.util.HashMap<String, Object>() {{
            put("success", true);
            put("message", "Notifikasi berhasil dihapus");
        }});
    }
}