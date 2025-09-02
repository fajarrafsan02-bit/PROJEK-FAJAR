package com.projek.tokweb.service.admin;

import com.projek.tokweb.dto.admin.RevenueResponseDto;
import com.projek.tokweb.dto.admin.RevenueStatisticsDto;
import com.projek.tokweb.models.admin.Revenue;
import com.projek.tokweb.models.customer.Order;
import com.projek.tokweb.repository.admin.RevenueRepository;
import com.projek.tokweb.utils.NumberFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RevenueService {
    
    @Autowired
    private RevenueRepository revenueRepository;
    
    /**
     * Record revenue when admin confirms an order
     */
    @Transactional
    public RevenueResponseDto recordRevenue(Order order, String confirmedBy) {
        try {
            // Check if revenue already exists for this order
            if (revenueRepository.existsByOrderId(order.getId())) {
                log.warn("Revenue already exists for order ID: {}", order.getId());
                return mapToDto(revenueRepository.findByOrderId(order.getId()).orElse(null));
            }
            
            Revenue revenue = Revenue.builder()
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .amount(order.getTotalAmount())
                    .revenueDate(LocalDate.now())
                    .confirmedBy(confirmedBy)
                    .description("Revenue from order confirmation")
                    .customerName(order.getCustomerName())
                    .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().toString() : "Unknown")
                    .build();
            
            Revenue savedRevenue = revenueRepository.save(revenue);
            log.info("Revenue recorded successfully for order: {} with amount: {}", 
                    order.getOrderNumber(), order.getTotalAmount());
            
            return mapToDto(savedRevenue);
            
        } catch (Exception e) {
            log.error("Error recording revenue for order: {}", order.getOrderNumber(), e);
            throw new RuntimeException("Failed to record revenue: " + e.getMessage());
        }
    }
    
    /**
     * Get today's revenue statistics
     */
    public RevenueStatisticsDto getTodayStatistics() {
        LocalDate today = LocalDate.now();
        
        BigDecimal todayRevenue = revenueRepository.getDailyRevenue(today);
        Long todayOrderCount = revenueRepository.countByRevenueDate(today);
        
        return RevenueStatisticsDto.builder()
                .todayRevenue(todayRevenue)
                .formattedTodayRevenue(NumberFormatter.formatCurrency(todayRevenue.doubleValue()))
                .todayOrderCount(todayOrderCount)
                .statisticsDate(today)
                .build();
    }
    
    /**
     * Get monthly revenue statistics
     */
    public RevenueStatisticsDto getMonthlyStatistics(int year, int month) {
        BigDecimal monthlyRevenue = revenueRepository.getMonthlyRevenue(year, month);
        Long monthlyOrderCount = revenueRepository.countByMonthAndYear(year, month);
        
        // Get daily revenues for the month
        List<Object[]> dailyData = revenueRepository.getDailyRevenueStatistics(year, month);
        List<RevenueStatisticsDto.DailyRevenueDto> dailyRevenues = dailyData.stream()
                .map(data -> RevenueStatisticsDto.DailyRevenueDto.builder()
                        .date((LocalDate) data[0])
                        .amount((BigDecimal) data[1])
                        .formattedAmount(NumberFormatter.formatCurrency(((BigDecimal) data[1]).doubleValue()))
                        .build())
                .collect(Collectors.toList());
        
        return RevenueStatisticsDto.builder()
                .monthlyRevenue(monthlyRevenue)
                .formattedMonthlyRevenue(NumberFormatter.formatCurrency(monthlyRevenue.doubleValue()))
                .monthlyOrderCount(monthlyOrderCount)
                .month(month)
                .year(year)
                .dailyRevenues(dailyRevenues)
                .build();
    }
    
    /**
     * Get current month statistics
     */
    public RevenueStatisticsDto getCurrentMonthStatistics() {
        LocalDate now = LocalDate.now();
        return getMonthlyStatistics(now.getYear(), now.getMonthValue());
    }
    
    /**
     * Get yearly revenue statistics
     */
    public RevenueStatisticsDto getYearlyStatistics(int year) {
        BigDecimal yearlyRevenue = revenueRepository.getYearlyRevenue(year);
        
        // Get monthly revenues for the year
        List<Object[]> monthlyData = revenueRepository.getMonthlyRevenueStatistics(year);
        List<RevenueStatisticsDto.MonthlyRevenueDto> monthlyRevenues = monthlyData.stream()
                .map(data -> {
                    int monthNum = (Integer) data[0];
                    BigDecimal amount = (BigDecimal) data[1];
                    String monthName = java.time.Month.of(monthNum)
                            .getDisplayName(TextStyle.FULL, new Locale("id", "ID"));
                    
                    return RevenueStatisticsDto.MonthlyRevenueDto.builder()
                            .month(monthNum)
                            .monthName(monthName)
                            .amount(amount)
                            .formattedAmount(NumberFormatter.formatCurrency(amount.doubleValue()))
                            .build();
                })
                .collect(Collectors.toList());
        
        return RevenueStatisticsDto.builder()
                .yearlyRevenue(yearlyRevenue)
                .formattedYearlyRevenue(NumberFormatter.formatCurrency(yearlyRevenue.doubleValue()))
                .year(year)
                .monthlyRevenues(monthlyRevenues)
                .build();
    }
    
    /**
     * Get comprehensive statistics (today, this month, this year)
     */
    public RevenueStatisticsDto getComprehensiveStatistics() {
        LocalDate now = LocalDate.now();
        
        BigDecimal todayRevenue = revenueRepository.getDailyRevenue(now);
        BigDecimal monthlyRevenue = revenueRepository.getMonthlyRevenue(now.getYear(), now.getMonthValue());
        BigDecimal yearlyRevenue = revenueRepository.getYearlyRevenue(now.getYear());
        
        Long todayOrderCount = revenueRepository.countByRevenueDate(now);
        Long monthlyOrderCount = revenueRepository.countByMonthAndYear(now.getYear(), now.getMonthValue());
        
        return RevenueStatisticsDto.builder()
                .todayRevenue(todayRevenue)
                .formattedTodayRevenue(NumberFormatter.formatCurrency(todayRevenue.doubleValue()))
                .monthlyRevenue(monthlyRevenue)
                .formattedMonthlyRevenue(NumberFormatter.formatCurrency(monthlyRevenue.doubleValue()))
                .yearlyRevenue(yearlyRevenue)
                .formattedYearlyRevenue(NumberFormatter.formatCurrency(yearlyRevenue.doubleValue()))
                .todayOrderCount(todayOrderCount)
                .monthlyOrderCount(monthlyOrderCount)
                .statisticsDate(now)
                .month(now.getMonthValue())
                .year(now.getYear())
                .build();
    }
    
    /**
     * Get revenue between dates
     */
    public BigDecimal getRevenueBetweenDates(LocalDate startDate, LocalDate endDate) {
        return revenueRepository.getRevenueBetweenDates(startDate, endDate);
    }
    
    /**
     * Get dashboard statistics with comparisons
     */
    public RevenueStatisticsDto getDashboardStatistics() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate thisMonthStart = today.withDayOfMonth(1);
        LocalDate lastMonthStart = thisMonthStart.minusMonths(1);
        LocalDate lastMonthEnd = thisMonthStart.minusDays(1);
        
        // Today vs Yesterday
        BigDecimal todayRevenue = revenueRepository.getDailyRevenue(today);
        BigDecimal yesterdayRevenue = revenueRepository.getDailyRevenue(yesterday);
        Long todayOrderCount = revenueRepository.countByRevenueDate(today);
        Long yesterdayOrderCount = revenueRepository.countByRevenueDate(yesterday);
        
        // This month vs Last month
        BigDecimal thisMonthRevenue = revenueRepository.getMonthlyRevenue(today.getYear(), today.getMonthValue());
        BigDecimal lastMonthRevenue = revenueRepository.getMonthlyRevenue(lastMonthStart.getYear(), lastMonthStart.getMonthValue());
        Long thisMonthOrderCount = revenueRepository.countByMonthAndYear(today.getYear(), today.getMonthValue());
        Long lastMonthOrderCount = revenueRepository.countByMonthAndYear(lastMonthStart.getYear(), lastMonthStart.getMonthValue());
        
        return RevenueStatisticsDto.builder()
                .todayRevenue(todayRevenue)
                .formattedTodayRevenue(NumberFormatter.formatCurrency(todayRevenue.doubleValue()))
                .monthlyRevenue(thisMonthRevenue)
                .formattedMonthlyRevenue(NumberFormatter.formatCurrency(thisMonthRevenue.doubleValue()))
                .todayOrderCount(todayOrderCount)
                .monthlyOrderCount(thisMonthOrderCount)
                .statisticsDate(today)
                .month(today.getMonthValue())
                .year(today.getYear())
                // Add comparison data
                .dailyRevenues(List.of(
                    RevenueStatisticsDto.DailyRevenueDto.builder()
                            .date(today)
                            .amount(todayRevenue)
                            .formattedAmount(NumberFormatter.formatCurrency(todayRevenue.doubleValue()))
                            .orderCount(todayOrderCount)
                            .build(),
                    RevenueStatisticsDto.DailyRevenueDto.builder()
                            .date(yesterday)
                            .amount(yesterdayRevenue)
                            .formattedAmount(NumberFormatter.formatCurrency(yesterdayRevenue.doubleValue()))
                            .orderCount(yesterdayOrderCount)
                            .build()
                ))
                .monthlyRevenues(List.of(
                    RevenueStatisticsDto.MonthlyRevenueDto.builder()
                            .month(today.getMonthValue())
                            .monthName(today.getMonth().getDisplayName(TextStyle.FULL, new Locale("id", "ID")))
                            .amount(thisMonthRevenue)
                            .formattedAmount(NumberFormatter.formatCurrency(thisMonthRevenue.doubleValue()))
                            .orderCount(thisMonthOrderCount)
                            .build(),
                    RevenueStatisticsDto.MonthlyRevenueDto.builder()
                            .month(lastMonthStart.getMonthValue())
                            .monthName(lastMonthStart.getMonth().getDisplayName(TextStyle.FULL, new Locale("id", "ID")))
                            .amount(lastMonthRevenue)
                            .formattedAmount(NumberFormatter.formatCurrency(lastMonthRevenue.doubleValue()))
                            .orderCount(lastMonthOrderCount)
                            .build()
                ))
                .build();
    }
    
    /**
     * Calculate percentage change between two values
     */
    public static double calculatePercentageChange(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current != null && current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        if (current == null) {
            return -100.0;
        }
        
        BigDecimal difference = current.subtract(previous);
        return difference.divide(previous, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }
    
    /**
     * Get revenue records with pagination
     */
    public Page<RevenueResponseDto> getRevenueRecords(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Page<Revenue> revenues = revenueRepository.findByRevenueDateBetween(startDate, endDate, pageable);
        return revenues.map(this::mapToDto);
    }
    
    /**
     * Get top revenue days
     */
    public List<RevenueStatisticsDto.DailyRevenueDto> getTopRevenueDays(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> topDays = revenueRepository.getTopRevenueDays(pageable);
        
        return topDays.stream()
                .map(data -> RevenueStatisticsDto.DailyRevenueDto.builder()
                        .date((LocalDate) data[0])
                        .amount((BigDecimal) data[1])
                        .formattedAmount(NumberFormatter.formatCurrency(((BigDecimal) data[1]).doubleValue()))
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * Map Revenue entity to DTO
     */
    private RevenueResponseDto mapToDto(Revenue revenue) {
        if (revenue == null) {
            return null;
        }
        
        return RevenueResponseDto.builder()
                .id(revenue.getId())
                .orderId(revenue.getOrderId())
                .orderNumber(revenue.getOrderNumber())
                .amount(revenue.getAmount())
                .formattedAmount(NumberFormatter.formatCurrency(revenue.getAmount().doubleValue()))
                .revenueDate(revenue.getRevenueDate())
                .createdAt(revenue.getCreatedAt())
                .confirmedBy(revenue.getConfirmedBy())
                .description(revenue.getDescription())
                .customerName(revenue.getCustomerName())
                .paymentMethod(revenue.getPaymentMethod())
                .build();
    }
}