package com.projek.tokweb.config.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Component
@ConfigurationProperties(prefix = "app.checkout")
@Validated
@Data
public class CheckoutProperties {
    
    /**
     * Jika true -> stok akan dikurangi (direservasi) pada saat checkout.
     * Jika false -> stok akan dikurangi saat payment "PAID".
     */
    private boolean reserveStock = true;

    /**
     * TTL pembayaran dalam jam. Setelah melewati expiresAt = createdAt + paymentTtlHours, order dapat dibatalkan.
     */
    @Min(1)
    private long paymentTtlHours = 24L;

    /**
     * Nama gateway default (mis. "mock", "xendit", "midtrans").
     */
    private String gateway = "mock";

    // Lombok @Data menyediakan getter/setter/toString
}
