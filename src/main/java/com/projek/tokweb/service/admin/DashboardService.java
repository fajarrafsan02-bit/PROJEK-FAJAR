package com.projek.tokweb.service.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.projek.tokweb.models.customer.Order;
import com.projek.tokweb.models.customer.OrderStatus;
import com.projek.tokweb.repository.customer.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final OrderRepository orderRepository;

    /**
     * Mendapatkan data grafik penjualan berdasarkan periode
     */
    public Map<String, Object> getSalesChartData(String period) {
        try {
            log.info("📊 Getting sales chart data for period: {}", period);

            Map<String, Object> chartData = new HashMap<>();
            List<String> labels = new ArrayList<>();
            List<BigDecimal> data = new ArrayList<>();

            LocalDate endDate = LocalDate.now();
            LocalDate startDate;

            switch (period.toLowerCase()) {
                case "7days":
                    startDate = endDate.minusDays(6); // 7 hari termasuk hari ini
                    chartData = get7DaysSalesData(startDate, endDate);
                    break;

                case "30days":
                    startDate = endDate.minusDays(29); // 30 hari termasuk hari ini
                    chartData = get30DaysSalesData(startDate, endDate);
                    break;

                case "90days":
                    startDate = endDate.minusDays(89); // 90 hari termasuk hari ini
                    chartData = get90DaysSalesData(startDate, endDate);
                    break;

                default:
                    // Default ke 7 hari
                    startDate = endDate.minusDays(6);
                    chartData = get7DaysSalesData(startDate, endDate);
                    break;
            }

            log.info("✅ Chart data generated for period {} with {} data points",
                    period, ((List<?>) chartData.get("values")).size());

            return chartData;

        } catch (Exception e) {
            log.error("❌ Error generating sales chart data for period {}: {}", period, e.getMessage(), e);

            // Return default data jika error
            Map<String, Object> defaultData = new HashMap<>();
            defaultData.put("labels", List.of("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"));
            defaultData.put("values", List.of(0, 0, 0, 0, 0, 0, 0)); // Use 'values' to match frontend
            defaultData.put("period", period);
            defaultData.put("error", true);

            return defaultData;
        }
    }

    /**
     * Data penjualan 7 hari terakhir (harian)
     * PERBAIKAN: Menghitung 7 hari terakhir mulai dari 6 hari yang lalu hingga hari ini
     */
    private Map<String, Object> get7DaysSalesData(LocalDate startDate, LocalDate endDate) {
        List<String> labels = new ArrayList<>();
        List<BigDecimal> data = new ArrayList<>();

        // PERBAIKAN: Gunakan 7 hari terakhir yang sebenarnya (6 hari lalu + hari ini)
        LocalDate today = LocalDate.now();
        LocalDate realStartDate = today.minusDays(6); // 6 hari yang lalu
        LocalDate realEndDate = today; // hari ini

        log.info("📅 7-day chart (PERBAIKAN): {} - {} (today: {})", realStartDate, realEndDate, today);

        // Generate labels dan data untuk 7 hari terakhir yang sebenarnya
        LocalDate currentDate = realStartDate;
        while (!currentDate.isAfter(realEndDate)) {
            // Format label hari (Sen, Sel, Rab, dll)
            String dayLabel = currentDate.getDayOfWeek()
                    .getDisplayName(TextStyle.SHORT, new Locale("id", "ID"));
            labels.add(dayLabel);

            // Ambil total penjualan untuk hari ini
            BigDecimal dailySales = getDailySales(currentDate);
            data.add(dailySales);

            log.info("📅 {} ({}) - {}: Rp {}", currentDate, dayLabel, 
                    (currentDate.equals(today) ? "HARI INI" : 
                     currentDate.equals(today.minusDays(1)) ? "KEMARIN" : 
                     currentDate.toString()), formatCurrency(dailySales));

            currentDate = currentDate.plusDays(1);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("values", data);
        result.put("period", "7days");
        result.put("title", "Penjualan 7 Hari Terakhir");
        result.put("startDate", realStartDate.toString());
        result.put("endDate", realEndDate.toString());

        // Log total untuk debugging
        BigDecimal totalWeek = data.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        log.info("✅ 7-day chart total: {} (7 hari dari {} s/d {})", 
                formatCurrency(totalWeek), realStartDate, realEndDate);

        return result;
    }

    /**
     * Data penjualan 30 hari terakhir (mingguan)
     * Hari ini (terbaru) masuk ke Minggu 1, minggu terlama jadi minggu terakhir
     */
    private Map<String, Object> get30DaysSalesData(LocalDate startDate, LocalDate endDate) {
        List<String> labels = new ArrayList<>();
        List<BigDecimal> data = new ArrayList<>();

        // Kumpulkan semua periode minggu dulu (dari terlama ke terbaru)
        List<Map<String, Object>> weeks = new ArrayList<>();
        LocalDate currentWeekStart = startDate;

        while (currentWeekStart.isBefore(endDate) || currentWeekStart.equals(endDate)) {
            // Hitung akhir minggu (7 hari atau sampai endDate)
            LocalDate weekEnd = currentWeekStart.plusDays(6);
            if (weekEnd.isAfter(endDate)) {
                weekEnd = endDate;
            }

            // Simpan data minggu
            Map<String, Object> weekData = new HashMap<>();
            weekData.put("start", currentWeekStart);
            weekData.put("end", weekEnd);
            weekData.put("sales", getWeeklySales(currentWeekStart, weekEnd));
            weeks.add(weekData);

            log.debug("📅 Week period ({} - {}): Rp {}", currentWeekStart, weekEnd, weekData.get("sales"));

            // Pindah ke minggu berikutnya
            currentWeekStart = weekEnd.plusDays(1);

            // Break jika weekEnd sudah mencapai atau melewati endDate
            if (weekEnd.equals(endDate) || weekEnd.isAfter(endDate)) {
                break;
            }
        }

        // Sekarang buat labels dan data dengan urutan terbalik
        // Minggu 1 = minggu terbaru (yang mengandung hari ini)
        // Minggu terakhir = minggu terlama
        int totalWeeks = weeks.size();
        for (int i = totalWeeks - 1; i >= 0; i--) {
            Map<String, Object> week = weeks.get(i);

            // Label minggu dari 1 sampai N (di mana 1 adalah minggu terbaru)
            String weekLabel = String.format("Minggu %d", totalWeeks - i);
            labels.add(weekLabel);
            data.add((BigDecimal) week.get("sales"));

            log.info("📅 {} ({} - {}): Rp {}",
                    weekLabel,
                    week.get("start"),
                    week.get("end"),
                    week.get("sales"));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("values", data);
        result.put("period", "30days");
        result.put("title", "Penjualan 30 Hari Terakhir");

        log.info("✅ Generated 30-day chart with {} weeks. Today ({}) is in Minggu 1",
                labels.size(), LocalDate.now());
        return result;
    }

    /**
     * Data penjualan 90 hari terakhir (periode 30 hari)
     * Membagi 90 hari menjadi 3 periode 30 hari berurutan
     */
    private Map<String, Object> get90DaysSalesData(LocalDate startDate, LocalDate endDate) {
        List<String> labels = new ArrayList<>();
        List<BigDecimal> data = new ArrayList<>();

        // Bagi 90 hari menjadi 3 periode 30 hari berurutan dari yang terlama ke terbaru
        LocalDate currentPeriodStart = startDate;
        int periodNum = 1;

        while (currentPeriodStart.isBefore(endDate) || currentPeriodStart.equals(endDate)) {
            // Hitung akhir periode (30 hari atau sampai endDate)
            LocalDate periodEnd = currentPeriodStart.plusDays(29); // 30 hari total
            if (periodEnd.isAfter(endDate)) {
                periodEnd = endDate;
            }

            String periodLabel = String.format("Periode %d", periodNum);
            labels.add(periodLabel);

            // Total penjualan untuk periode ini
            BigDecimal periodTotal = getMonthlySales(currentPeriodStart, periodEnd);
            data.add(periodTotal);

            log.debug("📅 Periode {} ({} - {}): Rp {}", periodNum, currentPeriodStart, periodEnd, periodTotal);

            // Pindah ke periode berikutnya
            currentPeriodStart = periodEnd.plusDays(1);
            periodNum++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("values", data);
        result.put("period", "90days");
        result.put("title", "Penjualan 90 Hari Terakhir");

        log.info("✅ Generated 90-day chart with {} periods: {}", labels.size(), labels);
        return result;
    }

    /**
     * Mendapatkan total penjualan untuk hari tertentu
     */
    private BigDecimal getDailySales(LocalDate date) {
        try {
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59);

            log.info("🔍 DEBUG: Getting daily sales for date {} ({}), time range: {} to {}", 
                    date, date.getDayOfWeek(), startOfDay, endOfDay);

            // Ambil SEMUA order pada tanggal tersebut dulu untuk debugging
            List<Order> allOrders = orderRepository.findByCreatedAtBetweenAndStatusIn(
                    startOfDay,
                    endOfDay,
                    List.of(OrderStatus.PENDING_PAYMENT, OrderStatus.PENDING_CONFIRMATION, 
                            OrderStatus.PAID, OrderStatus.PROCESSING, OrderStatus.SHIPPED, 
                            OrderStatus.DELIVERED, OrderStatus.CANCELLED, OrderStatus.REFUNDED));

            log.info("🔍 DEBUG: Found {} total orders on {}", allOrders.size(), date);
            
            // Log detail semua order
            for (Order order : allOrders) {
                log.info("🔍 DEBUG: Order {} - Status: {} - Amount: Rp {} - Created: {}", 
                        order.getOrderNumber(), order.getStatus(), 
                        formatCurrency(order.getTotalAmount()), order.getCreatedAt());
            }

            // Sekarang ambil yang status nya untuk revenue (PENDING_CONFIRMATION ke atas)
            // PENDING_CONFIRMATION artinya sudah ada bukti pembayaran, jadi dihitung sebagai revenue
            List<Order> revenueOrders = orderRepository.findByCreatedAtBetweenAndStatusIn(
                    startOfDay,
                    endOfDay,
                    List.of(OrderStatus.PENDING_CONFIRMATION, OrderStatus.PAID, OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.DELIVERED));

            log.info("🔍 DEBUG: Found {} revenue orders (PAID+) on {}", revenueOrders.size(), date);

            BigDecimal total = revenueOrders.stream()
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            log.info("📊 Daily sales for {} ({}): {} orders = {}", 
                    date, date.getDayOfWeek(), revenueOrders.size(), formatCurrency(total));

            return total;

        } catch (Exception e) {
            log.error("❌ Error getting daily sales for {}: {}", date, e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Mendapatkan total penjualan untuk periode mingguan
     */
    private BigDecimal getWeeklySales(LocalDate startDate, LocalDate endDate) {
        try {
            LocalDateTime startOfWeek = startDate.atStartOfDay();
            LocalDateTime endOfWeek = endDate.atTime(23, 59, 59);

            List<Order> orders = orderRepository.findByCreatedAtBetweenAndStatusIn(
                    startOfWeek,
                    endOfWeek,
                    List.of(OrderStatus.PENDING_CONFIRMATION, OrderStatus.PAID, OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.DELIVERED));

            return orders.stream()
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

        } catch (Exception e) {
            log.error("❌ Error getting weekly sales for {} - {}: {}", startDate, endDate, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * Mendapatkan total penjualan untuk periode bulanan
     */
    private BigDecimal getMonthlySales(LocalDate startDate, LocalDate endDate) {
        try {
            LocalDateTime startOfMonth = startDate.atStartOfDay();
            LocalDateTime endOfMonth = endDate.atTime(23, 59, 59);

            List<Order> orders = orderRepository.findByCreatedAtBetweenAndStatusIn(
                    startOfMonth,
                    endOfMonth,
                    List.of(OrderStatus.PENDING_CONFIRMATION, OrderStatus.PAID, OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.DELIVERED));

            return orders.stream()
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

        } catch (Exception e) {
            log.error("❌ Error getting monthly sales for {} - {}: {}", startDate, endDate, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * Mendapatkan aktivitas terbaru (pesanan, perubahan status, dll)
     */
    public List<Map<String, Object>> getRecentActivities(int limit) {
        try {
            log.info("📋 Getting recent activities with limit: {}", limit);

            List<Map<String, Object>> activities = new ArrayList<>();

            // Ambil order terbaru dengan berbagai status
            List<Order> recentOrders = orderRepository.findTop20ByOrderByCreatedAtDesc();

            for (Order order : recentOrders) {
                if (activities.size() >= limit)
                    break;

                Map<String, Object> activity = new HashMap<>();
                String customerName = order.getCustomerName() != null && !order.getCustomerName().isEmpty()
                        ? order.getCustomerName()
                        : "Customer";

                // Tentukan jenis aktivitas berdasarkan status order
                switch (order.getStatus()) {
                    case PENDING_PAYMENT:
                        activity.put("type", "order");
                        activity.put("icon", "fas fa-shopping-cart");
                        activity.put("iconClass", "order");
                        activity.put("text", String.format("Pesanan baru %s dari %s",
                                order.getOrderNumber(), customerName));
                        break;

                    case PAID:
                        activity.put("type", "payment");
                        activity.put("icon", "fas fa-credit-card");
                        activity.put("iconClass", "product");
                        activity.put("text", String.format("Pembayaran pesanan %s telah diterima dari %s",
                                order.getOrderNumber(), customerName));
                        break;

                    case PROCESSING:
                        activity.put("type", "processing");
                        activity.put("icon", "fas fa-cogs");
                        activity.put("iconClass", "price");
                        activity.put("text", String.format("Pesanan %s sedang diproses untuk %s",
                                order.getOrderNumber(), customerName));
                        break;

                    case SHIPPED:
                        activity.put("type", "shipping");
                        activity.put("icon", "fas fa-truck");
                        activity.put("iconClass", "price");
                        activity.put("text", String.format("Pesanan %s telah dikirim kepada %s",
                                order.getOrderNumber(), customerName));
                        break;

                    case DELIVERED:
                        activity.put("type", "delivery");
                        activity.put("icon", "fas fa-check-circle");
                        activity.put("iconClass", "order");
                        activity.put("text", String.format("Pesanan %s telah selesai untuk %s",
                                order.getOrderNumber(), customerName));
                        break;

                    case CANCELLED:
                        activity.put("type", "cancelled");
                        activity.put("icon", "fas fa-times-circle");
                        activity.put("iconClass", "order");
                        activity.put("text", String.format("Pesanan %s dibatalkan untuk %s",
                                order.getOrderNumber(), customerName));
                        break;

                    default:
                        activity.put("type", "order");
                        activity.put("icon", "fas fa-shopping-cart");
                        activity.put("iconClass", "order");
                        activity.put("text", String.format("Aktivitas pesanan %s untuk %s",
                                order.getOrderNumber(), customerName));
                        break;
                }

                activity.put("time", formatTimeAgo(order.getCreatedAt()));
                activity.put("timestamp", order.getCreatedAt());
                activity.put("orderNumber", order.getOrderNumber());
                activity.put("customerName", customerName);
                activity.put("status", order.getStatus().toString());
                activity.put("amount", order.getTotalAmount());
                activity.put("formattedAmount", formatCurrency(order.getTotalAmount()));

                activities.add(activity);

                log.debug("📅 Activity: {} - {} - {}",
                        order.getOrderNumber(),
                        order.getStatus(),
                        formatTimeAgo(order.getCreatedAt()));
            }

            log.info("✅ Generated {} recent activities from database", activities.size());
            return activities;

        } catch (Exception e) {
            log.error("❌ Error getting recent activities: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Format waktu menjadi "X menit yang lalu", "X jam yang lalu", dll
     */
    private String formatTimeAgo(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        java.time.Duration duration = java.time.Duration.between(dateTime, now);

        long minutes = duration.toMinutes();
        long hours = duration.toHours();
        long days = duration.toDays();

        if (days > 0) {
            return days == 1 ? "1 hari yang lalu" : days + " hari yang lalu";
        } else if (hours > 0) {
            return hours == 1 ? "1 jam yang lalu" : hours + " jam yang lalu";
        } else if (minutes > 0) {
            return minutes == 1 ? "1 menit yang lalu" : minutes + " menit yang lalu";
        } else {
            return "Baru saja";
        }
    }

    /**
     * Format currency ke format Indonesia (Rp)
     */
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "Rp 0";
        }

        // Format number dengan pemisah ribuan
        java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,###");
        formatter.setDecimalFormatSymbols(java.text.DecimalFormatSymbols.getInstance(new Locale("id", "ID")));

        return "Rp " + formatter.format(amount);
    }

    /**
     * DEBUG: Method untuk mengambil semua order hari ini tanpa filter status
     */
    public List<Map<String, Object>> getAllTodayOrdersForDebug() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);
        
        log.info("🔍 DEBUG: Getting ALL orders for today {} from {} to {}", today, startOfDay, endOfDay);
        
        // Ambil SEMUA order hari ini tanpa filter status
        List<Order> allTodayOrders = orderRepository.findByCreatedAtBetweenAndStatusIn(
                startOfDay, 
                endOfDay, 
                List.of(OrderStatus.PENDING_PAYMENT, OrderStatus.PENDING_CONFIRMATION, 
                        OrderStatus.PAID, OrderStatus.PROCESSING, OrderStatus.SHIPPED, 
                        OrderStatus.DELIVERED, OrderStatus.CANCELLED, OrderStatus.REFUNDED));
        
        List<Map<String, Object>> debugData = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        
        for (Order order : allTodayOrders) {
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("orderNumber", order.getOrderNumber());
            orderData.put("status", order.getStatus().toString());
            orderData.put("amount", order.getTotalAmount());
            orderData.put("formattedAmount", formatCurrency(order.getTotalAmount()));
            orderData.put("createdAt", order.getCreatedAt());
            orderData.put("customerName", order.getCustomerName());
            
            // Hitung revenue hanya untuk status PENDING_CONFIRMATION ke atas
            if (order.getStatus() == OrderStatus.PENDING_CONFIRMATION ||
                order.getStatus() == OrderStatus.PAID || 
                order.getStatus() == OrderStatus.PROCESSING ||
                order.getStatus() == OrderStatus.SHIPPED ||
                order.getStatus() == OrderStatus.DELIVERED) {
                totalRevenue = totalRevenue.add(order.getTotalAmount());
                orderData.put("countsAsRevenue", true);
            } else {
                orderData.put("countsAsRevenue", false);
            }
            
            debugData.add(orderData);
        }
        
        log.info("🔍 DEBUG: Found {} orders today, total revenue: {}", 
                allTodayOrders.size(), formatCurrency(totalRevenue));
        
        return debugData;
    }
}
