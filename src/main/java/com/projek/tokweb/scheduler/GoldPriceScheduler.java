package com.projek.tokweb.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.projek.tokweb.models.goldPrice.GoldPriceEnum;
import com.projek.tokweb.service.admin.ProductService;
import com.projek.tokweb.service.goldprice.GoldPriceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class GoldPriceScheduler {
    private final GoldPriceService goldPriceService;
    private final ProductService productPriceUpdateService;

    @Scheduled(cron = "0 0 8 * * ?") // Update setiap hari jam 8 pagi
    public void updateDailyGoldPrice() {
        log.info(">> Scheduler : Memulai update harga emas harian pada {}", LocalDateTime.now());
        try {
            // Update harga emas (akan otomatis update harga produk)
            goldPriceService.fetchAndSaveLatestPrice(GoldPriceEnum.SISTEM);
            log.info(">> Scheduler : Update harga emas harian selesai (termasuk update harga produk)");

        } catch (Exception e) {
            log.error(">> Scheduler : Gagal update harga emas harian - {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 12 * * ?") // Update tambahan jam 12 siang
    public void updateMiddayGoldPrice() {
        log.info(">> Scheduler : Memulai update harga emas tengah hari pada {}", LocalDateTime.now());
        try {
            // Update harga emas
            goldPriceService.fetchAndSaveLatestPrice(GoldPriceEnum.SISTEM);
            log.info(">> Scheduler : Update harga emas tengah hari selesai");

            // Update harga produk berdasarkan harga emas baru
            log.info(">> Scheduler : Memulai update harga produk tengah hari");
            var result = productPriceUpdateService.updateAllProductPrices();
            log.info(">> Scheduler : Update harga produk tengah hari selesai - {}", result.get("message"));

        } catch (Exception e) {
            log.error(">> Scheduler : Gagal update harga emas tengah hari - {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 16 * * ?") // Update sore hari jam 4 sore
    public void updateAfternoonGoldPrice() {
        log.info(">> Scheduler : Memulai update harga emas sore hari pada {}", LocalDateTime.now());
        try {
            // Update harga emas
            goldPriceService.fetchAndSaveLatestPrice(GoldPriceEnum.SISTEM);
            log.info(">> Scheduler : Update harga emas sore hari selesai");

            // Update harga produk berdasarkan harga emas baru
            log.info(">> Scheduler : Memulai update harga produk sore hari");
            var result = productPriceUpdateService.updateAllProductPrices();
            log.info(">> Scheduler : Update harga produk sore hari selesai - {}", result.get("message"));

        } catch (Exception e) {
            log.error(">> Scheduler : Gagal update harga emas sore hari - {}", e.getMessage());
        }
    }
}
