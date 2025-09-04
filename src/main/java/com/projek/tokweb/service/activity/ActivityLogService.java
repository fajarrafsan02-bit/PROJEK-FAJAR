package com.projek.tokweb.service.activity;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projek.tokweb.models.activity.ActivityLog;
import com.projek.tokweb.models.activity.ActivityType;
import com.projek.tokweb.repository.activity.ActivityLogRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogService {
    
    private final ActivityLogRepository activityLogRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Log aktivitas sistem dengan informasi lengkap
     */
    @Transactional
    public ActivityLog logActivity(ActivityType activityType, String title, String description) {
        return logActivity(activityType, title, description, null, null, null, null, null, null, "INFO", null, null, null, null, null, null);
    }
    
    /**
     * Log aktivitas dengan user info
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public ActivityLog logActivity(ActivityType activityType, String title, String description,
                                 String userId, String userName, String userRole) {
        return logActivity(activityType, title, description, null, userId, userName, userRole, null, null, "INFO", null, null, null, null, null, null);
    }
    
    /**
     * Log aktivitas dengan perubahan data
     */
    @Transactional
    public ActivityLog logActivity(ActivityType activityType, String title, String description,
                                 String entityType, String entityId, Object oldValue, Object newValue) {
        String oldValueStr = null;
        String newValueStr = null;
        Double changeAmount = null;
        
        try {
            if (oldValue != null) {
                oldValueStr = oldValue instanceof String ? (String) oldValue : objectMapper.writeValueAsString(oldValue);
            }
            if (newValue != null) {
                newValueStr = newValue instanceof String ? (String) newValue : objectMapper.writeValueAsString(newValue);
            }
            
            // Hitung change amount jika old dan new value adalah number
            if (oldValue instanceof Number && newValue instanceof Number) {
                changeAmount = ((Number) newValue).doubleValue() - ((Number) oldValue).doubleValue();
            }
        } catch (Exception e) {
            log.warn("Error serializing values for activity log: {}", e.getMessage());
        }
        
        return logActivity(activityType, title, description, null, null, null, null, null, null, 
                         "SUCCESS", entityType, entityId, oldValueStr, newValueStr, changeAmount, null);
    }
    
    /**
     * Log aktivitas lengkap dengan semua parameter
     */
    @Transactional
    public ActivityLog logActivity(ActivityType activityType, String title, String description, String details,
                                 String userId, String userName, String userRole, String ipAddress, String userAgent,
                                 String status, String entityType, String entityId, String oldValue, String newValue,
                                 Double changeAmount, String additionalData) {
        try {
            ActivityLog activityLog = ActivityLog.builder()
                    .activityType(activityType)
                    .title(title)
                    .description(description)
                    .details(details)
                    .userId(userId)
                    .userName(userName)
                    .userRole(userRole)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .createdAt(LocalDateTime.now())
                    .status(status != null ? status : "INFO")
                    .entityType(entityType)
                    .entityId(entityId)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .changeAmount(changeAmount)
                    .additionalData(additionalData)
                    .build();
            
            ActivityLog saved = activityLogRepository.save(activityLog);
            log.info(">> ActivityLog saved: {} - {}", activityType, title);
            return saved;
            
        } catch (Exception e) {
            log.error(">> Error saving activity log: {}", e.getMessage(), e);
            // Jangan throw exception agar tidak mengganggu proses utama
            return null;
        }
    }
    
    /**
     * Log aktivitas Gold Price dengan detail perubahan harga
     */
    @Transactional
    public ActivityLog logGoldPriceActivity(ActivityType activityType, String title, String description,
                                          String userId, String userName, String purity, 
                                          Double oldPrice, Double newPrice, String source) {
        try {
            String details = String.format("Purity: %s, Old Price: %s, New Price: %s, Source: %s", 
                                          purity, 
                                          oldPrice != null ? String.format("Rp %.0f", oldPrice) : "N/A",
                                          newPrice != null ? String.format("Rp %.0f", newPrice) : "N/A",
                                          source);
            
            Double changeAmount = (oldPrice != null && newPrice != null) ? (newPrice - oldPrice) : null;
            String status = "SUCCESS";
            
            // Tentukan status berdasarkan jenis aktivitas
            if (activityType == ActivityType.GOLD_PRICE_NO_CHANGE) {
                status = "INFO";
            } else if (changeAmount != null && changeAmount != 0) {
                status = "SUCCESS";
            }
            
            return logActivity(activityType, title, description, details, userId, userName, "ADMIN", 
                             null, null, status, "GOLD_PRICE", purity, 
                             oldPrice != null ? oldPrice.toString() : null,
                             newPrice != null ? newPrice.toString() : null,
                             changeAmount, source);
            
        } catch (Exception e) {
            log.error(">> Error logging gold price activity: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Log aktivitas dengan request info dari HttpServletRequest
     */
    @Transactional
    public ActivityLog logActivityWithRequest(ActivityType activityType, String title, String description,
                                            String userId, String userName, String userRole,
                                            HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        String userAgent = request != null ? request.getHeader("User-Agent") : null;
        
        return logActivity(activityType, title, description, null, userId, userName, userRole, 
                         ipAddress, userAgent, "SUCCESS", null, null, null, null, null, null);
    }
    
    /**
     * Ambil aktivitas terbaru untuk dashboard
     */
    public List<ActivityLog> getLatestActivities(int limit) {
        try {
            if (limit <= 10) {
                return activityLogRepository.findTop10ByOrderByCreatedAtDesc();
            } else {
                Pageable pageable = PageRequest.of(0, limit);
                Page<ActivityLog> page = activityLogRepository.findAllByOrderByCreatedAtDesc(pageable);
                return page.getContent();
            }
        } catch (Exception e) {
            log.error(">> Error getting latest activities: {}", e.getMessage(), e);
            return List.of(); // Return empty list instead of null
        }
    }
    
    /**
     * Ambil aktivitas berdasarkan tipe dengan pagination
     */
    public Page<ActivityLog> getActivitiesByType(ActivityType activityType, int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            return activityLogRepository.findLatestByActivityType(activityType, pageable);
        } catch (Exception e) {
            log.error(">> Error getting activities by type: {}", e.getMessage(), e);
            return Page.empty();
        }
    }
    
    /**
     * Ambil aktivitas dalam rentang waktu tertentu
     */
    public List<ActivityLog> getActivitiesInDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            return activityLogRepository.findByCreatedAtBetween(startDate, endDate);
        } catch (Exception e) {
            log.error(">> Error getting activities in date range: {}", e.getMessage(), e);
            return List.of();
        }
    }
    
    /**
     * Ambil aktivitas terbaru dalam 24 jam terakhir
     */
    public List<ActivityLog> getRecentActivities() {
        try {
            LocalDateTime since = LocalDateTime.now().minusHours(24);
            return activityLogRepository.findRecentActivities(since);
        } catch (Exception e) {
            log.error(">> Error getting recent activities: {}", e.getMessage(), e);
            return List.of();
        }
    }
    
    /**
     * Ambil statistik aktivitas dalam N jam terakhir
     */
    public ActivityStats getActivityStats(int hours) {
        try {
            LocalDateTime since = LocalDateTime.now().minusHours(hours);
            
            Long successCount = activityLogRepository.countByStatusSince("SUCCESS", since);
            Long errorCount = activityLogRepository.countByStatusSince("ERROR", since);
            Long warningCount = activityLogRepository.countByStatusSince("WARNING", since);
            Long infoCount = activityLogRepository.countByStatusSince("INFO", since);
            
            return ActivityStats.builder()
                    .successCount(successCount != null ? successCount : 0)
                    .errorCount(errorCount != null ? errorCount : 0)
                    .warningCount(warningCount != null ? warningCount : 0)
                    .infoCount(infoCount != null ? infoCount : 0)
                    .totalCount((successCount != null ? successCount : 0) + 
                              (errorCount != null ? errorCount : 0) + 
                              (warningCount != null ? warningCount : 0) + 
                              (infoCount != null ? infoCount : 0))
                    .periodHours(hours)
                    .build();
            
        } catch (Exception e) {
            log.error(">> Error getting activity stats: {}", e.getMessage(), e);
            return ActivityStats.builder()
                    .successCount(0L)
                    .errorCount(0L)
                    .warningCount(0L)
                    .infoCount(0L)
                    .totalCount(0L)
                    .periodHours(hours)
                    .build();
        }
    }
    
    /**
     * Cleanup aktivitas lama (lebih dari N hari)
     */
    @Transactional
    public void cleanupOldActivities(int daysToKeep) {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
            activityLogRepository.deleteOldActivities(cutoffDate);
            log.info(">> Cleanup completed: deleted activities older than {} days", daysToKeep);
        } catch (Exception e) {
            log.error(">> Error during cleanup: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Ambil IP address dari request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) return null;
        
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Helper class untuk statistik aktivitas
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ActivityStats {
        private Long successCount;
        private Long errorCount;
        private Long warningCount;
        private Long infoCount;
        private Long totalCount;
        private Integer periodHours;
    }
}
