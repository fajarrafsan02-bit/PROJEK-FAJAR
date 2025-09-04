package com.projek.tokweb.repository.customer;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.projek.tokweb.models.customer.Wishlist;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    
    /**
     * Find all wishlist items for a user
     */
    @EntityGraph(attributePaths = {"product"})
    @Query("SELECT w FROM Wishlist w WHERE w.userId = :userId ORDER BY w.createdAt DESC")
    List<Wishlist> findByUserIdWithProduct(@Param("userId") Long userId);
    
    /**
     * Find all wishlist items for a user with pagination
     */
    @EntityGraph(attributePaths = {"product"})
    @Query("SELECT w FROM Wishlist w WHERE w.userId = :userId ORDER BY w.createdAt DESC")
    Page<Wishlist> findByUserIdWithProduct(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Find wishlist item by user and product
     */
    @EntityGraph(attributePaths = {"product"})
    @Query("SELECT w FROM Wishlist w WHERE w.userId = :userId AND w.productId = :productId")
    Optional<Wishlist> findByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);
    
    /**
     * Check if product exists in user's wishlist
     */
    @Query("SELECT COUNT(w) > 0 FROM Wishlist w WHERE w.userId = :userId AND w.productId = :productId")
    boolean existsByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);
    
    /**
     * Count wishlist items for a user
     */
    @Query("SELECT COUNT(w) FROM Wishlist w WHERE w.userId = :userId")
    Long countByUserId(@Param("userId") Long userId);
    
    /**
     * Get top products in wishlists (most wishlisted products)
     */
    @Query("SELECT w.productId, COUNT(w) as wishlistCount FROM Wishlist w " +
           "GROUP BY w.productId ORDER BY wishlistCount DESC")
    List<Object[]> findMostWishlistedProducts(Pageable pageable);
    
    /**
     * Delete wishlist item by user and product
     */
    @Modifying
    @Query("DELETE FROM Wishlist w WHERE w.userId = :userId AND w.productId = :productId")
    int deleteByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);
    
    /**
     * Delete all wishlist items for a user
     */
    @Modifying
    @Query("DELETE FROM Wishlist w WHERE w.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
    
    /**
     * Delete multiple wishlist items by IDs for a specific user (security check)
     */
    @Modifying
    @Query("DELETE FROM Wishlist w WHERE w.id IN :ids AND w.userId = :userId")
    int deleteByIdsAndUserId(@Param("ids") List<Long> ids, @Param("userId") Long userId);
    
    /**
     * Find wishlist items by product availability (active products only)
     */
    @EntityGraph(attributePaths = {"product"})
    @Query("SELECT w FROM Wishlist w JOIN w.product p WHERE w.userId = :userId AND p.isActive = true ORDER BY w.createdAt DESC")
    List<Wishlist> findByUserIdWithActiveProducts(@Param("userId") Long userId);
    
    /**
     * Get wishlist statistics for a user
     */
    @Query("SELECT " +
           "COUNT(w) as totalItems, " +
           "COUNT(CASE WHEN p.isActive = true THEN 1 END) as availableItems, " +
           "COALESCE(SUM(CASE WHEN p.isActive = true THEN p.finalPrice ELSE 0 END), 0) as totalValue " +
           "FROM Wishlist w JOIN w.product p WHERE w.userId = :userId")
    Object[] getWishlistStatistics(@Param("userId") Long userId);
    
    /**
     * Find recent wishlist additions
     */
    @EntityGraph(attributePaths = {"product"})
    @Query("SELECT w FROM Wishlist w WHERE w.userId = :userId ORDER BY w.createdAt DESC")
    List<Wishlist> findRecentWishlistItems(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Search wishlist items by product name
     */
    @EntityGraph(attributePaths = {"product"})
    @Query("SELECT w FROM Wishlist w JOIN w.product p WHERE w.userId = :userId AND " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY w.createdAt DESC")
    List<Wishlist> searchWishlistItems(@Param("userId") Long userId, @Param("searchTerm") String searchTerm);
}
