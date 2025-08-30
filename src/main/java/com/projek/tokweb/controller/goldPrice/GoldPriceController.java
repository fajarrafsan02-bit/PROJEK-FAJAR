package com.projek.tokweb.controller.goldPrice;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.projek.tokweb.dto.ApiResponse;
import com.projek.tokweb.dto.goldPrice.GoldPriceComparisonDto;
import com.projek.tokweb.dto.goldPrice.GoldPriceResponseDto;
import com.projek.tokweb.dto.goldPrice.GoldPriceUpdateRequestDto;
import com.projek.tokweb.models.goldPrice.GoldPrice;
import com.projek.tokweb.models.goldPrice.GoldPriceChange;
import com.projek.tokweb.service.admin.ProductService;
import com.projek.tokweb.service.goldprice.GoldPriceService;
import com.projek.tokweb.utils.NumberFormatter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/gold-price")
@Slf4j
public class GoldPriceController {
    @Autowired
    private GoldPriceService goldPriceService;
    @Autowired
    private ProductService productService;

    @Operation(summary = "Update Harga Emas", description = "Mengupdate harga emas dari API eksternal dengan update otomatis harga produk")
    @PostMapping("/update")
    public ResponseEntity<ApiResponse<GoldPriceResponseDto>> updateGoldPrice(@RequestBody GoldPriceUpdateRequestDto request) {
        try {
            log.info(">> Controller: Memulai update harga emas dari request: {}", request);
            
            // Debug: log request body
            if (request == null) {
                log.error(">> Controller: Request body null");
                return ResponseEntity.badRequest()
                    .body(ApiResponse.<GoldPriceResponseDto>builder()
                        .success(false)
                        .message("Request body tidak boleh kosong")
                        .build());
            }
            
            log.info(">> Controller: Request harga24k: {}", request.getHarga24k());
            
            // Validasi input
            if (request.getHarga24k() <= 0) {
                log.error(">> Controller: Harga 24K tidak valid: {}", request.getHarga24k());
                return ResponseEntity.badRequest()
                    .body(ApiResponse.<GoldPriceResponseDto>builder()
                        .success(false)
                        .message("Harga 24K harus lebih dari 0")
                        .build());
            }
            
            // Update harga emas
            GoldPrice updatedGoldPrice = goldPriceService.updateGoldPriceFromRequest(request.getHarga24k());
            
            GoldPriceResponseDto response = GoldPriceResponseDto.fromGoldPrice(updatedGoldPrice);
            
            log.info(">> Controller: Update harga emas berhasil: {}", response);
            
            return ResponseEntity.ok(ApiResponse.<GoldPriceResponseDto>builder()
                .success(true)
                .message("Harga emas berhasil diupdate")
                .data(response)
                .build());
                
        } catch (Exception e) {
            log.error(">> Controller: Error dalam update harga emas: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<GoldPriceResponseDto>builder()
                    .success(false)
                    .message("Gagal mengupdate harga emas: " + e.getMessage())
                    .build());
        }
    }

    @Operation(summary = "Get Harga Emas Terbaru", description = "Mendapatkan harga emas terbaru")
    @GetMapping("/latest")
    public ResponseEntity<?> getLatestGoldPrice() {
        try {
            GoldPrice latestPrice = goldPriceService.getLatestGoldPrice();
            GoldPriceResponseDto responseDto = GoldPriceResponseDto.fromGoldPrice(latestPrice);

            Map<String, Object> response = new HashMap<>();
            response.put("data", responseDto);
            response.put("timestamp", latestPrice.getTanggalAmbil());

            return ResponseEntity.ok(ApiResponse.success("Harga emas terbaru berhasil diambil", response));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Harga emas belum tersedia: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get Harga Emas by Purity", description = "Mendapatkan harga emas berdasarkan kadar kemurnian")
    @GetMapping("/price/{purity}")
    public ResponseEntity<?> getGoldPriceByPurity(
            @Parameter(description = "Kadar kemurnian (10, 14, 16, 18, 21, 22, 24)", example = "18") @PathVariable int purity) {
        try {
            double hargaJual = goldPriceService.getLatestHargaJual(purity);
            double hargaBeli = goldPriceService.getLatestHargaBeli(purity);

            Map<String, Object> response = new HashMap<>();
            response.put("purity", purity + "k");
            response.put("hargaJual", NumberFormatter.formatCurrencyWithoutSymbol(hargaJual));
            response.put("hargaBeli", NumberFormatter.formatCurrencyWithoutSymbol(hargaBeli));
            response.put("timestamp", goldPriceService.getLatestGoldPrice().getTanggalAmbil());

            return ResponseEntity.ok(ApiResponse.success("Harga emas " + purity + "k berhasil diambil", response));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Gagal mengambil harga emas: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get Riwayat Harga Emas", description = "Mendapatkan riwayat harga emas dengan pagination")
    @GetMapping("/history")
    public ResponseEntity<?> getGoldPriceHistory(
            @Parameter(description = "Nomor halaman (dimulai dari 0)", example = "0") @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Jumlah item per halaman", example = "10") @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Field untuk sorting", example = "tanggalAmbil") @RequestParam(defaultValue = "tanggalAmbil") String sortBy,

            @Parameter(description = "Arah sorting (asc/desc)", example = "desc") @RequestParam(defaultValue = "desc") String sortDirection) {
        try {
            if (page < 0) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Nomor halaman tidak boleh negatif"));
            }
            if (size <= 0 || size > 100) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Ukuran halaman harus antara 1-100"));
            }
            if (!sortDirection.equalsIgnoreCase("asc") && !sortDirection.equalsIgnoreCase("desc")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Arah sorting harus 'asc' atau 'desc'"));
            }

            Page<GoldPrice> pricePage = goldPriceService.getGoldPriceHistory(page, size, sortBy, sortDirection);
            Page<GoldPriceResponseDto> responsePage = pricePage.map(GoldPriceResponseDto::fromGoldPrice);

            Map<String, Object> response = new HashMap<>();
            response.put("content", responsePage.getContent());
            response.put("currentPage", responsePage.getNumber());
            response.put("totalPages", responsePage.getTotalPages());
            response.put("totalItems", responsePage.getTotalElements());
            response.put("size", responsePage.getSize());
            response.put("hasNext", responsePage.hasNext());
            response.put("hasPrevious", responsePage.hasPrevious());

            return ResponseEntity.ok(ApiResponse.success("Riwayat harga emas berhasil diambil", response));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Gagal mengambil riwayat harga emas: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get Semua Harga Emas", description = "Mendapatkan semua harga emas untuk berbagai kadar kemurnian")
    @GetMapping("/all-prices")
    public ResponseEntity<?> getAllGoldPrices() {
        try {
            GoldPrice latestPrice = goldPriceService.getLatestGoldPrice();
            GoldPriceResponseDto responseDto = GoldPriceResponseDto.fromGoldPrice(latestPrice);

            Map<String, Object> prices = new HashMap<>();
            prices.put("24k", Map.of("jual", responseDto.getHargaJual24k(), "beli", responseDto.getHargaBeli24k()));
            prices.put("22k", Map.of("jual", responseDto.getHargaJual22k(), "beli", responseDto.getHargaBeli22k()));
            prices.put("18k", Map.of("jual", responseDto.getHargaJual18k(), "beli", responseDto.getHargaBeli18k()));

            Map<String, Object> response = new HashMap<>();
            response.put("prices", prices);
            response.put("timestamp", latestPrice.getTanggalAmbil());

            return ResponseEntity.ok(ApiResponse.success("Semua harga emas berhasil diambil", response));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Harga emas belum tersedia: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get Harga Emas by Date Range", description = "Mendapatkan harga emas dalam rentang tanggal tertentu")
    @GetMapping("/by-date")
    public ResponseEntity<?> getGoldPriceByDateRange(
            @Parameter(description = "Tanggal mulai (format: yyyy-MM-dd)", example = "2024-01-01") @RequestParam String startDate,

            @Parameter(description = "Tanggal akhir (format: yyyy-MM-dd)", example = "2024-01-31") @RequestParam String endDate,

            @Parameter(description = "Nomor halaman", example = "0") @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Jumlah item per halaman", example = "10") @RequestParam(defaultValue = "10") int size) {
        try {
            List<GoldPrice> prices = goldPriceService.getGoldPriceByDateRange(startDate, endDate, page, size);

            List<GoldPriceResponseDto> responseDtos = prices.stream()
                    .map(GoldPriceResponseDto::fromGoldPrice)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("data", responseDtos);
            response.put("startDate", startDate);
            response.put("endDate", endDate);
            response.put("totalItems", prices.size());

            return ResponseEntity
                    .ok(ApiResponse.success("Harga emas dalam rentang tanggal berhasil diambil", response));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Gagal mengambil harga emas: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get Harga Emas Hari Ini", description = "Mendapatkan harga emas hari ini")
    @GetMapping("/today")
    public ResponseEntity<?> getTodayGoldPrice() {
        try {
            GoldPrice todayPrice = goldPriceService.getTodayGoldPrice();

            if (todayPrice == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Harga emas hari ini belum tersedia"));
            }

            GoldPriceResponseDto responseDto = GoldPriceResponseDto.fromGoldPrice(todayPrice);

            Map<String, Object> response = new HashMap<>();
            response.put("data", responseDto);
            response.put("message", "Harga emas hari ini berhasil diambil");
            response.put("timestamp", todayPrice.getTanggalAmbil());

            return ResponseEntity.ok(ApiResponse.success("Harga emas hari ini berhasil diambil", response));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Gagal mengambil harga emas hari ini: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get Harga Emas Kemarin", description = "Mendapatkan harga emas kemarin")
    @GetMapping("/yesterday")
    public ResponseEntity<?> getYesterdayGoldPrice() {
        try {
            GoldPrice yesterdayPrice = goldPriceService.getYesterdayGoldPrice();

            if (yesterdayPrice == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Harga emas kemarin tidak tersedia"));
            }

            GoldPriceResponseDto responseDto = GoldPriceResponseDto.fromGoldPrice(yesterdayPrice);

            Map<String, Object> response = new HashMap<>();
            response.put("data", responseDto);
            response.put("message", "Harga emas kemarin berhasil diambil");
            response.put("timestamp", yesterdayPrice.getTanggalAmbil());

            return ResponseEntity.ok(ApiResponse.success("Harga emas kemarin berhasil diambil", response));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Gagal mengambil harga emas kemarin: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get Perbandingan Harga Emas", description = "Mendapatkan perbandingan harga emas hari ini vs kemarin")
    @GetMapping("/comparison")
    public ResponseEntity<?> getPriceComparison() {
        try {
            Map<String, Object> comparison = goldPriceService.getPriceComparison();

            if (comparison.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Data perbandingan harga emas tidak tersedia"));
            }

            GoldPriceComparisonDto responseDto = GoldPriceComparisonDto.fromComparison(comparison);

            return ResponseEntity.ok(ApiResponse.success("Perbandingan harga emas berhasil diambil", responseDto));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Gagal mengambil perbandingan harga emas: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get Trend Harga Emas", description = "Mendapatkan trend harga emas (naik/turun/stabil)")
    @GetMapping("/trend")
    public ResponseEntity<?> getGoldPriceTrend() {
        try {
            Map<String, Object> comparison = goldPriceService.getPriceComparison();

            if (comparison.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Data trend harga emas tidak tersedia"));
            }

            GoldPriceComparisonDto responseDto = GoldPriceComparisonDto.fromComparison(comparison);

            Map<String, Object> trendResponse = new HashMap<>();
            trendResponse.put("trend", responseDto.getTrend());
            trendResponse.put("change24k", responseDto.getChange24k());
            trendResponse.put("changePercent24k", responseDto.getChangePercent24k());
            trendResponse.put("today", responseDto.getToday());
            trendResponse.put("yesterday", responseDto.getYesterday());

            return ResponseEntity.ok(ApiResponse.success("Trend harga emas berhasil diambil", trendResponse));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Gagal mengambil trend harga emas: " + e.getMessage()));
        }
    }

    @Operation(summary = "Update Harga Emas by Purity", description = "Update harga emas berdasarkan karat/purity dan otomatis update harga produk")
    @PostMapping("/update/{purity}")
    public ResponseEntity<?> updateGoldPriceByPurity(
            @Parameter(description = "Kadar kemurnian (10, 14, 16, 18, 21, 22, 24)", example = "18") @PathVariable int purity) {
        try {
            System.out.println(">> Admin Controller : Memulai update harga emas untuk " + purity + "k");

            // Update harga emas dari API eksternal
            GoldPrice updatedPrice = goldPriceService.fetchAndSaveLatestPrice();
            GoldPriceResponseDto responseDto = GoldPriceResponseDto.fromGoldPrice(updatedPrice);

            // Update harga produk berdasarkan karat yang diupdate
            Map<String, Object> productUpdateResult = productService.updateProductPricesByPurity(purity);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Harga emas " + purity + "k berhasil diupdate");
            response.put("goldPrice", responseDto);
            response.put("productUpdate", productUpdateResult);
            response.put("timestamp", updatedPrice.getTanggalAmbil());

            return ResponseEntity.ok(ApiResponse.success("Update harga emas dan produk berhasil", response));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Gagal mengupdate harga emas dan produk: " + e.getMessage()));
        }
    }

    @Operation(summary = "Update Harga Emas Manual by Purity", description = "Update harga emas secara manual untuk 1 karat/purity dan otomatis update harga produk terkait")
    @PostMapping("/update/{purity}/manual")
    public ResponseEntity<?> updateGoldPriceManualByPurity(
            @Parameter(description = "Kadar kemurnian (10,14,16,18,21,22,24)", example = "18") @PathVariable int purity,
            @Parameter(description = "Harga jual per gram") @RequestParam double hargaJual,
            @Parameter(description = "Harga beli per gram") @RequestParam double hargaBeli) {
        try {
            System.out.println(">> Admin Controller : Update manual harga emas untuk " + purity + "k");

            // Simpan perubahan manual ke DB (service sudah ada: updateGoldPriceManual)
            GoldPrice updatedPrice = goldPriceService.updateGoldPriceManual(purity, hargaJual, hargaBeli);
            GoldPriceResponseDto responseDto = GoldPriceResponseDto.fromGoldPrice(updatedPrice);

            // Update harga produk hanya untuk purity terkait
            Map<String, Object> productUpdateResult = productService.updateProductPricesByPurity(purity);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Harga emas " + purity + "k berhasil diupdate secara manual");
            response.put("goldPrice", responseDto);
            response.put("productUpdate", productUpdateResult);
            response.put("timestamp", updatedPrice.getTanggalAmbil());

            return ResponseEntity.ok(ApiResponse.success("Update manual harga emas dan produk berhasil", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Gagal mengupdate harga emas manual: " + e.getMessage()));
        }
    }

    @Operation(summary = "Update Semua Harga Emas Manual", description = "Update semua harga emas secara manual sekaligus")
    @PostMapping("/update/manual")
    public ResponseEntity<?> updateAllGoldPricesManual(
            @Parameter(description = "Harga jual 24k per gram") @RequestParam double hargaJual24k,
            @Parameter(description = "Harga jual 22k per gram") @RequestParam double hargaJual22k,
            @Parameter(description = "Harga jual 18k per gram") @RequestParam double hargaJual18k) {
        try {
            System.out.println(">> Admin Controller : Memulai update manual semua harga emas");

            // Update harga emas secara manual untuk semua karat
            GoldPrice updatedPrice = goldPriceService.updateGoldPriceManualAll(hargaJual24k, hargaJual22k,
                    hargaJual18k);
            GoldPriceResponseDto responseDto = GoldPriceResponseDto.fromGoldPrice(updatedPrice);

            // Update harga produk untuk semua karat
            Map<String, Object> productUpdateResult = productService.updateAllProductPrices();

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Semua harga emas berhasil diupdate secara manual");
            response.put("goldPrice", responseDto);
            response.put("productUpdate", productUpdateResult);
            response.put("timestamp", updatedPrice.getTanggalAmbil());

            return ResponseEntity
                    .ok(ApiResponse.success("Update manual semua harga emas dan produk berhasil", response));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Gagal mengupdate semua harga emas dan produk: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get Perubahan Harga Emas Terbaru", description = "Mendapatkan perubahan harga emas terbaru dari database")
    @GetMapping("/changes/latest")
    public ResponseEntity<?> getLatestPriceChanges() {
        try {
            List<GoldPriceChange> latestChanges = goldPriceService.getLatestChangesForAllPurities();
            
            if (latestChanges.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success("Belum ada perubahan harga emas", new ArrayList<>()));
            }

            return ResponseEntity.ok(ApiResponse.success("Perubahan harga emas terbaru berhasil diambil", latestChanges));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Gagal mengambil perubahan harga emas: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get Riwayat Perubahan Harga", description = "Mendapatkan riwayat perubahan harga emas untuk karat tertentu")
    @GetMapping("/changes/{purity}")
    public ResponseEntity<?> getPriceChangesByPurity(
            @Parameter(description = "Kadar kemurnian (10k, 14k, 16k, 18k, 21k, 22k, 24k)", example = "18k") @PathVariable String purity) {
        try {
            List<GoldPriceChange> changes = goldPriceService.getPriceChanges(purity);

            Map<String, Object> response = new HashMap<>();
            response.put("purity", purity);
            response.put("changes", changes);
            response.put("totalChanges", changes.size());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity
                    .ok(ApiResponse.success("Riwayat perubahan harga emas " + purity + " berhasil diambil", response));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Gagal mengambil riwayat perubahan harga emas: " + e.getMessage()));
        }
    }

    @GetMapping("/fetch-external")
    public ResponseEntity<ApiResponse<Map<String, Object>>> fetchExternalPrice() {
        try {
            log.info(">> Controller: Memulai fetch harga emas eksternal");
            
            // Fetch harga dari API eksternal
            Map<String, Object> externalPrice = goldPriceService.fetchExternalPriceOnly();
            
            log.info(">> Controller: Harga eksternal berhasil diambil: {}", externalPrice);
            
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("Harga eksternal berhasil diambil")
                .data(externalPrice)
                .build());
                
        } catch (Exception e) {
            log.error(">> Controller: Error mengambil harga eksternal: {}", e.getMessage(), e);
            
            // Return error response yang lebih informatif
            String errorMessage = "Gagal mengambil harga eksternal: " + e.getMessage();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message(errorMessage)
                    .build());
        }
    }
}
