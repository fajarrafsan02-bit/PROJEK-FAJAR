package com.projek.tokweb.service.customer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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
import com.projek.tokweb.repository.customer.CartItemRepository;
import com.projek.tokweb.repository.customer.BuktiPembayaranRepository;
import com.projek.tokweb.models.customer.Cart;
import java.util.Optional;
import com.projek.tokweb.models.customer.BuktiPembayaran;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.io.IOException;

import com.projek.tokweb.models.activity.ActivityType;
import com.projek.tokweb.service.activity.ActivityLogService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CheckoutService {
    
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final BuktiPembayaranRepository buktiPembayaranRepository;
    private final ActivityLogService activityLogService;
    
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createOrder(CheckoutRequest req, User user) {
        System.out.println("=== CREATING ORDER ===");
        System.out.println("Request: " + req);
        
        // PERBAIKAN 1: Validasi input yang lebih robust
        if (req == null) {
            System.out.println("❌ CheckoutRequest is null");
            throw new IllegalArgumentException("Data checkout tidak boleh kosong");
        }
        
        if (req.getItems() == null || req.getItems().isEmpty()) {
            System.out.println("❌ No items to checkout");
            throw new IllegalArgumentException("Tidak ada item untuk dibeli");
        }
        
        if (req.getCustomerInfo() == null) {
            System.out.println("❌ Customer info is null");
            throw new IllegalArgumentException("Data customer tidak lengkap");
        }
        
        if (user == null || user.getId() == null) {
            System.out.println("❌ User is null or user ID is null");
            throw new IllegalArgumentException("User tidak valid");
        }
        
        System.out.println("✅ Input validation passed");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expires = now.plusHours(24); // 24 hours payment window

        // Build shipping address from customer info
        String shippingAddress = buildShippingAddress(req.getCustomerInfo());
        System.out.println("📍 Shipping address: " + shippingAddress);

        // PERBAIKAN 2: Validasi products dan hitung total dengan error handling yang lebih baik
        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        
        System.out.println("🔍 Processing " + req.getItems().size() + " items...");
        
        for (int i = 0; i < req.getItems().size(); i++) {
            CheckoutRequest.Item item = req.getItems().get(i);
            System.out.println("Processing item " + (i+1) + "/" + req.getItems().size() + ": ProductId=" + item.getProductId() + ", Quantity=" + item.getQuantity());

            if (item.getProductId() == null) {
                System.out.println("❌ Item " + (i+1) + " has null product ID");
                throw new IllegalArgumentException("Item ke-" + (i+1) + " tidak memiliki ID produk");
            }
            
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                System.out.println("❌ Item " + (i+1) + " has invalid quantity: " + item.getQuantity());
                throw new IllegalArgumentException("Item ke-" + (i+1) + " memiliki quantity tidak valid");
            }

            try {
                // Lock product row to prevent oversell during concurrent checkouts
                Product product = productRepository.findByIdForUpdate(item.getProductId())
                        .orElseThrow(() -> {
                            System.out.println("❌ Product not found: " + item.getProductId());
                            return new IllegalArgumentException("Produk tidak ditemukan: " + item.getProductId());
                        });

                if (!product.getIsActive()) {
                    System.out.println("❌ Product not active: " + product.getName());
                    throw new IllegalStateException("Produk tidak aktif: " + product.getName());
                }
                
                if (product.getFinalPrice() == null) {
                    System.out.println("❌ Product has null final price: " + product.getName());
                    throw new IllegalStateException("Produk tidak memiliki harga: " + product.getName());
                }
                
                // Validasi ketersediaan stok (tidak mengurangi dulu, tunggu konfirmasi admin)
                int available = product.getStock();
                int qty = item.getQuantity();
                if (qty > available) {
                    System.out.println("❌ Stok tidak cukup untuk produk: " + product.getName() + " | Diminta: " + qty + ", Tersedia: " + available);
                    throw new IllegalStateException("Stok tidak cukup untuk produk '" + product.getName() + "'. Tersedia: " + available + ", diminta: " + qty);
                }
                // TIDAK MENGURANGI STOCK SAAT CHECKOUT - tunggu admin konfirmasi
                System.out.println("✅ Stock tersedia untuk: " + product.getName() + " | Diminta: " + qty + ", Tersedia: " + available);
                
                System.out.println("✅ Product found: " + product.getName() + " (ID: " + product.getId() + ", Price: " + product.getFinalPrice() + ")");

                BigDecimal unitPrice = BigDecimal.valueOf(product.getFinalPrice());
                BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(qty));

                // PERBAIKAN 3: Buat order item tanpa circular reference dulu
                OrderItem orderItem = OrderItem.builder()
                        .product(product)
                        .quantity(qty)
                        .unitPrice(unitPrice)
                        .subtotal(subtotal)
                        .build();

                orderItems.add(orderItem);
                total = total.add(subtotal);
                
                System.out.println("✅ Item " + (i+1) + " processed successfully: " + product.getName() + " x" + qty + " = " + subtotal);
                
            } catch (Exception e) {
                System.out.println("❌ Error processing item " + (i+1) + ": " + e.getMessage());
                throw new IllegalStateException("Gagal memproses item ke-" + (i+1) + ": " + e.getMessage(), e);
            }
        }
        
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("❌ Total amount is zero or negative: " + total);
            throw new IllegalStateException("Total pesanan tidak valid: " + total);
        }
        
        System.out.println("✅ All items processed successfully. Total: " + total);

        // PERBAIKAN 4: Create order dengan validation yang lebih ketat
        Order order;
        try {
            order = Order.builder()
                    .orderNumber("ORD-" + System.currentTimeMillis())
                    .userId(user.getId())
                    .shippingAddress(shippingAddress)
                    .customerName(req.getCustomerInfo().getFullName())
                    .customerPhone(req.getCustomerInfo().getPhone())
                    .customerEmail(req.getCustomerInfo().getEmail())
                    .paymentMethod(req.getPaymentMethod())
                    .status(OrderStatus.PENDING_PAYMENT)
                    .totalAmount(total)
                    .createdAt(now)
                    .expiresAt(expires)
                    .isReservedStock(false)
                    .build();
            System.out.println("✅ Order object created successfully");
        } catch (Exception e) {
            System.out.println("❌ Error creating order object: " + e.getMessage());
            throw new IllegalStateException("Gagal membuat order: " + e.getMessage(), e);
        }

        // PERBAIKAN 5: Set bidirectional relationship dengan proper error handling
        try {
            for (OrderItem item : orderItems) {
                item.setOrder(order);
                System.out.println("🔗 Set order relationship for item: " + item.getProduct().getName());
            }
            order.setItems(orderItems);
            System.out.println("✅ Bidirectional relationships set successfully");
        } catch (Exception e) {
            System.out.println("❌ Error setting relationships: " + e.getMessage());
            throw new IllegalStateException("Gagal mengatur relasi order-item: " + e.getMessage(), e);
        }

        // PERBAIKAN 6: Save dengan explicit error handling
        Order savedOrder;
        try {
            System.out.println("💾 Saving order...");
            savedOrder = orderRepository.save(order);
            System.out.println("✅ Order saved successfully with ID: " + savedOrder.getId());
            
            // Validate saved order
            if (savedOrder.getId() == null) {
                throw new IllegalStateException("Order tidak tersimpan dengan benar - ID null");
            }
            
            if (savedOrder.getItems() == null || savedOrder.getItems().isEmpty()) {
                System.out.println("⚠️ Order saved but items list is empty, checking manually...");
                // Refresh the order to get items
                savedOrder = orderRepository.findById(savedOrder.getId())
                    .orElseThrow(() -> new IllegalStateException("Order tidak ditemukan setelah save"));
            }
            
            System.out.println("✅ Order validation passed. Items count: " + 
                (savedOrder.getItems() != null ? savedOrder.getItems().size() : "NULL"));
            
        } catch (Exception e) {
            System.out.println("❌ Error saving order: " + e.getMessage());
            e.printStackTrace();
            throw new IllegalStateException("Gagal menyimpan pesanan: " + e.getMessage(), e);
        }

        // PERBAIKAN 7: Create payment transaction dengan validation
        PaymentTransaction savedTransaction;
        try {
            System.out.println("💳 Creating payment transaction...");
            PaymentTransaction transaction = PaymentTransaction.builder()
                    .externalPaymentId("ORDER-" + savedOrder.getId() + "-" + System.currentTimeMillis())
                    .order(savedOrder)
                    .amount(savedOrder.getTotalAmount())
                    .paymentMethod(savedOrder.getPaymentMethod())
                    .status(PaymentStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            savedTransaction = paymentTransactionRepository.save(transaction);
            System.out.println("✅ Payment transaction saved with External ID: " + savedTransaction.getExternalPaymentId());
            
        } catch (Exception e) {
            System.out.println("❌ Error creating payment transaction: " + e.getMessage());
            e.printStackTrace();
            throw new IllegalStateException("Gagal membuat transaksi pembayaran: " + e.getMessage(), e);
        }

        // PERBAIKAN 8: Clear cart SYNCHRONOUSLY dalam transaksi yang sama untuk memastikan konsistensi
        try {
            System.out.println("🗑️ [SYNC] Clearing cart items dalam transaksi yang sama...");
            debugCartContents(user.getId(), "BEFORE_CLEARING");
            clearUserCartAfterCheckout(user.getId(), req);
            debugCartContents(user.getId(), "AFTER_CLEARING");
            System.out.println("✅ [SYNC] Cart cleared successfully dalam transaksi utama");
        } catch (Exception e) {
            System.out.println("⚠️ [SYNC] Error clearing cart dalam transaksi utama: " + e.getMessage());
            // Log error tapi jangan lempar exception karena order sudah berhasil dibuat
            e.printStackTrace();
        }

        // Create final copies for lambda usage (effectively final requirement)
        final Order finalSavedOrder = savedOrder;
        final User finalUser = user;
        
        // PERBAIKAN 9: Activity log tetap async karena tidak kritis
        try {
            System.out.println("🤷 Scheduling async activity log...");
            CompletableFuture.runAsync(() -> {
                // Activity log
                try {
                    System.out.println("📝 [ASYNC] Logging activity...");
                    activityLogService.logActivity(
                        ActivityType.ORDER_NEW,
                        "Pesanan Baru Masuk",
                        String.format("Pesanan baru %s dari %s dengan total %s",
                            finalSavedOrder.getOrderNumber(),
                            finalSavedOrder.getCustomerName(),
                            String.format("Rp %.0f", finalSavedOrder.getTotalAmount().doubleValue())),
                        finalUser.getId() != null ? finalUser.getId().toString() : "USER",
                        finalSavedOrder.getCustomerName(),
                        "CUSTOMER"
                    );
                    System.out.println("✅ [ASYNC] Activity logged successfully");
                } catch (Exception e) {
                    System.out.println("⚠️ [ASYNC] Error logging activity (non-critical): " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.out.println("⚠️ Warning: Failed to schedule async activity log: " + e.getMessage());
        }

        // PERBAIKAN 10: Create payment instruction dengan error handling
        Map<String, Object> paymentData;
        try {
            System.out.println("💰 Creating payment instructions...");
            paymentData = createPaymentInstruction(savedOrder, savedTransaction);
            System.out.println("✅ Payment instructions created successfully");
        } catch (Exception e) {
            System.out.println("❌ Error creating payment instructions: " + e.getMessage());
            throw new IllegalStateException("Gagal membuat instruksi pembayaran: " + e.getMessage(), e);
        }
        
        System.out.println("=== ORDER CREATED SUCCESSFULLY ===");
        System.out.println("Order ID: " + savedOrder.getId());
        System.out.println("Order Number: " + savedOrder.getOrderNumber());
        System.out.println("External Payment ID: " + savedTransaction.getExternalPaymentId());
        System.out.println("Total Amount: " + savedOrder.getTotalAmount());
        System.out.println("Items Count: " + (savedOrder.getItems() != null ? savedOrder.getItems().size() : "NULL"));

        return paymentData;
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
        try {
            PaymentTransaction transaction = paymentTransactionRepository.findByExternalPaymentId(externalId)
                    .orElseThrow(() -> new IllegalArgumentException("Transaction tidak ditemukan: " + externalId));

            Order order = transaction.getOrder();
            
            // Read the uploaded file and save to database
            Path filePath = Paths.get("uploads/payment-proofs/", fileName);
            if (Files.exists(filePath)) {
                try {
                    byte[] fileData = Files.readAllBytes(filePath);
                    String contentType = Files.probeContentType(filePath);
                    if (contentType == null) {
                        // Default content type for images
                        contentType = "image/jpeg";
                    }
                    
                    // Save bukti pembayaran to database
                    BuktiPembayaran buktiPembayaran = BuktiPembayaran.builder()
                            .orderId(order.getId())
                            .fileName(fileName)
                            .contentType(contentType)
                            .fileData(fileData)
                            .build();
                    
                    BuktiPembayaran savedBukti = buktiPembayaranRepository.save(buktiPembayaran);
                    System.out.println("✅ Bukti pembayaran saved to database with ID: " + savedBukti.getId());
                    
                } catch (IOException e) {
                    System.out.println("❌ Error reading payment proof file: " + e.getMessage());
                    throw new RuntimeException("Gagal membaca file bukti pembayaran: " + e.getMessage());
                }
            } else {
                System.out.println("⚠️ Payment proof file not found: " + filePath.toString());
                throw new RuntimeException("File bukti pembayaran tidak ditemukan");
            }

            // Update transaction with payment proof filename reference
            transaction.setRawPayload(fileName);
            transaction.setStatus(PaymentStatus.PROCESSING);
            paymentTransactionRepository.save(transaction);

            // Update order status to PENDING_CONFIRMATION (belum konfirmasi)
            order.setStatus(OrderStatus.PENDING_CONFIRMATION);
            orderRepository.save(order);

            System.out.println("✅ Payment proof updated for external ID: " + externalId);
            System.out.println("✅ Order status changed to: " + OrderStatus.PENDING_CONFIRMATION.getDisplayName());
            
        } catch (Exception e) {
            System.out.println("❌ Error updating payment proof: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Gagal menyimpan bukti pembayaran: " + e.getMessage());
        }
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

        // PERBAIKAN: Kurangi stock produk ketika admin mengkonfirmasi pembayaran
        try {
            System.out.println("📉 [STOCK_REDUCTION] Starting stock reduction for confirmed order: " + order.getOrderNumber());
            reduceProductStockForOrder(order);
            System.out.println("✅ [STOCK_REDUCTION] Stock reduction completed successfully");
        } catch (Exception e) {
            System.out.println("❌ [STOCK_REDUCTION] Failed to reduce stock: " + e.getMessage());
            e.printStackTrace();
            throw new IllegalStateException("Gagal mengurangi stok produk: " + e.getMessage(), e);
        }

        // Update transaction status
        transaction.setStatus(PaymentStatus.SUCCESS);
        paymentTransactionRepository.save(transaction);

        // Update order status to PAID (sudah dibayar)
        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);
        
        // Log aktivitas: pesanan dikonfirmasi
        activityLogService.logActivity(
            ActivityType.ORDER_CONFIRMED,
            "Pesanan Dikonfirmasi",
            String.format("Pesanan %s dari %s telah dikonfirmasi pembayarannya", 
                order.getOrderNumber(),
                order.getCustomerName()),
            "ADMIN",
            "Admin System",
            "ADMIN"
        );

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
        
        // Log aktivitas: pesanan sedang diproses
        activityLogService.logActivity(
            ActivityType.ORDER_PROCESSING,
            "Pesanan Sedang Diproses",
            String.format("Pesanan %s dari %s sedang diproses", 
                order.getOrderNumber(),
                order.getCustomerName()),
            "ADMIN",
            "Admin System",
            "ADMIN"
        );

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
        
        // Log aktivitas: pesanan selesai
        activityLogService.logActivity(
            ActivityType.ORDER_COMPLETED,
            "Pesanan Selesai",
            String.format("Pesanan %s dari %s telah selesai dan diterima pelanggan", 
                order.getOrderNumber(),
                order.getCustomerName()),
            "ADMIN",
            "Admin System",
            "ADMIN"
        );

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

        // PERBAIKAN: Kembalikan stock jika order sudah PAID (stock sudah dikurangi)
        if (order.getStatus() == OrderStatus.PAID || 
            order.getStatus() == OrderStatus.PROCESSING || 
            order.getStatus() == OrderStatus.SHIPPED) {
            try {
                System.out.println("🔄 [STOCK_RESTORE] Restoring stock for cancelled order: " + order.getOrderNumber());
                restoreProductStockForCancelledOrder(order);
                System.out.println("✅ [STOCK_RESTORE] Stock restoration completed successfully");
            } catch (Exception e) {
                System.out.println("❌ [STOCK_RESTORE] Failed to restore stock: " + e.getMessage());
                e.printStackTrace();
                // Log error tapi jangan lempar exception, tetap lanjutkan pembatalan
            }
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
     * Reduce product stock when admin confirms order payment
     */
    @Transactional
    private void reduceProductStockForOrder(Order order) {
        if (order == null || order.getItems() == null || order.getItems().isEmpty()) {
            System.out.println("⚠️ [STOCK_REDUCTION] Order or order items is null/empty");
            return;
        }
        
        System.out.println("🔄 [STOCK_REDUCTION] Processing stock reduction for order: " + order.getOrderNumber());
        
        for (OrderItem orderItem : order.getItems()) {
            try {
                Product product = orderItem.getProduct();
                int orderedQuantity = orderItem.getQuantity();
                
                System.out.println("🔍 [STOCK_REDUCTION] Processing product: " + product.getName() + 
                                 " | Current stock: " + product.getStock() + 
                                 " | Ordered qty: " + orderedQuantity);
                
                // Lock product untuk mencegah race condition
                Product lockedProduct = productRepository.findByIdForUpdate(product.getId())
                        .orElseThrow(() -> new IllegalStateException("Product tidak ditemukan: " + product.getId()));
                
                int currentStock = lockedProduct.getStock();
                
                // Validasi stock masih mencukupi
                if (currentStock < orderedQuantity) {
                    System.out.println("❌ [STOCK_REDUCTION] Stock tidak mencukupi untuk: " + lockedProduct.getName() + 
                                     " | Tersedia: " + currentStock + ", Diminta: " + orderedQuantity);
                    throw new IllegalStateException(
                        String.format("Stock tidak mencukupi untuk produk '%s'. Tersedia: %d, Diminta: %d",
                            lockedProduct.getName(), currentStock, orderedQuantity));
                }
                
                // Kurangi stock
                int newStock = currentStock - orderedQuantity;
                lockedProduct.setStock(newStock);
                productRepository.save(lockedProduct);
                
                System.out.println("✅ [STOCK_REDUCTION] Stock updated for: " + lockedProduct.getName() + 
                                 " | " + currentStock + " -> " + newStock);
                
            } catch (Exception e) {
                System.out.println("❌ [STOCK_REDUCTION] Error reducing stock for product ID " + 
                                 orderItem.getProduct().getId() + ": " + e.getMessage());
                e.printStackTrace();
                throw new IllegalStateException("Gagal mengurangi stok untuk produk '" + 
                    orderItem.getProduct().getName() + "': " + e.getMessage(), e);
            }
        }
        
        // Force flush untuk memastikan perubahan stock ter-commit
        try {
            entityManager.flush();
            System.out.println("💾 [STOCK_REDUCTION] Stock changes flushed to database");
        } catch (Exception e) {
            System.out.println("⚠️ [STOCK_REDUCTION] Error flushing stock changes: " + e.getMessage());
            throw e;
        }
        
        System.out.println("🏁 [STOCK_REDUCTION] Stock reduction completed for order: " + order.getOrderNumber());
    }
    
    /**
     * Restore product stock when order is cancelled after payment confirmation
     */
    @Transactional
    private void restoreProductStockForCancelledOrder(Order order) {
        if (order == null || order.getItems() == null || order.getItems().isEmpty()) {
            System.out.println("⚠️ [STOCK_RESTORE] Order or order items is null/empty");
            return;
        }
        
        System.out.println("🔄 [STOCK_RESTORE] Processing stock restoration for cancelled order: " + order.getOrderNumber());
        
        for (OrderItem orderItem : order.getItems()) {
            try {
                Product product = orderItem.getProduct();
                int orderedQuantity = orderItem.getQuantity();
                
                System.out.println("🔍 [STOCK_RESTORE] Processing product: " + product.getName() + 
                                 " | Current stock: " + product.getStock() + 
                                 " | Quantity to restore: " + orderedQuantity);
                
                // Lock product untuk mencegah race condition
                Product lockedProduct = productRepository.findByIdForUpdate(product.getId())
                        .orElseThrow(() -> new IllegalStateException("Product tidak ditemukan: " + product.getId()));
                
                int currentStock = lockedProduct.getStock();
                
                // Kembalikan stock
                int restoredStock = currentStock + orderedQuantity;
                lockedProduct.setStock(restoredStock);
                productRepository.save(lockedProduct);
                
                System.out.println("✅ [STOCK_RESTORE] Stock restored for: " + lockedProduct.getName() + 
                                 " | " + currentStock + " -> " + restoredStock);
                
            } catch (Exception e) {
                System.out.println("❌ [STOCK_RESTORE] Error restoring stock for product ID " + 
                                 orderItem.getProduct().getId() + ": " + e.getMessage());
                e.printStackTrace();
                // Log error tapi jangan lempar exception, tetap lanjutkan restore yang lain
            }
        }
        
        // Force flush untuk memastikan perubahan stock ter-commit
        try {
            entityManager.flush();
            System.out.println("💾 [STOCK_RESTORE] Stock changes flushed to database");
        } catch (Exception e) {
            System.out.println("⚠️ [STOCK_RESTORE] Error flushing stock changes: " + e.getMessage());
        }
        
        System.out.println("🏁 [STOCK_RESTORE] Stock restoration completed for order: " + order.getOrderNumber());
    }
    
    /**
     * Debug method to show cart contents before checkout
     */
    private void debugCartContents(Long userId, String phase) {
        try {
            Optional<Cart> cartOptional = cartRepository.findByUserId(userId);
            if (cartOptional.isPresent()) {
                Cart cart = cartOptional.get();
                System.out.println("🔍 [" + phase + "] Cart contents for user " + userId + ":");
                System.out.println("   Total items in cart: " + cart.getItems().size());
                for (com.projek.tokweb.models.customer.CartItem item : cart.getItems()) {
                    System.out.println("   - Product ID: " + item.getProduct().getId() + 
                                     " | Name: " + item.getProduct().getName() + 
                                     " | Qty: " + item.getQuantity());
                }
            } else {
                System.out.println("🔍 [" + phase + "] No cart found for user " + userId);
            }
        } catch (Exception e) {
            System.out.println("🔍 [" + phase + "] Error debugging cart: " + e.getMessage());
        }
    }

    /**
     * Clear specific items from user's cart after successful checkout
     */
    @Transactional
    private void clearUserCartAfterCheckout(Long userId, CheckoutRequest req) {
        try {
            System.out.println("🗑️ [CART_CLEAR] Starting cart clearance for user ID: " + userId + " after successful checkout");

            // Create list of product IDs that were checked out
            List<Long> checkedOutProductIds = req.getItems().stream()
                    .map(CheckoutRequest.Item::getProductId)
                    .collect(Collectors.toList());
            
            System.out.println("🎯 [CART_CLEAR] Product IDs to remove from cart: " + checkedOutProductIds);

            // Find user's cart
            Optional<Cart> cartOptional = cartRepository.findByUserId(userId);

            if (cartOptional.isPresent()) {
                Cart cart = cartOptional.get();
                Long cartId = cart.getId();
                System.out.println("📦 [CART_CLEAR] Found cart ID: " + cartId + " with " + cart.getItems().size() + " items total");

                // Method 1: Direct deletion using CartItemRepository for each checked out product
                int deletedCount = 0;
                for (Long productId : checkedOutProductIds) {
                    try {
                        System.out.println("🗑️ [CART_CLEAR] Attempting to delete cart item - Cart ID: " + cartId + ", Product ID: " + productId);
                        
                        // Use the existing method from CartService that works
                        cartItemRepository.deleteByCartIdAndProductId(cartId, productId);
                        deletedCount++;
                        
                        System.out.println("✅ [CART_CLEAR] Successfully deleted cart item for Product ID: " + productId);
                    } catch (Exception e) {
                        System.out.println("❌ [CART_CLEAR] Error deleting cart item for Product ID " + productId + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                
                System.out.println("✅ [CART_CLEAR] Deleted " + deletedCount + " out of " + checkedOutProductIds.size() + " items from cart");
                
                // Method 2: Force flush the transaction to ensure deletion is committed
                try {
                    // This will force the deletion to be committed immediately
                    System.out.println("💾 [CART_CLEAR] Flushing transaction to commit cart deletions...");
                    entityManager.flush(); // Force immediate database synchronization
                    System.out.println("✅ [CART_CLEAR] EntityManager flush completed");
                    
                    // Refresh cart from database to verify deletions
                    Cart refreshedCart = cartRepository.findByUserId(userId).orElse(null);
                    if (refreshedCart != null) {
                        int remainingItems = refreshedCart.getItems().size();
                        System.out.println("📊 [CART_CLEAR] After deletion - Remaining items in cart: " + remainingItems);
                        
                        // Log remaining items for debugging
                        for (com.projek.tokweb.models.customer.CartItem item : refreshedCart.getItems()) {
                            System.out.println("📦 [CART_CLEAR] Remaining item - Product ID: " + item.getProduct().getId() + 
                                             " | Name: " + item.getProduct().getName() + 
                                             " | Qty: " + item.getQuantity());
                        }
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ [CART_CLEAR] Error during verification: " + e.getMessage());
                }
                
            } else {
                System.out.println("ℹ️ [CART_CLEAR] No cart found for user ID: " + userId);
            }

        } catch (Exception e) {
            System.out.println("❌ [CART_CLEAR] Critical error during cart clearance for user ID: " + userId);
            e.printStackTrace();
            // Don't throw exception here as the order was already created successfully
            // Just log the error and continue
        }
        
        System.out.println("🏁 [CART_CLEAR] Cart clearance process completed for user ID: " + userId);
    }
}