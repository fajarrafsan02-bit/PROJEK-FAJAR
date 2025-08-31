package com.projek.tokweb.controller.user;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projek.tokweb.dto.ApiResponse;
// <<<<<<< HEAD
// import com.projek.tokweb.models.customer.Order;
// =======
import com.projek.tokweb.models.customer.Order;
// >>>>>>> 60deefb2408e43e20270b07dc90cb285629c2af4
import com.projek.tokweb.repository.customer.OrderRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;

    @GetMapping("/{orderId}/status")
    public ResponseEntity<?> getOrderStatus(@PathVariable Long orderId) {
        try {
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            
            if (orderOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Order tidak ditemukan dengan ID: " + orderId));
            }

            Order order = orderOpt.get();
            String status = order.getStatus().name();
            
            return ResponseEntity.ok(ApiResponse.success("Order status retrieved", 
                Map.of("status", status, "orderId", orderId)));
                
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserOrders(@PathVariable Long userId) {
        try {
// <<<<<<< HEAD
            // Gunakan method repository yang baru ditambahkan
            // List<Order> userOrders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
// =======
//             // Use findAll for now, implement proper user filtering later
            List<Order> userOrders = orderRepository.findAll();
// >>>>>>> 60deefb2408e43e20270b07dc90cb285629c2af4
            
            return ResponseEntity.ok(ApiResponse.success("User orders retrieved", 
                Map.of("orders", userOrders, "total", userOrders.size())));
                
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderDetail(@PathVariable Long orderId) {
        try {
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            
            if (orderOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Order tidak ditemukan dengan ID: " + orderId));
            }

            Order order = orderOpt.get();
            
            return ResponseEntity.ok(ApiResponse.success("Order detail retrieved", order));
                
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
