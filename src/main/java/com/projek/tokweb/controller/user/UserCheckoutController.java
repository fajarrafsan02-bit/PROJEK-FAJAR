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
import com.projek.tokweb.utils.AuthUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

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

            // Get authenticated user with fallback
            User user = getCurrentAuthenticatedUser();
            if (user == null) {
                System.out.println("❌ User not authenticated");
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "User tidak terautentikasi. Silakan login terlebih dahulu."
                ));
            }
            
            System.out.println("🛒 Processing checkout for user: " + user.getNamaLengkap() + " (ID: " + user.getId() + ")");

            // Use the checkout service to create real order
            Map<String, Object> paymentData = checkoutService.createOrder(req, user);

            System.out.println("=== CHECKOUT SUCCESS ===");
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "ORDER_CREATED",
                "data", paymentData
            ));
            
        } catch (IllegalArgumentException e) {
            System.out.println("=== CHECKOUT VALIDATION ERROR ===");
            System.out.println("Validation Error: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "VALIDATION_ERROR",
                "message", e.getMessage()
            ));
        } catch (IllegalStateException e) {
            System.out.println("=== CHECKOUT STATE ERROR ===");
            System.out.println("State Error: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "STATE_ERROR", 
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            System.out.println("=== CHECKOUT SYSTEM ERROR ===");
            System.out.println("System Error Type: " + e.getClass().getSimpleName());
            System.out.println("System Error Message: " + e.getMessage());
            System.out.println("Full Stack Trace:");
            e.printStackTrace();
            
            // PERBAIKAN: Better error message mapping
            String errorMessage = e.getMessage();
            String errorType = "SYSTEM_ERROR";
            int httpStatus = 500;
            
            if (errorMessage != null) {
                // Handle specific error types
                if (errorMessage.contains("rollback-only")) {
                    errorMessage = "Transaksi gagal diproses karena ada masalah data. Silakan periksa data Anda dan coba lagi.";
                    errorType = "TRANSACTION_ERROR";
                } else if (errorMessage.contains("tidak ditemukan") || errorMessage.contains("not found")) {
                    errorMessage = "Data yang diperlukan tidak ditemukan. " + errorMessage;
                    errorType = "DATA_ERROR";
                    httpStatus = 404;
                } else if (errorMessage.contains("tidak aktif") || errorMessage.contains("tidak valid")) {
                    errorMessage = "Data tidak valid: " + errorMessage;
                    errorType = "VALIDATION_ERROR";
                    httpStatus = 400;
                } else if (errorMessage.contains("ConstraintViolation") || errorMessage.contains("constraint")) {
                    errorMessage = "Terjadi konflik data. Silakan coba lagi atau hubungi administrator.";
                    errorType = "CONSTRAINT_ERROR";
                    httpStatus = 409;
                } else if (errorMessage.toLowerCase().contains("connection") || 
                          errorMessage.toLowerCase().contains("timeout") ||
                          errorMessage.toLowerCase().contains("database")) {
                    errorMessage = "Terjadi masalah koneksi database. Silakan coba lagi dalam beberapa saat.";
                    errorType = "DATABASE_ERROR";
                }
            } else {
                errorMessage = "Terjadi kesalahan sistem yang tidak diketahui. Silakan coba lagi.";
            }
            
            System.out.println("=== MAPPED ERROR RESPONSE ===");
            System.out.println("Error Type: " + errorType);
            System.out.println("HTTP Status: " + httpStatus);
            System.out.println("Error Message: " + errorMessage);
            
            return ResponseEntity.status(httpStatus).body(Map.of(
                "success", false,
                "error", errorType,
                "message", errorMessage,
                "timestamp", System.currentTimeMillis()
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
    
    /**
     * Helper method to get current authenticated user with multiple fallback strategies
     */
    private User getCurrentAuthenticatedUser() {
        System.out.println("🔍 [CONTROLLER] Attempting to get current user...");
        
        // Method 1: Try AuthUtils first
        User user = AuthUtils.getCurrentUser();
        if (user != null) {
            System.out.println("✅ [CONTROLLER] User found via AuthUtils - ID: " + user.getId());
            return user;
        }
        
        // Method 2: Direct SecurityContext access
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                System.out.println("🔍 [CONTROLLER] Principal type: " + principal.getClass().getSimpleName());
                
                if (principal instanceof User) {
                    user = (User) principal;
                    System.out.println("✅ [CONTROLLER] User found via direct SecurityContext - ID: " + user.getId());
                    return user;
                } else if (principal instanceof String) {
                    String email = (String) principal;
                    System.out.println("🔍 [CONTROLLER] Principal is email: " + email + ", looking up in database...");
                    
                    var userOpt = userRespository.findByEmail(email);
                    if (userOpt.isPresent()) {
                        user = userOpt.get();
                        System.out.println("✅ [CONTROLLER] User found via email lookup - ID: " + user.getId() + ", Email: " + user.getEmail());
                        return user;
                    } else {
                        System.out.println("❌ [CONTROLLER] No user found for email: " + email);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("❌ [CONTROLLER] Error in fallback authentication: " + e.getMessage());
        }
        
        System.out.println("❌ [CONTROLLER] All authentication methods failed");
        return null;
    }
}