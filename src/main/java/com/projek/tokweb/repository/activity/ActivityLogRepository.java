package com.projek.tokweb.repository.activity;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.projek.tokweb.models.activity.ActivityLog;
import com.projek.tokweb.models.activity.ActivityType;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    
    // Find latest activities with pagination
    Page<ActivityLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    // Find activities by type
    List<ActivityLog> findByActivityTypeOrderByCreatedAtDesc(ActivityType activityType);
    
    // Find activities by entity type
    List<ActivityLog> findByEntityTypeOrderByCreatedAtDesc(String entityType);
    
    // Find activities by user
    List<ActivityLog> findByUserIdOrderByCreatedAtDesc(String userId);
    
    // Find activities by status
    List<ActivityLog> findByStatusOrderByCreatedAtDesc(String status);
    
    // Find activities within date range
    @Query("SELECT a FROM ActivityLog a WHERE a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    List<ActivityLog> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);
    
    // Find latest activities by activity type with limit
    @Query("SELECT a FROM ActivityLog a WHERE a.activityType = :activityType ORDER BY a.createdAt DESC")
    Page<ActivityLog> findLatestByActivityType(@Param("activityType") ActivityType activityType, Pageable pageable);
    
    // Find recent activities (last 24 hours)
    @Query("SELECT a FROM ActivityLog a WHERE a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<ActivityLog> findRecentActivities(@Param("since") LocalDateTime since);
    
    // Find activities by multiple activity types
    @Query("SELECT a FROM ActivityLog a WHERE a.activityType IN :activityTypes ORDER BY a.createdAt DESC")
    Page<ActivityLog> findByActivityTypeIn(@Param("activityTypes") List<ActivityType> activityTypes, Pageable pageable);
    
    // Count activities by status in last N hours
    @Query("SELECT COUNT(a) FROM ActivityLog a WHERE a.status = :status AND a.createdAt >= :since")
    Long countByStatusSince(@Param("status") String status, @Param("since") LocalDateTime since);
    
    // Find activities by entity id and type
    List<ActivityLog> findByEntityIdAndEntityTypeOrderByCreatedAtDesc(String entityId, String entityType);
    
    // Get latest 10 activities for dashboard
    @Query(value = "SELECT * FROM activity_logs ORDER BY created_at DESC LIMIT 10", nativeQuery = true)
    List<ActivityLog> findTop10ByOrderByCreatedAtDesc();
    
    // Delete old activities (cleanup)
    @Query("DELETE FROM ActivityLog a WHERE a.createdAt < :cutoffDate")
    void deleteOldActivities(@Param("cutoffDate") LocalDateTime cutoffDate);
}
