package com.projek.tokweb.repository.customer;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projek.tokweb.models.customer.Order;
import com.projek.tokweb.models.customer.PaymentTransaction;
import com.projek.tokweb.models.customer.PaymentStatus;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    
    List<PaymentTransaction> findByOrder(Order order);
    
    List<PaymentTransaction> findByOrderOrderByCreatedAtDesc(Order order);
    
    Optional<PaymentTransaction> findByExternalPaymentId(String externalPaymentId);
    
    List<PaymentTransaction> findByStatus(PaymentStatus status);
    
    List<PaymentTransaction> findByOrderAndStatus(Order order, PaymentStatus status);
}