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
public class ReportSummaryResponse {
    private BigDecimal totalRevenue;
    private long totalOrders;
    private long totalProductsSold;
    private long newCustomers;
}
