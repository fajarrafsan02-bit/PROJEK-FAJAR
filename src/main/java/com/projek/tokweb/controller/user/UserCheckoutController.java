package com.projek.tokweb.controller.user;

import java.util.Map;
import java.util.HashMap;
// import java.util.Optional;

// import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// import com.projek.tokweb.dto.ApiResponse;
import com.projek.tokweb.dto.customer.CheckoutRequest;
// import com.projek.tokweb.dto.customer.PaymentInstruction;
// import com.projek.tokweb.models.User;
import com.projek.tokweb.repository.UserRespository;
// import com.projek.tokweb.service.customer.Checkout;

// import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Validated
public class UserCheckoutController {

    // private final Checkout checkout;
    // private final UserRespository userRespository;

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequest req) {
        try {
            System.out.println("=== CHECKOUT REQUEST RECEIVED ===");
            System.out.println("Request: " + req);
            System.out.println("Items: " + (req != null ? req.getItems() : "NULL"));
            System.out.println("CustomerInfo: " + (req != null && req.getCustomerInfo() != null ? "NOT NULL" : "NULL"));
            System.out.println("PaymentMethod: " + (req != null ? req.getPaymentMethod() : "NULL"));
            System.out.println("TotalAmount: " + (req != null ? req.getTotalAmount() : "NULL"));
            
            if (req == null) {
                System.out.println("Request is NULL");
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Request tidak boleh kosong"
                ));
            }

            if (req.getItems() == null || req.getItems().isEmpty()) {
                System.out.println("Items is NULL or empty");
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Tidak ada item untuk dibeli"
                ));
            }

            if (req.getCustomerInfo() == null) {
                System.out.println("CustomerInfo is NULL");
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Data customer tidak lengkap"
                ));
            }

            String method = req.getPaymentMethod() != null ? req.getPaymentMethod() : "QR_CODE";
            
            // Gunakan totalAmount dari request jika ada, jika tidak hitung manual
            double totalAmount = req.getTotalAmount() != null ? req.getTotalAmount() : 
                req.getItems().stream()
                    .mapToDouble(item -> item.getQuantity() * 1000000.0) // Simulasi harga 1 juta per item
                    .sum();

            System.out.println("Total amount: " + totalAmount);

            Map<String, Object> data = new HashMap<>();
            data.put("externalId", "ORDER-" + System.currentTimeMillis());
            data.put("amount", totalAmount);
            data.put("paymentMethod", method);
            data.put("customerName", req.getCustomerInfo().getFullName());
            data.put("customerEmail", req.getCustomerInfo().getEmail());

            if ("QR_CODE".equals(method)) {
                // Generate QR code data untuk pembayaran (format EMV QR)
                String qrData = String.format(
                    "00020101021226620014ID.CO.QRIS.WWW01189360091408123456789015201123456789010303UEN52045IDR5303360540%.0f5802ID6304%s",
                    totalAmount,
                    generateChecksum(totalAmount)
                );
                
                data.put("qrData", qrData); // Data untuk generate QR code di frontend
                data.put("instructions", "Scan QR code untuk melakukan pembayaran. Setelah bayar, upload bukti pembayaran.");
                data.put("paymentAmount", totalAmount);
            } else if ("BANK_TRANSFER".equals(method)) {
                data.put("bankName", "Bank Central Asia (BCA)");
                data.put("bankAccountNumber", "123456789");
                data.put("accountHolderName", "PT. FAJAR GOLD INDONESIA");
                data.put("instructions", "Transfer ke rekening di atas sesuai jumlah tagihan: Rp " + String.format("%,.0f", totalAmount) + ". Setelah bayar, upload bukti pembayaran.");
            }

            System.out.println("=== CHECKOUT SUCCESS ===");
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "ORDER_CREATED",
                "data", data
            ));
        } catch (Exception e) {
            System.out.println("=== CHECKOUT ERROR ===");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/upload-payment-proof")
    public ResponseEntity<?> uploadPaymentProof(
            @RequestParam("paymentProof") MultipartFile file,
            @RequestParam("orderId") String orderId) {
        
        try {
            // Simpan file bukti pembayaran
            String fileName = "payment_proof_" + orderId + "_" + System.currentTimeMillis() + ".jpg";
            // Implementasi penyimpanan file sesuai kebutuhan
            
            // Update status order menjadi WAITING_CONFIRMATION
            // orderService.updateStatus(orderId, "WAITING_CONFIRMATION");
            
            return ResponseEntity.ok(Map.of("message", "Bukti pembayaran berhasil diupload"));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Gagal upload bukti pembayaran"));
        }
    }

    // Tambahkan method untuk generate checksum sederhana
    private String generateChecksum(double amount) {
        // Checksum sederhana untuk demo
        String amountStr = String.format("%.0f", amount);
        int checksum = 0;
        for (char c : amountStr.toCharArray()) {
            checksum += Character.getNumericValue(c);
        }
        return String.format("%04d", checksum % 10000);
    }
}