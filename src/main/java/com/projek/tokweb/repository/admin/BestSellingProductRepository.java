package com.projek.tokweb.repository.admin;

import com.projek.tokweb.models.admin.BestSellingProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BestSellingProductRepository extends JpaRepository<BestSellingProduct, Long> {
    
    @Query("SELECT bp FROM BestSellingProduct bp ORDER BY bp.salesCount DESC")
    List<BestSellingProduct> findTopBestSellingProducts();
    
    @Query("SELECT bp FROM BestSellingProduct bp WHERE bp.product.id = :productId")
    BestSellingProduct findByProductId(Long productId);
}