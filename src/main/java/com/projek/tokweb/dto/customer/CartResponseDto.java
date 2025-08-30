package com.projek.tokweb.dto.customer;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponseDto {
    private Long cartId;
    private List<CartItemDto> items;
    private Integer totalItems;
    private Double totalPrice;
}


