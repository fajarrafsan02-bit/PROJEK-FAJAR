package com.projek.tokweb.models.admin;

import java.beans.Transient;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.projek.tokweb.utils.NumberFormatter;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private double weight;
    private int purity;
    private String category;
    private String imageUrl;

    private double markup;
    private Double finalPrice;

    private int stock;
    private int minStock;
    private boolean isActive;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateAt;

    public boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    // Formatted fields untuk response
    @Transient
    @JsonIgnore
    public String getFormattedFinalPrice() {
        return NumberFormatter.formatCurrencyWithoutSymbolRounded(this.finalPrice);
    }

    @Transient
    @JsonIgnore
    public String getFormattedFinalPriceWithCurrency() {
        return NumberFormatter.formatCurrencyRounded(this.finalPrice);
    }

    @Transient
    @JsonIgnore
    public String getFormattedMarkup() {
        return NumberFormatter.formatNumber(this.markup) + "%";
    }

    @Transient
    @JsonIgnore
    public String getFormattedWeight() {
        return NumberFormatter.formatNumber(this.weight) + " gram";
    }
}
