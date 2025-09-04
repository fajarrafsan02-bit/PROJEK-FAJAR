package com.projek.tokweb.repository.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.projek.tokweb.models.customer.CartItem;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.product.id = :productId")
    CartItem findByCartIdAndProductId(@Param("cartId") Long cartId, @Param("productId") Long productId);
    
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.product.id = :productId")
    @org.springframework.transaction.annotation.Transactional
    void deleteByCartIdAndProductId(@Param("cartId") Long cartId, @Param("productId") Long productId);
}
