package com.projek.tokweb.event;

import com.projek.tokweb.models.goldPrice.GoldPrice;
import com.projek.tokweb.service.admin.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GoldPriceUpdateEventListener {
    
    private final ProductService productService;
    
    @EventListener
    @Async
    public void handleGoldPriceUpdate(GoldPriceUpdateEvent event) {
        try {
            log.info(">> Event listener: Memulai update harga produk berdasarkan perubahan harga emas");
            
            GoldPrice newGoldPrice = event.getNewGoldPrice();
            productService.updateProductPricesByGoldPriceChange(newGoldPrice);
            
            log.info(">> Event listener: Update harga produk selesai");
            
        } catch (Exception e) {
            log.error(">> Event listener: Error dalam update harga produk: {}", e.getMessage());
        }
    }
}
