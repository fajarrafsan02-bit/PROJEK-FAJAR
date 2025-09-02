package com.projek.tokweb.repository.admin;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.projek.tokweb.models.admin.Product;

import jakarta.persistence.LockModeType;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrCategoryContainingIgnoringCase(
            String name, String description, String category, Pageable pageable);

    Page<Product> findByIsActiveTrueAndNameContainingIgnoreCaseOrIsActiveTrueAndDescriptionContainingIgnoreCaseOrIsActiveTrueAndCategoryContainingIgnoreCase(
            String name, String description, String category, Pageable pageable);

    List<Product> findByStockLessThanEqualAndIsActiveTrue(int minStock);

    Page<Product> findByIsActiveTrue(Pageable pageable);

    List<Product> findByIsActiveTrue();

    List<Product> findByIsActiveTrueAndPurity(int purity);

    /**
     * Find products by purity and active status
     */
    List<Product> findByPurityAndIsActiveTrue(int purity);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
