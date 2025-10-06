package com.projek.tokweb.dto.admin.report;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailySalesReportRow {
    private LocalDate date;
    private long orders;
    private long productsSold;
    private BigDecimal revenue;
    private BigDecimal averageOrderValue;
}
