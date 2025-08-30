package com.projek.tokweb.dto.goldPrice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.projek.tokweb.models.goldPrice.GoldPrice;
import com.projek.tokweb.models.goldPrice.GoldPriceEnum;
import com.projek.tokweb.utils.NumberFormatter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoldPriceResponseDto {
    private Long id;
    
    // Harga 24k
    private String hargaJual24k;
    private String hargaBeli24k;
    
    // Harga 22k
    private String hargaJual22k;
    private String hargaBeli22k;
    
    // Harga 18k
    private String hargaJual18k;
    private String hargaBeli18k;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime tanggalAmbil;

    private GoldPriceEnum goldPriceEnum;
    
    // Constructor dari GoldPrice entity
    public static GoldPriceResponseDto fromGoldPrice(GoldPrice goldPrice) {
        return GoldPriceResponseDto.builder()
                .id(goldPrice.getId())
                .hargaJual24k(NumberFormatter.formatCurrencyWithoutSymbolRounded(goldPrice.getHargaJual24k()))
                .hargaBeli24k(NumberFormatter.formatCurrencyWithoutSymbolRounded(goldPrice.getHargaBeli24k()))
                .hargaJual22k(NumberFormatter.formatCurrencyWithoutSymbolRounded(goldPrice.getHargaJual22k()))
                .hargaBeli22k(NumberFormatter.formatCurrencyWithoutSymbolRounded(goldPrice.getHargaBeli22k()))
                .hargaJual18k(NumberFormatter.formatCurrencyWithoutSymbolRounded(goldPrice.getHargaJual18k()))
                .hargaBeli18k(NumberFormatter.formatCurrencyWithoutSymbolRounded(goldPrice.getHargaBeli18k()))
                .tanggalAmbil(goldPrice.getTanggalAmbil())
                .goldPriceEnum(goldPrice.getGoldPriceEnum())
                .build();
    }
    
    /**
     * Mendapatkan semua harga dalam format Map
     */
    public Map<String, Map<String, String>> getAllPricesFormatted() {
        Map<String, Map<String, String>> prices = new HashMap<>();
        
        prices.put("24k", Map.of("jual", hargaJual24k, "beli", hargaBeli24k));
        prices.put("22k", Map.of("jual", hargaJual22k, "beli", hargaBeli22k));
        prices.put("18k", Map.of("jual", hargaJual18k, "beli", hargaBeli18k));
        
        return prices;
    }
}
