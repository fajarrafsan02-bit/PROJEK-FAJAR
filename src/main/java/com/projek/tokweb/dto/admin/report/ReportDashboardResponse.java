package com.projek.tokweb.dto.admin.report;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDashboardResponse {
    private ReportSummaryResponse summary;
    private List<TopProductReport> topProducts;
    private List<DailySalesReportRow> dailySales;
    private SalesTrendResponse salesTrend;
    private CategoryDistributionResponse categoryDistribution;
}
