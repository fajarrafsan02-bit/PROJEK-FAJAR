package com.projek.tokweb.service.customer;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projek.tokweb.models.User;
import com.projek.tokweb.models.admin.Product;
import com.projek.tokweb.models.customer.Wishlist;
import com.projek.tokweb.repository.admin.ProductRepository;
import com.projek.tokweb.repository.customer.WishlistRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WishlistService {
    
    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    
    /**
     * Add product to user's wishlist
     */
    public Map<String, Object> addToWishlist(Long userId, Long productId, String notes) {
        try {
            log.info("💝 Adding product {} to wishlist for user {}", productId, userId);
            
            // Check if product exists and is active
            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isEmpty()) {
                return createErrorResponse("Produk tidak ditemukan");
            }
            
            Product product = productOpt.get();
            if (!product.getIsActive()) {
                return createErrorResponse("Produk tidak tersedia");
            }
            
            // Check if already in wishlist
            if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
                return createErrorResponse("Produk sudah ada di wishlist");
            }
            
            // Add to wishlist
            Wishlist wishlist = Wishlist.builder()
                    .userId(userId)
                    .productId(productId)
                    .notes(notes)
                    .build();
            
            Wishlist saved = wishlistRepository.save(wishlist);
            log.info("✅ Product {} added to wishlist with ID {}", productId, saved.getId());
            
            // Get updated wishlist count
            Long wishlistCount = wishlistRepository.countByUserId(userId);
            
            return createSuccessResponse("Produk berhasil ditambahkan ke wishlist", Map.of(
                "wishlistId", saved.getId(),
                "productId", productId,
                "productName", product.getName(),
                "wishlistCount", wishlistCount
            ));
            
        } catch (Exception e) {
            log.error("❌ Error adding product {} to wishlist for user {}: {}", productId, userId, e.getMessage(), e);
            return createErrorResponse("Gagal menambahkan ke wishlist: " + e.getMessage());
        }
    }
    
    /**
     * Remove product from user's wishlist
     */
    public Map<String, Object> removeFromWishlist(Long userId, Long productId) {
        try {
            log.info("🗑️ Removing product {} from wishlist for user {}", productId, userId);
            
            int deleted = wishlistRepository.deleteByUserIdAndProductId(userId, productId);
            
            if (deleted == 0) {
                return createErrorResponse("Produk tidak ditemukan di wishlist");
            }
            
            // Get updated wishlist count
            Long wishlistCount = wishlistRepository.countByUserId(userId);
            
            log.info("✅ Product {} removed from wishlist for user {}", productId, userId);
            
            return createSuccessResponse("Produk berhasil dihapus dari wishlist", Map.of(
                "productId", productId,
                "wishlistCount", wishlistCount
            ));
            
        } catch (Exception e) {
            log.error("❌ Error removing product {} from wishlist for user {}: {}", productId, userId, e.getMessage(), e);
            return createErrorResponse("Gagal menghapus dari wishlist: " + e.getMessage());
        }
    }
    
    /**
     * Remove multiple items from wishlist
     */
    public Map<String, Object> removeMultipleFromWishlist(Long userId, List<Long> wishlistIds) {
        try {
            log.info("🗑️ Removing {} items from wishlist for user {}", wishlistIds.size(), userId);
            
            int deleted = wishlistRepository.deleteByIdsAndUserId(wishlistIds, userId);
            
            if (deleted == 0) {
                return createErrorResponse("Tidak ada item yang dihapus");
            }
            
            // Get updated wishlist count
            Long wishlistCount = wishlistRepository.countByUserId(userId);
            
            log.info("✅ {} items removed from wishlist for user {}", deleted, userId);
            
            return createSuccessResponse(String.format("%d produk berhasil dihapus dari wishlist", deleted), Map.of(
                "deletedCount", deleted,
                "wishlistCount", wishlistCount
            ));
            
        } catch (Exception e) {
            log.error("❌ Error removing multiple items from wishlist for user {}: {}", userId, e.getMessage(), e);
            return createErrorResponse("Gagal menghapus dari wishlist: " + e.getMessage());
        }
    }
    
    /**
     * Get user's wishlist with products
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserWishlist(Long userId) {
        try {
            log.info("📋 Getting wishlist for user {}", userId);
            
            List<Wishlist> wishlistItems = wishlistRepository.findByUserIdWithProduct(userId);
            
            // Convert to response format
            List<Map<String, Object>> items = wishlistItems.stream()
                    .map(this::convertWishlistToMap)
                    .collect(Collectors.toList());
            
            // Get wishlist statistics
            Object[] stats = wishlistRepository.getWishlistStatistics(userId);
            Map<String, Object> statistics = new HashMap<>();
            
            if (stats != null && stats.length >= 3) {
                statistics.put("totalItems", ((Number) stats[0]).longValue());
                statistics.put("availableItems", ((Number) stats[1]).longValue());
                statistics.put("totalValue", stats[2] != null ? ((BigDecimal) stats[2]) : BigDecimal.ZERO);
            } else {
                statistics.put("totalItems", 0L);
                statistics.put("availableItems", 0L);
                statistics.put("totalValue", BigDecimal.ZERO);
            }
            
            log.info("✅ Retrieved {} wishlist items for user {}", items.size(), userId);
            
            return createSuccessResponse("Wishlist berhasil dimuat", Map.of(
                "items", items,
                "statistics", statistics,
                "totalItems", items.size()
            ));
            
        } catch (Exception e) {
            log.error("❌ Error getting wishlist for user {}: {}", userId, e.getMessage(), e);
            return createErrorResponse("Gagal memuat wishlist: " + e.getMessage());
        }
    }
    
    /**
     * Search wishlist items
     */
    @Transactional(readOnly = true)
    public Map<String, Object> searchWishlist(Long userId, String searchTerm) {
        try {
            log.info("🔍 Searching wishlist for user {} with term: {}", userId, searchTerm);
            
            List<Wishlist> wishlistItems;
            
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                wishlistItems = wishlistRepository.findByUserIdWithProduct(userId);
            } else {
                wishlistItems = wishlistRepository.searchWishlistItems(userId, searchTerm.trim());
            }
            
            // Convert to response format
            List<Map<String, Object>> items = wishlistItems.stream()
                    .map(this::convertWishlistToMap)
                    .collect(Collectors.toList());
            
            log.info("✅ Found {} wishlist items for search term: {}", items.size(), searchTerm);
            
            return createSuccessResponse("Pencarian berhasil", Map.of(
                "items", items,
                "searchTerm", searchTerm != null ? searchTerm : "",
                "totalFound", items.size()
            ));
            
        } catch (Exception e) {
            log.error("❌ Error searching wishlist for user {}: {}", userId, e.getMessage(), e);
            return createErrorResponse("Gagal mencari di wishlist: " + e.getMessage());
        }
    }
    
    /**
     * Check if product is in user's wishlist
     */
    @Transactional(readOnly = true)
    public boolean isProductInWishlist(Long userId, Long productId) {
        return wishlistRepository.existsByUserIdAndProductId(userId, productId);
    }
    
    /**
     * Get wishlist count for user
     */
    @Transactional(readOnly = true)
    public Long getWishlistCount(Long userId) {
        return wishlistRepository.countByUserId(userId);
    }
    
    /**
     * Get recent wishlist additions
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRecentWishlistItems(Long userId, int limit) {
        try {
            Pageable pageable = PageRequest.of(0, limit);
            List<Wishlist> recentItems = wishlistRepository.findRecentWishlistItems(userId, pageable);
            
            List<Map<String, Object>> items = recentItems.stream()
                    .map(this::convertWishlistToMap)
                    .collect(Collectors.toList());
            
            return createSuccessResponse("Recent wishlist items retrieved", Map.of(
                "items", items,
                "limit", limit
            ));
            
        } catch (Exception e) {
            log.error("❌ Error getting recent wishlist items for user {}: {}", userId, e.getMessage(), e);
            return createErrorResponse("Gagal memuat item wishlist terbaru: " + e.getMessage());
        }
    }
    
    /**
     * Clear entire wishlist for user
     */
    public Map<String, Object> clearWishlist(Long userId) {
        try {
            log.info("🗑️ Clearing entire wishlist for user {}", userId);
            
            int deleted = wishlistRepository.deleteByUserId(userId);
            
            log.info("✅ Cleared {} items from wishlist for user {}", deleted, userId);
            
            return createSuccessResponse(String.format("Wishlist berhasil dikosongkan (%d item dihapus)", deleted), Map.of(
                "deletedCount", deleted,
                "wishlistCount", 0L
            ));
            
        } catch (Exception e) {
            log.error("❌ Error clearing wishlist for user {}: {}", userId, e.getMessage(), e);
            return createErrorResponse("Gagal mengosongkan wishlist: " + e.getMessage());
        }
    }
    
    /**
     * Convert Wishlist entity to Map for API response
     */
    private Map<String, Object> convertWishlistToMap(Wishlist wishlist) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", wishlist.getId());
        map.put("notes", wishlist.getNotes());
        map.put("createdAt", wishlist.getCreatedAt());
        map.put("formattedDate", wishlist.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        
        // Product information
        if (wishlist.getProduct() != null) {
            Product product = wishlist.getProduct();
            Map<String, Object> productMap = new HashMap<>();
            productMap.put("id", product.getId());
            productMap.put("name", product.getName());
            productMap.put("description", product.getDescription());
            productMap.put("finalPrice", product.getFinalPrice());
            productMap.put("formattedPrice", formatCurrency(product.getFinalPrice()));
            productMap.put("imageUrl", product.getImageUrl());
            productMap.put("category", product.getCategory());
            productMap.put("weight", product.getWeight());
            productMap.put("purity", product.getPurity());
            productMap.put("stock", product.getStock());
            productMap.put("isActive", product.getIsActive());
            productMap.put("isAvailable", product.getIsActive() && product.getStock() > 0);
            
            map.put("product", productMap);
        }
        
        return map;
    }
    
    /**
     * Format currency to Indonesian format
     */
    private String formatCurrency(Number amount) {
        if (amount == null) {
            return "Rp 0";
        }
        
        java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,###");
        formatter.setDecimalFormatSymbols(java.text.DecimalFormatSymbols.getInstance(new java.util.Locale("id", "ID")));
        
        return "Rp " + formatter.format(amount);
    }
    
    /**
     * Create success response
     */
    private Map<String, Object> createSuccessResponse(String message, Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
    
    /**
     * Create error response
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}
