package com.projek.tokweb.models.customer;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.projek.tokweb.models.User;
import com.projek.tokweb.models.admin.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "wishlist", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "product_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wishlist {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "notes", length = 500)
    private String notes; // Optional notes user can add to wishlist item
    
    // Helper constructor for creating wishlist items
    public Wishlist(Long userId, Long productId) {
        this.userId = userId;
        this.productId = productId;
    }
    
    // Helper method to check if wishlist item belongs to user
    public boolean belongsToUser(Long userId) {
        return this.userId != null && this.userId.equals(userId);
    }
}
