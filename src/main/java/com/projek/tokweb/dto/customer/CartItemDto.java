package com.projek.tokweb.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDto {
    private Long id;
    private Long userId;
    private Long productId;
    private String productName;
    private String imageUrl;
    private Double weight;
    private Integer purity;
    private Double price;
    private Double priceAtTime; // Harga yang disimpan saat item ditambahkan ke keranjang
    private Integer quantity;
    private String specs;
}
