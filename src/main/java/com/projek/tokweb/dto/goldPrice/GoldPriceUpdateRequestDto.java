package com.projek.tokweb.dto.goldPrice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoldPriceUpdateRequestDto {
    private double harga24k;
    private double harga22k;
    private double harga18k;
    
    // Constructor untuk backward compatibility dengan frontend yang hanya mengirim harga24k
    public GoldPriceUpdateRequestDto(double harga24k) {
        this.harga24k = harga24k;
        this.harga22k = 0.0;
        this.harga18k = 0.0;
    }
}
