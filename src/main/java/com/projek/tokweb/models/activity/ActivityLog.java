package com.projek.tokweb.models.activity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "activity_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private ActivityType activityType;
    
    @Column(name = "title", nullable = false)
    private String title;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;
    
    @Column(name = "user_id")
    private String userId;
    
    @Column(name = "user_name")
    private String userName;
    
    @Column(name = "user_role")
    private String userRole;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "status")
    private String status; // SUCCESS, ERROR, WARNING, INFO
    
    @Column(name = "entity_type")
    private String entityType; // GOLD_PRICE, PRODUCT, ORDER, USER, etc
    
    @Column(name = "entity_id")
    private String entityId;
    
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;
    
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;
    
    @Column(name = "change_amount")
    private Double changeAmount;
    
    @Column(name = "additional_data", columnDefinition = "TEXT")
    private String additionalData; // JSON atau informasi tambahan
}
