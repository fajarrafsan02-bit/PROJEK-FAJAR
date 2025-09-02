package com.projek.tokweb.service.customer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projek.tokweb.dto.customer.CheckoutRequest;
import com.projek.tokweb.models.User;
import com.projek.tokweb.models.admin.Product;
import com.projek.tokweb.models.customer.Order;
import com.projek.tokweb.models.customer.OrderItem;
import com.projek.tokweb.models.customer.OrderStatus;
import com.projek.tokweb.models.customer.PaymentTransaction;
import com.projek.tokweb.models.customer.PaymentStatus;
import com.projek.tokweb.repository.admin.ProductRepository;
import com.projek.tokweb.repository.customer.OrderRepository;
import com.projek.tokweb.repository.customer.OrderItemRepository;
import com.projek.tokweb.repository.customer.PaymentTransactionRepository;
import com.projek.tokweb.repository.customer.CartRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CheckoutService {
    
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final CartRepository cartRepository;

    @Transactional
    public Map<String, Object> createOrder(CheckoutRequest req, User user) {
        try {
            System.out.println("=== CREATING ORDER ===");
            System.out.println("Request: " + req);
            
            if (req.getItems() == null || req.getItems().isEmpty()) {
                throw new IllegalArgumentException("Tidak ada item untuk dibeli.");
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expires = now.plusHours(24); // 24 hours payment window

            // Build shipping address from customer info
            String shippingAddress = buildShippingAddress(req.getCustomerInfo());

            // Create order
            Order order = Order.builder()
                    .orderNumber("ORD-" + System.currentTimeMillis())
                    .userId(user.getId())
                    .shippingAddress(shippingAddress)
                    .customerName(req.getCustomerInfo().getFullName())
                    .customerPhone(req.getCustomerInfo().getPhone())
                    .customerEmail(req.getCustomerInfo().getEmail())
                    .paymentMethod(req.getPaymentMethod())
                    .status(OrderStatus.PENDING_PAYMENT)
                    .createdAt(now)
                    .expiresAt(expires)
                    .isReservedStock(false) // No stock reservation for now
                    .build();

            BigDecimal total = BigDecimal.ZERO;
            List<OrderItem> orderItems = new ArrayList<>();

            // Process each item
            for (CheckoutRequest.Item item : req.getItems()) {
                System.out.println("Processing item - ProductId: " + item.getProductId() + ", Quantity: " + item.getQuantity());

                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(() -> new IllegalArgumentException("Produk tidak ditemukan: " + item.getProductId()));

                if (!product.getIsActive()) {
                    throw new IllegalStateException("Produk tidak aktif: " + product.getName());
                }

                System.out.println("Product found: " + product.getName() + " (ID: " + product.getId() + ")");

                BigDecimal unitPrice = product.getFinalPrice() != null ? 
                    BigDecimal.valueOf(product.getFinalPrice()) : BigDecimal.ZERO;
                BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

                OrderItem orderItem = OrderItem.builder()
                        .order(order)
                        .product(product)
                        .quantity(item.getQuantity())
                        .unitPrice(unitPrice)
                        .subtotal(subtotal)
                        .build();

                orderItems.add(orderItem);
                total = total.add(subtotal);
            }

            order.setTotalAmount(total);

            // Set order items with proper relationship
            for (OrderItem item : orderItems) {
                item.setOrder(order);
                System.out.println("📦 Prepared order item: " + item.getProduct().getName() + " (Qty: " + item.getQuantity() + ")");
            }
            order.setItems(orderItems);

            // Save order with cascade - this will also save all order items
            Order savedOrder = orderRepository.save(order);
            System.out.println("✅ Saved order with " + orderItems.size() + " items");
            System.out.println("📦 Order ID: " + savedOrder.getId() + ", Order Number: " + savedOrder.getOrderNumber());

            // Verify that items were saved by checking the saved order
            if (savedOrder.getItems() != null) {
                System.out.println("✅ Order items saved successfully: " + savedOrder.getItems().size() + " items");
                for (OrderItem item : savedOrder.getItems()) {
                    System.out.println("   - Item ID: " + item.getId() + ", Product: " + item.getProduct().getName() + ", Qty: " + item.getQuantity());
                }
            } else {
                System.out.println("⚠️ Order items list is null after save");
            }

            // Create payment transaction
            PaymentTransaction transaction = PaymentTransaction.builder()
                    .externalPaymentId("ORDER-" + savedOrder.getId() + "-" + System.currentTimeMillis())
                    .order(savedOrder)
                    .amount(savedOrder.getTotalAmount())
                    .paymentMethod(savedOrder.getPaymentMethod())
                    .status(PaymentStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            PaymentTransaction savedTransaction = paymentTransactionRepository.save(transaction);

            // Clear cart after successful order creation
            clearUserCartAfterCheckout(user.getId());

            // Create payment instruction
            Map<String, Object> paymentData = createPaymentInstruction(savedOrder, savedTransaction);

            System.out.println("=== ORDER CREATED SUCCESSFULLY ===");
            System.out.println("Order ID: " + savedOrder.getId());
            System.out.println("External ID: " + savedTransaction.getExternalPaymentId());

            return paymentData;

        } catch (Exception e) {
            System.out.println("=== ORDER CREATION ERROR ===");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private String buildShippingAddress(CheckoutRequest.CustomerInfo customerInfo) {
        if (customerInfo == null) {
            return "Alamat tidak tersedia";
        }

        try {
            String address = customerInfo.getAddress() != null ? customerInfo.getAddress() : "";
            String city = customerInfo.getCity() != null ? customerInfo.getCity() : "";
            String postalCode = customerInfo.getPostalCode() != null ? customerInfo.getPostalCode() : "";

            String fullAddress = String.format("%s, %s %s", address, city, postalCode).trim();
            return fullAddress.isEmpty() ? "Alamat tidak tersedia" : fullAddress;
        } catch (Exception e) {
            return "Alamat tidak tersedia";
        }
    }

    private Map<String, Object> createPaymentInstruction(Order order, PaymentTransaction transaction) {
        Map<String, Object> data = new HashMap<>();
        
        data.put("externalId", transaction.getExternalPaymentId());
        data.put("amount", order.getTotalAmount().doubleValue());
        data.put("paymentMethod", order.getPaymentMethod());
        data.put("customerName", order.getCustomerName());
        data.put("customerEmail", order.getCustomerEmail());

        if ("QR_CODE".equals(order.getPaymentMethod())) {
            // Generate QR code data untuk pembayaran (format EMV QR)
            String qrData = String.format(
                "00020101021226620014ID.CO.QRIS.WWW01189360091408123456789015201123456789010303UEN52045IDR5303360540%.0f5802ID6304%s",
                order.getTotalAmount().doubleValue(),
                generateChecksum(order.getTotalAmount().doubleValue())
            );
            
            data.put("qrData", qrData);
            data.put("instructions", "Scan QR code untuk melakukan pembayaran. Setelah bayar, upload bukti pembayaran.");
            data.put("paymentAmount", order.getTotalAmount().doubleValue());
            
        } else if ("BANK_TRANSFER".equals(order.getPaymentMethod())) {
            data.put("bankName", "Bank Central Asia (BCA)");
            data.put("bankAccountNumber", "123456789");
            data.put("accountHolderName", "PT. FAJAR GOLD INDONESIA");
            data.put("instructions", "Transfer ke rekening di atas sesuai jumlah tagihan: Rp " + 
                String.format("%,.0f", order.getTotalAmount().doubleValue()) + ". Setelah bayar, upload bukti pembayaran.");
        }

        return data;
    }

    private String generateChecksum(double amount) {
        // Checksum sederhana untuk demo
        String amountStr = String.format("%.0f", amount);
        int checksum = 0;
        for (char c : amountStr.toCharArray()) {
            checksum += Character.getNumericValue(c);
        }
        return String.format("%04d", checksum % 10000);
    }

    @Transactional
    public void updatePaymentProof(String externalId, String fileName) {
        PaymentTransaction transaction = paymentTransactionRepository.findByExternalPaymentId(externalId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction tidak ditemukan: " + externalId));

        // Update transaction with payment proof
        transaction.setRawPayload(fileName);
        transaction.setStatus(PaymentStatus.PROCESSING);
        paymentTransactionRepository.save(transaction);

        // Update order status to PENDING_CONFIRMATION (belum konfirmasi)
        Order order = transaction.getOrder();
        order.setStatus(OrderStatus.PENDING_CONFIRMATION);
        orderRepository.save(order);

        System.out.println("Payment proof updated for external ID: " + externalId);
        System.out.println("Order status changed to: " + OrderStatus.PENDING_CONFIRMATION.getDisplayName());
    }

    @Transactional
    public void confirmPayment(String externalId) {
        PaymentTransaction transaction = paymentTransactionRepository.findByExternalPaymentId(externalId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction tidak ditemukan: " + externalId));

        Order order = transaction.getOrder();

        // Validate status transition
        if (!order.getStatus().canTransitionTo(OrderStatus.PAID)) {
            throw new IllegalStateException("Tidak dapat mengkonfirmasi pembayaran. Status saat ini: " + order.getStatus().getDisplayName());
        }

        // Update transaction status
        transaction.setStatus(PaymentStatus.SUCCESS);
        paymentTransactionRepository.save(transaction);

        // Update order status to PAID (sudah dibayar)
        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);

        System.out.println("Payment confirmed for order: " + order.getOrderNumber());
        System.out.println("Order status changed to: " + OrderStatus.PAID.getDisplayName());
    }

    @Transactional
    public void startProcessing(String externalId) {
        PaymentTransaction transaction = paymentTransactionRepository.findByExternalPaymentId(externalId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction tidak ditemukan: " + externalId));

        Order order = transaction.getOrder();

        // Validate status transition
        if (!order.getStatus().canTransitionTo(OrderStatus.PROCESSING)) {
            throw new IllegalStateException("Tidak dapat memulai pemrosesan. Status saat ini: " + order.getStatus().getDisplayName());
        }

        // Update order status to PROCESSING (sedang diproses)
        order.setStatus(OrderStatus.PROCESSING);
        orderRepository.save(order);

        System.out.println("Order processing started for: " + order.getOrderNumber());
        System.out.println("Order status changed to: " + OrderStatus.PROCESSING.getDisplayName());
    }

    @Transactional
    public void markAsShipped(String externalId, String trackingNumber) {
        PaymentTransaction transaction = paymentTransactionRepository.findByExternalPaymentId(externalId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction tidak ditemukan: " + externalId));

        Order order = transaction.getOrder();

        // Validate status transition
        if (!order.getStatus().canTransitionTo(OrderStatus.SHIPPED)) {
            throw new IllegalStateException("Tidak dapat menandai sebagai dikirim. Status saat ini: " + order.getStatus().getDisplayName());
        }

        // Update order status to SHIPPED (dikirim)
        order.setStatus(OrderStatus.SHIPPED);
        if (trackingNumber != null && !trackingNumber.trim().isEmpty()) {
            order.setNotes("Nomor Resi: " + trackingNumber + (order.getNotes() != null ? "\n" + order.getNotes() : ""));
        }
        orderRepository.save(order);

        System.out.println("Order marked as shipped: " + order.getOrderNumber());
        System.out.println("Order status changed to: " + OrderStatus.SHIPPED.getDisplayName());
        if (trackingNumber != null) {
            System.out.println("Tracking number: " + trackingNumber);
        }
    }

    @Transactional
    public void markAsDelivered(String externalId) {
        PaymentTransaction transaction = paymentTransactionRepository.findByExternalPaymentId(externalId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction tidak ditemukan: " + externalId));

        Order order = transaction.getOrder();

        // Validate status transition
        if (!order.getStatus().canTransitionTo(OrderStatus.DELIVERED)) {
            throw new IllegalStateException("Tidak dapat menandai sebagai terkirim. Status saat ini: " + order.getStatus().getDisplayName());
        }

        // Update order status to DELIVERED (terkirim)
        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);

        System.out.println("Order marked as delivered: " + order.getOrderNumber());
        System.out.println("Order status changed to: " + OrderStatus.DELIVERED.getDisplayName());
    }

    @Transactional
    public void cancelOrder(String externalId, String reason) {
        PaymentTransaction transaction = paymentTransactionRepository.findByExternalPaymentId(externalId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction tidak ditemukan: " + externalId));

        Order order = transaction.getOrder();

        // Validate status transition
        if (!order.getStatus().canTransitionTo(OrderStatus.CANCELLED)) {
            throw new IllegalStateException("Tidak dapat membatalkan pesanan. Status saat ini: " + order.getStatus().getDisplayName());
        }

        // Update transaction status
        transaction.setStatus(PaymentStatus.CANCELLED);
        paymentTransactionRepository.save(transaction);

        // Update order status to CANCELLED (dibatalkan)
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        if (reason != null && !reason.trim().isEmpty()) {
            order.setNotes("Dibatalkan: " + reason + (order.getNotes() != null ? "\n" + order.getNotes() : ""));
        }
        orderRepository.save(order);

        System.out.println("Order cancelled: " + order.getOrderNumber());
        System.out.println("Order status changed to: " + OrderStatus.CANCELLED.getDisplayName());
        if (reason != null) {
            System.out.println("Cancellation reason: " + reason);
        }
    }

    /**
     * Clear user's cart after successful checkout
     */
    private void clearUserCartAfterCheckout(Long userId) {
        try {
            System.out.println("🗑️ Clearing cart for user ID: " + userId + " after successful checkout");

            // Find user's cart
            var cartOptional = cartRepository.findByUserId(userId);

            if (cartOptional.isPresent()) {
                var cart = cartOptional.get();
                System.out.println("📦 Found cart with " + cart.getItems().size() + " items");

                // Clear all items from cart
                cart.getItems().clear();

                // Save the updated cart (items will be deleted due to orphanRemoval = true)
                cartRepository.save(cart);

                System.out.println("✅ Cart cleared successfully for user ID: " + userId);
            } else {
                System.out.println("ℹ️ No cart found for user ID: " + userId);
            }

        } catch (Exception e) {
            System.out.println("❌ Error clearing cart for user ID: " + userId);
            e.printStackTrace();
            // Don't throw exception here as the order was already created successfully
            // Just log the error and continue
        }
    }
}