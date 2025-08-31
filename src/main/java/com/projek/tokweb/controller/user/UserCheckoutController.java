package com.projek.tokweb.controller.user;

import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projek.tokweb.dto.customer.CheckoutRequest;
import com.projek.tokweb.models.User;
import com.projek.tokweb.repository.UserRespository;
import com.projek.tokweb.service.customer.CheckoutService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Validated
public class UserCheckoutController {

    private final CheckoutService checkoutService;
    private final UserRespository userRespository;

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@Valid @RequestBody CheckoutRequest req) {
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

            // Get user (for now, create a dummy user - in production, get from authentication)
            User user = User.builder()
                    .id(1L)
                    .namaLengkap(req.getCustomerInfo().getFullName())
                    .email(req.getCustomerInfo().getEmail())
                    .build();

            // Use the checkout service to create real order
            Map<String, Object> paymentData = checkoutService.createOrder(req, user);

            System.out.println("=== CHECKOUT SUCCESS ===");
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "ORDER_CREATED",
                "data", paymentData
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
            @RequestParam("orderId") String externalId) {
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "File tidak boleh kosong"
                ));
            }

            // Create upload directory if not exists
            String uploadDir = "uploads/payment-proofs/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique file name
            String originalFileName = file.getOriginalFilename();
            String fileExtension = originalFileName != null && originalFileName.contains(".") 
                ? originalFileName.substring(originalFileName.lastIndexOf("."))
                : ".jpg";
            String fileName = "payment_proof_" + externalId + "_" + System.currentTimeMillis() + fileExtension;
            
            // Save file
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            // Update payment status using service
            checkoutService.updatePaymentProof(externalId, fileName);
            
            System.out.println("Payment proof uploaded: " + fileName);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Bukti pembayaran berhasil diupload",
                "fileName", fileName
            ));
            
        } catch (IOException e) {
            System.out.println("File upload error: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Gagal menyimpan file: " + e.getMessage()
            ));
        } catch (Exception e) {
            System.out.println("Upload payment proof error: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Gagal upload bukti pembayaran: " + e.getMessage()
            ));
        }
    }
}