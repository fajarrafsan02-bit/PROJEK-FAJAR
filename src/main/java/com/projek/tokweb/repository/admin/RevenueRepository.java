package com.projek.tokweb.repository.admin;

import com.projek.tokweb.models.admin.Revenue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RevenueRepository extends JpaRepository<Revenue, Long> {
    
    // Find revenue by order ID
    Optional<Revenue> findByOrderId(Long orderId);
    
    // Find revenue by order number
    Optional<Revenue> findByOrderNumber(String orderNumber);
    
    // Get daily revenue
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Revenue r WHERE r.revenueDate = :date")
    BigDecimal getDailyRevenue(@Param("date") LocalDate date);
    
    // Get monthly revenue
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Revenue r WHERE YEAR(r.revenueDate) = :year AND MONTH(r.revenueDate) = :month")
    BigDecimal getMonthlyRevenue(@Param("year") int year, @Param("month") int month);
    
    // Get yearly revenue
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Revenue r WHERE YEAR(r.revenueDate) = :year")
    BigDecimal getYearlyRevenue(@Param("year") int year);
    
    // Get revenue between dates
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Revenue r WHERE r.revenueDate BETWEEN :startDate AND :endDate")
    BigDecimal getRevenueBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    // Get revenue by date range with pagination
    @Query("SELECT r FROM Revenue r WHERE r.revenueDate BETWEEN :startDate AND :endDate ORDER BY r.revenueDate DESC, r.createdAt DESC")
    Page<Revenue> findByRevenueDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);
    
    // Get revenue by specific date
    @Query("SELECT r FROM Revenue r WHERE r.revenueDate = :date ORDER BY r.createdAt DESC")
    List<Revenue> findByRevenueDate(@Param("date") LocalDate date);
    
    // Get revenue by month and year
    @Query("SELECT r FROM Revenue r WHERE YEAR(r.revenueDate) = :year AND MONTH(r.revenueDate) = :month ORDER BY r.revenueDate DESC, r.createdAt DESC")
    List<Revenue> findByMonthAndYear(@Param("year") int year, @Param("month") int month);
    
    // Get daily revenue statistics for a month
    @Query("SELECT r.revenueDate, COALESCE(SUM(r.amount), 0) FROM Revenue r WHERE YEAR(r.revenueDate) = :year AND MONTH(r.revenueDate) = :month GROUP BY r.revenueDate ORDER BY r.revenueDate")
    List<Object[]> getDailyRevenueStatistics(@Param("year") int year, @Param("month") int month);
    
    // Get monthly revenue statistics for a year
    @Query("SELECT MONTH(r.revenueDate), COALESCE(SUM(r.amount), 0) FROM Revenue r WHERE YEAR(r.revenueDate) = :year GROUP BY MONTH(r.revenueDate) ORDER BY MONTH(r.revenueDate)")
    List<Object[]> getMonthlyRevenueStatistics(@Param("year") int year);
    
    // Count revenue records by date
    @Query("SELECT COUNT(r) FROM Revenue r WHERE r.revenueDate = :date")
    Long countByRevenueDate(@Param("date") LocalDate date);
    
    // Count revenue records by month
    @Query("SELECT COUNT(r) FROM Revenue r WHERE YEAR(r.revenueDate) = :year AND MONTH(r.revenueDate) = :month")
    Long countByMonthAndYear(@Param("year") int year, @Param("month") int month);
    
    // Get top revenue days
    @Query("SELECT r.revenueDate, COALESCE(SUM(r.amount), 0) FROM Revenue r GROUP BY r.revenueDate ORDER BY SUM(r.amount) DESC")
    List<Object[]> getTopRevenueDays(Pageable pageable);
    
    // Check if revenue exists for order
    boolean existsByOrderId(Long orderId);
    boolean existsByOrderNumber(String orderNumber);
}