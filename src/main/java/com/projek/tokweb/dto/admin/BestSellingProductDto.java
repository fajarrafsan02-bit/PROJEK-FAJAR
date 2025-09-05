package com.projek.tokweb.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BestSellingProductDto {
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal productPrice;
    private Integer salesCount;
    private String productCategory;
}