package com.projek.tokweb.controller.admin;

import com.projek.tokweb.dto.ApiResponse;
import com.projek.tokweb.dto.admin.RevenueResponseDto;
import com.projek.tokweb.dto.admin.RevenueStatisticsDto;
import com.projek.tokweb.service.admin.RevenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin/api/revenue")
@Tag(name = "Revenue Management", description = "API untuk mengelola data pendapatan")
@Slf4j
public class RevenueControllerApi {
    
    @Autowired
    private RevenueService revenueService;
    
    /**
     * Get today's revenue statistics
     */
    @GetMapping("/today")
    @Operation(summary = "Get Today Revenue", description = "Mendapatkan statistik pendapatan hari ini")
    public ResponseEntity<ApiResponse<RevenueStatisticsDto>> getTodayRevenue() {
        try {
            RevenueStatisticsDto statistics = revenueService.getTodayStatistics();
            return ResponseEntity.ok(ApiResponse.success("Statistik pendapatan hari ini berhasil diambil", statistics));
        } catch (Exception e) {
            log.error("Error getting today revenue: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Gagal mengambil statistik pendapatan hari ini: " + e.getMessage()));
        }
    }
    
    /**
     * Get current month revenue statistics
     */
    @GetMapping("/current-month")
    @Operation(summary = "Get Current Month Revenue", description = "Mendapatkan statistik pendapatan bulan ini")
    public ResponseEntity<ApiResponse<RevenueStatisticsDto>> getCurrentMonthRevenue() {
        try {
            RevenueStatisticsDto statistics = revenueService.getCurrentMonthStatistics();
            return ResponseEntity.ok(ApiResponse.success("Statistik pendapatan bulan ini berhasil diambil", statistics));
        } catch (Exception e) {
            log.error("Error getting current month revenue: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Gagal mengambil statistik pendapatan bulan ini: " + e.getMessage()));
        }
    }
    
    /**
     * Get monthly revenue statistics for specific month
     */
    @GetMapping("/monthly")
    @Operation(summary = "Get Monthly Revenue", description = "Mendapatkan statistik pendapatan bulanan")
    public ResponseEntity<ApiResponse<RevenueStatisticsDto>> getMonthlyRevenue(
            @Parameter(description = "Tahun") @RequestParam int year,
            @Parameter(description = "Bulan (1-12)") @RequestParam int month) {
        try {
            if (month < 1 || month > 12) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Bulan harus antara 1-12"));
            }
            
            RevenueStatisticsDto statistics = revenueService.getMonthlyStatistics(year, month);
            return ResponseEntity.ok(ApiResponse.success("Statistik pendapatan bulanan berhasil diambil", statistics));
        } catch (Exception e) {
            log.error("Error getting monthly revenue: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Gagal mengambil statistik pendapatan bulanan: " + e.getMessage()));
        }
    }
    
    /**
     * Get yearly revenue statistics
     */
    @GetMapping("/yearly")
    @Operation(summary = "Get Yearly Revenue", description = "Mendapatkan statistik pendapatan tahunan")
    public ResponseEntity<ApiResponse<RevenueStatisticsDto>> getYearlyRevenue(
            @Parameter(description = "Tahun") @RequestParam int year) {
        try {
            RevenueStatisticsDto statistics = revenueService.getYearlyStatistics(year);
            return ResponseEntity.ok(ApiResponse.success("Statistik pendapatan tahunan berhasil diambil", statistics));
        } catch (Exception e) {
            log.error("Error getting yearly revenue: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Gagal mengambil statistik pendapatan tahunan: " + e.getMessage()));
        }
    }
    
    /**
     * Get comprehensive revenue statistics (today, this month, this year)
     */
    @GetMapping("/comprehensive")
    @Operation(summary = "Get Comprehensive Revenue Statistics", description = "Mendapatkan statistik pendapatan komprehensif")
    public ResponseEntity<ApiResponse<RevenueStatisticsDto>> getComprehensiveStatistics() {
        try {
            RevenueStatisticsDto statistics = revenueService.getComprehensiveStatistics();
            return ResponseEntity.ok(ApiResponse.success("Statistik pendapatan komprehensif berhasil diambil", statistics));
        } catch (Exception e) {
            log.error("Error getting comprehensive statistics: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Gagal mengambil statistik pendapatan komprehensif: " + e.getMessage()));
        }
    }
    
    /**
     * Get revenue between specific dates
     */
    @GetMapping("/range")
    @Operation(summary = "Get Revenue Range", description = "Mendapatkan total pendapatan dalam rentang tanggal")
    public ResponseEntity<ApiResponse<BigDecimal>> getRevenueRange(
            @Parameter(description = "Tanggal mulai (YYYY-MM-DD)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Tanggal akhir (YYYY-MM-DD)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            if (startDate.isAfter(endDate)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Tanggal mulai tidak boleh lebih besar dari tanggal akhir"));
            }
            
            BigDecimal totalRevenue = revenueService.getRevenueBetweenDates(startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success("Total pendapatan berhasil diambil", totalRevenue));
        } catch (Exception e) {
            log.error("Error getting revenue range: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Gagal mengambil total pendapatan: " + e.getMessage()));
        }
    }
    
    /**
     * Get revenue records with pagination
     */
    @GetMapping("/records")
    @Operation(summary = "Get Revenue Records", description = "Mendapatkan daftar record pendapatan dengan pagination")
    public ResponseEntity<ApiResponse<Page<RevenueResponseDto>>> getRevenueRecords(
            @Parameter(description = "Tanggal mulai (YYYY-MM-DD)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Tanggal akhir (YYYY-MM-DD)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Halaman (mulai dari 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Ukuran halaman") @RequestParam(defaultValue = "10") int size) {
        try {
            // Default to current month if dates not provided
            if (startDate == null || endDate == null) {
                LocalDate now = LocalDate.now();
                startDate = now.withDayOfMonth(1);
                endDate = now.withDayOfMonth(now.lengthOfMonth());
            }
            
            if (startDate.isAfter(endDate)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Tanggal mulai tidak boleh lebih besar dari tanggal akhir"));
            }
            
            Pageable pageable = PageRequest.of(page, size);
            Page<RevenueResponseDto> records = revenueService.getRevenueRecords(startDate, endDate, pageable);
            
            return ResponseEntity.ok(ApiResponse.success("Daftar record pendapatan berhasil diambil", records));
        } catch (Exception e) {
            log.error("Error getting revenue records: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Gagal mengambil daftar record pendapatan: " + e.getMessage()));
        }
    }
    
    /**
     * Get top revenue days
     */
    @GetMapping("/top-days")
    @Operation(summary = "Get Top Revenue Days", description = "Mendapatkan hari-hari dengan pendapatan tertinggi")
    public ResponseEntity<ApiResponse<List<RevenueStatisticsDto.DailyRevenueDto>>> getTopRevenueDays(
            @Parameter(description = "Jumlah hari teratas") @RequestParam(defaultValue = "10") int limit) {
        try {
            if (limit <= 0 || limit > 100) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Limit harus antara 1-100"));
            }
            
            List<RevenueStatisticsDto.DailyRevenueDto> topDays = revenueService.getTopRevenueDays(limit);
            return ResponseEntity.ok(ApiResponse.success("Hari-hari dengan pendapatan tertinggi berhasil diambil", topDays));
        } catch (Exception e) {
            log.error("Error getting top revenue days: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Gagal mengambil hari-hari dengan pendapatan tertinggi: " + e.getMessage()));
        }
    }
    
    /**
     * Get dashboard summary (quick stats for admin dashboard)
     */
    @GetMapping("/dashboard-summary")
    @Operation(summary = "Get Dashboard Summary", description = "Mendapatkan ringkasan pendapatan untuk dashboard admin")
    public ResponseEntity<ApiResponse<RevenueStatisticsDto>> getDashboardSummary() {
        try {
            RevenueStatisticsDto summary = revenueService.getComprehensiveStatistics();
            
            // Add top revenue days to summary
            List<RevenueStatisticsDto.DailyRevenueDto> topDays = revenueService.getTopRevenueDays(5);
            summary.setDailyRevenues(topDays);
            
            return ResponseEntity.ok(ApiResponse.success("Ringkasan dashboard berhasil diambil", summary));
        } catch (Exception e) {
            log.error("Error getting dashboard summary: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Gagal mengambil ringkasan dashboard: " + e.getMessage()));
        }
    }
    
    /**
     * Get dashboard statistics with comparisons for admin home page
     */
    @GetMapping("/dashboard")
    @Operation(summary = "Get Dashboard Statistics", description = "Mendapatkan statistik dashboard dengan perbandingan")
    public ResponseEntity<ApiResponse<RevenueStatisticsDto>> getDashboardStatistics() {
        try {
            RevenueStatisticsDto statistics = revenueService.getDashboardStatistics();
            return ResponseEntity.ok(ApiResponse.success("Statistik dashboard berhasil diambil", statistics));
        } catch (Exception e) {
            log.error("Error getting dashboard statistics: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Gagal mengambil statistik dashboard: " + e.getMessage()));
        }
    }
}