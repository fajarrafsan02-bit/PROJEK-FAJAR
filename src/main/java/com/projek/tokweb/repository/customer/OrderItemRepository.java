package com.projek.tokweb.repository.customer;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projek.tokweb.models.customer.Order;
import com.projek.tokweb.models.customer.OrderItem;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    List<OrderItem> findByOrder(Order order);
    
    List<OrderItem> findByOrderOrderById(Order order);
    
    List<OrderItem> findByOrderId(Long orderId);
    
    void deleteByOrder(Order order);
}