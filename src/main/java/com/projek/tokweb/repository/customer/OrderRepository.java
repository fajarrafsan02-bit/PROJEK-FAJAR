package com.projek.tokweb.repository.customer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
}