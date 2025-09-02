package com.projek.tokweb.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueResponseDto {
    
    private Long id;
    private Long orderId;
    private String orderNumber;
    private BigDecimal amount;
    private String formattedAmount;
    private LocalDate revenueDate;
    private LocalDateTime createdAt;
    private String confirmedBy;
    private String description;
    private String customerName;
    private String paymentMethod;
}