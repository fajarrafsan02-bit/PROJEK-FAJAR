package com.projek.tokweb.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueStatisticsDto {
    
    private BigDecimal todayRevenue;
    private String formattedTodayRevenue;
    
    private BigDecimal monthlyRevenue;
    private String formattedMonthlyRevenue;
    
    private BigDecimal yearlyRevenue;
    private String formattedYearlyRevenue;
    
    private Long todayOrderCount;
    private Long monthlyOrderCount;
    
    private LocalDate statisticsDate;
    private int month;
    private int year;
    
    private List<DailyRevenueDto> dailyRevenues;
    private List<MonthlyRevenueDto> monthlyRevenues;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyRevenueDto {
        private LocalDate date;
        private BigDecimal amount;
        private String formattedAmount;
        private Long orderCount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRevenueDto {
        private int month;
        private String monthName;
        private BigDecimal amount;
        private String formattedAmount;
        private Long orderCount;
    }
}