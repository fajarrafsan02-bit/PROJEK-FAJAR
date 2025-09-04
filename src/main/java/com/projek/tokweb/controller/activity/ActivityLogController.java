package com.projek.tokweb.controller.activity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.projek.tokweb.models.activity.ActivityLog;
import com.projek.tokweb.models.activity.ActivityType;
import com.projek.tokweb.service.activity.ActivityLogService;
import com.projek.tokweb.service.activity.ActivityLogService.ActivityStats;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
@Slf4j
public class ActivityLogController {
    
    private final ActivityLogService activityLogService;
    
    /**
     * Get latest activities for dashboard/home page
     */
    @GetMapping("/latest")
    public ResponseEntity<Map<String, Object>> getLatestActivities(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            log.info(">> Getting latest {} activities", limit);
            
            List<ActivityLog> activities = activityLogService.getLatestActivities(limit);
            
            // Transform data untuk frontend
            List<Map<String, Object>> activitiesData = activities.stream()
                .map(this::transformActivityForFrontend)
                .toList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Latest activities retrieved successfully");
            response.put("data", activitiesData);
            response.put("count", activitiesData.size());
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error(">> Error getting latest activities: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get latest activities: " + e.getMessage());
            errorResponse.put("data", List.of());
            errorResponse.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    /**
     * Get recent activities (last 24 hours)
     */
    @GetMapping("/recent")
    public ResponseEntity<Map<String, Object>> getRecentActivities() {
        try {
            log.info(">> Getting recent activities (last 24 hours)");
            
            List<ActivityLog> activities = activityLogService.getRecentActivities();
            
            // Transform data untuk frontend
            List<Map<String, Object>> activitiesData = activities.stream()
                .map(this::transformActivityForFrontend)
                .toList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Recent activities retrieved successfully");
            response.put("data", activitiesData);
            response.put("count", activitiesData.size());
            response.put("period", "24 hours");
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error(">> Error getting recent activities: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get recent activities: " + e.getMessage());
            errorResponse.put("data", List.of());
            errorResponse.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    /**
     * Get activities with pagination
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String activityType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String entityType) {
        try {
            log.info(">> Getting activities with pagination - page: {}, size: {}", page, size);
            
            // TODO: Implementasi filter berdasarkan parameters
            List<ActivityLog> activities = activityLogService.getLatestActivities(size);
            
            // Transform data untuk frontend
            List<Map<String, Object>> activitiesData = activities.stream()
                .map(this::transformActivityForFrontend)
                .toList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Activities retrieved successfully");
            response.put("data", activitiesData);
            response.put("page", page);
            response.put("size", size);
            response.put("totalElements", activitiesData.size());
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error(">> Error getting activities: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get activities: " + e.getMessage());
            errorResponse.put("data", List.of());
            errorResponse.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    /**
     * Get activity statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getActivityStats(
            @RequestParam(defaultValue = "24") int hours) {
        try {
            log.info(">> Getting activity stats for last {} hours", hours);
            
            ActivityStats stats = activityLogService.getActivityStats(hours);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Activity stats retrieved successfully");
            response.put("data", stats);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error(">> Error getting activity stats: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get activity stats: " + e.getMessage());
            errorResponse.put("data", null);
            errorResponse.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    /**
     * Get activities by type
     */
    @GetMapping("/by-type")
    public ResponseEntity<Map<String, Object>> getActivitiesByType(
            @RequestParam String activityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            log.info(">> Getting activities by type: {}", activityType);
            
            ActivityType type = ActivityType.valueOf(activityType.toUpperCase());
            Page<ActivityLog> activitiesPage = activityLogService.getActivitiesByType(type, page, size);
            
            // Transform data untuk frontend
            List<Map<String, Object>> activitiesData = activitiesPage.getContent().stream()
                .map(this::transformActivityForFrontend)
                .toList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Activities by type retrieved successfully");
            response.put("data", activitiesData);
            response.put("page", page);
            response.put("size", size);
            response.put("totalElements", activitiesPage.getTotalElements());
            response.put("totalPages", activitiesPage.getTotalPages());
            response.put("activityType", activityType);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error(">> Error getting activities by type: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get activities by type: " + e.getMessage());
            errorResponse.put("data", List.of());
            errorResponse.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    /**
     * Transform ActivityLog entity to frontend-friendly format
     */
    private Map<String, Object> transformActivityForFrontend(ActivityLog activity) {
        Map<String, Object> data = new HashMap<>();
        
        data.put("id", activity.getId());
        data.put("activityType", activity.getActivityType().name());
        data.put("activityTypeDescription", activity.getActivityType().getDescription());
        data.put("title", activity.getTitle());
        data.put("description", activity.getDescription());
        data.put("details", activity.getDetails());
        data.put("status", activity.getStatus());
        data.put("entityType", activity.getEntityType());
        data.put("entityId", activity.getEntityId());
        data.put("userId", activity.getUserId());
        data.put("userName", activity.getUserName());
        data.put("userRole", activity.getUserRole());
        data.put("createdAt", activity.getCreatedAt());
        data.put("createdAtFormatted", activity.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));
        data.put("oldValue", activity.getOldValue());
        data.put("newValue", activity.getNewValue());
        data.put("changeAmount", activity.getChangeAmount());
        data.put("additionalData", activity.getAdditionalData());
        
        // Calculate time ago
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdAt = activity.getCreatedAt();
        long minutesAgo = java.time.Duration.between(createdAt, now).toMinutes();
        
        String timeAgo;
        if (minutesAgo < 1) {
            timeAgo = "Baru saja";
        } else if (minutesAgo < 60) {
            timeAgo = minutesAgo + " menit yang lalu";
        } else {
            long hoursAgo = minutesAgo / 60;
            if (hoursAgo < 24) {
                timeAgo = hoursAgo + " jam yang lalu";
            } else {
                long daysAgo = hoursAgo / 24;
                timeAgo = daysAgo + " hari yang lalu";
            }
        }
        data.put("timeAgo", timeAgo);
        
        // Add icon based on activity type
        data.put("icon", getIconForActivityType(activity.getActivityType()));
        data.put("color", getColorForStatus(activity.getStatus()));
        
        return data;
    }
    
    /**
     * Get icon for activity type
     */
    private String getIconForActivityType(ActivityType activityType) {
        switch (activityType) {
            case GOLD_PRICE_UPDATE_API:
            case GOLD_PRICE_UPDATE_MANUAL:
                return "fas fa-chart-line";
            case GOLD_PRICE_FETCH_EXTERNAL:
                return "fas fa-download";
            case GOLD_PRICE_NO_CHANGE:
                return "fas fa-equals";
            case PRODUCT_CREATE:
            case PRODUCT_UPDATE:
            case PRODUCT_DELETE:
                return "fas fa-gem";
            case ORDER_CREATE:
            case ORDER_UPDATE:
            case ORDER_PAYMENT:
                return "fas fa-shopping-cart";
            case USER_LOGIN:
            case USER_LOGOUT:
            case ADMIN_LOGIN:
            case ADMIN_LOGOUT:
                return "fas fa-user";
            case SYSTEM_START:
            case SYSTEM_ERROR:
            case SYSTEM_MAINTENANCE:
                return "fas fa-cog";
            default:
                return "fas fa-info-circle";
        }
    }
    
    /**
     * Get color for status
     */
    private String getColorForStatus(String status) {
        if (status == null) return "info";
        
        switch (status.toUpperCase()) {
            case "SUCCESS":
                return "success";
            case "ERROR":
                return "error";
            case "WARNING":
                return "warning";
            case "INFO":
            default:
                return "info";
        }
    }
}
