// package com.projek.tokweb.repository.customer;

// import java.time.LocalDateTime;
// import java.util.List;
// import java.util.Optional;

// import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.stereotype.Repository;

// import com.projek.tokweb.models.customer.Order;
// import com.projek.tokweb.models.customer.OrderStatus;

// @Repository
// public interface OrderRepository extends JpaRepository<Order, Long> {
//     List<Order> findByStatusAndExpiresAtBefore(OrderStatus status, LocalDateTime time);
    
//     // Tambahkan method baru
//     List<Order> findByUserId(Long userId);
    
//     List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    
//     List<Order> findByStatus(OrderStatus status);

//     // Optional<Order> findById(Long id);

// }