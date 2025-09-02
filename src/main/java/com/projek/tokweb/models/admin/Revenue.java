package com.projek.tokweb.models.admin;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "revenues")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Revenue {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    
    @Column(name = "order_number", nullable = false)
    private String orderNumber;
    
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "revenue_date", nullable = false)
    private LocalDate revenueDate;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "confirmed_by")
    private String confirmedBy;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "customer_name")
    private String customerName;
    
    @Column(name = "payment_method")
    private String paymentMethod;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (revenueDate == null) {
            revenueDate = LocalDate.now();
        }
    }
}