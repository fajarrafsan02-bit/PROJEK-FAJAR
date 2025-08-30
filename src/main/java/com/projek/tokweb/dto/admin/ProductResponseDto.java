package com.projek.tokweb.dto.admin;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.projek.tokweb.models.admin.Product;
import com.projek.tokweb.utils.NumberFormatter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {
    private Long id;
    private String name;
    private String description;
    private String formattedWeight;
    private int purity;
    private String category;
    private String imageUrl;
    private String formattedMarkup;
    private String formattedFinalPrice;
    private String formattedFinalPriceWithCurrency;
    private int stock;
    private int minStock;
    private boolean isActive;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateAt;

    // Constructor dari Product entity
    public static ProductResponseDto fromProduct(Product product) {
        return ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .formattedWeight(product.getWeight() + " gram")
                .purity(product.getPurity())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .formattedMarkup(NumberFormatter.formatNumber(product.getMarkup()) + "%")
                .formattedFinalPrice(NumberFormatter.formatCurrencyWithoutSymbol(product.getFinalPrice()))
                .formattedFinalPriceWithCurrency(NumberFormatter.formatCurrency(product.getFinalPrice()))
                .stock(product.getStock())
                .minStock(product.getMinStock())
                .isActive(product.getIsActive())
                .updateAt(product.getUpdateAt())
                .build();
    }
}