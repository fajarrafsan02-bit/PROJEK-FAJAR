package com.projek.tokweb.service.admin;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projek.tokweb.dto.admin.ProductRequestDto;
import com.projek.tokweb.dto.admin.ProductResponseDto;
import com.projek.tokweb.models.admin.Product;
import com.projek.tokweb.models.goldPrice.GoldPrice;
import com.projek.tokweb.repository.admin.ProductRepository;
import com.projek.tokweb.service.cloudinary.CloudinaryService;
import com.projek.tokweb.service.goldprice.GoldPriceService;
import com.projek.tokweb.utils.NumberFormatter;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final GoldPriceService goldPriceService;
    private final CloudinaryService cloudinaryService;

    public Product addProduct(ProductRequestDto dto) {
        double hargaPerGram = goldPriceService.getLatestHargaJual(dto.getPurity());
        double purityDecimal = dto.getPurity() / 24.0;
        double markupDecimal = dto.getMarkup() / 100.0;

        double totalHarga = (hargaPerGram * purityDecimal) * (1 + markupDecimal) * dto.getWeight();

        int finalStock = dto.getStock() != null ? dto.getStock() : 0;
        int finalMinStock = dto.getMinStock() != null ? dto.getMinStock() : 0;

        Product product = Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .weight(dto.getWeight())
                .purity(dto.getPurity())
                .category(dto.getCategory())
                .imageUrl(dto.getImageUrl())
                .markup(dto.getMarkup())
                .finalPrice(totalHarga)
                .stock(finalStock)
                .minStock(finalMinStock)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .updateAt(LocalDateTime.now())
                .build();

        return productRepository.save(product);
    }

    public ProductResponseDto addProductWithFormattedResponse(ProductRequestDto dto) {
        Product product = addProduct(dto);
        return ProductResponseDto.fromProduct(product);
    }

    public Product updateProduct(Long productId, ProductRequestDto request) {
        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produk tidak di temukan dengan ID : " + productId));

        List<String> errors = new ArrayList<>();

        if (request.getName() != null) {
            if (request.getName().length() < 3 || request.getName().length() > 100) {
                errors.add("Nama produk harus antara 3-100 karakter");
            } else {
                existingProduct.setName(request.getName());
            }
        }
        if (request.getDescription() != null) {
            if (request.getDescription().length() < 5 || request.getDescription().length() > 500) {
                errors.add("Deskripsi produk harus antara 10 - 500 karakter");
            } else {
                existingProduct.setDescription(request.getDescription());
            }
        }
        if (request.getWeight() != null) {
            if (request.getWeight() <= 0) {
                errors.add("Berat Produk harus lebih dari 0");
            } else if (request.getWeight() > 10000) {
                errors.add("Berat Produk maksimal 10.000 gram");
            } else {
                existingProduct.setWeight(request.getWeight());
            }
        }
        if (request.getPurity() != null) {
            if (request.getPurity() < 10 || request.getPurity() > 24) {
                errors.add("kadar kemurnian harus antara 10-24 karat");
            } else {
                existingProduct.setPurity(request.getPurity());
            }
        }
        if (request.getCategory() != null) {
            if (!request.getCategory().matches("^(CINCIN|GELANG|KALUNG|BATANGAN)")) {
                errors.add("kategori harus CINCIN, GELANG, KALUNG, atau BATANGAN");
            } else {
                existingProduct.setCategory(request.getCategory());
            }
        }
        if (request.getMarkup() != null) {
            if (request.getMarkup() > 1000) {
                errors.add("Markup maksimal 1000%");
            } else {
                existingProduct.setMarkup(request.getMarkup());
            }
        }

        if (request.getStock() != null) {
            if (request.getStock() < 0) {
                errors.add("stok tidak boleh kurang dari 0");
            } else {
                existingProduct.setStock(request.getStock());
            }
        }

        if (request.getMinStock() != null) {
            if (request.getMinStock() < 0) {
                errors.add("stok minimun tidak boleh kurang dari 0");
            } else {
                existingProduct.setMinStock(request.getMinStock());
            }
        }

        // REMOVED: Stock vs MinStock validation for edit operations
        // This allows admins to set stock below minStock during inventory updates
        // Original validation logic removed to provide more flexibility for administrators

        if (request.getIsActive() != null) {
            existingProduct.setIsActive(request.getIsActive());
        }

        if (!errors.isEmpty()) {
            throw new RuntimeException("Validasi gagal : " + String.join(", ", errors));
        }

        if (request.getMarkup() != null || request.getPurity() != null || request.getMarkup() != null) {
            Double hargaPerGram = goldPriceService.getLatestHargaJual();
            Double purityDecimal = existingProduct.getPurity() / 24.0;
            Double markupDecimal = existingProduct.getMarkup() / 100.0;
            double totalHarga = (hargaPerGram * purityDecimal) * (1 + markupDecimal)
                    * existingProduct.getWeight();
            existingProduct.setFinalPrice(totalHarga);
        }

        existingProduct.setUpdateAt(LocalDateTime.now());
        return productRepository.save(existingProduct);
    }

    public ProductResponseDto updateProductWithFormattedResponse(Long productId, ProductRequestDto request) {
        Product product = updateProduct(productId, request);
        return ProductResponseDto.fromProduct(product);
    }

    public Product updateStockProduct(Long productId, int newStock) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produk tidak di temukan dengan ID : " + productId));

        if (newStock < 0) {
            throw new RuntimeException("Stok tidak boleh kurang dari 0");
        }

        if (newStock < product.getMinStock()) {
            throw new RuntimeException("Stok baru (" + newStock + ") tidak boleh lebih kecil dari stok minimum ("
                    + product.getMinStock() + ")");
        }

        product.setStock(newStock);
        product.setUpdateAt(LocalDateTime.now());

        return productRepository.save(product);
    }

    public Product addStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produk tidak ditemukan dengan ID: " + productId));

        if (quantity < 0) {
            throw new RuntimeException("Quantity tidak boleh negatif");
        }

        int newStock = product.getStock() + quantity;

        if (newStock < product.getMinStock()) {
            throw new RuntimeException("Stok baru (" + newStock + ") tidak boleh lebih kecil dari stok minimum ("
                    + product.getMinStock() + ")");
        }

        product.setStock(newStock);
        product.setUpdateAt(LocalDateTime.now());

        return productRepository.save(product);
    }

    public Product reduceStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produk tidak ditemukan dengan ID: " + productId));

        if (quantity < 0) {
            throw new RuntimeException("Quantity tidak boleh negatif");
        }

        if (product.getStock() < quantity) {
            throw new RuntimeException("Stok tidak mencukupi. Stok tersedia: " + product.getStock());
        }

        int newStock = product.getStock() - quantity;

        if (newStock < product.getMinStock()) {
            throw new RuntimeException("Stok baru (" + newStock + ") tidak boleh lebih kecil dari stok minimum ("
                    + product.getMinStock() + ")");
        }

        product.setStock(newStock);
        product.setUpdateAt(LocalDateTime.now());

        return productRepository.save(product);
    }

    public Product getProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produk tidak di temukan dengan ID : " + productId));
    }

    public ProductResponseDto getProductByIdWithFormattedResponse(Long productId) {
        Product product = getProductById(productId);
        return ProductResponseDto.fromProduct(product);
    }

    public Page<Product> getAllProductsWithPagination(int page, int size, String sortBy, String sortDirection) {
        Sort sort = sortDirection.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return productRepository.findAll(pageable);
    }

    public Page<ProductResponseDto> getAllProductsWithPaginationAndFormattedResponse(int page, int size, String sortBy,
            String sortDirection) {
        Page<Product> productPage = getAllProductsWithPagination(page, size, sortBy, sortDirection);
        return productPage.map(ProductResponseDto::fromProduct);
    }

    public Page<Product> searchProductsWithPagination(String keyword, int page, int size, String sortBy,
            String sortDirection) {
        Sort sort = sortDirection.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        if (keyword == null || keyword.trim().isEmpty()) {
            return productRepository.findAll(pageable);
        }

        return productRepository
                .findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrCategoryContainingIgnoringCase(
                        keyword.trim(), keyword.trim(), keyword.trim(), pageable);
    }

    public Page<ProductResponseDto> searchProductsWithPaginationAndFormattedResponse(String keyword, int page, int size,
            String sortBy, String sortDirection) {
        Page<Product> productPage = searchProductsWithPagination(keyword, page, size, sortBy, sortDirection);
        return productPage.map(ProductResponseDto::fromProduct);
    }

    public List<Product> getProductsWithLowStock() {
        List<Product> activeProducts = productRepository.findByIsActiveTrue();

        return activeProducts.stream()
                .filter(product -> product.getStock() <= product.getMinStock())
                .collect(Collectors.toList());
    }

    public List<ProductResponseDto> getProductsWithLowStockWithFormattedResponse() {
        List<Product> products = getProductsWithLowStock();
        return products.stream()
                .map(ProductResponseDto::fromProduct)
                .collect(Collectors.toList());
    }

    public Page<Product> getAllActiveProductsWithPagination(int page, int size, String sortBy, String sortDirection) {
        Sort sort = sortDirection.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return productRepository.findByIsActiveTrue(pageable);
    }

    public Page<ProductResponseDto> getAllActiveProductsWithPaginationAndFormattedResponse(int page, int size,
            String sortBy, String sortDirection) {
        Page<Product> productPage = getAllActiveProductsWithPagination(page, size, sortBy, sortDirection);
        return productPage.map(ProductResponseDto::fromProduct);
    }
    
    public Page<Product> searchActiveProductsWithPagination(String keyword, int page, int size, String sortBy,
            String sortDirection) {
        Sort sort = sortDirection.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        if (keyword == null || keyword.trim().isEmpty()) {
            return productRepository.findByIsActiveTrue(pageable);
        }

        return productRepository
                .findByIsActiveTrueAndNameContainingIgnoreCaseOrIsActiveTrueAndDescriptionContainingIgnoreCaseOrIsActiveTrueAndCategoryContainingIgnoreCase(
                        keyword.trim(), keyword.trim(), keyword.trim(), pageable);
    }

    public Page<ProductResponseDto> searchActiveProductsWithPaginationAndFormattedResponse(String keyword, int page, int size,
            String sortBy, String sortDirection) {
        Page<Product> productPage = searchActiveProductsWithPagination(keyword, page, size, sortBy, sortDirection);
        return productPage.map(ProductResponseDto::fromProduct);
    }

    public void deleteProduct(Long productId) {
        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produk tidak ditemukan dengan ID : " + productId));
        if (existingProduct.getImageUrl() != null && existingProduct.getImageUrl().contains("cloudinary")) {
            try {
                cloudinaryService.deleteImage(existingProduct.getImageUrl());
            } catch (Exception e) {
                System.err.println("Gagal menghapus gambar dari Cloudinary: " + e.getMessage());
            }
        }
        productRepository.delete(existingProduct);
    }

    public boolean existsById(Long productId) {
        return productRepository.existsById(productId);
    }

    /**
     * Mengupdate harga semua produk aktif berdasarkan harga emas terbaru
     */
    @Transactional
    public Map<String, Object> updateAllProductPrices() {
        try {
            log.info(">> Memulai update harga semua produk");

            // Ambil semua produk aktif
            List<Product> activeProducts = productRepository.findByIsActiveTrue();

            if (activeProducts.isEmpty()) {
                log.info(">> Tidak ada produk aktif untuk diupdate");
                return Map.of(
                        "success", true,
                        "message", "Tidak ada produk aktif untuk diupdate",
                        "updatedCount", 0,
                        "timestamp", LocalDateTime.now());
            }

            int updatedCount = 0;
            int failedCount = 0;
            List<String> failedProducts = new java.util.ArrayList<>();

            for (Product product : activeProducts) {
                try {
                    boolean updated = updateProductPrice(product);
                    if (updated) {
                        updatedCount++;
                    } else {
                        failedCount++;
                        failedProducts.add(product.getName() + " (ID: " + product.getId() + ")");
                    }
                } catch (Exception e) {
                    log.error(">> Gagal update harga produk {}: {}", product.getName(), e.getMessage());
                    failedCount++;
                    failedProducts.add(product.getName() + " (ID: " + product.getId() + ") - " + e.getMessage());
                }
            }

            Map<String, Object> result = Map.of(
                    "success", true,
                    "message",
                    String.format("Update harga produk selesai. Berhasil: %d, Gagal: %d", updatedCount, failedCount),
                    "updatedCount", updatedCount,
                    "failedCount", failedCount,
                    "failedProducts", failedProducts,
                    "totalProducts", activeProducts.size(),
                    "timestamp", LocalDateTime.now());

            log.info(">> Update harga produk selesai. Berhasil: {}, Gagal: {}", updatedCount, failedCount);
            return result;

        } catch (Exception e) {
            log.error(">> Error dalam update harga produk: {}", e.getMessage());
            throw new RuntimeException("Gagal mengupdate harga produk: " + e.getMessage(), e);
        }
    }

    /**
     * Mengupdate harga produk berdasarkan kadar kemurnian tertentu
     */
    @Transactional
    public Map<String, Object> updateProductPricesByPurity(int purity) {
        try {
            log.info(">> Memulai update harga produk dengan kadar kemurnian {}k", purity);

            // Ambil semua produk aktif dengan kadar kemurnian tertentu
            List<Product> productsByPurity = productRepository.findByIsActiveTrueAndPurity(purity);

            if (productsByPurity.isEmpty()) {
                log.info(">> Tidak ada produk aktif dengan kadar kemurnian {}k", purity);
                return Map.of(
                        "success", true,
                        "message", "Tidak ada produk aktif dengan kadar kemurnian " + purity + "k",
                        "updatedCount", 0,
                        "purity", purity + "k",
                        "timestamp", LocalDateTime.now());
            }

            int updatedCount = 0;
            int failedCount = 0;
            List<String> failedProducts = new java.util.ArrayList<>();
            List<String> updatedProducts = new java.util.ArrayList<>();

            for (Product product : productsByPurity) {
                try {
                    boolean updated = updateProductPrice(product);
                    if (updated) {
                        updatedCount++;
                        updatedProducts.add(product.getName() + " (ID: " + product.getId() + ")");
                    } else {
                        failedCount++;
                        failedProducts.add(product.getName() + " (ID: " + product.getId() + ")");
                    }
                } catch (Exception e) {
                    log.error(">> Gagal update harga produk {}: {}", product.getName(), e.getMessage());
                    failedCount++;
                    failedProducts.add(product.getName() + " (ID: " + product.getId() + ") - " + e.getMessage());
                }
            }

            Map<String, Object> result = Map.of(
                    "success", true,
                    "message", String.format("Update harga produk %dk selesai. Berhasil: %d, Gagal: %d",
                            purity, updatedCount, failedCount),
                    "updatedCount", updatedCount,
                    "failedCount", failedCount,
                    "updatedProducts", updatedProducts,
                    "failedProducts", failedProducts,
                    "totalProducts", productsByPurity.size(),
                    "purity", purity + "k",
                    "timestamp", LocalDateTime.now());

            log.info(">> Update harga produk {}k selesai. Berhasil: {}, Gagal: {}", purity, updatedCount, failedCount);
            return result;

        } catch (Exception e) {
            log.error(">> Error dalam update harga produk {}k: {}", purity, e.getMessage());
            throw new RuntimeException("Gagal mengupdate harga produk: " + e.getMessage(), e);
        }
    }

    /**
     * Mengupdate harga produk berdasarkan kadar kemurnian
     */
    @Transactional
    public boolean updateProductPrice(Product product) {
        try {
            log.info(">> MarkUp produk: {} (ID: {})", product.getMarkup(), product.getId());
            // Ambil harga emas berdasarkan kadar kemurnian produk
            double hargaPerGram = goldPriceService.getLatestHargaJual(product.getPurity());

            // Hitung harga baru
            double purityDecimal = product.getPurity() / 24.0;
            double markupDecimal = product.getMarkup() / 100.0;
            double newFinalPrice = (hargaPerGram * purityDecimal) * (1 + markupDecimal) * product.getWeight();
            log.info(">> Final Price produk: {} (ID: {})", newFinalPrice, product.getId());

            // Update harga produk
            double oldPrice = product.getFinalPrice();
            product.setFinalPrice(newFinalPrice);
            product.setUpdateAt(LocalDateTime.now());

            productRepository.save(product);

            log.info(">> Produk {} (ID: {}) harga diupdate: {} -> {}",
                    product.getName(), product.getId(), oldPrice, newFinalPrice);

            return true;

        } catch (Exception e) {
            log.error(">> Gagal update harga produk {}: {}", product.getName(), e.getMessage());
            return false;
        }
    }

    public Map<String, Object> getProductFilterStats() {
        Map<String, Object> stats = new HashMap<>();

        // Statistik kategori
        Map<String, Long> categoryStats = productRepository.findAll().stream()
                .filter(Product::getIsActive)
                .collect(Collectors.groupingBy(
                        Product::getCategory,
                        Collectors.counting()));
        stats.put("categories", categoryStats);

        // Statistik kadar emas
        Map<String, Long> purityStats = productRepository.findAll().stream()
                .filter(Product::getIsActive)
                .collect(Collectors.groupingBy(
                        product -> product.getPurity() + "K",
                        Collectors.counting()));
        stats.put("purities", purityStats);

        // Statistik berat
        Map<String, Long> weightStats = new HashMap<>();
        List<Product> activeProducts = productRepository.findByIsActiveTrue();

        long weight0to2 = activeProducts.stream()
                .filter(p -> p.getWeight() >= 0 && p.getWeight() <= 2)
                .count();
        weightStats.put("0-2", weight0to2);

        long weight2to5 = activeProducts.stream()
                .filter(p -> p.getWeight() > 2 && p.getWeight() <= 5)
                .count();
        weightStats.put("2-5", weight2to5);

        long weight5to10 = activeProducts.stream()
                .filter(p -> p.getWeight() > 5 && p.getWeight() <= 10)
                .count();
        weightStats.put("5-10", weight5to10);

        long weight10plus = activeProducts.stream()
                .filter(p -> p.getWeight() > 10)
                .count();
        weightStats.put("10+", weight10plus);

        stats.put("weights", weightStats);

        // Range harga - PERBAIKAN: Gunakan finalPrice
        if (!activeProducts.isEmpty()) {
            double minPrice = activeProducts.stream()
                    .mapToDouble(product -> {
                        // Gunakan finalPrice jika ada, fallback ke price
                        return product.getFinalPrice() != null ? product.getFinalPrice() : 0.0;
                    })
                    .min()
                    .orElse(0.0);

            double maxPrice = activeProducts.stream()
                    .mapToDouble(product -> {
                        // Gunakan finalPrice jika ada, fallback ke price
                        return product.getFinalPrice() != null ? product.getFinalPrice() : 0.0;
                    })
                    .max()
                    .orElse(0.0);

            Map<String, Double> priceRange = new HashMap<>();
            priceRange.put("min", minPrice);
            priceRange.put("max", maxPrice);
            stats.put("priceRange", priceRange);
        }

        return stats;
    }

    /**
     * Mendapatkan produk berdasarkan kadar kemurnian
     */
    public List<Product> getProductsByPurity(int purity) {
        try {
            return productRepository.findByPurityAndIsActiveTrue(purity);
        } catch (Exception e) {
            log.error(">> Error getting products by purity {}: {}", purity, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Save produk (untuk update harga)
     */
    @Transactional
    public Product saveProduct(Product product) {
        try {
            return productRepository.save(product);
        } catch (Exception e) {
            log.error(">> Error saving product {}: {}", product.getName(), e.getMessage());
            throw new RuntimeException("Gagal menyimpan produk: " + e.getMessage());
        }
    }

    /**
     * Update harga produk berdasarkan perubahan harga emas (dipanggil dari GoldPriceService)
     */
    @Transactional
    public void updateProductPricesByGoldPriceChange(GoldPrice newGoldPrice) {
        try {
            log.info(">> ProductService: Memulai update harga produk berdasarkan harga emas baru: {}", newGoldPrice.getHargaJual24k());
            
            // Ambil semua produk aktif
            List<Product> activeProducts = productRepository.findByIsActiveTrue();
            
            if (activeProducts.isEmpty()) {
                log.info(">> ProductService: Tidak ada produk aktif yang perlu diupdate");
                return;
            }
            
            int updatedCount = 0;
            
            for (Product product : activeProducts) {
                try {
                    // Hitung harga baru berdasarkan kadar kemurnian
                    double newPrice = calculateProductPriceByPurity(
                        product.getPurity(), 
                        newGoldPrice.getHargaJual24k(), // Gunakan harga jual 24k sebagai base
                        true // Selalu gunakan harga jual untuk produk
                    );
                    
                    // Update finalPrice produk
                    product.setFinalPrice(newPrice);
                    product.setUpdateAt(LocalDateTime.now());
                    updatedCount++;
                    
                    log.debug(">> ProductService: Update produk {} ({}K): {} -> {}", 
                        product.getName(), product.getPurity(), 
                        product.getFinalPrice(),
                        newPrice);
                        
                } catch (Exception e) {
                    log.error(">> ProductService: Error update produk {}: {}", product.getName(), e.getMessage());
                }
            }
            
            // Simpan semua perubahan
            productRepository.saveAll(activeProducts);
            
            log.info(">> ProductService: Berhasil update {} produk", updatedCount);
            
        } catch (Exception e) {
            log.error(">> ProductService: Error dalam update harga produk: {}", e.getMessage(), e);
            throw new RuntimeException("Gagal update harga produk: " + e.getMessage());
        }
    }

    /**
     * Hitung harga produk berdasarkan kadar kemurnian dan harga emas
     */
    private double calculateProductPriceByPurity(int purity, double harga24k, boolean isJual) {
        // Gunakan logika yang sama dengan GoldPriceService
        double purityRatio = purity / 24.0;
        double basePrice = harga24k * purityRatio;
        
        if (isJual) {
            // Tambah margin untuk harga jual (misal 5%)
            return basePrice * 1.05;
        } else {
            // Kurangi margin untuk harga beli (misal 3%)
            return basePrice * 0.97;
        }
    }

    /**
     * Helper method untuk mendapatkan harga jual berdasarkan kadar kemurnian
     */
    private double getHargaJualByPurity(GoldPrice goldPrice, int purity) {
        switch (purity) {
            case 24:
                return goldPrice.getHargaJual24k();
            case 22:
                return goldPrice.getHargaJual22k();
            case 18:
                return goldPrice.getHargaJual18k();
            default:
                throw new RuntimeException("Kadar kemurnian " + purity + "k tidak didukung");
        }
    }
}
