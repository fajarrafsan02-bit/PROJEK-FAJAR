package com.projek.tokweb.models.goldPrice;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class GoldPrice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double hargaJual24k;
    private double hargaBeli24k;
    private double hargaJual22k;
    private double hargaBeli22k;
    private double hargaJual18k;
    private double hargaBeli18k;

    @Builder.Default
    private LocalDateTime tanggalAmbil = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private GoldPriceEnum goldPriceEnum;
}
