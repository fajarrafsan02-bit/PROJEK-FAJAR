package com.projek.tokweb.controller.user;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.projek.tokweb.dto.ApiResponse;
import com.projek.tokweb.service.customer.WishlistService;
import com.projek.tokweb.utils.AuthUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/user/api/wishlist")
@RequiredArgsConstructor
@Slf4j
public class WishlistController {
    
    private final WishlistService wishlistService;
    
    /**
     * Get user's wishlist
     */
    @GetMapping
    public ResponseEntity<?> getUserWishlist() {
        try {
            // Get current authenticated user ID
            Long userId = AuthUtils.getCurrentUserId();
            log.info("🔍 [WISHLIST_CONTROLLER] Getting wishlist for user ID: {}", userId);
            
            if (userId == null) {
                log.info("❌ [WISHLIST_CONTROLLER] User ID is null - user not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("User tidak terautentikasi. Silakan login kembali."));
            }
            
            Map<String, Object> result = wishlistService.getUserWishlist(userId);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(ApiResponse.success("Wishlist berhasil dimuat", result.get("data")));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error((String) result.get("message")));
            }
            
        } catch (Exception e) {
            log.error("❌ Error getting wishlist: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Terjadi kesalahan saat memuat wishlist"));
        }
    }
    
    /**
     * Add product to wishlist
     */
    @PostMapping("/add")
    public ResponseEntity<?> addToWishlist(@RequestBody Map<String, Object> request) {
        try {
            // Get current authenticated user ID
            Long userId = AuthUtils.getCurrentUserId();
            log.info("🔍 [WISHLIST_CONTROLLER] Adding to wishlist for user ID: {}", userId);
            
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("User tidak terautentikasi"));
            }
            
            // Get product ID from request
            Long productId = null;
            if (request.get("productId") != null) {
                productId = Long.valueOf(request.get("productId").toString());
            }
            
            if (productId == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Product ID diperlukan"));
            }
            
            String notes = request.get("notes") != null ? request.get("notes").toString() : null;
            
            Map<String, Object> result = wishlistService.addToWishlist(userId, productId, notes);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(ApiResponse.success((String) result.get("message"), result.get("data")));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error((String) result.get("message")));
            }
            
        } catch (Exception e) {
            log.error("❌ Error adding to wishlist: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Terjadi kesalahan saat menambahkan ke wishlist"));
        }
    }
    
    /**
     * Remove product from wishlist
     */
    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<?> removeFromWishlist(@PathVariable Long productId) {
        try {
            // Get current authenticated user ID
            Long userId = AuthUtils.getCurrentUserId();
            log.info("🔍 [WISHLIST_CONTROLLER] Removing from wishlist for user ID: {} product ID: {}", userId, productId);
            
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("User tidak terautentikasi"));
            }
            
            Map<String, Object> result = wishlistService.removeFromWishlist(userId, productId);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(ApiResponse.success((String) result.get("message"), result.get("data")));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error((String) result.get("message")));
            }
            
        } catch (Exception e) {
            log.error("❌ Error removing from wishlist: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Terjadi kesalahan saat menghapus dari wishlist"));
        }
    }
    
    /**
     * Remove multiple items from wishlist
     */
    @DeleteMapping("/remove-multiple")
    public ResponseEntity<?> removeMultipleFromWishlist(@RequestBody Map<String, Object> request) {
        try {
            // Get current authenticated user ID
            Long userId = AuthUtils.getCurrentUserId();
            log.info("🔍 [WISHLIST_CONTROLLER] Removing multiple from wishlist for user ID: {}", userId);
            
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("User tidak terautentikasi"));
            }
            
            // Get wishlist IDs from request
            @SuppressWarnings("unchecked")
            List<Long> wishlistIds = (List<Long>) request.get("wishlistIds");
            
            if (wishlistIds == null || wishlistIds.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Wishlist IDs diperlukan"));
            }
            
            Map<String, Object> result = wishlistService.removeMultipleFromWishlist(userId, wishlistIds);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(ApiResponse.success((String) result.get("message"), result.get("data")));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error((String) result.get("message")));
            }
            
        } catch (Exception e) {
            log.error("❌ Error removing multiple from wishlist: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Terjadi kesalahan saat menghapus dari wishlist"));
        }
    }
    
    /**
     * Clear entire wishlist
     */
    @DeleteMapping("/clear")
    public ResponseEntity<?> clearWishlist() {
        try {
            // Get current authenticated user ID
            Long userId = AuthUtils.getCurrentUserId();
            log.info("🔍 [WISHLIST_CONTROLLER] Clearing wishlist for user ID: {}", userId);
            
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("User tidak terautentikasi"));
            }
            
            Map<String, Object> result = wishlistService.clearWishlist(userId);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(ApiResponse.success((String) result.get("message"), result.get("data")));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error((String) result.get("message")));
            }
            
        } catch (Exception e) {
            log.error("❌ Error clearing wishlist: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Terjadi kesalahan saat mengosongkan wishlist"));
        }
    }
    
    /**
     * Search wishlist items
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchWishlist(@RequestParam(required = false) String q) {
        try {
            // Get current authenticated user ID
            Long userId = AuthUtils.getCurrentUserId();
            log.info("🔍 [WISHLIST_CONTROLLER] Searching wishlist for user ID: {} term: {}", userId, q);
            
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("User tidak terautentikasi"));
            }
            
            Map<String, Object> result = wishlistService.searchWishlist(userId, q);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(ApiResponse.success((String) result.get("message"), result.get("data")));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error((String) result.get("message")));
            }
            
        } catch (Exception e) {
            log.error("❌ Error searching wishlist: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Terjadi kesalahan saat mencari di wishlist"));
        }
    }
    
    /**
     * Check if product is in wishlist
     */
    @GetMapping("/check/{productId}")
    public ResponseEntity<?> checkProductInWishlist(@PathVariable Long productId) {
        try {
            // Get current authenticated user ID
            Long userId = AuthUtils.getCurrentUserId();
            log.info("🔍 [WISHLIST_CONTROLLER] Checking product {} in wishlist for user ID: {}", productId, userId);
            
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("User tidak terautentikasi"));
            }
            
            boolean inWishlist = wishlistService.isProductInWishlist(userId, productId);
            
            return ResponseEntity.ok(ApiResponse.success("Status wishlist retrieved", Map.of(
                "productId", productId,
                "inWishlist", inWishlist
            )));
            
        } catch (Exception e) {
            log.error("❌ Error checking wishlist status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Terjadi kesalahan saat mengecek status wishlist"));
        }
    }
    
    /**
     * Get wishlist count
     */
    @GetMapping("/count")
    public ResponseEntity<?> getWishlistCount() {
        try {
            // Get current authenticated user ID
            Long userId = AuthUtils.getCurrentUserId();
            
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("User tidak terautentikasi"));
            }
            
            Long count = wishlistService.getWishlistCount(userId);
            
            return ResponseEntity.ok(ApiResponse.success("Wishlist count retrieved", Map.of(
                "count", count
            )));
            
        } catch (Exception e) {
            log.error("❌ Error getting wishlist count: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Terjadi kesalahan saat mengambil jumlah wishlist"));
        }
    }
    
    /**
     * Get recent wishlist items
     */
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentWishlistItems(@RequestParam(defaultValue = "5") int limit) {
        try {
            // Get current authenticated user ID
            Long userId = AuthUtils.getCurrentUserId();
            
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("User tidak terautentikasi"));
            }
            
            Map<String, Object> result = wishlistService.getRecentWishlistItems(userId, limit);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(ApiResponse.success((String) result.get("message"), result.get("data")));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error((String) result.get("message")));
            }
            
        } catch (Exception e) {
            log.error("❌ Error getting recent wishlist items: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Terjadi kesalahan saat mengambil wishlist terbaru"));
        }
    }
}
