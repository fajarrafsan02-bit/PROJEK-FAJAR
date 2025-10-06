package com.projek.tokweb.dto.admin.report;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopProductReport {
    private Long productId;
    private String name;
    private String category;
    private long quantitySold;
    private BigDecimal revenue;
    private Double markup;
}
