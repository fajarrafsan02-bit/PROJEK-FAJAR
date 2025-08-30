package com.projek.tokweb.config.initializer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.projek.tokweb.models.goldPrice.GoldPriceEnum;
import com.projek.tokweb.repository.goldprice.GoldPriceRepository;
import com.projek.tokweb.service.goldprice.GoldPriceService;

import lombok.RequiredArgsConstructor;

// @Component
@RequiredArgsConstructor
public class HargaEmailInitializer implements CommandLineRunner{
    @Autowired
    private final GoldPriceService goldPriceService;
    @Autowired
    private final GoldPriceRepository goldPriceRepository;

    @Override
    public void run(String... args) throws Exception {
        if (goldPriceRepository.count() == 0) {
            goldPriceService.fetchAndSaveLatestPrice(GoldPriceEnum.SISTEM);
            System.out.println(">> Harga Emas awal berhasil di ambil dari API");
        } else {
            System.out.println(">> Data Harga emas sudah tersedia, tidak perlu inisialisasi ulang");
        }
    }
}
