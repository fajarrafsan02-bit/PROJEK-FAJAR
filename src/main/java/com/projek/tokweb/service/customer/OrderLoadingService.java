package com.projek.tokweb.service.customer;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projek.tokweb.models.customer.Order;
import com.projek.tokweb.models.customer.OrderStatus;
import com.projek.tokweb.repository.customer.OrderRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Service khusus untuk menangani lazy loading Order dengan BuktiPembayaran
 */
@Service
@Slf4j
public class OrderLoadingService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    /**
     * Find order by ID dengan bukti pembayaran dimuat
     */
    @Transactional(readOnly = true)
    public Optional<Order> findByIdWithBukti(Long id) {
        log.debug("🔍 Loading order {} with bukti pembayaran", id);
        return orderRepository.findByIdWithBukti(id);
    }
    
    /**
     * Find order by order number dengan bukti pembayaran dimuat
     */
    @Transactional(readOnly = true)
    public Optional<Order> findByOrderNumberWithBukti(String orderNumber) {
        log.debug("🔍 Loading order {} with bukti pembayaran", orderNumber);
        return orderRepository.findByOrderNumberWithBukti(orderNumber);
    }
    
    /**
     * Find orders by user dengan bukti pembayaran dimuat
     */
    @Transactional(readOnly = true)
    public List<Order> findByUserIdWithBukti(Long userId) {
        log.debug("🔍 Loading orders for user {} with bukti pembayaran", userId);
        return orderRepository.findByUserIdWithBukti(userId);
    }
    
    /**
     * Find orders by status dengan bukti pembayaran dimuat (untuk admin)
     */
    @Transactional(readOnly = true)
    public List<Order> findByStatusWithBukti(List<OrderStatus> statuses) {
        log.debug("🔍 Loading orders with status {} and bukti pembayaran", statuses);
        return orderRepository.findByStatusInWithBukti(statuses);
    }
    
    /**
     * Find all orders dengan bukti pembayaran dimuat (untuk admin)
     */
    @Transactional(readOnly = true)
    public List<Order> findAllWithBukti() {
        log.debug("🔍 Loading all orders with bukti pembayaran");
        return orderRepository.findAllWithBukti();
    }
    
    /**
     * Force load bukti pembayaran untuk existing order (alternative method)
     * Gunakan ini jika order sudah dimuat tapi bukti belum
     */
    @Transactional(readOnly = true)
    public Order loadBuktiForOrder(Order order) {
        if (order == null) {
            return null;
        }
        
        log.debug("🔄 Force loading bukti for order {}", order.getOrderNumber());
        
        // Force trigger lazy loading dalam transaction
        if (order.getBukti() != null) {
            // Access property to trigger lazy loading
            order.getBukti().getId();
        }
        
        return order;
    }
    
    /**
     * Check if order has bukti pembayaran
     */
    @Transactional(readOnly = true)
    public boolean hasBuktiPembayaran(Long orderId) {
        Optional<Order> orderOpt = orderRepository.findByIdWithBukti(orderId);
        return orderOpt.isPresent() && orderOpt.get().getBukti() != null;
    }
    
    /**
     * Check if order has bukti pembayaran by order number
     */
    @Transactional(readOnly = true)
    public boolean hasBuktiPembayaran(String orderNumber) {
        Optional<Order> orderOpt = orderRepository.findByOrderNumberWithBukti(orderNumber);
        return orderOpt.isPresent() && orderOpt.get().getBukti() != null;
    }
}
