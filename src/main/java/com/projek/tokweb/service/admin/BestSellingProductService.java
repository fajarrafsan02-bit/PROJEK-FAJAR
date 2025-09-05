package com.projek.tokweb.service.admin;

import com.projek.tokweb.models.admin.BestSellingProduct;
import com.projek.tokweb.models.admin.Product;
import com.projek.tokweb.repository.admin.BestSellingProductRepository;
import com.projek.tokweb.repository.admin.ProductRepository;
import com.projek.tokweb.dto.admin.BestSellingProductDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;

@Service
public class BestSellingProductService {

    @Autowired
    private BestSellingProductRepository bestSellingProductRepository;

    @Autowired
    private ProductRepository productRepository;

    public List<BestSellingProductDto> getTopBestSellingProducts(int limit) {
        List<BestSellingProduct> bestSellingProducts = bestSellingProductRepository.findTopBestSellingProducts();
        
        // Batasi jumlah produk sesuai limit
        return bestSellingProducts.stream()
                .sorted(Comparator.comparing(BestSellingProduct::getSalesCount).reversed())
                .limit(limit)
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public void updateBestSellingProduct(Long productId, int quantity) {
        BestSellingProduct bestSellingProduct = bestSellingProductRepository.findByProductId(productId);
        
        if (bestSellingProduct != null) {
            // Update jumlah penjualan
            bestSellingProduct.setSalesCount(bestSellingProduct.getSalesCount() + quantity);
            bestSellingProductRepository.save(bestSellingProduct);
        } else {
            // Buat entri baru jika belum ada
            Product product = productRepository.findById(productId).orElse(null);
            if (product != null) {
                BestSellingProduct newBestSellingProduct = BestSellingProduct.builder()
                        .product(product)
                        .salesCount(quantity)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                bestSellingProductRepository.save(newBestSellingProduct);
            }
        }
    }

    public void initializeBestSellingProducts() {
        // Inisialisasi produk terlaris berdasarkan data historis penjualan
        // Untuk sekarang kita akan membuat entri kosong untuk semua produk aktif
        List<Product> activeProducts = productRepository.findByIsActiveTrue();
        
        for (Product product : activeProducts) {
            BestSellingProduct existing = bestSellingProductRepository.findByProductId(product.getId());
            if (existing == null) {
                BestSellingProduct bestSellingProduct = BestSellingProduct.builder()
                        .product(product)
                        .salesCount(0) // Awalnya 0, akan diupdate saat ada penjualan
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                bestSellingProductRepository.save(bestSellingProduct);
            }
        }
    }

    private BestSellingProductDto convertToDto(BestSellingProduct bestSellingProduct) {
        return BestSellingProductDto.builder()
                .productId(bestSellingProduct.getProduct().getId())
                .productName(bestSellingProduct.getProduct().getName())
                .productImage(bestSellingProduct.getProduct().getImageUrl())
                .productPrice(bestSellingProduct.getProduct().getFinalPrice() != null ? 
                    BigDecimal.valueOf(bestSellingProduct.getProduct().getFinalPrice()) : BigDecimal.ZERO)
                .salesCount(bestSellingProduct.getSalesCount())
                .productCategory(bestSellingProduct.getProduct().getCategory())
                .build();
    }
}