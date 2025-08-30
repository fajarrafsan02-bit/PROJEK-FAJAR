package com.projek.tokweb.models.goldPrice;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "gold_price_changes")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class GoldPriceChange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String purity; // 24k, 22k, 18k, 14k
    private double oldPrice;
    private double newPrice;
    private double changeAmount;
    private double changePercent;
    private String changeType; // INCREASE, DECREASE, STABLE
    private LocalDateTime changeDate;
    private String changeSource; // API, MANUAL, SYSTEM
    private String notes;
}
