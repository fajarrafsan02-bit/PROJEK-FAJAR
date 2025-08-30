package com.projek.tokweb.dto.goldPrice;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.projek.tokweb.utils.NumberFormatter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoldPriceComparisonDto {
    private GoldPriceResponseDto today;
    private GoldPriceResponseDto yesterday;
    private String change24k;
    private String changePercent24k;
    private String trend;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime comparisonDate;
    
    public static GoldPriceComparisonDto fromComparison(Map<String, Object> comparison) {
        GoldPriceComparisonDto dto = new GoldPriceComparisonDto();
        
        if (comparison.get("today") != null) {
            dto.setToday(GoldPriceResponseDto.fromGoldPrice((com.projek.tokweb.models.goldPrice.GoldPrice) comparison.get("today")));
        }
        
        if (comparison.get("yesterday") != null) {
            dto.setYesterday(GoldPriceResponseDto.fromGoldPrice((com.projek.tokweb.models.goldPrice.GoldPrice) comparison.get("yesterday")));
        }
        
        if (comparison.get("change24k") != null) {
            double change = (Double) comparison.get("change24k");
            dto.setChange24k(NumberFormatter.formatCurrencyWithoutSymbol(change));
        }
        
        if (comparison.get("changePercent24k") != null) {
            double changePercent = (Double) comparison.get("changePercent24k");
            dto.setChangePercent24k(NumberFormatter.formatNumber(changePercent) + "%");
        }
        
        dto.setTrend((String) comparison.get("trend"));
        dto.setComparisonDate(LocalDateTime.now());
        
        return dto;
    }
}