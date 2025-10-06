package com.projek.tokweb.repository.customer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.projek.tokweb.models.customer.Order;
import com.projek.tokweb.models.customer.OrderStatus;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    List<Order> findByStatusAndExpiresAtBefore(OrderStatus status, LocalDateTime time);
    
    List<Order> findByUserId(Long userId);
    
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<Order> findByStatus(OrderStatus status);
    
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    
    Page<Order> findByStatusIn(List<OrderStatus> statuses, Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE o.status IN :statuses ORDER BY o.createdAt DESC")
    List<Order> findByStatusInOrderByCreatedAtDesc(@Param("statuses") List<OrderStatus> statuses);
    
    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC")
    Page<Order> findAllOrderByCreatedAtDesc(Pageable pageable);
    
    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC")
    List<Order> findAllOrderByCreatedAtDesc();
    
    @Query("SELECT o FROM Order o WHERE o.orderNumber LIKE %:orderNumber% ORDER BY o.createdAt DESC")
    List<Order> findByOrderNumberContainingOrderByCreatedAtDesc(@Param("orderNumber") String orderNumber);
    
    @Query("SELECT o FROM Order o WHERE o.customerName LIKE %:customerName% OR o.customerPhone LIKE %:customerPhone% ORDER BY o.createdAt DESC")
    List<Order> findByCustomerNameOrPhoneContainingOrderByCreatedAtDesc(
        @Param("customerName") String customerName, 
        @Param("customerPhone") String customerPhone
    );
    
    Optional<Order> findByOrderNumber(String orderNumber);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    Long countByStatus(@Param("status") OrderStatus status);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status IN :statuses")
    Long countByStatusIn(@Param("statuses") List<OrderStatus> statuses);
    
    /**
     * Find orders by date range and status list
     */
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate AND o.status IN :statuses ORDER BY o.createdAt DESC")
    List<Order> findByCreatedAtBetweenAndStatusIn(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("statuses") List<OrderStatus> statuses
    );
    
    /**
     * Find top 10 most recent orders
     */
    List<Order> findTop10ByOrderByCreatedAtDesc();
    
    /**
     * Find top 20 most recent orders for dashboard activities
     */
    List<Order> findTop20ByOrderByCreatedAtDesc();
    
    /**
     * Find orders created today
     */
    @Query("SELECT o FROM Order o WHERE DATE(o.createdAt) = CURRENT_DATE AND o.status IN :statuses")
    List<Order> findTodayOrdersByStatus(@Param("statuses") List<OrderStatus> statuses);
    
    /**
     * Find orders created this month
     */
    @Query("SELECT o FROM Order o WHERE MONTH(o.createdAt) = MONTH(CURRENT_DATE) AND YEAR(o.createdAt) = YEAR(CURRENT_DATE) AND o.status IN :statuses")
    List<Order> findThisMonthOrdersByStatus(@Param("statuses") List<OrderStatus> statuses);
    
    // Methods with EntityGraph to eagerly load bukti pembayaran
    
    /**
     * Find order by ID with bukti pembayaran eagerly loaded
     */
    @EntityGraph(attributePaths = {"bukti"})
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdWithBukti(@Param("id") Long id);
    
    /**
     * Find order by order number with bukti pembayaran eagerly loaded
     */
    @EntityGraph(attributePaths = {"bukti"})
    @Query("SELECT o FROM Order o WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberWithBukti(@Param("orderNumber") String orderNumber);
    
    /**
     * Find all orders with bukti pembayaran eagerly loaded (for admin)
     */
    @EntityGraph(attributePaths = {"bukti"})
    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC")
    List<Order> findAllWithBukti();
    
    /**
     * Find orders by status with bukti pembayaran eagerly loaded (for admin)
     */
    @EntityGraph(attributePaths = {"bukti"})
    @Query("SELECT o FROM Order o WHERE o.status IN :statuses ORDER BY o.createdAt DESC")
    List<Order> findByStatusInWithBukti(@Param("statuses") List<OrderStatus> statuses);
    
    /**
     * Find orders by user with bukti pembayaran eagerly loaded
     */
    @EntityGraph(attributePaths = {"bukti"})
    @Query("SELECT o FROM Order o WHERE o.userId = :userId ORDER BY o.createdAt DESC")
    List<Order> findByUserIdWithBukti(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"items", "items.product"})
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN o.items items WHERE o.createdAt BETWEEN :startDate AND :endDate AND o.status IN :statuses ORDER BY o.createdAt DESC")
    List<Order> findByCreatedAtBetweenAndStatusInWithItems(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("statuses") List<OrderStatus> statuses
    );
}
