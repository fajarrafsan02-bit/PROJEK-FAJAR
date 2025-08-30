package com.projek.tokweb.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class NumberFormatter {
    private static final DecimalFormat IDR_FORMATTER = new DecimalFormat("#,##0");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(Locale.of("id", "ID"));

    public static String formatNumber(double number) {
        return IDR_FORMATTER.format(number);
    }

    public static String formatNumber(int number) {
        return IDR_FORMATTER.format(number);
    }

    /**
     * Format angka dengan pemisah ribuan (contoh: 1,000,000)
     */
    public static String formatNumber(long number) {
        return IDR_FORMATTER.format(number);
    }

    /**
     * Format currency Indonesia (contoh: Rp 1,000,000)
     */
    public static String formatCurrency(double amount) {
        return CURRENCY_FORMATTER.format(amount);
    }

    /**
     * Format currency Indonesia tanpa "Rp" (contoh: 1,000,000)
     */
    public static String formatCurrencyWithoutSymbol(double amount) {
        return IDR_FORMATTER.format(amount);
    }

    /**
     * Membulatkan harga emas ke angka yang lebih mudah dibaca
     * Contoh: 1887672.0119880002 -> 1900000
     * 
     * @param amount Harga yang akan dibulatkan
     * @return Harga yang sudah dibulatkan
     */
    public static double roundGoldPrice(double amount) {
        if (amount <= 0) return 0;
        
        // Jika harga < 1000, bulatkan ke puluhan terdekat
        if (amount < 1000) {
            return Math.round(amount / 10.0) * 10;
        }
        
        // Jika harga < 10000, bulatkan ke ratusan terdekat
        if (amount < 10000) {
            return Math.round(amount / 100.0) * 100;
        }
        
        // Jika harga < 100000, bulatkan ke ribuan terdekat
        if (amount < 100000) {
            return Math.round(amount / 1000.0) * 1000;
        }
        
        // Jika harga < 1000000, bulatkan ke puluhan ribu terdekat
        if (amount < 1000000) {
            return Math.round(amount / 10000.0) * 10000;
        }
        
        // Jika harga >= 1000000, bulatkan ke ratusan ribu terdekat
        return Math.round(amount / 100000.0) * 100000;
    }

    /**
     * Format currency Indonesia dengan pembulatan harga emas (contoh: Rp 1,900,000)
     */
    public static String formatCurrencyRounded(double amount) {
        double roundedAmount = roundGoldPrice(amount);
        return CURRENCY_FORMATTER.format(roundedAmount);
    }

    /**
     * Format currency Indonesia tanpa "Rp" dengan pembulatan harga emas (contoh: 1,900,000)
     */
    public static String formatCurrencyWithoutSymbolRounded(double amount) {
        double roundedAmount = roundGoldPrice(amount);
        return IDR_FORMATTER.format(roundedAmount);
    }
}
