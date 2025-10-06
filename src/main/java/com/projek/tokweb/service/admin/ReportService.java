package com.projek.tokweb.service.admin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projek.tokweb.dto.admin.report.CategoryDistributionResponse;
import com.projek.tokweb.dto.admin.report.DailySalesReportRow;
import com.projek.tokweb.dto.admin.report.ReportDashboardResponse;
import com.projek.tokweb.dto.admin.report.ReportSummaryResponse;
import com.projek.tokweb.dto.admin.report.SalesTrendResponse;
import com.projek.tokweb.dto.admin.report.TopProductReport;
import com.projek.tokweb.models.Role;
import com.projek.tokweb.models.admin.Product;
import com.projek.tokweb.models.customer.Order;
import com.projek.tokweb.models.customer.OrderItem;
import com.projek.tokweb.models.customer.OrderStatus;
import com.projek.tokweb.repository.UserRespository;
import com.projek.tokweb.repository.customer.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final Locale LOCALE_ID = new Locale("id", "ID");
    private static final List<OrderStatus> REVENUE_STATUSES = List.of(
            OrderStatus.PENDING_CONFIRMATION,
            OrderStatus.PAID,
            OrderStatus.PROCESSING,
            OrderStatus.SHIPPED,
            OrderStatus.DELIVERED);

    private final OrderRepository orderRepository;
    private final UserRespository userRepository;

    @Transactional(readOnly = true)
    public ReportDashboardResponse getDashboardReport(LocalDate startDate, LocalDate endDate) {
        LocalDate normalizedStart = Optional.ofNullable(startDate).orElse(LocalDate.now().withDayOfMonth(1));
        LocalDate normalizedEnd = Optional.ofNullable(endDate).orElse(LocalDate.now());

        if (normalizedStart.isAfter(normalizedEnd)) {
            LocalDate temp = normalizedStart;
            normalizedStart = normalizedEnd;
            normalizedEnd = temp;
        }

        LocalDateTime startDateTime = normalizedStart.atStartOfDay();
        LocalDateTime endDateTime = normalizedEnd.atTime(23, 59, 59);

        List<Order> orders = orderRepository.findByCreatedAtBetweenAndStatusInWithItems(
                startDateTime,
                endDateTime,
                REVENUE_STATUSES);

        BigDecimal totalRevenue = orders.stream()
                .map(Order::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalOrders = orders.size();

        Map<Long, ProductAggregate> productAggregates = new LinkedHashMap<>();
        Map<LocalDate, DayAggregate> dayAggregates = initDayAggregates(normalizedStart, normalizedEnd);

        long totalProductsSold = 0L;

        for (Order order : orders) {
            LocalDate orderDate = order.getCreatedAt() != null
                    ? order.getCreatedAt().toLocalDate()
                    : normalizedStart;

            DayAggregate dayAggregate = dayAggregates.get(orderDate);
            if (dayAggregate == null) {
                dayAggregate = new DayAggregate();
                dayAggregates.put(orderDate, dayAggregate);
            }
            dayAggregate.orders++;
            BigDecimal orderAmount = Optional.ofNullable(order.getTotalAmount()).orElse(BigDecimal.ZERO);
            dayAggregate.revenue = dayAggregate.revenue.add(orderAmount);

            List<OrderItem> items = order.getItems();
            if (items == null) {
                continue;
            }

            for (OrderItem item : items) {
                Product product = item.getProduct();
                if (product == null || product.getId() == null) {
                    continue;
                }

                int quantity = Optional.ofNullable(item.getQuantity()).orElse(0);
                BigDecimal subtotal = Optional.ofNullable(item.getSubtotal()).orElse(BigDecimal.ZERO);

                dayAggregate.productsSold += quantity;

                totalProductsSold += quantity;

                productAggregates.compute(product.getId(), (key, aggregate) -> {
                    ProductAggregate acc = aggregate != null ? aggregate : new ProductAggregate();
                    acc.productId = product.getId();
                    acc.name = product.getName();
                    acc.category = product.getCategory();
                    acc.markup = product.getMarkup();
                    acc.quantity += quantity;
                    acc.revenue = acc.revenue.add(subtotal);
                    return acc;
                });
            }
        }

        for (DayAggregate aggregate : dayAggregates.values()) {
            if (aggregate.orders > 0) {
                aggregate.averageOrderValue = aggregate.revenue
                        .divide(BigDecimal.valueOf(aggregate.orders), 2, RoundingMode.HALF_UP);
            }
        }

        long newCustomers = userRepository.countByWaktuBuatBetweenAndRoleNot(startDateTime, endDateTime, Role.ADMIN);

        List<TopProductReport> topProducts = productAggregates.values().stream()
                .sorted(Comparator.comparingLong(ProductAggregate::getQuantity).reversed()
                        .thenComparing((a, b) -> b.getRevenue().compareTo(a.getRevenue())))
                .limit(10)
                .map(ProductAggregate::toDto)
                .collect(Collectors.toList());

        List<DailySalesReportRow> dailySales = dayAggregates.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> DailySalesReportRow.builder()
                        .date(entry.getKey())
                        .orders(entry.getValue().orders)
                        .productsSold(entry.getValue().productsSold)
                        .revenue(entry.getValue().revenue)
                        .averageOrderValue(entry.getValue().averageOrderValue)
                        .build())
                .collect(Collectors.toList());

        SalesTrendResponse salesTrend = buildSalesTrend(normalizedStart, normalizedEnd, dayAggregates);
        CategoryDistributionResponse distribution = buildCategoryDistribution(productAggregates);

        ReportSummaryResponse summary = ReportSummaryResponse.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .totalProductsSold(totalProductsSold)
                .newCustomers(newCustomers)
                .build();

        return ReportDashboardResponse.builder()
                .summary(summary)
                .topProducts(topProducts)
                .dailySales(dailySales)
                .salesTrend(salesTrend)
                .categoryDistribution(distribution)
                .build();
    }

    private Map<LocalDate, DayAggregate> initDayAggregates(LocalDate start, LocalDate end) {
        Map<LocalDate, DayAggregate> dayAggregates = new LinkedHashMap<>();
        LocalDate pointer = start;
        while (!pointer.isAfter(end)) {
            dayAggregates.put(pointer, new DayAggregate());
            pointer = pointer.plusDays(1);
        }
        return dayAggregates;
    }

    private SalesTrendResponse buildSalesTrend(LocalDate start, LocalDate end, Map<LocalDate, DayAggregate> dayAggregates) {
        long daysBetween = Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(start, end));

        if (daysBetween <= 31) {
            List<String> labels = new ArrayList<>();
            List<Double> values = new ArrayList<>();

            dayAggregates.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        labels.add(formatDateLabel(entry.getKey()));
                        values.add(entry.getValue().revenue.doubleValue());
                    });

            return SalesTrendResponse.builder()
                    .labels(labels)
                    .values(values)
                    .granularity("daily")
                    .build();
        }

        Map<YearMonth, BigDecimal> monthlyRevenue = new LinkedHashMap<>();
        YearMonth startMonth = YearMonth.from(start);
        YearMonth endMonth = YearMonth.from(end);
        YearMonth pointer = startMonth;

        while (!pointer.isAfter(endMonth)) {
            monthlyRevenue.put(pointer, BigDecimal.ZERO);
            pointer = pointer.plusMonths(1);
        }

        for (Map.Entry<LocalDate, DayAggregate> entry : dayAggregates.entrySet()) {
            YearMonth key = YearMonth.from(entry.getKey());
            monthlyRevenue.computeIfPresent(key, (k, value) -> value.add(entry.getValue().revenue));
        }

        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        for (Map.Entry<YearMonth, BigDecimal> entry : monthlyRevenue.entrySet()) {
            labels.add(formatMonthLabel(entry.getKey()));
            values.add(entry.getValue().doubleValue());
        }

        return SalesTrendResponse.builder()
                .labels(labels)
                .values(values)
                .granularity("monthly")
                .build();
    }

    private CategoryDistributionResponse buildCategoryDistribution(Map<Long, ProductAggregate> aggregates) {
        Map<String, BigDecimal> byCategory = new LinkedHashMap<>();

        aggregates.values().forEach(aggregate -> {
            String category = Optional.ofNullable(aggregate.category).filter(val -> !val.isBlank()).orElse("Lainnya");
            byCategory.merge(category, aggregate.revenue, BigDecimal::add);
        });

        if (byCategory.isEmpty()) {
            return CategoryDistributionResponse.builder()
                    .labels(List.of("Tidak ada data"))
                    .values(List.of(0.0))
                    .build();
        }

        List<String> labels = new ArrayList<>(byCategory.keySet());
        List<Double> values = byCategory.values().stream()
                .map(BigDecimal::doubleValue)
                .collect(Collectors.toList());

        return CategoryDistributionResponse.builder()
                .labels(labels)
                .values(values)
                .build();
    }

    private String formatDateLabel(LocalDate date) {
        return date.getDayOfWeek().getDisplayName(TextStyle.SHORT, LOCALE_ID) + ", " + date.getDayOfMonth();
    }

    private String formatMonthLabel(YearMonth month) {
        String monthName = month.getMonth().getDisplayName(TextStyle.SHORT, LOCALE_ID);
        return monthName + " " + month.getYear();
    }

    private static class ProductAggregate {
        private Long productId;
        private String name;
        private String category;
        private Double markup;
        private long quantity = 0L;
        private BigDecimal revenue = BigDecimal.ZERO;

        public long getQuantity() {
            return quantity;
        }

        public BigDecimal getRevenue() {
            return revenue;
        }

        public TopProductReport toDto() {
            return TopProductReport.builder()
                    .productId(productId)
                    .name(name)
                    .category(category)
                    .quantitySold(quantity)
                    .revenue(revenue)
                    .markup(markup)
                    .build();
        }
    }

    private static class DayAggregate {
        private long orders = 0L;
        private long productsSold = 0L;
        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal averageOrderValue = BigDecimal.ZERO;
    }
}
