package com.projek.tokweb.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ProductRequestDto {

    private String name;
    private String description;
    private Double weight;
    private Integer purity;
    private String category;
    private String imageUrl;
    private Double markup;
    private Integer stock;
    private Integer minStock;
    private Boolean isActive;
}
