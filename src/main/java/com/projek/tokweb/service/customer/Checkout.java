// package com.projek.tokweb.service.customer;

// import java.time.LocalDateTime;

// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import com.projek.tokweb.config.config.CheckoutProperties;
// import com.projek.tokweb.dto.customer.CartItemDto;
// import com.projek.tokweb.dto.customer.CheckoutRequest;
// import com.projek.tokweb.dto.customer.PaymentInstruction;
// import com.projek.tokweb.models.User;
// import com.projek.tokweb.models.admin.Product;
// import com.projek.tokweb.models.customer.Order;
// import com.projek.tokweb.models.customer.OrderItem;
// import com.projek.tokweb.models.customer.OrderStatus;
// import com.projek.tokweb.models.customer.PaymentTransaction;
// import com.projek.tokweb.repository.admin.ProductRepository;
// import com.projek.tokweb.repository.customer.OrderRepository;
// import com.projek.tokweb.repository.customer.PaymentTransactionRepository;

// import lombok.RequiredArgsConstructor;

// @Service
// @RequiredArgsConstructor
// public class Checkout {
//     private final ProductRepository productRepository;
//     private final OrderRepository orderRepository;
//     private final PaymentTransactionRepository paymentTransactionRepository;
//     private final PaymentGatewayFactory gatewayFactory;
//     private final CheckoutProperties props;

//     /**
//      * Create order and optionally reserve stock; then call payment gateway to
//      * create payment.
//      */
//     @Transactional
//     public PaymentInstruction createOrderAndPayment(CheckoutRequest req, User user) {
//         if (req.getItems() == null || req.getItems().isEmpty()) {
//             throw new IllegalArgumentException("Tidak ada item untuk dibeli.");
//         }

//         boolean reserve = props.isReserveStock();
//         LocalDateTime now = LocalDateTime.now();
//         LocalDateTime expires = now.plusHours(props.getPaymentTtlHours());

//         System.out.println("expires : " + expires);

//         // Perbaiki: gunakan customerInfo untuk alamat pengiriman
//         String shippingAddress = "Alamat pengiriman"; // Default value
        
//         if (req.getCustomerInfo() != null) {
//             try {
//                 String address = req.getCustomerInfo().getAddress();
//                 String city = req.getCustomerInfo().getCity();
//                 String postalCode = req.getCustomerInfo().getPostalCode();
                
//                 shippingAddress = String.format("%s, %s, %s", 
//                     address != null ? address : "",
//                     city != null ? city : "",
//                     postalCode != null ? postalCode : ""
//                 ).trim();
//             } catch (Exception e) {
//                 shippingAddress = "Alamat tidak tersedia";
//             }
//         }

//         Order order = Order.builder()
//                 .user(user)
//                 .shippingAddress(shippingAddress) // Gunakan alamat yang sudah di-format
//                 .paymentMethod(req.getPaymentMethod())
//                 .status(OrderStatus.PENDING_PAYMENT)
//                 .createdAt(now)
//                 .expiresAt(expires)
//                 .reservedStock(reserve)
//                 .build();
        
//         System.out.println("expires 2 : " + order.getExpiresAt());
//         double total = 0.0;

//         for (CartItemDto item : req.getItems()) {
//             // Ganti findByIdForUpdate dengan findById jika method tidak ada
//             Product p = productRepository.findById(item.getProductId())
//                     .orElseThrow(() -> new IllegalArgumentException("Produk tidak ditemukan: " + item.getProductId()));

//             if (!p.getIsActive())
//                 throw new IllegalStateException("Produk tidak aktif: " + p.getName());

//             if (reserve) {
//                 if (p.getStock() < item.getQuantity())
//                     throw new IllegalStateException("Stok tidak cukup: " + p.getName());
//                 p.setStock(p.getStock() - item.getQuantity());
//                 productRepository.save(p);
//             }

//             double unitPrice = p.getFinalPrice() != null ? p.getFinalPrice() : 0.0;
//             double sub = unitPrice * item.getQuantity();

//             OrderItem oi = OrderItem.builder()
//                     .order(order)
//                     .productId(p.getId())
//                     .productName(p.getName())
//                     .unitPrice(unitPrice)
//                     .quantity(item.getQuantity())
//                     .subtotal(sub)
//                     .build();

//             order.getItems().add(oi);
//             total += sub;
//         }

//         order.setTotalAmount(total);
//         Order saved = orderRepository.save(order);

//         PaymentTransaction tx = PaymentTransaction.builder()
//                 .externalId(null)
//                 .orderId(saved.getId())
//                 .amount(saved.getTotalAmount())
//                 .currency("IDR")
//                 .method(saved.getPaymentMethod())
//                 .status("PENDING")
//                 .createdAt(LocalDateTime.now())
//                 .rawPayload(null)
//                 .build();
//         paymentTransactionRepository.save(tx);

//         PaymentGateway gateway = gatewayFactory.getGateway();
//         String pm = saved.getPaymentMethod();
//         PaymentInstruction instr = gateway.createPayment(saved, pm);

//         // save external id/instruction to tx
//         tx.setExternalId(instr.getExternalId());
//         tx.setRawPayload(instr.getRawJson());
//         paymentTransactionRepository.save(tx);

//         return instr;
//     }

//     /**
//      * Called by webhook when payment is successful.
//      */
//     @Transactional
//     public void markOrderPaid(Long orderId, String externalPaymentId, String method, String rawPayload) {
//         Order order = orderRepository.findById(orderId)
//                 .orElseThrow(() -> new IllegalArgumentException("Order tidak ditemukan: " + orderId));

//         if (!OrderStatus.PENDING_PAYMENT.equals(order.getStatus()))
//             return;

//         // if reservedStock==false, deduct now
//         if (!order.isReservedStock()) {
//             for (OrderItem item : order.getItems()) {
//                 Product p = productRepository.findByIdForUpdate(item.getProductId())
//                         .orElseThrow(() -> new IllegalArgumentException(
//                                 "Produk tidak ditemukan saat payment confirmation: " + item.getProductId()));

//                 if (p.getStock() < item.getQuantity()) {
//                     throw new IllegalStateException("Stok tidak cukup saat konfirmasi pembayaran: " + p.getName());
//                 }
//                 p.setStock(p.getStock() - item.getQuantity());
//                 productRepository.save(p);
//             }
//         }

//         // save payment transaction if not exists
//         paymentTransactionRepository.findByExternalId(externalPaymentId).orElseGet(() -> {
//             PaymentTransaction tx = PaymentTransaction.builder()
//                     .externalId(externalPaymentId)
//                     .orderId(order.getId())
//                     .amount(order.getTotalAmount())
//                     .currency("IDR")
//                     .method(method)
//                     .status("SUCCESS")
//                     .rawPayload(rawPayload)
//                     .createdAt(LocalDateTime.now())
//                     .build();
//             return paymentTransactionRepository.save(tx);
//         });

//         order.setStatus(OrderStatus.PAID);
//         orderRepository.save(order);
//         // further async: enqueue fulfilment, send email, etc.
//     }

//     /**
//      * Cancel order and restore stock if reserved on checkout.
//      */
//     @Transactional
//     public void cancelOrder(Long orderId, String reason) {
//         Order order = orderRepository.findById(orderId)
//                 .orElseThrow(() -> new IllegalArgumentException("Order tidak ditemukan: " + orderId));

//         if (OrderStatus.CANCELLED.equals(order.getStatus()) || OrderStatus.PAID.equals(order.getStatus()))
//             return;

//         if (order.isReservedStock()) {
//             for (OrderItem item : order.getItems()) {
//                 productRepository.findByIdForUpdate(item.getProductId()).ifPresent(p -> {
//                     p.setStock(p.getStock() + item.getQuantity());
//                     productRepository.save(p);
//                 });
//             }
//         }

//         order.setStatus(OrderStatus.CANCELLED);
//         orderRepository.save(order);
//     }
// }
