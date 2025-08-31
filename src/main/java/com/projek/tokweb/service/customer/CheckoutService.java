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
import com.projek.tokweb.repository.customer.PaymentTransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CheckoutService {
    
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

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
                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(() -> new IllegalArgumentException("Produk tidak ditemukan: " + item.getProductId()));

                if (!product.getIsActive()) {
                    throw new IllegalStateException("Produk tidak aktif: " + product.getName());
                }

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
            Order savedOrder = orderRepository.save(order);
            
            // Save order items
            for (OrderItem item : orderItems) {
                item.setOrder(savedOrder);
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

        // Update order status
        Order order = transaction.getOrder();
        order.setStatus(OrderStatus.PENDING_CONFIRMATION);
        orderRepository.save(order);
        
        System.out.println("Payment proof updated for external ID: " + externalId);
    }
}